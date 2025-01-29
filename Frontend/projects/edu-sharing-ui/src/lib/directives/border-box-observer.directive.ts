import { Directive, ElementRef, EventEmitter, OnDestroy, OnInit, Output } from '@angular/core';
import { Observable, ReplaySubject } from 'rxjs';
import { map } from 'rxjs/operators';

interface BorderBox {
    width: number;
    height: number;
}

/**
 * Gets the border-box dimensions of an element in an asynchronous manner.
 *
 * This allows subscribing to changes and using values as input to other Angular components without
 * causing changed-after-checked errors.
 */
@Directive({
    selector: '[esBorderBoxObserver]',
    exportAs: 'borderBoxObserver',
})
export class BorderBoxObserverDirective implements OnInit, OnDestroy {
    static observeElement(elementRef: ElementRef<HTMLElement>): Observable<BorderBox> {
        return new Observable((subscriber) => {
            const borderBoxObserver = new BorderBoxObserverDirective(elementRef);
            borderBoxObserver.ngOnInit();
            borderBoxObserver.borderBoxSubject.subscribe(subscriber);
            return () => borderBoxObserver.ngOnDestroy();
        });
    }

    @Output('esBorderBoxObserver') borderBoxEmitter = new EventEmitter<BorderBox>();

    private observer: ResizeObserver;
    private readonly borderBoxSubject = new ReplaySubject<BorderBox>(1);

    width$ = this.borderBoxSubject.pipe(map(({ width }) => width));
    height$ = this.borderBoxSubject.pipe(map(({ height }) => height));

    constructor(private elementRef: ElementRef<HTMLElement>) {}

    ngOnInit(): void {
        this.registerEventEmitter();
        this.registerObserver();
        // Can cause changed-after-checked errors if done synchronously.
        void Promise.resolve().then(() => this.setInitialValue());
    }

    ngOnDestroy(): void {
        this.observer.disconnect();
    }

    private registerEventEmitter(): void {
        this.borderBoxSubject.subscribe((borderBox) => this.borderBoxEmitter.emit(borderBox));
    }

    private registerObserver(): void {
        this.observer = new ResizeObserver((entries) => {
            entries.forEach((entry) => {
                const borderBoxSize: ResizeObserverSize = entry.borderBoxSize[0];
                this.borderBoxSubject.next({
                    width: borderBoxSize.inlineSize,
                    height: borderBoxSize.blockSize,
                });
            });
        });
        this.observer.observe(this.elementRef.nativeElement);
    }

    private setInitialValue(): void {
        const boundingClientRect = this.elementRef.nativeElement.getBoundingClientRect();
        this.borderBoxSubject.next({
            width: boundingClientRect.width,
            height: boundingClientRect.height,
        });
    }
}
