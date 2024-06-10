import {
    ComponentFactoryResolver,
    ComponentRef,
    Injectable,
    ViewContainerRef,
} from '@angular/core';
import {
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OptionsHelperDataService,
    Scope,
    SpinnerComponent,
    Target,
} from 'ngx-edu-sharing-ui';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { EventType, Node } from '../../core-module/rest/data-object';
import { RestConstants } from '../../core-module/rest/rest-constants';
import { RestTrackingService } from '../../core-module/rest/services/rest-tracking.service';
import { RestUsageService } from '../../core-module/rest/services/rest-usage.service';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { MdsEditorWrapperComponent } from '../../features/mds/mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { replaceElementWithDiv } from '../../features/mds/mds-editor/util/replace-element-with-div';
import { CommentsListComponent } from './comments-list/comments-list.component';
import { MdsNodeRelationsWidgetComponent } from './node-relations/node-relations-widget.component';
import { VideoControlsComponent } from './video-controls/video-controls.component';

@Injectable()
export class RenderHelperService {
    private static isCollectionRef(node: Node) {
        return node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }

    public videoControlsRef: ComponentRef<VideoControlsComponent>;
    private viewContainerRef: ViewContainerRef;

    constructor(
        private componentFactoryResolver: ComponentFactoryResolver,
        private usageApi: RestUsageService,
        private optionsHelperService: OptionsHelperDataService,
        private tracking: RestTrackingService,
    ) {}

    setViewContainerRef(viewContainerRef: ViewContainerRef) {
        this.viewContainerRef = viewContainerRef;
    }

    injectModuleComments(node: Node) {
        const data = {
            node,
        };
        UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            CommentsListComponent,
            document.getElementsByTagName('comments')[0],
            data,
        );
    }

    injectModuleInCollections(node: Node) {
        let domCollections: Element;
        let parentInner: Element;
        let parent: Element;
        try {
            domCollections = document.evaluate(
                '//*[@id="edusharing_rendering_metadata"]//collections',
                document,
                null,
                XPathResult.FIRST_ORDERED_NODE_TYPE,
                null,
            ).singleNodeValue as Element;
            parentInner = domCollections.parentElement;
            parent = domCollections.parentElement.parentElement;
        } catch (e) {
            return;
        }

        domCollections = replaceElementWithDiv(domCollections);
        UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            SpinnerComponent,
            domCollections,
        );

        function removeCollectionsContainer() {
            // if its the only widget inside the group
            if (parentInner.children.length <= 2) {
                parent.remove();
                return;
            } else {
                const children = [...parentInner.children];
                let index = children.findIndex((child) => child === domCollections);
                if (index > 0) {
                    // remove caption
                    children[index - 1].remove();
                    children[index].remove();
                }
                return;
            }
        }

        this.getCollectionsContainingNode(node).subscribe(
            (collections) => {
                // @TODO: This does currently ignore the "hideIfEmpty" flag of the mds template
                if (collections.length === 0) {
                    removeCollectionsContainer();
                }
                const data = {
                    dataSource: new NodeDataSource(collections),
                    columns: ListItem.getCollectionDefaults(),
                    displayType: NodeEntriesDisplayType.SmallGrid,
                };
                const entriesComponentRef = UIHelper.injectAngularComponent(
                    this.componentFactoryResolver,
                    this.viewContainerRef,
                    NodeEntriesWrapperComponent,
                    domCollections,
                    data,
                    { delay: 250 },
                );
                entriesComponentRef.instance.ngOnChanges();
            },
            (error) => {
                removeCollectionsContainer();
            },
        );
    }
    injectNodeRelationsWidget(node: Node) {
        let domRelations;
        try {
            domRelations = document.evaluate(
                '//*[@id="edusharing_rendering_metadata"]//nodeRelations',
                document,
                null,
                XPathResult.FIRST_ORDERED_NODE_TYPE,
                null,
            ).singleNodeValue as Element;
        } catch (e) {}
        if (domRelations) {
            domRelations = replaceElementWithDiv(domRelations);
            const component = UIHelper.injectAngularComponent(
                this.componentFactoryResolver,
                this.viewContainerRef,
                MdsNodeRelationsWidgetComponent,
                domRelations,
                {
                    node,
                },
            );
            component.instance.ngOnChanges();
            return component;
        }
        return null;
    }

    injectMetadataEditor(node: Node, groupId = 'io_render') {
        const metadata = document.querySelector('.edusharing_rendering_metadata_body');
        const parent = metadata.parentElement;
        parent.removeChild(metadata);
        const component = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            MdsEditorWrapperComponent,
            parent,
            {
                groupId,
                nodes: [node],
                editorMode: 'inline',
                embedded: true,
            },
        );
        // enforce to render all widgets, since rendering does not support extended state
        component.instance.getInstanceService().shouldShowExtendedWidgets$.next(true);
        return component;
    }

    private getCollectionsContainingNode(node: Node): Observable<Node[]> {
        const id = this.getOriginalId(node);
        return this.usageApi.getNodeUsagesCollection(id, node.ref.repo).pipe(
            map((usages) => usages.filter((usage) => usage.collectionUsageType === 'ACTIVE')),
            map((usages) => usages.map((usage) => usage.collection)),
        );
    }

    private getOriginalId(node: Node): string {
        if (RenderHelperService.isCollectionRef(node)) {
            return node.properties[RestConstants.CCM_PROP_IO_ORIGINAL];
        } else {
            return node.ref.id;
        }
    }

    /**
     * find injected link based actions with attribute es-action
     * (they're generated by the backend)
     * and replace them with internal function/action calls
     */
    applyActionButtons(node: Node) {
        let result: XPathResult;
        try {
            result = document.evaluate(
                '//*[@id="edusharing_rendering_metadata"]//a[@data-es-action]',
                document,
                null,
                XPathResult.UNORDERED_NODE_ITERATOR_TYPE,
                null,
            );
        } catch (e) {}
        if (result) {
            let action: HTMLElement;
            while ((action = result.iterateNext() as HTMLElement)) {
                const actionName = action.getAttribute('data-es-action');
                action.onclick = async (event) => {
                    event.preventDefault();
                    this.optionsHelperService.setData({
                        scope: Scope.Render,
                        activeObjects: [node],
                    });
                    const option = (
                        await this.optionsHelperService.getAvailableOptions(Target.List, [node])
                    ).filter((o) => o.name === actionName);
                    if (option.length === 1 && option[0].isEnabled) {
                        option[0].callback(node);
                    } else {
                        console.warn('No action was available for: ' + actionName);
                    }
                };
            }
        }
    }

    doAll(node: Node) {
        this.injectModuleInCollections(node);
        this.injectNodeRelationsWidget(node);
        this.injectModuleComments(node);
        this.applyActionButtons(node);
        this.injectVideoControls(node);
        this.injectEventHandler(node);
    }

    injectEventHandler(node: Node) {
        const videoElement = document.querySelector('#edusharing_rendering_content_href');
        videoElement?.addEventListener('click', () => {
            this.tracking.trackEvent(EventType.OPEN_EXTERNAL_LINK, node.ref.id).subscribe(() => {});
        });
    }
    injectVideoControls(node: Node) {
        let videoElement: HTMLVideoElement;
        let target: Element;
        try {
            videoElement = document.querySelector('.edusharing_rendering_content_wrapper video');
            if (!videoElement) {
                throw new Error();
            }
            const listener = () => {
                this.tracking
                    .trackEvent(EventType.VIEW_MATERIAL_PLAY_MEDIA, node.ref.id)
                    .subscribe(() => {});
                videoElement.removeEventListener('play', listener);
            };
            videoElement.addEventListener('play', listener);
            target = document.createElement('div');
            document
                .querySelector('.edusharing_rendering_wrapper')
                .parentElement.appendChild(target);
        } catch (e) {
            // console.log("did not find video element, skipping controls",e);
            setTimeout(() => this.injectVideoControls(node), 1000 / 30);
            return;
        }
        const data = {
            video: videoElement,
            node,
        };
        this.videoControlsRef = UIHelper.injectAngularComponent(
            this.componentFactoryResolver,
            this.viewContainerRef,
            VideoControlsComponent,
            target,
            data,
        );
    }

    /**
     * applies the render data given as the detailsSnippet and injects it in the given HTMLELement
     * Script urls given by the rendering data will be obeyed. However, inline scripts are NOT supported
     * This function should be used to align with CSP security policies
     */
    applyRenderData(htmlElement: HTMLElement, detailsSnippet: string) {
        detailsSnippet = detailsSnippet.replaceAll(
            '<style',
            '<style nonce="' +
                document.querySelector('es-router[ngCspNonce]')?.getAttribute('ngCspNonce') +
                '"',
        );

        let styleIndex = -1;
        do {
            styleIndex = detailsSnippet.indexOf('style=', styleIndex + 1);
            if (styleIndex === -1) {
                break;
            }
            console.warn(
                'Rendering provided an inline style attribute! This style will be ignored if unsafe-inline is disabled!',
                detailsSnippet.substring(styleIndex - 50, styleIndex + 50),
            );
        } while (true);

        htmlElement.innerHTML = detailsSnippet;
        Array.from(htmlElement.querySelectorAll('script')).forEach((script) => {
            const newScriptElement = document.createElement('script');
            if (
                !Array.from(script.attributes)
                    .map((a) => a.name)
                    .includes('src')
            ) {
                console.warn(
                    'Rendering provided an inline script without a src! This script can not be executed!',
                    script,
                );
            }
            Array.from(script.attributes).forEach((attr) => {
                newScriptElement.setAttribute(attr.name, attr.value);
            });
            document.head.appendChild(newScriptElement);
            script.parentElement.removeChild(script);
        });
    }
}
