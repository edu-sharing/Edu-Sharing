import { Injectable } from '@angular/core';
import {
    AuthenticationService,
    ClientConfig,
    ConfigService,
    LicenseAgreement,
    LoginInfo,
    Node,
} from 'ngx-edu-sharing-api';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import {
    catchError,
    distinctUntilChanged,
    filter,
    first,
    map,
    switchMap,
    tap,
} from 'rxjs/operators';
import { RestNodeService, SessionStorageService } from '../core-module/core.module';
import {
    LicenseAgreementDialogData,
    LicenseAgreementDialogResult,
} from '../features/dialogs/dialog-modules/license-agreement-dialog/license-agreement-dialog-data';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { MainNavService } from '../main/navigation/main-nav.service';
import { TranslationsService } from 'ngx-edu-sharing-ui';

/** Version string to remember as accepted in case we encountered errors fetching the license. */
const LICENSE_ERROR_CASE_VERSION = '0.0';
const LICENSE_AGREEMENT_STORAGE_KEY = 'licenseAgreement';

@Injectable({
    providedIn: 'root',
})
export class LicenseAgreementService {
    /** Whether the agreement (if any) is accepted for the current user. */
    private agreementClearedSubject = new rxjs.BehaviorSubject<
        LicenseAgreementDialogResult | 'pending' | 'dialog-not-shown'
    >('pending');

    constructor(
        private authentication: AuthenticationService,
        private config: ConfigService,
        private dialogs: DialogsService,
        private mainNav: MainNavService,
        private nodeService: RestNodeService,
        private session: SessionStorageService,
        private translations: TranslationsService,
    ) {}

    /**
     * Returns an Observable that emits once the license agreement has either been agreed to by the
     * user or we found that we don't have to show any agreement to the user.
     */
    waitForAgreementCleared(): Observable<void> {
        return this.agreementClearedSubject.pipe(
            first((result) => result === 'accepted' || result === 'dialog-not-shown'),
            map(() => void 0),
        );
    }

    setup() {
        // Pipe from configuration and login information to downloading the license notice and
        // showing it to the user. We pass `null` to indicate that there is no license notice to be
        // shown.
        rxjs.combineLatest([
            this.config.observeConfig().pipe(filter((config) => !!config)),
            this.authentication.observeLoginInfo(),
        ])
            .pipe(
                filter(([_, loginInfo]) => loginInfo?.statusCode === 'OK'),
                tap(() => this.agreementClearedSubject.next('pending')),
                map(([config, loginInfo]) => {
                    if (this.shouldShowLicenseAgreementForUser(config, loginInfo)) {
                        return this.getNodeId(config.licenseAgreement);
                    } else {
                        return null;
                    }
                }),
                distinctUntilChanged(),
                switchMap((nodeId) => this.downloadLicenseNode(nodeId)),
                switchMap((licenseNode) =>
                    this.hasUserAcceptedLicense(licenseNode?.content.version).pipe(
                        map((hasAccepted) => ({ hasAccepted, licenseNode })),
                    ),
                ),
                switchMap(({ licenseNode, hasAccepted }) =>
                    this.downloadLicenseContentIfNeeded(licenseNode, hasAccepted),
                ),
                catchError((error) =>
                    this.hasUserAcceptedLicense(LICENSE_ERROR_CASE_VERSION).pipe(
                        map((hasAccepted) => {
                            if (hasAccepted) {
                                return null;
                            } else if (typeof error === 'string') {
                                return {
                                    licenseHtml: error,
                                    version: LICENSE_ERROR_CASE_VERSION,
                                };
                            } else {
                                console.error(
                                    'Unexpected error when trying to show the license agreement',
                                    error,
                                );
                                return null;
                            }
                        }),
                    ),
                ),
                switchMap((data: LicenseAgreementDialogData) =>
                    this.showLicenseDialog(data).pipe(
                        map((result) => ({ result, version: data?.version })),
                    ),
                ),
            )
            .subscribe(
                (data: { result: LicenseAgreementDialogResult | null; version?: string }) => {
                    this.agreementClearedSubject.next(data.result ?? 'dialog-not-shown');
                    if (data.result === 'accepted') {
                        this.onAccepted(data.version);
                    } else if (data.result === 'declined') {
                        this.onDeclined();
                        this.setup();
                    }
                },
            );
    }

    private shouldShowLicenseAgreementForUser(config: ClientConfig, loginInfo: LoginInfo): boolean {
        return config.licenseAgreement && !loginInfo.isGuest && loginInfo.isValidLogin;
    }

    private getNodeId(licenseAgreement: LicenseAgreement): string {
        let nodeId: string = null;
        for (const node of licenseAgreement.nodeId) {
            if (node.language == null) {
                nodeId = node.value;
            }
            if (node.language === this.translations.getLanguage()) {
                return node.value;
            }
        }
        return nodeId;
    }

    private hasUserAcceptedLicense(version: string): Observable<boolean | null> {
        if (version) {
            return this.session
                .get(LICENSE_AGREEMENT_STORAGE_KEY, false)
                .pipe(map((acceptedVersion) => acceptedVersion === version));
        } else {
            return rxjs.of(null);
        }
    }

    private downloadLicenseNode(nodeId: string | null): Observable<Node | null> {
        if (nodeId) {
            return this.nodeService.getNodeMetadata(nodeId).pipe(
                map((nodeWrapper) => nodeWrapper.node),
                catchError(() =>
                    rxjs.throwError(
                        `Error loading metadata for license agreement node '${nodeId}'`,
                    ),
                ),
            );
        } else {
            return rxjs.of(null);
        }
    }

    private downloadLicenseContentIfNeeded(
        licenseNode: Node,
        hasAccepted: boolean,
    ): Observable<LicenseAgreementDialogData | null> {
        if (licenseNode && !hasAccepted) {
            return this.downloadLicenseContent(licenseNode.ref.id).pipe(
                map((licenseHtml) => ({
                    licenseHtml,
                    version: licenseNode.content.version,
                })),
            );
        } else {
            return rxjs.of(null);
        }
    }

    private downloadLicenseContent(nodeId: string): Observable<string> {
        return this.nodeService.getNodeTextContent(nodeId).pipe(
            map((content) => content.html || content.raw || content.text),
            catchError(() =>
                rxjs.throwError(`Error loading content for license agreement node '${nodeId}'`),
            ),
        );
    }

    private showLicenseDialog(
        data: LicenseAgreementDialogData | null,
    ): Observable<LicenseAgreementDialogResult | null> {
        if (data) {
            return rxjs
                .from(this.dialogs.openLicenseAgreementDialog(data))
                .pipe(switchMap((dialogRef) => dialogRef.afterClosed()));
        } else {
            return rxjs.of(null);
        }
    }

    private onAccepted(version: string): void {
        void this.session.set(LICENSE_AGREEMENT_STORAGE_KEY, version);
    }

    private onDeclined(): void {
        this.mainNav.getMainNav().logout();
    }
}
