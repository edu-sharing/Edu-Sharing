import { Location } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Injectable, Injector, NgZone } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { AuthenticationService, LoginInfo } from 'ngx-edu-sharing-api';
import { AppService as AppServiceAbstract, DateHelper, UIConstants } from 'ngx-edu-sharing-ui';
import { BehaviorSubject, Observable, Observer } from 'rxjs';
import { first, map, share } from 'rxjs/operators';
import { RestLocatorService } from '../core-module/core.module';
import { OAuthResult } from '../core-module/rest/data-object';
import { RestConstants } from '../core-module/rest/rest-constants';
import { FrameEventsService } from '../core-module/rest/services/frame-events.service';

declare var cordova: any;

export enum OnBackBehaviour {
    default,
    closeApp,
}

/**
 * All services that touch the mobile app or cordova plugins are available here.
 */
@Injectable({ providedIn: 'root' })
// tslint:disable:no-console
export class CordovaService extends AppServiceAbstract {
    private onBackBehaviour = OnBackBehaviour.default;
    platform: 'ios' | 'android';
    private lastValidLogin: number;

    oauthRequestData: string;
    oauthRequest$ = new Observable<OAuthResult>((observer: Observer<OAuthResult>) => {
        const url = this.injector.get(RestLocatorService).endpointUrl + '../oauth2/token';
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded', Accept: '*/*' };
        const options = { headers, withCredentials: false };

        this.http.post<OAuthResult>(url, this.oauthRequestData, options).subscribe(
            async (oauth: OAuthResult) => {
                if (oauth == null) {
                    observer.error('INVALID_CREDENTIALS');
                    observer.complete();
                    return;
                }

                // set local expire ts on token
                this.oauth = oauth;
                // force a renew to consum the token
                this.injector.get(AuthenticationService).loginToken(oauth.access_token);
                observer.next(this.oauth);
                observer.complete();
            },
            (error: any) => {
                if (error.status == 401) {
                    observer.error('LOGIN.ERROR');
                    observer.complete();
                    return;
                }

                observer.error(error);
                observer.complete();
            },
        );
    }).pipe(share());

    get oauth() {
        return this.oauth$.value;
    }

    set oauth(oauth: OAuthResult) {
        if (oauth) {
            oauth.expires_ts = Date.now() + oauth.expires_in * 1000;
            this.setPermanentStorage(
                RestConstants.CORDOVA_STORAGE_OAUTHTOKENS,
                JSON.stringify(oauth),
            );
            this.oauth$.next(oauth);
        } else {
            this.oauth$.next(null);
        }
    }

    /**
     * CONSTRUCTOR
     */
    constructor(
        private router: Router,
        private ngZone: NgZone,
        private http: HttpClient,
        private location: Location,
        private injector: Injector,
        private authenticationService: AuthenticationService,
        private events: FrameEventsService,
    ) {
        super();
        const userAgent = navigator.userAgent;
        if (userAgent?.includes('ionic / edu-sharing-app')) {
            if (userAgent.includes('ios')) {
                this.platform = 'ios';
            }
            if (userAgent.includes('android')) {
                this.platform = 'android';
            }
            const splitted = userAgent.split('/');
            const version =
                splitted
                    .filter((s) => {
                        const versionRegExp = new RegExp('\\d\\.\\d(\\.\\d)?');
                        if (versionRegExp.test(s.trim())) {
                            return true;
                        }
                        return false;
                    })?.[0]
                    .trim() || '0.0.0';

            const script = document.createElement('script');
            script.type = 'text/javascript';
            script.src =
                'https://app-registry.edu-sharing.com/js/' +
                version +
                '/' +
                this.platform +
                '/cordova.js';
            document.getElementsByTagName('head')[0].appendChild(script);
            console.info('ionic user agent, add cordova.js to header', this.platform, version);
        }
        this.initialHref = window.location.href;

        // CORDOVA EVENT: Pause (App is put into Background)
        const whenDeviceGoesBackground = () => {
            // rember time when app went into background
            this.appGoneBackgroundTS = Date.now();
        };
        // CORDOVA EVENT: Resume (App comes back from Background)
        const whenDeviceGoesForeground = () => {
            /*
             * ignore pauses under 1 minute that appear when going into
             * a plugin (camera) or you get a permission request from the OS
             */
            if (this.appGoneBackgroundTS == null) return;
            if (Date.now() - this.appGoneBackgroundTS < 60 * 1000) return;

            // OK - real pasuse detected

            // call listener if set
            if (this.deviceResumeCallback != null) this.deviceResumeCallback();
        };

        if (this.isRunningCordova()) {
            this.registerSessionListener();
            // deviceready may not work, because cordova is already loaded, so try to set it ready after some time
            const checkInterval = setInterval(() => {
                if ((window as any).plugins) {
                    console.info('cordova: plugins object found, setting device ready');
                    this.deviceIsReady = true;
                    clearInterval(checkInterval);
                }
            }, 100);
        }
        // adding listener for cordova events
        document.addEventListener(
            'deviceready',
            () => {
                this.deviceIsReady = true;
            },
            false,
        );
        document.addEventListener('pause', whenDeviceGoesBackground, false);
        document.addEventListener('resume', whenDeviceGoesForeground, false);

        // just for simulation on forced cordova mode
        if (this.forceCordovaMode && !this.isReallyRunningCordova()) {
            setTimeout(this.whenDeviceIsReady, 500 + Math.random() * 1000);
        } else if (this.isReallyRunningCordova()) {
            this.deviceReadyLoop(1);
        }
    }

    /**********************************************************
     * Sync with iOS Share Screen thru NSUserdefaults Plugin
     **********************************************************
     * Server and oAuth Token need to be synced with the iOS share
     * screen thru the NSUserdefaults storage ny using the following plugin:
     * https://github.com/rootzoll/cordova-plugin-nsuserdefaults-for-app-groups.git
     */

    // keys used to exchange data with ios share extension
    public static IOSSHARE_ACCESS = 'access_token';
    public static IOSSHARE_REFRESH = 'refresh_token';
    public static IOSSHARE_EXPIRES = 'expires_in';
    public static IOSSHARE_SERVER = 'eduserver';

    /**********************************************************
     * OAUTH Server Communication
     **********************************************************
     * The REST-Services depend on Configuration Service that already need config from a fixed server.
     * To seperate the app login from those dependencies .. the cordova service provides all the HTTP
     * tools to select a server, make oAuth logins/management, get JSession .. from there on the
     * regular REST-Services can handle all the communication.
     */

    // errors results that can result when testing a server url
    public static TEST_ERROR_NOTFOUND = 'APINOTFOUND';
    public static TEST_ERROR_NOINTERNET = 'NOINTERNET';
    public static TEST_ERROR_INCORRECTVERSION = 'INCORRECTVERSION';
    public static TEST_ERROR_UNKNOWN = 'UNKOWN';

    // success results that can result when testing a server url
    public static TEST_WARNING_NOHTTPS = 'NOHTTPS';
    public static TEST_TESTSKIPPED = 'TESTSKIPPED';
    public static TEST_OK = 'OK';

    // change this during development for testing true, but false is default
    private forceCordovaMode = false;

    private deviceIsReady = false;

    private deviceResumeCallback: Function = null;

    private observerShareContent: Observer<any> = null;

    private appGoneBackgroundTS: number = null;

    private oauth$ = new BehaviorSubject<OAuthResult | null | undefined>(undefined);
    private serviceIsReady = false;

    private lastIntent: any;

    initialHref: string;

    /**
     * Called when the current status is logged out
     * Cordova needs to refresh tokens
     */
    private reiniting = false;

    /**
     * get the last android/ios intent
     */
    public getLastIntent() {
        return this.lastIntent;
    }
    private deviceReadyLoop(counter: number): void {
        setTimeout(() => {
            if (this.deviceIsReady) {
                this.whenDeviceIsReady();
            } else {
                this.deviceReadyLoop(++counter);
            }
        }, 250);
    }

    // CORDOVA EVENT: Device is Ready (on App StartUp)
    private whenDeviceIsReady = () => {
        // window.open = cordova.InAppBrowser.open;

        // load basic data from storage
        this.loadStorage();

        // --> navigation issues exist anyway, need to check that later
        document.addEventListener('backbutton', () => this.onBackKeyDown(), false);
        // when new share contet - go to share screen
        const shareInterval = setInterval(async () => {
            if (await this.hasValidConfig()) {
                clearInterval(shareInterval);
                this.onNewShareContent().subscribe(
                    async (data: any) => {
                        await this.ngZone.run(() =>
                            this.router.navigate([UIConstants.ROUTER_PREFIX, 'app', 'share'], {
                                queryParams: data,
                            }),
                        );
                    },
                    (error) => {},
                );
            }
        }, 1000);

        // hide the splashscreen (if still showing)
        setTimeout(() => {
            try {
                (navigator as any).splashscreen.hide();
            } catch (e) {
                console.error(
                    'CordovaService: FAILED to call splashscreen.hide() - is plugin cordova-plugin-splashscreen installed?',
                );
            }
        }, 1500);

        // flag that device is ready
        this.deviceIsReady = true;

        // check if to register on share events
        if (this.observerShareContent != null) this.registerOnShareContent();
    };
    /**********************************************************
     * Plugin: WebIntent (for Android)
     **********************************************************
     * To receive share content from other apps.
     * https://github.com/cordova-misc/cordova-webintent
     */

    private onNewShareContent(): Observable<any> {
        return new Observable<any>((observer: Observer<any>) => {
            this.observerShareContent = observer;

            // if device is already ready -> register now, otherwise wait
            if (this.deviceIsReady) this.registerOnShareContent();
        });
    }
    public getFileAsBlob(file: string, mimetype: string) {
        return new Observable<Blob>((observer: Observer<Blob>) => {
            (window as any).resolveLocalFileSystemURL(
                file,
                (data: any) => {
                    data.file((data2: File) => {
                        observer.next(data2);
                        observer.complete();
                    });
                },
                (error: any) => {
                    observer.error(error);
                    observer.complete();
                },
            );
        });
    }
    private registerOnShareContent(): void {
        console.info('registerOnShareContent', this.platform, this.isAndroid());
        if (this.isAndroid()) {
            const handleIntentBase = (intent: any) => {
                if (intent && intent.extras) {
                    let uri = intent.extras['android.intent.extra.TEXT'];
                    if (uri) {
                        this.lastIntent = intent;
                        this.observerShareContent.next({ uri, mimetype: intent.type });
                        // clear handler to just fire it on first app opening
                        return;
                    }
                    uri = intent.extras['android.intent.extra.STREAM'];
                    // it's a file
                    if (uri) {
                        this.lastIntent = intent;
                        this.observerShareContent.next({
                            uri,
                            file: intent.clipItems?.length ? intent.clipItems[0].file : null,
                            mimetype: intent.type,
                        });
                    }
                }
            };
            // only run once. Will loop otherwise if no auth is found and intent was send
            const handleIntent = (intent: any) => {
                // Do things
                console.info('cordova: android new intent', intent);
                if (intent && intent.action == 'android.intent.action.VIEW') {
                    const hit = '/edu-sharing';
                    const target = intent.data.substr(0, intent.data.indexOf(hit));
                    const current = window.location.href.substr(
                        0,
                        window.location.href.indexOf(hit),
                    );
                    if (target == current) {
                        window.location.href = intent.data;
                    } else {
                        this.resetAndGoToServerlist('url=' + intent.data);
                    }
                } else {
                    handleIntentBase(intent);
                }
                (window as any).plugins.intent.getCordovaIntent(null);
            };
            (window as any).plugins.intent.getCordovaIntent(handleIntentBase);
            (window as any).plugins.intent.setNewIntentHandler(handleIntent);
            /*
           (window as any).plugins.webintent.onNewIntent((uri:string)=> {
               (window as any).plugins.webintent.getExtra((window as any).plugins.webintent.EXTRA_TEXT,
                   (extra:string)=> {
                       this.observerShareContent.next(extra);
                   },(error:any)=>{
                       console.error(error);
                       (window as any).plugins.webintent.getExtra((window as any).plugins.webintent.EXTRA_STREAM,
                       (extra:string)=> {
                           this.observerShareContent.next(extra);
                       },(error:any)=>{console.error(error);});
               });

           });*/
        }
        if (this.isIOS()) {
            // Initialize the plugin
            cordova.openwith.init(
                () => {},
                () => {
                    console.warn('failed to init openWith ios');
                },
            );

            // Define your file handler
            cordova.openwith.addHandler((intent: any) => {
                const item = intent.items[0];
                // console.log('  image base64 string: ', item.base64)

                // some optional additional info
                item.stream = item.base64; // convert it so it's like on android
                // alert(item.type+" : "+item.name+" : "+item.path+" : "+item.uri);
                item.uri = DateHelper.getDateForNewFile() + '.jpg';

                this.lastIntent = item;
                const data = {
                    uri: item.uri,
                    mimetype: item.type,
                    file: item.name,
                    text: item.text,
                };
                this.observerShareContent.next(data);
            });
        }
    }

    /*
   private resolveFileUri(URI:string, callbackResult:Function) : void {

      try {

        if ((typeof URI !== "undefined") && (URI !== null)) {

          if (URI.indexOf("file://") === 0) {

            // lets resolve to native path
            (window as any).FilePath.resolveNativePath(URI, (localFileUri: string) => {
              callbackResult(localFileUri)
            }, (e: any) => {
              alert("FAILED to resolve ContentURL(" + URI + ")");
            });

          }

          if (URI.indexOf("content://") === 0) {

            // try to resolve CONTENT-URL: https://developer.android.com/guide/topics/providers/content-providers.html
            (window as any).FilePath.resolveNativePath(URI, (win: string) => {
              this.resolveFileUri(win, callbackResult);

            }, (error: any) => {
              alert("FAILED to resolve ContentURL(" + URI + ")");
            });
          }

          else {
            alert("ImageShare ERROR: fileUri unkown " + URI);
          }

        } else {
          alert("ERROR: fileUri undefined or NULL");
        }

      } catch (e) {
        console.error("EXCEPTION resolveFileUri", e);
        callbackResult(URI);
      }

    }
    */

    /**********************************************************
     * BASIC CORDOVA
     **********************************************************
     * Tools needed to setup, for info or to react on cordova events
     */

    /**
     * Use to check if angular is running in a cordova environment.
     */
    isRunningCordova(): boolean {
        if (this.forceCordovaMode) return true;
        return this.isReallyRunningCordova();
    }

    // just for internal use
    private isReallyRunningCordova(): boolean {
        return typeof (window as any).cordova != 'undefined' || this.platform != null;
    }

    /**
     * Check if app is running on a iOS device.
     * https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-device/index.html
     */
    isIOS(): boolean {
        try {
            const device: any = (window as any).device;
            return device.platform == 'iOS';
        } catch (e) {
            return false;
        }
    }

    /**
     * Check if app is running on a Android device.
     * https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-device/index.html
     */
    isAndroid(): boolean {
        try {
            const device: any = (window as any).device;
            return (
                device.platform.toLowerCase() === 'android' ||
                device.platform.toLowerCase() === 'amazon-fireos'
            );
        } catch (e) {
            return true;
        }
    }

    /**
     * Use to check if cordova plugins are ready to use.
     * If angular is running in a cordova environment - make sure that device is ready befor using plugin tools.
     */
    isDeviceReady(): boolean {
        return this.deviceIsReady;
    }

    /*
     * Set a callback function to be called then device is ready for cordova service action.
     */
    subscribeServiceReady(): Observable<void> {
        return new Observable<void>((observer: Observer<void>) => {
            if (this.serviceIsReady) {
                // cordova already signaled that it is ready - call on the spot
                observer.next(null);
                observer.complete();
            } else {
                const waitLoop = () => {
                    if (this.serviceIsReady) {
                        observer.next(null);
                        observer.complete();
                    } else {
                        setTimeout(waitLoop, 200);
                    }
                };
                waitLoop();
            }
        });
    }

    /**
     * Set a callback function to be called then device resumes from pause.
     * > 1 Minute in Background
     * @param callback callback function (with void parameter)
     */
    setDeviceResumeCallback(callback: Function) {
        this.deviceResumeCallback = callback;
    }

    /**
     * Closes the App when running as real app.
     */
    exitApp() {
        try {
            (navigator as any).app.exitApp();
        } catch (e) {}
    }

    restartCordova(parameters = ''): void {
        this.setPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS, null);
        if (parameters) parameters = '&' + parameters;
        console.log(navigator.userAgent, navigator.userAgent.includes('ionic / edu-sharing-app'));
        console.log((window as any).device);
        if (navigator.userAgent.includes('ionic / edu-sharing-app')) {
            // go to ionic local server
            if (this.isAndroid() && navigator.userAgent.includes('3.0.1')) {
                window.location.replace('http://localhost/?reset=true' + parameters);
            } else {
                window.location.replace('http://localhost:54361/?reset=true' + parameters);
            }
        } else {
            window.location.replace(
                'http://app-registry.edu-sharing.com/ng/?reset=true' + parameters,
            );
        }
        /*
    try {
      (navigator as any).splashscreen.show();
    } catch (e) {}
    document.location.href = this.initialHref;
    */
    }

    /**********************************************************
     * APP PRESISTENCE
     **********************************************************
     * Uses HTML5 storage as a base, but also backups thru the following plugin-in ...
     * https://www.npmjs.com/package/cordova-plugin-nativestorage
     * .. just in case that for example on iOS the HTML5 local storage gets ereased:
     * https://stackoverflow.com/questions/7750857/how-permanent-is-local-storage-on-android-and-ios
     * Dont use plugin on Android to avoid starting the app with a permission request.
     */

    /**
     * load permanent key/value
     * @param key the key to request value
     * @param callback function callback with value as parameter - null if not available
     */
    getPermanentStorage(key: string, callback: Function): void {
        // callback - to sync with ios sharescreen
        const callbackWrapper: Function = (val: any) => {
            /*
      if (this.isReallyRunningCordova() & this.isIOS()) {
        if (key==CordovaService.STORAGE_OAUTHTOKENS) {

              // see what was the last ios share oauth expire date
              this.iosShareScreenLoadValue(CordovaService.IOSSHARE_EXPIRES, (shareExpire:any) => {

                // if there is no value continue with local
                if (shareExpire==null) {
                  callback(val);
                } else {

                  // check if expire is newer in share then local
                  try {
                    let oAuthLocal:any = null;
                    if (val!=null) oAuthLocal = JSON.parse(val);
                    if ((oAuthLocal==null) || (+shareExpire>oAuthLocal.expires_ts)) {

                      // OK share has more up to date oauth
                      // --> update local from share
                      oAuthLocal.expires_ts = +shareExpire;
                      this.iosShareScreenLoadValue(CordovaService.IOSSHARE_ACCESS,(shareAccess:string)=>{
                        oAuthLocal.access_token = shareAccess;
                        this.iosShareScreenLoadValue(CordovaService.IOSSHARE_REFRESH, (shareRefresh:string)=>{
                          oAuthLocal.refresh_token = shareRefresh;
                          let oAuthLocalJSON = JSON.stringify(oAuthLocal);
                          this.setPermanentStorage(CordovaService.STORAGE_OAUTHTOKENS, oAuthLocalJSON);
                          callback(oAuthLocalJSON);
                        });
                      });

                    } else {
                      callback(val);
                    }

                  } catch (e) {
                    console.error("EXCEPTION on sync with ios share extension", e);
                    callback(val);
                  }

                }

              });

        } else {
          callback(val);
        }
      } else {*/
            callback(val);
            // }
        };

        // get value from HTML5 local storage
        const value = window.localStorage.getItem(key);

        // just iun case - check if backup is available from nativestorage plugin
        if (
            (typeof value == 'undefined' || value == null) &&
            this.isIOS() &&
            (window as any).NativeStorage
        ) {
            try {
                // window['NativeStorage'].getItem("reference_to_value",<success-callback>, <error-callback>);
                (window as any).NativeStorage.getItem(
                    key,
                    (valueNative: any) => {
                        // WIN
                        if (typeof valueNative == 'undefined') valueNative = null;
                        callbackWrapper(valueNative);
                    },
                    (error: any) => {
                        // FAIL (also when key not available)
                        callbackWrapper(null);
                    },
                );
            } catch (e) {
                console.error('Plugin Fail', e);
                callbackWrapper(null);
            }
        } else {
            callbackWrapper(value);
        }
    }

    /**
     * save permament key/value
     * @param key
     * @param value
     */
    setPermanentStorage(key: string, value: string) {
        // set on HTML5 storage
        window.localStorage.setItem(key, value);

        // as backup set on native storage
        if (this.isIOS()) {
            try {
                (window as any).NativeStorage.setItem(
                    key,
                    value,
                    () => {
                        // WIN - thats OK
                    },
                    (error: any) => {
                        // FAIL
                        console.error('Fail NativeStorage.setItem', error);
                    },
                );
            } catch (e) {
                console.error('Plugin Fail', e);
            }
        }

        // if a oauth relevant key - sync with sharescreen
        if (key == RestConstants.CORDOVA_STORAGE_OAUTHTOKENS && this.isIOS()) {
            try {
                const oauthData: any = JSON.parse(value);
                this.iosShareScreenStoreValue(
                    CordovaService.IOSSHARE_ACCESS,
                    oauthData.access_token,
                );
                this.iosShareScreenStoreValue(
                    CordovaService.IOSSHARE_REFRESH,
                    oauthData.refresh_token,
                );
                this.iosShareScreenStoreValue(
                    CordovaService.IOSSHARE_EXPIRES,
                    oauthData.expires_ts,
                );
            } catch (e) {
                console.error('EXCEPTION on storing oauth data for ios sharescreen ', e);
            }
        }

        // if server address - sync with sharescreen
        if (key == RestConstants.CORDOVA_STORAGE_SERVER_OWN && this.isIOS()) {
            try {
                this.iosShareScreenStoreValue(CordovaService.IOSSHARE_SERVER, value);
            } catch (e) {
                console.error('EXCEPTION on storing server data for ios sharescreen ', e);
            }
        }
    }

    /**
     * erase all permanent data
     */
    clearPermanentStorage(): void {
        // clear HTML5 local storage
        window.localStorage.clear();

        // clear native storage
        if (this.isIOS()) {
            try {
                (window as any).NativeStorage.clear(
                    () => {},
                    (error: any) => {
                        // FAIL
                        console.error('Fail NativeStorage.clear', error);
                    },
                );
            } catch (e) {
                console.error('Plugin Fail', e);
            }
        }

        // clear oauth sync with ios share screen
        try {
            this.iosShareScreenStoreValue(CordovaService.IOSSHARE_ACCESS, '');
            this.iosShareScreenStoreValue(CordovaService.IOSSHARE_REFRESH, '');
            this.iosShareScreenStoreValue(
                CordovaService.IOSSHARE_EXPIRES,
                new Date().getTime() + '',
            );
        } catch (e) {
            console.error('EXCEPTION on storing oauth data for ios sharescreen ', e);
        }
    }

    /**
     * after init, load the stored info from the cordova storage and save it as class members for access of other services
     */
    loadStorage() {
        this.getPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS, (data: string) => {
            this.oauth = data != null ? JSON.parse(data) : null;
            this.serviceIsReady = true;
        });
    }

    clearAllCookies(): void {
        const cookies = document.cookie.split(';');
        for (let i = 0; i < cookies.length; i++) {
            const cookie = cookies[i];
            const eqPos = cookie.indexOf('=');
            const name = eqPos > -1 ? cookie.substr(0, eqPos) : cookie;
            document.cookie = name + '=;expires=Thu, 01 Jan 1970 00:00:00 GMT';
        }
    }

    /**
     * Use internally every time a value gets loaded from local storage
     * that also needs to be in sync with the iOS share screen.
     * @param key
     * @param callback returns with string value or null
     */
    private iosShareScreenLoadValue(key: string, callback: Function): void {
        try {
            if (this.isReallyRunningCordova() && this.isIOS()) {
                (window as any).AppGroupsUserDefaults.save(
                    {
                        suite: 'group.edusharing',
                        key,
                    },
                    function (value: any) {
                        if (typeof value == 'undefined') value = null;
                        callback(value);
                    },
                    function (fail: any) {
                        console.error(
                            'PLUGIN FAIL info.protonet.appgroupsuserdefaults LOAD: ',
                            fail,
                        );
                        callback(null);
                    },
                );
            }
        } catch (e) {
            console.error('PLUGIN EXCEPTION info.protonet.appgroupsuserdefaults LOAD: ', e);
        }
    }

    /**
     * Use internally every time a value gets stored to local storage
     * that also needs to be in sync with the iOS share screen.
     * @param key
     * @param value
     */
    private iosShareScreenStoreValue(key: string, value: string): void {
        try {
            if (this.isReallyRunningCordova() && this.isIOS()) {
                (window as any).AppGroupsUserDefaults.save(
                    {
                        suite: 'group.edusharing',
                        key,
                        value,
                    },
                    function (win: any) {},
                    function (fail: any) {
                        console.error(
                            'PLUGIN FAIL info.protonet.appgroupsuserdefaults SAVE: ',
                            fail,
                        );
                    },
                );
            }
        } catch (e) {
            console.error('PLUGIN EXCEPTION info.protonet.appgroupsuserdefaults SAVE: ', e);
        }
    }

    /**********************************************************
     * Permissions Plugin
     **********************************************************
     * Use to wrapp the use of plugin functions, that need certain permissions.
     * https://github.com/NeoLSN/cordova-plugin-android-permissions
     */

    // for 'permission' user values like in this side
    // https://developer.android.com/reference/android/Manifest.permission.html
    private makeSurePermission(
        permission: string,
        successCallback: Function,
        errorCallback: Function,
    ): void {
        if (this.isIOS()) {
            successCallback();
            return;
        }

        try {
            const permissions = (window as any).cordova.plugins.permissions;
            const permissionString: string = permissions[permission] as string;

            // console.log("permissions",permissions);
            // console.log("permissionString",permissionString);

            permissions.checkPermission(
                permissionString,
                (status: any) => {
                    if (status.hasPermission) {
                        // permission is available
                        successCallback();
                    } else {
                        // try to get permission by request
                        permissions.requestPermission(
                            permissionString,
                            (response: any) => {
                                if (response.hasPermission) {
                                    // permission is granted
                                    successCallback();
                                } else {
                                    // permission denied
                                    errorCallback(
                                        'FAIL-PERMISSION-1',
                                        'permission not granted by user or not part of config.xml',
                                    );
                                }
                            },
                            (error: any) => {
                                errorCallback('FAIL-PERMISSION-2', error);
                            },
                        );
                    }
                },
                (error: any) => {
                    errorCallback('FAIL-PERMISSION-3', error);
                },
            );
        } catch (error) {
            console.error(error);
            errorCallback('FAIL-EXCEPTION', error);
        }
    }

    /**********************************************************
     * Camera Plugin
     **********************************************************
     * https://cordova.apache.org/docs/en/latest/reference/cordova-plugin-camera
     */

    getPhotoFromCamera(
        successCallback: (data: string) => void,
        errorCallback: Function,
        options: any = null,
    ): void {
        try {
            // Default Options
            if (options == null)
                options = {
                    correctOrientation: true,
                    destinationType: 0, // Camera.DestinationType.DATA_URL
                    sourceType: 1, // Camera.PictureSourceType.CAMERA
                    encodingType: 0, // Camera.EncodingType.JPEG
                    quality: 70,
                };

            // Camera PlugIn
            // https://github.com/apache/cordova-plugin-camera
            const runPlugIn: Function = () => {
                (navigator as any).camera.getPicture(
                    (result: string) => {
                        successCallback(result);
                    },
                    (error: any) => {
                        errorCallback('FAIL-PLUGIN', error);
                    },
                    options,
                );
            };

            // Permissions PlugIn
            // https://github.com/NeoLSN/cordova-plugin-android-permissions
            // TODO: check that the app just asks for photo permission
            this.makeSurePermission('CAMERA', runPlugIn, errorCallback);
        } catch (error) {}
    }

    public uploadLocalContent(uri: string, endpointUrl: string, headers: any): Observable<any> {
        return new Observable<string>((observer: Observer<any>) => {
            const fileTransfer: any = new (window as any).FileTransfer();
            fileTransfer.upload(
                uri,
                endpointUrl,
                (result: any) => {
                    observer.next(result);
                    observer.complete();
                },
                (error: any) => {
                    observer.error(error);
                    observer.complete();
                },
                { headers },
            );
        });
    }

    /**********************************************************
     * Download PlugIn
     **********************************************************
     * Make it possible to download content to the mobile device.
     * Make sure that the URL given will authenticate the user.
     * iOS: https://github.com/apache/cordova-plugin-file-transfer
     * Android: https://github.com/wymsee/cordova-HTTP
     */

    downloadContent(
        downloadURL: string,
        fileName: string,
        winCallback: Function = null,
        failCallback: Function = null,
    ): void {
        let status = 0;
        let resultPath = '';
        try {
            this.makeSurePermission(
                'WRITE_EXTERNAL_STORAGE',
                (win: any) => {
                    console.log('perm win', win);

                    // add oauth token if not alreafy in URL
                    if (downloadURL.indexOf('accessToken=') < 0 && this.oauth !== null) {
                        if (downloadURL.indexOf('?') < 0) {
                            downloadURL = downloadURL + '?accessToken=' + this.oauth.access_token;
                        } else {
                            downloadURL = downloadURL + '&accessToken=' + this.oauth.access_token;
                        }
                    }

                    if (this.isIOS()) {
                        // iOS: following redirects works automatically - so go direct
                        this.startContentDownload(
                            downloadURL,
                            fileName,
                            (filePath: string) => {
                                status = 1;
                                resultPath = filePath;
                            },
                            () => (status = -1),
                        );
                    } else {
                        // Android: resolve redirect (because plugin download can not follow redirect)
                        /*console.log("resolving redirects for downloadContent URL ANDROID: " + downloadURL);
           (window as any).CordovaHttpPlugin.head(downloadURL, {}, {}, (response: any) => {
             */
                        this.startContentDownload(
                            downloadURL,
                            fileName,
                            (filePath: string) => {
                                resultPath = filePath;
                                status = 1;
                            },
                            () => (status = -1),
                        );
                        /*}, (response: any) => {
             if (response.status == 302) {
               let redirectURL = decodeURIComponent(response.headers.Location);
               this.startContentDownload(redirectURL, fileName,()=>status=1, ()=>status=-1);
             } else {
               status=-1;
             }
           });*/
                    }
                },
                (error: any) => {
                    status = -1;
                },
            );
        } catch (e) {
            console.warn(e);
            status = -1;
        }
        const interval = setInterval(() => {
            if (status == 0) return;
            clearInterval(interval);
            if (status == 1 && winCallback) {
                if (this.isAndroid()) {
                    // suggest user to open the file
                    (window as any).plugins.intent.showOpenWith(
                        resultPath,
                        () => {},
                        () => {},
                    );
                }
                winCallback();
            }
            if (status == -1 && failCallback) failCallback();
        }, 100);
    }

    private startContentDownload(
        downloadURL: string,
        fileName: string,
        winCallback: Function,
        failCallback: Function,
    ): void {
        // set path to store on device
        let targetPath = (window as any).cordova.file.externalRootDirectory + 'Download/';
        if (this.isIOS()) targetPath = (window as any).cordova.file.documentsDirectory;
        const localPath = targetPath + fileName;
        const filePath = encodeURI(localPath);
        // iOS
        const fileTransfer: any = new (window as any).FileTransfer();
        fileTransfer.download(
            downloadURL,
            filePath,
            (result: any) => {
                console.info('content download done', result);
                winCallback(localPath);
            },
            (err: any) => {
                failCallback('FAIL startContentDownload', err);
            },
            true,
            {},
        );
    }

    openInAppBrowser(url: string) {
        let params: string;
        if (this.isAndroid()) {
            params = 'location=no,zoom=no';
        } else if (this.isIOS()) {
            params =
                'toolbar=yes,hideurlbar=yes,hidenavigationbuttons=yes,closebuttoncolor=#ffffff,closebuttoncaption=' +
                this.injector.get(TranslateService).instant('CANCEL');
        }
        const win: any = cordova.InAppBrowser.open(url, '_blank', params);
        win.addEventListener('loadstop', () => {
            // register iframe handling
            win.executeScript({
                code: `
                var cordovaIframeList=document.getElementsByTagName('iframe');
                function cordovaReceiveMessage(event){
                    for(var i=0;i<cordovaIframeList.length;i++){
                        if (cordovaIframeList[i].contentWindow === event.source) {
                            window.opener.postMessage(event.data);
                            return;
                        }
                    }
                    for(var i=0;i<cordovaIframeList.length;i++){
                        cordovaIframeList[i].contentWindow.postMessage(event.detail,'*');
                    }
                }
                window.addEventListener("message", cordovaReceiveMessage, false);
           `,
            });
            win.postMessage = (data: any) => {
                // Redirect post messages to new window
                win.executeScript(
                    {
                        code:
                            `
                        var event = new CustomEvent('message',{detail:` +
                            JSON.stringify(data) +
                            `});
                        window.dispatchEvent(event);`,
                    },
                    null,
                );
            };
            this.events.addWindow(win);
            const loop = setInterval(() => {
                // Execute JavaScript to fetch message chain
                win.executeScript(
                    {
                        code: `
                        if(!window.messages){
                            window.messages=[];
                        }
                        window.opener={
                            postMessage:function(event,scope){
                                window.messages.push(event);
                            }
                        };
                        var msg=window.messages;
                        window.messages=[];
                        JSON.stringify(msg);`,
                    },
                    (values: string[]) => {
                        const events = JSON.parse(values[0]);
                        for (const e of events) {
                            const event = { source: win, data: e };
                            this.events.onEvent(event);
                            if (e.event == FrameEventsService.EVENT_CLOSE) {
                                clearInterval(loop);
                            }
                            if (e.event == FrameEventsService.EVENT_CORDOVA_CAMERA) {
                                this.getPhotoFromCamera(
                                    (data: any) => {
                                        this.events.broadcastEvent(
                                            FrameEventsService.EVENT_CORDOVA_CAMERA_RESPONSE,
                                            data,
                                        );
                                    },
                                    () => {
                                        this.events.broadcastEvent(
                                            FrameEventsService.EVENT_CORDOVA_CAMERA_RESPONSE,
                                            null,
                                        );
                                    },
                                );
                            }
                        }
                    },
                );
            }, 100);
        });
        return win;
    }
    openBrowser(url: string) {
        window.open(url, '_system');
    }
    /**********************************************************
     * FileViewer PlugIn
     **********************************************************
     * To open content native on the app - e.g. after download
     * use this plugin
     * https://github.com/SpiderOak/FileViewerPlugin
     */

    openContentNative(
        filePath: string,
        successCallback: Function = null,
        failCallback: Function = null,
    ): void {
        try {
            (window as any).FileViewerPlugin.view(
                {
                    action: (window as any).FileViewerPlugin.ACTION_VIEW,
                    url: filePath,
                },
                () => {
                    // WIN
                    if (successCallback) successCallback();
                },
                (error: any) => {
                    // FAIL
                    if (failCallback) failCallback('FAIL on openContentNative', error);
                },
            );
        } catch (e) {
            if (failCallback) failCallback('EXCEPTION on openContentNative', e);
        }
    }

    // oAuth login that is used when running as mobile app
    public loginOAuth(
        endpointUrl: string,
        username: string = '',
        password: string = '',
        grantType: 'password' | 'client_credentials' = 'password',
    ): Observable<OAuthResult> {
        const url = endpointUrl + '../oauth2/token';
        const headers = { 'Content-Type': 'application/x-www-form-urlencoded', Accept: '*/*' };
        const options = { headers, withCredentials: true };

        let data =
            'client_id=eduApp&client_secret=secret&grant_type=' + encodeURIComponent(grantType);
        if (grantType === 'password') {
            data +=
                '&username=' +
                encodeURIComponent(username) +
                '&password=' +
                encodeURIComponent(password);
        } else if (grantType === 'client_credentials') {
            // nothing is needed, session will be sent automatically
        }
        this.oauthRequestData = data;
        return this.oauthRequest$;
    }
    public reinitStatus(
        endpointUrl = this.injector.get(RestLocatorService).endpointUrl,
        goToLogin = true,
        loginNext = window.location.href,
    ): Observable<void> {
        return new Observable<void>((observer: Observer<void>) => {
            console.info('cordova: reinit', this.reiniting, this.oauth$.value, goToLogin);

            if (this.reiniting) {
                const interval = setInterval(() => {
                    if (!this.reiniting) {
                        clearInterval(interval);
                        observer.next(null);
                        observer.complete();
                    }
                }, 50);
                return;
            }
            // FIXME: this will always be false.
            if (!this.oauth$) {
                if (goToLogin) {
                    this.goToLogin(loginNext);
                }
                observer.error(null);
                observer.complete();
                return;
            }
            this.reiniting = true;
            this.refreshOAuth(endpointUrl).subscribe(
                async (oauth) => {
                    console.info('cordova: oauth OK');
                    this.reiniting = false;
                    const auth = this.injector.get(AuthenticationService);
                    auth.loginToken(oauth.access_token);
                    auth.forceLoginInfoRefresh();
                    observer.next(null);
                    observer.complete();
                },
                (error: any) => {
                    console.warn(error);
                    console.warn('cordova: invalid oauth, go back to server selection');
                    this.reiniting = false;
                    this.resetAndGoToServerlist();
                    observer.error(null);
                    observer.complete();
                },
            );
        });
    }

    private resetAndGoToServerlist(parameters = '') {
        this.setPermanentStorage(RestConstants.CORDOVA_STORAGE_OAUTHTOKENS, null);
        this.clearAllCookies();
        this.restartCordova(parameters);
    }

    // oAuth refresh tokens
    refreshOAuth(
        endpointUrl = this.injector.get(RestLocatorService).endpointUrl,
    ): Observable<OAuthResult> {
        this.oauthRequestData =
            'grant_type=refresh_token&client_id=eduApp&client_secret=secret' +
            '&refresh_token=' +
            encodeURIComponent(this.oauth.refresh_token);
        return this.oauthRequest$;
    }

    /**
     * If the user has a set of oAuth tokens (after login or when starting the app again)
     * then use this method to get a cookie/session and subscribe on refreshes on oAuth tokens.
     * When the Observable an error mit "INVALID" - oAuth tokens are outdated and new login is needed.
     */
    /*
  public initOAuthSession(oauth: OAuthResult): Observable<OAuthResult> {

      return new Observable<OAuthResult>((observer: Observer<OAuthResult>) => {

        let localErrorHandling = (error: any, tag: string = "") => {
          if ((typeof error != "string") && (error.status == 401)) {
            // oauth tokens are invalid
            observer.error("INVALID");
            observer.complete();
          } else {
            // on all other errors (server, internet, etc)
            observer.error(error);
            observer.complete();
          }
        };
        if ((Date.now() + 60000) > oauth.expires_ts || true) {
            // oAuth needs refresh first
            this.refreshOAuth(oauth).subscribe(
                (win) => {

                    // now oauth is fresh - continue with init session
                    this.oauth = win;
                    observer.next(this.oauth);
                    observer.complete();

                },
                (error) => {
                    localErrorHandling(error, "(on planned refresh)");
                }
            );
        }
      });

    }
    */

    hasValidConfig(): Promise<Boolean> {
        return this.oauth$
            .pipe(
                first((f) => f !== undefined),
                map((f) => f !== null),
            )
            .toPromise();
    }

    getLanguage() {
        return new Promise<string>((resolve) => {
            resolve(navigator.language.split('-')[0]);
            /*try {
          (navigator as any).globalization.getPreferredLanguage(
            (lang:any)=>{
              // WIN
              let code = (lang.value as string).substr(0,2);
              observer.next(code);
              observer.complete();
          },(error:any)=>{
              // ERROR - go with default
              observer.next("de");
              observer.complete();
          });
        } catch(e) {
          observer.next("de");
          observer.complete();
        }
        */
        });
    }
    setOnBackBehaviour(behaviour: OnBackBehaviour) {
        this.onBackBehaviour = behaviour;
    }

    private onBackKeyDown() {
        const eventDown = new KeyboardEvent('keydown', {
            key: 'Escape',
            view: window,
            bubbles: true,
            cancelable: true,
        });
        const eventUp = new KeyboardEvent('keyup', {
            key: 'Escape',
            view: window,
            bubbles: true,
            cancelable: true,
        });
        const down = !window.document.dispatchEvent(eventDown);
        const up = !window.document.dispatchEvent(eventUp);
        if (down || up) {
        } // if(window.history.length>2) {
        // (navigator as any).app.backHistory();
        else if (this.onBackBehaviour === OnBackBehaviour.closeApp) {
            (navigator as any).app.exitApp();
        } else {
            this.location.back();
        }
        /*}
        else{
            (navigator as any).app.exitApp();
        }*/
    }
    getIndexPath() {
        return cordova.file.applicationDirectory + 'www/';
    }

    private goToLogin(next: string) {
        console.info('navigating to app login', next);
        this.router.navigate([UIConstants.ROUTER_PREFIX, 'app'], {
            replaceUrl: true,
            queryParams: { next },
        });
    }

    private registerSessionListener() {
        this.authenticationService
            .observeLoginInfo()
            .subscribe((info) => this.handleLoginState(info));
        this.ngZone.runOutsideAngular(() => {
            setInterval(async () => {
                if (!this.lastValidLogin) {
                    return;
                }
                const info = await this.authenticationService
                    .observeLoginInfo()
                    .pipe(first())
                    .toPromise();
                const timeDiff = (Date.now() - this.lastValidLogin) / 1000;
                if (timeDiff > info.sessionTimeout / 10) {
                    console.info(
                        'Last login diff is old, will refetch auth for app',
                        timeDiff,
                        info.sessionTimeout,
                    );
                    await this.handleAppReAuthentication();
                }
            }, 30000);
        });
    }
    async handleLoginState(login: LoginInfo) {
        if (login.isValidLogin) {
            this.lastValidLogin = Date.now();
            return;
        }
        /*if(this.activeComponent instanceof LoginAppComponent) {
            console.log('handle state canceled, in login process');
            return;
        }*/
        // handle re-login from cordova app
        await this.handleAppReAuthentication();
    }

    async handleAppReAuthentication(reload = false) {
        const cordova = this.injector.get(CordovaService);
        if (cordova.isRunningCordova()) {
            if (await cordova.hasValidConfig()) {
                console.info('oauth present');
                await this.injector
                    .get(AuthenticationService)
                    .loginToken((await cordova.refreshOAuth().toPromise()).access_token)
                    .toPromise();
                if (reload) {
                    console.info('login done, reloading page', window.location.href);
                    window.location.reload();
                }
            } else {
                // this also navigates to the login if required
                console.info('Invalid/Broken oauth cordova config, forcing init');
                cordova.reinitStatus();
            }
        }
    }

    isRunningApp(): boolean {
        return this.isRunningCordova();
    }
}
