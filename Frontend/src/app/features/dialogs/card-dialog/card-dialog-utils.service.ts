import { Injectable } from '@angular/core';
import { CardDialogConfig } from './card-dialog-config';
import { RestHelper } from '../../../core-module/core.module';
import { Node } from 'ngx-edu-sharing-api';
import { RepoUrlService } from 'ngx-edu-sharing-ui';
import { TranslateService } from '@ngx-translate/core';

@Injectable({
    providedIn: 'root',
})
export class CardDialogUtilsService {
    constructor(private repoUrlService: RepoUrlService, private translate: TranslateService) {}

    async configForNode(node: Node): Promise<Partial<CardDialogConfig<unknown>>> {
        return {
            avatar: {
                kind: 'image',
                url: await this.repoUrlService.getRepoUrl(node.iconURL, node),
            },
            subtitle: RestHelper.getTitle(node),
        };
    }

    async configForNodes(nodes: Node[]): Promise<Partial<CardDialogConfig>> {
        if (nodes.length === 1) {
            return this.configForNode(nodes[0]);
        }
        const subtitle = await this.translate
            .get('CARD_SUBTITLE_MULTIPLE', { count: nodes.length })
            .toPromise();
        return {
            avatar: null,
            subtitle,
        };
    }
}
