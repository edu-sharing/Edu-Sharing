import { Component, EventEmitter, HostBinding, Input, Output } from '@angular/core';
import { Params, QueryParamsHandling } from '@angular/router';
import { CanDrop, DragData, DropSource, NodesDragDropService } from 'ngx-edu-sharing-ui';
import { Node } from 'ngx-edu-sharing-api';
import { RestNodeService } from '../../../core-module/core.module';

import { BreadcrumbsService } from './breadcrumbs.service';

/**
 * Breadcrumbs for nodes or collections.
 */
@Component({
    selector: 'es-breadcrumbs',
    templateUrl: 'breadcrumbs.component.html',
    styleUrls: ['breadcrumbs.component.scss'],
})
export class BreadcrumbsComponent {
    /**
     * Caption of the home, if not set, a default icon is used.
     */
    @Input() home: string;

    /**
     * Icon of home, can be used together with `home`.
     */
    @Input() homeIcon: string;

    /**
     * should an information be shown that you're in the root path in case of an empty nodes path?
     */
    @Input() showHomeNotice = true;
    /**
     * shall an invisbile description (for screen readers) be generated, similar to
     * 'You're here'
     */
    @Input() invisibleDescription = false;
    /**
     * The path to give to `routerLink` on the home element.
     *
     * If not given, clicks will be emitted via the `onClick` output.
     */
    @Input() homeRouterLink: {
        routerLink: any[] | string;
        queryParams?: Params | null;
        queryParamsHandling?: QueryParamsHandling | null;
    };
    /**
     * Attach a clickable class so the user cursor will be a hand.
     */
    @Input() clickable = true;
    /**
     * Show a short variant (only the last item).
     *
     * `auto` (default) decides via media query.
     */
    @HostBinding('attr.short')
    @Input()
    short: 'never' | 'always' | 'auto' = 'auto';
    /**
     * Should automatically be linked via angular routing.
     *
     * If set true, the onClick emitter will only be fired for the "root" element.
     */
    @Input() createLink = true;
    /**
     * Set a search query so the breadcrumbs will show this query.
     */
    @Input() set searchQuery(searchQuery: string) {
        this._searchQuery = searchQuery;
        this.addSearch();
    }

    @Input() canDropNodes: (dragData: DragData<'HOME' | Node>) => CanDrop;

    /**
     * A breadcrumb is clicked.
     *
     * Passes the index **starting at 1** of the clicked breadcrumb, or 0 for the root element.
     */
    @Output() clickBreadcrumb = new EventEmitter<number>();
    /**
     * Called when an item is dropped on the breadcrumbs.
     */
    @Output() dropElement = new EventEmitter<{ target: Node | 'HOME'; source: DropSource<Node> }>();

    readonly HOME = 'HOME' as 'HOME';
    nodes: Node[] = [];

    private _searchQuery: string;

    constructor(
        private node: RestNodeService,
        private breadcrumbsService: BreadcrumbsService,
        private nodesDragDrop: NodesDragDropService,
    ) {
        this.breadcrumbsService.breadcrumbs$.subscribe((nodes) => {
            if (nodes == null) {
                return;
            }
            this.nodes = nodes;
            this.addSearch();
        });
    }

    openBreadcrumb(position: number) {
        this.clickBreadcrumb.emit(position);
    }

    private addSearch() {
        const add = !(
            this.nodes.length > 0 &&
            this.nodes[this.nodes.length - 1] &&
            this.nodes[this.nodes.length - 1].type === 'SEARCH'
        );
        if (this._searchQuery) {
            const search = {
                name: `'${this._searchQuery}'`,
                type: 'SEARCH',
            } as Node;
            if (add) {
                this.nodes.splice(this.nodes.length, 0, search);
            } else {
                this.nodes = this.nodes.filter((n) => n.type !== 'SEARCH');
            }
        } else if (!add) {
            this.nodes = this.nodes.filter((n) => n.type !== 'SEARCH');
        }
    }

    onDropped(dragData: DragData<'HOME' | Node>) {
        this.dropElement.emit({
            target: dragData.target,
            source: {
                element: dragData.draggedNodes,
                mode: dragData.action,
            },
        });
    }

    // drop(event: CdkDragDrop<Node | any>) {
    //     // this.onDrop.emit({
    //     //     target: event.container.data,
    //     //     source: {
    //     //         element: [event.item.data],
    //     //         sourceList: null,
    //     //         mode: DragCursorDirective.dragState.mode,
    //     //     },
    //     // });
    //     // DragCursorDirective.dragState.element = null;
    // }
    // getDragState() {
    //     return {} as any;
    // }

    // dragExit(event: CdkDragExit<any>) {
    //     // DragCursorDirective.dragState.element = null;
    // }

    // dragEnter(event: CdkDragEnter<any>) {
    //     // DragCursorDirective.dragState.element = event.container.data;
    //     // DragCursorDirective.dragState.dropAllowed = true;
    // }
}
