import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { RestConstants, RestIamService } from '../../../core-module/core.module';
import { UIAnimation, UIConstants } from 'ngx-edu-sharing-ui';
import { LoginInfo, User, UserService } from 'ngx-edu-sharing-api';
import { takeUntil } from 'rxjs/operators';
import { Subject } from 'rxjs';

@Component({
    selector: 'es-main-menu-sidebar',
    templateUrl: './main-menu-sidebar.component.html',
    styleUrls: ['./main-menu-sidebar.component.scss'],
    animations: [trigger('fade', UIAnimation.fade()), trigger('fromLeft', UIAnimation.fromLeft())],
})
export class MainMenuSidebarComponent implements OnInit, OnDestroy {
    readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
    readonly ME = RestConstants.ME;
    private readonly destroyed$ = new Subject<void>();

    @Input() currentScope: string;

    @Output() showLicenses = new EventEmitter<void>();
    @Output() closeSidebar = new EventEmitter<void>();

    // Internal state
    show = false;

    loginInfo: LoginInfo;
    currentUser: User;

    constructor(public iam: RestIamService, private user: UserService) {
        this.user
            .observeCurrentUser()
            .pipe(takeUntil(this.destroyed$))
            .subscribe(async (currentUser) => {
                this.currentUser = currentUser?.person;
            });
    }

    // Public methods, meant for invocation from outside this component.

    /**
     * Toggle the menu.
     */
    toggle() {
        this.show = !this.show;
    }

    // Internal methods, should only be called by this component.

    ngOnInit() {
        this.user
            .observeCurrentUserInfo()
            .pipe(takeUntil(this.destroyed$))
            .subscribe(({ loginInfo, user }) => {
                (this.loginInfo = loginInfo), (this.currentUser = user?.person);
            });
    }

    /**
     * Listen to keyboard events on the sidebar.
     *
     * When the sidebar is shown, there is a focus trap in place that should prevent the focus from
     * ever leaving the sidebar, so this should catch keyboard events if and only if
     * `this.show == true`.
     */
    @HostListener('keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.code === 'Escape') {
            event.preventDefault();
            event.stopPropagation();
            this.hide();
        }
    }

    hide() {
        this.show = false;
        this.closeSidebar.emit();
    }

    onShowLicenses() {
        this.hide();
        this.showLicenses.emit();
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }
}
