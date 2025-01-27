import { RestConstants } from '../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { CanDrop, DragData } from 'ngx-edu-sharing-ui';

export function canDropOnNode(dragData: DragData<Node>): CanDrop {
    if (dragData.draggedNodes.includes(dragData.target)) {
        // When the target is the exact same object as one of the dragged nodes, the user is
        // hovering over a placeholder of a dragged node. They likely want to cancel the drag
        // operation, so we don't print an error message.
        return { accept: false, denyExplicit: false };
    } else if (!dragData.target.isDirectory) {
        return { accept: false, denyExplicit: false, denyReason: 'WORKSPACE.TARGET_NO_DIRECTORY' };
    } else if (dragData.draggedNodes.some((node) => node.ref.id === dragData.target.ref.id)) {
        return {
            accept: false,
            denyExplicit: false,
            denyReason: 'WORKSPACE.SOURCE_TARGET_IDENTICAL',
        };
    } else if (dragData.draggedNodes.some((node) => node.parent?.id === dragData.target.ref.id)) {
        return {
            accept: false,
            denyExplicit: false,
            denyReason: 'WORKSPACE.SOURCE_TARGET_IDENTICAL',
        };
    } else if (!dragData.target.access.includes(RestConstants.ACCESS_WRITE)) {
        return {
            accept: false,
            denyExplicit: true,
            denyReason: 'WORKSPACE.TARGET_NO_WRITE_PERMISSIONS',
        };
    } else {
        return canDragDrop(dragData);
    }
}

export function canDragDrop(dragData: DragData): CanDrop {
    if (
        dragData.action === 'move' &&
        dragData.draggedNodes.some((node) => !node.access.includes(RestConstants.ACCESS_WRITE))
    ) {
        return {
            accept: false,
            denyExplicit: true,
            denyReason: 'WORKSPACE.SOURCE_NO_WRITE_PERMISSIONS',
        };
    } else if (dragData.action === 'link') {
        return {
            accept: false,
            denyExplicit: true,
            denyReason: 'WORKSPACE.FEATURE_NOT_IMPLEMENTED',
        };
    } else {
        return { accept: true };
    }
}
