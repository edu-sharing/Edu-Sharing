import { Injectable, TemplateRef } from '@angular/core';
import { Node } from 'ngx-edu-sharing-api';
import { ListItemType } from '../types/list-item';
import { Scope } from '../types/option-item';

export type CustomField = {
    type: ListItemType;
    name: string | CustomFieldSpecialType;
};
export enum CustomFieldSpecialType {
    preview,
    type,
}
export type CustomFieldInfo = {
    type: ListItemType;
    /**
     * Either the property name (i.e. "cm:name") or a special value.
     */
    name: string | CustomFieldSpecialType;

    /**
     * Custom callback which should return true if the template should be used for the given item.
     */
    useCallback?: (node: Node) => boolean;
    templateRef: TemplateRef<unknown>;
};

export type PaginationStrategy = 'infinite-scroll' | 'paginator';

type PaginationScope = Scope | 'DEFAULT';
/**
 * this service is intended to add custom behaviour to the global tables & grid views
 */
@Injectable({
    providedIn: 'root',
})
export class NodeEntriesGlobalService {
    private customFields: CustomFieldInfo[] = [];
    private paginationStrategy: { [key in PaginationScope]?: PaginationStrategy } = {
        [Scope.WorkspaceList]: 'infinite-scroll',
        [Scope.Search]: 'infinite-scroll',
        DEFAULT: 'infinite-scroll',
    };
    private paginatorSizeOptions: { [key in PaginationScope]?: number[] } = {
        DEFAULT: [25, 50, 75, 100],
    };
    private customCssClassProvider: (node: Node) => string;

    constructor() {}

    public setPaginationStrategy(scope: PaginationScope, strategy: PaginationStrategy) {
        this.paginationStrategy[scope] = strategy;
    }
    public getPaginationStrategy(scope: Scope) {
        return this.paginationStrategy[scope] ?? this.paginationStrategy['DEFAULT'];
    }
    public setPaginatorSizeOptions(scope: PaginationScope, size: number[]) {
        this.paginatorSizeOptions[scope] = size;
    }
    public getPaginatorSizeOptions(scope: Scope) {
        return this.paginatorSizeOptions[scope] ?? this.paginatorSizeOptions['DEFAULT'];
    }
    public getCustomCssClass(node: Node) {
        if (this.customCssClassProvider) {
            return this.customCssClassProvider(node);
        }
        return '';
    }

    /**
     * a custom provider for a css class based on a node
     * it should return the given css class that will be added to each node entry based on the given node
     */
    public setCustomCssClassProvider(provider: (node: Node) => string) {
        this.customCssClassProvider = provider;
    }
    public getCustomFieldTemplate(item: CustomField, node: Node) {
        return this.customFields.filter(
            (c) =>
                c.type === item.type &&
                c.name === item.name &&
                (!c.useCallback || c.useCallback(node)),
        )?.[0]?.templateRef;
    }

    /**
     * Registers a custom (node) attribute you want to render via the given template.
     *
     * You may also override existing attributes if you want to provide a custom view.
     */
    registerCustomFieldRendering(customFieldInfo: CustomFieldInfo) {
        this.customFields.push(customFieldInfo);
    }
}
