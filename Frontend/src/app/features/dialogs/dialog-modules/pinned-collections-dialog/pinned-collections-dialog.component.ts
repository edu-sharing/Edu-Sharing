import { CdkDragDrop } from '@angular/cdk/drag-drop';
import { Component, Inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
    DialogButton,
    NodeList,
    RestCollectionService,
    RestConstants,
    RestHelper,
    RestNodeService,
    RestSearchService,
} from '../../../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { Helper } from '../../../../core-module/rest/helper';
import { Toast } from '../../../../services/toast';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import {
    PinnedCollectionsDialogData,
    PinnedCollectionsDialogResult,
} from './pinned-collections-dialog-data';

@Component({
    selector: 'es-pinned-collections-dialog',
    templateUrl: './pinned-collections-dialog.component.html',
    styleUrls: ['./pinned-collections-dialog.component.scss'],
})
export class PinnedCollectionsDialogComponent {
    pinnedCollections: Node[];
    checked: string[] = [];

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: PinnedCollectionsDialogData,
        private dialogRef: CardDialogRef<
            PinnedCollectionsDialogData,
            PinnedCollectionsDialogResult
        >,
        private collection: RestCollectionService,
        private node: RestNodeService,
        private search: RestSearchService,
        private toast: Toast,
        private translate: TranslateService,
    ) {
        this.init();
        this.initButtons();
    }

    private close(): void {
        this.dialogRef.close(null);
    }

    private init(): void {
        this.dialogRef.patchState({ isLoading: true });
        this.search
            .searchByProperties(
                [RestConstants.CCM_PROP_COLLECTION_PINNED_STATUS],
                ['true'],
                ['='],
                RestConstants.COMBINE_MODE_AND,
                RestConstants.CONTENT_TYPE_COLLECTIONS,
                {
                    sortBy: [RestConstants.CCM_PROP_COLLECTION_PINNED_ORDER],
                    sortAscending: true,
                    count: RestConstants.COUNT_UNLIMITED,
                },
            )
            .subscribe((data: NodeList) => {
                this.pinnedCollections = data.nodes;
                this.updateSubtitle();
                for (let collection of this.pinnedCollections) {
                    // collection is already pinned, don't add it
                    if (collection.ref.id === this.data.collection.ref.id) {
                        this.setAllChecked();
                        this.dialogRef.patchState({ isLoading: false });
                        return;
                    }
                }
                this.node.getNodeMetadata(this.data.collection.ref.id).subscribe((add) => {
                    this.pinnedCollections.splice(0, 0, add.node);
                    this.updateSubtitle();
                    this.dialogRef.patchState({ isLoading: false });
                    this.setAllChecked();
                });
            });
    }

    private initButtons(): void {
        const buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.close()),
            new DialogButton('APPLY', { color: 'primary' }, () => this.apply()),
        ];
        this.dialogRef.patchConfig({ buttons });
    }

    private updateSubtitle(): void {
        this.translate
            .get('CARD_SUBTITLE_MULTIPLE', { count: this.pinnedCollections.length })
            .subscribe((subtitle) => this.dialogRef.patchConfig({ subtitle }));
    }

    isChecked(collection: Node) {
        return this.checked.indexOf(collection.ref.id) != -1;
    }

    onDropped(event: CdkDragDrop<unknown>) {
        Helper.arraySwap(this.pinnedCollections, event.previousIndex, event.currentIndex);
        this.dialogRef.patchConfig({ closable: Closable.Confirm });
    }

    getName(collection: Node): string {
        return RestHelper.getTitle(collection);
    }

    private apply() {
        this.dialogRef.patchState({ isLoading: true });
        let collections: string[] = [];
        for (let collection of this.pinnedCollections) {
            if (this.isChecked(collection)) {
                collections.push(collection.ref.id);
            }
        }
        this.collection.setPinning(collections).subscribe(
            () => {
                this.toast.toast('COLLECTIONS.PINNING.UPDATED');
                this.close();
            },
            (error) => {
                this.toast.error(error);
                this.dialogRef.patchState({ isLoading: false });
            },
        );
    }

    setChecked(collection: Node, event: any) {
        if (this.isChecked(collection)) {
            this.checked.splice(this.checked.indexOf(collection.ref.id), 1);
        } else {
            this.checked.push(collection.ref.id);
        }
    }

    private setAllChecked() {
        for (let collection of this.pinnedCollections) {
            this.checked.push(collection.ref.id);
        }
    }
}
