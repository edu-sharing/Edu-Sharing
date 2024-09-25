import { Component, EventEmitter, NgZone, Output, ViewChild } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Group, MediacenterService, Node } from 'ngx-edu-sharing-api';
import {
    DefaultGroups,
    ElementType,
    InteractionType,
    ListItem,
    ListSortConfig,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OptionItem,
    Scope,
    SearchHelperService,
    VCard,
} from 'ngx-edu-sharing-ui';
import {
    Mediacenter,
    RequestObject,
    RestConnectorService,
    RestMdsService,
    RestMediacenterService,
    RestSearchService,
} from '../../../core-module/core.module';
import { CsvHelper } from '../../../core-module/csv.helper';
import { Helper } from '../../../core-module/rest/helper';
import { MdsHelperService } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { YES_OR_NO } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { MdsEditorWrapperComponent } from '../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { Values } from '../../../features/mds/types/types';
import { AuthoritySearchMode } from '../../../shared/components/authority-search-input/authority-search-input.component';
import { OptionsHelperService } from '../../../services/options-helper.service';

@Component({
    selector: 'es-admin-mediacenter',
    templateUrl: 'mediacenter.component.html',
    styleUrls: ['mediacenter.component.scss'],
    providers: [OptionsHelperService],
})
export class AdminMediacenterComponent {
    readonly AuthoritySearchMode = AuthoritySearchMode;
    readonly SCOPES = Scope;
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    @ViewChild('mediacenterMds') mediacenterMds: MdsEditorWrapperComponent;
    @ViewChild('nodeEntriesTable') nodeEntriesTable: NodeEntriesWrapperComponent<Node>;
    @ViewChild('groupEntriesTable') groupEntriesTable: NodeEntriesWrapperComponent<Node>;
    @Output() onOpenNode = new EventEmitter<Node>();
    // @TODO: declare the mediacenter type when it is finalized in backend
    mediacenters: any[];
    // original link to mediacenter object (contained in mediacenters[])
    currentMediacenter: Mediacenter;
    // copy of the current mediacenter for (temporary) edits
    currentMediacenterCopy: Mediacenter;

    addGroup: Group;
    mediacenterGroups = new NodeDataSource<Group>();
    mediacenterNodesDataSource = new NodeDataSource<Node>();
    mediacenterNodesSearchWord = '';
    mediacenterNodesSort: ListSortConfig = {
        columns: RestConstants.POSSIBLE_SORT_BY_FIELDS,
        active: RestConstants.LOM_PROP_TITLE,
        direction: 'asc',
        allowed: true,
    };

    groupColumns: ListItem[];
    nodeColumns: ListItem[];
    _currentTab = 0;
    get currentTab() {
        return this._currentTab;
    }
    set currentTab(currentTab: number) {
        this._currentTab = currentTab;
        this.nodeEntriesTable?.initOptionsGenerator({
            customOptions: {
                useDefaultOptions: true,
                supportedOptions: ['OPTIONS.DEBUG', 'OPTIONS.DOWNLOAD'],
            },
        });
        this.groupEntriesTable?.initOptionsGenerator({
            customOptions: {
                useDefaultOptions: false,
            },
        });
        if (this.isAdmin) {
            const remove = new OptionItem(
                'ADMIN.MEDIACENTER.GROUPS.REMOVE',
                'delete',
                async (a: Group) => {
                    const authority = this.optionsHelperService.getObjects(
                        a,
                        this.groupEntriesTable.optionsHelper.getData(),
                    )[0];
                    console.log(authority);
                    const dialogRef = await this.dialogs.openGenericDialog({
                        title: 'ADMIN.MEDIACENTER.GROUPS.REMOVE_TITLE',
                        message: 'ADMIN.MEDIACENTER.GROUPS.REMOVE_MESSAGE',
                        messageParameters: { name: authority.profile.displayName },
                        buttons: YES_OR_NO,
                    });
                    dialogRef.afterClosed().subscribe((response) => {
                        if (response === 'YES') {
                            this.deleteGroup(authority);
                        }
                    });
                },
            );
            remove.elementType = [ElementType.Group];
            remove.group = DefaultGroups.Delete;
            this.groupEntriesTable?.initOptionsGenerator({
                customOptions: {
                    useDefaultOptions: false,
                    addOptions: [remove],
                },
            });
        }
    }
    isAdmin: boolean;
    hasManagePermissions: boolean;
    public mediacentersFile: File;
    public organisationsFile: File;
    public orgMcFile: File;
    public globalProgress = false;

    public removeSchoolsFromMC = false;

    constructor(
        private mediacenterServiceLegacy: RestMediacenterService,
        private mediacenterService: MediacenterService,
        private mdsService: RestMdsService,
        private optionsHelperService: OptionsHelperService,
        private searchHelperService: SearchHelperService,
        private translate: TranslateService,
        private connector: RestConnectorService,
        private ngZone: NgZone,
        private dialogs: DialogsService,
        private toast: Toast,
    ) {
        this.isAdmin = this.connector.getCurrentLogin().isAdmin;
        this.hasManagePermissions = this.connector.hasToolPermissionInstant(
            RestConstants.TOOLPERMISSION_MEDIACENTER_MANAGE,
        );
        this.refresh();
        this.mdsService.getSet().subscribe((mds) => {
            this.nodeColumns = MdsHelperService.getColumns(
                this.translate,
                mds,
                'mediacenterManaged',
            );
            this.groupColumns = MdsHelperService.getColumns(
                this.translate,
                mds,
                'mediacenterGroups',
            );
        });
    }

    setMediacenter(mediacenter: any) {
        this.currentMediacenter = mediacenter;
        this.currentMediacenterCopy = Helper.deepCopy(mediacenter);
        this.mediacenterGroups.reset();
        this.mediacenterGroups.isLoading = true;

        this.resetMediacenterNodes();

        if (mediacenter) {
            this.mediacenterServiceLegacy
                .getManagedGroups(mediacenter.authorityName)
                .subscribe((groups) => {
                    console.log(groups);
                    this.mediacenterGroups.setData(groups as Group[]);
                    this.mediacenterGroups.isLoading = false;
                });
            this.mediacenterNodesDataSource.reset();
            UIHelper.waitForComponent(this.ngZone, this, 'mediacenterMds').subscribe(() =>
                this.mediacenterMds.loadMds(),
            );
            // done via mds
            // this.loadMediacenterNodes();
        }
    }

    async loadMediacenterNodes() {
        if (
            !this.mediacenterNodesDataSource.isEmpty() &&
            !this.mediacenterNodesDataSource.hasMore()
        ) {
            return;
        }
        if (this.currentMediacenter) {
            const licensedNodeReq: RequestObject = {
                offset: this.mediacenterNodesDataSource?.getData()?.length,
                count: this.mediacenterNodesDataSource?.getData()?.length ? 50 : null,
                propertyFilter: [RestConstants.ALL],
                sortBy: [this.mediacenterNodesSort.active],
                sortAscending: [this.mediacenterNodesSort.direction === 'asc'],
            };
            this.mediacenterNodesDataSource.isLoading = true;

            let criteria = await this.getMediacenterNodesCriteria();

            this.mediacenterServiceLegacy
                .getLicensedNodes(
                    this.currentMediacenter.authorityName,
                    criteria,
                    RestConstants.HOME_REPOSITORY,
                    licensedNodeReq,
                )
                .subscribe((data) => {
                    if (
                        this.mediacenterNodesDataSource.isEmpty() ||
                        (this.mediacenterNodesSearchWord != null &&
                            this.mediacenterNodesSearchWord.trim().length > 0)
                    ) {
                        this.mediacenterNodesDataSource.setData(data.nodes, data.pagination);
                    } else {
                        this.mediacenterNodesDataSource.appendData(data.nodes);
                    }
                    this.mediacenterNodesDataSource.isLoading = false;
                });
        }
    }

    private async getMediacenterNodesCriteria() {
        let criteria: any = [];
        if (this.mediacenterNodesSearchWord) {
            criteria.push({
                property: RestConstants.PRIMARY_SEARCH_CRITERIA,
                values: [this.mediacenterNodesSearchWord],
            });
        }
        return criteria.concat(
            this.searchHelperService.convertCritieria(
                await this.mediacenterMds.getValues(),
                this.mediacenterMds.currentWidgets,
            ),
        );
    }

    async searchMediaCenterNodes() {
        this.mediacenterNodesDataSource.reset();
        await this.loadMediacenterNodes();
    }

    removeCatalog(catalog: any) {
        this.currentMediacenterCopy.profile.mediacenter.catalogs.splice(
            this.currentMediacenterCopy.profile.mediacenter.catalogs.indexOf(catalog),
            1,
        );
    }

    addCatalog() {
        if (!this.currentMediacenterCopy.profile.mediacenter.catalogs) {
            this.currentMediacenterCopy.profile.mediacenter.catalogs = [];
        }
        this.currentMediacenterCopy.profile.mediacenter.catalogs.push({ name: '', url: '' });
    }

    async addMediacenter() {
        const dialogRef = await this.dialogs.openInputDialog({
            title: 'ADMIN.MEDIACENTER.ADD_MEDIACENTER_TITLE',
            message: 'ADMIN.MEDIACENTER.ADD_MEDIACENTER_MESSAGE',
            label: 'ADMIN.MEDIACENTER.ADD_MEDIACENTER_LABEL',
        });
        const id = await dialogRef.afterClosed().toPromise();
        if (id) {
            const profile = {
                displayName: this.translate.instant('ADMIN.MEDIACENTER.UNNAMED_MEDIACENTER', {
                    id,
                }),
                mediacenter: {
                    id,
                },
            };
            this.toast.showProgressSpinner();
            this.mediacenterServiceLegacy.addMediacenter(id, profile).subscribe(
                (result) => {
                    RestHelper.waitForResult(
                        () => this.mediacenterServiceLegacy.getMediacenters(),
                        (list: Mediacenter[]) => {
                            return (
                                list.filter((r) => r.authorityName === result.authorityName)
                                    .length === 1
                            );
                        },
                        () => {
                            this.toast.closeProgressSpinner();
                            this.toast.toast('ADMIN.MEDIACENTER.CREATED', { name: id });
                            this.setMediacenter(null);
                            this.refresh();
                        },
                    );
                },
                (error: any) => {
                    this.toast.error(error);
                    this.toast.closeProgressSpinner();
                },
            );
        }
    }

    saveChanges() {
        this.toast.showProgressSpinner();
        this.mediacenterServiceLegacy
            .editMediacenter(
                this.currentMediacenterCopy.authorityName,
                this.currentMediacenterCopy.profile,
            )
            .subscribe(
                () => {
                    this.toast.toast('ADMIN.MEDIACENTER.UPDATED', {
                        name: this.currentMediacenterCopy.profile.displayName,
                    });
                    this.toast.closeProgressSpinner();
                    this.refresh();
                },
                (error: any) => {
                    this.toast.error(error);
                    this.toast.closeProgressSpinner();
                    this.refresh();
                },
            );
    }

    refresh() {
        this.mediacenters = null;
        this.mediacenterServiceLegacy.getMediacenters().subscribe((m) => {
            this.mediacenters = m.filter((m) => m.administrationAccess);
        });
    }

    addCurrentGroup() {
        this.toast.showProgressSpinner();
        this.mediacenterServiceLegacy
            .addManagedGroup(this.currentMediacenterCopy.authorityName, this.addGroup.authorityName)
            .subscribe(
                (groups) => {
                    this.mediacenterGroups.setData(groups as Group[]);
                    this.toast.toast('ADMIN.MEDIACENTER.GROUPS.ADDED', {
                        name: this.addGroup.profile.displayName,
                    });
                    this.toast.closeProgressSpinner();
                    this.addGroup = null;
                },
                (error) => {
                    this.toast.error(error);
                    this.toast.closeProgressSpinner();
                },
            );
    }

    async deleteMediacenter() {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'ADMIN.MEDIACENTER.DELETE_TITLE',
            message: 'ADMIN.MEDIACENTER.DELETE_MESSAGE',
            messageParameters: { name: this.currentMediacenterCopy.profile.displayName },
            buttons: YES_OR_NO,
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES') {
                this.toast.showProgressSpinner();
                this.mediacenterServiceLegacy
                    .deleteMediacenter(this.currentMediacenter.authorityName)
                    .subscribe(
                        () => {
                            this.toast.closeProgressSpinner();
                            this.toast.toast('ADMIN.MEDIACENTER.DELETED', {
                                name: this.currentMediacenterCopy.profile.displayName,
                            });
                            this.setMediacenter(null);
                            this.refresh();
                        },
                        (error: any) => {
                            this.toast.error(error);
                            this.toast.closeProgressSpinner();
                        },
                    );
            }
        });
    }

    private deleteGroup(authority: Group) {
        this.toast.showProgressSpinner();
        this.mediacenterServiceLegacy
            .removeManagedGroup(this.currentMediacenterCopy.authorityName, authority.authorityName)
            .subscribe(
                (groups) => {
                    this.mediacenterGroups.setData(groups as Group[]);
                    this.toast.toast('ADMIN.MEDIACENTER.GROUPS.REMOVED', {
                        name: authority.profile.displayName,
                    });
                    this.toast.closeProgressSpinner();
                },
                (error) => {
                    this.toast.error(error);
                    this.toast.closeProgressSpinner();
                },
            );
    }

    public updateMediacentersFile(event: any) {
        this.mediacentersFile = event.target.files[0];
    }

    public updateOrganisationsFile(event: any) {
        this.organisationsFile = event.target.files[0];
    }

    public updateOrgMcFile(event: any) {
        this.orgMcFile = event.target.files[0];
    }

    public importMediacenters() {
        if (!this.mediacentersFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.IMPORT.CHOOSE_MEDIACENTERS');
            return;
        }
        this.globalProgress = true;
        this.mediacenterServiceLegacy.importMediacenters(this.mediacentersFile).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.MEDIACENTER.IMPORT.IMPORTED', { rows: data.rows });
                this.globalProgress = false;
                this.mediacentersFile = null;
            },
            (error: any) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }

    public importOrganisations() {
        if (!this.organisationsFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.ORGIMPORT.CHOOSE_ORGANISATIONS');
            return;
        }
        this.globalProgress = true;
        this.mediacenterServiceLegacy.importOrganisations(this.organisationsFile).subscribe(
            (data: any) => {
                this.toast.toast('ADMIN.MEDIACENTER.ORGIMPORT.IMPORTED', { rows: data.rows });
                this.globalProgress = false;
                this.organisationsFile = null;
            },
            (error: any) => {
                this.toast.error(error);
                this.globalProgress = false;
            },
        );
    }

    // importMcOrgConnections
    public importOrgMc() {
        if (!this.orgMcFile) {
            this.toast.error(null, 'ADMIN.MEDIACENTER.ORG_MC_CONNECT.CHOOSE');
            return;
        }
        this.globalProgress = true;
        this.mediacenterServiceLegacy
            .importMcOrgConnections(this.orgMcFile, this.removeSchoolsFromMC)
            .subscribe(
                (data: any) => {
                    this.toast.toast('ADMIN.MEDIACENTER.ORG_MC_CONNECT.IMPORTED', {
                        rows: data.rows,
                    });
                    this.globalProgress = false;
                    this.orgMcFile = null;
                },
                (error: any) => {
                    this.toast.error(error);
                    this.globalProgress = false;
                },
            );
    }

    setMediacenterNodesSort(sort: ListSortConfig) {
        this.mediacenterNodesSort = sort;
        this.resetMediacenterNodes();
        this.loadMediacenterNodes();
    }

    private resetMediacenterNodes() {
        this.mediacenterNodesDataSource.reset();
    }

    async exportNodes() {
        const properties = this.nodeColumns
            .map((c) => c.name)
            .filter((n) => n !== 'ccm:mediacenter');
        const propertiesLabel = properties.map((p) => this.translate.instant('NODE.' + p));
        this.toast.showProgressSpinner();
        const data = (await this.mediacenterService
            .exportMediacenterLicensedNodes({
                repository: RestConstants.HOME_REPOSITORY,
                mediacenter: this.currentMediacenter.authorityName,
                sortProperties: [this.mediacenterNodesSort.active],
                sortAscending: [this.mediacenterNodesSort.direction === 'asc'],
                body: {
                    criteria: await this.getMediacenterNodesCriteria(),
                },
                properties,
            })
            .toPromise()) as unknown as any[];
        this.toast.closeProgressSpinner();
        data.forEach((d) => {
            Object.keys(d)
                .filter((c) => RestConstants.getAllVCardFields().includes(c))
                .forEach((c) => {
                    d[c] = d[c]?.map((d2: string) => new VCard(d2).getDisplayName());
                });
        });
        CsvHelper.download(
            await this.translate.get('ADMIN.MEDIACENTER.NODES.CSV_FILENAME').toPromise(),
            propertiesLabel,
            data as unknown as Values,
            properties,
        );
    }
}
