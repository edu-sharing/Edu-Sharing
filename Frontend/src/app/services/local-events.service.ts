import { EventEmitter, Injectable } from '@angular/core';
import { Node } from '../core-module/core.module';

/**
 * An application-wide event broker.
 */
@Injectable({
    providedIn: 'root',
})
export class LocalEventsService {
    /**
     * The metadata of one or more nodes have been updated.
     *
     * The emitted value is the array of updated nodes.
     *
     * The emitter should not be triggered with an empty array or null.
     */
    readonly nodesChanged = new EventEmitter<Node[]>();
    /**
     * One or more nodes have been moved to the recycle bin.
     *
     * The emitted value is the array of former nodes.
     *
     * The emitter should not be triggered with an empty array or null.
     */
    readonly nodesDeleted = new EventEmitter<Node[]>();
}
