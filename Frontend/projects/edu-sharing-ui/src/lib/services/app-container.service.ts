import { Injectable, NgZone } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { take } from 'rxjs/operators';

/**
 * Provides methods on the container that hosts the application.
 *
 * Usually, this will be `window`, but in case the application is embedded as a web component, it is
 * a `HTMLElement`.
 */
@Injectable({
    providedIn: 'root',
})
export class AppContainerService {
    private scrollContainer?: HTMLElement;

    public scrollContainerChange = new Subject<HTMLElement>();

    constructor(private ngZone: NgZone) {}

    init(appElement: HTMLElement): void {
        this.registerScrollContainer(appElement);
    }

    /**
     * Registers one or more events on the application's scroll container.
     *
     * Note that the callback will be called outside of ng zone, so you must wrap any action that
     * should be reflected by Angular with `ngZone.run()`.
     */
    registerScrollEvents(
        callback: (event: Event) => void,
        until: Observable<void>,
        { events = ['scroll'] } = {},
    ) {
        this.ngZone.runOutsideAngular(() => {
            const scrollTarget = this.scrollContainer ?? window;
            for (const event of events) {
                scrollTarget.addEventListener(event, callback);
                until
                    .pipe(take(1))
                    .subscribe(() => scrollTarget.removeEventListener(event, callback));
            }
        });
    }

    /**
     * Return true if there is a scrolling ancestor that encompasses the app other than the document
     * element.
     */
    hasScrollContainer(): boolean {
        return this.scrollContainer != null;
    }

    /**
     * Returns the nearest scrolling container element that encompasses the app if the app is
     * embedded in another web page.
     *
     * If the app is served natively, returns the document element.
     */
    getScrollContainer(): HTMLElement;
    getScrollContainer<T>({ fallback }: { fallback: T }): T;
    getScrollContainer({ fallback = document.documentElement } = {}): HTMLElement {
        return this.scrollContainer ?? fallback;
    }

    private registerScrollContainer(appElement: HTMLElement): void {
        this.scrollContainer = this.getNearestScrollingAncestor(appElement);
        this.scrollContainerChange.next(this.scrollContainer);
    }

    private getNearestScrollingAncestor(element: HTMLElement): HTMLElement | null {
        while (element.parentElement) {
            const overflowStyle = window.getComputedStyle(element.parentElement).overflowY;
            if (['auto', 'scroll'].includes(overflowStyle)) {
                return element.parentElement;
            }
            element = element.parentElement;
        }
        return null;
    }
}
