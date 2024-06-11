import {
    BehaviorSubject,
    Notification,
    Observable,
    ObservableNotification,
    OperatorFunction,
    ReplaySubject,
    Subject,
    Subscription,
} from 'rxjs';
import { delay, dematerialize, filter, first, materialize, switchMap, tap } from 'rxjs/operators';

/**
 * Like `switchMap` followed by `shareReplay`, but does not emit old values to new subscribers while
 * the observable returned by `project` is in-flight.
 *
 * In other words, if an update was triggered by an emission of `source$` and a new subscriber comes
 * in, they won't get the previous value replayed. Instead, the first value they see is the result
 * of the projected observable once it emits. After that, everyone gets shared updates on newly
 * emitted values once they become available (like a normal `switchMap`-`shareReplay` combination).
 *
 * There are some additional changes in behavior compared to the `switchMap`-`shareReplay`
 * combination:
 * - `project` will not be called again automatically when there are new subscribers after an error
 *   was returned (it will be called again when `source` emits, though).
 * - `project` will not be called while there are no subscribers.
 * - `source` will not be unsubscribed from, even if there are no subscribers, so when new
 *   subscribers come in, they immediately get a replay of the last value if `source` did not emit
 *   in the meantime. (TODO: maybe add a timeout, so subscriptions won't be kept alive indefinitely)
 */
export function switchReplay<T, R>(
    project: (value: T, index: number) => Observable<R>,
): OperatorFunction<T, R> {
    return (source: Observable<T>) => {
        const inFlightSubject = new BehaviorSubject(false);
        const lastSubject = new ReplaySubject<ObservableNotification<R>>(1);
        const trigger = new Subject<T>();
        let subscription: Subscription;
        let refCount = 0;
        let missedEmission: { value: T } | null = null;
        lastSubject.subscribe(() => inFlightSubject.next(false));
        trigger
            .pipe(
                tap(() => inFlightSubject.next(true)),
                switchMap((value, index) =>
                    project(value, index).pipe(
                        materialize(),
                        filter((notification) => notification.kind !== 'C'),
                    ),
                ),
            )
            .subscribe({
                next: (notification) => lastSubject.next(notification),
                complete: () => lastSubject.complete(),
            });

        return new Observable((subscriber) => {
            refCount++;
            if (!subscription) {
                subscription = source.subscribe({
                    next: (value) => {
                        if (refCount > 0) {
                            trigger.next(value);
                        } else {
                            missedEmission = { value };
                        }
                    },
                    error: (err) => lastSubject.next({ kind: 'E', error: err }),
                    complete: () => trigger.complete(),
                });
            }
            if (missedEmission) {
                trigger.next(missedEmission.value);
                missedEmission = null;
            }
            inFlightSubject
                .pipe(
                    first((inFlight) => !inFlight),
                    delay(0),
                    switchMap(() => lastSubject),
                    dematerialize(),
                )
                .subscribe(subscriber);
            return () => {
                refCount--;
            };
        });
    };
}

// Several approaches for the `switchReplay` function.

// function switchReplayOld<T, R>(project: (value: T, index: number) => Observable<R>) {
//     const inFlightSubject = new BehaviorSubject(false);
//     return (source$: Observable<T>) => {
//         const inner$ = source$.pipe(
//             tap(() => inFlightSubject.next(true)),

//             switchMap((value, index) =>
//                 project(value, index).pipe(
//                     materialize(),
//                     filter((notification) => notification.kind !== 'C'),
//                 ),
//             ),
//             tap({
//                 next: () => inFlightSubject.next(false),
//                 error: () => inFlightSubject.next(false),
//             }),
//             shareReplay({ bufferSize: 1, refCount: true }),
//             dematerialize(),

//             // Alternative approaches.

//             // This might not be ideal. `shareReplay` will re-subscribe to `source$` when a new
//             // subscriber comes in after `source$` emitted an error.

//             // shareReplay({ bufferSize: 1, refCount: true }),

//             // This combination is an alternative, but will emit the last valid value to new
//             // subscribers, even if `source$` has emitted an error. Also `publishReplay` is
//             // deprecated.

//             // publishReplay(1),
//             // refCount(),

//             // This is new new version of the deprecated `publishReplay(), refCount()` combination
//             // above, but is not available in RXJS 6. When it becomes available in RXJS 7, other
//             // parameters might provide something closer to what we need.

//             // share({
//             //     connector: () => new ReplaySubject(1),
//             //     resetOnError: false,
//             //     resetOnComplete: false,
//             //     resetOnRefCountZero: false,
//             // }),
//         );
//         return inFlightSubject.pipe(
//             first((inFlight) => !inFlight),
//             delay(0),
//             switchMap(() => inner$),
//         );
//     };
// }
