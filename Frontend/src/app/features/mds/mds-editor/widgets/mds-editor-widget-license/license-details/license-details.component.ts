import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { RestConnectorService, RestConstants } from '../../../../../../core-module/core.module';
import { NodeHelperService } from '../../../../../../services/node-helper.service';
import { Values } from '../../../../types/types';
import { Helper } from '../../../../../../core-module/rest/helper';

@Component({
    selector: 'es-license-details',
    templateUrl: 'license-details.component.html',
    styleUrls: ['license-details.component.scss'],
})
export class LicenseDetailsComponent implements OnChanges {
    static PROPERTIES_MAPPING_GRAPHQL: { [key: string]: string } = {
        [RestConstants.CCM_PROP_LICENSE]: 'lom.rights.internal',
        [RestConstants.LOM_PROP_RIGHTS_DESCRIPTION]: 'lom.rights.description',
    };

    @Input() nodes: Node[];
    @Input() properties: Values;
    type: string;
    ccShare: string;
    ccCommercial: string;
    ccVersion: string;
    ccCountry: string;
    eduType: string;
    copyrightType: string;
    eduDownload: boolean;
    rightsDescription: string;

    constructor(
        private connector: RestConnectorService,
        private translate: TranslateService,
        private nodeHelper: NodeHelperService,
    ) {}
    ngOnChanges(changes: SimpleChanges) {
        // Set the `draggable` attribute when this directive is active.
        if (changes.nodes?.currentValue) {
            this.readLicense();
        }
    }
    private readLicense() {
        let license = this.getValueForAll(RestConstants.CCM_PROP_LICENSE, 'MULTI', 'NONE');
        if (!license) license = 'NONE';
        this.type = license;
        if (license.startsWith('CC_BY')) {
            this.type = 'CC_BY';
            if (license.indexOf('SA') !== -1) this.ccShare = 'SA';
            if (license.indexOf('ND') !== -1) this.ccShare = 'ND';
            if (license.indexOf('NC') !== -1) this.ccCommercial = 'NC';

            this.ccVersion = this.getValueForAll(
                RestConstants.CCM_PROP_LICENSE_CC_VERSION,
                this.ccVersion,
            );
            this.ccCountry = this.getValueForAll(RestConstants.CCM_PROP_LICENSE_CC_LOCALE);
        }
        if (license === 'CC_0') {
            this.type = 'CC_0';
        }
        if (license === 'PDM') {
            this.type = 'PDM';
        }
        if (license.startsWith('COPYRIGHT')) {
            this.type = 'COPYRIGHT';
            this.copyrightType = license;
        }
        if (license === 'SCHULFUNK') {
            this.type = license;
        }
        if (license.startsWith('EDU')) {
            this.type = 'EDU';
            if (license.indexOf('P_NR') !== -1) this.eduType = 'P_NR';
            if (license.indexOf('NC') !== -1) this.eduType = 'NC';
            this.eduDownload = license.indexOf('ND') === -1;
        }
        if (license === 'CUSTOM') this.type = license;

        this.rightsDescription = this.getValueForAll(RestConstants.LOM_PROP_RIGHTS_DESCRIPTION);
    }

    private getValueForAll(
        prop: string,
        fallbackNotIdentical: any = '',
        fallbackIsEmpty = fallbackNotIdentical,
    ) {
        if (this.properties) {
            return this.properties[prop] ? this.properties[prop][0] : fallbackIsEmpty;
        } else if (this.nodes) {
            return this.nodeHelper.getValueForAll(
                this.nodes,
                prop,
                fallbackNotIdentical,
                fallbackIsEmpty,
                false,
            );
        } else {
            // console.warn('license has no data to display');
            return fallbackIsEmpty;
        }
    }
    getLicenseIcon() {
        return this.nodeHelper.getLicenseIconByString(this.getLicenseProperty());
    }
    isOerLicense() {
        return (
            this.getLicenseProperty() === 'CC_0' ||
            this.getLicenseProperty() === 'PDM' ||
            this.getLicenseProperty() === 'CC_BY' ||
            this.getLicenseProperty() === 'CC_BY_SA'
        );
    }

    getLicenseProperty() {
        return this.getValueForAll(RestConstants.CCM_PROP_LICENSE, 'MULTI', 'NONE');
    }
    getLicenseName() {
        return this.nodeHelper.getLicenseNameByString(this.getLicenseProperty());
    }
    getLicenseUrl() {
        return this.nodeHelper.getLicenseUrlByString(
            this.getLicenseProperty(),
            this.ccVersion,
            this.ccCountry,
        );
    }
}
