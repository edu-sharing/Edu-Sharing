import { Component, Input, signal, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { ActionbarComponent, OptionsHelperDataService, Scope } from 'ngx-edu-sharing-ui';
import { RenderDataRequest } from 'ngx-rendering-service-api';
import { firstValueFrom } from 'rxjs';
import { RestConstants } from '../../../core-module/rest/rest-constants';

@Component({
    selector: 'es-render-wrapper-component',
    templateUrl: 'render-wrapper.component.html',
    styleUrls: ['render-wrapper.component.scss'],
    providers: [OptionsHelperDataService],
})
export class RenderWrapperComponent implements OnChanges {
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    @Input() nodeId: string;
    @Input() version: string;

    node = signal<Node>(null);
    dummyRequest = signal<RenderDataRequest>(null);

    constructor(private nodeApi: NodeService, private optionsHelper: OptionsHelperDataService) {
        this.optionsHelper.registerGlobalKeyboardShortcuts();
        /*this.dummyRequest.set({
            nodeId: 'TEST_lviv.jpg',
            size: -1,
            type: 'file-image',
            hash: '1',
            mimeType: 'image/jpeg',
            version: '1.0',
            repoId: '',
        });
        */
        this.dummyRequest.set({
            nodeId: 'TEST_4k.mp4',
            size: -1,
            type: 'file-video',
            hash: '' + Math.random(),
            mimeType: 'video/mpeg',
            version: '1.0',
            repoId: '',
        });

        this.dummyRequest.set({
            nodeId: 'TEST_portrait.pdf',
            size: -1,
            type: 'file-pdf',
            hash: '' + Math.random(),
            mimeType: 'application/pdf',
            version: '1.0',
            repoId: '',
        });

        /*this.dummyRequest.set({
            nodeId: 'TEST_lorem_ipsum.odt',
            size: -1,
            type: 'file-word',
            hash: '' + Math.random(),
            mimeType: 'application/vnd.oasis.opendocument.text',
            version: '1.0',
            repoId: '',
        });*/
        // url module
        this.dummyRequest.set(undefined);
        /*
        combineLatest([this.route.params, this.route.queryParams]).subscribe(
            ([params, queryParams]) => {
                this.nodeApi
                    .getNode(params.node, {
                        repository: queryParams.repo || HOME_REPOSITORY,
                    })
                    .subscribe((n) => {
                        console.log(n);
                        this.node.set(n);
                        this.dummyRequest.set({
                            nodeId: 'TEST_lviv.jpg',
                            size: -1,
                            type: 'file-image',
                            hash: '1',
                            mimeType: 'image/jpeg',
                            version: '1.0',
                            repoId: '',
                        });

                    });
            },
        );
        */
    }

    async ngOnChanges(changes: SimpleChanges) {
        if (changes.nodeId) {
            this.node.set(await firstValueFrom(this.nodeApi.getNode(changes.nodeId.currentValue)));
            this.optionsHelper.setData({
                scope: Scope.Render,
                activeObjects: [this.node()],
                parent: {
                    ref: {
                        id: this.node().parent.id,
                    },
                },
                customOptions: {
                    useDefaultOptions: true,
                },
                postPrepareOptions: (options, objects) => {
                    if (this.version && this.version !== RestConstants.NODE_VERSION_CURRENT) {
                        options.filter((o) => o.name === 'OPTIONS.OPEN')[0].isEnabled = false;
                    }
                },
            });
            await this.optionsHelper.initComponents(this.actionbar);
            this.optionsHelper.refreshComponents();
        }
    }

    goBack() {
        window.history.back();
    }
}
