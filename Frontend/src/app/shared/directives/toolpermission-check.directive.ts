import { Directive, ElementRef, Input, OnChanges } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RestConnectorService } from '../../core-module/core.module';

/**
 * Directive to automatically disable buttons if a required toolpermission is missing for the action
 */
@Directive({ selector: '[esToolpermissionCheck]' })
export class ToolpermissionCheckDirective implements OnChanges {
    @Input() toolpermission: string;
    @Input() toolpermissionDisplayHint = true;
    constructor(
        private element: ElementRef,
        private translate: TranslateService,
        private connector: RestConnectorService,
    ) {
        this.element = element;
    }

    async ngOnChanges() {
        await this.handlePermission();
    }

    async handlePermission() {
        const hasPermission = await this.connector
            .hasToolPermission(this.toolpermission)
            .toPromise();
        if (hasPermission) {
        } else {
            this.element.nativeElement.disabled = true;
            if (this.toolpermissionDisplayHint) {
                const div = document.createElement('div');
                div.className = 'toolpermission-missing';
                div.innerHTML = this.translate.instant('TOOLPERMISSION_ERROR_INLINE', {
                    permission: this.translate.instant('TOOLPERMISSION.' + this.toolpermission),
                });
                // div.appendChild(this.element.nativeElement.cloneNode(true));
                this.element.nativeElement.parentElement.replaceChild(
                    div,
                    this.element.nativeElement,
                );
                // this.renderer.appendChild(this.element.nativeElement, div);
            }
        }
    }
}
