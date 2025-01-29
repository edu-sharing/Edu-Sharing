import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import * as rxjs from 'rxjs';
import { MdsEditorInstanceService } from './mds-editor-instance.service';
import { EditorMode, Values } from '../types/types';

/** Information that require a re-initialization once changed. */
interface InitInfo {
    repository: string;
    metadataSet: string;
    group: string;
    editorMode: EditorMode;
}

/**
 * This is meant to replace `MdsEditorWrapper` as entry point when the `legacy` mode is phased out.
 */
// Since this doesn't need to be compatible to `legacy`, its `Input`s, `Output`s, and public
// variables and methods should be kept clean to only include things that should be part of the
// public interface, i.e.., no internal logic like when to initialize components.
@Component({
    selector: 'es-mds-editor',
    templateUrl: './mds-editor.component.html',
    providers: [MdsEditorInstanceService],
})
export class MdsEditorComponent implements OnInit {
    /** The repository to which the metadata set to be used belongs. */
    @Input() set repository(repository: string) {
        this.updateInitInfo({ repository });
    }
    /** The metadata set to be used. */
    @Input() set metadataSet(metadataSet: string) {
        this.updateInitInfo({ metadataSet });
    }
    /** The metadata set's group to be used. */
    @Input() set group(group: string) {
        this.updateInitInfo({ group });
    }
    @Input() set editorMode(editorMode: EditorMode) {
        this.updateInitInfo({ editorMode });
    }
    /** Values to populate or update the editor with. */
    @Input() values: Values;
    /** Value changes as defaults are applied or the user uses the editor. */
    @Output() valuesChange = new EventEmitter<Values>();
    /** The mode in which to run the editor. */

    private readonly initInfoSubject = new rxjs.BehaviorSubject<Partial<InitInfo>>({});

    constructor(
        // Please do not make this public. If there is any need to do so, we should find a way to
        // solve it without sacrificing encapsulation. In the meantime, there is `MdsEditorWrapper`.
        private mdsEditorInstance: MdsEditorInstanceService,
    ) {}

    ngOnInit(): void {
        this.initInfoSubject.subscribe((info) => this.init(info));
    }

    private init(info: Partial<InitInfo>): void {
        if (info.repository && info.metadataSet && info.group) {
            void this.mdsEditorInstance.initWithoutNodes(
                info.group,
                info.metadataSet,
                info.repository,
                info.editorMode ?? 'search',
            );
        } else {
            console.warn('Could not init mds editor: incomplete information.');
        }
    }

    private updateInitInfo(newInfo: Partial<InitInfo>): void {
        const oldInfo = this.initInfoSubject.value;
        // Only trigger an update if some value actually changed.
        if (
            Object.entries(newInfo).some(([key, value]) => oldInfo[key as keyof InitInfo] !== value)
        ) {
            this.initInfoSubject.next({ ...oldInfo, ...newInfo });
        }
    }
}
