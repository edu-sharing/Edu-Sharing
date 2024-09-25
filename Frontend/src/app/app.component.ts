import {
    AfterViewInit,
    Component,
    DoCheck,
    ElementRef,
    HostListener,
    Injector,
    NgZone,
    OnInit,
    ViewChild,
} from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { AuthenticationService } from 'ngx-edu-sharing-api';
import {
    AccessibilityService,
    AppContainerService,
    TranslationsService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { environment } from '../environments/environment';
import { ConfigurationService, RestHelper, RestNetworkService } from './core-module/core.module';
import { WorkspaceManagementDialogsComponent } from './features/management-dialogs/management-dialogs.component';
import { ManagementDialogsService } from './features/management-dialogs/management-dialogs.service';
import { CookieInfoComponent } from './main/cookie-info/cookie-info.component';
import { LoadingScreenService } from './main/loading-screen/loading-screen.service';
import { MainNavService } from './main/navigation/main-nav.service';
import { printCurrentTaskInfo } from './main/track-change-detection';
import { BridgeService } from './services/bridge.service';
import { LicenseAgreementService } from './services/license-agreement.service';
import { ScrollPositionRestorationService } from './services/scroll-position-restoration.service';
import { ThemeService } from './services/theme.service';
import { PlatformLocation } from '@angular/common';

@Component({
    selector: 'es-app',
    templateUrl: 'app.component.html',
    providers: [],
})
export class AppComponent implements OnInit, DoCheck, AfterViewInit {
    private static readonly CHECKS_PER_SECOND_WARNING_THRESHOLD = 0;
    private static readonly CONSECUTIVE_TRANSGRESSION_THRESHOLD = 10;
    private static history = new BehaviorSubject<string[]>([]);

    public static isRedirectedFromLogin() {
        const history = AppComponent.history.value;
        if (history.length < 2) {
            return false;
        }
        return (
            history[history.length - 1].indexOf(UIConstants.ROUTER_PREFIX + 'login') !== -1 ||
            history[history.length - 1].indexOf(UIConstants.ROUTER_PREFIX) === -1
        );
    }

    @ViewChild('management') management: WorkspaceManagementDialogsComponent;
    @ViewChild('cookie') cookie: CookieInfoComponent;

    isLoading = this.loadingScreen.isLoading;

    private numberOfChecks = 0;
    private consecutiveTransgression = 0;
    private checksMonitorInterval: number;

    /**
     * Adds a prefix to all routes for compatibility with tomcat.
     */
    static transformRoute(route: any): any {
        const result: any = []; // we need a deep copy
        for (const r of route) {
            const a: any = {
                path: r.path,
                component: r.component,
                children: r.children,
            };

            if (a.path) a.path = UIConstants.ROUTER_PREFIX + r.path;
            result.push(a);
        }
        return result;
    }

    // FIXME: should we really do this?
    // > Warning: The beforeunload event should only be used to alert the user of unsaved changes.
    // > Once those changes are saved, the event should be removed. It should never be added
    // > unconditionally to the page, as doing so can hurt performance in some cases. See the legacy
    // > APIs section for details.
    // --- https://developer.chrome.com/blog/page-lifecycle-api/
    @HostListener('window:beforeunload', ['$event'])
    interceptRoute(event: BeforeUnloadEvent) {
        // console.log(event);
    }

    constructor(
        private appContainer: AppContainerService,
        private elementRef: ElementRef<HTMLElement>,
        private mainNavService: MainNavService,
        private dialogs: ManagementDialogsService,
        private ngZone: NgZone,
        private bridge: BridgeService,
        private injector: Injector,
        private accessibilityService: AccessibilityService,
        private translations: TranslationsService,
        private loadingScreen: LoadingScreenService,
        private licenseAgreement: LicenseAgreementService,
        private themeService: ThemeService,
        private authentication: AuthenticationService,
        private configuration: ConfigurationService,
        private scrollPositionRestoration: ScrollPositionRestorationService,
        private legacyRestService: RestNetworkService,
    ) {
        this.injector.get(Router).events.subscribe((event) => {
            // if (event instanceof NavigationStart) {
            //     console.log('NavigationStart', event.url);
            // }
            if (event instanceof NavigationEnd) {
                AppComponent.history.value.push(event.url);
                AppComponent.history.next(AppComponent.history.value);
            }
        });
        this.ngZone.runOutsideAngular(() => {
            // Do not trigger change detection with setInterval.
            this.checksMonitorInterval = window.setInterval(() => this.monitorChecks(), 1000);
        });
    }

    ngOnInit(): void {
        this.elementRef.nativeElement.removeAttribute('ng-version');
        this.translations
            .initialize()
            .pipe(
                this.loadingScreen.showUntilFinished({
                    // The router component lives as long as the application, so we don't need to
                    // set `until` to anything meaningful.
                    until: rxjs.EMPTY,
                }),
            )
            .subscribe();
        this.setUserScale();
        this.registerRedirectToLogin();
        this.registerContrastMode();
        this.appContainer.init(this.elementRef.nativeElement);
        this.licenseAgreement.setup();
        this.scrollPositionRestoration.setup();
        this.legacyRestService.init();
    }

    ngDoCheck(): void {
        this.numberOfChecks++;
        if (environment.traceChangeDetection) {
            printCurrentTaskInfo('doCheck');
        }
    }

    ngAfterViewInit(): void {
        this.dialogs.registerDialogsComponent(this.management);
        this.mainNavService.registerCookieInfo(this.cookie);
        this.mainNavService.registerAccessibility();
    }

    private monitorChecks(): void {
        // console.log('Change detections run in the past second:', this.numberOfChecks);
        if (this.numberOfChecks > AppComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD) {
            this.consecutiveTransgression++;
            if (this.consecutiveTransgression >= AppComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD) {
                console.warn(
                    'Change detection triggered more than ' +
                        AppComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD +
                        ' times per second for the past ' +
                        AppComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD +
                        ' seconds consecutively.' +
                        ' Not showing any more warnings.',
                );
                window.clearInterval(this.checksMonitorInterval);
            }
        } else {
            this.consecutiveTransgression = 0;
        }
        this.numberOfChecks = 0;
    }

    private setUserScale(): void {
        if (this.bridge.isRunningCordova()) {
            const viewport: HTMLMetaElement = document.head.querySelector('meta[name="viewport"]');
            viewport.content += ', user-scalable=no';
        }
    }

    /**
     * Redirects the user to the login page in case they don't have a valid session.
     */
    private registerRedirectToLogin(): void {
        this.authentication.observeLoginInfo().subscribe(async (loginInfo) => {
            // dirty hack: location + router components return null values
            const route = window.location.pathname.substring(
                this.injector.get(PlatformLocation).getBaseHrefFromDOM()?.length ?? 0,
            );
            if (
                !loginInfo.isValidLogin &&
                !(
                    route.startsWith(UIConstants.ROUTER_PREFIX + 'login') ||
                    route.startsWith(UIConstants.ROUTER_PREFIX + 'register') ||
                    route.startsWith(UIConstants.ROUTER_PREFIX + 'error') ||
                    route.startsWith(UIConstants.ROUTER_PREFIX + 'message') ||
                    // public link sharing
                    route.startsWith(UIConstants.ROUTER_PREFIX + 'sharing') ||
                    route.startsWith('shibboleth')
                )
            ) {
                RestHelper.goToLogin(this.injector.get(Router), this.configuration);
            } else if (loginInfo.isGuest) {
                this.configuration.get('loginSilentMode').subscribe((mode: string) => {
                    if ('iframe' === mode) {
                        const iframe_id: string = 'sso_check';
                        console.log('guest is active');
                        if (!document.getElementById(iframe_id)) {
                            let iframe = document.createElement('iframe');
                            iframe.style.display = 'none';
                            iframe.src =
                                window.location.origin +
                                '/edu-sharing/rest/authentication/v1/validateSSOSession';
                            iframe.id = iframe_id;
                            iframe.onload = (ev) => {
                                try {
                                    var y = iframe.contentDocument;
                                    var pre_info = y.getElementsByTagName('pre')[0].innerHTML;
                                    console.log('object', pre_info);
                                    var o = JSON.parse(pre_info);
                                    if (o.error && o.error === 'login_required') {
                                        console.log('login_required');
                                    } else {
                                        console.log('login exists');
                                        RestHelper.goToLogin(
                                            this.injector.get(Router),
                                            this.configuration,
                                        );
                                    }
                                } catch (error) {
                                    console.error('check session iframe fails:' + error);
                                }
                            };
                            document.body.appendChild(iframe);
                        }
                    }
                });

                /*this.authentication.validateSSOSession().subscribe(async (loginInfo) => {
                    console.log("got a login will reload page");
                    window.location.reload();
                }, error => {
                    console.error(error);
                })*/
            }
        });
    }

    private registerContrastMode(): void {
        const contrastModeClass = 'es-contrast-mode';
        this.accessibilityService.observe('contrastMode').subscribe((value) => {
            if (value) {
                document.body.classList.add(contrastModeClass);
            } else {
                document.body.classList.remove(contrastModeClass);
            }
        });
    }
}
