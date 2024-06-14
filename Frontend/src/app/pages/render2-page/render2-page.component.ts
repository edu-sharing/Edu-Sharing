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
    nodeId = signal<string>(null);
    constructor(private route: ActivatedRoute, private nodeApi: NodeService) {
        combineLatest([this.route.params, this.route.queryParams]).subscribe(
            ([params, queryParams]) => {
                this.nodeId.set(params.node);
            },
        );
    }
}
