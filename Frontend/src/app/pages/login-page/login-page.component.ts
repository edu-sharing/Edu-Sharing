import { trigger } from '@angular/animations';
import { PlatformLocation } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { AfterViewInit, Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { delay, first, map, startWith, switchMap } from 'rxjs/operators';
import { BridgeService } from '../../services/bridge.service';
import {
    ConfigurationService,
    DialogButton,
    LoginResult,
    RestConnectorService,
    RestConstants,
    RestHelper,
} from '../../core-module/core.module';
import { Helper } from '../../core-module/rest/helper';
import { InputPasswordComponent } from '../../shared/components/input-password/input-password.component';
import { RouterHelper } from '../../util/router.helper';
import { Toast } from '../../services/toast';
import { OPEN_URL_MODE, TranslationsService, UIAnimation, UIConstants } from 'ngx-edu-sharing-ui';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { AuthenticationService, LoginInfo } from 'ngx-edu-sharing-api';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { Subject } from 'rxjs';
import { ThemeService } from '../../services/theme.service';

@Component({
    selector: 'es-login-page',
    templateUrl: 'login-page.component.html',
    styleUrls: ['login-page.component.scss'],
    animations: [trigger('dialog', UIAnimation.switchDialog(UIAnimation.ANIMATION_TIME_FAST))],
})
export class LoginPageComponent implements OnInit, OnDestroy, AfterViewInit {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    @ViewChild('loginForm') loginForm: ElementRef;
    @ViewChild('passwordInput') passwordInput: InputPasswordComponent;
    @ViewChild('usernameInput') usernameInput: ElementRef;

    buttons: DialogButton[];
    caption = 'LOGIN.TITLE';
    config: any = {};
    currentProvider: any;
    disabled = false;
    isSafeLogin = false;
    filteredProviders: any;
    isLoading = true;
    loginUrl: any;
    password = '';
    providerControl = new UntypedFormControl();
    showProviders = false;
    username = '';

    private next = '';
    private providers: any;
    private scope = '';
    private destroyed = new Subject<void>();

    constructor(
        private connector: RestConnectorService,
        private toast: Toast,
        private platformLocation: PlatformLocation,
        private router: Router,
        private http: HttpClient,
        private translations: TranslationsService,
        private configService: ConfigurationService,
        private route: ActivatedRoute,
        private bridge: BridgeService,
        private authentication: AuthenticationService,
        private themeService: ThemeService,
        private loadingScreen: LoadingScreenService,
        private mainNav: MainNavService,
    ) {
        // reset the theme in case user was in safe previously
        this.themeService.initWithDefaults();
        const loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed });
        this.isLoading = true;
        this.updateButtons();
        this.translations
            .waitForInit()
            .pipe(switchMap(() => this.configService.getAll()))
            .subscribe((data: any) => {
                this.config = data;
                if (!this.config.register) {
                    // default register mode: allow local registration if not disabled
                    this.config.register = { local: true };
                }
                if (this.bridge.getCordova().isRunningCordova()) {
                    this.router.navigate([UIConstants.ROUTER_PREFIX, 'app'], {
                        replaceUrl: true,
                    });
                    return;
                }
                this.updateButtons();
                this.username = this.configService.instant('defaultUsername', '');
                this.password = this.configService.instant('defaultPassword', '');
                this.route.queryParams.forEach((params: Params) => {
                    if (params.username) {
                        this.username = params.username;
                    }
                    this.next = params.next;
                    this.connector.onAllRequestsReady().subscribe(() => {
                        setTimeout(() => {
                            if (this.username && this.passwordInput) {
                                this.passwordInput.nativeInput.nativeElement.focus();
                            } else if (this.usernameInput) {
                                this.usernameInput.nativeElement.focus();
                            }
                        }, 100);
                    });
                    this.scope = params.scope;
                    if (!this.scope) {
                        this.scope = null;
                    }
                    this.connector.isLoggedIn().subscribe(async (data: LoginResult) => {
                        if (data.currentScope) {
                            // just to make sure there is no scope still set // NO: We need a valid session when login to scope!!!
                            try {
                                await this.connector.logout().toPromise();
                            } catch (e) {
                                console.warn(e);
                            }
                            data.statusCode = null;
                        } else if (data.currentScope === this.scope) {
                            if (
                                data.statusCode === RestConstants.STATUS_CODE_OK &&
                                params.local !== 'true'
                            ) {
                                this.goToNext(data);
                            }
                        }
                        // when there is a request to go into safe mode, first, the user needs to log in regularly
                        else if (data.statusCode !== RestConstants.STATUS_CODE_OK && this.scope) {
                            // RestHelper.goToLogin()
                        }
                        this.loginUrl = configService.instant('loginUrl');
                        const allowLocal = configService.instant('loginAllowLocal', false);
                        if (
                            params.local !== 'true' &&
                            !allowLocal &&
                            this.loginUrl &&
                            data.statusCode !== RestConstants.STATUS_CODE_OK
                        ) {
                            this.openLoginUrl();
                            return;
                        }
                        this.isLoading = false;
                        loadingTask.done();
                        if (configService.instant('loginProvidersUrl')) {
                            this.showProviders = true;
                            this.updateButtons();
                            // delay to make sure animation of card has finished
                            // otherwise, overlay gets aligned wrongly
                            const providers = await this.http
                                .get(configService.instant('loginProvidersUrl'))
                                .pipe(delay(UIAnimation.ANIMATION_TIME_NORMAL))
                                .toPromise();
                            this.processProviders(providers);
                        }
                    });
                    this.isSafeLogin = this.scope == RestConstants.SAFE_SCOPE;
                    if (this.scope === RestConstants.SAFE_SCOPE) {
                        this.connector.isLoggedIn(true).subscribe(
                            (data: LoginResult) => {
                                if (data.statusCode !== RestConstants.STATUS_CODE_OK) {
                                    RestHelper.goToLogin(this.router, this.configService);
                                } else {
                                    this.authentication
                                        .observeHasAccessToScope(RestConstants.SAFE_SCOPE)
                                        .pipe(first())
                                        .subscribe((hasAccess) => {
                                            if (hasAccess) {
                                                this.username = data.authorityName;
                                            } else {
                                                this.toast.error(null, 'LOGIN.NO_ACCESS');
                                                this.router.navigate([
                                                    UIConstants.ROUTER_PREFIX + 'workspace',
                                                ]);
                                                // window.history.back();
                                            }
                                        });
                                }
                            },
                            (error: any) => RestHelper.goToLogin(this.router, this.configService),
                        );
                    }

                    if (this.scope === RestConstants.SAFE_SCOPE) {
                        this.caption = 'LOGIN.TITLE_SAFE';
                    } else {
                        this.caption = 'LOGIN.TITLE';
                    }
                });
            });
    }

    ngAfterViewInit(): void {
        setTimeout(() => window.dispatchEvent(new Event('resize')), 100);
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    canRegister(): boolean {
        return (
            this.config.register && (this.config.register.local || this.config.register.registerUrl)
        );
    }

    checkConditions() {
        this.disabled = !this.username || this.currentProvider; // || !this.password;
        this.updateButtons();
    }

    currentProviderDisplay(provider: any) {
        return provider ? provider.name : '';
    }

    goToProvider() {
        if (!this.currentProvider) {
            this.toast.error(null, 'LOGIN.NO_PROVIDER_SELECTED');
        }
        let url = this.configService.instant('loginProviderTargetUrl');
        if (!url) {
            this.toast.error(null, 'No configuration for loginProviderTargetUrl found.');
            return;
        }
        const target =
            this.connector.getAbsoluteServerUrl() + this.configService.instant('loginUrl');
        url = url
            .replace(':target', encodeURIComponent(target))
            // remove invalid parameters for multiple universities using the same idp
            .replace(
                ':entity',
                encodeURIComponent(this.currentProvider.url.replace(/@_.*?_@/, '')),
            );
        // @TODO: Redirect to shibboleth provider
        UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.Current);
    }

    login() {
        this.isLoading = true;
        this.connector.login(this.username, this.password, this.scope).subscribe(
            (data) => {
                if (data.statusCode === RestConstants.STATUS_CODE_OK) {
                    this.goToNext(data);
                } else {
                    if (
                        data.statusCode === RestConstants.STATUS_CODE_PREVIOUS_SESSION_REQUIRED ||
                        data.statusCode === RestConstants.STATUS_CODE_PREVIOUS_USER_WRONG
                    ) {
                        this.toast.error(null, 'LOGIN.SAFE_PREVIOUS');
                    } else if (data.statusCode === RestConstants.STATUS_CODE_PASSWORD_EXPIRED) {
                        this.toast.error(
                            null,
                            'LOGIN.PASSWORD_EXPIRED' + (this.isSafeLogin ? '_SAFE' : ''),
                        );
                    } else if (data.statusCode === RestConstants.STATUS_CODE_PERSON_BLOCKED) {
                        this.toast.error(null, 'LOGIN.PERSON_BLOCKED');
                    } else {
                        this.toast.error(null, 'LOGIN.ERROR' + (this.isSafeLogin ? '_SAFE' : ''));
                    }
                    this.password = '';
                    this.isLoading = false;
                }
            },
            (error: any) => {
                this.toast.error(error);
                this.isLoading = false;
            },
        );
    }

    ngOnInit() {
        this.mainNav.setMainNavConfig({
            currentScope: 'login',
            title: 'SIDEBAR.LOGIN',
        });
    }

    openLoginUrl() {
        window.location.href = this.loginUrl;
    }

    register() {
        if (this.config.register.local) {
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'register']);
        } else {
            window.location.href = this.config.register.registerUrl;
        }
    }

    private filterProviders(filter: any = '') {
        const filtered = [];
        if (!this.providers) {
            return null;
        }
        // an object was detected, abort
        if (filter.name) {
            return this.providers;
        }
        this.currentProvider = null;
        for (const p of Helper.deepCopy(this.providers)) {
            p.providers = p.providers.filter(
                (p: any) =>
                    p.name.toLowerCase().includes(filter.toLowerCase()) ||
                    p.data?.toLowerCase().includes(filter.toLowerCase()),
            );
            if (p.providers.length) {
                filtered.push(p);
            }
        }
        return filtered;
    }

    private goToNext(data: LoginInfo) {
        if (this.next) {
            this.next = Helper.addGetParameter('fromLogin', 'true', this.next);
            RouterHelper.navigateToAbsoluteUrl(this.platformLocation, this.router, this.next);
        } else if (data.currentScope === RestConstants.SAFE_SCOPE) {
            this.router.navigate([UIConstants.ROUTER_PREFIX, 'workspace', 'safe']);
        } else {
            UIHelper.goToDefaultLocation(this.router, this.platformLocation, this.configService);
        }
    }

    private processProviders(providers: any) {
        const data: any = {};
        for (const provider of Object.keys(providers.wayf_idps)) {
            const object = providers.wayf_idps[provider];
            if (object) {
                object.url = provider;
                const type = object.type;
                if (!data[type]) {
                    data[type] = {
                        group: providers.wayf_categories[type],
                        providers: [],
                    };
                }
                data[type].providers.push(object);
            }
        }
        this.providers = [];
        for (const key of Object.keys(data)) {
            this.providers.push(data[key]);
        }

        // register observer for autocomplete
        this.filteredProviders = this.providerControl.valueChanges.pipe(
            startWith(''),
            map((value: string) => this.filterProviders(value)),
        );
    }

    private updateButtons(): any {
        this.buttons = [];
        if (this.showProviders) {
            return;
        }
        if (this.canRegister()) {
            this.buttons.push(
                new DialogButton('LOGIN.REGISTER_TEXT', { color: 'standard' }, () =>
                    this.register(),
                ),
            );
        }
        const login = new DialogButton('LOGIN.LOGIN', { color: 'primary' }, () => this.login());
        login.disabled = this.disabled;
        this.buttons.push(login);
    }
}
