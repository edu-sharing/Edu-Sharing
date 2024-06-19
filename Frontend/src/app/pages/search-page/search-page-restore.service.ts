import { ViewportScroller } from '@angular/common';
import { Injectable } from '@angular/core';
import { NavigationStart, Router } from '@angular/router';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Subject } from 'rxjs';
import { debounceTime, delay, filter, map, switchMap } from 'rxjs/operators';
import { NodeDataSourceRemote, NodeDataSourceRemoteState } from './node-data-source-remote';
import { notNull } from 'ngx-edu-sharing-ui';
import { SearchPageState } from './search-page-results.service';

class RestoreEntry {
    /**
     * The event's lifecycle state.
     *
     * - 'active': The entry represents the active page. State is being pushed into the entry.
     * - 'restore': The browser has just navigated back to page represented by the entry. The entry is
     *   ready to be restored.
     */
    state: 'active' | 'restore' = null;
    searchState: SearchPageState = null;
    scrollPosition: [number, number] | null = null;
    dataSourceStates: { [key: string]: NodeDataSourceRemoteState } = {};
}

@Injectable({
    providedIn: 'root',
})
export class SearchPageRestoreService {
    private readonly _entries: RestoreEntry[] = [];
    private readonly _restoreScrollTrigger = new Subject<RestoreEntry>();

    private _currentNavigationId = 1;

    constructor(private _router: Router, private _viewportScroller: ViewportScroller) {
        this._registerRouterEvents();
        this._registerRestoreScrollTrigger();
    }

    registerSearchState(state: BehaviorSubject<SearchPageState>): void {
        state.subscribe((s) => (this._getEntryOrCreate().searchState = s));
    }
    registerDataSource(key: string, dataSource: NodeDataSourceRemote): void {
        dataSource
            .connect()
            .pipe(
                filter(() => !dataSource.isLoading),
                debounceTime(0),
            )
            .subscribe(() => this._putState(key, dataSource.dumpState()));
        dataSource.registerRestoreFunction(() => this._restoreState(key));
    }

    private _restoreState(key: string): NodeDataSourceRemoteState | null {
        const entry = this.getRestoreEntry();
        this._restoreScrollTrigger.next(entry);
        return entry?.dataSourceStates[key] ?? null;
    }

    private _registerRestoreScrollTrigger(): void {
        this._restoreScrollTrigger
            .pipe(
                filter(notNull),
                debounceTime(0),
                map(({ scrollPosition }) => scrollPosition),
                filter(notNull),
                // In case the page hasn't been populated yet, wait another tick before attempting
                // to scroll.
                switchMap((scrollPosition) => {
                    if (
                        document.documentElement.scrollHeight <
                        window.innerHeight + scrollPosition[1]
                    ) {
                        return rxjs.of(scrollPosition).pipe(delay(0));
                    } else {
                        return rxjs.of(scrollPosition);
                    }
                }),
            )
            .subscribe((scrollPosition) => {
                this._viewportScroller.scrollToPosition(scrollPosition);
            });
    }

    private _putState(key: string, state: NodeDataSourceRemoteState): void {
        const entry = this._getEntryOrCreate();
        entry.state = 'active';
        entry.dataSourceStates[key] = state;
    }

    getRestoreEntry(): RestoreEntry | null {
        return this._entries.find(({ state }) => state === 'restore') ?? null;
    }

    private _getEntryOrCreate(): RestoreEntry {
        let entry = this._entries[0];
        if (entry == null) {
            entry = new RestoreEntry();
            this._entries.push(entry);
        }
        return entry;
    }

    private _registerRouterEvents(): void {
        // store the last scroll position
        this._router.events
            .pipe(
                filter((event): event is NavigationStart => event instanceof NavigationStart),
                filter(
                    (event) =>
                        event.url.includes('/render') && event.navigationTrigger === 'imperative',
                ),
            )
            .subscribe((event) => {
                this._getEntryOrCreate().scrollPosition = [window.scrollX, window.scrollY];
            });
        // load the last scroll position
        this._router.events
            .pipe(
                filter((event): event is NavigationStart => event instanceof NavigationStart),
                filter(
                    (event) =>
                        event.url.includes('/search') && event.navigationTrigger === 'popstate',
                ),
            )
            .subscribe((event) => {
                const restoredId = event.restoredState.navigationId;
                const entry = this._entries[0];
                if (entry) {
                    entry.state = 'restore';
                }
            });
    }
}
