import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ContentChild,
    EventEmitter,
    Input,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { combineLatest, Observable } from 'rxjs';
import { UntypedFormControl } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { debounceTime, filter, map, switchMap, tap } from 'rxjs/operators';
import { RestSearchService } from '../../../../../core-module/rest/services/rest-search.service';
import { Node, SearchRequestCriteria } from '../../../../../core-module/rest/data-object';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { MdsHelperService, SearchHelperService } from 'ngx-edu-sharing-ui';
import { RestMdsService } from '../../../../../core-module/rest/services/rest-mds.service';
import { TranslateService } from '@ngx-translate/core';
import { ListItem, NodesRightMode, UIAnimation } from 'ngx-edu-sharing-ui';
import { NodeHelperService } from '../../../../../services/node-helper.service';
import { trigger } from '@angular/animations';
import { MdsEditorWrapperComponent } from '../../../../mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';

type Status = {
    loading: boolean;
    result?: Node[];
};
@Component({
    selector: 'es-node-search-selector',
    templateUrl: 'node-search-selector.component.html',
    styleUrls: ['node-search-selector.component.scss'],
    animations: [trigger('switchDialog', UIAnimation.switchDialogBoolean())],
})
export class NodeSearchSelectorComponent implements AfterViewInit {
    @ContentChild('noPermissions') noPermissionsRef: TemplateRef<any>;
    @ViewChild(MdsEditorWrapperComponent) mdsEditor: MdsEditorWrapperComponent;
    /**
     * group id of the mds set to use the search filters from
     */
    @Input() groupId: string;
    /**
     * query id to use for the search query
     */
    @Input() queryId: string;
    /**
     * the id to use for the displayed columns
     */
    @Input() columnsIds: string;
    /**
     * label of the search field
     */
    @Input() label: string;
    /**
     * only show nodes with appropriate permissions
     */
    @Input() permissions: string[] = [];
    /**
     * additional search criterias that should be added
     */
    @Input() criteria: SearchRequestCriteria[] = [];
    /**
     * count of items to search
     */
    @Input() itemCount = 25;
    @Output() onSelect = new EventEmitter<Node>();
    searchStatus: Status = {
        loading: false,
    };
    input = new UntypedFormControl('');
    columns: ListItem[];
    showMds = false;
    private values: { [p: string]: string[] };
    hasMds = false;

    constructor(
        private searchApi: RestSearchService,
        private mdsService: RestMdsService,
        private translate: TranslateService,
        private nodeHelper: NodeHelperService,
        private searchHelperService: SearchHelperService,
        private changeDetectorRef: ChangeDetectorRef,
    ) {}

    setOption(event: MatAutocompleteSelectedEvent) {
        this.onSelect.emit(event.option.value);
        this.input.setValue('');
    }

    private searchNodes(): Observable<Node[]> {
        let criterias: SearchRequestCriteria[] = [];
        if (this.input.value) {
            criterias.push({
                property: RestConstants.PRIMARY_SEARCH_CRITERIA,
                values: [this.input.value],
            });
        }
        if (this.values) {
            criterias = criterias.concat(
                this.searchHelperService.convertCritieria(
                    this.values,
                    this.mdsEditor.currentWidgets,
                ),
            );
        }
        criterias = criterias.concat(this.criteria);
        const request = {
            count: this.itemCount,
            sortBy: [RestConstants.LUCENE_SCORE],
            sortAscending: [false],
        };
        return this.searchApi
            .search(
                criterias,
                [],
                request,
                RestConstants.CONTENT_TYPE_ALL,
                RestConstants.HOME_REPOSITORY,
                RestConstants.DEFAULT,
                [RestConstants.ALL],
                this.queryId,
                this.permissions,
            )
            .pipe(
                map((m) =>
                    m.nodes.sort((a, b) =>
                        !this.hasPermissions(a) && this.hasPermissions(b)
                            ? 1
                            : this.hasPermissions(a) && !this.hasPermissions(b)
                            ? -1
                            : 0,
                    ),
                ),
            );
    }

    ngAfterViewInit(): void {
        this.mdsService.getSet().subscribe((set) => {
            this.columns = MdsHelperService.getColumns(this.translate, set, this.columnsIds);
        });
        combineLatest([this.input.valueChanges, this.mdsEditor.mdsEditorInstance.values])
            .pipe(
                debounceTime(500),
                filter(() => {
                    if (this.input.value?.length < 2) {
                        this.searchStatus = {
                            loading: false,
                        };
                        this.changeDetectorRef.detectChanges();
                        return false;
                    }
                    return true;
                }),
                tap(() => {
                    this.searchStatus.loading = true;
                    this.changeDetectorRef.detectChanges();
                }),
                switchMap(() => this.searchNodes()),
            )
            .subscribe((result) => {
                this.searchStatus = {
                    loading: false,
                    result,
                };
                this.changeDetectorRef.detectChanges();
            });
        this.mdsEditor.loadMds();
        this.mdsEditor.mdsEditorInstance.values.subscribe((v) => (this.values = v));
    }

    hasPermissions(suggestion: Node) {
        return this.permissions.every((p) =>
            this.nodeHelper.getNodesRight([suggestion], p, NodesRightMode.Effective),
        );
    }

    onMdsLoaded() {
        console.log('mds loaded', this.mdsEditor.currentWidgets);
        this.hasMds = this.mdsEditor.currentWidgets?.length > 0;
    }
}
