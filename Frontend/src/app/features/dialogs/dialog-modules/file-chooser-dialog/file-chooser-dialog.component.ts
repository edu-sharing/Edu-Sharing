import { AfterViewInit, Component, Inject, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { MatTabChangeEvent } from '@angular/material/tabs';
import { TranslateService } from '@ngx-translate/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { distinctUntilChanged, map, skip, switchMap, takeUntil, tap } from 'rxjs/operators';
import {
    DialogButton,
    NodeList,
    RestCollectionService,
    RestConnectorService,
    RestConstants,
    RestNodeService,
} from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { Node } from 'ngx-edu-sharing-api';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { WorkspaceExplorerComponent } from '../../../../pages/workspace-page/explorer/explorer.component';
import { CARD_DIALOG_DATA, CardDialogConfig } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { FileChooserDialogData, FileChooserDialogResult } from './file-chooser-dialog-data';
import { BreadcrumbsService } from '../../../../shared/components/breadcrumbs/breadcrumbs.service';
import {
    InteractionType,
    ListItem,
    ListSortConfig,
    NodeDataSource,
    NodeEntriesDisplayType,
} from 'ngx-edu-sharing-ui';

const SINGLE_COLUMN_WIDTH = 600;
const MULTI_COLUMN_WIDTH = 900;

@Component({
    selector: 'es-file-chooser-dialog',
    templateUrl: './file-chooser-dialog.component.html',
    styleUrls: ['./file-chooser-dialog.component.scss'],
    providers: [BreadcrumbsService],
})
export class FileChooserDialogComponent implements OnInit, AfterViewInit {
    @ViewChild('bottomBarContent') bottomBarContent: TemplateRef<HTMLElement>;

    readonly InteractionType = InteractionType;
    /**
     * Path to show as breadcrumbs, beginning with the first item after the current home directory.
     */
    readonly path$ = new BehaviorSubject<Node[]>([]);
    isLoading: boolean;
    columns: ListItem[] = [];
    sort: ListSortConfig;
    selectedFiles: Node[] = [];
    displayType = NodeEntriesDisplayType.Table;
    searchMode: boolean;
    searchQuery: string;
    private defaultSubtitleSubject = new BehaviorSubject<string>(null);
    private get defaultSubtitle(): string {
        return this.defaultSubtitleSubject.value;
    }
    private set defaultSubtitle(value: string) {
        this.defaultSubtitleSubject.next(value);
    }
    list = new NodeDataSource<Node>();
    cardIcon: string;
    hasHeading = true;
    tabs = [
        {
            label: 'WORKSPACE.MY_FILES',
            homeIcon: 'person',
            directory: RestConstants.USERHOME,
            canSelectHome: true,
        },
        {
            label: 'WORKSPACE.SHARED_FILES',
            homeIcon: 'group',
            directory: RestConstants.SHARED_FILES,
            canSelectHome: false,
        },
    ];
    /** Replace home icon and -label given by tabs. */
    homeOverride: {
        label: string;
        icon: string;
    } = null;
    private homeDirectory: string;
    private currentDirectory: string;
    canSelectHome: boolean;
    private loadDirectoryTrigger = new Subject<{ directory: string; reset: boolean }>();

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: FileChooserDialogData,
        private dialogRef: CardDialogRef<FileChooserDialogData, FileChooserDialogResult>,
        private connector: RestConnectorService,
        private collectionApi: RestCollectionService,
        private nodeApi: RestNodeService,
        private toast: Toast,
        private breadcrumbsService: BreadcrumbsService,
        private translate: TranslateService,
    ) {
        // http://plnkr.co/edit/btpW3l0jr5beJVjohy1Q?p=preview
        this.columns.push(new ListItem('NODE', RestConstants.CM_NAME));
    }

    ngOnInit(): void {
        this.processDialogData();
        this.registerObservables();
        this.initialize();
        this.setDialogConfig();
        this.defaultSubtitleSubject
            .pipe(
                map((defaultSubtitle) => this.data.subtitle ?? defaultSubtitle),
                distinctUntilChanged(),
            )
            .subscribe((subtitle) => this.dialogRef.patchConfig({ subtitle }));
    }

    ngAfterViewInit(): void {
        this.dialogRef.patchConfig({ customBottomBarContent: this.bottomBarContent });
    }

    private setDialogConfig(): void {
        const config: Partial<CardDialogConfig<FileChooserDialogData>> = {
            title: this.data.title,
            width: SINGLE_COLUMN_WIDTH,
        };
        this.dialogRef.patchConfig(config);
        if (this.cardIcon) {
            config.avatar = { kind: 'icon', icon: this.cardIcon };
        }
    }

    folderIsWritable() {
        return (
            this.path$?.value?.[this.path$?.value?.length - 1]?.access?.indexOf(
                RestConstants.ACCESS_WRITE,
            ) !== -1
        );
    }

    private registerObservables(): void {
        this.loadDirectoryTrigger
            .pipe(switchMap(({ directory, reset }) => this.loadDirectory(directory, reset)))
            .subscribe();
        this.path$.subscribe((path) => {
            this.breadcrumbsService.setNodePath(path);
            this.updateButtons();
        });
    }

    onTabChange(event: MatTabChangeEvent) {
        const tab = this.tabs[event.index];
        this.setHomeDirectory(tab.directory, { canSelectHome: tab.canSelectHome });
    }

    private setHomeDirectory(directory: string, { canSelectHome = false }): void {
        this.homeDirectory = directory;
        // FIXME: confusing naming: `canSelectHome` doesn't relate to `selectHome`.
        this.canSelectHome = canSelectHome;
        this.selectHome();
    }

    onSelection(node: Node[]) {
        this.selectedFiles = node;
    }

    private processDialogData() {
        if (this.data.pickDirectory) {
            this.cardIcon = 'folder';
            this.data.filter.push(RestConstants.FILTER_FOLDERS);
        }
        if (this.data.collections) {
            this.displayType = NodeEntriesDisplayType.SmallGrid;
            this.tabs = null;
            this.setHomeDirectory(RestConstants.ROOT, { canSelectHome: false });
            this.hasHeading = false;
            this.searchMode = true;
            this.searchQuery = '';
            this.columns = UIHelper.getDefaultCollectionColumns();
            this.sort = {
                active: RestConstants.CM_MODIFIED_DATE,
                direction: 'desc',
                columns: [],
            };
        } else {
            this.columns = WorkspaceExplorerComponent.getColumns(this.connector);
            this.columns = this.columns.map((c, i) => {
                c.visible = i === 0;
                return c;
            });
            this.sort = {
                active: this.columns[0].name,
                direction: 'asc',
                allowed: true,
                columns: RestConstants.POSSIBLE_SORT_BY_FIELDS.filter((s) =>
                    this.columns.some((c) => c.name === s.name),
                ),
            };
        }
    }

    private initialize() {
        if (this.homeDirectory) {
            this.viewDirectory(this.homeDirectory);
        } else {
            this.setHomeDirectory(this.tabs[0].directory, {
                canSelectHome: this.tabs[0].canSelectHome,
            });
        }
    }

    hasWritePermissions(node: any) {
        if (node.access.indexOf(RestConstants.ACCESS_WRITE) == -1) {
            return { status: false, message: 'NO_WRITE_PERMISSIONS' };
        }
        return { status: true };
    }

    selectBreadcrumb(position: number) {
        if (position === 0) {
            this.selectHome();
        } else {
            this.selectItem(this.path$.value[position - 1]);
        }
    }

    selectHome() {
        this.homeOverride = null;
        this.path$.next([]);
        this.viewDirectory(this.homeDirectory, true);
    }

    selectItem(event: Node) {
        if (event.isDirectory || this.data.collections) {
            if (this.searchMode) {
                this.selectedFiles = [event];
                this.updateButtons();
                return;
            }
            this.selectedFiles = [];
            if (this.path$.value?.[this.path$.value.length - 1]?.ref.id === event.parent.id) {
                // We selected the direct child of our previous node. Can update the path in place.
                this.path$.next([...this.path$.value, event]);
            } else if (this.path$.value?.some((ancestor) => ancestor.ref.id === event.ref.id)) {
                // We selected an ancestor of our previous node. Can update the path in place.
                const index = this.path$.value.findIndex(
                    (ancestor) => ancestor.ref.id === event.ref.id,
                );
                this.path$.next(this.path$.value.slice(0, index + 1));
            } else {
                // Couldn't update the path in place, load from backend.
                if (this.path$.value.length === 0) {
                    // We probably just selected a child of the home directory. Optimistically
                    // update accordingly, but fetch the actual data below anyway.
                    this.path$.next([event]);
                }
                this.updatePathFromBackend(event.ref.id)
                    // Abort when new directory is loaded before we could update the path.
                    .pipe(takeUntil(this.loadDirectoryTrigger.pipe(skip(1))))
                    .subscribe();
            }
            this.viewDirectory(event.ref.id);
        }
    }

    private updatePathFromBackend(id: string) {
        return this.nodeApi.getNodeParents(id).pipe(
            tap((data) => {
                this.path$.next(data.nodes.reverse());
                // When `scope` is not set, we didn't get the children of our home directory but
                // instead a path relative to root. This happens for the admin user.
                if (!data.scope) {
                    this.homeOverride = {
                        label: null,
                        icon: data.scope === 'SHARED_FILES' ? 'group' : 'person',
                    };
                }
            }),
        );
    }

    onSelectionChanged(event: Node[]): void {
        // Triggered in collection mode.
        this.selectedFiles = event;
        this.updateButtons();
    }

    search() {
        this.viewDirectory(this.homeDirectory);
    }

    private viewDirectory(directory: string, reset = true) {
        this.loadDirectoryTrigger.next({ directory, reset });
    }

    private loadDirectory(directory: string, reset = true): Observable<void> {
        this.currentDirectory = directory;
        if (reset) {
            this.list.reset();
            // this.hasMoreToLoad = true; // !this.data.collections; // Collections have no paging
        }
        this.isLoading = true;
        if (this.data.collections) {
            return this.collectionApi
                .search(this.searchQuery, {
                    offset: this.list.getData().length,
                    sortBy: [this.sort.active],
                    sortAscending: this.sort.direction === 'asc',
                    propertyFilter: [RestConstants.ALL],
                })
                .pipe(
                    tap((data) => {
                        const list: Node[] = [];
                        for (const c of data.collections) {
                            const obj: any = c;
                            // dummy for list-table so it recognizes a collection
                            obj.collection = c;
                            list.push(obj);
                        }
                        this.showList({
                            pagination: data.pagination,
                            nodes: list,
                        });
                    }),
                    map(() => {}),
                );
        } else {
            return this.nodeApi
                .getChildren(directory, this.data.filter, {
                    offset: this.list.getData().length,
                    sortBy: [this.sort.active],
                    sortAscending: this.sort.direction === 'asc',
                    propertyFilter: [RestConstants.ALL],
                })
                .pipe(
                    tap((list: NodeList) => {
                        this.showList(list);
                    }),
                    map(() => {}),
                );
        }
    }

    loadMore() {
        if (!this.list.hasMore()) {
            return;
        }
        this.viewDirectory(this.currentDirectory, false);
        this.isLoading = true;
    }

    setSorting(data: ListSortConfig) {
        this.sort = data;
        this.list.reset();
        this.viewDirectory(this.currentDirectory);
    }

    private showList(list: NodeList) {
        this.addToList(list);
        this.updateButtons();
        this.isLoading = false;
    }

    cancel() {
        this.dialogRef.close(null);
    }

    private addToList(list: NodeList) {
        this.isLoading = false;
        if (!list.nodes.length) {
            return;
        }
        this.list.setPagination(list.pagination);
        this.list.appendData(list.nodes);
    }

    private chooseDirectory() {
        (() => {
            if (this.path$.value.length) {
                return rxjs.of(this.path$.value[this.path$.value.length - 1]);
            } else {
                return this.nodeApi
                    .getNodeMetadata(this.homeDirectory)
                    .pipe(map((nodeWrapper) => nodeWrapper.node as Node));
            }
        })().subscribe((node) => {
            if (this.data.collections) {
                if (node.access.indexOf(RestConstants.ACCESS_WRITE) === -1) {
                    this.toast.error(null, 'NO_WRITE_PERMISSIONS');
                    return;
                }
            }
            this.dialogRef.close([node]);
        });
    }

    private chooseFile() {
        if (this.data.collections) {
            if (this.selectedFiles[0].access.indexOf(RestConstants.ACCESS_WRITE) == -1) {
                this.toast.error(null, 'NO_WRITE_PERMISSIONS');
                return;
            }
        }
        this.dialogRef.close(this.selectedFiles);
    }

    updateButtons() {
        const buttons = [
            new DialogButton(this.translate.instant('CANCEL'), { color: 'standard' }, () =>
                this.cancel(),
            ),
        ];
        let confirmButton;
        if (this.data.pickDirectory) {
            if (this.path$.value.length) {
                this.defaultSubtitle = this.path$.value[this.path$.value.length - 1].name;
            } else {
                this.defaultSubtitle = this.translate.instant('SELECT_ROOT_NAME');
            }
            confirmButton = new DialogButton(
                this.translate.instant('APPLY'),
                { color: 'primary' },
                () => this.chooseDirectory(),
            );
            confirmButton.disabled =
                (!this.path$.value.length && !this.canSelectHome) || !this.folderIsWritable();
        } else if (this.data.collections && !this.selectedFiles.length) {
            this.defaultSubtitle = null;
            confirmButton = new DialogButton(
                'SELECT_ROOT_DISABLED',
                { color: 'primary' },
                () => {},
            );
            confirmButton.disabled = true;
        } else if (this.selectedFiles.length) {
            this.defaultSubtitle = this.selectedFiles[0].name;
            confirmButton = new DialogButton(
                this.translate.instant(
                    this.data.collections ? 'SELECT_COLLECTION' : 'SELECT_FILE',
                    {
                        name: this.defaultSubtitle,
                    },
                ),
                { color: 'primary' },
                () => this.chooseFile(),
            );
            confirmButton.disabled =
                this.data.writeRequired &&
                this.hasWritePermissions(this.selectedFiles[0]).status == false;
        }
        if (confirmButton) {
            buttons.push(confirmButton);
        }
        this.dialogRef.patchConfig({ buttons });
    }

    checkColumnState(event: ListItem[]) {
        if (event.filter((i) => i.visible).length > 1) {
            this.dialogRef.patchConfig({ width: MULTI_COLUMN_WIDTH });
        } else {
            this.dialogRef.patchConfig({ width: SINGLE_COLUMN_WIDTH });
        }
    }
}
