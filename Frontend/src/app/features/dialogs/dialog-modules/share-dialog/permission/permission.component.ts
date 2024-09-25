import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Ace, AuthenticationService } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../../core-module/core.module';
import { ExtendedAce } from '../share-dialog.component';
import { TypeResult } from '../choose-type/choose-type.component';

@Component({
    selector: 'es-share-dialog-permission',
    templateUrl: 'permission.component.html',
    styleUrls: ['permission.component.scss'],
})
export class ShareDialogPermissionComponent implements OnInit {
    public _permission: ExtendedAce = null;
    public invalidPermission = false;
    public isEveryone: boolean;
    permissionTimebased: boolean;

    constructor(private authenticationService: AuthenticationService) {}

    async ngOnInit() {
        this.permissionTimebased = await this.authenticationService.hasToolpermission(
            RestConstants.TOOLPERMISSION_INVITE_TIMEBASED,
        );
    }

    @Input() set permission(permission: ExtendedAce) {
        this._permission = permission;
        let coordinator = permission.permissions.indexOf(RestConstants.PERMISSION_COORDINATOR);
        let collaborator = permission.permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
        if (coordinator != -1) {
            let i = permission.permissions.indexOf(RestConstants.PERMISSION_COLLABORATOR);
            if (i != -1) permission.permissions.splice(i, 1);
        }
        if (coordinator != -1 || collaborator != -1) {
            let i = permission.permissions.indexOf(RestConstants.PERMISSION_CONSUMER);
            if (i != -1) permission.permissions.splice(i, 1);
        }
        this.isEveryone = permission.authority.authorityName == RestConstants.AUTHORITY_EVERYONE;
        let check = this._permission.permissions.slice();
        if (check.indexOf(RestConstants.ACCESS_CC_PUBLISH) != -1) {
            check.splice(check.indexOf(RestConstants.ACCESS_CC_PUBLISH), 1);
        }
        this.invalidPermission =
            check.length != 1 ||
            (check[0] != RestConstants.PERMISSION_OWNER &&
                check[0] != RestConstants.PERMISSION_CONSUMER &&
                check[0] != RestConstants.PERMISSION_COLLABORATOR &&
                check[0] != RestConstants.PERMISSION_COORDINATOR);
    }
    @Input() inherit = false;
    @Input() isEditable: boolean | 'timebasedOnly' = false;
    @Input() showDelete = true;
    @Input() isDeleted = false;
    @Input() isDirectory = false;
    @Input() canPublish = true;
    @Input() timebasedAllowed = true;
    @Input() timebasedInvalid = false;
    @Output() onRemove = new EventEmitter();
    @Output() onType = new EventEmitter<TypeResult>();

    public showChooseType = false;
    timebasedOpen = false;
    currentDate = new Date().getTime();

    public remove() {
        if (this.showDelete) this.onRemove.emit();
    }
    public chooseType() {
        if (this.isEditable !== true || this.isEveryone) return;
        this.showChooseType = true;
    }
    changeType(type: TypeResult) {
        this.onType.emit(type);
        if (type.wasMain) this.showChooseType = false;
    }

    getDateTomorrow() {
        return new Date().getTime() + 1000 * 60 * 60 * 24;
    }
}
