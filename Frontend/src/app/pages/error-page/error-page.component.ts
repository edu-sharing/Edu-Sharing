import { Component } from '@angular/core';
import { ActivatedRoute, Data, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { TranslationsService, UIConstants } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-error-page',
    templateUrl: 'error-page.component.html',
    styleUrls: ['error-page.component.scss'],
})
export class ErrorPageComponent {
    public message: string;
    public messageDetail: string;
    public messageText: string;
    public error: string;
    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private translate: TranslateService,
        private translations: TranslationsService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            this.route.params.subscribe(async (data: Params) => {
                this.setMessage(data);
                this.route.data.subscribe((routeData) => {
                    if (routeData && Object.keys(routeData).length) {
                        this.setMessage(routeData);
                    }
                });
            });
        });
    }

    private setMessage(data: Params | Data) {
        this.message = 'MESSAGES.' + data.message;
        this.messageDetail = 'MESSAGES.DETAILS.' + data.message;
        this.messageText = data.text;
        this.error = data.message;

        if (this.translate.instant(this.message) === this.message) {
            this.message = 'MESSAGES.INVALID';
            this.messageDetail = 'MESSAGES.DETAILS.INVALID';
        }

        // custom message without translation
        if (data.message === 'OTHER') {
            this.messageDetail = data.text;
        }
    }

    public openSearch() {
        void this.router.navigate([UIConstants.ROUTER_PREFIX + 'search']);
    }
    public closeWindow() {
        window.close();
    }
}
