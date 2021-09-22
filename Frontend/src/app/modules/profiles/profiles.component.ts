
import {Component, ElementRef, ViewChild} from '@angular/core';
import {Translation} from '../../core-ui-module/translation';
import {
    LoginResult,
    ProfileSettings,
    SessionStorageService, UIConstants,
    UserStats
} from '../../core-module/core.module';
import {TranslateService} from '@ngx-translate/core';
import {DomSanitizer} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {Toast} from '../../core-ui-module/toast';
import {RestConnectorService} from '../../core-module/core.module';
import {ConfigurationService} from '../../core-module/core.module';
import {RestIamService} from '../../core-module/core.module';
import {IamUser, User} from '../../core-module/core.module';
import {AuthorityNamePipe} from '../../core-ui-module/pipes/authority-name.pipe';
import {trigger} from '@angular/animations';
import {UIAnimation} from '../../core-module/ui/ui-animation';
import {UserProfileComponent} from '../../common/ui/user-profile/user-profile.component';
import {RestConstants} from '../../core-module/core.module';
import {RestHelper} from '../../core-module/core.module';
import {MainNavComponent} from '../../common/ui/main-nav/main-nav.component';
import {Helper} from '../../core-module/rest/helper';
import {GlobalContainerComponent} from '../../common/ui/global-container/global-container.component';
import {DefaultGroups, OptionGroup, OptionItem} from '../../core-ui-module/option-item';
import {Observable} from 'rxjs';
import { SkipTarget } from '../../common/ui/skip-nav/skip-nav.service';

@Component({
  selector: 'app-profiles',
  templateUrl: 'profiles.component.html',
  styleUrls: ['profiles.component.scss'],
  animations: [
    trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST))
  ]
})
export class ProfilesComponent {
  readonly SkipTarget = SkipTarget;
  constructor(private toast: Toast,
              private route: ActivatedRoute,
              private connector: RestConnectorService,
              private translate: TranslateService,
              private router: Router,
              private config: ConfigurationService,
              private sanitizer: DomSanitizer,
              private storage : SessionStorageService,
              private iamService: RestIamService) {
    Translation.initialize(translate, this.config, this.storage, this.route).subscribe(() => {
      route.params.subscribe((params)=> {
        this.editProfileUrl=this.config.instant('editProfileUrl');
        this.editProfile=this.config.instant('editProfile',true);
        this.activateLinks = this.config.instant('activateProfileLinks', false);
        this.loadUser(params.authority);
        this.getProfileSetting(params.authority);
      });
    });
    this.editAction = new OptionItem('PROFILES.EDIT', 'edit', () => this.beginEdit());
    this.editAction.group = DefaultGroups.Edit;
    this.editAction.showAsAction = true;
    this.actions = [
      this.editAction,
    ];
  }
  private static PASSWORD_MIN_LENGTH = 5;
  readonly ROUTER_PREFIX = UIConstants.ROUTER_PREFIX;
  public user: User;
  public userStats: UserStats;
  public userEdit: User;
  public isMe: boolean;
  public edit: boolean;
  public avatarFile: any;
  public changePassword: boolean;
  public editAbout = false;
  public oldPassword='';
  public password='';
  public hasAccessWorkspace=false;
  // is editing allowed at all (via global config)
  editProfile: boolean;
  private editProfileUrl: string;
  private activateLinks = false;
  avatarImage: any;
  profileSettings: ProfileSettings;
  @ViewChild('mainNav') mainNavRef: MainNavComponent;
  @ViewChild('avatar') avatarElement : ElementRef;
  // can the particular user profile (based on the source) be edited?
  userEditProfile: boolean;
  actions: OptionItem[];
  private editAction: OptionItem;
  showPersistentIds = false;
  public loadUser(authority:string) {
    this.toast.showProgressDialog();
    this.connector.isLoggedIn().subscribe((login)=> {
      Observable.forkJoin(
          this.iamService.getUser(authority),
          this.iamService.getUserStats(authority),
      ).subscribe(([profile, stats]) => {
        this.user = profile.person;
        this.userStats = stats;
        this.userEditProfile = profile.editProfile;
        this.toast.closeModalDialog();
        this.userEdit=Helper.deepCopy(this.user);
        this.userEdit.profile.vcard = this.user.profile.vcard?.copy();
        GlobalContainerComponent.finishPreloading();
        this.iamService.getUser().subscribe((me)=> {
          this.isMe = profile.person.authorityName === me.person.authorityName;
          this.canAccessWorkspace(login);
          if(this.isMe && login.isGuest) {
            RestHelper.goToLogin(this.router,this.config);
          }
          this.editAction.isEnabled = this.editProfile && !!(this.userEditProfile || this.editProfileUrl);
        });
      }, (error: any) => {
        this.toast.closeModalDialog();
        GlobalContainerComponent.finishPreloading();
        this.toast.error(null, 'PROFILES.LOAD_ERROR');
      });
    });
  }
  private getProfileSetting(authority:string){
        this.iamService.getProfileSettings(authority).subscribe((res: ProfileSettings) => {
            this.profileSettings = res;
        }, (error: any) => {
            this.profileSettings=null;
        });
  }
  public updateAvatar(event:any) {
    if(this.avatarElement.nativeElement.files && this.avatarElement.nativeElement.files.length) {
      this.avatarFile=this.avatarElement.nativeElement.files[0];
      this.avatarImage=this.sanitizer.bypassSecurityTrustUrl(URL.createObjectURL(this.avatarFile));
    }
  }
  public beginEdit() {
    if(!this.userEditProfile && this.editProfileUrl) {
      window.location.href=this.editProfileUrl;
      return;
    }
    this.userEdit=Helper.deepCopy(this.user);
    this.userEdit.profile.vcard = this.user.profile.vcard.copy();
    this.edit=true;
    this.avatarFile=null;
  }
  public clearAvatar() {
    this.avatarFile=null;
    this.userEdit.profile.avatar=null;
  }
  public hasAvatar() {
    return this.userEdit.profile.avatar || this.avatarFile;
  }
  public savePassword() {
    if(this.changePassword) {
      this.toast.showProgressDialog();
      if(this.password.length<ProfilesComponent.PASSWORD_MIN_LENGTH) {
        this.toast.error(null,'PASSWORD_MIN_LENGTH',{length:ProfilesComponent.PASSWORD_MIN_LENGTH});
        this.toast.closeModalDialog();
        return;
      }
      const credentials= {oldPassword:this.oldPassword,newPassword:this.password};
      this.iamService.editUserCredentials(this.user.authorityName,credentials).subscribe(()=> {
        this.saveAvatar();
      },(error:any)=> {
        if(RestHelper.errorMessageContains(error,'BadCredentialsException')) {
          this.toast.error(null,'WRONG_PASSWORD');
          this.toast.closeModalDialog();
        }
        else {
          this.toast.error(error);
          this.saveAvatar();
        }
      });
    }
    else {
      this.saveAvatar();
    }
  }
  public saveEdits() {
    if(!this.userEdit.profile.firstName.trim()) {
      this.toast.error(null,'PROFILES.ERROR.FIRST_NAME');
      return;
    }
    if(!this.userEdit.profile.lastName.trim()) {
      this.toast.error(null,'PROFILES.ERROR.LAST_NAME');
      return;
    }
    if(!this.userEdit.profile.email.trim()) {
      this.toast.error(null,'PROFILES.ERROR.EMAIL');
      return;
    }
    this.toast.showProgressDialog();
    this.iamService.editUser(this.user.authorityName,this.userEdit.profile).subscribe(()=> {
      this.saveProfileSettings();
    },(error:any)=> {
      this.toast.closeModalDialog();
      this.toast.error(error);
    });
  }

  private saveAvatar() {
    this.user=null;
    if(!this.userEdit.profile.avatar && !this.avatarFile) {
      this.iamService.removeUserAvatar(this.userEdit.authorityName).subscribe(()=> {
        this.edit=false;
        this.editAbout=false;
        this.oldPassword='';
        this.password='';
        this.changePassword=false;
        this.toast.toast('PROFILE_UPDATED');
        this.loadUser(this.userEdit.authorityName);
      },(error)=> {
        this.toast.error(error);
      });
    }
    else if(this.avatarFile) {
      this.iamService.setUserAvatar(this.avatarFile,this.userEdit.authorityName).subscribe(()=> {
        this.edit=false;
        this.editAbout=false;
        this.toast.toast('PROFILE_UPDATED');
        this.loadUser(this.userEdit.authorityName);
      },(error)=> {
        this.toast.error(error);
      });
    }
    else {
      this.toast.closeModalDialog();
      this.edit=false;
      this.editAbout=false;
      this.toast.toast('PROFILE_UPDATED');
      this.loadUser(this.userEdit.authorityName);
    }
  }

  private saveProfileSettings() {
    this.iamService.setProfileSettings(this.profileSettings, this.user.authorityName).subscribe(() => {
      this.saveAvatar();
    }, (error) => {
      this.toast.closeModalDialog();
      this.toast.error(error);
    });
  }
  public aboutEdit() {
    this.userEdit=Helper.deepCopy(this.user);
    this.userEdit.profile.vcard = this.user.profile.vcard?.copy();
    this.editAbout = true;
  }

  public editPassword() {
    this.changePassword =! this.changePassword;
    this.password = '';
    this.oldPassword = '';
  }

  savePersistentIds() {
    this.saveEdits();
  }

    /**
     * Check if USER has permissions to activate the Links in statistics section
     */
    public canActivateLinks(): boolean {
        return this.isMe && this.hasAccessWorkspace && this.activateLinks;
    }

    /**
     * check if current user have access to workspace
     * @param login params that contain all userPermission
     */
    private canAccessWorkspace(login: LoginResult): void {
        this.hasAccessWorkspace = (
            login.toolPermissions &&
            login.toolPermissions.indexOf(
                RestConstants.TOOLPERMISSION_WORKSPACE,
            ) !== -1
        );
    }


    /**
     * This function will build query and navigate to specific Profile links
     */
    public navigateTo(component: string): void {
        if (component.toLowerCase() === 'search')
            this.router.navigate([UIConstants.ROUTER_PREFIX, component.toLowerCase()], {
                queryParams: {
                    parameters: JSON.stringify({'cm:creator': [this.user.userName],license: ['CC_*']})
                }
            });
        else if (component.toLowerCase() === 'workspace')
            this.router.navigate([UIConstants.ROUTER_PREFIX, 'search'], {
                queryParams: {
                    parameters: JSON.stringify({'cm:creator': [this.user.userName]})
                }
            });
        else if (component.toLowerCase() === 'collections')
            this.router.navigate([UIConstants.ROUTER_PREFIX, component.toLowerCase()], {queryParams: {scope: 'MY'}});
    }

}

