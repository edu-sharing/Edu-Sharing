import {
    Component,
    Injector,
    Input,
    OnChanges,
    signal,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { AboutService, Node, NodeService, NodeServiceUnwrapped } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    OptionsHelperDataService,
    Scope,
    TranslationsService,
} from 'ngx-edu-sharing-ui';
import { RenderDataRequestWithToken, RSApiConfiguration } from 'ngx-rendering-service-api';
import { firstValueFrom } from 'rxjs';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { environment } from '../../../../environments/environment';

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
    request = signal<RenderDataRequestWithToken>(null);

    constructor(
        private nodeApi: NodeService,
        private nodeApiUnwrapped: NodeServiceUnwrapped,
        private aboutService: AboutService,
        private translations: TranslationsService,
        private injector: Injector,
        private optionsHelper: OptionsHelperDataService,
    ) {
        this.translations.waitForInit().subscribe(() => {});
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

        this.request.set({
            nodeId: 'TEST_4k.mp4',
            size: -1,
            type: 'file-video',
            hash: '' + Math.random(),
            mimeType: 'video/mpeg',
            version: '1.0',
            repoId: '',
        });

        this.request.set({
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
        this.request.set(undefined);
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
            const about = await firstValueFrom(this.aboutService.getAbout());
            if (!about.renderingService2) {
                console.error('no rendering service 2 url was configured. Will not continue.');
                return;
            }
            console.log(about.renderingService2?.url);
            if (environment.production) {
                this.injector.get(RSApiConfiguration).rootUrl = about.renderingService2.url.replace(
                    /\/$/g,
                    '',
                );
            } else {
                this.injector.get(RSApiConfiguration).rootUrl = '/rendering2';
            }
            console.log(this.injector.get(RSApiConfiguration));
            this.node.set(await firstValueFrom(this.nodeApi.getNode(changes.nodeId.currentValue)));
            const token = await (
                (await firstValueFrom(
                    this.nodeApiUnwrapped.getJwt({
                        repository: this.node().ref.repo,
                        node: this.node().ref.id,
                    }),
                )) as unknown as Blob
            ).text();
            //const token = 'tst';
            console.log(token, this.node());
            const resourceType =
                this.node().properties[RestConstants.CCM_PROP_CCRESSOURCETYPE] === undefined
                    ? ''
                    : this.node().properties[RestConstants.CCM_PROP_CCRESSOURCETYPE][0] ?? '';
            this.request.set({
                nodeId: this.node().ref.id,
                size: parseInt(this.node().size),
                hash: this.node().content.hash,
                mimeType: this.node().mimetype,
                type: this.node().mediatype,
                repoId: this.node().ref.repo,
                version: this.node().content.version,
                resourceType: resourceType,
                // the replication source flag can be set in order to trigger special treatments
                // in the backend. For example, it can be used for sodix paid media in order to
                // fetch two instead of one url. This logic has to be implemented
                replicationSourceFlag: false,
                token: token,
            });
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
