import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import {
    debounce,
    filter,
    first,
    map,
    mapTo,
    scan,
    share,
    startWith,
    switchMap,
    take,
    tap,
} from 'rxjs/operators';
import { ApiRequestConfiguration } from '../api-request-configuration';
import * as apiModels from '../api/models';
import { AuthenticationV1Service as AuthenticationApiService } from '../api/services';
import { switchReplay } from '../utils/rxjs-operators/switch-replay';

export type LoginInfo = apiModels.Login;

type LoginAction =
    | {
          // Logs the user in with the provided credentials.
          kind: 'login';
          username: string;
          password: string;
          scope?: string;
      }
    | {
          // Logs the user in with the provided credentials.
          kind: 'loginToken';
          accessToken: string;
      }
    | {
          // Logs the user in with the provided credentials.
          kind: 'loginEduTicket';
          ticket: string;
      }
    | {
          // Logs the user out.
          kind: 'logout';
      }
    | {
          // Initially fetches login information from the backend.
          kind: 'initial';
      }
    | {
          // Triggers a refresh of login information after the state was changed from outside.
          kind: 'forceRefresh';
      };

type LoginActionResponse = {
    loginInfo: LoginInfo;
    loginAction: LoginAction;
};

@Injectable({
    providedIn: 'root',
})
export class AuthenticationService {
    /** Triggers requests concerning the login state. */
    // We funnel login actions through this subject, so rapidly requested actions will not update
    // `loginInfoSubject` uncontrolled but unfinished actions will be canceled cleanly.
    private readonly loginActionTrigger = new BehaviorSubject<LoginAction | null>(null);
    /**
     * The last login action with its returned login information..
     *
     * Becomes `null` while a request is underway, so subscribers to `loginInfo$` who subscribed
     * after triggering an action will receive the login information after the action has completed
     * (or was canceled).
     */
    private readonly loginActionResponseSubject = new BehaviorSubject<LoginActionResponse | null>(
        null,
    );
    /**
     * Whether there is currently a request underway that affects the login state.
     *
     * This being `true` means that we cannot currently provide information that depend on the login
     * state and should withhold any cached information until we learn whether they have to be
     * invalidated.
     *
     * The observable becomes `true` for all requests but the initial login-information fetch, which
     * doesn't affect the login state on the backend.
     *
     * A `forceRefresh` will also set this to `true` since we must assume the refresh was triggered
     * as a result of a state change.
     */
    private readonly loginActionInFlight$ = this.createLoginActionInFlight();
    /**
     * The currently known login information.
     *
     * The observable will replay the last state as long as there is no request in-flight which will
     * update the information once completed.
     */
    private readonly loginInfo$ = this.createLoginInfo();
    /** Emits when the logged-in user changes. */
    private readonly userChanges$ = this.createUserChanges();
    /**
     * A dictionary of observables for calls to `hasAccessToScope`.
     *
     * This functions as a cache to already fetched information. The observables inside invalidate
     * and update themselves when appropriate and stop doing work when there are no more
     * subscribers.
     */
    private readonly accessToScopeObservables: { [scope: string]: Observable<boolean> } = {};
    /**
     * Fires when the backend has logged out the user due to inactivity.
     */
    private readonly autoLogoutSubject = new Subject<void>();
    /**
     * The time of automatic logout due to inactivity by the backend.
     */
    private readonly autoLogoutTimeSubject = new BehaviorSubject<Date | null>(null);
    /**
     * Fires when an API request was reported that was not performed by this module.
     */
    private readonly outsideApiRequest = new Subject<void>();
    /**
     * The login information are possible invalid, but we don't want to fetch them without user
     * interaction to not reset the auto-logout timeout.'
     */
    private loginInfoNeedRefresh = false;
    /** Timeout for scheduled logout-time checks. */
    private logoutCheckTimeout: ReturnType<typeof setTimeout> | null = null;

    constructor(
        private authentication: AuthenticationApiService,
        private apiRequestConfiguration: ApiRequestConfiguration,
    ) {
        this.registerLoginActionResponseSubject();
        this.registerAutoLogoutSubjects();
    }

    /**
     * Logs in to the backend and returns the login information.
     *
     * The returned login information might be the result of a different action in case a different
     * action was triggered before the login could be completed.
     */
    login(username: string, password: string, scope?: string): Observable<LoginInfo> {
        this.loginActionTrigger.next({
            kind: 'login',
            username,
            password,
            scope,
        });
        return this.loginInfo$.pipe(first());
    }
    loginToken(accessToken: string): Observable<LoginInfo> {
        this.loginActionTrigger.next({
            kind: 'loginToken',
            accessToken,
        });
        return this.loginInfo$.pipe(first());
    }

    loginEduTicket(ticket: string): Observable<LoginInfo> {
        this.loginActionTrigger.next({
            kind: 'loginEduTicket',
            ticket,
        });
        return this.loginInfo$.pipe(first());
    }

    /**
     * Logs out of the backend and returns the login information.
     *
     * The returned login information might be the result of a different action in case a different
     * action was triggered before the logout could be completed.
     */
    logout(): Observable<LoginInfo> {
        this.loginActionTrigger.next({ kind: 'logout' });
        return this.loginInfo$.pipe(first());
    }

    /**
     * Returns a stream of login information.
     *
     * The observable is updated on any login action.
     */
    observeLoginInfo(): Observable<LoginInfo> {
        if (this.loginActionTrigger.value === null) {
            this.loginActionTrigger.next({ kind: 'initial' });
        }
        return this.loginInfo$;
    }

    hasToolpermission(toolpermission: string) {
        return this.observeLoginInfo()
            .pipe(
                map((login) => login.toolPermissions?.includes(toolpermission)),
                take(1),
            )
            .toPromise() as Promise<boolean>;
    }

    /**
     * Emits when the logged-in user changes.
     */
    observeUserChanges(): Observable<void> {
        return this.userChanges$;
    }

    /**
     * Triggers an API request to update login information.
     *
     * This is usually not needed since `login` and `logout` will update login information
     * automatically. Use only if the login state was affected by some outside actor.
     */
    forceLoginInfoRefresh(): void {
        this.loginActionTrigger.next({ kind: 'forceRefresh' });
    }

    /**
     * Returns whether the user has access to the given scope.
     *
     * The observable is updated when the user logs in or out.
     */
    observeHasAccessToScope(scope: string): Observable<boolean> {
        if (!this.accessToScopeObservables[scope]) {
            this.accessToScopeObservables[scope] = this.createHasAccessToScope(scope);
        }
        return this.accessToScopeObservables[scope];
    }

    /**
     * Fires when the user was logged out by the backend due to inactivity.
     */
    observeAutoLogout(): Observable<void> {
        return this.autoLogoutSubject;
    }

    /**
     * Returns the time of automatic logout due to inactivity by the backend.
     */
    observeAutoLogoutTime(): Observable<Date | null> {
        return this.autoLogoutTimeSubject;
    }

    /**
     * Returns the time until automatic logout due to inactivity in milliseconds.
     *
     * @param interval how often to emit the updated value in milliseconds
     */
    observeTimeUntilAutoLogout(interval: number): Observable<number | null> {
        let lastEmittedValue: number | null = null;
        return this.autoLogoutTimeSubject.pipe(
            switchMap((autoLogoutTime) =>
                autoLogoutTime
                    ? rxjs.interval(interval).pipe(
                          startWith(0),
                          map(() => autoLogoutTime.getTime() - new Date().getTime()),
                      )
                    : rxjs.of(null),
            ),
            // Debounce updates. The subscriber is probably not interested in changes smaller than
            // `interval`. To not miss emissions to arithmetic errors or slight timing offsets, we
            // use `interval / 2` as threshold.
            filter((value) => !areSimilar(value, lastEmittedValue, interval / 2)),
            tap((value) => (lastEmittedValue = value)),
        );
    }

    /**
     * Report an API request that was done without this module.
     *
     * API requests are relevant for automatic logouts by the backend.
     *
     * This is a transitionary method until all requests have been ported to this module.
     */
    reportOutsideApiRequest(): void {
        this.outsideApiRequest.next();
    }

    private registerLoginActionResponseSubject(): void {
        this.loginActionTrigger
            .pipe(
                filter((loginAction): loginAction is LoginAction => loginAction !== null),
                tap(() => this.loginActionResponseSubject.next(null)),
                switchMap((loginAction) =>
                    this.handleLoginAction(loginAction).pipe(
                        map((loginInfo) => ({ loginInfo, loginAction })),
                    ),
                ),
            )
            .subscribe((response) => this.loginActionResponseSubject.next(response));
    }

    private registerAutoLogoutSubjects(): void {
        rxjs.combineLatest([
            this.loginInfo$,
            this.apiRequestConfiguration.apiRequest,
            this.outsideApiRequest,
        ])
            .pipe(map(([loginInfo]) => this.calculateAutoLogoutTimeAfterReset(loginInfo)))
            .subscribe((logoutTime) => this.autoLogoutTimeSubject.next(logoutTime));
        this.autoLogoutTimeSubject.subscribe((autoLogoutTime) =>
            this.scheduleLogoutTimeCheck(autoLogoutTime),
        );
        // `setTimeout` timers will be paused while the system is suspended. We use the `online`
        // event to get notified of a resumed system and update the timeout.
        window.addEventListener('online', () => this.checkLogoutTime());
        this.autoLogoutSubject.subscribe(() => (this.loginInfoNeedRefresh = true));
    }

    /**
     * Schedules a check of the auto-logout time at the given time.
     *
     * Only one check can be scheduled. Previous schedules will be overwritten each time this method
     * is called.
     */
    private scheduleLogoutTimeCheck(timeOfCheck: Date | null) {
        if (this.logoutCheckTimeout) {
            clearTimeout(this.logoutCheckTimeout);
            this.logoutCheckTimeout = null;
        }
        if (timeOfCheck) {
            const delta = timeOfCheck.getTime() - new Date().getTime();
            this.logoutCheckTimeout = setTimeout(() => this.checkLogoutTime(), delta);
        }
    }

    /**
     * Check the auto-logout time now.
     *
     * Either emit the `autoLogoutSubject` if the auto-logout time has been reached, or schedule
     * another check at the time we expect it to be reached.
     */
    private checkLogoutTime() {
        if (this.autoLogoutTimeSubject.value) {
            const delta = this.autoLogoutTimeSubject.value.getTime() - new Date().getTime();
            if (delta <= 0) {
                this.autoLogoutTimeSubject.next(null);
                this.autoLogoutSubject.next();
            } else {
                this.scheduleLogoutTimeCheck(this.autoLogoutTimeSubject.value);
            }
        }
    }

    /**
     * Calculates the time the backend will automatically log out the user assuming the backend just
     * reset the timer for automatic logout.
     *
     * The automatic logout timer is reset by the backend whenever an API call is made. Use this
     * function to get the updated logout time after doing an API request.
     *
     * @returns the time of automatic logout or null, if not logged in
     */
    private calculateAutoLogoutTimeAfterReset(loginInfo: LoginInfo): Date | null {
        if (loginInfo.statusCode === 'OK') {
            return new Date(new Date().getTime() + loginInfo.sessionTimeout * 1000);
        } else {
            return null;
        }
    }

    private createLoginInfo(): Observable<LoginInfo> {
        return rxjs.of(void 0).pipe(
            tap(() => {
                // Reed `loginInfoNeedRefresh` on subscriptions to `loginInfo$` since new
                // subscriptions should only happen on user interaction.
                if (this.loginInfoNeedRefresh) {
                    this.loginActionTrigger.next({ kind: 'forceRefresh' });
                    this.loginInfoNeedRefresh = false;
                }
            }),
            switchMap(() => this.loginActionResponseSubject),
            filter((response): response is LoginActionResponse => response !== null),
            map((response) => response.loginInfo),
        );
    }

    private handleLoginAction(action: LoginAction): Observable<LoginInfo> {
        switch (action.kind) {
            case 'login':
                if (action.scope) {
                    return this.loginToScope(action.username, action.password, action.scope);
                } else {
                    return this.loginWithBasicAuth(action.username, action.password);
                }
            case 'loginToken':
                return this.loginWithToken(action.accessToken);
            case 'loginEduTicket':
                return this.loginWithEduTicket(action.ticket);
            case 'logout':
                return this.authentication.logout().pipe(switchMap(() => this.fetchLoginInfo()));
            case 'initial':
            case 'forceRefresh':
                return this.fetchLoginInfo();
        }
    }

    private loginToScope(username: string, password: string, scope: string): Observable<LoginInfo> {
        return this.authentication.loginToScope({
            body: { userName: username, password: password, scope: scope },
        });
    }

    /**
     * request authentication by providing an username and password
     */
    private loginWithBasicAuth(username: string, password: string): Observable<LoginInfo> {
        return rxjs.of(void 0).pipe(
            // Make `setBasicAuthForNextRequest` part of the observable, so it is guaranteed to
            // be run together with the login request.
            tap(() =>
                this.apiRequestConfiguration.setBasicAuthForNextRequest({ username, password }),
            ),
            switchMap(() => this.authentication.login()),
        );
    }
    /**
     * request authentication by providing an oauth token
     */
    private loginWithToken(accessToken: string): Observable<LoginInfo> {
        return rxjs.of(void 0).pipe(
            // Make `setBasicAuthForNextRequest` part of the observable, so it is guaranteed to
            // be run together with the login request.
            tap(() => this.apiRequestConfiguration.setBearerAuthForNextRequest(accessToken)),
            switchMap(() => this.authentication.login()),
        );
    }

    /**
     * request authentication by providing an edu-sharing ticket (usually gathered via authByApp)
     */
    private loginWithEduTicket(ticket: string): Observable<LoginInfo> {
        return rxjs.of(void 0).pipe(
            // Make `setBasicAuthForNextRequest` part of the observable, so it is guaranteed to
            // be run together with the login request.
            tap(() => this.apiRequestConfiguration.setEduTicketAuthForNextRequest(ticket)),
            switchMap(() => this.authentication.login()),
        );
    }

    private fetchLoginInfo(): Observable<LoginInfo> {
        return this.authentication.login();
    }

    private createLoginActionInFlight(): Observable<boolean> {
        return rxjs.combineLatest([this.loginActionTrigger, this.loginActionResponseSubject]).pipe(
            map(
                ([actionTrigger, response]) =>
                    // An action was requested.
                    actionTrigger !== null &&
                    // We don't have a response yet.
                    response === null &&
                    // The requested action was not the initial data fetch.
                    actionTrigger.kind !== 'initial',
            ),
        );
    }

    private createUserChanges(): Observable<void> {
        return this.loginActionResponseSubject.pipe(
            filter((response): response is LoginActionResponse => response !== null),
            scan((acc, response) => {
                if (acc === null && response.loginAction.kind === 'initial') {
                    // We had no information about the logged-in user so far and just refreshed the
                    // login information. We learned about the logged-in user, but they really were
                    // already logged-in before.
                    return {
                        changed: false,
                        authorityName: response.loginInfo.authorityName,
                    };
                } else if (acc?.authorityName !== response.loginInfo.authorityName) {
                    // The logged-in user changed.
                    return {
                        changed: true,
                        authorityName: response.loginInfo.authorityName,
                    };
                } else {
                    return {
                        changed: false,
                        authorityName: response.loginInfo.authorityName,
                    };
                }
            }, null as { changed: boolean; authorityName?: string } | null),
            filter((acc) => acc?.changed ?? false),
            mapTo(void 0),
            share(),
        );
    }

    /**
     * Creates the observable for `hasAccessToScope` requests for a given scope.
     *
     * The returned observable
     *   - updates on changes,
     *   - triggers a single api call for all subscribers,
     *   - guarantees up-to-date values to new subscribers, i.e., does not replay old values after a
     *     login request is made.
     */
    private createHasAccessToScope(scope: string): Observable<boolean> {
        // We use `userChanges` as starting point, so the cached response will be refreshed when
        // needed.
        const inner$ = this.userChanges$.pipe(
            startWith(void 0 as void),
            switchReplay(() => this.authentication.hasAccessToScope({ scope })),
            map((response: { hasAccess: boolean }) => response.hasAccess),
        );
        // Do not resolve the observable for new subscribers while a login request is in-flight.
        // Instead, make sure the first value they see is the one relevant after the login request
        // has completed.
        return this.loginActionInFlight$.pipe(
            first((inFlight) => !inFlight),
            switchMap(() => inner$),
        );
    }
}

function areSimilar(lhs: number | null, rhs: number | null, epsilon: number): boolean {
    if (lhs === null && rhs === null) {
        return true;
    } else if (lhs === null || rhs === null) {
        return false;
    } else {
        return Math.abs(lhs - rhs) < epsilon;
    }
}
