import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
    DialogButton,
    RestAdminService,
    RestConstants,
    RestIamService,
    RestNodeService,
    ToolPermission,
} from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { TranslateService } from '@ngx-translate/core';
import { Helper } from '../../../core-module/rest/helper';
import { trigger } from '@angular/animations';
import { AuthorityNamePipe } from '../../../shared/pipes/authority-name.pipe';
import { UIAnimation } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-toolpermission-manager',
    templateUrl: 'toolpermission-manager.component.html',
    styleUrls: ['toolpermission-manager.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class ToolpermissionManagerComponent {
    isLoading = false;
    addName = '';
    creatingToolpermission = false;
    static STATUS_ALLOWED = 'ALLOWED';
    static STATUS_DENIED = 'DENIED';
    static STATUS_UNDEFINED = 'UNDEFINED';
    static STATUS_UNKNOWN = 'UNKNOWN';
    static GROUPS: any = [
        {
            name: 'SHARING',
            icon: 'share',
            permissions: [
                RestConstants.TOOLPERMISSION_INVITE,
                RestConstants.TOOLPERMISSION_INVITE_STREAM,
                RestConstants.TOOLPERMISSION_INVITE_LINK,
                RestConstants.TOOLPERMISSION_INVITE_SHARE,
                RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH,
                RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE,
                RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY,
                RestConstants.TOOLPERMISSION_INVITE_TIMEBASED,
                RestConstants.TOOLPERMISSION_INVITE_HISTORY,
            ],
        },
        {
            name: 'LICENSING',
            icon: 'copyright',
            permissions: [
                RestConstants.TOOLPERMISSION_LICENSE,
                RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES,
                RestConstants.TOOLPERMISSION_PUBLISH_COPY,
                RestConstants.TOOLPERMISSION_MANAGE_RELATIONS,
                RestConstants.TOOLPERMISSION_HANDLESERVICE,
                RestConstants.TOOLPERMISSION_CONTROL_RESTRICTED_ACCESS,
            ],
        },
        {
            name: 'DATA_MANAGEMENT',
            icon: 'folder',
            permissions: [
                RestConstants.TOOLPERMISSION_WORKSPACE,
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS,
                RestConstants.TOOLPERMISSION_CREATE_MAP_LINK,
                RestConstants.TOOLPERMISSION_UNCHECKEDCONTENT,
                RestConstants.TOOLPERMISSION_COMMENT_WRITE,
                RestConstants.TOOLPERMISSION_RATE_READ,
                RestConstants.TOOLPERMISSION_RATE_WRITE,
                RestConstants.TOOLPERMISSION_MATERIAL_FEEDBACK,
            ],
        },
        {
            name: 'ACCOUNT_MANAGEMENT',
            icon: 'group',
            permissions: [RestConstants.TOOLPERMISSION_SIGNUP_GROUP],
        },
        {
            name: 'SAFE',
            icon: 'lock',
            permissions: [
                RestConstants.TOOLPERMISSION_CONFIDENTAL,
                RestConstants.TOOLPERMISSION_INVITE_SAFE,
                RestConstants.TOOLPERMISSION_INVITE_SHARE_SAFE,
                RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE,
                RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE,
            ],
        },
        {
            name: 'COLLECTIONS',
            icon: 'layers',
            permissions: [
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
                RestConstants.TOOLPERMISSION_COLLECTION_PROPOSAL,
                RestConstants.TOOLPERMISSION_COLLECTION_EDITORIAL,
                RestConstants.TOOLPERMISSION_COLLECTION_CURRICULUM,
                RestConstants.TOOLPERMISSION_COLLECTION_PINNING,
                RestConstants.TOOLPERMISSION_VIDEO_AUDIO_CUT,
                RestConstants.TOOLPERMISSION_COLLECTION_CHANGE_OWNER,
            ],
        },
        {
            name: 'MANAGEMENT',
            icon: 'settings',
            permissions: [
                RestConstants.TOOLPERMISSION_USAGE_STATISTIC,
                RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_NODES,
                RestConstants.TOOLPERMISSION_GLOBAL_STATISTICS_USER,
            ],
        },
        {
            name: 'MEDIACENTER',
            icon: 'business',
            permissions: [RestConstants.TOOLPERMISSION_MEDIACENTER_MANAGE],
        },
        { name: 'CONNECTORS', icon: 'edit' },
        { name: 'REPOSITORIES', icon: 'cloud' },
        { name: 'OTHER', icon: 'help' },
    ];
    changing: string[] = [];
    buttons: DialogButton[];

    getGroups() {
        return ToolpermissionManagerComponent.GROUPS;
    }
    getToolpermissionsForGroup(group: any) {
        if (group.permissions) {
            return group.permissions;
        }
        let permissions = Object.keys(this.permissions);
        if (group.name == 'CONNECTORS') {
            return permissions.filter((p) =>
                p.startsWith(RestConstants.TOOLPERMISSION_CONNECTOR_PREFIX),
            );
        } else if (group.name == 'REPOSITORIES') {
            return permissions.filter((p) =>
                p.startsWith(RestConstants.TOOLPERMISSION_REPOSITORY_PREFIX),
            );
        }
        // filter "OTHER"
        for (let group of ToolpermissionManagerComponent.GROUPS) {
            if (group.permissions) {
                for (let tp of group.permissions) {
                    let pos = permissions.indexOf(tp);
                    if (pos != -1) {
                        permissions.splice(pos, 1);
                    }
                }
            } else if (group.name == 'CONNECTORS') {
                permissions = permissions.filter(
                    (p) => !p.startsWith(RestConstants.TOOLPERMISSION_CONNECTOR_PREFIX),
                );
            } else if (group.name == 'REPOSITORIES') {
                permissions = permissions.filter(
                    (p) => !p.startsWith(RestConstants.TOOLPERMISSION_REPOSITORY_PREFIX),
                );
            }
        }
        return permissions;
    }
    _authority: any;
    name: string;
    @Input() set authority(authority: any) {
        if (authority == null) return;
        this._authority = authority;
        this.isLoading = true;
        this.name = new AuthorityNamePipe(this.translate).transform(authority, null);
        this.refresh();
    }
    @Output() onClose = new EventEmitter();
    permissions: ToolPermission | any;
    allow: any;
    allowInit: any;
    deny: any;
    denyInit: any;

    constructor(
        private toast: Toast,
        private admin: RestAdminService,
        private node: RestNodeService,
        private translate: TranslateService,
        private iam: RestIamService,
    ) {
        this.buttons = DialogButton.getSingleButton('CLOSE', () => this.close(), 'standard');
    }
    close() {
        this.onClose.emit();
    }
    change(key: string) {
        this.changing.push(key);
        this.admin
            .setToolpermissions(this._authority.authorityName, this.getPermissions())
            .subscribe(
                () => {
                    /*this.toast.toast('PERMISSIONS.TOOLPERMISSIONS.SAVED');
        this.close();*/
                    this.refresh(() => {
                        let i = this.changing.indexOf(key);
                        if (i != -1) {
                            this.changing.splice(i, 1);
                        }
                    });
                },
                (error: any) => {
                    this.toast.error(error);
                },
            );
    }
    getEffective(key: string) {
        if (this.deny[key]) {
            return ToolpermissionManagerComponent.STATUS_DENIED;
        }
        if (
            this.allow[key] &&
            this.permissions[key]?.effective != ToolpermissionManagerComponent.STATUS_DENIED
        ) {
            return ToolpermissionManagerComponent.STATUS_ALLOWED;
        }
        if (
            !this.denyInit[key] &&
            this.permissions[key]?.effective == ToolpermissionManagerComponent.STATUS_DENIED
        ) {
            return ToolpermissionManagerComponent.STATUS_DENIED;
        }
        if (this.allow[key] != this.allowInit[key] || this.deny[key] != this.denyInit[key]) {
            return ToolpermissionManagerComponent.STATUS_UNKNOWN;
        }
        return this.permissions[key]?.effective;
    }
    isImplicit(key: string) {
        if (this._authority.authorityType == RestConstants.AUTHORITY_TYPE_EVERYONE) {
            return false;
        }
        if (this.getEffective(key) == ToolpermissionManagerComponent.STATUS_UNDEFINED) return false;
        if (this.deny[key]) {
            return false;
        }
        if (
            this.allow[key] &&
            this.permissions[key].effective == ToolpermissionManagerComponent.STATUS_DENIED
        ) {
            return true;
        }
        return !this.allow[key];
    }
    getImplicitDetail(key: string) {
        let names = [];
        for (let group of this.permissions[key].effectiveSource) {
            if (group.authorityType == RestConstants.AUTHORITY_TYPE_EVERYONE) {
                names.push(this.translate.instant('PERMISSIONS.TOOLPERMISSIONS.EVERYONE_ALLOWED'));
            } else {
                names.push(new AuthorityNamePipe(this.translate).transform(group, null));
            }
        }
        return this.translate.instant('PERMISSIONS.TOOLPERMISSIONS.INHERIT_DETAIL', {
            memberships: names.join(', '),
        });
    }

    private getPermissions() {
        let result: any = {};
        for (let key in this.permissions) {
            if (this.allow[key]) {
                result[key] = ToolpermissionManagerComponent.STATUS_ALLOWED;
            } else if (this.deny[key]) {
                result[key] = ToolpermissionManagerComponent.STATUS_DENIED;
            }
        }
        return result;
    }

    private refresh(callback: Function = null) {
        this.admin.getToolpermissions(this._authority.authorityName).subscribe(
            (data: any) => {
                this.isLoading = false;
                this.permissions = data;
                this.allow = {};
                this.deny = {};
                for (let key in this.permissions) {
                    let value = this.permissions[key].explicit;
                    this.allow[key] = value == ToolpermissionManagerComponent.STATUS_ALLOWED;
                    this.deny[key] = value == ToolpermissionManagerComponent.STATUS_DENIED;
                }
                this.allowInit = Helper.deepCopy(this.allow);
                this.denyInit = Helper.deepCopy(this.deny);
                if (callback) callback();
            },
            (error: any) => {
                this.toast.error(error);
                this.close();
            },
        );
    }
    createToolpermission() {
        this.creatingToolpermission = true;
        this.admin.addToolpermission(this.addName).subscribe(
            () => {
                this.toast.toast('PERMISSIONS.TOOLPERMISSIONS.ADDED', { name: this.addName });
                this.addName = '';
                this.creatingToolpermission = false;
                this.refresh();
            },
            (error) => {
                this.creatingToolpermission = false;
                this.toast.error(error);
            },
        );
    }
    getTpConnector(tp: string) {
        tp = tp.substring(RestConstants.TOOLPERMISSION_CONNECTOR_PREFIX.length);
        if (tp.indexOf('_safe') != -1) tp = tp.substring(0, tp.indexOf('_safe'));
        let connector = this.translate.instant('CONNECTOR.' + tp + '.NAME');
        return connector;
    }
    getTpSafe(tp: string) {
        return tp.endsWith('_safe');
    }
    getTpRepository(tp: any) {
        tp = tp.substring(RestConstants.TOOLPERMISSION_REPOSITORY_PREFIX.length);
        return tp;
    }
}
