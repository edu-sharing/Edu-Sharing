import { Component, EventEmitter, Input, OnDestroy, OnInit, Output } from '@angular/core';
import { Node, SearchService } from 'ngx-edu-sharing-api';
import { Subject } from 'rxjs';
import { first } from 'rxjs/operators';
import { RestConstants } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import {
    BulkBehavior,
    EditorMode,
    EditorType,
    MdsWidget,
    UserPresentableError,
    Values,
} from '../../types/types';
import { valuesDictIsEquivalent } from './values-dict-is-equivalent';

/**
 * Wrapper component to select between the legacy `<es-mds>` component and the Angular-native
 * `<es-mds-editor>`.
 *
 * Input properties have to be stable after initialization.
 *
 * In case <es-mds-editor> is selected, do some data preprocessing.
 */
@Component({
    selector: 'es-mds-editor-wrapper',
    templateUrl: './mds-editor-wrapper.component.html',
    styleUrls: ['./mds-editor-wrapper.component.scss'],
    providers: [MdsEditorInstanceService],
})
export class MdsEditorWrapperComponent implements OnInit, OnDestroy {
    // tslint:disable: no-output-on-prefix  // Keep API compatibility.

    @Input() addWidget = false;
    @Input() allowReplacing = true;
    @Input() bulkBehaviour = BulkBehavior.Default;
    @Input() create: string;
    @Input() currentValues: Values;
    @Input() customTitle: string;
    @Input() embedded = false;
    @Input() extended = false;
    @Input() groupId: string;
    @Input() invalidate: boolean;
    @Input() labelNegative = 'CANCEL';
    @Input() labelPositive = 'SAVE';
    @Input() toastOnSave = 'WORKSPACE.EDITOR.UPDATED';
    @Input() mode: 'search' | 'default' = 'default';
    @Input() nodes: Node[];
    @Input() graphqlIds: string[];
    @Input() parentNode: Node;
    @Input() priority = 1;
    @Input() repository = RestConstants.HOME_REPOSITORY;
    @Input() editorMode: EditorMode;
    @Input() setId: string;
    /**
     * Filters that should be applied in addition to the MDS's own values when fetching remote
     * values to be suggested to the user.
     *
     * Currently applied only if `editorMode === 'search'`.
     */
    @Input() set externalFilters(values: Values) {
        this.mdsEditorInstance.externalFilters = values;
    }

    @Output() extendedChange = new EventEmitter();
    @Output() onCancel = new EventEmitter();
    @Output() onDone = new EventEmitter<Node[] | Values>();
    @Output() onMdsLoaded = new EventEmitter();
    @Output() openContributor = new EventEmitter();
    /**
     * @DEPRECATED old mds only
     */
    @Output() openLicense = new EventEmitter();
    @Output() openTemplate = new EventEmitter();

    // Internal state.
    isLoading = true;
    editorType: EditorType;

    private destroyed$ = new Subject<void>();
    private values: Values;

    constructor(
        public mdsEditorInstance: MdsEditorInstanceService,
        private toast: Toast,
        private search: SearchService,
    ) {}

    getInstanceService() {
        return this.mdsEditorInstance;
    }

    ngOnInit(): void {
        // For compatibility reasons, we wait for `loadMds()` to be called before initializing when
        // `nodes` is undefined.
        //
        // TODO: Make sure that inputs are ready when this component is initialized and remove calls
        // to `loadMds()`.
        if (this.graphqlIds || this.nodes || this.currentValues) {
            this.init();
        }
        this.mdsEditorInstance.values.subscribe((values) => (this.values = values));

        if (!this.embedded) {
            throw new Error(
                'Non-embedded use of mds-editor-wrapper has been deprecated in favor of mds-editor-dialog. ' +
                    'Please migrate.',
            );
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    /** @deprecated compatibility to legacy `mds` component */
    handleKeyboardEvent(event: KeyboardEvent): boolean {
        switch (this.editorType) {
            case 'angular':
                // Tell the outer component that we handle all keyboard events. This prevents the
                // dialog to be closed from outside on Escape without confirmation.
                return true;
            default:
                console.warn('handleKeyboardEvent() was called before init finished');
                return null;
        }
    }

    async getValues(node: Node = null): Promise<{ [property: string]: string[] }> {
        switch (this.editorType) {
            case 'angular':
                return this.mdsEditorInstance.getValues(node);
            default:
                console.warn('getValues() was called before init finished');
                return null;
        }
    }

    /**
     * @deprecated compatibility to legacy `mds` component
     *
     * Use `getValues()`
     */
    async saveValues(): Promise<{ [property: string]: string[] }> {
        switch (this.editorType) {
            case 'angular':
                const values = await this.mdsEditorInstance.getValues();
                this.onDone.emit(values);
                return values;
            default:
                console.warn('saveValues() was called before init finished');
                return null;
        }
    }

    /** @deprecated compatibility to legacy `mds` component */
    get currentWidgets(): MdsWidget[] {
        switch (this.editorType) {
            case 'angular':
                return this.mdsEditorInstance.widgets.value.map((widget) => widget.definition);
            default:
                console.warn('get currentWidgets() was called before init finished');
                return null;
        }
    }

    /**
     * @deprecated compatibility to legacy `mds` component
     *
     * Use `reInit()` and make sure inputs are prepared before calling.
     * Does not work properly for the angular based variant!
     */
    loadMds(onlyLegacy = false): void {
        // In case of `SearchComponent`, `currentValues` is not ready when `loadMds` is called. So
        // we wait tick before initializing.
        setTimeout(() => {
            // Re-inits the MDS if needed, otherwise pretends to do so.
            if (
                this.editorType === 'angular' &&
                this.groupId === this.mdsEditorInstance.groupId &&
                this.setId === this.mdsEditorInstance.mdsId &&
                this.repository === this.mdsEditorInstance.repository &&
                valuesDictIsEquivalent(this.currentValues, this.values)
            ) {
                // Don't need to re-init
                this.loadMdsAfterInit(onlyLegacy);
            } else {
                this.init().then(() => {
                    this.loadMdsAfterInit(onlyLegacy);
                });
            }
        });
    }

    private loadMdsAfterInit(onlyLegacy: boolean): void {
        switch (this.editorType) {
            case 'angular':
                if (onlyLegacy) {
                    return;
                }
                this.mdsEditorInstance.mdsDefinition$
                    .pipe(first((definition) => definition !== null))
                    .subscribe((definition) => this.onMdsLoaded.emit(definition));
        }
    }

    async onSave(): Promise<void> {
        this.isLoading = true;
        try {
            if (!this.mdsEditorInstance.getCanSave()) {
                // no changes, behave like close
                if (this.mdsEditorInstance.getIsValid()) {
                    this.onDone.emit(this.nodes);
                    return;
                } else {
                    console.warn(
                        "The following widgets are required but don't have a value: ",
                        this.mdsEditorInstance
                            .getCompletitonStatus()
                            .mandatory.fields.filter((f) => !f.isCompleted)
                            .map((f) => f.widget.definition.id),
                    );
                    this.mdsEditorInstance.showMissingRequiredWidgets();
                }
                return;
            }
            const updatedNodes = await this.mdsEditorInstance.save();
            if (this.toastOnSave) {
                this.toast.toast(this.toastOnSave);
            }
            this.onDone.emit(updatedNodes);
        } catch (error) {
            if (
                error?.error?.error?.endsWith(
                    RestConstants.CONTENT_FILE_EXTENSION_VERIFICATION_EXCEPTION,
                )
            ) {
                this.handleError(
                    new UserPresentableError('MDS.ERROR_FILE_EXTENSION_VERIFICATION_EXCEPTION'),
                );
            } else {
                this.handleError(error);
            }
        } finally {
            this.isLoading = false;
        }
    }

    async reInit(): Promise<void> {
        return this.init();
    }

    private async init(): Promise<void> {
        if (this.mdsEditorInstance.mdsInflatedValue) {
            this.mdsEditorInstance.mdsInflated.next(false);
        }
        this.isLoading = true;
        try {
            const config = {
                groupId: this.groupId,
                bulkBehavior: this.bulkBehaviour,
                editorMode: this.editorMode ?? 'nodes',
            };
            /*if (this.graphqlIds) {
                this.editorType = await this.mdsEditorInstance.initWithGraphqlData(
                    this.graphqlIds,
                    config,
                );
            } else */ if (this.nodes) {
                this.editorType = await this.mdsEditorInstance.initWithNodes(this.nodes, config);
            } else {
                try {
                    this.editorType = await this.mdsEditorInstance.initWithoutNodes(
                        this.groupId,
                        this.setId,
                        this.repository,
                        this.editorMode ?? 'search',
                        this.currentValues,
                    );
                } catch (e) {
                    return;
                }
            }
            if (!this.editorType) {
                console.info(
                    `mds ${this.setId} at ${this.repository} did not specify any rendering type (group ${this.groupId})`,
                );
                this.editorType = 'angular';
            }
        } catch (error) {
            this.handleError(error);
        } finally {
            this.isLoading = false;
        }
    }

    private handleError(error: any): void {
        console.error(error);
        if (error instanceof UserPresentableError) {
            this.toast.error(null, error.message);
        } else {
            this.toast.error(error);
        }
        this.onCancel.emit();
    }
}
