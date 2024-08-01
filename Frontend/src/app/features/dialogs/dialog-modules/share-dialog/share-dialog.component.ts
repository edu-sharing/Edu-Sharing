import {
    AfterViewInit,
    ApplicationRef,
    Component,
    Inject,
    OnInit,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { LocalEventsService, UIAnimation, UIConstants } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { forkJoin as observableForkJoin } from 'rxjs';
import {
    CollectionUsage,
    ConfigurationService,
    DialogButton,
    LocalPermissions,
    LoginResult,
    Node,
    NodePermissions,
    NodeShare,
    NodeWrapper,
    Permission,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    RestUsageService,
    Usage,
    UsageList,
} from '../../../../core-module/core.module';
import { Helper } from '../../../../core-module/rest/helper';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { CARD_DIALOG_DATA, CardDialogConfig } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CardDialogUtilsService } from '../../card-dialog/card-dialog-utils.service';
import { DialogsService } from '../../dialogs.service';
import { ShareDialogPublishComponent } from './publish/publish.component';
import { ShareDialogData, ShareDialogResult } from './share-dialog-data';
import { trigger } from '@angular/animations';
import { AuthenticationService } from 'ngx-edu-sharing-api';
import { ShareDialogRestrictedAccessComponent } from './restricted-access/restricted-access.component';

@Component({
    selector: 'es-share-dialog',
    templateUrl: './share-dialog.component.html',
    styleUrls: ['./share-dialog.component.scss'],
    animations: [trigger('overlay', UIAnimation.openOverlay())],
})
export class ShareDialogComponent implements OnInit, AfterViewInit {
    @ViewChild('publish') publishComponent: ShareDialogPublishComponent;
    @ViewChild(ShareDialogRestrictedAccessComponent)
    restrictedAccessComponent: ShareDialogRestrictedAccessComponent;
    @ViewChild('inheritRef') inheritRef: any;
    @ViewChild('state') stateRef: TemplateRef<HTMLElement>;
    @ViewChild('shareLink') shareLinkRef: TemplateRef<HTMLElement>;
    readonly RestConstants = RestConstants;
    readonly ALL_PERMISSIONS = [
        'All',
        'Read',
        'ReadPreview',
        'ReadContent',
        'DownloadContent',
        'Print',
        'ReadAll',
        'Comment',
        'Rate',
        'Write',
        'Delete',
        'DeleteChildren',
        'DeleteNode',
        'AddChildren',
        'Consumer',
        'ConsumerMetadata',
        'Editor',
        'Contributor',
        'Collaborator',
        'Coordinator',
        'ReadPermissions',
        'ChangePermissions',
        'CCPublish',
        'Relation',
        'Comment',
        'Feedback',
        'Deny',
    ];
    readonly PERMISSIONS_FORCES = [
        ['Read', ['ConsumerMetadata']],
        ['Read', ['Consumer']],
        ['ReadPreview', ['ReadAll']],
        ['ReadContent', ['ReadAll']],
        ['Print', ['Consumer']],
        ['DownloadContent', ['Consumer']],
        ['ReadAll', ['Consumer']],
        ['Comment', ['Consumer']],
        ['Feedback', ['Consumer']],
        ['Rate', ['Consumer']],
        ['Write', ['Editor']],
        ['DeleteChildren', ['Delete']],
        ['DeleteNode', ['Delete']],
        ['AddChildren', ['Contributor']],
        ['Relation', ['Contributor']],
        ['ReadPermissions', ['Contributor']],
        ['Contributor', ['Collaborator']],
    ];

    initialState: string;
    _tab = 0;
    set tab(tab: number) {
        this._tab = tab;
        this.updateButtons();
    }
    get tab() {
        return this._tab;
    }
    permissionsUser: Permission[];
    permissionsGroup: Permission[];
    newPermissions: Permission[] = [];
    inheritAccessDenied = false;
    bulkMode = 'extend';
    bulkInvite = false;
    owner: Permission;
    publishEnabled: Permission;
    linkEnabled: Permission;
    linkDisabled: Permission;
    link = false;
    _nodes: Node[];
    searchStr: string;
    inheritAllowed = false;
    isSharedScope = false;
    globalSearch = false;
    globalAllowed = false;
    fuzzyAllowed = false;
    showLink: boolean;
    isAdmin: boolean;
    publishPermission: boolean;
    restrictedAccessPermission: boolean;
    isSafe = false;
    collectionColumns = UIHelper.getDefaultCollectionColumns();
    collections: CollectionUsage[];
    // store authorities marked for deletion
    deletedPermissions: string[] = [];
    deletedUsages: any[] = [];
    usages: { [type: string]: Usage[] };

    currentType = [RestConstants.ACCESS_CONSUMER, RestConstants.ACCESS_CC_PUBLISH];
    inherited: boolean;
    notifyUsers = true;
    notifyMessage: string;
    inherit: Permission[] = [];
    permissions: Permission[] = null;
    private originalPermissions: LocalPermissions[];
    showChooseType = false;
    private showChooseTypeList: Permission;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: ShareDialogData,
        private dialogRef: CardDialogRef<ShareDialogData, ShareDialogResult>,
        private applicationRef: ApplicationRef,
        private authenticationService: AuthenticationService,
        private cardDialogUtils: CardDialogUtilsService,
        private collectionService: RestCollectionService,
        private config: ConfigurationService,
        private connector: RestConnectorService,
        private localEvents: LocalEventsService,
        private dialogs: DialogsService,
        private iam: RestIamService,
        private nodeApi: RestNodeService,
        private toast: Toast,
        private translate: TranslateService,
        private usageApi: RestUsageService,
    ) {
        //this.dataService=new SearchData(iam);

        this.linkEnabled = new Permission();
        this.linkEnabled.authority = {
            authorityName: this.translate.instant('WORKSPACE.SHARE.LINK_ENABLED_INFO'),
            authorityType: 'LINK',
        };
        this.linkEnabled.permissions = [RestConstants.PERMISSION_CONSUMER];
        this.linkDisabled = new Permission();
        this.linkDisabled.authority = {
            authorityName: this.translate.instant('WORKSPACE.SHARE.LINK_DISABLED_INFO'),
            authorityType: 'LINK',
        };
        this.linkDisabled.permissions = [];
        this.publishEnabled = new Permission();
        this.publishEnabled.authority = {
            authorityName: this.translate.instant('WORKSPACE.SHARE.PUBLISH_ENABLED'),
            authorityType: 'EVERYONE',
        };
        this.publishEnabled.permissions = [
            RestConstants.PERMISSION_CONSUMER,
            RestConstants.ACCESS_CC_PUBLISH,
        ];

        this.connector.isLoggedIn(false).subscribe((data: LoginResult) => {
            this.isSafe = data.currentScope != null;
            this.updateToolpermissions();
        });
        // Call in constructor to avoid changed-after-checked error when setting `isLoading` state.
        this.initNodes();
    }

    ngOnInit(): void {
        this.initButtons();
    }

    ngAfterViewInit(): void {
        this.initCardRefs();
    }

    private initButtons(): void {
        const buttons: DialogButton[] = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton(
                'SAVE', // Can be changed in `updateButtons`.
                { color: 'primary' },
                () => this.save(),
            ),
        ];
        this.dialogRef.patchConfig({ buttons });
    }

    private initNodes() {
        const isStringArray = (a: string[] | Node[]): a is string[] => typeof a[0] === 'string';
        if (isStringArray(this.data.nodes)) {
            this.dialogRef.patchState({ isLoading: true });
            rxjs.forkJoin(
                this.data.nodes.map((nodeId) =>
                    this.nodeApi.getNodeMetadata(nodeId, [RestConstants.ALL]),
                ),
            ).subscribe((nodeWrappers) => {
                this.dialogRef.patchState({ isLoading: false });
                const nodes = nodeWrappers.map(({ node }) => node);
                this.setNodes(nodes);
            });
        } else {
            this.setNodes(this.data.nodes);
        }
    }

    private initCardRefs(): void {
        this.dialogRef.patchConfig({
            customHeaderBarContent: this.shareLinkRef,
            customBottomBarContent: this.stateRef,
        });
    }

    isCollection() {
        if (this._nodes == null) {
            return true;
        }
        return this._nodes[0].aspects.indexOf(RestConstants.CCM_ASPECT_COLLECTION) !== -1;
    }

    async openShareLinkDialog(): Promise<void> {
        const node = this._nodes[0];
        const dialogRef = await this.dialogs.openShareLinkDialog({ node });
        dialogRef.afterClosed().subscribe(() => this.updateNodeLink());
    }

    addSuggestion(data: any) {
        this.addAuthority(data);
    }

    setNodes(nodes: Node[]) {
        void this.cardDialogUtils
            .configForNodes(nodes)
            .then((config: Partial<CardDialogConfig<ShareDialogData>>) =>
                this.dialogRef.patchConfig(config),
            );
        this._nodes = nodes;
        this.initialize();
    }

    private initialize() {
        const isDirectory = new Set(this._nodes.map((n) => n.isDirectory));
        if (isDirectory.size !== 1) {
            this.toast.error(null, 'WORKSPACE.SHARE.ERROR_INVALID_TYPE_COMBINATION');
            // async to make sure the dialogRef is available
            setTimeout(() => this.cancel());
            return;
        }
        if (isDirectory.values().next()) {
            this.currentType = [RestConstants.ACCESS_CONSUMER];
        }
        if (this.data.currentPermissions) {
            this.originalPermissions = Helper.deepCopy(this.data.currentPermissions);
            this.setPermissions(this.data.currentPermissions.permissions);
            this.isInherited(this.data.currentPermissions.inherited);
            this.showLink = false;
        } else {
            this.showLink = true;
            this.updateNodeLink();
            this.dialogRef.patchState({ isLoading: true });
            observableForkJoin(
                this._nodes.map((n) => this.nodeApi.getNodePermissions(n.ref.id)),
            ).subscribe((permissions) => {
                this.originalPermissions = Helper.deepCopy(
                    permissions.map((p) => p.permissions.localPermissions),
                );
                if (permissions.length === 1 && permissions[0].permissions) {
                    //this.originalPermissions=Helper.deepCopy(permissions[0].permissions.localPermissions);
                    this.setPermissions(permissions[0].permissions.localPermissions.permissions);
                    this.isInherited(permissions[0].permissions.localPermissions.inherited);
                    setTimeout(() => this.setInitialState());
                } else {
                    this.setPermissions([]);
                }
                this.dialogRef.patchState({ isLoading: false });
            });
            this.reloadUsages();
        }
        if (this._nodes.length === 1 && this._nodes[0].parent && this._nodes[0].parent.id) {
            this.nodeApi.getNodePermissions(this._nodes[0].parent.id).subscribe(
                (data: NodePermissions) => {
                    if (data.permissions) {
                        this.inherit = data.permissions.inheritedPermissions;
                        this.removePermissions(this.inherit, 'OWNER');
                        this.inherit = this.inherit.filter(
                            (p) =>
                                p.authority.authorityName !==
                                this.connector.getCurrentLogin()?.authorityName,
                        );
                        this.removePermissions(
                            data.permissions.localPermissions.permissions,
                            'OWNER',
                        );
                        this.inherit = UIHelper.mergePermissions(
                            this.inherit,
                            data.permissions.localPermissions.permissions,
                        );
                        // dealy on tick to let sub-components (share-publish) init
                        this.initialState = this.getState();
                    }
                },
                (error: any) => {
                    this.inheritAccessDenied = true;
                },
            );
            this.nodeApi.getNodeParents(this._nodes[0].ref.id).subscribe(
                (data) => {
                    //this.inheritAllowed = !this.isCollection() && data.nodes.length > 1;
                    // changed in 4.1 to keep inherit state of collections
                    this.inheritAllowed = data.scope === 'MY_FILES' || data.nodes.length > 1;
                    this.isSharedScope = data.scope === 'SHARED_FILES';
                    this.updateToolpermissions();
                },
                (error) => {
                    // this can be caused if the node is somewhere at a location not fully visible to the user
                    this.inheritAllowed = true;
                    this.updateToolpermissions();
                },
            );
            if (this._nodes[0].ref.id) {
                this.nodeApi
                    .getNodeMetadata(this._nodes[0].ref.id, [RestConstants.ALL])
                    .subscribe((data: NodeWrapper) => {
                        let authority = data.node.properties[RestConstants.CM_CREATOR][0];
                        let user = data.node.createdBy;

                        if (data.node.properties[RestConstants.CM_OWNER]) {
                            authority = data.node.properties[RestConstants.CM_OWNER][0];
                            user = data.node.owner;
                        }
                        this.owner = new Permission();
                        this.owner.authority = {
                            authorityName: authority,
                            authorityType: 'USER',
                        };
                        (this.owner as any).user = user;
                        this.iam.getUser(authority).subscribe(
                            (apiUser) => {
                                (this.owner as any).user = apiUser.person.profile;
                                // force a refresh of the data for ui update
                                this.owner = Helper.deepCopy(this.owner);
                            },
                            (error) => {
                                // ignore, only relevant for the avatar of the owner
                            },
                        );
                        this.owner.permissions = ['Owner'];
                    });
            }
        }
        this.connector.isLoggedIn(false).subscribe((data: LoginResult) => {
            this.isAdmin = data.isAdmin;
        });
    }

    isDeleted(p: Permission) {
        return this.deletedPermissions.indexOf(p.authority.authorityName) !== -1;
    }

    cancel() {
        this.dialogRef.close(null);
    }

    hasUsages() {
        return this.usages && Object.keys(this.usages).length;
    }

    showHistory() {
        const node = this._nodes[0];
        void this.dialogs.openShareHistoryDialog({ node });
    }

    filterDisabledPermissions(permissions: Permission[]) {
        let result: Permission[] = [];
        if (!permissions) return result;
        for (let p of permissions) {
            if (this.deletedPermissions.indexOf(p.authority.authorityName) === -1) result.push(p);
        }
        return result;
    }

    setPermission(permission: Permission, name: string, status: any) {
        if (status.checked) {
            if (permission.permissions.indexOf(name) == -1) permission.permissions.push(name);
        } else {
            let index = permission.permissions.indexOf(name);
            if (index != -1) {
                permission.permissions.splice(index, 1);
            }
        }
        this.applicationRef.tick();
    }

    isImplicitPermission(permission: Permission, name: string) {
        //if(name=="Consumer") // this is the default permission, can't be removed
        //  return true;
        if (name != 'All' && permission.permissions.indexOf('All') != -1)
            // coordinator implies all permissions
            return true;
        if (name != 'Coordinator' && permission.permissions.indexOf('Coordinator') != -1)
            // coordinator implies all permissions
            return true;
        for (let array of this.PERMISSIONS_FORCES) {
            if (array[0] != name) continue;
            let list = array[1];
            if (!list) return false;
            let result = true;
            for (let perm of list) {
                if (perm == name) continue;
                if (this.hasImplicitPermission(permission, perm)) continue;
                result = false;
                break;
            }
            if (result) return true;
        }
        return false;
    }

    hasImplicitPermission(permission: Permission, name: string) {
        if (permission.permissions.indexOf(name) != -1) return true;
        return this.isImplicitPermission(permission, name);
    }

    private updateNodeLink() {
        this.nodeApi
            .getNodeShares(this._nodes[0].ref.id, RestConstants.SHARE_LINK)
            .subscribe((data: NodeShare[]) => {
                this.link = data.length > 0 && data[0].expiryDate != 0;
            });
    }

    reloadUsages() {
        this.usageApi.getNodeUsagesCollection(this._nodes[0].ref.id).subscribe((collections) => {
            this.collections = collections.filter((c) => c.collectionUsageType === 'ACTIVE');
            this.usageApi.getNodeUsages(this._nodes[0].ref.id).subscribe((usages: UsageList) => {
                const filteredUsages = usages.usages.filter(
                    (u) => this.collections.filter((c) => c.nodeId === u.nodeId).length === 0,
                );
                this.usages = RestUsageService.getNodeUsagesByRepositoryType(filteredUsages);
            });
        });
    }

    openCollection(collection: Node) {
        window.open(UIConstants.ROUTER_PREFIX + 'collections?id=' + collection.ref.id);
    }

    isStateModified() {
        return this.initialState !== this.getState();
    }

    getState() {
        if (this.getPublishActive() || this.getPublishInherit()) {
            return 'PUBLIC';
        }
        const permissions = [...(this.permissions ?? []), ...(this.inherited ? this.inherit : [])];
        for (const perm of permissions) {
            if (
                perm.authority.authorityName !== RestConstants.AUTHORITY_EVERYONE &&
                perm.authority.authorityName !== this.connector.getCurrentLogin()?.authorityName
            )
                return 'SHARED';
        }
        return 'PRIVATE';
    }

    isBulk() {
        return this._nodes && this._nodes.length > 1;
    }

    showShareLink() {
        return (
            !this.isCollection() &&
            this.connector.hasToolPermissionInstant(RestConstants.TOOLPERMISSION_INVITE_LINK)
        );
    }

    updateButtons() {
        // const saveButton = this.buttons[1];
        // saveButton.label = 'SAVE'; // this.tab == 0 ? 'WORKSPACE.BTN_INVITE' : 'APPLY';
    }

    chooseType() {
        this.showChooseType = true;
    }

    private chooseTypeList(p: Permission) {
        this.showChooseTypeList = p;
    }

    removePermission(p: Permission) {
        if (this.isDeleted(p))
            this.deletedPermissions.splice(
                this.deletedPermissions.indexOf(p.authority.authorityName),
                1,
            );
        else this.deletedPermissions.push(p.authority.authorityName);
        /*
    if(this.newPermissions.indexOf(p)!=-1)
    this.newPermissions.splice(this.newPermissions.indexOf(p),1);
  this.permissions.splice(this.permissions.indexOf(p),1);
  this.setPermissions(this.permissions);
  */
    }

    setType(type: any) {
        this.currentType = type.permissions;
        if (type.wasMain) this.showChooseType = false;
        for (let permission of this.newPermissions) {
            permission.permissions = Helper.deepCopy(this.currentType);
        }
    }

    private contains(
        permissions: Permission[],
        permission: Permission,
        comparePermissions: boolean,
    ): boolean {
        for (let p of permissions) {
            if (p.authority.authorityName == permission.authority.authorityName) {
                if (!comparePermissions) return true;
                if (Helper.arrayEquals(p.permissions, permission.permissions)) return true;
            }
        }
        return false;
    }

    private addAuthority(selected: any) {
        if (selected == null) return;
        let permission: any = new Permission();
        permission.authority = {
            authorityName: selected.authorityName,
            authorityType: selected.authorityType,
        };
        if (selected.authorityType == 'USER') {
            permission.user = selected.profile;
        } else {
            permission.group = selected.profile;
        }
        permission.permissions = this.currentType;
        permission = Helper.deepCopy(permission);
        if (this.deletedPermissions.indexOf(permission.authority.authorityName) != -1) {
            this.deletedPermissions.splice(
                this.deletedPermissions.indexOf(permission.authority.authorityName),
                1,
            );
        } else if (!this.contains(this.permissions, permission, false)) {
            this.newPermissions.push(permission);
            this.permissions.push(permission);
            this.setPermissions(this.permissions);
        } else this.toast.error(null, 'WORKSPACE.PERMISSION_AUTHORITY_EXISTS');
        this.searchStr = '';
    }

    isNewPermission(p: Permission) {
        if (!this.originalPermissions?.length || !this.originalPermissions[0].permissions)
            return true;
        return !this.contains(this.originalPermissions[0].permissions, p, true);
    }

    async save() {
        if (this.permissions != null) {
            this.dialogRef.patchState({ isLoading: true });
            let inherit = this.inherited && this.inheritAllowed && !this.isCollection();
            const actions = this._nodes.map((n, i) => {
                return async () => {
                    let permissions: Permission[] = Helper.deepCopy(this.permissions);
                    if (this.isBulk()) {
                        if (this.bulkInvite) {
                            const permission = RestHelper.getAllAuthoritiesPermission();
                            permission.permissions = [
                                RestConstants.ACCESS_CONSUMER,
                                RestConstants.ACCESS_CC_PUBLISH,
                            ];
                            permissions.push(permission);
                        }
                        // keep inherit state of original node
                        inherit = this.originalPermissions[i].inherited;
                        if (this.bulkMode === 'extend') {
                            permissions = UIHelper.mergePermissionsWithHighestPermission(
                                this.originalPermissions[i].permissions,
                                permissions,
                            );
                        } else {
                            // we do nothing, because the original ones are getting deleted
                        }
                    }
                    permissions = permissions.filter((p: Permission) => !this.isDeleted(p));
                    // handle the invitation of group everyone
                    if (this.publishComponent) {
                        permissions = this.publishComponent.updatePermissions(permissions);
                        try {
                            await this.publishComponent.save().toPromise();
                        } catch (error) {
                            if (!error.defaultPrevented) {
                                this.toast.error(error);
                            }
                            this.dialogRef.patchState({ isLoading: false });
                            return;
                        }
                    }
                    if (this.restrictedAccessComponent) {
                        try {
                            if (this.data.sendToApi) {
                                await this.restrictedAccessComponent.save();
                            }
                        } catch (error) {
                            this.toast.error(error);
                            this.dialogRef.patchState({ isLoading: false });
                            return;
                        }
                    }
                    await this.handlePermissionsPerNode(n, permissions, inherit);
                };
            });
            observableForkJoin(actions.map((a) => a())).subscribe(
                () => {
                    if (!this.data.sendToApi) {
                        return;
                    }
                    this.updateUsages(
                        RestHelper.copyPermissions(Helper.deepCopy(this.permissions), inherit),
                    );
                },
                (error: any) => {
                    this.toast.error(error);
                    this.dialogRef.patchState({ isLoading: false });
                },
            );
        }
    }
    updateToolpermissions() {
        this.connector
            .hasToolPermission(
                this.isSafe
                    ? this.isSharedScope
                        ? RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE
                        : RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE
                    : this.isSharedScope
                    ? RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE
                    : RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH,
            )
            .subscribe((has: boolean) => (this.globalAllowed = has));
        this.authenticationService
            .hasToolpermission(
                this.isSafe
                    ? this.isSharedScope
                        ? RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE_SAFE
                        : RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SAFE
                    : this.isSharedScope
                    ? RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_SHARE
                    : RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH,
            )
            .then((has: boolean) => (this.globalAllowed = has));
        this.authenticationService
            .hasToolpermission(RestConstants.TOOLPERMISSION_GLOBAL_AUTHORITY_SEARCH_FUZZY)
            .then((has: boolean) => (this.fuzzyAllowed = has));
        this.authenticationService
            .hasToolpermission(RestConstants.TOOLPERMISSION_INVITE_ALLAUTHORITIES)
            .then((has: boolean) => (this.publishPermission = has));
        this.authenticationService
            .hasToolpermission(RestConstants.TOOLPERMISSION_CONTROL_RESTRICTED_ACCESS)
            .then((has: boolean) => (this.restrictedAccessPermission = has));
    }
    updatePermissionInfo() {
        let type: string[];
        for (let permission of this.newPermissions) {
            if (type && !Helper.arrayEquals(type, permission.permissions)) {
                this.currentType = [];
                return;
            }
            type = permission.permissions;
        }
        if (type) this.currentType = type;
    }

    private removePermissions(permissions: Permission[], remove: string) {
        for (let i = 0; i < remove.length; i++) {
            if (permissions[i] && permissions[i].authority.authorityType == remove) {
                permissions.splice(i, 1);
                i--;
            }
        }
    }

    private setPermissions(permissions: Permission[]) {
        if (permissions == null) {
            permissions = [];
        }
        this.permissions = permissions;
        this.permissionsUser = this.permissions.slice();
        this.permissionsGroup = this.permissions.slice();
        this.removePermissions(this.permissionsUser, RestConstants.AUTHORITY_TYPE_GROUP);
        this.removePermissions(this.permissionsUser, RestConstants.AUTHORITY_TYPE_EVERYONE);
        this.removePermissions(this.permissionsGroup, RestConstants.AUTHORITY_TYPE_USER);
        // do not show GROUP_EVERYONE permission, is displayed in the share-publish dialog
        this.removePermissions(this.permissionsGroup, RestConstants.AUTHORITY_TYPE_EVERYONE);
    }
    getPublishInherit() {
        return (
            this.inherited &&
            this.getAuthorityPos(this.inherit, RestConstants.AUTHORITY_EVERYONE) !== -1
        );
    }
    localPublish() {
        return (
            this.getAuthorityPos(this.permissions, RestConstants.AUTHORITY_EVERYONE) !== -1 &&
            this.deletedPermissions?.indexOf(RestConstants.AUTHORITY_EVERYONE) === -1
        );
    }
    getPublishActive() {
        return (
            this.getPublishInherit() ||
            // this.localPublish() ||
            this.publishComponent?.shareModeDirect ||
            this.publishComponent?.shareModeCopy
        );
    }

    private getAuthorityPos(permissions: Permission[], authority: string) {
        let i = 0;
        for (let permission of permissions) {
            if (permission.authority.authorityName == authority) return i;
            i++;
        }
        return -1;
    }

    private updateUsages(permissions: LocalPermissions, pos = 0, error = false) {
        // skip for bulk mode
        if (pos === this.deletedUsages.length || this.isBulk()) {
            if (this.data.sendToApi) {
                this.localEvents.nodesChanged.emit(this.data.nodes as Node[]);
            }
            this.dialogRef.close(this.getEmitObject(permissions));
            if (!error) {
                this.toast.toast('WORKSPACE.PERMISSIONS_UPDATED');
            }
            return;
        }
        let usage = this.deletedUsages[pos];
        // collection
        if (usage.collection) {
            this.collectionService
                .removeFromCollection(usage.resourceId, usage.collection.ref.id)
                .subscribe(
                    () => {
                        this.updateUsages(permissions, pos + 1);
                    },
                    (error) => {
                        this.toast.error(error);
                        this.updateUsages(permissions, pos + 1, true);
                    },
                );
        } else {
            this.usageApi.deleteNodeUsage(this._nodes[0].ref.id, usage.nodeId).subscribe(
                () => {
                    this.updateUsages(permissions, pos + 1);
                },
                (error) => {
                    this.toast.error(error);
                    this.updateUsages(permissions, pos + 1, true);
                },
            );
        }
    }

    private getEmitObject(localPermissions: LocalPermissions) {
        return {
            permissions: localPermissions,
            notify: this.notifyUsers,
            notifyMessage: this.notifyMessage,
        };
    }

    private async handlePermissionsPerNode(
        n: Node,
        permissions: Permission[],
        inherit: boolean,
    ): Promise<void> {
        const permissionsCopy = RestHelper.copyAndCleanPermissions(permissions, inherit);
        if (!this.data.sendToApi) {
            this.dialogRef.close(
                this.getEmitObject(RestHelper.copyPermissions(permissions, inherit)),
            );
            return null;
        }
        await this.nodeApi
            .setNodePermissions(
                n.ref.id,
                permissionsCopy,
                this.notifyUsers && this.data.sendMessages,
                this.notifyMessage,
                false,
            )
            .toPromise();
    }

    setInitialState() {
        this.initialState = this.getState();
    }

    getNewInvitedAuthorities() {
        return this.filterDisabledPermissions(this.newPermissions).filter(
            (p) => p.authority.authorityName !== RestConstants.AUTHORITY_EVERYONE,
        );
    }

    onCheckInherit(event: any): void {
        if (!event._checked) {
            if (this.isLicenseMandatory() && !this.isLicenseEmpty()) {
                if (this.isAuthorRequired() && this.isAuthorEmpty()) {
                    this.toast.error(
                        null,
                        this.translate.instant('WORKSPACE.LICENSE.RELEASE_WITHOUT_AUTHOR'),
                    );
                } else {
                    this.toast.error(
                        null,
                        this.translate.instant('WORKSPACE.SHARE.PUBLISH.LICENSE_REQUIRED'),
                    );
                }
                event.preventDefaultEvent();
            }
        }
    }

    private isInherited(inherited: boolean) {
        if (this.isLicenseMandatory() && !this.isLicenseEmpty()) {
            if (this.isAuthorRequired() && this.isAuthorEmpty()) {
                this.inherited = false;
            } else {
                this.inherited = inherited;
            }
        } else {
            this.inherited = inherited;
        }
    }

    /**
     * Check if license is mandatory
     * @return true | false | not exist return false
     */
    isLicenseMandatory() {
        return this.config.instant('publish.licenseMandatory', false);
    }
    isAuthorMandatory() {
        return this.config.instant('publish.authorMandatory', false);
    }

    /**
     * Check if license is empty
     * @return true | false | not exist return false
     */
    isLicenseEmpty() {
        if (
            this._nodes == null ||
            !this._nodes[0].properties[RestConstants.CCM_PROP_LICENSE]?.[0]
        ) {
            return true;
        }
        return false;
    }

    /**
     * Check if author is required
     * For CC_0 and PDM, Author is not required, and we can share also without author
     * @return true | false | not exist return false
     */
    isAuthorRequired() {
        if (!this.isAuthorMandatory()) {
            return false;
        }
        if (this._nodes !== null) {
            return (
                !this._nodes[0].properties[RestConstants.CCM_PROP_LICENSE]?.includes('CC_0') &&
                !this._nodes[0].properties[RestConstants.CCM_PROP_LICENSE]?.includes('PDM')
            );
        }
        return false;
    }

    /**
     * Check if Author is empty
     * @return true | false | not exist return false
     */
    isAuthorEmpty() {
        if (
            this._nodes == null ||
            !this._nodes[0].properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.[0]
        ) {
            return true;
        }
        return false;
    }
}
