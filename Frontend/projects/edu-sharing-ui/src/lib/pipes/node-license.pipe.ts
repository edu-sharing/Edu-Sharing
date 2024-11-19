import { Pipe, PipeTransform } from '@angular/core';
import { Node, RestConstants } from 'ngx-edu-sharing-api';
import { TranslateService } from '@ngx-translate/core';

export type LicenseType = 'name';

@Pipe({ name: 'esNodeLicense' })
export class NodeLicensePipe implements PipeTransform {
    constructor(private translate: TranslateService) {}
    transform(node: Node, args: { type: LicenseType }) {
        if (node.properties[RestConstants.CCM_PROP_LICENSE]?.[0]) {
            if (args?.type === 'name') {
                return this.translate.get(
                    'LICENSE.NAMES.' + node.properties[RestConstants.CCM_PROP_LICENSE]?.[0],
                );
            }
        }
        return null;
    }
}
