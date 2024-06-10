import { Directive, ElementRef, Input, OnInit } from '@angular/core';

/**
 * Checks whether text of the annotated element or a descendent (give a selector) was cut of, e.g.,
 * with an ellipsis.
 *
 * Call `hasTextOverflow()` for the result.
 */
@Directive({
    selector: '[esCheckTextOverflow]',
    exportAs: 'esCheckTextOverflow',
})
export class CheckTextOverflowDirective implements OnInit {
    @Input('esCheckTextOverflow') selector?: string;

    private textElement: HTMLElement;

    hasTextOverflow = delay(this.hasTextOverflow_);

    constructor(private readonly elementRef: ElementRef<HTMLElement>) {}

    ngOnInit(): void {
        this.textElement = this.getTextElement();
    }

    private hasTextOverflow_(): boolean {
        if (!this.textElement && this.selector) {
            // refetch element in case it has changed
            this.textElement = this.getTextElement();
        }
        const element = this.textElement;
        if (element) {
            return (
                element.offsetWidth < element.scrollWidth ||
                // use buffer to prevent overflow caused by small margins
                element.offsetHeight + 5 < element.scrollHeight
            );
        } else {
            return false;
        }
    }

    private getTextElement(): HTMLElement {
        if (this.selector) {
            return this.elementRef.nativeElement.querySelector(this.selector);
        } else {
            return this.elementRef.nativeElement;
        }
    }
}

/** Delay the result of a function one tick to avoid changed-after-checked errors. */
function delay<T>(f: () => T): () => T {
    let previousValue: any = null;
    let updating = false;
    return function () {
        if (!updating) {
            const newValue = f.apply(this);
            if (newValue !== previousValue) {
                updating = true;
                Promise.resolve().then(() => {
                    previousValue = newValue;
                    updating = false;
                });
            }
        }
        return previousValue;
    };
}
