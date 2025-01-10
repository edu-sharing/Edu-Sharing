import {
    AfterViewInit,
    Component,
    DoCheck,
    HostListener,
    Injector,
    NgZone,
    OnInit,
    ViewChild,
} from '@angular/core';
import { MdsTestComponent } from '../common/test/mds-test/mds-test.component';
import { ApplyToLmsComponent } from '../common/ui/apply-to-lms/apply-to-lms.component';
import { NodeRenderComponent } from '../common/ui/node-render/node-render.component';
import { AccessibilityService, TranslationsService, UIConstants } from 'ngx-edu-sharing-ui';
import { AdminComponent } from '../modules/admin/admin.component';
import { CollectionNewComponent } from '../modules/collections/collection-new/collection-new.component';
import { CollectionsMainComponent } from '../modules/collections/collections.component';
import { FileUploadComponent } from '../modules/file-upload/file-upload.component';
import { LoginAppComponent } from '../modules/login-app/login-app.component';
import { LoginComponent } from '../modules/login/login.component';
import { WorkspaceManagementDialogsComponent } from '../modules/management-dialogs/management-dialogs.component';
import { MessagesComponent } from '../modules/messages/messages.component';
import { RecycleMainComponent } from '../modules/node-list/recycle/recycle.component';
import { TasksMainComponent } from '../modules/node-list/tasks/tasks.component';
import { OerComponent } from '../modules/oer/oer.component';
import { PermissionsRoutingComponent } from '../modules/permissions/permissions-routing.component';
import { PermissionsMainComponent } from '../modules/permissions/permissions.component';
import { ProfilesComponent } from '../modules/profiles/profiles.component';
import { RegisterComponent } from '../modules/register/register.component';
import { ServicesComponent } from '../modules/services/services.components';
import { ShareAppComponent } from '../modules/share-app/share-app.component';
import { SharingComponent } from '../modules/sharing/sharing.component';
import { StartupComponent } from '../modules/startup/startup.component';
import { StreamComponent } from '../modules/stream/stream.component';
import { WorkspaceMainComponent } from '../modules/workspace/workspace.component';
import { NavigationEnd, Router, Routes } from '@angular/router';
import { CookieInfoComponent } from '../common/ui/cookie-info/cookie-info.component';
import { BridgeService } from '../core-bridge-module/bridge.service';
import { extensionRoutes } from '../extension/extension-routes';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { LtiComponent } from '../modules/lti/lti.component';
import { printCurrentTaskInfo } from './track-change-detection';
import { environment } from '../../environments/environment';
import { LoadingScreenService } from '../main/loading-screen/loading-screen.service';
import { MainNavService } from '../main/navigation/main-nav.service';
import { ManagementDialogsService } from '../modules/management-dialogs/management-dialogs.service';
import { ThemeService } from '../common/services/theme.service';
import { LicenseAgreementService } from '../services/license-agreement.service';
import { DialogsNavigationGuard } from '../features/dialogs/dialogs-navigation.guard';
import { AuthenticationService } from 'ngx-edu-sharing-api';
import { ConfigurationService, RestHelper } from '../core-module/core.module';
import { ScrollPositionRestorationService } from '../services/scroll-position-restoration.service';

@Component({
    selector: 'es-router',
    templateUrl: 'router.component.html',
    providers: [],
})
export class RouterComponent implements OnInit, DoCheck, AfterViewInit {
    private static readonly CHECKS_PER_SECOND_WARNING_THRESHOLD = 0;
    private static readonly CONSECUTIVE_TRANSGRESSION_THRESHOLD = 10;
    static history = new BehaviorSubject<string[]>([]);

    public static isRedirectedFromLogin() {
        const history = RouterComponent.history.value;
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
    ) {
        this.injector.get(Router).events.subscribe((event) => {
            // if (event instanceof NavigationStart) {
            //     console.log('NavigationStart', event.url);
            // }
            if (event instanceof NavigationEnd) {
                RouterComponent.history.value.push(event.url);
                RouterComponent.history.next(RouterComponent.history.value);
            }
        });
        this.ngZone.runOutsideAngular(() => {
            // Do not trigger change detection with setInterval.
            this.checksMonitorInterval = window.setInterval(() => this.monitorChecks(), 1000);
        });
    }

    ngOnInit(): void {
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
        this.licenseAgreement.setup();
        this.scrollPositionRestoration.setup();
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
        if (this.numberOfChecks > RouterComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD) {
            this.consecutiveTransgression++;
            if (
                this.consecutiveTransgression >= RouterComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD
            ) {
                console.warn(
                    'Change detection triggered more than ' +
                        RouterComponent.CHECKS_PER_SECOND_WARNING_THRESHOLD +
                        ' times per second for the past ' +
                        RouterComponent.CONSECUTIVE_TRANSGRESSION_THRESHOLD +
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
        this.authentication.observeLoginInfo().subscribe((loginInfo) => {
            const router = this.injector.get(Router);
            if (
                !loginInfo.isValidLogin &&
                !(
                    router.url.startsWith('/' + UIConstants.ROUTER_PREFIX + 'login') ||
                    router.url.startsWith('/' + UIConstants.ROUTER_PREFIX + 'register') ||
                    router.url.startsWith('/' + UIConstants.ROUTER_PREFIX + 'error') ||
                    router.url.startsWith('/' + UIConstants.ROUTER_PREFIX + 'message') ||
                    router.url.startsWith('/' + 'shibboleth') ||
                    // public link sharing
                    router.url.startsWith('/' + UIConstants.ROUTER_PREFIX + 'sharing')
                )
            ) {
                RestHelper.goToLogin(router, this.configuration);
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

// RouterComponent.transformRoute
// this fails for aot because it can't call static functions
/*

 export var ROUTES_COMMON:any=[
 { path: '', component: NoRouteComponent },
 { path: 'rest-test',component: RestTestComponent},
 { path: 'render/:node', component: NodeRenderComponent},
 { path: 'render/:node/:version', component: NodeRenderComponent},
 { path: 'apply-to-lms/:node', component: ApplyToLmsComponent}
 ];
 .concat(ROUTES_SEARCH)
 .concat(ROUTES_WORKSPACE)
 .concat(ROUTES_RECYCLE)
 .concat(ROUTES_COLLECTIONS)
 .concat(ROUTES_LOGIN)
 .concat(ROUTES_PERMISSIONS)
 */

// Due to ahead of time, we need to create all routes manually.
const childRoutes: Routes = [
    // overrides and additional routes
    ...extensionRoutes,

    // global
    { path: '', component: StartupComponent },
    { path: UIConstants.ROUTER_PREFIX + 'app', component: LoginAppComponent },
    { path: UIConstants.ROUTER_PREFIX + 'app/share', component: ShareAppComponent },
    { path: UIConstants.ROUTER_PREFIX + 'sharing', component: SharingComponent },
    { path: UIConstants.ROUTER_PREFIX + 'test/mds', component: MdsTestComponent },
    { path: UIConstants.ROUTER_PREFIX + 'render/:node', component: NodeRenderComponent },
    { path: UIConstants.ROUTER_PREFIX + 'render/:node/:version', component: NodeRenderComponent },
    {
        path: UIConstants.ROUTER_PREFIX + 'apply-to-lms/:repo/:node',
        component: ApplyToLmsComponent,
    },
    // search
    {
        path: UIConstants.ROUTER_PREFIX + 'search',
        loadChildren: () =>
            import('../pages/search-page/search-page.module').then((m) => m.SearchPageModule),
    },
    // workspace
    { path: UIConstants.ROUTER_PREFIX + 'workspace', component: WorkspaceMainComponent },
    { path: UIConstants.ROUTER_PREFIX + 'workspace/:mode', component: WorkspaceMainComponent },
    // recycle/node component
    { path: UIConstants.ROUTER_PREFIX + 'recycle', component: RecycleMainComponent },
    { path: UIConstants.ROUTER_PREFIX + 'tasks', component: TasksMainComponent },
    // collections
    { path: UIConstants.ROUTER_PREFIX + 'collections', component: CollectionsMainComponent },
    {
        path: UIConstants.ROUTER_PREFIX + 'collections/collection/:mode/:id',
        component: CollectionNewComponent,
    },
    // login
    { path: UIConstants.ROUTER_PREFIX + 'login', component: LoginComponent },
    // register
    { path: UIConstants.ROUTER_PREFIX + 'register', component: RegisterComponent },
    { path: UIConstants.ROUTER_PREFIX + 'register/:status', component: RegisterComponent },
    { path: UIConstants.ROUTER_PREFIX + 'register/:status/:key', component: RegisterComponent },
    {
        path: UIConstants.ROUTER_PREFIX + 'register/:status/:key/:email',
        component: RegisterComponent,
    },
    // file upload
    { path: UIConstants.ROUTER_PREFIX + 'upload', component: FileUploadComponent },
    // admin
    { path: UIConstants.ROUTER_PREFIX + 'admin', component: AdminComponent },
    // permissions
    {
        path: UIConstants.ROUTER_PREFIX + 'permissions',
        component: PermissionsRoutingComponent,
        children: [{ path: '', component: PermissionsMainComponent }],
    },
    // oer
    { path: UIConstants.ROUTER_PREFIX + 'oer', component: OerComponent },
    // stream
    { path: UIConstants.ROUTER_PREFIX + 'stream', component: StreamComponent },
    // profiles
    { path: UIConstants.ROUTER_PREFIX + 'profiles/:authority', component: ProfilesComponent },

    // messages
    { path: UIConstants.ROUTER_PREFIX + 'messages/:message', component: MessagesComponent },
    { path: UIConstants.ROUTER_PREFIX + 'messages/:message/:text', component: MessagesComponent },
    // error (same as message)
    { path: UIConstants.ROUTER_PREFIX + 'error/:message', component: MessagesComponent },
    { path: UIConstants.ROUTER_PREFIX + 'error/:message/:text', component: MessagesComponent },

    // link-share
    { path: UIConstants.ROUTER_PREFIX + 'sharing', component: SharingComponent },
    // services
    { path: UIConstants.ROUTER_PREFIX + 'services', component: ServicesComponent },

    // embed
    {
        path: UIConstants.ROUTER_PREFIX + 'embed/:component',
        loadChildren: () => import('../common/ui/embed/embed.module').then((m) => m.EmbedModule),
    },

    { path: UIConstants.ROUTER_PREFIX + 'lti', component: LtiComponent },

    // wildcard 404
    { path: '**', component: MessagesComponent, data: { message: 404 } },
];

export const ROUTES: Routes = [
    // Add a `canDeactivate` guard to all routes, that closes any open dialogs before allowing
    // navigation.
    {
        path: '',
        canDeactivate: [DialogsNavigationGuard],
        children: childRoutes,
        runGuardsAndResolvers: 'paramsOrQueryParamsChange',
    },
];
