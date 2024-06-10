import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnInit,
    Optional,
    ViewChild,
} from '@angular/core';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatRadioButton } from '@angular/material/radio';
import { MatSlideToggle } from '@angular/material/slide-toggle';

/**
 * Multi-line labels and descriptions for `MatCheckbox` and `MatRadioButton`.
 *
 * Aria-attributes will be assigned to the checkbox / radio-button elements.
 *
 * Usage: Place inside either a  `mat-checkbox` or a `mat-radio-button` and add the following
 * elements as content of this element:
 * - any element with `slot="label"`
 * - any element with `slot="description"` (optional)
 *
 * @example
 * <mat-radio-button value="my-radio-button">
 *   <es-multi-line-label>
 *     <ng-container slot="label">
 *       A radio button
 *     </ng-container>
 *     <ng-container slot="description">
 *       Description for the radio button
 *     </ng-container>
 *   </es-multi-line-label>
 * </mat-radio-button>
 */
@Component({
    selector: 'es-multi-line-label',
    templateUrl: './multi-line-label.component.html',
    styleUrls: ['./multi-line-label.component.scss'],
})
export class MultiLineLabelComponent implements OnInit, AfterViewInit {
    @ViewChild('label') label: ElementRef;
    @ViewChild('description') description: ElementRef<HTMLElement>;
    @ViewChild('additionalInformation') additionalInformation: ElementRef;

    parent: MatCheckbox | MatRadioButton | MatSlideToggle;
    input: HTMLInputElement;
    hasDescription = true;
    hasAdditionalInformation = true;

    constructor(
        private changeDetector: ChangeDetectorRef,
        @Optional() matCheckbox: MatCheckbox,
        @Optional() matRadioButton: MatRadioButton,
        @Optional() matSlideToggle: MatSlideToggle,
    ) {
        this.parent = matCheckbox ?? matRadioButton ?? matSlideToggle;
        if (!this.parent) {
            console.error(
                'Multi-line-label component missing input element.',
                'Please provide either a mat-checkbox or a mat-radio-button.',
            );
        }
    }

    ngOnInit(): void {
        const ref =
            (this.parent as MatCheckbox)._elementRef ||
            (this.parent as MatRadioButton)._inputElement ||
            (this.parent as MatSlideToggle)._switchElement;
        this.input = ref.nativeElement.querySelector('input');
        ref.nativeElement.classList.add('has-multi-line-label');
    }

    ngAfterViewInit() {
        this.hasDescription = this.description.nativeElement.childNodes.length !== 0;
        this.label.nativeElement.id = `${this.parent.id}-label`;
        this.input.setAttribute('aria-labelledby', this.label.nativeElement.id);
        if (this.description) {
            this.description.nativeElement.id = `${this.parent.id}-description`;
            this.input.setAttribute('aria-describedby', this.description.nativeElement.id);
        }
        this.changeDetector.detectChanges();
    }
}
