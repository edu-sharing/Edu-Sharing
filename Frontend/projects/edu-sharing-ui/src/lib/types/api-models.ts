import { Node } from 'ngx-edu-sharing-api';
export interface VirtualNode extends Node {
    virtual: boolean;
    /**
     * should the virtual node override an existing node
     * (if unset, equals true)
     */
    override?: boolean;
    /**
     * observe this node for changes (i.e. connector nodes)
     */
    observe?: boolean;
}
