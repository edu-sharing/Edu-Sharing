import { Component, QueryList, ViewChildren } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../mds-editor-instance.service';
import { EditorMode, MdsView } from '../../types/types';
import { MdsEditorViewComponent } from '../mds-editor-view/mds-editor-view.component';

@Component({
    selector: 'es-mds-editor-core',
    templateUrl: './mds-editor-core.component.html',
    styleUrls: ['./mds-editor-core.component.scss'],
})
export class MdsEditorCoreComponent {
    @ViewChildren('view') viewRef: QueryList<MdsEditorViewComponent>;

    views: MdsView[];
    suggestionsViews: MdsView[];
    hasExtendedWidgets$: Observable<boolean>;
    readonly editorMode: EditorMode;
    readonly shouldShowExtendedWidgets$: BehaviorSubject<boolean>;

    constructor(public mdsEditorInstance: MdsEditorInstanceService) {
        this.shouldShowExtendedWidgets$ = this.mdsEditorInstance.shouldShowExtendedWidgets$;
        this.editorMode = this.mdsEditorInstance.editorMode;
        this.mdsEditorInstance.mdsInitDone.subscribe(() => this.init());
        this.hasExtendedWidgets$ = this.mdsEditorInstance.widgets.pipe(
            map((widgets) => widgets?.some((widget) => widget.definition.isExtended)),
        );
    }

    clear(): void {
        void this.mdsEditorInstance.clearValues();
    }

    private async init(): Promise<void> {
        if (this.views) {
            // Make sure existing views are destroyed and reinitialized.
            this.views = [];
            this.suggestionsViews = [];
            await tick();
        }
        this.mdsEditorInstance.resetWidgets();
        this.views = this.mdsEditorInstance.views.filter((view) => !view.rel);
        this.suggestionsViews = this.mdsEditorInstance.views.filter(
            (view) => view.rel === 'suggestions',
        );
        // Wait for `MdsEditorViewComponent`s to be injected.
        await tick();
        // Wait for `MdsEditorViewComponent`s to inject their widgets.
        await tick();
        this.mdsEditorInstance.mdsInflated.next(true);
    }
}

function tick(): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve));
}
