import { Sort } from '@angular/material/sort';

import { SelectionModel } from '@angular/cdk/collections';
import { CustomOptions, OptionItem, Target } from '../types/option-item';
import { ListItem, ListItemSort } from '../types/list-item';
import { CanDrop, DragData, DropAction } from '../types/drag-drop';
import { Node, GenericAuthority } from 'ngx-edu-sharing-api';
import { ActionbarComponent } from '../actionbar/actionbar.component';

export type NodeRoot =
    | 'MY_FILES'
    | 'COLLECTION_HOME'
    | 'SHARED_FILES'
    | 'MY_SHARED_FILES'
    | 'TO_ME_SHARED_FILES'
    | 'WORKFLOW_RECEIVE'
    | 'RECYCLE'
    | 'ALL_FILES';

export enum NodeEntriesDisplayType {
    Table,
    Grid,
    SmallGrid,
}

export enum InteractionType {
    // create router link
    DefaultActionLink,
    // emit an event
    Emitter,
    None,
}

export type ListOptions = { [key in Target]?: OptionItem[] };
export type ListOptionsConfig = {
    actionbar?: ActionbarComponent;
    parent?: Node;
    customOptions?: CustomOptions;
};

export interface ListSortConfig extends Sort {
    columns: ListItemSort[];
    allowed?: boolean;
    customSortingInProgress?: boolean;
}

export type DropTarget = Node | NodeRoot;

export interface DropSource<T extends NodeEntriesDataType> {
    element: T[];
    // sourceList: ListEventInterface<T>;
    mode: DropAction;
}

export interface ListDragGropConfig<T extends NodeEntriesDataType> {
    dragAllowed: boolean;
    dropAllowed?: (dragData: DragData<T>) => CanDrop;
    dropped?: (target: Node, source: DropSource<NodeEntriesDataType>) => void;
}

export enum ClickSource {
    Preview,
    Icon,
    Metadata,
    Comments,
    Overlay,
    Dropdown, // keep: used in extensions
}

export type NodeClickEvent<T extends NodeEntriesDataType> = {
    element: T;
    source: ClickSource;
    attribute?: ListItem; // only when source === Metadata
};
export type FetchEvent = {
    offset: number;
    amount?: number;
    /**
     * is a reset of the current data required?
     * this should be true if this was a pagination request
     */
    reset?: boolean;
};
export type NodeEntriesDataType = Node | GenericAuthority;
export type GridLayout = 'grid' | 'scroll';
export type GridConfig = {
    /**
     * max amount of rows that should be visible, unset for no limit
     */
    maxRows?: number;
    /**
     * layout, defaults to 'grid'
     * 'scroll' may only be used when maxRows is not set
     */
    layout?: GridLayout;
};

export interface ListEventInterface<T extends NodeEntriesDataType> {
    updateNodes(nodes: void | T[]): void;

    getDisplayType(): NodeEntriesDisplayType;

    setDisplayType(displayType: NodeEntriesDisplayType): void;

    showReorderColumnsDialog(): void;

    addVirtualNodes(virtual: T[]): void;

    setOptions(options: ListOptions): void;

    /**
     * activate option (dropdown) generation
     */
    initOptionsGenerator(config: ListOptionsConfig): void | Promise<void>;

    getSelection(): SelectionModel<T>;

    /**
     * triggered when nodes/objects are deleted and should not be shown in the list anymore
     */
    deleteNodes(objects: T[]): void;
}
