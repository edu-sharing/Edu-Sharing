import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { LtiPlatformV13Service, ManualRegistrationData } from 'ngx-edu-sharing-api';
import { Toast } from '../../../services/toast';

@Component({
    selector: 'es-ltitool-admin',
    templateUrl: './ltitool-admin.component.html',
    styleUrls: ['./ltitool-admin.component.scss'],
})
export class LtitoolAdminComponent implements OnInit {
    @Output() refreshAppList = new EventEmitter<void>();

    showIframe: boolean = false;
    ltiToolDynRegUrl: string = '';
    ltiToolDynRegUrlSafe: SafeResourceUrl;

    //manual registration
    manualRegistrationData: ManualRegistrationData = { clientName: '', targetLinkUri: '' };

    static staticRef: LtitoolAdminComponent;

    constructor(
        public sanitizer: DomSanitizer,
        private toast: Toast,
        private ltiPlatformService: LtiPlatformV13Service,
    ) {
        LtitoolAdminComponent.staticRef = this;
    }

    ngOnInit(): void {
        let eventListener = function (event: any) {
            if (event.data.subject == 'org.imsglobal.lti.close') {
                LtitoolAdminComponent.staticRef.showIframe = false;
                LtitoolAdminComponent.staticRef.ltiToolDynRegUrlSafe = null;
                LtitoolAdminComponent.staticRef.refreshAppList.emit();
            }
        };
        window.addEventListener('message', eventListener);
    }

    register() {
        console.log('iframeSrc:' + this.ltiToolDynRegUrl);

        try {
            new URL(this.ltiToolDynRegUrl);
        } catch {
            this.toast.toast('Invalid Url');
            return;
        }

        let url =
            '/edu-sharing/rest/ltiplatform/v13/start-dynamic-registration?url=' +
            this.ltiToolDynRegUrl;
        this.ltiToolDynRegUrlSafe = this.sanitizer.bypassSecurityTrustResourceUrl(url);
        this.showIframe = true;
    }

    manualRegistration() {
        this.ltiPlatformService
            .manualRegistration({ body: this.manualRegistrationData })
            .subscribe((s) => {
                this.refreshAppList.emit();
            });
    }

    changeRedirectionUrl(event: any) {
        this.manualRegistrationData.redirectionUrls = Array.of(event.target.value);
    }
}
