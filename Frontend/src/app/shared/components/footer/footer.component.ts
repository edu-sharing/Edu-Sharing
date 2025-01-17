/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, Input } from '@angular/core';
import { ConfigurationHelper } from '../../../core-module/core.module';
import { ConfigService } from 'ngx-edu-sharing-api';
import { take } from 'rxjs/operators';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'es-footer',
    templateUrl: 'footer.component.html',
    styleUrls: ['footer.component.scss'],
    imports: [CommonModule],
    standalone: true,
})
export class FooterComponent {
    banner: any;
    _scope: string;
    public show: boolean;
    @Input() set scope(scope: string) {
        this._scope = scope;
        this.config
            .observeConfig()
            .pipe(take(1))
            .subscribe(() => {
                this.banner = ConfigurationHelper.getBanner(this.config);
                this.show = this.banner && this.banner.components.indexOf(this._scope) != -1;
            });
    }
    constructor(private config: ConfigService) {}
}
