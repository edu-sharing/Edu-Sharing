import { Component, HostBinding, OnInit } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';

@Component({
    selector: 'es-spinner',
    templateUrl: 'spinner.component.html',
    styleUrls: ['spinner.component.scss'],
    standalone: true,
    imports: [TranslateModule],
})
export class SpinnerComponent {
    @HostBinding('attr.data-test') readonly dataTest = 'loading-spinner';

    constructor() {}
}
