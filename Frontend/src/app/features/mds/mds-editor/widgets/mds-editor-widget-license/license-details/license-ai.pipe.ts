import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Values } from 'ngx-edu-sharing-ui';
import { RestConstants } from 'src/app/core-module/core.module';
import { MdsWidget } from 'ngx-edu-sharing-api';
import { MdsEditorInstanceService } from '../../../mds-editor-instance.service';

@Pipe({
    name: 'esLicenseAiInfo',
})
export class LicenseAiPipe implements PipeTransform {
    constructor(
        private translate: TranslateService,
        private mdsEditorInstanceService: MdsEditorInstanceService,
    ) {}
    transform(properties: Values) {
        let value = [];
        if (properties[RestConstants.CCM_PROP_LICENSE_AI_ALLOW_USAGE]?.[0] === 'false') {
            value.push(this.translate.instant('LICENSE.AI.NO_USAGE_ALLOWED'));
        }
        if (properties[RestConstants.CCM_PROP_LICENSE_AI_GENERATED]?.[0] === 'true') {
            let info = this.translate.instant('LICENSE.AI.GENERATED');
            if (properties[RestConstants.CCM_PROP_LICENSE_AI_TOOL]?.[0]) {
                info += this.translate.instant('LICENSE.AI.TOOL', {
                    tool: this.mapTool(properties[RestConstants.CCM_PROP_LICENSE_AI_TOOL]?.[0]),
                });
            }
            if (properties[RestConstants.CCM_PROP_LICENSE_AI_MANUALLY_MODIFIED]?.[0] === 'true') {
                info += ' ' + this.translate.instant('LICENSE.AI.MODIFIED');
            }
            value.push(info + '.');
        }
        return value.join('\n');
    }
    mapTool(tool: string) {
        return (
            this.mdsEditorInstanceService.mdsDefinition$.value.widgets
                .find((w: MdsWidget) => w.id === RestConstants.CCM_PROP_LICENSE_AI_TOOL)
                ?.values?.find((v) => v.id === tool)?.caption || tool
        );
    }
}
