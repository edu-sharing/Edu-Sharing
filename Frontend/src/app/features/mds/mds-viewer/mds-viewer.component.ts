import {
    ChangeDetectorRef,
    Component,
    ComponentFactoryResolver,
    ElementRef,
    Injector,
    Input,
    OnChanges,
    NgZone,
    QueryList,
    SimpleChanges,
    ViewChildren,
    ViewContainerRef,
} from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { RestConstants } from '../../../core-module/core.module';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { MdsEditorInstanceService } from '../mds-editor/mds-editor-instance.service';
import { ViewInstanceService } from '../mds-editor/mds-editor-view/view-instance.service';
import { replaceElementWithDiv } from '../mds-editor/util/replace-element-with-div';
import { Values } from '../types/types';
import { MdsWidgetComponent } from './widget/mds-widget.component';
import { MdsView, MdsDefinition, MdsService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-mds-viewer',
    templateUrl: 'mds-viewer.component.html',
    styleUrls: ['mds-viewer.component.scss'],
    providers: [MdsEditorInstanceService, ViewInstanceService],
})
export class MdsViewerComponent implements OnChanges {
    @ViewChildren('container') container: QueryList<ElementRef>;

    @Input() groupId: string;
    @Input() setId: string;
    @Input() data: Values;
    mds: MdsDefinition;
    templates: {
        view: MdsView;
        html: SafeHtml;
    }[];

    /**
     * show group headings (+ icons) for the individual templates
     */
    @Input() showGroupHeadings = true;

    /**
     * The heading level from 1 to 6 to use for widget labels, equivalent to `h1` to `h6`.
     *
     * If not set, widget labels are not marked as headings and an invisible colon is added between
     * labels and values, that will be read out by screen readers.
     */
    @Input()
    set headingLevel(value: number | null) {
        this.viewInstance.headingLevel = value;
    }
    get headingLevel() {
        return this.viewInstance.headingLevel;
    }

    constructor(
        private mdsService: MdsService,
        private mdsEditorInstanceService: MdsEditorInstanceService,
        private factoryResolver: ComponentFactoryResolver,
        private injector: Injector,
        private containerRef: ViewContainerRef,
        private sanitizer: DomSanitizer,
        private viewInstance: ViewInstanceService,
    ) {}

    getGroup() {
        return this.mds.groups.find((g: any) => g.id == this.groupId);
    }
    getView(id: string) {
        return this.mds.views.find((v: any) => v.id == id);
    }

    public async inflate() {
        if (!this.mds) {
            setTimeout(() => this.inflate(), 1000 / 60);
            return;
        }
        try {
            const editor = await this.mdsEditorInstanceService.initWithoutNodes(
                this.groupId,
                this.setId,
                RestConstants.HOME_REPOSITORY,
                'viewer',
                this.data,
            );
            if (!editor) {
                // Initialization was interrupted. Probably, this method was called again before it
                // could finish.
                return;
            }
        } catch (e) {
            return;
        }
        this.templates = [];
        for (const view of this.getGroup().views) {
            const v = this.getView(view);
            this.templates.push({
                view: v,
                html: this.sanitizer.bypassSecurityTrustHtml(this.prepareHTML(v.html)),
            });
        }
        // wait for angular to inflate the new binding
        setTimeout(() => {
            for (const widget of this.mdsEditorInstanceService.widgets.value) {
                // @TODO: it would be better to filter by widgets based on template and condition, should be implemented in 5.1
                this.container.toArray().forEach((c) => {
                    let element: HTMLElement = c.nativeElement.getElementsByTagName(
                        widget.definition.id,
                    )?.[0];
                    if (element) {
                        // MdsEditorViewComponent.updateWidgetWithHTMLAttributes(element, w);
                        element = replaceElementWithDiv(element);

                        UIHelper.injectAngularComponent(
                            this.factoryResolver,
                            this.containerRef,
                            MdsWidgetComponent,
                            element,
                            {
                                widget,
                            },
                            {},
                            this.injector,
                        );
                    }
                });
            }
        });
    }

    /**
     * close all custom tags inside the html which are not closed
     * e.g. <cm:name>
     *     -> <cm:name></cm:name>
     * @param html
     */
    private prepareHTML(html: string) {
        for (const w of this.mds.widgets) {
            const start = html.indexOf('<' + w.id);
            if (start == -1) {
                continue;
            }
            const end = html.indexOf('>', start) + 1;
            html = html.substring(0, end) + '</' + w.id + '>' + html.substring(end);
        }
        return html;
    }

    async ngOnChanges(changes: SimpleChanges) {
        let inflate = false;
        if (changes.setId) {
            this.mds = await this.mdsService
                .getMetadataSet({ metadataSet: this.setId })
                .toPromise();
            inflate = true;
        }
        if (changes.data) {
            if (this.data[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] != null) {
                this.mds = await this.mdsService
                    .getMetadataSet({
                        metadataSet:
                            this.data[RestConstants.CM_PROP_METADATASET_EDU_METADATASET][0],
                    })
                    .toPromise();
            } else if (!this.mds) {
                this.mds = await this.mdsService.getMetadataSet({}).toPromise();
            }
            inflate = true;
        }
        if (inflate) {
            this.inflate();
        }
    }
}
