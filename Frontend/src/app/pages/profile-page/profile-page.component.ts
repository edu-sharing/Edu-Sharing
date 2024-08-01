import { forkJoin as observableForkJoin, Subject, timer } from 'rxjs';

import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import {
    DefaultGroups,
    OptionItem,
    TranslationsService,
    UIAnimation,
    VCard,
} from 'ngx-edu-sharing-ui';
import {
    ConfigurationService,
    ProfileSettings,
    RestConnectorService,
    RestHelper,
    RestIamService,
    User,
    UserStats,
} from '../../core-module/core.module';
import { DomSanitizer } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Toast } from '../../services/toast';
import { trigger } from '@angular/animations';
import { Helper } from '../../core-module/rest/helper';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { take, takeUntil } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'es-profile-page',
    templateUrl: 'profile-page.component.html',
    styleUrls: ['profile-page.component.scss'],
    animations: [trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))],
})
export class ProfilePageComponent implements OnInit, OnDestroy {
    private destroyed = new Subject<void>();
    private loadingTask = this.loadingScreen.addLoadingTask({ until: this.destroyed });
    // dummy parameter to refetch the avatar
    avatarCache = '';
    constructor(
        private toast: Toast,
        private route: ActivatedRoute,
        private mainNav: MainNavService,
        private connector: RestConnectorService,
        private translations: TranslationsService,
        private router: Router,
        private config: ConfigurationService,
        private sanitizer: DomSanitizer,
        private loadingScreen: LoadingScreenService,
        private iamService: RestIamService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            route.params.subscribe((params) => {
                this.editProfileUrl = this.config.instant('editProfileUrl');
                this.editProfile = this.config.instant('editProfile', true);
                this.loadUser(params.authority);
                this.getProfileSetting(params.authority);
            });
        });
        this.editAction = new OptionItem('PROFILES.EDIT', 'edit', () => this.beginEdit());
        this.editAction.group = DefaultGroups.Edit;
        this.editAction.showAsAction = true;
        this.actions = [this.editAction];
    }
    private static PASSWORD_MIN_LENGTH = 5;
    public user: User;
    public userStats: UserStats;
    public userEdit: User;
    public isMe: boolean;
    public edit: boolean;
    public avatarFile: any;
    public changePassword: boolean;
    public editAbout = false;
    public oldPassword = '';
    public password = '';
    // is editing allowed at all (via global config)
    editProfile: boolean;
    private editProfileUrl: string;
    avatarImage: any;
    profileSettings: ProfileSettings;
    @ViewChild('avatar') avatarElement: ElementRef;
    // can the particular user profile (based on the source) be edited?
    userEditProfile: boolean;
    actions: OptionItem[];
    private editAction: OptionItem;
    showPersistentIds = false;

    ngOnInit(): void {
        this.mainNav.setMainNavConfig({
            title: 'PROFILES.TITLE_NAV',
            currentScope: 'profiles',
        });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    public loadUser(authority: string) {
        this.toast.showProgressSpinner();
        this.connector.isLoggedIn().subscribe((login) => {
            observableForkJoin(
                this.iamService.getUser(authority),
                this.iamService.getUserStats(authority),
            ).subscribe(
                ([profile, stats]) => {
                    this.user = profile.person;
                    this.userStats = stats;
                    this.userEditProfile = profile.editProfile;
                    this.toast.closeProgressSpinner();
                    this.userEdit = Helper.deepCopy(this.user);
                    if (!(this.user.profile.vcard instanceof VCard)) {
                        this.user.profile.vcard = new VCard(
                            this.user.profile.vcard as unknown as string,
                        );
                    }
                    this.userEdit.profile.vcard = this.user.profile.vcard?.copy();
                    if (!this.loadingTask.isDone) {
                        this.loadingTask.done();
                    }
                    this.iamService.getCurrentUserAsync().then((me) => {
                        this.isMe = profile.person.authorityName === me.person.authorityName;
                        if (this.isMe && login.isGuest) {
                            RestHelper.goToLogin(this.router, this.config);
                        }
                        this.editAction.isEnabled =
                            this.editProfile && !!(this.userEditProfile || this.editProfileUrl);
                    });
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    if (!this.loadingTask.isDone) {
                        this.loadingTask.done();
                    }
                    this.toast.error(null, 'PROFILES.LOAD_ERROR');
                },
            );
        });
    }
    private getProfileSetting(authority: string) {
        this.iamService.getProfileSettings(authority).subscribe(
            (res: ProfileSettings) => {
                this.profileSettings = res;
            },
            (error: any) => {
                this.profileSettings = null;
            },
        );
    }
    public updateAvatar(event: any) {
        if (
            this.avatarElement.nativeElement.files &&
            this.avatarElement.nativeElement.files.length
        ) {
            this.avatarFile = this.avatarElement.nativeElement.files[0];
            this.avatarImage = this.sanitizer.bypassSecurityTrustUrl(
                URL.createObjectURL(this.avatarFile),
            );
        }
    }
    public beginEdit() {
        if (!this.userEditProfile && this.editProfileUrl) {
            window.location.href = this.editProfileUrl;
            return;
        }
        this.userEdit = Helper.deepCopy(this.user);
        this.userEdit.profile.vcard = this.user.profile.vcard.copy();
        this.edit = true;
        this.avatarFile = null;
    }
    public clearAvatar() {
        this.avatarFile = null;
        this.userEdit.profile.avatar = null;
    }
    public hasAvatar() {
        return this.userEdit.profile.avatar || this.avatarFile;
    }
    public savePassword() {
        if (this.changePassword) {
            this.toast.showProgressSpinner();
            if (this.password.length < ProfilePageComponent.PASSWORD_MIN_LENGTH) {
                this.toast.error(null, 'PASSWORD_MIN_LENGTH', {
                    length: ProfilePageComponent.PASSWORD_MIN_LENGTH,
                });
                this.toast.closeProgressSpinner();
                return;
            }
            const credentials = { oldPassword: this.oldPassword, newPassword: this.password };
            this.iamService.editUserCredentials(this.user.authorityName, credentials).subscribe(
                () => {
                    this.saveAvatar();
                },
                (error: any) => {
                    if (RestHelper.errorMessageContains(error, 'BadCredentialsException')) {
                        this.toast.error(null, 'WRONG_PASSWORD');
                        this.toast.closeProgressSpinner();
                    } else {
                        this.toast.error(error);
                        this.saveAvatar();
                    }
                },
            );
        } else {
            this.saveAvatar();
        }
    }
    public saveEdits() {
        if (!this.userEdit.profile.firstName?.trim()) {
            this.toast.error(null, 'PROFILES.ERROR.FIRST_NAME');
            return;
        }
        if (!this.userEdit.profile.lastName?.trim()) {
            this.toast.error(null, 'PROFILES.ERROR.LAST_NAME');
            return;
        }
        if (!this.userEdit.profile.email?.trim()) {
            this.toast.error(null, 'PROFILES.ERROR.EMAIL');
            return;
        }
        this.toast.showProgressSpinner();
        this.iamService.editUser(this.user.authorityName, this.userEdit.profile).subscribe(
            () => {
                this.saveProfileSettings();
            },
            (error: any) => {
                this.toast.closeProgressSpinner();
                this.toast.error(error);
            },
        );
    }

    private saveAvatar() {
        this.user = null;
        if (!this.userEdit.profile.avatar && !this.avatarFile) {
            this.iamService.removeUserAvatar(this.userEdit.authorityName).subscribe(
                () => {
                    this.edit = false;
                    this.editAbout = false;
                    this.oldPassword = '';
                    this.password = '';
                    this.changePassword = false;
                    this.toast.toast('PROFILE_UPDATED');
                    this.loadUser(this.userEdit.authorityName);
                },
                (error) => {
                    this.toast.error(error);
                },
            );
        } else if (this.avatarFile) {
            this.iamService.setUserAvatar(this.avatarFile, this.userEdit.authorityName).subscribe(
                () => {
                    this.edit = false;
                    this.editAbout = false;
                    this.toast.toast('PROFILE_UPDATED');
                    this.loadUser(this.userEdit.authorityName);
                    // the backend is running an async refresh task
                    // so we wait here and try to refetch the icon
                    timer(0, 1000)
                        .pipe(take(10), takeUntil(this.destroyed))
                        .subscribe(() => {
                            this.avatarCache = '&dontcache = ' + Math.random();
                        });
                },
                (error) => {
                    this.toast.error(error);
                },
            );
        } else {
            this.toast.closeProgressSpinner();
            this.edit = false;
            this.editAbout = false;
            this.toast.toast('PROFILE_UPDATED');
            this.loadUser(this.userEdit.authorityName);
        }
    }

    private saveProfileSettings() {
        this.iamService.setProfileSettings(this.profileSettings, this.user.authorityName).subscribe(
            () => {
                this.saveAvatar();
            },
            (error) => {
                this.toast.closeProgressSpinner();
                this.toast.error(error);
            },
        );
    }
    public aboutEdit() {
        this.userEdit = Helper.deepCopy(this.user);
        this.userEdit.profile.vcard = this.user.profile.vcard?.copy();
        this.editAbout = true;
    }

    public editPassword() {
        this.changePassword = !this.changePassword;
        this.password = '';
        this.oldPassword = '';
    }

    savePersistentIds() {
        this.saveEdits();
    }
}
