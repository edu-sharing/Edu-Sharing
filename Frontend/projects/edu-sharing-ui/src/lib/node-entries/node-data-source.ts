import { DataSource } from '@angular/cdk/collections';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { ItemsCap } from './items-cap';
import { GenericAuthority, Node, Pagination } from 'ngx-edu-sharing-api';
import { Helper } from '../util/helper';

export type LoadingState =
    // The data source is loading data for the first time.
    | 'initial'
    // Loading data after change of parameters, i.e., it will replace the current data when done.
    | 'reset'
    // Loading another page with unchanged parameters.
    | 'page'
    // Loading done.
    | false;

export class NodeDataSource<T extends Node | GenericAuthority> extends DataSource<T> {
    protected dataStream = new BehaviorSubject<T[]>([]);
    private pagination$ = new BehaviorSubject<Pagination>(null);
    // Include `LoadingState` to be type-compatible to `NodeDataSourceRemote` although not used
    // here.
    public isLoadingSubject = new BehaviorSubject<LoadingState | boolean>(false);
    get isLoading() {
        return this.isLoadingSubject.value;
    }
    set isLoading(isLoading: LoadingState | boolean) {
        this.isLoadingSubject.next(isLoading);
    }
    initialPageLoaded = false;
    protected _itemsCap: ItemsCap<T> | null;
    get itemsCap(): ItemsCap<T> | null {
        return this._itemsCap;
    }
    set itemsCap(value: ItemsCap<T> | null) {
        this._itemsCap = value;
        this.connectRenderData();
    }
    protected renderData = new BehaviorSubject<T[]>([]);
    protected renderDataSubscription: Subscription | null;

    constructor(initialData: T[] = []) {
        super();
        this.setData(initialData);
    }

    connect(): Observable<T[]> {
        if (!this.renderDataSubscription) {
            this.connectRenderData();
        }
        return this.renderData;
    }

    private connectRenderData(): void {
        this.renderDataSubscription?.unsubscribe();
        if (this.itemsCap) {
            this.renderDataSubscription = this.itemsCap
                .connect(this.dataStream)
                .subscribe((data) => this.renderData.next(data));
        } else {
            this.renderDataSubscription = this.dataStream.subscribe((data) =>
                this.renderData.next(data),
            );
        }
    }

    connectPagination(): Observable<Pagination> {
        return this.pagination$;
    }

    disconnect() {}

    setData(data: T[], pagination: Pagination = null) {
        this.dataStream.next(data);
        this.setPagination(pagination);
    }

    appendData(appendData: T[], location: 'before' | 'after' = 'after') {
        let data = this.getData();
        if (location === 'after') {
            data = data.concat(appendData);
        } else {
            data = appendData.concat(data);
        }
        this.dataStream.next(data);
    }

    /**
     * Removes elements from the visible data.
     */
    removeData(toRemove: T[]): void {
        const newData = this.getData().filter(
            (value) =>
                !toRemove.some((d) => Helper.objectEquals((d as Node).ref, (value as Node).ref)),
        );
        const removedData = this.getData().filter((value) => !newData.includes(value));
        this.dataStream.next(newData);
        if (this.pagination$.value) {
            const pagination = this.pagination$.value;
            this.setPagination({
                count: pagination.count - removedData.length,
                from: pagination.from,
                total: pagination.total - removedData.length,
            });
        }
    }

    setPagination(pagination: Pagination) {
        this.pagination$.next(pagination);
    }

    reset() {
        this.setData([]);
    }

    hasMore() {
        if (!this.pagination$.value) {
            return undefined;
        }
        return this.pagination$.value.total > this.dataStream.value?.length;
    }

    // FIXME: This is somewhat dangerous because we rely on `connect` being called from outside, but
    // this method provides a way to access data without ever calling `connect`.
    getData() {
        return this.renderData.value;
    }

    isEmpty(): boolean {
        return this.dataStream.value?.length === 0;
    }

    getTotal() {
        return this.pagination$.value?.total
            ? Math.max(this.pagination$.value?.total, this.getData()?.length || 0)
            : this.dataStream.value?.length ?? 0;
    }

    isFullyLoaded() {
        return this.getTotal() <= this.dataStream.value?.length;
    }

    /**
     * force a refresh of all elements in the current data stream
     * trigger this to enforce a rebuild of the nodes in all sub-components
     * i.e. if data from some nodes has changed
     */
    refresh() {
        this.dataStream.next(Helper.deepCopy(this.dataStream.value));
    }
}
