import { TranslateService } from '@ngx-translate/core';
import { forkJoin, Observable, Observer } from 'rxjs';
import { Params, Router } from '@angular/router';
import { Location } from '@angular/common';
import {
    ASSETS_BASE_PATH,
    DefaultGroups,
    ListItem,
    NodeHelperService as NodeHelperServiceBase,
    NodePersonNamePipe,
    NodesRightMode,
    OptionGroup,
    OptionItem,
    RepoUrlService,
    TemporaryStorageService,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { Helper } from '../core-module/rest/helper';
import { HttpClient } from '@angular/common/http';
import { MessageType } from '../util/message-type';
import { Toast } from './toast';
import {
    ComponentFactoryResolver,
    Inject,
    Injectable,
    Optional,
    ViewContainerRef,
} from '@angular/core';
import { BridgeService } from './bridge.service';
import {
    AuthorityProfile,
    CollectionReference,
    DeepLinkResponse,
    Permission,
    Repository,
    User,
} from '../core-module/rest/data-object';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestHelper } from '../core-module/rest/rest-helper';
import { RestConnectorService } from '../core-module/rest/services/rest-connector.service';
import { UniversalNode } from '../core-module/rest/definitions';
import { SessionStorageService } from '../core-module/rest/services/session-storage.service';
import { map } from 'rxjs/operators';
import { RestNodeService } from '../core-module/rest/services/rest-node.service';
import {
    ApiHelpersService,
    ConfigService,
    HOME_REPOSITORY,
    NetworkService,
    Node,
    TrackingV1Service,
} from 'ngx-edu-sharing-api';

export interface ConfigEntry {
    name: string;
    icon: string;
    scope?: string;
    isDisabled?: boolean;
    isSeparate?: boolean;
    isCustom: boolean;
    position?: number;
    url?: string;
    open?: () => void;
}

export interface ConfigOptionItem extends ConfigEntry {
    mode: string;
    scopes: string[];
    ajax: boolean;
    permission: string;
    group: string;
    toolpermission: string;
    isDirectory: string;
    showAsAction: boolean;
    multiple: boolean;
    changeStrategy: string;
}
export type HandleState = {
    [key in 'handleService' | 'doiService']?: boolean;
};
@Injectable({ providedIn: 'root' })
export class NodeHelperService extends NodeHelperServiceBase {
    private viewContainerRef: ViewContainerRef;
    constructor(
        translate: TranslateService,
        apiHelpersService: ApiHelpersService,
        networkService: NetworkService,
        configService: ConfigService,
        repoUrlService: RepoUrlService,
        @Optional() @Inject(ASSETS_BASE_PATH) assetsBasePath: string,
        private componentFactoryResolver: ComponentFactoryResolver,
        private config: ConfigService,
        private rest: RestConnectorService,
        private bridge: BridgeService,
        private http: HttpClient,
        private connector: RestConnectorService,
        private nodeService: RestNodeService,
        private toast: Toast,
        private router: Router,
        private sessionStorage: SessionStorageService,
        private storage: TemporaryStorageService,
        private trackingV1Service: TrackingV1Service,
        private location: Location,
    ) {
        super(
            translate,
            apiHelpersService,
            networkService,
            configService,
            repoUrlService,
            assetsBasePath,
        );
    }
    setViewContainerRef(viewContainerRef: ViewContainerRef) {
        this.viewContainerRef = viewContainerRef;
    }
    /**
     * returns true if all nodes have the requested right
     * mode (only works for collection refs):
     *   Local: check only rights of the node itself
     Original: check only rights of the original node this refers to (collection ref). If it is not a collection ref, fallback to local
     Both: check both rights of node + original combined via or
     *
     */
    public getNodesRight(nodes: any[], right: string, mode = NodesRightMode.Local) {
        if (nodes == null) return true;
        for (const node of nodes) {
            let currentMode = mode;
            // if this is not a collection ref -> force local mode
            if (
                node.aspects &&
                node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) === -1
            ) {
                currentMode = NodesRightMode.Local;
            }
            if (currentMode === NodesRightMode.Effective) {
                if (node.accessOriginal && node.accessOriginal.indexOf(right) !== -1) {
                    continue;
                }
                if (RestConstants.IMPLICIT_COLLECTION_PERMISSIONS.indexOf(right) === -1) {
                    // permission not matched on original -> implicit permissions from collection may apply
                    return false;
                }
            }
            // check regular node rights
            if (!node.access || node.access.indexOf(right) === -1) {
                return false;
            }
        }
        return true;
    }
    public handleNodeError(name: string, error: any): number {
        if (error.status === RestConstants.DUPLICATE_NODE_RESPONSE) {
            this.bridge.showTemporaryMessage(MessageType.error, 'WORKSPACE.TOAST.DUPLICATE_NAME', {
                name,
            });
            return error.status;
        } else if (
            error.error?.message?.includes(
                'org.alfresco.service.cmr.repository.CyclicChildRelationshipException',
            )
        ) {
            this.bridge.showTemporaryMessage(MessageType.error, 'WORKSPACE.TOAST.CYCLIC_NODE', {
                name,
            });
            return error.status;
        }
        this.bridge.showTemporaryMessage(MessageType.error, null, null, null, error);
        return error.status;
    }

    public downloadUrl(
        url: string,
        fileName = 'download',
        details?: { triggerTrackingEvent: boolean; node: Node },
    ) {
        if (details?.triggerTrackingEvent) {
            this.trackingV1Service
                .trackEvent({
                    repository: HOME_REPOSITORY,
                    node: details.node.ref.id,
                    event: 'DOWNLOAD_MATERIAL',
                })
                .subscribe(() => {});
        }
        if (this.bridge.isRunningCordova()) {
            this.bridge.showTemporaryMessage(MessageType.info, 'TOAST.DOWNLOAD_STARTED', {
                name: fileName,
            });
            this.bridge.getCordova().downloadContent(
                url,
                fileName,
                (deviceFileName: string) => {
                    if (this.bridge.getCordova().isAndroid()) {
                        this.bridge.showTemporaryMessage(
                            MessageType.info,
                            'TOAST.DOWNLOAD_FINISHED_ANDROID',
                            { name: fileName },
                        );
                    } else {
                        this.bridge.showTemporaryMessage(
                            MessageType.info,
                            'TOAST.DOWNLOAD_FINISHED_IOS',
                            { name: fileName },
                        );
                    }
                },
                () => {
                    this.bridge.showTemporaryMessage(
                        MessageType.error,
                        'TOAST.DOWNLOAD_FAILED',
                        { name: fileName },
                        {
                            link: {
                                caption: 'TOAST.DOWNLOAD_TRY_AGAIN',
                                callback: () => {
                                    this.downloadUrl(url, fileName);
                                },
                            },
                        },
                    );
                },
            );
        } else {
            window.open(url);
        }
    }

    /**
     * Download (a single) node
     */
    public async downloadNode(
        node: any,
        version = RestConstants.NODE_VERSION_CURRENT,
        metadata = false,
    ) {
        this.downloadUrl(
            (await this.repoUrlService.getRepoUrl(node.downloadUrl, node)) +
                (version && version != RestConstants.NODE_VERSION_CURRENT
                    ? '&version=' + version
                    : '') +
                '&metadata=' +
                metadata,
            node.name + (metadata ? '.txt' : ''),
        );
    }

    /**
     * fetches the preview of the node and appends it at preview.data
     */
    public async appendImageData(node: Node, quality = 70) {
        const options: any = this.rest.getRequestOptions();
        options.responseType = 'blob';
        const url = await this.repoUrlService.getRepoUrl(node.preview.url, node);
        const data = await this.rest
            .get<Blob>(url + '&allowRedirect=false&quality=' + quality, options, false)
            .toPromise();
        const reader = new FileReader();
        return new Promise((resolve) => {
            reader.onload = () => {
                const dataUrl = reader.result;
                node.preview.data = dataUrl.toString();
                resolve(node);
            };
            reader.readAsDataURL(data);
        });
    }

    /**
     * Get a user name for displaying
     * @param user
     * @returns {string}
     */
    public getUserDisplayName(user: AuthorityProfile | User) {
        return (user.profile.firstName + ' ' + user.profile.lastName).trim();
    }
    isSavedSearchObject(node: Node) {
        return node.mediatype == 'saved_search';
    }

    /**
     * Add custom options to the node menu (loaded via config)
     */
    public applyCustomNodeOptions(
        custom: ConfigOptionItem[],
        allNodes: Node[],
        selectedNodes: Node[],
        options: OptionItem[],
        replaceUrl: any = {},
    ) {
        if (custom) {
            for (const c of custom) {
                let item: OptionItem;
                if (c.changeStrategy === 'remove') {
                    const i = Helper.indexOfObjectArray(options, 'name', c.name);
                    if (i != -1) options.splice(i, 1);
                    continue;
                } else if (c.changeStrategy === 'update') {
                    item = options.find((o) => o.name === c.name);
                    if (!item) {
                        console.warn(
                            'Updating item ' +
                                c.name +
                                ' failed: No such item is currently present',
                        );
                        continue;
                    }
                } else {
                    let callback = (node: Node) => {
                        const nodes = node == null ? selectedNodes : [node];
                        let ids = '';
                        if (nodes) {
                            for (const node of nodes) {
                                if (ids) ids += ',';
                                ids += node.ref.id;
                            }
                        }
                        let url = c.url.replace(':id', ids);
                        url = url.replace(':api', this.connector.getAbsoluteEndpointUrl());
                        if (selectedNodes.length === 1) {
                            url = url.replace(
                                ':node',
                                encodeURIComponent(JSON.stringify(nodes[0])),
                            );
                        }
                        if (replaceUrl) {
                            for (const key in replaceUrl) {
                                url = url.replace(key, encodeURIComponent(replaceUrl[key]));
                            }
                        }
                        if (!c.ajax) {
                            window.open(url);
                            return;
                        }
                        this.toast.showProgressSpinner();
                        this.http.get(url).subscribe(
                            (data: any) => {
                                if (data.success)
                                    this.toast.error(
                                        data.success,
                                        null,
                                        data.message ? data.success : data.message,
                                        data.message,
                                    );
                                else if (data.error)
                                    this.toast.error(
                                        null,
                                        data.error,
                                        null,
                                        data.message ? data.error : data.message,
                                        data.message,
                                    );
                                else this.toast.error(null);
                                this.toast.closeProgressSpinner();
                            },
                            (error: any) => {
                                this.toast.error(error);
                                this.toast.closeProgressSpinner();
                            },
                        );
                    };
                    item = new OptionItem(c.name, c.icon, callback);
                }
                if (c.group) {
                    item.group = (DefaultGroups as any)[
                        Object.keys(DefaultGroups).find(
                            (key) => ((DefaultGroups as any)[key] as OptionGroup).id === c.group,
                        )
                    ];
                }
                if (c.position != null) {
                    let position = c.position;
                    if (c.position < 0) {
                        position =
                            options
                                .filter((o) => o.group.id === item.group.id)
                                .reduce((o1, o2) => (o1.priority > o2.priority ? o1 : o2))
                                .priority - c.position;
                    }
                    item.priority = c.position;
                }
                if (c.icon != null) {
                    item.icon = c.icon;
                }
                item.showAsAction = c.showAsAction;
                item.isSeparate = c.isSeparate;
                if (c.changeStrategy !== 'update') {
                    item.customEnabledCallback = (nodes) => {
                        if (c.permission) {
                            return this.getNodesRight(nodes, c.permission);
                        }
                        return true;
                    };
                    item.isEnabled = item.customEnabledCallback(null);
                    item.customShowCallback = async (nodes) => {
                        if (c.mode == 'nodes' && !nodes?.length) return false;
                        if (c.mode == 'noNodes' && nodes && nodes.length) return false;
                        if (
                            c.mode == 'noNodesNotEmpty' &&
                            ((nodes && nodes.length) || !allNodes || !allNodes.length)
                        )
                            return false;
                        // @ts-ignore
                        if (
                            c.mode == 'nodes' &&
                            c.isDirectory != 'any' &&
                            nodes &&
                            c.isDirectory != nodes[0].isDirectory
                        )
                            return false;
                        if (
                            c.toolpermission &&
                            !this.connector.hasToolPermissionInstant(c.toolpermission)
                        )
                            return false;
                        if (c.multiple != null && !c.multiple && nodes && nodes.length > 1)
                            return false;

                        return true;
                    };
                    options.splice(c.position ?? 0, 0, item);
                }
            }
        }
    }

    /**
     * Apply (redirect url) node for usage by LMS systems
     * @param router
     * @param node
     */
    addNodeToLms(node: Node, reurl: string) {
        this.storage.set(TemporaryStorageService.APPLY_TO_LMS_PARAMETER_NODE, node);
        this.router.navigate(
            [UIConstants.ROUTER_PREFIX + 'apply-to-lms', node.ref.repo, node.ref.id],
            { queryParams: { reurl } },
        );
    }

    async addNodesToLTIPlatform(nodes: Node[]) {
        this.toast.showProgressSpinner();
        try {
            // prepare usages in case of remote refs
            nodes = await forkJoin(
                nodes.map((n) => this.nodeService.prepareUsage(n.ref.id, n.ref.repo)),
            )
                .pipe(map((nodes) => nodes.map((n) => n.remote || n.node)))
                .toPromise();
            let url = this.connector.createUrl('lti/v13/generateDeepLinkingResponse', null, []);
            nodes.forEach((n) => {
                if (!url.includes('?')) {
                    url += '?nodeIds=' + n.ref.id;
                } else {
                    url += '&nodeIds=' + n.ref.id;
                }
            });
            const response = await this.connector
                .get<DeepLinkResponse>(url, this.connector.getRequestOptions())
                .toPromise();
            this.postLtiDeepLinkResponse(
                response.jwtDeepLinkResponse,
                response.ltiDeepLinkReturnUrl,
            );
        } catch (e) {
            this.toast.error(e);
        }
        this.toast.closeProgressSpinner();
    }

    private postLtiDeepLinkResponse(jwt: string, url: string) {
        const form = window.document.createElement('form');
        form.setAttribute('method', 'post');
        form.setAttribute('action', url);
        form.appendChild(this.createHiddenElement('JWT', jwt));
        window.document.body.appendChild(form);
        form.submit();
    }

    private createHiddenElement(name: string, value: string): HTMLInputElement {
        const hiddenField = document.createElement('input');
        hiddenField.setAttribute('name', name);
        hiddenField.setAttribute('value', value);
        hiddenField.setAttribute('type', 'hidden');
        return hiddenField;
    }

    /**
     * Download one or multiple nodes
     * @param node
     */
    async downloadNodes(nodes: Node[], fileName = 'download.zip') {
        if (nodes.length === 1) return await this.downloadNode(nodes[0]);

        const nodesString = RestHelper.getNodeIds(nodes).join(',');
        const url =
            this.connector.getAbsoluteEndpointUrl() +
            '../eduservlet/download?appId=' +
            encodeURIComponent(nodes[0].ref.repo) +
            '&nodeIds=' +
            encodeURIComponent(nodesString) +
            '&fileName=' +
            encodeURIComponent(fileName);
        this.downloadUrl(url, fileName);
    }

    getLRMIProperty(data: any, item: ListItem) {
        // http://dublincore.org/dcx/lrmi-terms/2014-10-24/
        if (item.type == 'NODE') {
            if (item.name == RestConstants.CM_NAME || item.name == RestConstants.CM_PROP_TITLE) {
                return 'name';
            }
            if (item.name == RestConstants.CM_CREATOR) {
                return 'author';
            }
            if (item.name == RestConstants.CM_PROP_C_CREATED) {
                return 'dateCreated';
            }
        }
        return null;
    }
    getLRMIAttribute(data: any, item: ListItem) {
        // http://dublincore.org/dcx/lrmi-terms/2014-10-24/
        if (item.type === 'NODE') {
            if (data.reference) {
                data = data.reference;
            }
            if (item.name === RestConstants.CM_CREATOR) {
                return new NodePersonNamePipe(this.config).transform(data);
            }
            if (item.name === RestConstants.CM_NAME) {
                return data.name;
            }
            if (
                item.name === RestConstants.CM_PROP_TITLE ||
                item.name === RestConstants.LOM_PROP_TITLE
            ) {
                return data.title;
            }
            if (
                item.name === RestConstants.CM_PROP_C_CREATED ||
                item.name === RestConstants.CM_MODIFIED_DATE
            ) {
                return data.properties[item.name + 'ISO8601'];
            }
        }
        return null;
    }

    public getSourceIconRepoPath(repo: Repository) {
        if (repo.icon) return repo.icon;
        if (repo.isHomeRepo) return this.getSourceIconPath('home');
        return this.getSourceIconPath(repo.repositoryType.toLowerCase());
    }
    allFiles(nodes: any[]) {
        let allFiles = true;
        if (nodes) {
            for (let node of nodes) {
                if (!node) continue;
                if (node.reference) node = node.reference;
                if (node.isDirectory || node.type != RestConstants.CCM_TYPE_IO) allFiles = false;
            }
        }
        return allFiles;
    }
    allFolders(nodes: Node[]) {
        let allFolders = true;
        if (nodes) {
            for (const node of nodes) {
                if (!node.isDirectory) allFolders = false;
            }
        }
        return allFolders;
    }
    hasAnimatedPreview(node: Node) {
        return (
            !node.preview.isIcon && (node.mediatype == 'file-video' || node.mimetype == 'image/gif')
        );
    }

    askCCPublish(node: Node) {
        const mail =
            node.createdBy.firstName +
            ' ' +
            node.createdBy.lastName +
            '<' +
            node.createdBy.mailbox +
            '>';
        const subject = this.translate.instant('ASK_CC_PUBLISH_SUBJECT', {
            name: RestHelper.getTitle(node),
        });
        window.location.href = 'mailto:' + mail + '?subject=' + encodeURIComponent(subject);
    }

    /**
     * checks if a doi handle is active (node must be explicitly public and handle it must be present)
     * @param {Node} node
     * @param {Permissions} permissions
     * @returns {boolean}
     */
    getHandleStates(node: Node, permissions: Permission[]) {
        const states: HandleState = {};
        if (
            node.aspects.indexOf(RestConstants.CCM_ASPECT_PUBLISHED) != -1 &&
            node.properties[RestConstants.CCM_PROP_PUBLISHED_HANDLE_ID]
        ) {
            for (const permission of permissions) {
                if (permission.authority.authorityName === RestConstants.AUTHORITY_EVERYONE)
                    states.handleService = true;
            }
        }
        if (
            node.aspects.indexOf(RestConstants.CCM_ASPECT_PUBLISHED) != -1 &&
            node.properties[RestConstants.CCM_PROP_PUBLISHED_DOI_ID]
        ) {
            for (const permission of permissions) {
                if (permission.authority.authorityName === RestConstants.AUTHORITY_EVERYONE)
                    states.doiService = true;
            }
        }
        return states;
    }

    propertiesFromConnector(event: any) {
        const name = event.name + '.' + event.type.filetype;
        const prop = RestHelper.createNameProperty(name);
        prop[RestConstants.LOM_PROP_TECHNICAL_FORMAT] = [event.type.mimetype];
        if (event.type.mimetype == 'application/zip') {
            prop[RestConstants.CCM_PROP_CCRESSOURCETYPE] = [event.type.ccressourcetype];
            prop[RestConstants.CCM_PROP_CCRESSOURCESUBTYPE] = [event.type.ccresourcesubtype];
            prop[RestConstants.CCM_PROP_CCRESSOURCEVERSION] = [event.type.ccressourceversion];
        }
        if (event.type.editorType) {
            prop[RestConstants.CCM_PROP_EDITOR_TYPE] = [event.type.editorType];
        }
        return prop;
    }
    static getActionbarNodes<T>(nodes: T[], node: T): T[] {
        return node ? [node] : nodes && nodes.length ? nodes : null;
    }

    referenceOriginalExists(node: Node | CollectionReference) {
        if (node == null) return true;
        return node.hasOwnProperty('originalId') ? (node as any).originalId != null : true;
    }

    /**
     * returns true if the nodes have different values for the given property, false if all values of this property are identical
     */
    hasMixedPropertyValues(nodes: Node[], prop: string) {
        let found = null;
        let foundAny = false;
        for (let node of nodes) {
            const value = node.properties[prop];
            if (foundAny && !Helper.arrayEquals(found, value)) return true;
            found = value;
            foundAny = !!value;
        }
        return false;
    }
    /**
     * get the value for all nodes, if it is identical. Otherwise, the fallback is returned
     * @param prop
     * @param fallbackNotIdentical Fallback when they're not equaling
     * @param fallbackIsEmpty Fallback when all are empty
     * @param asArray If false, only the first element of the property array will be returned
     */
    getValueForAll(
        nodes: Node[],
        prop: string,
        fallbackNotIdentical: any = '',
        fallbackIsEmpty = fallbackNotIdentical,
        asArray = true,
    ) {
        let found = null;
        let foundAny = false;

        for (let node of nodes) {
            const v = node.properties[prop];
            const value = v ? (asArray ? v : v[0]) : fallbackIsEmpty;
            if (foundAny && found !== value) return fallbackNotIdentical;
            found = value;
            foundAny = true;
        }
        if (!foundAny) return fallbackIsEmpty;
        return found;
    }

    getValueForAllString(
        values: string[],
        fallbackNotIdentical: any = '',
        fallbackIsEmpty = fallbackNotIdentical,
        asArray = true,
    ) {
        let found = null;
        let foundAny = false;

        for (let v of values) {
            const value = v ? (asArray ? v : v[0]) : fallbackIsEmpty;
            if (foundAny && found !== value) return fallbackNotIdentical;
            found = value;
            foundAny = true;
        }
        if (!foundAny) return fallbackIsEmpty;
        return found;
    }

    addHttpIfRequired(link: string) {
        if (link.indexOf('://') == -1) {
            return 'http://' + link;
        }
        return link;
    }

    /**
     * Returns true if this node is a copy of another node, just used as a publish target.
     */
    isNodePublishedCopy(o: Node): boolean {
        return !!o.properties?.[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]?.[0];
    }

    /**
     * Returns the full URL to a node, including the server origin and base href.
     */
    getNodeUrl(node: UniversalNode, queryParams?: Params): string {
        const link = this.getNodeLink('queryParams', node);
        if (link) {
            const urlTree = this.router.createUrlTree([this.getNodeLink('routerLink', node)], {
                queryParams: {
                    ...(this.getNodeLink('queryParams', node) as Params),
                    ...queryParams,
                },
            });
            return location.origin + this.location.prepareExternalUrl(urlTree.toString());
        } else {
            return null;
        }
    }

    getDefaultInboxFolder() {
        return new Observable<Node>((subscriber) => {
            this.sessionStorage.get('defaultInboxFolder', RestConstants.INBOX).subscribe(
                (id) => {
                    this.nodeService.getNodeMetadata(id).subscribe(
                        (node) => {
                            subscriber.next(node.node);
                            subscriber.complete();
                        },
                        (error) => {
                            console.warn('error resolving defaultInboxFolder', error);
                            return this.nodeService
                                .getNodeMetadata(RestConstants.INBOX)
                                .pipe(map((n) => n.node))
                                .subscribe(subscriber);
                        },
                    );
                },
                (error) => {
                    console.warn('error resolving defaultInboxFolder', error);
                    return this.nodeService
                        .getNodeMetadata(RestConstants.INBOX)
                        .pipe(map((n) => n.node))
                        .subscribe(subscriber);
                },
            );
        });
    }

    /**
     * this method syncs common attributes like name, title, description on this node by fetching it from the properties
     * This is helpful if you did client-side editing and want to reflect the changes in the UI
     * @param node
     */
    syncAttributesWithProperties(node: Node) {
        node.name = node.properties[RestConstants.CM_NAME]?.[0];
        node.title =
            node.properties[RestConstants.CM_PROP_TITLE]?.[0] ||
            node.properties[RestConstants.LOM_PROP_TITLE]?.[0];
        if (node.collection) {
            node.collection.description = node.properties[RestConstants.CM_DESCRIPTION]?.[0];
        }
        return node;
    }
}
