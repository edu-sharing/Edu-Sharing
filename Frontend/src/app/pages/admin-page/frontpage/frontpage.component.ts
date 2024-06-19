import { AfterViewInit, Component, EventEmitter, Output, ViewChild } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, ValidatorFn, Validators } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import {
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
} from 'ngx-edu-sharing-ui';
import {
    RestCollectionService,
    RestMdsService,
    RestNodeService,
} from '../../../core-module/core.module';
import { Node } from '../../../core-module/rest/data-object';
import { MdsHelperService } from 'ngx-edu-sharing-ui';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { ConfigurationService } from '../../../core-module/rest/services/configuration.service';
import { RestAdminService } from '../../../core-module/rest/services/rest-admin.service';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { Closable } from '../../../features/dialogs/card-dialog/card-dialog-config';
import { DialogsService } from '../../../features/dialogs/dialogs.service';

@Component({
    selector: 'es-admin-frontpage',
    templateUrl: 'frontpage.component.html',
    styleUrls: ['frontpage.component.scss'],
})
export class AdminFrontpageComponent implements AfterViewInit {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;

    @ViewChild(NodeEntriesWrapperComponent) nodeEntries: NodeEntriesWrapperComponent<Node>;

    @Output() onOpenNode = new EventEmitter();
    previewLoading = true;
    config: any;
    modes = ['collection', 'rating', 'views', 'downloads'];
    conditionTypes = ['TOOLPERMISSION'];
    form: UntypedFormGroup;
    previewNodesDataSource = new NodeDataSource();
    previewColumns: ListItem[] = [];
    previewError: string;
    collectionName = '';
    chooseCollection = false;
    codeOptions = { minimap: { enabled: false }, language: 'json' };
    toolpermissions: any;

    constructor(
        private adminService: RestAdminService,
        private collectionService: RestCollectionService,
        private dialogs: DialogsService,
        private formBuilder: UntypedFormBuilder,
        private mdsService: RestMdsService,
        private nodeService: RestNodeService,
        private toast: Toast,
        private translate: TranslateService,
        public configService: ConfigurationService,
    ) {
        this.form = this.formBuilder.group(
            {
                totalCount: [
                    '',
                    [Validators.required, Validators.min(1), Validators.pattern('[0-9]*')],
                ],
                displayCount: [
                    '',
                    [Validators.required, Validators.min(1), Validators.pattern('[0-9]*')],
                ],
                timespan: [
                    '',
                    [Validators.required, Validators.min(1), Validators.pattern('[0-9]*')],
                ],
                timespanAll: [],
            },
            { validator: [ValidateForm] },
        );
        this.form.valueChanges.subscribe((values) => {
            values.timespanAll
                ? this.form.get('timespan').disable({ emitEvent: false })
                : this.form.get('timespan').enable({ emitEvent: false });
        });
        this.mdsService.getSet().subscribe((set) => {
            this.previewColumns = MdsHelperService.getColumns(this.translate, set, 'search');
        });
        this.adminService
            .getToolpermissions()
            .subscribe((toolpermissions) => (this.toolpermissions = Object.keys(toolpermissions)));
        this.update();
    }

    ngAfterViewInit(): void {
        void this.nodeEntries.initOptionsGenerator({});
    }

    save() {
        for (const key of Object.keys(this.form.value)) {
            this.config.frontpage[key] = this.form.value[key];
        }
        this.toast.showProgressSpinner();
        this.adminService.updateRepositoryConfig(this.config).subscribe(() => {
            this.update();
            this.toast.toast('ADMIN.FRONTPAGE.SAVED');
        });
    }

    private async update() {
        try {
            this.config = await this.adminService.getRepositoryConfig().toPromise();
            const values = this.form.value;
            for (const key of Object.keys(values)) {
                values[key] = this.config.frontpage[key];
            }
            // fix if field is disabled, still fetch value
            if (!values.timespan) {
                values.timespan = this.form.get('timespan').value;
            }
            this.form.setValue(values);
            this.toast.closeProgressSpinner();
            if (this.config.frontpage.collection) {
                this.collectionService
                    .getCollection(this.config.frontpage.collection)
                    .subscribe((c) => {
                        this.collectionName = c.collection.title;
                    });
            }
        } catch (e) {
            this.toast.error(e);
            this.toast.closeProgressSpinner();
            const dialogRef = await this.dialogs.openGenericDialog({
                title: 'ADMIN.FRONTPAGE.CONFIG_BROKEN',
                message: 'ADMIN.FRONTPAGE.CONFIG_BROKEN_INFO',
                buttons: [
                    { label: 'CANCEL', config: { color: 'standard' } },
                    { label: 'ADMIN.FRONTPAGE.RESET', config: { color: 'danger' } },
                ],
                closable: Closable.Standard,
                maxWidth: 500,
            });
            dialogRef.afterClosed().subscribe((response) => {
                if (response === 'ADMIN.FRONTPAGE.RESET') {
                    this.toast.showProgressSpinner();
                    this.adminService.updateRepositoryConfig(null).subscribe(() => {
                        void this.update();
                    });
                } else {
                    this.toast.closeProgressSpinner();
                }
            });
        }
        this.updatePreviews();
    }

    updatePreviews() {
        this.previewLoading = true;
        this.previewNodesDataSource.reset();
        this.previewError = null;
        this.nodeService
            .getChildren(RestConstants.NODES_FRONTPAGE, [], {
                propertyFilter: [RestConstants.ALL],
            })
            .subscribe(
                (nodes) => {
                    this.previewLoading = false;
                    this.previewNodesDataSource.setData(nodes.nodes, nodes.pagination);
                },
                (error) => {
                    if (UIHelper.errorContains(error, 'No Elasticsearch instance')) {
                        this.previewError = 'ELASTICSEARCH';
                    } else {
                        this.previewError = 'UNKNOWN';
                    }
                },
            );
    }
    openNode(node: any) {
        this.onOpenNode.emit(node.node);
    }

    setCollection(collection: Node) {
        this.config.frontpage.collection = collection.ref.id;
        this.collectionName = collection.title;
        this.chooseCollection = false;
    }

    queryHelp() {
        // @TODO: Link to edu-sharing manpage!
    }

    addQueryCondition() {
        if (!this.config.frontpage.queries) this.config.frontpage.queries = [];
        this.config.frontpage.queries.push({
            condition: {
                type: this.conditionTypes[0],
                negate: false,
            },
        });
    }
    removeQueryCondition(query: any) {
        this.config.frontpage.queries.splice(this.config.frontpage.queries.indexOf(query), 1);
    }
}
const ValidateForm: ValidatorFn = (control) => {
    const displayCount = control.get('displayCount');
    const totalCount = control.get('totalCount');

    if (parseInt(displayCount.value, 10) > parseInt(totalCount.value, 10)) {
        totalCount.setErrors({ outOfRange: true });
    }
    return null;
};
