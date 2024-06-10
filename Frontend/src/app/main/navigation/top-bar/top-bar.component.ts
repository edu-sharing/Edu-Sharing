import {
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
    Input,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { User } from 'ngx-edu-sharing-api';
import { Observable } from 'rxjs';
import { ConfigurationService, Node, RestConnectorService } from '../../../core-module/core.module';
import { OptionItem } from 'ngx-edu-sharing-ui';
import { CreateMenuComponent } from '../create-menu/create-menu.component';
import { MainMenuDropdownComponent } from '../main-menu-dropdown/main-menu-dropdown.component';
import { MainMenuSidebarComponent } from '../main-menu-sidebar/main-menu-sidebar.component';
import { MainNavCreateConfig, MainNavService, TemplateSlot } from '../main-nav.service';

@Component({
    selector: 'es-top-bar',
    templateUrl: './top-bar.component.html',
    styleUrls: ['./top-bar.component.scss'],
})
export class TopBarComponent {
    readonly TemplateSlot = TemplateSlot;
    @ContentChild('createButton') createButtonRef: TemplateRef<any>;
    @ViewChild('createMenu') createMenu: CreateMenuComponent;
    @ViewChild('dropdownTriggerDummy') createMenuTrigger: MatMenuTrigger;
    @ViewChild('mainMenuDropdown') mainMenuDropdown: MainMenuDropdownComponent;
    @ViewChild('mainMenuSidebar') mainMenuSidebar: MainMenuSidebarComponent;
    @ViewChild('userRef') userRef: ElementRef;

    @Input() autoLogoutTimeout$: Observable<string>;
    @Input() canOpen = true;
    @Input() chatCount: number;
    @Input() config: any;
    @Input() create: MainNavCreateConfig;
    @Input() currentScope: string;
    @Input() currentUser: User;
    @Input() isCreateAllowed: boolean;
    @Input() isSafe: boolean;
    @Input() mainMenuStyle: 'sidebar' | 'dropdown' = 'sidebar';
    @Input() searchEnabled: boolean;
    @Input() showChat: boolean;
    @Input() showScope = true;
    @Input() showUser: boolean;
    @Input() title: string;
    @Input() userMenuOptions: OptionItem[];

    @Output() created = new EventEmitter<Node[]>();
    @Output() createNotAllowed = new EventEmitter<void>();
    @Output() openChat = new EventEmitter<void>();
    @Output() showLicenses = new EventEmitter<void>();
    @Output() onCloseScopeSelector = new EventEmitter<void>();

    createMenuX: number;
    createMenuY: number;
    toggleSidebar = () => this.mainMenuSidebar.toggle();

    constructor(
        // FIXME: Required values should be passed as inputs.
        public connector: RestConnectorService,
        private configService: ConfigurationService,
        public mainNavService: MainNavService,
        public elementRef: ElementRef,
    ) {}

    getIconSource() {
        return this.configService.instant('mainnav.icon.url', 'assets/images/edu-white.svg');
    }

    toggleMenuSidebar() {
        if (this.canOpen) {
            if (this.mainMenuSidebar) {
                this.mainMenuSidebar.toggle();
            } else if (this.mainMenuDropdown) {
                this.mainMenuDropdown.dropdown.menuTrigger.openMenu();
            }
        }
    }

    isSidenavOpen() {
        return this.mainMenuSidebar?.show;
    }

    openCreateMenu(x: number, y: number) {
        this.createMenuX = x;
        this.createMenuY = y;

        this.createMenu.updateOptions();
        this.createMenuTrigger.openMenu();
        this.createMenuTrigger.onMenuClose;
    }
}
