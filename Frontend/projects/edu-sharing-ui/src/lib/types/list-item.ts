import { Sort, SortDirection } from '@angular/material/sort';
import { Observable } from 'rxjs';
import { EventEmitter } from '@angular/core';

export type ListItemType = 'NODE' | 'NODE_PROPOSAL' | 'COLLECTION' | 'ORG' | 'GROUP' | 'USER';

/**
 * A list item info, which is basically a column
 * Example:
 this.columns.push(new ListItem(RestConstants.CM_NAME));
 this.columns.push(new ListItem(RestConstants.CM_ARCHIVED_DATE));
 */
export class ListItem {
    /**
     * Should this item be shown by default
     * @type {boolean}
     */
    public visible = true;

    /**
     * Label to display, if set, should be preferred instead of automatic i18n
     */
    public label: string;

    /**
     * custom format string for date fields, may be null
     */
    public format: string;
    constructor(
        public type: ListItemType,
        public name: string,
        public config = {
            showLabel: false,
        },
    ) {}

    static getCollectionDefaults() {
        let columns = [];
        columns.push(new ListItem('COLLECTION', 'title'));
        columns.push(new ListItem('COLLECTION', 'info'));
        columns.push(new ListItem('COLLECTION', 'scope'));
        return columns;
    }
}
export class ListItemSort extends ListItem {
    constructor(
        public type: ListItemType,
        public name: string,
        public mode: 'ascending' | 'descending' | null = null,
        public config = {
            showLabel: false,
        },
    ) {
        super(type, name, config);
    }
}
export class SortEvent extends ListItemSort {
    ascending: boolean;
}

/**
 * UI element that allows the user to choose sorting.
 */
export interface SortPanel {
    active: string;
    direction: SortDirection;
    readonly sortChange: EventEmitter<Sort>;
}
