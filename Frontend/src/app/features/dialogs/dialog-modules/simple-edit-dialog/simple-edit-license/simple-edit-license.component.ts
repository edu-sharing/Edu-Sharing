import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { MatButtonToggleGroup } from '@angular/material/button-toggle';
import { UIAnimation, VCard } from 'ngx-edu-sharing-ui';
import { Observable, forkJoin, from } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import {
    ConfigurationService,
    Node,
    RestConnectorService,
    RestConstants,
    RestIamService,
    RestNodeService,
    RestOrganizationService,
} from '../../../../../core-module/core.module';
import { NodeHelperService } from '../../../../../services/node-helper.service';
import { Toast } from '../../../../../services/toast';
import { Values } from '../../../../../features/mds/types/types';

@Component({
    selector: 'es-simple-edit-license',
    templateUrl: 'simple-edit-license.component.html',
    styleUrls: ['simple-edit-license.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class SimpleEditLicenseComponent implements OnInit {
    @ViewChild('modeGroup') modeGroup: MatButtonToggleGroup;
    @ViewChild('licenseGroup') licenseGroup: MatButtonToggleGroup;
    @Input() fromUpload: boolean;
    @Output() onInitFinished = new EventEmitter<void>();
    @Output() onError = new EventEmitter<any>();

    _nodes: Node[];
    allowedLicenses: string[];
    authorFreetext: string;
    invalid: boolean;
    wasInvalid: boolean;
    private initialLicense: string;
    private initalAuthorFreetext: string;
    private initialMode: string;
    tpLicense: boolean;
    ccTitleOfWork: string;
    ccSourceUrl: string;
    ccProfileUrl: string;
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
        if (nodes) {
            this.prepare(true);
        }
    }
    constructor(
        private nodeApi: RestNodeService,
        private connector: RestConnectorService,
        private configService: ConfigurationService,
        private iamApi: RestIamService,
        private nodeHelper: NodeHelperService,
        private organizationApi: RestOrganizationService,
        private toast: Toast,
    ) {
        this.configService
            .get('simpleEdit.licenses', ['NONE', 'COPYRIGHT_FREE', 'CC_BY', 'CC_0'])
            .subscribe((licenses) => (this.allowedLicenses = licenses));
        this.connector
            .hasToolPermission(RestConstants.TOOLPERMISSION_LICENSE)
            .subscribe((tp) => (this.tpLicense = tp));
    }
    async getESUID() {
        return (await this.iamApi.getCurrentUserVCard()).uid;
    }
    isDirty() {
        // state is untouched -> so not dirty
        if (this.invalid || !this.tpLicense) {
            return false;
        }
        // when was initialy invalid -> the invalid was changed, so it is touched
        if (this.wasInvalid) {
            return true;
        }
        return (
            this.initialLicense !== this.licenseGroup.value ||
            this.initalAuthorFreetext !== this.authorFreetext ||
            this.initialMode !== this.modeGroup.value
        );
    }
    save(): Observable<any> {
        if (!this.isDirty()) {
            return new Observable<void>((observer) => {
                observer.next();
                observer.complete();
                return;
            });
        }
        return forkJoin(
            this._nodes.map((n) => {
                return from(this.getProperties()).pipe(
                    switchMap((props) =>
                        this.nodeApi.editNodeMetadataNewVersion(
                            n.ref.id,
                            RestConstants.COMMENT_LICENSE_UPDATE,
                            props,
                        ),
                    ),
                );
            }),
        );
    }

    prepare(updateInvalid = false) {
        forkJoin(
            this._nodes.map((n) => this.nodeApi.getNodeMetadata(n.ref.id, [RestConstants.ALL])),
        ).subscribe(
            (nodes) => {
                this._nodes = nodes.map((n) => n.node);
                const license = this.nodeHelper.getValueForAll(
                    this._nodes,
                    RestConstants.CCM_PROP_LICENSE,
                    null,
                    'NONE',
                    false,
                );
                this.authorFreetext = this.nodeHelper.getValueForAll(
                    this._nodes,
                    RestConstants.CCM_PROP_AUTHOR_FREETEXT,
                    '',
                    '',
                    false,
                );
                this.ccTitleOfWork = this.nodeHelper.getValueForAll(
                    this._nodes,
                    RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK,
                    '',
                );
                this.ccProfileUrl = this.nodeHelper.getValueForAll(
                    this._nodes,
                    RestConstants.CCM_PROP_LICENSE_PROFILE_URL,
                    '',
                );
                this.ccSourceUrl = this.nodeHelper.getValueForAll(
                    this._nodes,
                    RestConstants.CCM_PROP_LICENSE_SOURCE_URL,
                    '',
                );
                const vcard = new VCard(
                    this.nodeHelper.getValueForAll(
                        this._nodes,
                        RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR,
                        '',
                        '',
                        false,
                    ),
                );
                let isValid = true;
                if (license) {
                    if (license.startsWith('CC_BY')) {
                        const version = this.nodeHelper.getValueForAll(
                            this._nodes,
                            RestConstants.CCM_PROP_LICENSE_CC_VERSION,
                            null,
                            null,
                            false,
                        );
                        isValid = version === '4.0';
                    }
                    if (this.allowedLicenses.indexOf(license) === -1) {
                        isValid = false;
                    }
                } else {
                    isValid = false;
                }
                this.initialLicense = license;
                this.initalAuthorFreetext = this.authorFreetext;
                setTimeout(async () => {
                    if (updateInvalid) {
                        this.invalid = !isValid;
                        this.wasInvalid = this.invalid;
                    }
                    if (this.tpLicense && this.modeGroup) {
                        if (this.fromUpload) {
                            this.modeGroup.value = 'own';
                        } else {
                            const esuid = await this.getESUID();
                            if (this.modeGroup) {
                                if (esuid && esuid === vcard.uid) {
                                    this.modeGroup.value = 'own';
                                } else {
                                    this.modeGroup.value = 'foreign';
                                }
                            }
                        }
                        if (this.modeGroup) {
                            this.initialMode = this.modeGroup.value;
                            if (isValid) {
                                this.licenseGroup.value = license;
                            } else {
                                this.licenseGroup.value = 'NONE';
                            }
                        }
                    }
                    this.onInitFinished.emit();
                });
            },
            (error) => this.onError.emit(error),
        );
    }

    private async getProperties(): Promise<Values> {
        const properties: any = {};
        if (this.modeGroup.value === 'own') {
            const vcard = await this.iamApi.getCurrentUserVCard();
            properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = [
                vcard.toVCardString(),
            ];
            properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = null;
        } else {
            properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR] = null;
            properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT] = [this.authorFreetext];
        }
        if (this.isCCAttributableLicense()) {
            if (this.ccTitleOfWork) {
                properties[RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK] = [this.ccTitleOfWork];
            } else {
                properties[RestConstants.CCM_PROP_LICENSE_TITLE_OF_WORK] = null;
            }
            if (this.ccSourceUrl) {
                properties[RestConstants.CCM_PROP_LICENSE_SOURCE_URL] = [this.ccSourceUrl];
            } else {
                properties[RestConstants.CCM_PROP_LICENSE_SOURCE_URL] = null;
            }
            if (this.ccProfileUrl) {
                properties[RestConstants.CCM_PROP_LICENSE_PROFILE_URL] = [this.ccProfileUrl];
            } else {
                properties[RestConstants.CCM_PROP_LICENSE_PROFILE_URL] = null;
            }
        }
        properties[RestConstants.CCM_PROP_LICENSE] = [this.licenseGroup.value];
        properties[RestConstants.CCM_PROP_LICENSE_CC_VERSION] =
            this.licenseGroup.value === 'CC_BY' ? ['4.0'] : null;
        return properties;
    }

    isCCAttributableLicense() {
        return (
            this.licenseGroup &&
            this.licenseGroup.value &&
            this.licenseGroup.value.startsWith('CC_BY')
        );
    }

    async ngOnInit() {
        if (!(await this.getESUID())) {
            console.warn('Current user has no esuid, detecting owner of license is impossible');
        }
    }
}
