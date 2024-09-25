import { Injectable } from '@angular/core';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { ConfigService } from 'ngx-edu-sharing-api';
import { OPEN_URL_MODE } from 'ngx-edu-sharing-ui';
import { BridgeService } from 'src/app/services/bridge.service';

@Injectable({
    providedIn: 'root',
})
export class ImprintPrivacyService {
    imprintUrl: string;
    privacyInformationUrl: string;
    constructor(private configService: ConfigService, private bridge: BridgeService) {
        this.configService.observeConfig().subscribe((data: any) => {
            this.imprintUrl = data.imprintUrl;
            this.privacyInformationUrl = data.privacyInformationUrl;
        });
    }

    openImprint() {
        UIHelper.openUrl(this.imprintUrl, this.bridge, OPEN_URL_MODE.BlankSystemBrowser);
    }

    openPrivacy() {
        UIHelper.openUrl(this.privacyInformationUrl, this.bridge, OPEN_URL_MODE.BlankSystemBrowser);
    }
}
