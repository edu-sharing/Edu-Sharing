import { Component } from '@angular/core';

import { Toast } from '../../services/toast';
import { ActivatedRoute, Router } from '@angular/router';
import {
    ConfigurationService,
    DialogButton,
    OAuthResult,
    RestConnectorService,
    RestConstants,
    RestLocatorService,
} from '../../core-module/core.module';
import { OPEN_URL_MODE, TranslationsService, UIConstants } from 'ngx-edu-sharing-ui';
import { CordovaService } from '../../services/cordova.service';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { BridgeService } from '../../services/bridge.service';
import { PlatformLocation } from '@angular/common';

// possible states this UI component can be in
enum StateUI {
    SERVERLIST = 0,
    LOGIN = 1,
    SERVERURL = 2,
    NOINTERNET = 3,
}

@Component({
    selector: 'es-app-login-page',
    templateUrl: 'app-login-page.component.html',
    styleUrls: ['app-login-page.component.scss'],
})
// tslint:disable:no-console
export class AppLoginPageComponent {
    public isLoading = true;
    public disabled = true;

    username = '';
    password = '';
    private serverurl = 'https://';

    errorURL: string = null;

    servers: any;
    currentServer: any;
    private locationNext: string;
    config: any;
    buttons: DialogButton[];

    constructor(
        private toast: Toast,
        private router: Router,
        private route: ActivatedRoute,
        private translations: TranslationsService,
        private platformLocation: PlatformLocation,
        private cordova: CordovaService,
        private connector: RestConnectorService,
        private bridge: BridgeService,
        private configService: ConfigurationService,
        private locator: RestLocatorService,
    ) {
        console.info('startup app');
        this.isLoading = true;

        // WHEN RUNNING ON DESKTOP --> FORWARD TO BASIC LOGIN PAGE
        if (!this.cordova.isRunningCordova()) {
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'login']);
            return;
        }

        this.route.queryParams.subscribe((params) => {
            this.locationNext = params['next'];
        });

        // 1. Wait until Cordova is Ready
        this.cordova.subscribeServiceReady().subscribe(async () => {
            // app startup, cordova has valid data ?
            // -> go to default location (this will check oauth)
            if (await this.cordova.hasValidConfig()) {
                console.info('app config valid, continuing to default location');
                this.cordova.refreshOAuth().subscribe(() => {
                    this.goToDefaultLocation();
                });
                return;
            }

            // set the self set server url if available from persistence
            // for this value its no problem that result is async
            // init translation service
            this.init();
        });
    }
    private recoverPassword() {
        if (this.config.register.local) {
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'register', 'request']);
        } else {
            window.location.href = this.config.register.recoverUrl;
        }
    }
    buttonExitApp(): void {
        this.cordova.exitApp();
    }

    checkConditions(): void {
        this.disabled = !this.username; // || !this.password;
        this.updateButtons();
    }

    buttonLoginBack(): void {
        //window.history.back();
        //window.location.replace(this.cordova.getIndexPath()+"?reset=true");
        this.cordova.restartCordova();
        //(navigator as any).app.loadUrl(this.cordova.getIndexPath()+"?reset=true");
    }

    login() {
        /*
        // test camera
        this.cordova.getPhotoFromCamera(
        (win:any)=>{
        },
        (error:any, info:any)=>{
            console.dir(info);
        });
        if (1==1) return;
        */

        /*
        // test file download
        this.cordova.downloadContent("http://sample-videos.com/video/mp4/240/big_buck_bunny_240p_20mb.mp4", "test.mp4",(win:any)=>{
            alert("OK "+win);
            this.cordova.openContentNative(win,()=>{
                alert("OK Open");
            }, (text:string, error:any) => {
                alert("FAIL open");
            });
        }, (text:string,error:any) => {
            console.error(text, error);
            alert("FAIL: "+text);
        });
        if (1==1) return;
        */
        this.isLoading = true;
        // APP: oAuth Login
        this.cordova.loginOAuth(this.locator.endpointUrl, this.username, this.password).subscribe(
            (oauthTokens: OAuthResult) => {
                this.cordova.setPermanentStorage(
                    RestConstants.CORDOVA_STORAGE_OAUTHTOKENS,
                    JSON.stringify(oauthTokens),
                );
                // continue to within the app
                this.goToDefaultLocation();
            },
            (error) => {
                console.warn(error);
                this.isLoading = false;
                if (typeof error == 'string') {
                    this.toast.error(null, error);
                } else {
                    this.toast.error(null, 'LOGIN.ERROR');
                }
            },
        );
        /*
        this.cordova.setServerURL(this.currentServer.url+"rest/",true).subscribe(()=> {


        });
        */
    }

    private goToDefaultLocation() {
        if (this.locationNext) {
            console.info('location next', this.locationNext);
            window.location.replace(this.locationNext);
        } else {
            this.configService.getAll().subscribe(() => {
                UIHelper.goToDefaultLocation(
                    this.router,
                    this.platformLocation,
                    this.configService,
                    true,
                );
            });
        }
    }
    getServerIcon() {
        return 'assets/images/app-icon.svg';
    }
    private init() {
        this.translations.waitForInit().subscribe(() => {
            this.serverurl = this.locator.endpointUrl;
            this.configService.getAll().subscribe((config) => {
                this.config = config;
                if (!this.config.register) {
                    // default register mode: allow local registration if not disabled
                    this.config.register = { local: true };
                }
                this.isLoading = false;

                void this.handleCurrentState();
            });
        });
    }
    register() {
        if (this.config.register.local) {
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'register']);
        } else {
            UIHelper.openUrl(
                this.config.register.registerUrl,
                this.bridge,
                OPEN_URL_MODE.BlankSystemBrowser,
            );
        }
    }
    updateButtons() {
        const login = new DialogButton('LOGIN.LOGIN', { color: 'primary' }, () => this.login());
        login.disabled = this.disabled;
        if (this.config && (this.config.register.local || this.config.register.recoverUrl)) {
            const recover = new DialogButton('LOGIN.RECOVER_PASSWORD', { color: 'standard' }, () =>
                this.recoverPassword(),
            );
            this.buttons = [recover, login];
        } else {
            this.buttons = [login];
        }
    }

    private async handleCurrentState() {
        // an external login, e.g. via shibboleth, may occured. get oauth for the session, and store it

        // this will break shibboleth/sso!
        /*if (!(await this.cordova.hasValidConfig())) {
            console.info('empty app oauth status, show login');
            this.checkLoginUrl();
            return;
        }*/
        this.connector.isLoggedIn(true).subscribe(
            (data) => {
                console.info('app login status', data);
                if (data.statusCode === RestConstants.STATUS_CODE_OK) {
                    this.cordova
                        .loginOAuth(this.locator.endpointUrl, null, null, 'client_credentials')
                        .subscribe((oauthTokens: OAuthResult) => {
                            this.cordova.setPermanentStorage(
                                RestConstants.CORDOVA_STORAGE_OAUTHTOKENS,
                                JSON.stringify(oauthTokens),
                            );
                            // continue to within the app
                            this.goToDefaultLocation();
                        });
                } else {
                    this.checkLoginUrl();
                }
            },
            (error) => {
                this.checkLoginUrl();
            },
        );
    }

    private checkLoginUrl() {
        if (this.configService.instant('loginUrl')) {
            window.location.href = this.configService.instant('loginUrl');
        }
    }
}
