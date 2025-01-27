import { EventEmitter, Injectable } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';

/**
 * Passes on workspace properties and functions for outside use.
 */
@Injectable({
    providedIn: 'root',
})
export class WorkspaceService {
    nodeSidebar: Node;
    nodeSidebarChange = new EventEmitter<Node>();
}
