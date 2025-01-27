import { Injectable, TemplateRef } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { map } from 'rxjs/operators';
import { Node } from 'ngx-edu-sharing-api';
import { FrameEventsService } from '../../core-module/core.module';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { ManagementDialogsService } from '../../features/management-dialogs/management-dialogs.service';
import { MainNavComponent } from '../../main/navigation/main-nav/main-nav.component';
import { CookieInfoComponent } from '../cookie-info/cookie-info.component';
import { SkipNavService } from './skip-nav/skip-nav.service';
import { OptionItem } from 'ngx-edu-sharing-ui';

export class MainNavCreateConfig {
    /** allowed / display new material button */
    allowed?: boolean | 'EMIT_EVENT' = false;
    /** refer to CreateMenuComponent */
    allowBinary?: boolean = true;
    parent?: Node = null;
    folder?: boolean = false;
}

export class MainNavConfig {
    /**
     * Show or hide the entire component including banner and navigation bar
     */
    show? = true;
    /**
     * Show or hide the navigation bar
     */
    showNavigation? = true;
    /**
     * Shows the current location
     */
    showScope? = true;
    /**
     * Shows and enables the user menu
     */
    showUser? = true;
    /**
     * When true, the sidebar can be clicked to open the menu
     */
    canOpen? = true;
    /**
     * The title on the left side, will be translated
     */
    title?: string;
    /**
     * "add material" options
     */
    create?: MainNavCreateConfig = new MainNavCreateConfig();
    currentScope: string;

    /**
     * additional scope info, i.e. for collections this can be "edit" when in edit/create context
     */
    additionalScope?: 'edit';
    /**
     * Hide the search field although it was enabled via `SearchFieldService`.
     *
     * Use if you include the search-field component yourself in your page.
     */
    hideSearchField? = false;
    /**
     * Custom options that should be placed in the "New" menu
     */
    customCreateOptions?: OptionItem[];

    /**
     * If create is allowed, this event will fire the new nodes
     */
    onCreate?: (node: Node[]) => void;
    onCreateNotAllowed?: () => void;
}

export enum TemplateSlot {
    MainScopeButton,
    BeforeUserMenu,
    BelowTopBar,
}

@Injectable({
    providedIn: 'root',
})
export class MainNavService {
    private mainnav: MainNavComponent;
    private cookieInfo: CookieInfoComponent;
    private mainNavConfigSubject = new BehaviorSubject<MainNavConfig>(new MainNavConfig());
    private mainNavConfigOverrideSubject = new BehaviorSubject<Partial<MainNavConfig> | null>(null);
    private customTemplates: { [key in TemplateSlot]?: TemplateRef<any> } = {};
    /**
     * is triggered when a connector or lti element was successfully created
     * The observable will receive the newly generated node
     */
    onConnectorCreated = new Subject<Node>();

    constructor(
        private managementDialogs: ManagementDialogsService,
        private event: FrameEventsService,
        private skipNav: SkipNavService,
        private dialogs: DialogsService,
    ) {}

    /**
     * register a template to be used in the top bar instead of the default one
     */
    registerCustomTemplateSlot(slot: TemplateSlot, template: TemplateRef<any>) {
        this.customTemplates[slot] = template;
    }
    getCustomTemplateSlot(slot: TemplateSlot) {
        return this.customTemplates[slot];
    }
    getDialogs() {
        return this.managementDialogs.getDialogsComponent();
    }

    getCookieInfo() {
        return this.cookieInfo;
    }

    registerCookieInfo(cookieInfo: CookieInfoComponent) {
        this.cookieInfo = cookieInfo;
    }

    registerAccessibility() {
        this.skipNav.register('ACCESSIBILITY_SETTINGS', () =>
            this.dialogs.openAccessibilityDialog(),
        );
    }

    getMainNav() {
        return this.mainnav;
    }

    registerMainNav(maiNnav: MainNavComponent) {
        this.mainnav = maiNnav;
    }

    /**
     * Configures the `MainNavComponent`, using defaults for omitted values.
     */
    setMainNavConfig(config: MainNavConfig): void {
        this.event.broadcastEvent(FrameEventsService.EVENT_VIEW_OPENED, config.currentScope);
        this.mainNavConfigSubject.next({
            ...new MainNavConfig(),
            ...config,
        });
        setTimeout(() => {
            this.getMainNav()?.refreshBanner();
        });
    }

    /**
     * Updates the configuration of `MainNavComponent`, leaving omitted values as they were before.
     */
    patchMainNavConfig(config: Partial<MainNavConfig>): void {
        this.mainNavConfigSubject.next({
            ...this.mainNavConfigSubject.value,
            ...config,
        });
    }

    /**
     * Override individual values for the entire application, independently of what values are given
     * with `setMainNavConfig` and `patchMainNavConfig`.
     */
    globallyOverrideMainNavConfig(config: Partial<MainNavConfig>): void {
        this.mainNavConfigOverrideSubject.next(config);
    }

    observeMainNavConfig(): Observable<MainNavConfig> {
        return rxjs
            .combineLatest([this.mainNavConfigSubject, this.mainNavConfigOverrideSubject])
            .pipe(map(([config, override]) => ({ ...config, ...(override ?? {}) })));
    }
}
