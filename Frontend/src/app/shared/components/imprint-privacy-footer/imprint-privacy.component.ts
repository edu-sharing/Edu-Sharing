import { Component, Input } from '@angular/core';
import { ImprintPrivacyService } from './imprint-privacy-service';

@Component({
    selector: 'es-imprint-privacy',
    templateUrl: './imprint-privacy.component.html',
    styleUrls: ['./imprint-privacy.component.scss'],
})
export class ImprintPrivacyComponent {
    @Input() separator: string = '';
    constructor(public service: ImprintPrivacyService) {}
}
