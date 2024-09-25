import { PipeTransform, Pipe } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Node } from 'ngx-edu-sharing-api';
import { RestHelper } from '../util/rest-helper';
import { NodeRoot } from '../node-entries/entries-model';

@Pipe({ name: 'nodeTitle' })
export class NodeTitlePipe implements PipeTransform {
    transform(node: Node | NodeRoot | 'HOME', args: string[] = null): string {
        if (!(node as Node).name) {
            if (node === 'HOME') {
                return this.translate.instant('WORKSPACE.' + node);
            }
            return this.translate.instant('WORKSPACE.' + node);
        }
        return RestHelper.getTitle(node as Node);
    }
    constructor(private translate: TranslateService) {}
}
