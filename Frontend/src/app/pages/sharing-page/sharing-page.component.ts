import { Component, ViewChild } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import {
    CustomOptions,
    DefaultGroups,
    ElementType,
    InteractionType,
    ListItem,
    ListSortConfig,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeEntriesWrapperComponent,
    OPEN_URL_MODE,
    OptionItem,
    Scope,
    TemporaryStorageService,
    TranslationsService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import {
    ConfigurationHelper,
    Node,
    Person,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestNodeService,
    RestSharingService,
    SharingInfo,
} from '../../core-module/core.module';
import { Toast } from '../../services/toast';
import { Helper } from '../../core-module/rest/helper';
import { UIHelper } from '../../core-ui-module/ui-helper';
import { BridgeService } from '../../services/bridge.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { ConfigService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-sharing-page',
    templateUrl: 'sharing-page.component.html',
    styleUrls: ['sharing-page.component.scss'],
})
export class SharingPageComponent {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    @ViewChild('nodeEntries') nodeEntries: NodeEntriesWrapperComponent<Node>;
    loading = true;
    passwordInput: string;
    private params: Params;
    sharingInfo: SharingInfo;
    nodesDataSource = new NodeDataSource<Node>();
    columns: ListItem[] = [];
    sort: ListSortConfig = {
        allowed: true,
        columns: RestConstants.POSSIBLE_SORT_BY_FIELDS,
        active: RestConstants.CM_NAME,
        direction: 'asc',
    };
    options: CustomOptions = {
        useDefaultOptions: false,
        addOptions: [],
    };
    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private connector: RestConnectorService,
        private nodeService: RestNodeService,
        private sharingService: RestSharingService,
        private bridge: BridgeService,
        private nodeHelperService: NodeHelperService,
        private storage: TemporaryStorageService,
        private toast: Toast,
        private config: ConfigService,
        private translations: TranslationsService,
    ) {
        this.columns.push(new ListItem('NODE', RestConstants.CM_NAME));
        this.columns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
        this.columns.push(new ListItem('NODE', RestConstants.SIZE));
        const download = new OptionItem('SHARING.DOWNLOAD', 'cloud_download', (node: Node) =>
            this.download(node),
        );
        download.elementType = [ElementType.Node];
        download.group = DefaultGroups.Primary;
        download.showAsAction = true;
        const open = new OptionItem('SHARING.OPEN', 'open_in_new', (node: Node) => {
            console.log(node);
            UIHelper.openUrl(
                node.properties[RestConstants.CCM_PROP_IO_WWWURL][0],
                this.bridge,
                OPEN_URL_MODE.BlankSystemBrowser,
            );
        });
        open.group = DefaultGroups.Primary;
        open.showAsAction = true;
        download.customShowCallback = async (nodes: Node[]) => nodes?.[0]?.mediatype !== 'link';
        open.customShowCallback = async (nodes: Node[]) => nodes?.[0]?.mediatype === 'link';
        this.options.addOptions.push(download);
        this.options.addOptions.push(open);
        this.translations.waitForInit().subscribe(() => {
            this.route.queryParams.subscribe((params) => {
                this.params = params;
                this.sharingService.getInfo(params.nodeId, params.token).subscribe(
                    (result) => {
                        this.loading = false;
                        this.sharingInfo = result;
                        if (result.expired) {
                            this.router.navigate([
                                UIConstants.ROUTER_PREFIX,
                                'messages',
                                'share_expired',
                            ]);
                            return;
                        }
                        this.loadChildren();
                    },
                    (error) => {
                        console.warn(error);
                        this.router.navigate([
                            UIConstants.ROUTER_PREFIX,
                            'messages',
                            'share_expired',
                        ]);
                        this.loading = false;
                    },
                );
            });
        });
    }

    validatePassword() {
        this.sharingService
            .getInfo(this.params.nodeId, this.params.token, this.passwordInput)
            .subscribe((result) => {
                if (!result.passwordMatches) {
                    this.toast.error(null, 'SHARING.ERROR_INVALID_PASSWORD');
                }
                this.sharingInfo = result;
                this.loadChildren();
            });
    }
    download(child: Node = null) {
        const node = this.params.nodeId;
        const token = this.params.token;
        let url =
            this.connector.getAbsoluteEndpointUrl() +
            '../share?mode=download&token=' +
            encodeURIComponent(token) +
            '&password=' +
            encodeURIComponent(this.passwordInput) +
            '&nodeId=' +
            encodeURIComponent(node);
        if (child == null && this.sharingInfo.node.isDirectory) {
            const ids = RestHelper.getNodeIds(this.nodesDataSource.getData()).join(',');
            url += '&childIds=' + encodeURIComponent(ids);
        } else {
            if (child != null) {
                url += '&childIds=' + encodeURIComponent(child.ref.id);
            }
        }
        window.open(url);
    }
    changeSort(sort: ListSortConfig) {
        this.sort = sort;
        this.loadChildren();
    }
    private loadChildren() {
        if (this.sharingInfo.password && !this.sharingInfo.passwordMatches) return;
        if (!this.sharingInfo.node.isDirectory) {
            return;
        }
        this.nodesDataSource.reset();
        this.nodesDataSource.isLoading = true;
        const request = {
            count: RestConstants.COUNT_UNLIMITED,
            sortBy: [this.sort.active],
            sortAscending: [this.sort.direction === 'asc'],
            propertyFilter: [RestConstants.ALL],
        };
        this.sharingService
            .getChildren(this.params.nodeId, this.params.token, this.passwordInput, request)
            .subscribe((nodes) => {
                this.nodesDataSource.setData(nodes.nodes);
                this.nodesDataSource.isLoading = false;
                setTimeout(() => {
                    this.nodeEntries.initOptionsGenerator({
                        customOptions: this.options,
                    });
                });
            });
    }
    inviterIsAuthor() {
        return Helper.objectEquals(this.sharingInfo.invitedBy, this.sharingInfo.node.createdBy);
    }
    getPersonName(person: Person) {
        return ConfigurationHelper.getPersonWithConfigDisplayName(person, this.config);
    }

    childCount() {
        if (this.sharingInfo.node.type === RestConstants.CCM_TYPE_IO) {
            try {
                return (
                    parseInt(
                        this.sharingInfo.node.properties[
                            RestConstants.VIRTUAL_PROP_CHILDOBJECTCOUNT
                        ]?.[0],
                        10,
                    ) || 0
                );
            } catch (e) {}
        }
        return 0;
    }
}
