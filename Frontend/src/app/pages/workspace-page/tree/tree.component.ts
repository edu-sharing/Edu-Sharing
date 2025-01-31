import { Component, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import {
    CanDrop,
    DragData,
    DropSource,
    NodeRoot,
    OptionItem,
    OptionsHelperDataService,
    Scope,
} from 'ngx-edu-sharing-ui';
import { WorkspacePageComponent } from '../workspace-page.component';
import { WorkspaceSubTreeComponent } from '../sub-tree/sub-tree.component';
import { NodeEntriesService } from 'ngx-edu-sharing-ui/services/node-entries.service';

@Component({
    selector: 'es-workspace-tree',
    templateUrl: 'tree.component.html',
    styleUrls: ['tree.component.scss'],
    providers: [NodeEntriesService, OptionsHelperDataService],
})
export class WorkspaceTreeComponent {
    @Input() root: NodeRoot;
    @Input() workspace: WorkspacePageComponent;
    @Input() isSafe: boolean;
    @Input() selectedNode: string;
    @Input() set path(path: Node[]) {
        const pathIds = path.map((node) => node.ref.id);
        this.currentPath = pathIds;
    }
    @Input() options: OptionItem[] = [];

    @Output() openNode = new EventEmitter<Node>();
    @Output() updateOptions = new EventEmitter<Node>();
    @Output() setRoot = new EventEmitter<NodeRoot>();
    @Output() dropElement = new EventEmitter<{ target: Node; source: DropSource<Node> }>();
    @Output() deleteNodes = new EventEmitter();

    @ViewChild(WorkspaceSubTreeComponent) subTree: WorkspaceSubTreeComponent;

    readonly MY_FILES = 'MY_FILES';
    readonly SHARED_FILES = 'SHARED_FILES';
    readonly MY_SHARED_FILES = 'MY_SHARED_FILES';
    readonly TO_ME_SHARED_FILES = 'TO_ME_SHARED_FILES';
    readonly WORKFLOW_RECEIVE = 'WORKFLOW_RECEIVE';
    readonly RECYCLE: 'RECYCLE' = 'RECYCLE';

    currentPath: string[] = [];

    constructor(private optionsHelperDataService: OptionsHelperDataService) {
        this.optionsHelperDataService.setData({
            scope: Scope.WorkspaceTree,
        });
        void this.optionsHelperDataService.initComponents();
    }

    canDropOnRecycle = (dragData: DragData<'RECYCLE'>): CanDrop => {
        return { accept: dragData.action === 'move' };
    };

    doSetRoot(root: NodeRoot) {
        this.setRoot.emit(root);
        this.currentPath = [];
    }

    onNodesDrop(dragData: DragData<'RECYCLE'>) {
        if (dragData.target === this.RECYCLE && dragData.action === 'move') {
            this.deleteNodes.emit(dragData.draggedNodes);
        }
    }

    drop(event: any) {
        this.dropElement.emit(event);
    }

    doUpdateOptions(event: Node) {
        this.updateOptions.emit(event);
    }

    doOpenNode(event: Node) {
        this.openNode.emit(event);
    }

    public refresh() {
        this.subTree?.refresh();
    }
}
