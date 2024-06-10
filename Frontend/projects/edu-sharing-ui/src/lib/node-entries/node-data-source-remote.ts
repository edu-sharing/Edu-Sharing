import { NodeDataSource } from './node-data-source';
import { GenericAuthority, Node } from 'ngx-edu-sharing-api';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import { PaginationStrategy } from './node-entries-global.service';
import { Sort } from '@angular/material/sort';
import { SortPanel } from '../types/list-item';
import { MatPaginator } from '@angular/material/paginator';

export interface PaginationConfig {
    defaultPageSize: number;
    strategy: PaginationStrategy;
}

export abstract class NodeDataSourceRemote<
    T extends Node | GenericAuthority = Node,
    P extends MatPaginator = MatPaginator,
> extends NodeDataSource<T> {
    abstract get paginator(): P | null;
    abstract set paginator(value: P | null);
    abstract get sortPanel(): SortPanel;
    abstract set sortPanel(value: SortPanel);
    abstract loadMore(): boolean;

    abstract registerQueryParameters(route: ActivatedRoute): void;

    abstract init({
        paginationConfig,
        defaultSort,
    }: {
        paginationConfig: PaginationConfig;
        defaultSort: Sort;
    }): void;
}
