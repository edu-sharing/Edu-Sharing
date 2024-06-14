import { trigger } from '@angular/animations';
import { Component, OnDestroy, OnInit, signal } from '@angular/core';
import { OptionsHelperDataService, UIAnimation } from 'ngx-edu-sharing-ui';
import { EventListener } from '../../core-module/core.module';
import { ActivatedRoute } from '@angular/router';
import { combineLatest, forkJoin } from 'rxjs';
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
        combineLatest([this.route.params, this.route.queryParams]).subscribe(
            ([params, queryParams]) => {
                this.nodeApi
                    .getNode(params.node, {
                        repository: queryParams.repo || HOME_REPOSITORY,
                    })
                    .subscribe((n) => {
                        console.log(n);
                        this.node.set(n);
                        /*this.dummyRequest.set({
                        nodeId: n.ref.id,
                        hash: Math.random() + '',
                        size: parseInt(n.size),
                        repoId: n.ref.repo,
                        type: 'image',
                        version: n.content.version,
                        mimeType: n.mimetype,
                    });*/
                        this.dummyRequest.set({
                            nodeId: 'TEST_lviv.jpg',
                            size: -1,
                            type: 'file-image',
                            hash: '1',
                            mimeType: 'image/jpeg',
                            version: '1.0',
                            repoId: '',
                        });
                        this.dummyRequest.set({
                            nodeId: 'TEST_4k.mp4',
                            size: -1,
                            type: 'file-video',
                            hash: '' + Math.random(),
                            mimeType: 'video/mpeg',
                            version: '1.0',
                            repoId: '',
                        });
                    });
            },
        );
    }
}
