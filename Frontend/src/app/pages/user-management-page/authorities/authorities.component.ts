import { trigger } from '@angular/animations';
import {
    AfterViewInit,
    ApplicationRef,
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnChanges,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { GenericAuthority, Group, User } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    Constrain,
    CustomOptions,
    DefaultGroups,
    ElementType,
    FetchEvent,
    InteractionType,
    ListItem,
    ListItemSort,
    ListItemType,
    ListSortConfig,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    UIAnimation,
} from 'ngx-edu-sharing-ui';
import { forkJoin } from 'rxjs';
import { filter } from 'rxjs/operators';
import { SuggestItem } from '../../../pages/admin-page/autocomplete/autocomplete.component';
import {
    DialogButton,
    GroupSignupDetails,
    IamAuthorities,
    IamGroups,
    IamUsers,
    Node,
    NodeList,
    Organization,
    OrganizationOrganizations,
    RestConnectorService,
    RestConstants,
    RestIamService,
    RestNodeService,
    RestOrganizationService,
    SharedFolder,
    UIService,
} from '../../../core-module/core.module';
import { CsvHelper } from '../../../core-module/csv.helper';
import { Helper } from '../../../core-module/rest/helper';
import { NodeHelperService } from '../../../services/node-helper.service';
import { Toast, ToastType } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { DELETE_OR_CANCEL } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { BreadcrumbsService } from '../../../shared/components/breadcrumbs/breadcrumbs.service';
import { InputPasswordComponent } from '../../../shared/components/input-password/input-password.component';

@Component({
    selector: 'es-permissions-authorities',
    templateUrl: 'authorities.component.html',
    styleUrls: ['authorities.component.scss'],
    animations: [
        trigger('fromRight', UIAnimation.fromRight()),
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
    providers: [BreadcrumbsService, OptionsHelperDataService],
})
export class PermissionsAuthoritiesComponent implements OnChanges, AfterViewInit {
    readonly DisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    sortConfig: ListSortConfig = {
        allowed: true,
        active: null,
        direction: 'asc',
        columns: [],
    };
    @ViewChild('actionbar') actionbar: ActionbarComponent;
    @ViewChild('passwordRef') passwordRef: InputPasswordComponent;
    @ViewChild('actionbarMember') actionbarMember: ActionbarComponent;
    @ViewChild('actionbarSignup') actionbarSignup: ActionbarComponent;
    @ViewChild('mainList') nodeEntries: NodeEntriesWrapperComponent<GenericAuthority>;
    @ViewChild('memberAdd') nodeMemberAdd: NodeEntriesWrapperComponent<GenericAuthority>;
    @ViewChild('signupList') signupList: NodeEntriesWrapperComponent<GenericAuthority>;
    @ViewChild('addToComponent') addToComponent: PermissionsAuthoritiesComponent;
    public GROUP_TYPES = RestConstants.VALID_GROUP_TYPES;
    public STATUS_TYPES = RestConstants.VALID_PERSON_STATUS_TYPES;
    public SCOPE_TYPES = RestConstants.VALID_SCOPE_TYPES;
    public ORG_TYPES = RestConstants.VALID_GROUP_TYPES_ORG;
    public PRIMARY_AFFILIATIONS = RestConstants.USER_PRIMARY_AFFILIATIONS;
    public edit: {
        authorityName?: string;
        constrains?: Constrain[];
        elementType?: ElementType[];
        folderPath?: Node[];
        group?: DefaultGroups;
        profile: any;
        quota?: any;
        priority?: number;
    };
    editDetails: any;
    editId: string;
    public columns: ListItem[] = [];
    public addMemberColumns: ListItem[] = [];
    public editGroupColumns: ListItem[] = [];
    public _searchQuery: string;
    manageMemberSearch: string;
    public options: CustomOptions = {
        useDefaultOptions: true,
        supportedOptions: [],
        addOptions: [],
    };
    public toolpermissionAuthority: any;
    public optionsActionbar: OptionItem[];
    private orgs: OrganizationOrganizations;
    public addMembers: GenericAuthority;
    public editGroups: GenericAuthority;
    memberOptions: CustomOptions = {
        useDefaultOptions: false,
        addOptions: [],
    };
    private addToList: any[];
    isAdmin = false;
    embeddedQuery: string;
    editButtons: DialogButton[];
    memberButtons: DialogButton[];
    signupButtons: DialogButton[];
    signupListButtons: DialogButton[];
    editStatus: User;
    resetPassword: User;
    newPassword = '';
    editStatusNotify = true;
    editStatusButtons: DialogButton[];
    resetPasswordButtons: DialogButton[];
    groupSignup: Organization;
    groupSignupListNode: Organization;
    groupSignupListShown = false;
    groupSignupList = new NodeDataSource<User>();
    groupSignupDetails: GroupSignupDetails;
    private _org: Organization;
    @Output() onDeselectOrg = new EventEmitter();

    @Input() set searchQuery(searchQuery: string) {
        this._searchQuery = searchQuery;
        // wait for other data to be initalized
        setTimeout(() => this.search());
    }

    public _mode: ListItemType;
    public addTo: any;
    addToSelection: GenericAuthority[];

    @Input() set org(org: Organization) {
        this._org = org;
        // this.refresh();
    }

    get org() {
        return this._org;
    }

    @Input() embedded = false;
    @Output() onSelection = new EventEmitter<GenericAuthority[]>();
    @Output() setTab = new EventEmitter<number>();
    public editMembers: GenericAuthority | 'ALL';
    memberList = new NodeDataSource<GenericAuthority>();
    private memberSuggestions: SuggestItem[];
    // show primary affiliations as list (or free text)
    primaryAffiliationList = true;
    signupActions: CustomOptions = {
        useDefaultOptions: false,
    };

    private updateMemberSuggestions(event: any) {
        if (this.editMembers == this.org || this.org == null) {
            this.iam.searchUsers(event.input).subscribe(
                (users: IamUsers) => {
                    const ret: SuggestItem[] = [];
                    for (const user of users.users) {
                        const item = new SuggestItem(
                            user.authorityName,
                            user.profile.firstName + ' ' + user.profile.lastName,
                            'person',
                            '',
                        );
                        item.originalObject = user;
                        ret.push(item);
                    }
                    this.memberSuggestions = ret;
                },
                (error) => console.log(error),
            );
        } else {
            this.iam.getGroupMembers(this.org.authorityName, event.input, 'USER').subscribe(
                (users: IamAuthorities) => {
                    const ret: SuggestItem[] = [];
                    for (const user of users.authorities) {
                        const item = new SuggestItem(
                            user.authorityName,
                            user.profile.firstName + ' ' + user.profile.lastName,
                            'person',
                            '',
                        );
                        item.originalObject = user;
                        ret.push(item);
                    }
                    this.memberSuggestions = ret;
                },
                (error) => console.log(error),
            );
        }
    }

    private addMember(event: any) {
        if (this.editMembers === 'ALL') {
            this.iam.addGroupMember(this.org.authorityName, event.item.id).subscribe(
                () => {
                    this.memberList.reset();
                    this.searchMembers();
                },
                (error: any) => this.handleError(error),
            );
        } else {
            this.iam
                .addGroupMember((this.editMembers as Group).authorityName, event.item.id)
                .subscribe(
                    () => {
                        this.memberList.reset();
                        this.searchMembers();
                    },
                    (error: any) => this.handleError(error),
                );
        }
    }

    dataSource = new NodeDataSource<GenericAuthority>();

    @Input() set mode(mode: ListItemType) {
        this._mode = mode;
    }

    private getMemberOptions(): OptionItem[] {
        const options: OptionItem[] = [];
        if (this.editMembers || this.editGroups) {
            const removeMembership = new OptionItem(
                'PERMISSIONS.MENU_REMOVE_MEMBERSHIP',
                'delete',
                (data) =>
                    this.deleteMembership(
                        NodeHelperService.getActionbarNodes(
                            this.nodeMemberAdd.getSelection().selected,
                            data,
                        ),
                    ),
            );
            removeMembership.constrains = [Constrain.User];
            removeMembership.group = DefaultGroups.Delete;
            removeMembership.elementType = [ElementType.Group];
            options.push(removeMembership);
            const removeFromGroup = new OptionItem(
                'PERMISSIONS.MENU_REMOVE_MEMBER',
                'delete',
                (data) => {
                    this.deleteMember(
                        NodeHelperService.getActionbarNodes(
                            this.nodeMemberAdd.getSelection().selected,
                            data,
                        ),
                    );
                },
            );
            removeFromGroup.constrains = [Constrain.User];
            removeFromGroup.group = DefaultGroups.Delete;
            removeFromGroup.elementType = [ElementType.Person];
            options.push(removeFromGroup);
        }
        return options;
    }

    private getColumns(mode: ListItemType, fromDialog = false) {
        const columns: ListItem[] = [];
        if (mode == 'USER') {
            columns.push(new ListItem(mode, RestConstants.AUTHORITY_NAME));
            columns.push(new ListItem(mode, RestConstants.AUTHORITY_FIRSTNAME));
            columns.push(new ListItem(mode, RestConstants.AUTHORITY_LASTNAME));
            if (!fromDialog) {
                columns.push(new ListItem(mode, RestConstants.AUTHORITY_EMAIL));
                columns.push(new ListItem(mode, RestConstants.AUTHORITY_STATUS));
            }
        } else if (mode == 'GROUP') {
            columns.push(new ListItem(mode, RestConstants.AUTHORITY_DISPLAYNAME));
            if (!fromDialog) {
                columns.push(new ListItem(mode, RestConstants.AUTHORITY_GROUPTYPE));
            }
        } else {
            columns.push(new ListItem(mode, RestConstants.AUTHORITY_DISPLAYNAME));
            columns.push(new ListItem(mode, RestConstants.AUTHORITY_GROUPTYPE));
        }
        return columns;
    }

    constructor(
        private breadcrumbsService: BreadcrumbsService,
        private connector: RestConnectorService,
        private dialogs: DialogsService,
        private iam: RestIamService,
        private node: RestNodeService,
        private nodeHelper: NodeHelperService,
        private optionsHelperService: OptionsHelperDataService,
        private organization: RestOrganizationService,
        private ref: ApplicationRef,
        private router: Router,
        private toast: Toast,
        private translate: TranslateService,
        private uiService: UIService,
    ) {
        this.isAdmin = this.connector.getCurrentLogin()?.isAdmin;
        this.organization.getOrganizations().subscribe((data: OrganizationOrganizations) => {
            this.updateOptions();
            this.updateButtons();
        });
    }

    ngAfterViewInit(): void {
        this.nodeEntries
            .getSelection()
            .changed.pipe(
                // do not fire in org mode since this loses selection on tab switch
                filter(() => this._mode !== 'ORG'),
            )
            .subscribe((selection) => this.onSelection.emit(selection.source.selected));
    }

    async ngOnChanges(changes: SimpleChanges) {
        this.updateColumns();
        await this.updateOptions();
    }

    private search() {
        this.refresh();
    }
    public changeSort(event: ListSortConfig) {
        //this.sortBy=event.sortBy;
        if (this._mode == 'GROUP' || this._mode == 'USER') {
            this.sortConfig.active = event.active;
        }
        this.sortConfig.direction = event.direction;
        this.dataSource.reset();
        this.loadAuthorities();
    }
    private getList<T>(data: T): T[] {
        return NodeHelperService.getActionbarNodes(
            this.nodeEntries.getSelection().selected as unknown as T[],
            data,
        );
    }
    private async updateOptions() {
        if (this.embedded) {
            this.options.addOptions = [];
            this.options.supportedOptions = [];
        } else {
            this.options.supportedOptions = ['OPTIONS.DEBUG'];
            const options: OptionItem[] = [];
            if (this._mode === 'ORG') {
                const global = new OptionItem(
                    'PERMISSIONS.MENU_TOOLPERMISSIONS_GLOBAL',
                    'playlist_add_check',
                    (data: any) =>
                        (this.toolpermissionAuthority = RestConstants.getAuthorityEveryone()),
                );
                global.elementType = [ElementType.Unknown];
                global.group = DefaultGroups.Primary;
                global.priority = 10;
                global.constrains = [Constrain.Admin, Constrain.NoSelection];
                options.push(global);
            }
            if (this._mode === 'GROUP') {
                const createGroup = new OptionItem('PERMISSIONS.MENU_CREATE_GROUP', 'add', (data) =>
                    this.createGroup(),
                );
                createGroup.elementType = [ElementType.Unknown];
                createGroup.group = DefaultGroups.Primary;
                createGroup.constrains = [Constrain.NoSelection];
                options.push(createGroup);
            }
            if (this._mode === 'USER') {
                if (this.org) {
                    const addUser = new OptionItem(
                        'PERMISSIONS.MENU_ADD_GROUP_MEMBERS',
                        'person_add',
                        (data: any) => this.addMembersFunction(this.org),
                    );
                    addUser.elementType = [ElementType.Unknown];
                    addUser.group = DefaultGroups.Primary;
                    addUser.priority = 10;
                    addUser.constrains = [Constrain.NoSelection];
                    options.push(addUser);
                }
                if (this.orgs) {
                    const createAuthority = new OptionItem(
                        'PERMISSIONS.MENU_CREATE_USER',
                        'add',
                        (data: any) => this.createAuthority(),
                    );
                    createAuthority.elementType = [ElementType.Unknown];
                    createAuthority.group = DefaultGroups.Primary;
                    createAuthority.priority = 10;
                    createAuthority.constrains = [Constrain.Admin, Constrain.NoSelection];
                    options.push(createAuthority);
                }
                const download = new OptionItem(
                    'PERMISSIONS.EXPORT_MEMBER',
                    'cloud_download',
                    (data: any) => this.downloadMembers(),
                );
                download.onlyDesktop = true;
                download.elementType = [ElementType.Unknown];
                download.group = DefaultGroups.Primary;
                download.priority = 10;
                download.constrains = [Constrain.NoSelection];
                options.push(download);
            }

            if (this._mode === 'ORG' && this.orgs && this.orgs.canCreate) {
                const newOrg = new OptionItem('PERMISSIONS.ADD_ORG', 'add', (data) =>
                    this.createOrg(),
                );
                newOrg.elementType = [ElementType.Unknown];
                newOrg.group = DefaultGroups.Primary;
                newOrg.priority = 20;
                newOrg.constrains = [Constrain.Admin, Constrain.NoSelection];
                options.push(newOrg);
            }
            const orgSignupList = new OptionItem(
                'PERMISSIONS.ORG_SIGNUP_LIST',
                'playlist_add',
                async (data) => {
                    this.toast.showProgressSpinner();
                    this.groupSignup = this.getList(data)[0];
                    this.groupSignupListShown = true;
                    this.ref.tick();
                    this.signupList.getSelection().clear();
                    this.groupSignupList.setData(
                        (await this.iam
                            .getGroupSignupList(this.groupSignup.authorityName)
                            .toPromise()) as unknown as User[],
                    );
                    await this.signupList.initOptionsGenerator({
                        actionbar: this.actionbarSignup,
                        customOptions: this.signupActions,
                    });
                    this.toast.closeProgressSpinner();
                },
            );
            orgSignupList.elementType = [ElementType.Group];
            orgSignupList.group = DefaultGroups.Edit;
            orgSignupList.customShowCallback = async (nodes) => {
                return nodes[0].signupMethod === 'list';
            };
            orgSignupList.priority = 20;
            orgSignupList.constrains = [Constrain.NoBulk];
            options.push(orgSignupList);
            const orgSignup = new OptionItem('PERMISSIONS.ORG_SIGNUP', 'checkbox', (data) => {
                this.groupSignup = this.getList(data)[0];
                this.groupSignupDetails = {
                    signupMethod: this.getList(data)[0].signupMethod ?? 'disabled',
                    signupPassword: '',
                };
            });
            orgSignup.elementType = [ElementType.Group];
            orgSignup.group = DefaultGroups.Edit;
            orgSignup.priority = 30;
            orgSignup.constrains = [Constrain.NoBulk];
            options.push(orgSignup);
            const addToGroup = new OptionItem(
                'PERMISSIONS.MENU_ADD_TO_GROUP',
                'group_add',
                (data) => this.addToGroup(data),
            );
            addToGroup.elementType = [ElementType.Person];
            addToGroup.group = DefaultGroups.Primary;
            addToGroup.priority = 10;
            addToGroup.constrains = [Constrain.User];
            options.push(addToGroup);
            const manageMemberships = new OptionItem(
                'PERMISSIONS.MENU_EDIT_GROUPS',
                'group',
                (data) => this.openEditGroups(data),
            );
            manageMemberships.elementType = [ElementType.Person];
            manageMemberships.group = DefaultGroups.Primary;
            manageMemberships.priority = 20;
            manageMemberships.constrains = [Constrain.User, Constrain.NoBulk];
            options.push(manageMemberships);

            if (this._mode === 'GROUP') {
                const addMembers = new OptionItem(
                    'PERMISSIONS.MENU_ADD_GROUP_MEMBERS',
                    'group_add',
                    (data: any) => this.addMembersFunction(data),
                );
                addMembers.elementType = [ElementType.Group];
                addMembers.group = DefaultGroups.Primary;
                addMembers.constrains = [Constrain.NoBulk, Constrain.User];
                options.push(addMembers);
                const manageMembers = new OptionItem(
                    'PERMISSIONS.MENU_MANAGE_GROUP',
                    'group',
                    (data: any) => this.manageMembers(data),
                );
                manageMembers.elementType = [ElementType.Group];
                manageMembers.group = DefaultGroups.Primary;
                manageMembers.constrains = [Constrain.NoBulk, Constrain.User];
                options.push(manageMembers);
            }
            if (this._mode === 'GROUP' || (this.orgs && this.orgs.canCreate)) {
                const editGroup = new OptionItem(
                    'PERMISSIONS.MENU_EDIT_GROUP',
                    'edit',
                    (data: any) => this.editAuthority(data),
                );
                editGroup.constrains = [Constrain.NoBulk];
                editGroup.elementType = [ElementType.Group];
                editGroup.group = DefaultGroups.Edit;
                editGroup.priority = 10;
                options.push(editGroup);
            }
            if (this.orgs && this.orgs.canCreate) {
                const edit = new OptionItem('PERMISSIONS.MENU_EDIT_PERSON', 'edit', (data: any) =>
                    this.editAuthority(data),
                );
                edit.constrains = [Constrain.Admin, Constrain.NoBulk];
                edit.elementType = [ElementType.Person];
                edit.group = DefaultGroups.Edit;
                edit.priority = 10;
                options.push(edit);
            }

            const manage = new OptionItem(
                'PERMISSIONS.MENU_TOOLPERMISSIONS',
                'playlist_add_check',
                (data: any) => {
                    this.toolpermissionAuthority = this.getList(data)[0];
                },
            );
            manage.constrains = [Constrain.Admin, Constrain.NoBulk];
            manage.elementType = [ElementType.Group, ElementType.Person];
            manage.group = DefaultGroups.Reuse;
            options.push(manage);

            if (this._mode === 'GROUP') {
                const removeGroup = new OptionItem(
                    'PERMISSIONS.MENU_DELETE',
                    'delete',
                    (data: any) =>
                        this.deleteAuthority(data, (list: any) => this.startDelete(list)),
                );
                removeGroup.constrains = [Constrain.User];
                removeGroup.elementType = [ElementType.Group];
                removeGroup.group = DefaultGroups.Delete;
                options.push(removeGroup);
            }
            const personStatus = new OptionItem('PERMISSIONS.MENU_STATUS', 'check', (data: any) =>
                this.setPersonStatus(this.getList(data)[0]),
            );
            personStatus.constrains = [Constrain.NoBulk, Constrain.Admin];
            personStatus.elementType = [ElementType.Person];
            personStatus.group = DefaultGroups.Edit;
            options.push(personStatus);
            const personPassword = new OptionItem(
                'PERMISSIONS.MENU_RESET_PASSWORD',
                'lock',
                (data: any) => this.resetPersonPassword(this.getList(data)[0]),
            );
            personPassword.constrains = [Constrain.NoBulk, Constrain.Admin];
            personPassword.elementType = [ElementType.Person];
            personPassword.group = DefaultGroups.Edit;
            options.push(personPassword);
            if (this.org) {
                const excludePerson = new OptionItem(
                    'PERMISSIONS.MENU_EXCLUDE',
                    'delete',
                    (data: any) => this.startExclude(this.getList(data)),
                );
                excludePerson.constrains = [Constrain.User];
                excludePerson.elementType = [ElementType.Person];
                excludePerson.group = DefaultGroups.Delete;
                options.push(excludePerson);
            }
            if (this._mode === 'ORG' && this.orgs && this.orgs.canCreate) {
                const remove = new OptionItem('PERMISSIONS.MENU_DELETE', 'delete', (data: any) =>
                    this.deleteAuthority(data, (list: any) => this.deleteOrg(list)),
                );
                remove.group = DefaultGroups.Delete;
                remove.elementType = [ElementType.Group];
                remove.constrains = [Constrain.Admin];
                options.push(remove);
            }

            this.options.addOptions = options;

            const signupAdd = new OptionItem(
                'PERMISSIONS.ORG_SIGNUP_ADD',
                'person_add',
                (node: User) => {
                    this.toast.showProgressSpinner();
                    const users = NodeHelperService.getActionbarNodes(
                        this.signupList.getSelection().selected,
                        node,
                    );
                    forkJoin(
                        users.map((u) =>
                            this.iam.confirmSignup(this.groupSignup.authorityName, u.authorityName),
                        ),
                    ).subscribe(
                        () => {
                            this.groupSignupListShown = false;
                            this.toast.toast('PERMISSIONS.ORG_SIGNUP_ADD_CONFIRM');
                            this.toast.closeProgressSpinner();
                        },
                        (error) => {
                            this.toast.error(error);
                            this.toast.closeProgressSpinner();
                        },
                    );
                },
            );
            signupAdd.elementType = [ElementType.Person];
            signupAdd.group = DefaultGroups.Primary;
            const signupRemove = new OptionItem(
                'PERMISSIONS.ORG_SIGNUP_REJECT',
                'close',
                (node: User) => {
                    this.toast.showProgressSpinner();
                    const users = NodeHelperService.getActionbarNodes(
                        this.signupList.getSelection().selected,
                        node,
                    );
                    forkJoin(
                        users.map((u) =>
                            this.iam.rejectSignup(this.groupSignup.authorityName, u.authorityName),
                        ),
                    ).subscribe(
                        () => {
                            this.groupSignupListShown = false;
                            this.toast.toast('PERMISSIONS.ORG_SIGNUP_REJECT_CONFIRM');
                            this.toast.closeProgressSpinner();
                        },
                        (error) => {
                            this.toast.error(error);
                            this.toast.closeProgressSpinner();
                        },
                    );
                },
            );
            signupRemove.elementType = [ElementType.Person];
            signupRemove.group = DefaultGroups.Delete;
            this.signupActions.addOptions = [signupAdd, signupRemove];
        }
        await this.nodeEntries?.initOptionsGenerator({
            actionbar: this.actionbar,
            customOptions: this.options,
        });
    }
    cancelEdit() {
        this.edit = null;
    }
    cancelAddTo() {
        this.addTo = null;
    }
    cancelEditMembers() {
        this.editMembers = null;
        this.addMembers = null;
        this.editGroups = null;
        // this.refresh();
    }
    private addMembersToGroup() {
        this.toast.showProgressSpinner();
        this.addToSelection = [this.addMembers];
        this.addToList = this.nodeMemberAdd.getSelection().selected;
        this.addMembers = null;

        this.addToSingle(() => this.refresh());
    }
    private checkOrgExists(orgName: string) {
        this.organization
            .getOrganizations(orgName, false)
            .subscribe((data: OrganizationOrganizations) => {
                if (data.organizations.length) {
                    this.closeDialog();
                    this.toast.toast('PERMISSIONS.ORG_CREATED');
                    this.refresh();
                } else {
                    setTimeout(() => this.checkOrgExists(orgName), 2000);
                }
            });
    }
    private saveEdits() {
        if (this._mode == 'GROUP' || this._mode == 'ORG') {
            if (this.editId == null) {
                const name = this.edit.profile.displayName;
                const profile = this.edit.profile;
                if (this._mode == 'ORG') {
                    this.toast.showProgressSpinner();
                    this.organization.createOrganization(name).subscribe(
                        (result) => {
                            this.edit = null;
                            this.iam.editGroup(result.authorityName, profile).subscribe(
                                () => {
                                    setTimeout(() => this.checkOrgExists(name), 2000);
                                },
                                (error) => {
                                    this.toast.error(error);
                                    this.toast.closeProgressSpinner();
                                },
                            );
                        },
                        (error) => {
                            this.toast.error(error);
                            this.toast.closeProgressSpinner();
                        },
                    );
                } else {
                    this.toast.showProgressSpinner();
                    this.iam
                        .createGroup(name, this.edit.profile, this.org ? this.org.groupName : '')
                        .subscribe(
                            () => {
                                this.edit = null;
                                this.toast.closeProgressSpinner();
                                this.toast.toast('PERMISSIONS.GROUP_CREATED');
                                this.refresh();
                            },
                            (error: any) => {
                                this.toast.error(error);
                                this.toast.closeProgressSpinner();
                            },
                        );
                }
                return;
            }
            this.iam.editGroup(this.editId, this.edit.profile).subscribe(
                () => {
                    this.edit = null;
                    this.toast.toast('PERMISSIONS.GROUP_EDITED');
                    this.refresh();
                },
                (error: any) => this.toast.error(error),
            );
        } else {
            const editStore = Helper.deepCopy(this.edit);
            if (this.edit.profile?.vcard) {
                editStore.profile.vcard = this.edit.profile.vcard.copy();
            }
            editStore.profile.sizeQuota *= 1024 * 1024;
            this.toast.showProgressSpinner();
            if (this.editId == null) {
                const name = this.editDetails.authorityName;
                const password = this.editDetails.password;
                this.iam.createUser(name, password, editStore.profile).subscribe(
                    () => {
                        this.edit = null;
                        this.toast.closeProgressSpinner();
                        if (this.org) {
                            this.iam.addGroupMember(this.org.authorityName, name).subscribe(
                                () => {
                                    this.toast.toast('PERMISSIONS.USER_CREATED');
                                    this.refresh();
                                },
                                (error: any) => this.toast.error(error),
                            );
                        } else {
                            this.toast.toast('PERMISSIONS.USER_CREATED');
                            this.refresh();
                        }
                    },
                    (error: any) => {
                        this.toast.error(error);
                        this.toast.closeProgressSpinner();
                    },
                );
            } else {
                this.iam.editUser(this.editId, editStore.profile).subscribe(
                    () => {
                        this.edit = null;
                        this.toast.toast('PERMISSIONS.USER_EDITED');
                        this.refresh();
                        this.toast.closeProgressSpinner();
                    },
                    (error: any) => {
                        this.toast.error(error);
                        this.toast.closeProgressSpinner();
                    },
                );
            }
        }
    }
    public loadAuthorities(event: FetchEvent = null) {
        this.dataSource.isLoading = true;
        let sort = RestConstants.AUTHORITY_NAME;
        if (this._mode == 'ORG') {
            sort = RestConstants.CM_PROP_AUTHORITY_DISPLAYNAME;
        }
        if (this._mode == 'GROUP' && !this.org) {
            sort = this.sortConfig.active;
            if (sort == RestConstants.AUTHORITY_DISPLAYNAME) {
                sort = RestConstants.CM_PROP_AUTHORITY_DISPLAYNAME;
            }
            if (sort == RestConstants.AUTHORITY_GROUPTYPE) {
                sort = RestConstants.CCM_PROP_AUTHORITY_GROUPTYPE;
            }
        } else if (this._mode == 'USER' && !this.org) {
            sort = this.sortConfig.active;
            if (sort === RestConstants.AUTHORITY_STATUS) {
                sort = RestConstants.CM_ESPERSONSTATUS;
            }
        }

        const request = {
            sortBy: [sort],
            sortAscending: this.sortConfig.direction === 'asc',
            offset: this.dataSource.getData().length,
        };
        const query = this._searchQuery ? this._searchQuery : '';
        this.organization
            .getOrganizations(query, false)
            .subscribe((orgs: OrganizationOrganizations) => {
                this.orgs = orgs;
                this.updateOptions();
            });
        if (this._mode === 'ORG') {
            // as non-admin, search only own orgs since these are the once with access
            this.organization
                .getOrganizations(query, !this.isAdmin, request)
                .subscribe(async (orgs: OrganizationOrganizations) => {
                    await this.dataSource.appendData(
                        orgs.organizations.filter((o) => o.administrationAccess),
                    );
                    this.dataSource.isLoading = false;
                    this.updateOptions();
                });
        } /*
    else if(this._mode=='USER'){
      this.iam.searchUsers(this.query,request).subscribe((users : IamUsers) => {
        for(let user of users.users)
          this.list.push(user);
        this.loading=false;
      });
    }
    else{
      this.iam.searchGroups(this.query,request).subscribe((groups : IamGroups) => {
        for(let group of groups.groups)
          this.list.push(group);
        this.loading=false;
      });
    }*/ else {
            if (this.org) {
                this.iam
                    .getGroupMembers(this.org.authorityName, query, this._mode, request)
                    .subscribe(async (data: IamAuthorities) => {
                        this.dataSource.setPagination(data.pagination);
                        await this.dataSource.appendData(data.authorities as Organization[]);
                        this.dataSource.isLoading = false;
                    });
            } else if (this._mode == 'GROUP') {
                this.iam
                    .searchGroups(query, true, '', '', request)
                    .subscribe(async (data: IamGroups) => {
                        this.dataSource.setPagination(data.pagination);
                        await this.dataSource.appendData(data.groups as Group[]);
                        this.dataSource.isLoading = false;
                    });
            } else if (this._mode == 'USER') {
                this.iam.searchUsers(query, true, '', request).subscribe(async (data: IamUsers) => {
                    this.dataSource.setPagination(data.pagination);
                    await this.dataSource.appendData(data.users as unknown as User[]);
                    this.dataSource.isLoading = false;
                });
            }
        }
    }

    private editAuthority(data: any) {
        const list = this.getList(data);

        if (this._mode == 'ORG') {
            this.node.getNodeParents(list[0].sharedFolder.id, true).subscribe(
                (data: NodeList) => {
                    this.edit = Helper.deepCopy(list[0]);
                    data.nodes = data.nodes.reverse().slice(1);
                    this.edit.folderPath = data.nodes;
                    this.breadcrumbsService.setNodePath(data.nodes);
                    this.editId = this.edit.authorityName;
                },
                (error: any) => this.toast.error(error),
            );
        } else if (this._mode == 'USER') {
            this.iam.getUser(list[0].authorityName).subscribe((user) => {
                this.edit = user.person;
                this.edit.profile.sizeQuota = user.person.quota.sizeQuota / 1024 / 1024;
                this.editId = this.edit.authorityName;
                this.primaryAffiliationList = this.edit.profile.primaryAffiliation
                    ? this.PRIMARY_AFFILIATIONS.indexOf(this.edit.profile.primaryAffiliation) != -1
                    : true;
            });
        } else {
            this.edit = Helper.deepCopy(list[0]);
            this.editId = this.edit.authorityName;
        }
        this.updateButtons();
    }
    private addToGroup(data: any) {
        const list = this.getList(data);

        this.addTo = list;
        this.addToSelection = null;
        this.uiService
            .waitForComponent(this, 'addToComponent')
            .subscribe(() => this.addToComponent.loadAuthorities());
    }
    private openEditGroups(data: User) {
        const list = this.getList(data);
        this.editGroups = list[0];
        this.initMembersList();
        this.manageMemberSearch = '';
        this.memberList.reset();
        this.searchMembers();
    }
    addToSelect() {
        this.addToList = this.nodeEntries.getSelection().selected;
        this.addToSingle();
    }
    private addToSingle(callback: Function = null, position = 0, groupPosition = 0, errors = 0) {
        if (position == this.addToList.length) {
            if (groupPosition < this.addToSelection.length - 1) {
                this.addToSingle(callback, 0, groupPosition + 1, errors);
            } else {
                if (groupPosition == 0) {
                    if (errors)
                        this.showToast('PERMISSIONS.USER_ADDED_FAILED', {
                            count: position - errors,
                            error: errors,
                            group: (this.addToSelection[0] as Group).profile.displayName,
                        });
                    else
                        this.showToast('PERMISSIONS.USER_ADDED_TO_GROUP', {
                            count: position,
                            group: (this.addToSelection[0] as Group).profile.displayName,
                        });
                } else {
                    const count = this.addToList.length * this.addToSelection.length;
                    if (errors)
                        this.showToast('PERMISSIONS.USER_ADDED_FAILED_MULTI', {
                            count: count - errors,
                            error: errors,
                        });
                    else
                        this.showToast('PERMISSIONS.USER_ADDED_TO_GROUP_MULTI', {
                            count,
                        });
                }

                this.addTo = null;
                this.toast.closeProgressSpinner();
                if (callback) callback();
            }
            return;
        }
        this.toast.showProgressSpinner();
        this.iam
            .addGroupMember(
                this.addToSelection[groupPosition].authorityName,
                this.addToList[position].authorityName,
            )
            .subscribe(
                () => {
                    this.addToSingle(callback, position + 1, groupPosition, errors);
                },
                (error: any) => {
                    if (error.status == RestConstants.DUPLICATE_NODE_RESPONSE) {
                        errors++;
                    } else {
                        this.toast.error(error);
                    }
                    this.addToSingle(callback, position + 1, groupPosition, errors);
                },
            );
    }
    private async deleteAuthority(data: any, callback: Function) {
        const list = this.getList(data);
        if (
            this._mode == 'GROUP' &&
            list.filter((l) => l.groupType == RestConstants.GROUP_TYPE_ADMINISTRATORS).length
        ) {
            this.toast.error(null, 'PERMISSIONS.DELETE_ERROR_ADMINISTRATORS');
            return;
        }
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'PERMISSIONS.DELETE_TITLE',
            message: 'PERMISSIONS.DELETE_' + this._mode + (list.length === 1 ? '_SINGLE' : ''),
            messageParameters: {
                name: this._mode == 'USER' ? list[0].authorityName : list[0].profile.displayName,
            },
            buttons: DELETE_OR_CANCEL,
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES_DELETE') {
                callback(list);
            }
        });
    }
    private refresh() {
        this.dataSource.reset();
        this.nodeEntries.getSelection().clear();
        this.optionsHelperService.refreshComponents();
        this.loadAuthorities();
    }

    private closeDialog() {
        this.toast.closeProgressSpinner();
    }

    private startDelete(data: any, position = 0, error = false) {
        this.closeDialog();
        if (position == data.length) {
            this.toast.closeProgressSpinner();
            this.refresh();
            if (!error) this.toast.toast('PERMISSIONS.DELETED_' + this._mode);
            return;
        }
        this.toast.showProgressSpinner();
        if (this._mode == 'USER') {
            console.error('delete for user does not exists');
        } else {
            this.iam.deleteGroup(data[position].authorityName).subscribe(
                () => this.startDelete(data, position + 1, error),
                (error: any) => {
                    this.toast.error(error);
                    this.startDelete(data, position + 1, true);
                },
            );
        }
    }
    private startExclude(data: any, position = 0) {
        this.closeDialog();
        if (position == data.length) {
            this.toast.closeProgressSpinner();
            this.refresh();
            this.toast.toast('PERMISSIONS.DELETED_' + this._mode);
            return;
        }
        this.toast.showProgressSpinner();
        this.organization.removeMember(this.org.groupName, data[position].authorityName).subscribe(
            () => this.startExclude(data, position + 1),
            (error: any) => this.toast.error(error),
        );
    }
    private createAuthority() {
        this.edit = { profile: {} };
        this.editDetails = {};
        this.editId = null;
        this.updateButtons();
    }
    private createGroup() {
        this.createAuthority();
        this.edit.profile.groupType = null;
        this.edit.profile.scopeType = null;
    }
    private createOrg() {
        this.createGroup();
    }

    private addMembersFunction(data: any) {
        if (data === 'ALL') this.addMembers = this.org;
        else {
            const list = this.getList(data);
            this.addMembers = list[0];
        }
        this.initMembersList();
        this.manageMemberSearch = '';
        this.searchMembers();
    }
    private manageMembers(data: any) {
        if (data === 'ALL') this.editMembers = this.org;
        else {
            const list = this.getList(data);
            this.editMembers = list[0];
        }
        this.initMembersList();
        this.manageMemberSearch = '';
        this.searchMembers();
    }

    private deleteMember(list: any[], position = 0) {
        if (list.length === position) {
            this.toast.toast('PERMISSIONS.MEMBER_REMOVED');
            this.memberOptions.addOptions = this.getMemberOptions();
            this.memberList.reset();
            this.searchMembers();
            this.toast.closeProgressSpinner();
            return;
        }
        this.toast.showProgressSpinner();
        this.iam
            .deleteGroupMember(
                this.editMembers === 'ALL'
                    ? this.org.authorityName
                    : (this.editMembers as Group).authorityName,
                list[position].authorityName,
            )
            .subscribe(
                () => {
                    this.deleteMember(list, position + 1);
                },
                (error: any) => this.toast.error(error),
            );
    }
    private deleteMembership(list: any[], position = 0) {
        if (list.length === position) {
            this.toast.toast('PERMISSIONS.MEMBERSHIP_REMOVED');
            this.memberOptions.addOptions = this.getMemberOptions();
            this.memberList.reset();
            this.searchMembers();
            this.toast.closeProgressSpinner();
            return;
        }
        this.toast.showProgressSpinner();
        this.iam
            .deleteGroupMember(list[position].authorityName, this.editGroups.authorityName)
            .subscribe(
                () => {
                    this.deleteMembership(list, position + 1);
                },
                (error: any) => this.toast.error(error),
            );
    }
    searchMembers() {
        this.memberOptions.addOptions = this.getMemberOptions();
        this.memberList.reset();
        this.nodeMemberAdd.getSelection().clear();
        this.refreshMemberList();
    }
    refreshMemberList() {
        this.memberList.isLoading = true;
        if (this.addMembers) {
            if (this.org && this.addMembers.authorityName != this.org.authorityName) {
                const request: any = {
                    sortBy: ['authorityName'],
                    offset: this.memberList.getData().length,
                };
                this.iam
                    .getGroupMembers(
                        this.org.authorityName,
                        this.manageMemberSearch,
                        RestConstants.AUTHORITY_TYPE_USER,
                        request,
                    )
                    .subscribe(async (data: IamAuthorities) => {
                        this.memberList.setPagination(data.pagination);
                        await this.memberList.appendData(data.authorities as unknown as User[]);
                        this.memberList.isLoading = false;
                    });
            } else {
                const request: any = {
                    sortBy: ['firstName'],
                    offset: this.memberList.getData().length,
                };
                this.iam
                    .searchUsers(this.manageMemberSearch, true, '', request)
                    .subscribe(async (data) => {
                        this.memberList.setPagination(data.pagination);
                        await this.memberList.appendData(data.users as unknown as User[]);
                        this.memberList.isLoading = false;
                    });
            }
        } else if (this.editGroups) {
            const request: any = {
                sortBy: ['authorityName'],
                offset: this.memberList.getData().length,
            };
            this.iam
                .getUserGroups(this.editGroups.authorityName, this.manageMemberSearch, request)
                .subscribe(async (data) => {
                    this.memberList.setPagination(data.pagination);
                    await this.memberList.appendData(data.groups as Group[]);
                    this.memberList.isLoading = false;
                });
        } else {
            const request: any = {
                sortBy: ['authorityName'],
                offset: this.memberList.getData().length,
            };
            this.iam
                .getGroupMembers(
                    (this.editMembers as Group).authorityName,
                    this.manageMemberSearch,
                    RestConstants.AUTHORITY_TYPE_USER,
                    request,
                )
                .subscribe(async (data) => {
                    this.memberList.setPagination(data.pagination);
                    await this.memberList.appendData(data.authorities as unknown as User[]);
                    this.memberList.isLoading = false;
                });
        }
        this.updateButtons();
    }

    private handleError(error: any) {
        if (error.status == RestConstants.DUPLICATE_NODE_RESPONSE)
            this.toast.error(null, 'PERMISSIONS.USER_EXISTS_IN_GROUP');
        else this.toast.error(error);
    }

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.key == 'Escape') {
            if (this.addTo) {
                this.addTo = null;
                return;
            }
            if (this.edit) {
                this.edit = null;
                return;
            }
            if (this.editMembers) {
                this.cancelEditMembers();
                return;
            }
        }
    }

    private deleteOrg(list: any) {
        this.toast.showProgressSpinner();
        const org = list[0];
        this.organization.deleteOrganization(org.authorityName).subscribe(
            () => {
                this.toast.toast('PERMISSIONS.ORG_REMOVED');
                this.toast.closeProgressSpinner();
                this.closeDialog();
                this.refresh();
            },
            (error: any) => {
                this.toast.error(error);
                this.toast.closeProgressSpinner();
                this.refresh();
            },
        );
    }
    deselectOrg() {
        this.onDeselectOrg.emit();
        setTimeout(() => this.refresh());
    }
    setOrgTab() {
        this.setTab.emit(0);
    }

    private downloadMembers() {
        const headers = this.columns.map((c) => this.translate.instant(this._mode + '.' + c.name));
        const data: string[][] = [];
        for (const entry of this.dataSource.getData() as User[]) {
            data.push([
                entry.authorityName,
                entry.profile.firstName,
                entry.profile.lastName,
                entry.profile.email,
                entry.status.status,
            ]);
        }
        CsvHelper.download(
            this.translate.instant('PERMISSIONS.DOWNLOAD_MEMBER_FILENAME'),
            headers,
            data,
        );
    }
    openFolder(folder: SharedFolder) {
        UIHelper.goToWorkspaceFolder(
            this.node,
            this.router,
            this.connector.getCurrentLogin(),
            folder.id,
        );
    }

    private updateButtons() {
        this.editButtons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancelEdit()),
            new DialogButton('SAVE', { color: 'primary' }, () => this.saveEdits()),
        ];
        /**
         *
         <div class="card-action" *ngIf="editMembers">
         <a class="waves-effect waves-light btn" (click)="cancelEditMembers()">{{'CLOSE' | translate }}</a>
         </div>
         <div class="card-action" *ngIf="addMembers">
         <a class="waves-effect waves-light btn" [class.disabled]="selectedMembers.length==0" (click)="addMembersToGroup()">{{'ADD' | translate }}</a>
         <a class="waves-effect waves-light btn-flat" (click)="cancelEditMembers()">{{'CLOSE' | translate }}</a>
         </div>
         </div>
         */
        const add = new DialogButton('ADD', { color: 'primary' }, () => this.addMembersToGroup());
        add.disabled = this.nodeMemberAdd?.getSelection()?.isEmpty();
        this.memberButtons = [
            new DialogButton('CLOSE', { color: 'standard' }, () => this.cancelEditMembers()),
        ];
        if (this.addMembers) {
            this.memberButtons.push(add);
        }
        this.editStatusButtons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => {
                this.editStatus = null;
            }),
            new DialogButton('SAVE', { color: 'primary' }, () => this.savePersonStatus()),
        ];
        this.resetPasswordButtons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => {
                this.editStatus = null;
            }),
            new DialogButton('SAVE', { color: 'primary' }, () => this.savePersonPassword()),
        ];
        this.signupButtons = DialogButton.getSaveCancel(
            () => (this.groupSignupDetails = null),
            () => this.saveGroupSignup(),
        );
        this.signupListButtons = [
            new DialogButton(
                'CLOSE',
                { color: 'standard' },
                () => (this.groupSignupListShown = null),
            ),
        ];
    }
    private setPersonStatus(data: User) {
        this.editStatus = data;
        this.updateButtons();
    }
    private resetPersonPassword(data: User) {
        this.resetPassword = data;
        this.newPassword = '';
        this.updateButtons();
    }

    private savePersonStatus() {
        this.toast.showProgressSpinner();
        this.iam
            .updateUserStatus(
                this.editStatus.authorityName,
                this.editStatus.status.status,
                this.editStatusNotify,
            )
            .subscribe(
                () => {
                    this.toast.closeProgressSpinner();
                    this.editStatus = null;
                },
                (error) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }
    private savePersonPassword() {
        if (!this.newPassword || this.passwordRef.passwordStrength === 'weak') {
            this.toast.error(null, 'PERMISSIONS.ERROR_PASSWORD_TO_WEAK');
            return;
        }
        this.toast.showProgressSpinner();
        this.iam
            .setCredentials(
                {
                    newPassword: this.newPassword,
                },
                this.resetPassword.authorityName,
            )
            .subscribe(
                () => {
                    this.toast.closeProgressSpinner();
                    this.resetPassword = null;
                    this.toast.show({
                        message: 'PERMISSIONS.TOAST_PASSWORD_RESET',
                        type: 'info',
                        subtype: ToastType.InfoAction,
                    });
                },
                (error) => {
                    this.toast.closeProgressSpinner();
                    this.toast.error(error);
                },
            );
    }
    saveGroupSignup() {
        this.toast.showProgressSpinner();
        if (this.groupSignupDetails.signupMethod === 'disabled') {
            this.groupSignupDetails.signupMethod = null;
        }
        this.iam.editGroupSignup(this.groupSignup.authorityName, this.groupSignupDetails).subscribe(
            () => {
                this.groupSignupDetails = null;
                this.refresh();
                this.toast.toast('PERMISSIONS.ORG_SIGNUP_SAVED');
                this.toast.closeProgressSpinner();
            },
            (error) => {
                this.toast.error(error);
                this.toast.closeProgressSpinner();
            },
        );
    }

    getLink(mode: 'routerLink' | 'queryParams', folder: Node) {
        return this.nodeHelper.getNodeLink(mode, folder);
    }

    onClick(event: NodeClickEvent<GenericAuthority>) {
        if (this._mode === 'ORG') {
            if (!this.nodeEntries.getSelection().isEmpty() && event.element === this._org) {
                this._org = null;
                this.onSelection.emit(null);
                this.nodeEntries.getSelection().clear();
                return;
            }
            this._org = event.element as Organization;
        }
        console.log(event, this._mode);
        this.nodeEntries.getSelection().clear();
        this.nodeEntries.getSelection().select(event.element);
        this.onSelection.emit(this.nodeEntries.getSelection().selected);
    }

    private updateColumns() {
        if (this._mode == 'USER') {
            this.sortConfig.columns = [
                new ListItemSort('USER', 'authorityName'),
                new ListItemSort('USER', 'firstName'),
                new ListItemSort('USER', 'lastName'),
                new ListItemSort('USER', 'email'),
                new ListItemSort('USER', 'status'),
            ];
        } else {
            this.sortConfig.columns = [
                new ListItemSort('GROUP', 'displayName'),
                new ListItemSort('GROUP', 'groupType'),
            ];
        }
        this.sortConfig.active = this.sortConfig.columns[0].name;
        this.columns = this.getColumns(this._mode, this.embedded);
        this.addMemberColumns = this.getColumns('USER', true);
        this.editGroupColumns = this.getColumns('GROUP', true);
        // will be called by searchQuery
        // this.loadAuthorities();
    }

    selectOnClick(
        source: NodeEntriesWrapperComponent<GenericAuthority>,
        event: NodeClickEvent<GenericAuthority>,
    ) {
        source.getSelection().toggle(event.element);
    }

    private async initMembersList() {
        this.ref.tick();
        await this.nodeMemberAdd?.initOptionsGenerator({
            actionbar: this.actionbarMember,
            customOptions: this.memberOptions,
        });
        this.nodeMemberAdd.getSelection().changed.subscribe(() => this.updateButtons());
    }

    private showToast(message: string, translationParams: any) {
        this.toast.toast(message, translationParams, null, null, null, {
            message,
            type: 'info',
            subtype: ToastType.InfoData,
            html: true,
        });
    }
}
