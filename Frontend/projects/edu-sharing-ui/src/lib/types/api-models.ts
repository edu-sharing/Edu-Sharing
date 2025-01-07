import { Node } from 'ngx-edu-sharing-api';
export interface VirtualNode extends Node {
    virtual: boolean;
    /**
     * observe this node for changes (i.e. connector nodes)
     */
    observe?: boolean;
}
