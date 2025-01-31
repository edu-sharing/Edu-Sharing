import { Component, ElementRef, EventEmitter, Input, Output, ViewChild } from '@angular/core';
import { trigger } from '@angular/animations';
import { DialogButton } from '../../../core-module/core.module';
import { UIAnimation } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-infobar',
    templateUrl: 'infobar.component.html',
    styleUrls: ['infobar.component.scss'],
    animations: [
        trigger('infobarBottom', UIAnimation.infobarBottom(UIAnimation.ANIMATION_TIME_SLOW)),
    ],
})
/**
 * A Infobar (usually at the bottom) which features action buttons
 * As soon as the title is set, the infobar will be added to the dom and animated
 * Don't add it with *ngIf, otherwise it can't animate properly
 * Just add it to the component without constraints and set or remove the title value
 */
export class InfobarComponent {
    /**
     * Wether or not this bar can be closed using an "x" icon
     * @type {boolean}
     */
    @Input() isCancelable = false;
    /**
     * Position, currently only 'bottom' is supported and default value
     */
    @Input() position = 'bottom';
    /**
     * The title, will be translated automatically
     */
    @Input() title: string;
    /**
     * The message, will be translated automatically
     */
    @Input() message: string;
    /**
     * Additional dynamic content for your language string, use an object, e.g.
     * Language String: Hello {{ name }}
     * And use messageParameters={name:'World'}
     */
    @Input() messageParameters: any;
    /**
     * Will be emitted when the users cancels the dialog
     */
    @Output() cancelInfobar = new EventEmitter<void>();
    /** A list of buttons, see @DialogButton
     * Also use the DialogButton.getYesNo() and others if applicable!
     */
    public _buttons: DialogButton[];
    @Input() set buttons(buttons: DialogButton[]) {
        if (!buttons) {
            this._buttons = null;
            return;
        }
        this._buttons = buttons.reverse();
        setTimeout(() => {
            if (this.buttonElements) this.buttonElements.nativeElement.focus();
        }, 10);
    }

    @ViewChild('buttonElements') buttonElements: ElementRef;

    public click(btn: DialogButton) {
        btn.callback();
    }
    public cancel() {
        this.cancelInfobar.emit();
    }
}
