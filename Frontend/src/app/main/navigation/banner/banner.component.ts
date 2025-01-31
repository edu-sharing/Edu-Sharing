/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ConfigurationHelper } from '../../../core-module/core.module';
import { ConfigService } from 'ngx-edu-sharing-api';
import { take } from 'rxjs/operators';

@Component({
    selector: 'es-banner',
    templateUrl: 'banner.component.html',
    styleUrls: ['banner.component.scss'],
})
export class BannerComponent {
    @Input() scope: string;
    @Output() update = new EventEmitter();
    public banner: any;
    constructor(private config: ConfigService) {
        this.banner = ConfigurationHelper.getBanner(this.config);
        this.config
            .observeConfig()
            .pipe(take(1))
            .subscribe(() => {
                this.banner = ConfigurationHelper.getBanner(this.config);
                this.update.emit(this.banner);
            });
    }

    clickBanner() {
        if (this.banner.href) {
            window.location.href = this.banner.href;
        }
    }
}
