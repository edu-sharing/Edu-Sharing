import { Component, OnInit } from '@angular/core';
import { CordovaService } from '../services/cordova.service';
import { UIConstants } from 'ngx-edu-sharing-ui';
import { Router } from '@angular/router';
import { ConfigurationService } from '../core-module/rest/services/configuration.service';
import { take } from 'rxjs/operators';
import { RouterHelper } from '../util/router.helper';
import { PlatformLocation } from '@angular/common';
import { environment } from '../../environments/environment';

@Component({
    selector: 'es-startup',
    template: '',
})
export class StartupComponent implements OnInit {
    constructor(
        private cordova: CordovaService,
        private router: Router,
        private platformLocation: PlatformLocation,
        private config: ConfigurationService,
    ) {}

    async ngOnInit() {
        if (this.cordova.isRunningCordova()) {
            // wait until cordova device init is ready
            this.cordova.subscribeServiceReady().subscribe(() => {
                // per default go to app
                this.router.navigate([UIConstants.ROUTER_PREFIX, 'app'], { replaceUrl: true });
            });
        } else {
            if (environment.production) {
                // is fully handled by the backend AuthenticationFilter.java
            } else {
                let location = await this.config
                    .get('defaultLocation', 'workspace')
                    .pipe(take(1))
                    .toPromise();
                if (!location.match(/https?:\/\/*/)) {
                    location = UIConstants.ROUTER_PREFIX + location;
                }
                RouterHelper.navigateToAbsoluteUrl(
                    this.platformLocation,
                    this.router,
                    location,
                    true,
                );
            }
        }
    }
}
