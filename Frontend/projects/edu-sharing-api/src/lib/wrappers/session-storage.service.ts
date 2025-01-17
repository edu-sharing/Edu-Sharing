import { Injectable } from '@angular/core';
import { combineLatest, EMPTY, merge, Observable, of, Subject, throwError } from 'rxjs';
import {
    catchError,
    distinctUntilChanged,
    filter,
    first,
    map,
    shareReplay,
    startWith,
    switchMap,
} from 'rxjs/operators';
import { RestConstants } from '../rest-constants';
import { UserService } from './user.service';
import { AuthenticationService } from './authentication.service';

/**
 * Service to store any data in session.
 *
 * - NEW: Now stored in repository
 * - OLD: stored as cookie
 *
 * This is not the cleanest solution, but the current available modules for ng are not that great
 * either. Note that it currently only supports strings. If a user is logged in, it will be stored
 * inside the user profile. Otherwise, localStorage will be used.
 */
@Injectable({ providedIn: 'root' })
export class SessionStorageService {
    static readonly KEY_WORKSPACE_SORT = 'workspace_sort';
    static readonly KEY_ROOT_COLLECTIONS = 'collections_root';

    private readonly localStorage = new BrowserStorage(localStorage);
    private readonly sessionStorage = new BrowserStorage(sessionStorage);

    /** User preferences from the backend were changed locally and pushed to the backend. */
    private readonly userPreferencesChanged = new Subject<{ [key: string]: any }>();
    /** User preferences in sync with the backend. */
    private readonly userPreferences: Observable<{ [key: string]: any }>;
    /** Refresh user preferences to reflect changes on the backend from outside this app. */
    private readonly triggerRefresh = new Subject<void>();

    constructor(private userService: UserService, private authentication: AuthenticationService) {
        // The currently logged in user. `null` for guest or no/invalid login.
        const currentUser = this.authentication.observeLoginInfo().pipe(
            filter((login) => login !== null),
            map((login) =>
                login.statusCode === RestConstants.STATUS_CODE_OK && !login.isGuest
                    ? login.authorityName
                    : null,
            ),
            distinctUntilChanged(),
        );
        // User preferences on the backend, updated when user changes and on `refresh`.
        const remoteUserPreferences = combineLatest([
            currentUser,
            // Wrongly detected as deprecated use of startWith. The `undefined` parameter is
            // important!
            //
            // tslint:disable-next-line: deprecation
            this.triggerRefresh.pipe(startWith(undefined)),
        ]).pipe(
            switchMap(([user]) =>
                // We use `null` as an indicator that preferences cannot be stored with the user
                // profile.
                user
                    ? // The backend might return `null` in case no preferences have been saved yet.
                      // We map to the empty object if that happens.
                      this.userService
                          .getUserPreferences()
                          .pipe(map((preferences) => preferences ?? {}))
                    : // We pass null when there is no valid user.
                      of(null),
            ),
        );
        // User preferences combining data from the backend and local changes pushed to the backend.
        // Will set to `null` while the user is not logged in.
        this.userPreferences = merge(remoteUserPreferences, this.userPreferencesChanged).pipe(
            // @ts-ignore
            shareReplay(1),
        );
    }

    /** Re-syncs with the backend after changes to the backend from outside this app. */
    refresh() {
        this.triggerRefresh.next();
    }

    /**
     * Gets a current storage value from the backend or browser storage, depending on login state
     * and `store`.
     */
    get(key: string, fallback: any = null, store = Store.UserProfile): Observable<any> {
        return this.observe(key, fallback, store).pipe(first());
    }

    /**
     * Continually observes a storage value.
     *
     * Will update
     * - on login state changes,
     * - when the value is updated from within this app, and
     * - when `refresh` is called.
     */
    observe(key: string, fallback: any = null, store = Store.UserProfile): Observable<any> {
        return (() => {
            switch (store) {
                case Store.UserProfile:
                    return this.observeFromUserProfile(key, fallback);
                case Store.Session:
                    return this.sessionStorage.observe(key, fallback);
            }
        })().pipe(
            // Return a deep copy to prevent manipulation of our internal state from outside and
            // bleeding over of manipulation from one client to another. Also, this gives us the
            // chance to avoid unnecessary emits using `distinctUntilChanged`.
            map((value) => JSON.stringify(value)),
            distinctUntilChanged(),
            map((value) => JSON.parse(value)),
        );
    }

    /**
     * Updates a storage value, immediately reflecting the changes within this service and on the
     * storage.
     */
    // Use a promise for `set` since an observable needs to be subscribed to to have an effect,
    // which users are likely to forget.
    async set(key: string, value: any, store = Store.UserProfile): Promise<void> {
        switch (store) {
            case Store.UserProfile:
                const obj: any = {};
                obj[key] = value;
                return this.setToUserProfile(obj);
            case Store.Session:
                return this.sessionStorage.set(key, value);
        }
    }

    /**
     * Sets multiple values at a time.
     *
     * Otherwise identical to `set`.
     */
    async setValues(values: { [key: string]: any }, store = Store.UserProfile): Promise<void> {
        if (store === Store.UserProfile) {
            return this.setToUserProfile(values);
        } else {
            for (const [key, value] of Object.entries(values)) {
                this.set(key, value, store);
            }
        }
    }

    async delete(key: string, store = Store.UserProfile): Promise<void> {
        switch (store) {
            case Store.UserProfile:
                return this.deleteFromUserProfile(key);
            case Store.Session:
                return this.sessionStorage.delete(key);
        }
    }

    private observeFromUserProfile(key: string, fallback: any): Observable<any> {
        return this.userPreferences.pipe(
            switchMap((preferences) => {
                if (preferences) {
                    return of(preferences[key] ?? fallback);
                } else {
                    return this.localStorage.observe(key, fallback);
                }
            }),
        );
    }

    private setToUserProfile(values: { [key: string]: any }): Promise<void> {
        // @ts-ignore
        return this.userPreferences
            .pipe(
                first(),
                switchMap((preferences) => {
                    if (preferences) {
                        const updatedPreferences = { ...preferences, ...values };
                        // Optimistically update our reference before the backend confirms the
                        // change.
                        this.userPreferencesChanged.next(updatedPreferences);
                        return this.userService.setUserPreferences(updatedPreferences).pipe(
                            catchError((error) => {
                                // Reset to the previous state in case the backend didn't accept the
                                // change.
                                this.userPreferencesChanged.next(preferences);
                                return throwError(error);
                            }),
                        );
                    } else {
                        for (const [key, value] of Object.entries(values)) {
                            this.localStorage.set(key, value);
                        }
                        return EMPTY;
                    }
                }),
            )
            .toPromise();
    }

    private deleteFromUserProfile(key: string): Promise<void> {
        // @ts-ignore
        return this.userPreferences
            .pipe(
                first(),
                switchMap((preferences) => {
                    if (preferences) {
                        const updatedPreferences = { ...preferences };
                        delete updatedPreferences[key];
                        this.userPreferencesChanged.next(updatedPreferences);
                        return this.userService.setUserPreferences(updatedPreferences).pipe(
                            catchError((error) => {
                                this.userPreferencesChanged.next(preferences);
                                return throwError(error);
                            }),
                        );
                    } else {
                        this.localStorage.delete(key);
                        return EMPTY;
                    }
                }),
            )
            .toPromise();
    }
}

export enum Store {
    /** The user profile, if available, otherwise localStorage. */
    UserProfile,
    /** Only the current running session (via sessionStorage). */
    Session,
}

class BrowserStorage {
    private entryChanged = new Subject<{ key: string; value: any }>();

    constructor(private storage: Storage) {}

    get(key: string, fallback: any): any {
        const rawValue = this.storage.getItem(key);
        return rawValue ? JSON.parse(rawValue) : fallback;
    }

    observe(key: string, fallback: any): Observable<any> {
        return this.entryChanged.pipe(
            filter((entry) => entry.key === key),
            map((entry) => entry.value),
            startWith(this.get(key, fallback)),
        );
    }

    set(key: string, value: any): void {
        this.storage.setItem(key, JSON.stringify(value));
        this.entryChanged.next({ key, value });
    }

    delete(key: string): void {
        this.storage.removeItem(key);
        this.entryChanged.next({ key, value: null });
    }
}
