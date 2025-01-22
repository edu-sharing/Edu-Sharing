import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { Node } from '../../../../../core-module/rest/data-object';
import { VCard } from 'ngx-edu-sharing-ui';

/**
 * Format the version label and checking constants if required
 */
@Pipe({ name: 'nodeAuthorName' })
export class NodeAuthorNamePipe implements PipeTransform {
    transform(node: Node, args: any = null): string {
        const data = [];
        if (node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0]) {
            data.push(node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]?.[0]);
        }
        if (node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR]?.[0]) {
            data.push(
                node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR].map(
                    (a: string) => new VCard(a).getDisplayName(),
                ),
            );
        }
        if (data.length === 0) {
            return this.translate.instant('MDS.AUTHOR_UNSET');
        }
        return data.join(', ');
    }
    constructor(private translate: TranslateService) {}
}
