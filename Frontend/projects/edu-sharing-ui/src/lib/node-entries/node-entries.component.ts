import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    OnDestroy,
    OnInit,
    Optional,
    ViewChild,
} from '@angular/core';
import { MatPaginator, PageEvent } from '@angular/material/paginator';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';
import { first, takeUntil } from 'rxjs/operators';
import { NodeEntriesDataType, NodeEntriesDisplayType } from './entries-model';
import { NodeEntriesGlobalService } from './node-entries-global.service';
import { NodeEntriesTemplatesService } from './node-entries-templates.service';
import { NodeEntriesService } from '../services/node-entries.service';
import { KeyboardShortcutsService } from '../services/abstract/keyboard-shortcuts.service';
import { NodeDataSourceRemote } from './node-data-source-remote';

@Component({
    selector: 'es-node-entries',
    templateUrl: 'node-entries.component.html',
    styleUrls: ['node-entries.component.scss'],
})
export class NodeEntriesComponent<T extends NodeEntriesDataType>
    implements OnInit, AfterViewInit, OnDestroy
{
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;

    @ViewChild(MatPaginator) paginator: MatPaginator;

    private readonly destroyed = new Subject<void>();

    constructor(
        public changeDetectorRef: ChangeDetectorRef,
        public entriesGlobalService: NodeEntriesGlobalService,
        public entriesService: NodeEntriesService<T>,
        public templatesService: NodeEntriesTemplatesService,
        @Optional() private globalKeyboardShortcuts: KeyboardShortcutsService,
        private route: ActivatedRoute,
        private translate: TranslateService,
    ) {}

    ngOnInit(): void {
        if (this.entriesService.primaryInstance) {
            this.registerGlobalKeyboardShortcuts();
        }
        if (this.entriesService.dataSource instanceof NodeDataSourceRemote) {
            // We don't require `sort` to be defined, but if it is set but not yet ready (`null`),
            // we wait for its value before initializing the data source, so the first request is
            // sent with the correct sort configuration. That is why we explicitly do not drop
            // `undefined` with the `first` operator below.
            this.entriesService.sortSubject
                .pipe(first((sort) => sort !== null))
                .subscribe(() =>
                    this.initRemoteDataSource(
                        this.entriesService.dataSource as NodeDataSourceRemote<T>,
                    ),
                );
        }
    }

    ngAfterViewInit() {
        if (this.paginator) {
            void this.initPaginator(this.paginator);
            this.changeDetectorRef.detectChanges();
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerGlobalKeyboardShortcuts() {
        this.globalKeyboardShortcuts.register(
            [
                {
                    modifiers: ['Ctrl/Cmd'],
                    keyCode: 'KeyA',
                    ignoreWhen: (event) =>
                        // SmallGrid doesn't support selection
                        this.entriesService.displayType === NodeEntriesDisplayType.SmallGrid,
                    callback: () => this.entriesService.toggleSelectAll(),
                },
            ],
            { until: this.destroyed },
        );
    }

    private initRemoteDataSource(dataSource: NodeDataSourceRemote<T>): void {
        const pageSize = this.entriesGlobalService.getPaginatorSizeOptions(
            this.entriesService.scope,
        )[0];
        dataSource.init({
            paginationConfig: {
                defaultPageSize: pageSize,
                strategy: this.entriesService.paginationStrategy,
            },
            defaultSort: this.entriesService.sort,
        });
        if (this.entriesService.primaryInstance) {
            // Automatic query-params handling is only supported by node-data-source-remote.
            dataSource.registerQueryParameters(this.route);
        }
    }

    private async initPaginator(paginator: MatPaginator) {
        paginator._intl.itemsPerPageLabel = await this.translate
            .get('PAGINATOR.itemsPerPageLabel')
            .toPromise();
        paginator._intl.nextPageLabel = await this.translate
            .get('PAGINATOR.nextPageLabel')
            .toPromise();
        paginator._intl.previousPageLabel = await this.translate
            .get('PAGINATOR.previousPageLabel')
            .toPromise();
        paginator._intl.getRangeLabel = (page, pageSize, length) =>
            this.translate.instant('PAGINATOR.getRangeLabel', {
                page: page + 1,
                pageSize,
                length,
                pageCount: Math.ceil(length / pageSize),
            });
        // Connect data source.
        this.entriesService.dataSource$.pipe(takeUntil(this.destroyed)).subscribe((dataSource) => {
            if (dataSource instanceof NodeDataSourceRemote) {
                (dataSource as NodeDataSourceRemote).paginator = paginator;
            } else {
                paginator.length = dataSource?.getTotal();
                dataSource
                    ?.connectPagination()
                    .pipe(takeUntil(this.destroyed))
                    .subscribe(() => {
                        paginator.length = dataSource.getTotal();
                    });
            }
        });
    }

    openPage(page: PageEvent) {
        this.entriesService.fetchData.emit({
            offset: page.pageIndex * page.pageSize,
            amount: page.pageSize,
            reset: true,
        });
    }
}
