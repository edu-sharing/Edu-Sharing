import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { Node } from 'ngx-edu-sharing-api';
import {
    DragData,
    DropdownComponent,
    DropSource,
    LocalEventsService,
    NodeEntriesService,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    Target,
    UIAnimation,
} from 'ngx-edu-sharing-ui';
import {
    NodeList,
    RestConstants,
    RestNodeService,
    UIService,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { canDropOnNode } from '../workspace-utils';
import { Event } from 'typedoc';

@Component({
    selector: 'es-workspace-sub-tree',
    templateUrl: 'sub-tree.component.html',
    styleUrls: ['sub-tree.component.scss'],
    animations: [
        trigger('openOverlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('open', UIAnimation.openOverlay()),
    ],
    providers: [OptionsHelperDataService, NodeEntriesService],
})
export class WorkspaceSubTreeComponent implements OnInit, OnDestroy {
    readonly Target = Target;
    private static MAX_FOLDER_COUNT = 100;

    @ViewChild('dropdown') dropdown: DropdownComponent;
    @ViewChild('dropdownTrigger') dropdownTrigger: MatMenuTrigger;
    dropdownLeft: number;
    dropdownTop: number;

    private _currentPath: string[] = [];
    /** Parent hierarchy of the currently selected node. */
    @Input()
    get currentPath(): string[] {
        return this._currentPath;
    }
    set currentPath(value: string[]) {
        this._currentPath = value;
        this.expandCurrentPath();
    }
    @Input() depth = 0;
    /** The node rendered by this sub tree. */
    @Input() set node(node: string) {
        this._node = node;
        if (node == null) {
            return;
        }
        this.refresh();
    }

    @Output() onClick = new EventEmitter<Node>();
    @Output() onLoading = new EventEmitter();
    @Output() onDrop = new EventEmitter<{ target: Node; source: DropSource<Node> }>();
    @Output() hasChildren = new EventEmitter<boolean>();
    @Output() onUpdateOptions = new EventEmitter();

    _node: string;
    loading = true;
    _nodes: Node[];
    _hasChildren: { [nodeId: string]: boolean } = {};
    moreItems: number;
    loadingMore: boolean;
    loadingStates: boolean[] = [];

    /** IDs of child nodes of the node rendered by this sub tree, that should be expanded. */
    private expandedNodes: string[] = [];
    private destroyed = new Subject<void>();

    constructor(
        private nodeApi: RestNodeService,
        private optionsService: OptionsHelperDataService,
        public entriesService: NodeEntriesService<Node>,
        private localEvents: LocalEventsService,
        public ui: UIService,
    ) {}

    ngOnInit(): void {
        rxjs.merge(this.localEvents.nodesChanged, this.localEvents.nodesDeleted)
            .pipe(takeUntil(this.destroyed))
            .subscribe((nodes) => {
                const nodeIds = this._nodes.map((node) => node.ref.id);
                if (nodes.some((node) => nodeIds.includes(node.ref.id))) {
                    this.refresh();
                }
            });
    }

    /**
     * Resets expanded nodes to the parent hierarchy of the currently selected node.
     */
    private expandCurrentPath() {
        const currentChildNode = this._nodes?.find(
            (node) => node.ref.id === this.currentPath[this.depth],
        );
        if (currentChildNode) {
            this.expandedNodes = [currentChildNode.ref.id];
        }
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    setLoadingState(state: boolean, pos: number) {
        this.loadingStates[pos] = state;
    }

    optionIsShown(optionItem: OptionItem, node: Node) {
        if (optionItem.showCallback) {
            return optionItem.showCallback([node]);
        }
        return true;
    }

    loadAll() {
        this.loadingMore = true;
        this.nodeApi
            .getChildren(this._node, [RestConstants.FILTER_FOLDERS], {
                offset: this._nodes.length,
                count: RestConstants.COUNT_UNLIMITED,
                propertyFilter: [RestConstants.ALL],
            })
            .subscribe((data: NodeList) => {
                this.loadingMore = false;
                this._nodes = this._nodes.concat(data.nodes);
                this.moreItems = 0;
            });
    }

    async contextMenu(event: MouseEvent, node: Node) {
        event.preventDefault();
        event.stopPropagation();
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        }
        this.showDropdown(event, node);
    }

    updateOptions(event: Node) {
        this.onUpdateOptions.emit(event);
    }

    private async showDropdown(event: MouseEvent, node: Node) {
        //if(this._options==null || this._options.length<1)
        //  return;
        this.optionsService.setData({
            scope: Scope.WorkspaceTree,
            allObjects: this._nodes,
            activeObjects: [node],
        });
        this.entriesService.options = {
            [Target.ListDropdown]: await this.optionsService.getAvailableOptions(
                Target.ListDropdown,
            ),
        };
        this.entriesService.openDropdown(this.dropdown, node, () =>
            this.dropdownTrigger.openMenu(),
        );
    }

    dropToParent(event: any) {
        this.onDrop.emit(event);
    }

    isSelected(node: Node): boolean {
        return this.currentPath[this.currentPath.length - 1] === node.ref.id;
    }

    openPathEvent(node: Node): void {
        this.onClick.emit(node);
    }

    isOpen(node: Node): boolean {
        return this.expandedNodes.includes(node.ref.id);
    }

    openOrCloseNode(node: Node): void {
        this.onClick.emit(node);
    }

    toggleNodeExpansion(event: MouseEvent, node: Node): void {
        if (this._hasChildren[node.ref.id] === false) {
            return;
        }
        event.stopPropagation();
        const index = this.expandedNodes.indexOf(node.ref.id);
        if (index < 0) {
            this.expandedNodes.push(node.ref.id);
        } else {
            this.expandedNodes.splice(index, 1);
        }
    }

    refresh() {
        if (!this._node) return;
        this.nodeApi
            .getChildren(this._node, [RestConstants.FILTER_FOLDERS], {
                count: WorkspaceSubTreeComponent.MAX_FOLDER_COUNT,
                propertyFilter: [RestConstants.ALL],
            })
            .subscribe(async (data: NodeList) => {
                this._nodes = data.nodes;
                this.moreItems = data.pagination.total - data.pagination.count;
                this.loadingStates = Helper.initArray(this._nodes.length, true);
                this.hasChildren.emit(this._nodes?.length > 0);
                this.onLoading.emit(false);
                this.loading = false;
                this.expandCurrentPath();
            });
    }

    canDropOnNode = canDropOnNode;
    onDropped(dragData: DragData<Node>) {
        this.onDrop.emit({
            target: dragData.target,
            source: {
                element: dragData.draggedNodes,
                mode: dragData.action,
            },
        });
    }
}
