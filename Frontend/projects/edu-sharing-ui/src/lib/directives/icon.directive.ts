/**
 * Created by Torsten on 13.01.2017.
 */

import { coerceBooleanProperty } from '@angular/cdk/coercion';
import {
    Directive,
    ElementRef,
    Input,
    OnDestroy,
    OnInit,
    Optional,
    Renderer2,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { BehaviorSubject, combineLatest } from 'rxjs';
import { filter } from 'rxjs/operators';
import { notNull } from '../util/functions';

type IconsConfig = Array<{ original: string; replace?: string; cssClass?: string }>;

/**
 * Replaces the element's content with an icon.
 *
 * Example: `<i esIcon="save"></i>`
 *
 * Optionally, a translated `aria-label` can be attached by setting `aria` to a truthy value: `<i
 * esIcon="save" aria="true"></i>`. Otherwise, `aria-hidden` will be set.
 *
 * For backwards compatibility, the directive is also activated on elements that set
 * `class="material-icons"`. This is mainly to set the `aria-hidden` attribute. Occurrences should
 * be updated to the syntax above.
 */
@Directive({
    selector: 'i[esIcon], i.material-icons',
})
export class IconDirective implements OnInit, OnDestroy {
    private originalId$ = new BehaviorSubject<string>(null);
    private _id: string;
    private _aria: boolean;
    private altTextSpan: HTMLElement;
    private isReady = false;
    private svg: HTMLImageElement;

    /**
     * An alt text to show to screen readers.
     *
     * If omitted, the icon will be invisible to screen readers.
     *
     * @see https://material.angular.io/components/icon/overview#indicator-icons
     */
    @Input() set altText(altText: string) {
        this.setAltText(altText);
    }

    /** If true, an alt text (see above) will be set based on the icon. */
    @Input() set aria(aria: boolean) {
        aria = coerceBooleanProperty(aria);
        if (aria !== this._aria) {
            this._aria = aria;
            if (this.isReady) {
                this.updateAria();
            }
        }
    }

    @Input() set esIcon(id: string) {
        this.originalId$.next(id);
    }

    constructor(
        private element: ElementRef<HTMLElement>,
        private translate: TranslateService,
        private renderer: Renderer2,
        @Optional() private config: ConfigService,
    ) {
        combineLatest([this.originalId$.pipe(filter(notNull)), this.config.get('icons', null)])
            .pipe(takeUntilDestroyed())
            .subscribe(([originalId, iconsConfig]) => this.setIcon(originalId, iconsConfig));
    }

    async ngOnInit() {
        this.isReady = true;
        this.element.nativeElement.setAttribute('aria-hidden', 'true');
        // Material styles expect icons to have the class `mat-icon`, e.g.,
        // https://github.com/angular/components/blob/ae0b9e1c1bae5e937d039ea53652fe1656bc4623/src/material/form-field/form-field.scss#L156
        this.element.nativeElement.classList.add('mat-icon');
        this.updateAria();
    }

    ngOnDestroy(): void {
        if (this.altTextSpan) {
            this.altTextSpan.remove();
        }
    }

    private setIcon(id: string, iconsConfig: IconsConfig) {
        if (this._id) {
            this.element.nativeElement.classList.remove(
                'edu-icons',
                'custom-icons',
                'material-icons',
            );
            if (this.svg) {
                this.renderer.removeChild(this.element.nativeElement, this.svg);
            }
        }
        if (id.startsWith('svg-')) {
            this.svg = document.createElement('img');
            this.svg.classList.add('svg-icons');
            this.svg.src = 'assets/images/icons/' + id.substring(4);
            this.renderer.appendChild(this.element.nativeElement, this.svg);
            if (this._aria) {
                this.updateAria();
            }
            return;
        }
        let customClass: string = null;
        const mapping = iconsConfig?.filter((i) => i.original === id);
        if (mapping?.length === 1) {
            id = mapping[0].replace || '';
            customClass = mapping[0].cssClass;
        }
        this._id = id;
        if (this._aria) {
            this.updateAria();
        }
        let cssClass: string;
        if (id?.startsWith('edu-') && !customClass) {
            cssClass = 'edu-icons';
            id = id.substring(4);
        } else if (id?.startsWith('custom-') || customClass) {
            cssClass = 'custom-icons';
            id = id.substring(7);
        } else {
            cssClass = 'material-icons';
        }
        this.element.nativeElement.classList.add(cssClass);
        if (customClass) {
            this.element.nativeElement.classList.add(customClass);
        }
        this.element.nativeElement.innerText = id;
    }

    private updateAria() {
        if (this._aria !== undefined) {
            if (this._aria && this._id) {
                this.translate.get('ICON_LABELS.' + this._id).subscribe((lang) => {
                    this.setAltText(lang);
                });
            } else {
                this.setAltText(null);
            }
        }
    }

    private setAltText(altText: string): void {
        if (this.svg) {
            this.svg.alt = altText;
        }
        if (altText && !this.altTextSpan) {
            this.insertAltTextSpan();
        }
        if (this.altTextSpan) {
            this.altTextSpan.innerText = altText;
        }
    }

    private insertAltTextSpan(): void {
        this.altTextSpan = document.createElement('span');
        this.altTextSpan.classList.add('cdk-visually-hidden');
        this.element.nativeElement.insertAdjacentElement('afterend', this.altTextSpan);
    }
}
