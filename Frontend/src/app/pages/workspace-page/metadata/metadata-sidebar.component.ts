import { trigger } from '@angular/animations';
import { Component, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { LocalEventsService, UIAnimation, UIConstants } from 'ngx-edu-sharing-ui';
import {
    NodeVersions,
    RestConstants,
    RestNodeService,
    Version,
} from '../../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { Toast } from '../../../services/toast';
import { YES_OR_NO } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { BulkBehavior } from '../../../features/mds/types/types';
import { WorkspaceService } from '../workspace.service';

/**
 * Container Component for the workspace's metadata sidebar.
 *
 * Handles input and output of the inner metadata component and shows / hides the sidebar as
 * requested.
 */
@Component({
    selector: 'es-metadata-sidebar',
    templateUrl: './metadata-sidebar.component.html',
    styleUrls: ['./metadata-sidebar.component.scss'],
    animations: [trigger('fromRight', UIAnimation.fromRight())],
})
export class MetadataSidebarComponent {
    get nodeSidebar() {
        return this.workspace.nodeSidebar;
    }

    constructor(
        private dialogs: DialogsService,
        private localEvents: LocalEventsService,
        private node: RestNodeService,
        private router: Router,
        private toast: Toast,
        private workspace: WorkspaceService,
    ) {}

    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            if (this.workspace.nodeSidebar != null) {
                this.closeSidebar();
                event.preventDefault();
                event.stopPropagation();
                return;
            }
        }
    }

    async restoreVersion(restore: { version: Version; node: Node }) {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'WORKSPACE.METADATA.RESTORE_TITLE',
            message: 'WORKSPACE.METADATA.RESTORE_MESSAGE',
            buttons: YES_OR_NO,
            nodes: [restore.node],
        });
        dialogRef.afterClosed().subscribe((response) => {
            if (response === 'YES') {
                this.doRestoreVersion(restore.version);
            }
        });
    }

    private doRestoreVersion(version: Version): void {
        this.toast.showProgressSpinner();
        this.node
            .revertNodeToVersion(
                version.version.node.id,
                version.version.major,
                version.version.minor,
            )
            .subscribe(
                (data: NodeVersions) => {
                    this.toast.closeProgressSpinner();
                    this.closeSidebar();
                    // @TODO type is not compatible
                    this.node
                        .getNodeMetadata(version.version.node.id, [RestConstants.ALL])
                        .subscribe(
                            (node) => {
                                this.localEvents.nodesChanged.emit([node.node]);
                                this.workspace.nodeSidebar = node.node;
                                this.workspace.nodeSidebarChange.emit(node.node);
                                this.toast.toast('WORKSPACE.REVERTED_VERSION');
                            },
                            (error: any) => this.toast.error(error),
                        );
                },
                (error: any) => this.toast.error(error),
            );
    }

    closeSidebar() {
        this.workspace.nodeSidebar = null;
        this.workspace.nodeSidebarChange.emit(null);
    }

    goToNode(node: Node) {
        if (node.content.version) {
            void this.router.navigate([
                UIConstants.ROUTER_PREFIX + 'render',
                node.ref.id,
                node.content.version,
            ]);
        } else {
            void this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id]);
        }
    }

    async openMdsEditor(nodes: Node[]): Promise<void> {
        const dialogRef = await this.dialogs.openMdsEditorDialogForNodes({
            nodes,
            bulkBehavior: BulkBehavior.Default,
        });
        dialogRef
            .afterClosed()
            .subscribe((updatedNodes) => this.closeMdsEditor(nodes, updatedNodes));
    }

    private closeMdsEditor(originalNodes: Node[], updatedNodes: Node[] = null) {
        let refresh = !!updatedNodes;
        if (
            this.workspace.nodeSidebar &&
            this.workspace.nodeSidebar.ref.id === originalNodes[0]?.ref.id &&
            updatedNodes
        ) {
            this.workspace.nodeSidebar = updatedNodes[0];
        }
        if (refresh) {
            this.localEvents.nodesChanged.emit(updatedNodes);
        }
    }
}
