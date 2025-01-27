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

    @Output() onOpenNode = new EventEmitter();
    @Output() onUpdateOptions = new EventEmitter();
    @Output() onSetRoot = new EventEmitter();
    @Output() onDrop = new EventEmitter<{ target: Node; source: DropSource<Node> }>();
    @Output() onDeleteNodes = new EventEmitter();

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
        this.optionsHelperDataService.initComponents();
    }

    canDropOnRecycle = (dragData: DragData<'RECYCLE'>): CanDrop => {
        return { accept: dragData.action === 'move' };
    };

    setRoot(root: string) {
        this.onSetRoot.emit(root);
        this.currentPath = [];
    }

    onNodesDrop(dragData: DragData<'RECYCLE'>) {
        if (dragData.target === this.RECYCLE && dragData.action === 'move') {
            this.onDeleteNodes.emit(dragData.draggedNodes);
        }
    }

    drop(event: any) {
        this.onDrop.emit(event);
    }

    updateOptions(event: Node) {
        this.onUpdateOptions.emit(event);
    }

    openNode(event: Node) {
        this.onOpenNode.emit(event);
    }

    public refresh() {
        this.subTree?.refresh();
    }
}
