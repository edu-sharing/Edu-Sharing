import { trigger } from '@angular/animations';
import { Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { AppContainerService, UIAnimation } from 'ngx-edu-sharing-ui';
import { Subject } from 'rxjs';

/**
 * Shows a small button on the bottom of the screen that scrolls to the page top.
 *
 * Place after all visible content for correct keyboard focus order.
 */
@Component({
    selector: 'es-scroll-to-top-button',
    templateUrl: './scroll-to-top-button.component.html',
    styleUrls: ['./scroll-to-top-button.component.scss'],
    animations: [trigger('fade', UIAnimation.fade())],
})
export class ScrollToTopButtonComponent implements OnInit, OnDestroy {
    showScrollToTop = false;

    private readonly destroyed$ = new Subject<void>();

    constructor(private appContainer: AppContainerService, private ngZone: NgZone) {}

    ngOnInit() {
        this.appContainer.scrollContainerChange.subscribe(() => this.registerScroll());
        this.registerScroll();
    }

    private registerScroll() {
        this.appContainer.registerScrollEvents(() => this.handleScroll(), this.destroyed$);
    }

    ngOnDestroy() {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    private handleScroll() {
        const showScrollTop = this.appContainer.getScrollContainer().scrollTop > 400;
        if (showScrollTop !== this.showScrollToTop) {
            this.ngZone.run(() => (this.showScrollToTop = showScrollTop));
        }
    }

    scrollToTop() {
        this.appContainer.getScrollContainer().scroll({
            top: 0,
            behavior: 'smooth',
        });
    }
}
