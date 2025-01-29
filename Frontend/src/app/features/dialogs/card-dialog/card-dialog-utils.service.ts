import { Injectable } from '@angular/core';
import { CardDialogConfig } from './card-dialog-config';
import { Node, RestHelper } from '../../../core-module/core.module';
import { RepoUrlService } from 'ngx-edu-sharing-ui';
import { TranslateService } from '@ngx-translate/core';
import { AuthorityNamePipe } from '../../../shared/pipes/authority-name.pipe';
import { Group, User } from 'ngx-edu-sharing-api';

@Injectable({
    providedIn: 'root',
})
export class CardDialogUtilsService {
    constructor(private repoUrlService: RepoUrlService, private translate: TranslateService) {}

    async configForNode(node: Node | User | Group): Promise<Partial<CardDialogConfig<unknown>>> {
        if ((node as User).profile) {
            return {
                avatar: {
                    kind: 'icon',
                    icon: (node as User).authorityType === 'GROUP' ? 'group' : 'person',
                },
                subtitle: new AuthorityNamePipe(this.translate).transform(node),
            };
        }
        return {
            avatar: {
                kind: 'image',
                url: await this.repoUrlService.getRepoUrl((node as Node).iconURL, node as Node),
            },
            subtitle: RestHelper.getTitle(node as Node),
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
