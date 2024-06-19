import { trigger } from '@angular/animations';
import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { UIAnimation } from 'ngx-edu-sharing-ui';
import { Observable, forkJoin, from, of } from 'rxjs';
import { map, switchMap } from 'rxjs/operators';
import { Node, RestConstants, RestNodeService } from '../../../../../core-module/core.module';
import { Toast } from '../../../../../services/toast';
import { MdsEditorWrapperComponent } from '../../../../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { BulkBehavior } from '../../../../../features/mds/types/types';

@Component({
    selector: 'es-simple-edit-metadata',
    templateUrl: 'simple-edit-metadata.component.html',
    styleUrls: ['simple-edit-metadata.component.scss'],
    animations: [
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class SimpleEditMetadataComponent {
    readonly BulkBehaviour = BulkBehavior;

    @ViewChild('mds') mds: MdsEditorWrapperComponent;

    private _nodes: Node[];
    @Input()
    get nodes(): Node[] {
        return this._nodes;
    }
    set nodes(value: Node[]) {
        // If nodes are changed and the mds is already rendered, force an update of values.
        if (this._nodes && this.mds) {
            void this.mds.reInit();
        }
        this._nodes = value;
    }
    @Input() fromUpload: boolean;
    @Output() onError = new EventEmitter<void>();

    constructor(private nodeApi: RestNodeService, private toast: Toast) {}

    isDirty() {
        return this.mds.mdsEditorInstance.getHasUserChanges();
    }

    /**
     * validate the form data
     * return true if data is valid, false otherwise
     */
    validate() {
        if (!this.mds.mdsEditorInstance.getIsValid()) {
            this.mds.mdsEditorInstance.showMissingRequiredWidgets();
            return false;
        }
        return true;
    }

    save(): Observable<void> {
        if (!this.isDirty()) {
            // emit null so that next and complete get's called
            return of(null);
        }
        return forkJoin(
            this.nodes.map((n) =>
                from(this.mds.getValues(n)).pipe(
                    switchMap((props) => {
                        delete props[RestConstants.CM_NAME];
                        return this.nodeApi.editNodeMetadataNewVersion(
                            n.ref.id,
                            RestConstants.COMMENT_METADATA_UPDATE,
                            props,
                        );
                    }),
                ),
            ),
        ).pipe(map(() => {}));
    }
}
