import { trigger } from '@angular/animations';
import {
    AfterViewInit,
    Component,
    ElementRef,
    HostBinding,
    HostListener,
    NgZone,
    OnDestroy,
    OnInit,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    About,
    AboutService,
    AuthenticationService,
    CurrentUserInfo,
    User,
    UserService,
} from 'ngx-edu-sharing-api';
import {
    AppContainerService,
    OPEN_URL_MODE,
    OptionGroup,
    OptionItem,
    UIAnimation,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { Observable, ReplaySubject, Subject } from 'rxjs';
import { delay, filter, map, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import {
    ConfigurationService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    TemporaryStorageService,
    UIService,
} from '../../../core-module/core.module';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { Closable } from '../../../features/dialogs/card-dialog/card-dialog-config';
import { CardDialogRef } from '../../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { BridgeService } from '../../../services/bridge.service';
import { LicenseAgreementService } from '../../../services/license-agreement.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { RocketChatService } from '../../rocketchat/rocket-chat.service';
import { MainMenuEntriesService } from '../main-menu-entries.service';
import { MainNavConfig, MainNavService } from '../main-nav.service';
import { SearchFieldService } from '../search-field/search-field.service';
import { TopBarComponent } from '../top-bar/top-bar.component';
import { ImprintPrivacyService } from '../../../shared/components/imprint-privacy-footer/imprint-privacy-service';

/**
 * The main nav (top bar + menus)
 */
@Component({
    selector: 'es-main-nav',
    templateUrl: 'main-nav.component.html',
    styleUrls: ['main-nav.component.scss'],
    providers: [MainMenuEntriesService],
    animations: [
        trigger('overlay', UIAnimation.openOverlay()),
        trigger('overlayBottom', UIAnimation.openOverlayBottom()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
        trigger('fade', UIAnimation.fade()),
    ],
    // changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainNavComponent implements OnInit, AfterViewInit, OnDestroy {
    // FIXME: This component contains a lot of business logic, that does not need to be exposed to
    // the rest of the application, but should live in a service. Refactoring into an internal
    // main-nav service would be a good solution.

    private static readonly ID_ATTRIBUTE_NAME = 'data-banner-id';

    @ViewChild(TopBarComponent) topBar: TopBarComponent;
    @ViewChild('tabNav') tabNav: ElementRef;

    private shouldAlwaysHide = this.storage.get(TemporaryStorageService.OPTION_HIDE_MAINNAV, false);
    /**
     * Whether the query param `mainnav=false` was set on the initial request.
     *
     * We save the value here, so we don't loose it on route changes.
     */
    private hiddenByQueryParam: boolean;

    @HostBinding('class.main-nav-visible') visible = !this.shouldAlwaysHide;
    autoLogoutTimeout$: Observable<string>;
    config: any = {};
    nodeStoreIsOpen = false;
    nodeStoreDialogRef: CardDialogRef<void, void> | null = null;
    canEditProfile: boolean;
    userMenuOptions: OptionItem[];
    tutorialElement: ElementRef;
    globalProgress = false;
    showEditProfile: boolean;
    showProfile: boolean;
    showUser = false;
    mainMenuStyle: 'sidebar' | 'dropdown' = 'sidebar';
    currentUser: User;
    canOpen: boolean;
    mainNavConfig: MainNavConfig;
    searchFieldEnabled: boolean;

    private readonly initDone$ = new ReplaySubject<void>();
    private readonly destroyed$ = new Subject<void>();
    private editUrl: string;
    private lastScroll = -1;
    private elementsTopY = 0;
    private elementsBottomY = 0;
    private fixScrollElements = false;
    private about: About;
    private queryParams: Params;

    constructor(
        private appContainer: AppContainerService,
        public iam: RestIamService,
        public connector: RestConnectorService,
        private bridge: BridgeService,
        private configService: ConfigurationService,
        private aboutService: AboutService,
        private uiService: UIService,
        private mainNavService: MainNavService,
        private storage: TemporaryStorageService,
        private router: Router,
        private route: ActivatedRoute,
        private nodeHelper: NodeHelperService,
        private authentication: AuthenticationService,
        private user: UserService,
        private ngZone: NgZone,
        // private changeDetectorRef: ChangeDetectorRef,
        private rocketChat: RocketChatService,
        private dialogs: DialogsService,
        private licenseAgreement: LicenseAgreementService,
        private searchField: SearchFieldService,
        private imprintPrivacy: ImprintPrivacyService,
    ) {}

    ngOnInit(): void {
        this.init();
        this.registerMainNavConfig();
        this.registerSearchField();
        this.registerCurrentUser();
        this.registerAutoLogoutDialog();
        this.registerAutoLogoutTimeout();
        this.registerHandleScroll();
        this.showLicenseAgreement();
    }

    ngAfterViewInit() {
        this.refreshBanner();
    }

    ngOnDestroy(): void {
        this.mainNavService.registerMainNav(null);
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    @HostListener('window:resize')
    onResize(event: any) {
        this.updateUserOptions();
    }

    private init(): void {
        this.mainNavService.registerMainNav(this);
        this.setMenuStyle();

        this.connector.setRoute(this.route, this.router).subscribe(() => {
            this.aboutService.getAbout().subscribe((about) => {
                this.about = about;
                this.initDone$.next();
                this.initDone$.complete();
            });
        });
    }

    private registerMainNavConfig() {
        const mainNavConfig$ = this.mainNavService.observeMainNavConfig().pipe(
            // Update `this.mainNavConfig` as soon as possible
            tap((config) => (this.mainNavConfig = config)),
        );
        rxjs.combineLatest([
            mainNavConfig$,
            this.user.observeCurrentUserInfo(),
            this.route.queryParams,
            this.initDone$,
        ])
            .pipe(takeUntil(this.destroyed$), delay(0))
            .subscribe(([mainNavConfig, userInfo, queryParams]) => {
                this.updateConfig(mainNavConfig, userInfo, queryParams);
                // this.changeDetectorRef.detectChanges();
            });
    }

    private registerSearchField() {
        this.searchField
            .observeEnabled()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((searchFieldEnabled) => (this.searchFieldEnabled = searchFieldEnabled));
    }

    private updateConfig(
        mainNavConfig: MainNavConfig,
        userInfo: CurrentUserInfo,
        queryParams: Params,
    ): void {
        this.visible = this.getIsVisible(mainNavConfig, queryParams);
        if (this.visible) {
            // Unset override.
            document.documentElement.style.setProperty('--mainnavHeight', null);
            document.documentElement.style.setProperty('--mainnavCurrentHeight', null);
        } else {
            // Override relevant css variables.
            document.documentElement.style.setProperty('--mainnavHeight', '0');
            document.documentElement.style.setProperty('--mainnavCurrentHeight', '0');
        }
        this.canOpen = mainNavConfig.canOpen;
        if (!userInfo.loginInfo.isValidLogin) {
            this.canOpen = userInfo.loginInfo.isGuest;
            this.checkConfig();
            return;
        }
        this.queryParams = queryParams;
        if (queryParams.noNavigation === 'true') {
            this.canOpen = false;
        }
        if (queryParams.connector) {
            this.topBar.createMenu?.showCreateConnector({
                connector: this.topBar.createMenu?.connectorList?.filter(
                    (c) => c.id === queryParams.connector,
                )[0],
            });
        }
        if (queryParams.nodeStore === 'true') {
            this.openNodeStore();
        } else {
            this.closeNodeStore();
        }
        this.showUser = mainNavConfig.currentScope !== 'login' && mainNavConfig.showUser;
        this.checkConfig();
        this.canEditProfile = userInfo.user.editProfile;
    }

    private async openNodeStore(): Promise<void> {
        if (this.nodeStoreIsOpen) {
            return;
        }
        this.nodeStoreIsOpen = true;
        this.nodeStoreDialogRef = await this.dialogs.openNodeStoreDialog();
        this.nodeStoreDialogRef.afterClosed().subscribe(() => {
            this.nodeStoreIsOpen = false;
            this.nodeStoreDialogRef = null;
            // Remove the query parameter only if it wasn't already removed by navigation (and the
            // dialog closed because of that).
            this.route.queryParams
                .pipe(
                    take(1),
                    filter(({ nodeStore }) => nodeStore === 'true'),
                )
                .subscribe(() => this.setNodeStore(false));
        });
    }

    private closeNodeStore(): void {
        if (this.nodeStoreDialogRef) {
            this.nodeStoreDialogRef.close();
        }
    }

    private getIsVisible(mainNavConfig: MainNavConfig, queryParams: Params): boolean {
        this.hiddenByQueryParam ??= queryParams.mainnav === 'false';
        if (this.shouldAlwaysHide || !this.mainNavConfig.show) {
            return false;
        } else if (
            this.hiddenByQueryParam &&
            ['login', 'search', 'collections'].includes(mainNavConfig.currentScope)
        ) {
            return false;
        } else {
            return true;
        }
    }

    private registerHandleScroll(): void {
        this.appContainer.registerScrollEvents(
            (event) => this.handleScroll(event),
            this.destroyed$,
            { events: ['scroll', 'touchmove'] }, // Not needed?
        );
    }

    private async handleScroll(event: Event) {
        if (this.storage.get(TemporaryStorageService.OPTION_DISABLE_SCROLL_LAYOUT, false)) {
            return;
        }
        if (event != null) {
            this.handleScrollHide();
        }
    }

    setNodeStore(value: boolean) {
        UIHelper.changeQueryParameter(this.router, this.route, 'nodeStore', value || null);
    }

    openProfileDialog() {
        this.showProfile = true;
    }

    openProfile() {
        this.router.navigate([UIConstants.ROUTER_PREFIX + 'profiles', RestConstants.ME]);
    }

    async refreshBanner(): Promise<void> {
        await new Promise((resolve) => resolve(void 0));
        await this.handleScroll(null);
    }

    showHelp(url: string) {
        UIHelper.openUrl(url, this.bridge, OPEN_URL_MODE.BlankSystemBrowser);
    }

    startTutorial() {
        this.user
            .observeCurrentUserInfo()
            .pipe(
                filter(({ user }) => user != null),
                filter(
                    ({ user, loginInfo }) =>
                        loginInfo.statusCode === RestConstants.STATUS_CODE_OK &&
                        user?.editProfile &&
                        this.configService.instant('editProfile', false),
                ),
                take(1),
                switchMap(() => this.uiService.waitForComponent(this, 'topBar')),
                switchMap(() => this.uiService.waitForComponent(this.topBar, 'userRef')),
            )
            .subscribe((userRef) => (this.tutorialElement = userRef));
    }

    setFixMobileElements(fix: boolean) {
        this.fixScrollElements = fix;
        this.handleScrollHide();
    }

    isSafe() {
        return (
            this.connector.getCurrentLogin() &&
            this.connector.getCurrentLogin().currentScope === RestConstants.SAFE_SCOPE
        );
    }

    showLicenses() {
        void this.dialogs.openThirdPartyLicensesDialog();
    }

    showChat() {
        return this.rocketChat._data;
    }

    getChatCount() {
        return this.rocketChat.unread;
    }

    openChat() {
        this.rocketChat.opened = true;
        this.rocketChat.unread = 0;
    }

    isCreateAllowed() {
        // @TODO: May Check for more constrains
        return (
            this.mainNavConfig.create?.allowed === true &&
            !this.connector.getCurrentLogin()?.isGuest &&
            this.queryParams?.reurlCreate !== 'false'
        );
    }

    logout() {
        this.globalProgress = true;
        this.uiService.handleLogout().subscribe(() => this.finishLogout());
    }

    private login(reurl = false) {
        RestHelper.goToLogin(
            this.router,
            this.configService,
            '',
            reurl ? window.location.href : '',
        );
    }

    private checkConfig() {
        this.configService.getAll().subscribe((data: any) => {
            this.config = data;
            this.editUrl = data.editProfileUrl;
            this.showEditProfile = data.editProfile;
            this.updateUserOptions();
        });
    }

    private setMenuStyle() {
        this.configService.get('mainnav.mainMenuStyle').subscribe({
            next: (mainMenuStyle?: string) => {
                switch (mainMenuStyle) {
                    case 'sidebar':
                    case 'dropdown':
                        this.mainMenuStyle = mainMenuStyle;
                        break;
                    case undefined:
                    case null:
                        break;
                    default:
                        console.error(
                            `Unsupported value for config mainMenuStyle: ${mainMenuStyle}`,
                        );
                }
            },
        });
    }

    private finishLogout() {
        if (this.config.logout && this.config.logout.next) {
            window.location.href = this.config.logout.next;
        } else {
            this.login(false);
        }
        this.globalProgress = false;
    }

    private showLicenseAgreement() {
        this.licenseAgreement.waitForAgreementCleared().subscribe(() => {
            this.startTutorial();
        });
    }

    private updateUserOptions() {
        this.userMenuOptions = [];
        if (
            this.connector.getCurrentLogin()?.statusCode === RestConstants.STATUS_CODE_OK &&
            this.about.plugins?.filter((s) => s.id === 'kafka-notification-plugin').length > 0
        ) {
            /*
            this.userMenuOptions.push(
                new OptionItem('NOTIFICATION.MENU', 'notifications', async () => {
                    await this.dialogs.openNotificationDialog();
                }),
            );
             */
        }
        if (
            !this.connector.getCurrentLogin()?.isGuest &&
            !this.connector.getCurrentLogin()?.currentScope
        ) {
            this.userMenuOptions.push(
                new OptionItem('EDIT_ACCOUNT', 'assignment_ind', () => this.openProfile()),
            );
            if (
                this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_SIGNUP_GROUP)
            ) {
                this.userMenuOptions.push(
                    new OptionItem('SIGNUP_GROUP.TITLE', 'group_add', () => {
                        void this.dialogs.openJoinGroupDialog();
                    }),
                );
            }
        }
        if (this.connector.getCurrentLogin()?.isGuest) {
            if (this.config.loginOptions) {
                for (const login of this.config.loginOptions) {
                    this.userMenuOptions.push(
                        new OptionItem(login.name, '', () => (window.location.href = login.url)),
                    );
                }
            } else {
                this.userMenuOptions.push(
                    new OptionItem('SIDEBAR.LOGIN', 'person', () => this.login(true)),
                );
            }
        }
        /*if (
            this._currentScope === 'workspace' ||
            this._currentScope === 'search' ||
            this._currentScope === 'stream' ||
            this._currentScope === 'collections'
        ) {*/
        const boomarkOption = new OptionItem('SEARCH.NODE_STORE.TITLE', 'bookmark_border', () =>
            this.setNodeStore(true),
        );
        this.userMenuOptions.push(boomarkOption);
        // }
        const accessibilityOptions = new OptionItem(
            'OPTIONS.ACCESSIBILITY',
            'accessibility',
            () => {
                void this.dialogs.openAccessibilityDialog();
            },
        );
        this.userMenuOptions.push(accessibilityOptions);
        for (const option of this.getConfigMenuHelpOptions()) {
            this.userMenuOptions.push(option);
        }
        const infoGroup = new OptionGroup('info', 20);
        if (this.config.imprintUrl) {
            const option = new OptionItem('IMPRINT', 'info_outline', () =>
                this.imprintPrivacy.openImprint(),
            );
            option.group = infoGroup;
            option.mediaQueryType = UIConstants.MEDIA_QUERY_MAX_WIDTH;
            option.mediaQueryValue = UIConstants.MOBILE_TAB_SWITCH_WIDTH;
            this.userMenuOptions.push(option);
        }
        if (this.config.privacyInformationUrl) {
            const option = new OptionItem('PRIVACY_INFORMATION', 'verified_user', () =>
                this.imprintPrivacy.openPrivacy(),
            );
            option.group = infoGroup;
            option.mediaQueryType = UIConstants.MEDIA_QUERY_MAX_WIDTH;
            option.mediaQueryValue = UIConstants.MOBILE_TAB_SWITCH_WIDTH;
            this.userMenuOptions.push(option);
        }
        const option = new OptionItem('LICENSE_INFORMATION', 'lightbulb_outline', () =>
            this.showLicenses(),
        );
        option.group = infoGroup;
        if (this.mainMenuStyle === 'sidebar') {
            option.mediaQueryType = UIConstants.MEDIA_QUERY_MAX_WIDTH;
            option.mediaQueryValue = UIConstants.MOBILE_TAB_SWITCH_WIDTH;
        }
        this.userMenuOptions.push(option);

        if (!this.connector.getCurrentLogin()?.isGuest) {
            this.userMenuOptions.push(new OptionItem('LOGOUT', 'undo', () => this.logout()));
        }
        this.applyUserMenuOverrides(this.userMenuOptions);
    }

    private applyUserMenuOverrides(options: OptionItem[]): void {
        this.configService
            .get('userMenuOverrides')
            .subscribe((overrides) =>
                this.nodeHelper.applyCustomNodeOptions(overrides, null, null, options),
            );
    }

    private getConfigMenuHelpOptions() {
        if (!this.config.helpMenuOptions) {
            // console.warn('config does not contain helpMenuOptions, will not display any options');
            return [];
        }
        const versionParts = this.about.version.repository.split('.');
        const version = versionParts[0] + '.' + versionParts[1];
        const group = new OptionGroup('help', 10);
        return this.config.helpMenuOptions.map(
            (entry: { key: string; icon: string; url: string }) => {
                const option = new OptionItem(entry.key, entry.icon, () =>
                    window.open(entry.url.replace(':version', version)),
                );
                option.group = group;
                return option;
            },
        );
    }

    /**
     * Method to dynamically hide objects when scrolling on mobile
     * Add css class mobile-move-top or mobile-move-bottom for specific items
     */
    private handleScrollHide() {
        if (this.tabNav == null || this.tabNav.nativeElement == null) {
            return;
        }
        const scrollContainer = this.appContainer.getScrollContainer();
        const scrollContainerOffset = this.appContainer.hasScrollContainer()
            ? scrollContainer.getBoundingClientRect().top
            : 0;
        const scrollY = this.appContainer.hasScrollContainer()
            ? scrollContainer.scrollTop
            : // Take the scroll position inside a viewport that was zoomed in using pinch-to-zoom into
              // account. This allows us to scroll bottom elements out of view, but is not really needed
              // for top elements. Using for both as long as no problems come up.
              (window as any).visualViewport?.pageTop ?? window.scrollY;
        if (this.lastScroll === -1) {
            this.lastScroll = scrollY;
            return;
        }
        const elementsTop: any = document.getElementsByClassName('mobile-move-top');
        const elementsBottom: any = document.getElementsByClassName('mobile-move-bottom');
        let top = -1;
        let bottom = -1;
        for (let i = 0; i < elementsTop.length; i++) {
            const rect = elementsTop.item(i).getBoundingClientRect();
            if (bottom === -1 || bottom < rect.bottom - scrollContainerOffset) {
                bottom = rect.bottom - scrollContainerOffset;
            }
        }
        for (let i = 0; i < elementsBottom.length; i++) {
            const rect = elementsBottom.item(i).getBoundingClientRect();
            if (top === -1 || top > rect.top - scrollContainerOffset) {
                top = rect.top - scrollContainerOffset;
            }
        }
        let diffTop = scrollY - this.lastScroll;
        let diffBottom = scrollY - this.lastScroll;
        if (diffTop < 0) {
            diffTop *= 2;
        }
        if (diffBottom < 0) {
            diffBottom *= 2;
        }

        // Don't move top elements any further up when they already lie above the screen.
        if (diffTop > 0 && bottom < 0) {
            diffTop = 0;
        }
        // Don't move bottom elements any further down when they already lie below the screen.
        if (diffBottom > 0 && top > scrollContainer.clientHeight) {
            diffBottom = 0;
        }
        // Don't move bottom elements any further up when the page is zoomed in on mobile.
        if (diffBottom < 0 && (window as any).visualViewport?.scale > 1) {
            diffBottom = 0;
        }
        this.elementsTopY += diffTop;
        this.elementsTopY = Math.max(0, this.elementsTopY);
        this.elementsBottomY += diffBottom;
        this.elementsBottomY = Math.max(0, this.elementsBottomY);
        // For ios elastic scroll
        if (
            scrollContainer.scrollTop < 0 ||
            this.fixScrollElements ||
            !UIService.evaluateMediaQuery(
                UIConstants.MEDIA_QUERY_MAX_WIDTH,
                UIConstants.MOBILE_TAB_SWITCH_WIDTH,
            )
        ) {
            this.elementsTopY = 0;
            this.elementsBottomY = 0;
        }
        for (let i = 0; i < elementsTop.length; i++) {
            elementsTop.item(i).style.position = 'relative';
            elementsTop.item(i).style.top = -this.elementsTopY + 'px';
        }
        for (let i = 0; i < elementsBottom.length; i++) {
            elementsBottom.item(i).style.position = 'relative';
            elementsBottom.item(i).style.top = this.elementsBottomY + 'px';
        }
        this.lastScroll = scrollY;
    }

    private showTimeout() {
        return (
            !this.bridge.isRunningCordova() &&
            (this.isSafe() ||
                this.configService.instant('sessionExpiredDialog', {
                    show: true,
                }).show)
        );
    }

    private getTimeoutString(timeUntilLogout: number): string {
        const time = Math.ceil(timeUntilLogout / 1000);
        const min = Math.floor(time / 60);
        const sec = time % 60;
        if (time >= 0) {
            return this.formatTimeout(min, 2) + ':' + this.formatTimeout(sec, 2);
        } else {
            return '';
        }
    }

    private registerCurrentUser(): void {
        this.user
            .observeCurrentUserInfo()
            .pipe(takeUntil(this.destroyed$))
            .subscribe(async ({ user, loginInfo }) => {
                this.currentUser = user?.person;
            });
    }

    private registerAutoLogoutTimeout(): void {
        this.autoLogoutTimeout$ = this.authentication.observeTimeUntilAutoLogout(1000).pipe(
            takeUntil(this.destroyed$),
            map((timeUntilLogout) => this.getTimeoutString(timeUntilLogout)),
        );
    }

    private registerAutoLogoutDialog(): void {
        if (this.showTimeout()) {
            this.authentication
                .observeAutoLogout()
                .pipe(takeUntil(this.destroyed$))
                .subscribe(async () => {
                    this.topBar?.mainMenuSidebar?.hide();
                    const dialogRef = await this.dialogs.openGenericDialog({
                        title: 'WORKSPACE.AUTOLOGOUT',
                        message: 'WORKSPACE.AUTOLOGOUT_INFO',
                        messageParameters: {
                            minutes: Math.round(this.connector.logoutTimeout / 60).toString(),
                        },
                        buttons: [{ label: 'WORKSPACE.RELOGIN', config: { color: 'primary' } }],
                        closable: Closable.Disabled,
                    });
                    await dialogRef.afterClosed().toPromise();
                    RestHelper.goToLogin(
                        this.router,
                        this.configService,
                        this.isSafe() ? RestConstants.SAFE_SCOPE : null,
                    );
                });
        }
    }

    private formatTimeout(num: number, size: number) {
        let s = num + '';
        while (s.length < size) {
            s = '0' + s;
        }
        return s;
    }
}
