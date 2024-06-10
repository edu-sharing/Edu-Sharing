import {
    AfterViewInit,
    Component,
    Input,
    OnDestroy,
    OnInit,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { GenericAuthority } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    CustomOptions,
    DefaultGroups,
    ElementType,
    FetchEvent,
    InteractionType,
    ListItem,
    ListItemSort,
    ListSortConfig,
    NodeClickEvent,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OptionItem,
    Scope,
} from 'ngx-edu-sharing-ui';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
    ArchiveRestore,
    Node,
    RestArchiveService,
    RestConstants,
    RestoreResult,
    TemporaryStorageService,
} from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { Closable } from '../../../features/dialogs/card-dialog/card-dialog-config';
import {
    CLOSE,
    DELETE_OR_CANCEL,
} from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { SearchFieldService } from '../../../main/navigation/search-field/search-field.service';

type RestoreResults = {
    results: (RestoreResult & { message: string })[];
} & {
    hasDuplicateNames: boolean;
    hasParentFolderMissing: boolean;
};

@Component({
    selector: 'es-recycle',
    templateUrl: 'recycle.component.html',
    styleUrls: ['recycle.component.scss'],
})
export class RecycleMainComponent implements OnInit, AfterViewInit, OnDestroy {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    @ViewChild('list') list: NodeEntriesWrapperComponent<Node>;
    @ViewChild('confirmDeleteDialog') confirmDeleteDialog: TemplateRef<unknown>;
    @ViewChild('restoreDialog') restoreDialog: TemplateRef<unknown>;
    dataSource = new NodeDataSource();

    @Input() actionbar: ActionbarComponent;

    public columns: ListItem[] = [
        new ListItem('NODE', RestConstants.CM_NAME),
        new ListItem('NODE', RestConstants.CM_ARCHIVED_DATE),
    ];
    public options: CustomOptions = {
        useDefaultOptions: false,
        addOptions: [],
    };
    sort: ListSortConfig = {
        columns: [
            new ListItemSort('NODE', RestConstants.CM_NAME),
            new ListItemSort('NODE', RestConstants.CM_ARCHIVED_DATE),
        ],
        allowed: true,
        active: RestConstants.CM_ARCHIVED_DATE,
        direction: 'desc',
    };
    searchQuery: string;
    skipDeleteConfirmation = false;
    loadData(currentQuery: string, offset: number, sortBy: string, sortAscending: boolean) {
        return this.archive.search(currentQuery || '*', '', {
            propertyFilter: [RestConstants.ALL],
            offset: offset,
            sortBy: [sortBy],
            sortAscending: sortAscending,
        });
    }
    private destroyed = new Subject<void>();

    constructor(
        private archive: RestArchiveService,
        private toast: Toast,
        private translate: TranslateService,
        private service: TemporaryStorageService,
        private searchField: SearchFieldService,
        private dialogs: DialogsService,
    ) {}

    ngOnInit(): void {
        const restoreOption = new OptionItem(
            'RECYCLE.OPTION.RESTORE_SINGLE',
            'undo',
            (node: Node) => this.restoreSingle(node),
        );
        restoreOption.group = DefaultGroups.Primary;
        const deleteOption = new OptionItem(
            'RECYCLE.OPTION.DELETE_SINGLE',
            'delete',
            (node: Node) => this.deleteSingle(node),
        );
        deleteOption.group = DefaultGroups.Primary;
        this.options.addOptions.push(restoreOption);
        this.options.addOptions.push(deleteOption);
        this.options.addOptions.forEach((o) => {
            o.elementType = [ElementType.Node, ElementType.NodePublishedCopy];
        });
        this.searchField
            .observeCurrentInstance()
            .pipe(takeUntil(this.destroyed))
            .subscribe((instance) =>
                instance
                    .onSearchTriggered()
                    .pipe(takeUntil(this.destroyed))
                    .subscribe(({ searchString }) => {
                        this.searchQuery = searchString;
                        this.refresh();
                    }),
            );
    }

    ngAfterViewInit(): void {
        this.refresh();
        this.list.initOptionsGenerator({
            actionbar: this.actionbar,
            customOptions: this.options,
        });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private async openConfirmDeleteDialog(nodes: Node[]): Promise<void> {
        this.skipDeleteConfirmation = false; // reset
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'RECYCLE.DELETE.CONFIRM_TITLE',
            buttons: DELETE_OR_CANCEL,
            closable: Closable.Standard,
            contentTemplate: this.confirmDeleteDialog,
            nodes,
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES_DELETE') {
                if (this.skipDeleteConfirmation) {
                    this.service.set('recycleSkipDeleteConfirmation', true);
                }
                this.deleteNodesWithoutConfirmation(nodes);
            }
        });
    }

    private restoreFinished(list: Node[], restoreResult: any) {
        this.toast.closeProgressSpinner();

        this.prepareResults(restoreResult);
        if (restoreResult.hasDuplicateNames || restoreResult.hasParentFolderMissing) {
            this.openRestoreDialog(restoreResult);
        }
        if (list.length == 1) {
            this.toast.toast('RECYCLE.TOAST.RESTORE_FINISHED_SINGLE');
        } else {
            this.toast.toast('RECYCLE.TOAST.RESTORE_FINISHED');
        }
        this.refresh();
    }

    private openRestoreDialog(restoreResults: RestoreResults): void {
        void this.dialogs.openGenericDialog({
            title: 'RECYCLE.RESTORE.NEW_LOCATION_TITLE',
            subtitle: 'RECYCLE.RESTORE.NEW_LOCATION_SUBTITLE',
            avatar: { kind: 'icon', icon: 'undo' },
            buttons: CLOSE,
            closable: Closable.Standard,
            contentTemplate: this.restoreDialog,
            context: { $implicit: restoreResults },
        });
    }

    private delete(): void {
        this.deleteNodes(this.list.getSelection().selected);
    }

    private deleteSingle(node: Node): void {
        if (node == null) {
            this.delete();
            return;
        }
        this.deleteNodes([node]);
    }

    private deleteNodesWithoutConfirmation(nodes: Node[]) {
        this.toast.showProgressSpinner();
        this.archive.delete(nodes).subscribe(
            () => this.deleteFinished(),
            (error) => this.handleErrors(error),
        );
    }

    private deleteFinished() {
        this.toast.closeProgressSpinner();
        this.toast.toast('RECYCLE.TOAST.DELETE_FINISHED');
        this.list.getSelection().clear();
        this.refresh();
    }
    private deleteNodes(list: Node[]) {
        if (this.service.get('recycleSkipDeleteConfirmation', false)) {
            this.deleteNodesWithoutConfirmation(list);
            return;
        }

        void this.openConfirmDeleteDialog([...list]);
    }

    public restoreNodes(list: Node[], toPath = '') {
        // archiveRestore list
        this.toast.showProgressSpinner();
        this.archive.restore(list, toPath).subscribe(
            (result: ArchiveRestore) => this.restoreFinished(list, result),
            (error: any) => this.handleErrors(error),
        );
    }
    private handleErrors(error: any) {
        this.toast.error(error);
        this.toast.closeProgressSpinner();
    }

    private restoreSingle(node: Node): void {
        if (node == null) {
            this.restore();
            return;
        }

        this.restoreNodes([node]);
    }
    private restore(): void {
        this.restoreNodes(this.list.getSelection().selected);
    }

    async refresh(event?: FetchEvent) {
        this.dataSource.isLoading = true;
        if (event == null) {
            this.dataSource.reset();
            this.list?.getSelection().clear();
        }
        const result = await this.loadData(
            this.searchQuery,
            event ? event?.offset || this.dataSource.getData().length : 0,
            this.sort.active,
            this.sort.direction === 'asc',
        ).toPromise();
        if (event == null) {
            this.dataSource.setData(result.nodes, result.pagination);
        } else {
            this.dataSource.appendData(result.nodes);
            this.dataSource.setPagination(result.pagination);
        }
        this.dataSource.isLoading = false;
    }

    click(event: NodeClickEvent<Node | GenericAuthority>) {
        this.list.getSelection().toggle(event.element as Node);
    }

    updateSort(sort: ListSortConfig) {
        this.sort = sort;
        this.refresh();
    }

    private prepareResults(results: RestoreResults) {
        for (const result of results.results) {
            if (result.restoreStatus === 'FINE') {
                continue;
            }
            this.translate
                .get('RECYCLE.RESTORE.' + result.restoreStatus)
                .subscribe((text) => (result.message = text));
            if (result.restoreStatus === 'DUPLICATENAME') {
                results.hasDuplicateNames = true;
            }
            if (
                result.restoreStatus === 'FALLBACK_PARENT_NOT_EXISTS' ||
                result.restoreStatus === 'FALLBACK_PARENT_NO_PERMISSION'
            ) {
                results.hasParentFolderMissing = true;
            }
        }
    }
}
