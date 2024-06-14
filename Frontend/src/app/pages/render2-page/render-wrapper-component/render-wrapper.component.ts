import { Component, Input, signal } from '@angular/core';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { RenderDataRequest } from 'ngx-rendering-service-api';

@Component({
    selector: 'es-render-wrapper-component',
    templateUrl: 'render-wrapper.component.html',
    styleUrls: ['render-wrapper.component.scss'],
})
export class RenderWrapperComponent {
    @Input() nodeId: string;

    node = signal<Node>(null);
    dummyRequest = signal<RenderDataRequest>(null);
    constructor(private nodeApi: NodeService) {
        this.dummyRequest.set({
            nodeId: 'TEST_lviv.jpg',
            size: -1,
            type: 'file-image',
            hash: '1',
            mimeType: 'image/jpeg',
            version: '1.0',
            repoId: '',
        });
        /*
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
        /*this.dummyRequest.set(undefined);
        this.node.set({
                title: "Test-Node",
                mediatype: "link",
                properties: {
                    "ccm:wwwurl": ["https://www.youtube.com/watch?v=SVOuYquXuuc"]
                }
            } as unknown as Node);*/
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
}
