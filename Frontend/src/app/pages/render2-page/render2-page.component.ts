import { trigger } from '@angular/animations';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { OptionsHelperDataService, UIAnimation } from 'ngx-edu-sharing-ui';
import { EventListener } from '../../core-module/core.module';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';
import { HOME_REPOSITORY, NodeService, Node } from 'ngx-edu-sharing-api';
import { RenderDataRequest } from 'ngx-rendering-service-api';

@Component({
    selector: 'es-render2-page',
    templateUrl: 'render2-page.component.html',
    styleUrls: ['render2-page.component.scss'],
    providers: [OptionsHelperDataService],
})
export class Render2PageComponent {
    node = signal<Node>(null);
    dummyRequest = signal<RenderDataRequest>(null);
    constructor(private route: ActivatedRoute, private nodeApi: NodeService) {
        forkJoin([this.route.params, this.route.queryParams]).subscribe(([params, queryParams]) => {
            this.nodeApi
                .getNode(params.nodeId, {
                    repository: queryParams.repo || HOME_REPOSITORY,
                })
                .subscribe((n) => {
                    this.node.set(n);
                    this.dummyRequest.set({
                        nodeId: n.ref.id,
                        hash: Math.random() + '',
                        size: parseInt(n.size),
                        repoId: n.ref.repo,
                        type: 'image',
                        version: n.content.version,
                        mimeType: n.mimetype,
                    });
                });
        });
    }
}
