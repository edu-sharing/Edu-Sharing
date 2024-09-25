import { AfterViewInit, Component, Inject, OnInit, TemplateRef, ViewChild } from '@angular/core';
import * as rxjs from 'rxjs';
import { delay, filter, first, map } from 'rxjs/operators';
import { DialogButton, Node } from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { JumpMark } from '../../../../services/jump-marks.service';
import { LocalEventsService } from 'ngx-edu-sharing-ui';
import { MdsEditorCoreComponent } from '../../../mds/mds-editor/mds-editor-core/mds-editor-core.component';
import { MdsEditorInstanceService } from '../../../mds/mds-editor/mds-editor-instance.service';
import { EditorType, UserPresentableError } from '../../../mds/types/types';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { JUMP_MARK_POSTFIX } from '../../card-dialog/card-dialog-container/jump-marks-handler.directive';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { FillTypeStatus } from './input-fill-progress/input-fill-progress.component';
import {
    hasNodes,
    hasValues,
    MdsEditorDialogData,
    MdsEditorDialogResult,
} from './mds-editor-dialog-data';

@Component({
    selector: 'es-mds-editor-dialog',
    templateUrl: './mds-editor-dialog.component.html',
    styleUrls: ['./mds-editor-dialog.component.scss'],
    providers: [MdsEditorInstanceService],
})
export class MdsEditorDialogComponent implements OnInit, AfterViewInit {
    @ViewChild('customBottomBarContent') customBottomBarContent: TemplateRef<HTMLElement>;
    @ViewChild(MdsEditorCoreComponent) mdsEditorCore: MdsEditorCoreComponent;

    // Progress indicator
    completedProperties: FillTypeStatus;
    totalProperties: FillTypeStatus;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: MdsEditorDialogData,
        private dialogRef: CardDialogRef<MdsEditorDialogData, MdsEditorDialogResult>,
        private mdsEditorInstance: MdsEditorInstanceService,
        private localEvents: LocalEventsService,
        private toast: Toast,
    ) {
        this.dialogRef.patchState({ isLoading: true });
    }

    async ngOnInit(): Promise<void> {
        try {
            await this.initMdsEditor();
        } catch (e) {
            this.handleError(e);
            this.dialogRef.close(null);
        }
        this.initButtons();
        this.registerProgressIndicator();
        // `SendFeedbackDialog` works similar to this component. Please update accordingly when
        // making changes here.
        this.mdsEditorInstance.mdsInflated.pipe(first(), delay(0)).subscribe(() => {
            this.dialogRef.patchState({ isLoading: false });
            if (this.data.immediatelyShowMissingRequiredWidgets) {
                this.mdsEditorInstance.showMissingRequiredWidgets(false);
            }
        });
    }

    ngAfterViewInit(): void {
        this.registerJumpMarks();
        this.dialogRef.patchConfig({
            customBottomBarContent: this.customBottomBarContent,
        });
    }

    onShowMissing(): void {
        this.mdsEditorInstance.showMissingRequiredWidgets();
    }

    private async initMdsEditor(): Promise<void> {
        let editorType: EditorType;
        /*if (hasGraphql(this.data)) {
            editorType = await this.mdsEditorInstance.initWithGraphqlData(
                (this.data as MdsEditorDialogDataGraphql).graphqlIds,
                {
                    groupId: this.data.groupId,
                    bulkBehavior: this.data.bulkBehavior,
                    editorMode: 'nodes',
                },
            );
        } else */
        if (hasNodes(this.data)) {
            editorType = await this.mdsEditorInstance.initWithNodes(this.data.nodes, {
                groupId: this.data.groupId,
                bulkBehavior: this.data.bulkBehavior,
                editorMode: 'nodes',
            });
        } else if (hasValues(this.data)) {
            editorType = await this.mdsEditorInstance.initWithoutNodes(
                this.data.groupId,
                this.data.setId,
                this.data.repository,
                this.data.editorMode,
                this.data.values,
            );
        }
        if (editorType !== 'angular') {
            throw new Error(
                'Called mds-editor-dialog with legacy mds. Supports only "angular" rendering.',
            );
        }
    }

    private initButtons(): void {
        this.dialogRef.patchConfig({
            buttons: [
                new DialogButton('CANCEL', { color: 'standard' }, () =>
                    this.dialogRef.close('CANCEL'),
                ),
                new DialogButton('SAVE', { color: 'primary' }, () => this.save()),
            ],
        });
        this.mdsEditorInstance.observeHasUserChanges().subscribe((hasUserChanges) =>
            this.dialogRef.patchConfig({
                closable: hasUserChanges ? Closable.Confirm : Closable.Standard,
            }),
        );
    }

    private registerJumpMarks(): void {
        rxjs.combineLatest([
            this.mdsEditorInstance.activeViews,
            this.mdsEditorInstance.shouldShowExtendedWidgets$,
        ])
            .pipe(
                map(([activeViews]) =>
                    activeViews.map((view) =>
                        this.mdsEditorCore.viewRef?.find((v) => v.view.id === view.id),
                    ),
                ),
                map((viewRef) =>
                    viewRef
                        .filter((v) => v && !v.isInHiddenState() && v.view.caption)
                        .map(
                            (v) =>
                                new JumpMark(
                                    v.view.id + JUMP_MARK_POSTFIX,
                                    v.view.caption,
                                    v.view.icon,
                                ),
                        ),
                ),
            )
            .subscribe((jumpMarks) => this.dialogRef.patchConfig({ jumpMarks }));
    }

    private registerProgressIndicator(): void {
        this.mdsEditorInstance
            .observeCompletionStatus()
            .pipe(filter((completionStatus) => !!completionStatus))
            .subscribe((completionStatus) => {
                this.completedProperties = mapDict(completionStatus, (entry) => entry.completed);
                this.totalProperties = mapDict(completionStatus, (entry) => entry.total);
            });
    }

    private async save(): Promise<void> {
        if (this.mdsEditorInstance.getCanSave()) {
            this.dialogRef.patchState({ isLoading: true });
            try {
                const updatedNodesOrValues = await this.mdsEditorInstance.save();
                this.toast.toast('WORKSPACE.EDITOR.UPDATED');
                this.dialogRef.close(updatedNodesOrValues);
                if (hasNodes(this.data)) {
                    this.localEvents.nodesChanged.emit(updatedNodesOrValues as Node[]);
                }
            } catch (e) {
                this.handleError(e);
                this.dialogRef.patchState({ isLoading: false });
            }
        } else {
            // No changes, behave like close.
            if (this.mdsEditorInstance.getIsValid()) {
                this.dialogRef.close('NO-CHANGES');
            } else {
                this.mdsEditorInstance.showMissingRequiredWidgets();
            }
        }
    }
    private handleError(error: any): void {
        console.error(error);
        if (error instanceof UserPresentableError || error.message) {
            this.toast.error(null, error.message);
        } else {
            this.toast.error(error);
        }
    }
}

function mapDict<K extends string, T, R>(
    dict: { [key in K]: T },
    f: (element: T) => R,
): { [key in K]: R } {
    return Object.entries(dict).reduce((acc, [key, value]) => {
        acc[key as K] = f(value as T);
        return acc;
    }, {} as { [key in K]: R });
}
