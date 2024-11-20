import { PlatformLocation } from '@angular/common';
import {
    ComponentFactoryResolver,
    ComponentRef,
    ElementRef,
    EmbeddedViewRef,
    Injector,
    NgZone,
    Type,
    ViewContainerRef,
} from '@angular/core';
import { ActivatedRoute, NavigationExtras, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Ace, LoginInfo } from 'ngx-edu-sharing-api';
import { ListItem, OPEN_URL_MODE, UIConstants } from 'ngx-edu-sharing-ui';
import { Observable, Observer, forkJoin as observableForkJoin, of } from 'rxjs';
import { catchError, first, take } from 'rxjs/operators';
import { BridgeService } from '../services/bridge.service';
import {
    CollectionReference,
    Connector,
    Filetype,
    MdsInfo,
    Node,
    NodeLock,
    NodeWrapper,
    ParentList,
    Permission,
} from '../core-module/rest/data-object';
import { Helper } from '../core-module/rest/helper';
import { RestConstants } from '../core-module/rest/rest-constants';
import { RestHelper } from '../core-module/rest/rest-helper';
import { ConfigurationService } from '../core-module/rest/services/configuration.service';
import { FrameEventsService } from '../core-module/rest/services/frame-events.service';
import { RestCollectionService } from '../core-module/rest/services/rest-collection.service';
import { RestConnectorService } from '../core-module/rest/services/rest-connector.service';
import { RestConnectorsService } from '../core-module/rest/services/rest-connectors.service';
import { RestIamService } from '../core-module/rest/services/rest-iam.service';
import { RestNodeService } from '../core-module/rest/services/rest-node.service';
import { MessageType } from '../util/message-type';
import {
    OK,
    YES_OR_NO,
} from '../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { NodeHelperService } from '../services/node-helper.service';
import { RouterHelper } from '../util/router.helper';
import { Toast } from '../services/toast';
import { ExtendedAce } from '../features/dialogs/dialog-modules/share-dialog/share-dialog.component';

export class UIHelper {
    // TODO: check, whether these parameters are still used and remove or include them in
    // `PRESERVED_QUERY_PARAMS` in `main/location-strategy.ts` accordingly.
    static COPY_URL_PARAMS = ['reurlTypes', 'reurlCreate', 'applyDirectories', 'onlyDownloadable'];
    public static getBlackWhiteContrast(color: string) {}

    static changeQueryParameter(router: Router, route: ActivatedRoute, name: string, value: any) {
        route.queryParams.pipe(take(1)).subscribe((data: any) => {
            let queryParams: any = {};
            for (let key in data) {
                queryParams[key] = data[key];
            }
            queryParams[name] = value;
            router.navigate([], { queryParams: queryParams });
        });
    }

    /**
     * The materialize textarea should auto-adjust height based on content
     * however, ngmodel does not refresh the element. This method will simulate a keyboard event to refresh the state
     * It will wait for the element to come active and send a keyboard event
     * Using ng's ElementRef will not work :/ We have to use a global dom id
     * @param {ElementRef} element
     */
    static invalidateMaterializeTextarea(id: string, timeout = 10) {
        setTimeout(() => {
            if (document.getElementById(id) == null) {
                UIHelper.invalidateMaterializeTextarea(id, 100);
                return;
            }
            let event = new KeyboardEvent('keyup', {
                view: window,
                bubbles: true,
                cancelable: true,
            });
            document.getElementById(id).dispatchEvent(event);
        }, timeout);
    }

    /**
     * returns true if the given string seems to be an email
     * @param {string} email
     */
    static isEmail(mail: string) {
        if (!mail) return false;
        if (mail.trim()) {
            const EMAIL_REGEXP =
                /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
            if (mail && !EMAIL_REGEXP.test(mail)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    /**
     * returns an factor indicating the strength of a password
     * Higher values mean better password strength
     * @param password
     */
    private static getPasswordStrength(password: string) {
        let strength: number;
        // These are weighting factors
        let flc = 1.0; // lowercase factor
        let fuc = 1.0; // uppercase factor
        let fnm = 1.3; // number factor
        let fsc = 1.5; // special char factor

        let regex_sc = new RegExp(/[!@#\$%\^\&*\)\(+=._-]/g);

        let lcase_count: any = password.match(/[a-z]/g);
        lcase_count = lcase_count ? lcase_count.length : 0;
        let ucase_count: any = password.match(/[A-Z]/g);
        ucase_count = ucase_count ? ucase_count.length : 0;
        let num_count: any = password.match(/[0-9]/g);
        num_count = num_count ? num_count.length : 0;
        let schar_count: any = password.match(regex_sc);
        schar_count = schar_count ? schar_count.length : 0;
        strength =
            (lcase_count * flc + 1) *
            (ucase_count * fuc + 1) *
            (num_count * fnm + 1) *
            (schar_count * fsc + 1);
        // / (avg + 1);

        // console.log('Strengt: '+strength);
        return strength;
    }

    /**
     * returns an factor indicating the repeat of signd in a password
     * Higher values mean better password strength
     * @param password
     */
    private static detectPW(password: string) {
        let pw_parts = password.split('');
        let i;
        let ords = new Array();
        for (i in pw_parts) {
            ords[i] = pw_parts[i].charCodeAt(0);
        }
        let accum = 0;
        let lasti = ords.length - 1;

        for (let i = 0; i < lasti; ++i) {
            accum += Math.abs(ords[i] - ords[i + 1]);
        }
        // console.log('detect: '+accum/lasti);
        return accum / lasti;
    }

    /**
     * returns the password strength as a string value
     * weak, accept, medium, strong
     * @param password
     */
    public static getPasswordStrengthString(password: string) {
        let min_length = 5;
        // console.log("strength: "+this.getPasswordStrength(password));
        if (password && password.length >= min_length) {
            if (this.getPasswordStrength(password) > 70) {
                if (this.getPasswordStrength(password) > 160) {
                    return 'strong';
                } else {
                    return 'medium';
                }
            } else {
                return 'accept';
            }
        } else {
            return 'weak';
        }
    }

    static routeToSearchNode(router: Router, reurl: string, node: Node) {
        let converted = UIHelper.convertSearchParameters(node);
        router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
            queryParams: {
                query: converted.query,
                reurl: reurl,
                repository: node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_REPOSITORY],
                mds: node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_MDS],
                parameters: JSON.stringify(converted.parameters),
            },
        });
    }

    public static goToNode(router: Router, node: Node) {
        router.navigate([UIConstants.ROUTER_PREFIX, 'render', node.ref.id]);
    }

    public static goToCollection(
        router: Router,
        node: Node,
        mode: null | 'new' | 'edit' = null,
        extras: NavigationExtras = {},
    ) {
        if (mode === 'new' || mode === 'edit') {
            router.navigate(
                [UIConstants.ROUTER_PREFIX, 'collections', 'collection', mode, node.ref.id],
                extras,
            );
        } else {
            extras.queryParams = { id: node.ref.id };
            router.navigate([UIConstants.ROUTER_PREFIX, 'collections'], extras);
        }
    }

    /**
     * Navigate to the search in reurl (apply) mode
     * when done, the app will redirect to the current location
     */
    public static openSearchWithReurl(
        platformLocation: PlatformLocation,
        router: Router,
        mode: 'REDIRECT' | 'WINDOW',
        extras: NavigationExtras = {},
    ) {
        if (!extras.queryParams) {
            extras.queryParams = {};
        }
        if (mode === 'REDIRECT') {
            extras.queryParams.reurl = window.location.href;
        } else {
            extras.queryParams.reurl = 'WINDOW';
        }
        if (mode === 'REDIRECT') {
            return router.navigate([UIConstants.ROUTER_PREFIX + 'search'], extras);
        } else {
            return window.open(
                platformLocation.getBaseHrefFromDOM() +
                    router.createUrlTree([UIConstants.ROUTER_PREFIX + 'search'], extras).toString(),
                '_blank',
                'toolbar=no,scrollbars=yes,resizable=yes',
            );
        }
    }

    /**
     * Navigate to the workspace
     * @param nodeService instance of NodeService
     * @param router instance of Router
     * @param login a result of the isValidLogin method
     * @param node The node to open and show
     */
    public static goToWorkspace(
        nodeService: RestNodeService,
        router: Router,
        login: LoginInfo,
        node: Node,
        extras: NavigationExtras = {},
    ) {
        nodeService.getNodeParents(node.ref.id).subscribe((data: ParentList) => {
            extras.queryParams = {
                id: node.parent.id,
                file: node.ref.id,
                root: data.scope,
            };
            void router.navigate(
                [
                    UIConstants.ROUTER_PREFIX +
                        'workspace/' +
                        (login.currentScope ? login.currentScope : 'files'),
                ],
                extras,
            );
        });
    }

    /**
     * Navigate to the workspace
     * @param nodeService instance of NodeService
     * @param router instance of Router
     * @param login a result of the isValidLogin method
     * @param folder The folder id to open
     */
    public static goToWorkspaceFolder(
        nodeService: RestNodeService,
        router: Router,
        login: LoginInfo,
        folder: string,
        extras: NavigationExtras = {},
    ) {
        extras.queryParams = { id: folder };
        router.navigate(
            [
                UIConstants.ROUTER_PREFIX +
                    'workspace/' +
                    (login && login.currentScope ? login.currentScope : 'files'),
            ],
            extras,
        );
    }

    static convertSearchParameters(node: Node) {
        let parameters = JSON.parse(
            node.properties[RestConstants.CCM_PROP_SAVED_SEARCH_PARAMETERS],
        );
        let result: any = { parameters: {}, query: null };
        for (let parameter of parameters) {
            if (parameter.property == RestConstants.PRIMARY_SEARCH_CRITERIA) {
                if (parameter.values[0] == '*') parameter.values[0] = '';
                result.query = parameter.values[0];
                continue;
            }
            result.parameters[parameter.property] = parameter.values;
        }
        return result;
    }

    static materializeSelect() {
        eval("$('select').css('display','none');$('select').material_select()");
    }

    static showAddedToCollectionInfo(
        bridge: BridgeService,
        router: Router,
        node: any,
        count: number,
        asProposal = false,
    ) {
        let scope = node.collection ? node.collection.scope : node.scope;
        let type = node.collection ? node.collection.type : node.type;
        if (scope == RestConstants.COLLECTIONSCOPE_MY) {
            scope = 'MY';
        } else if (
            scope == RestConstants.COLLECTIONSCOPE_ORGA ||
            scope == RestConstants.COLLECTIONSCOPE_CUSTOM
        ) {
            scope = 'SHARED';
        } else if (
            scope == RestConstants.COLLECTIONSCOPE_ALL ||
            scope == RestConstants.COLLECTIONSCOPE_CUSTOM_PUBLIC
        ) {
            scope = 'PUBLIC';
        } else if (type == RestConstants.COLLECTIONTYPE_EDITORIAL) {
            scope = 'PUBLIC';
        } else if (type == RestConstants.COLLECTIONTYPE_MEDIA_CENTER) {
            scope = 'MEDIA_CENTER';
        }
        if (asProposal) {
            bridge.showTemporaryMessage(
                MessageType.info,
                'WORKSPACE.TOAST.PROPOSED_FOR_COLLECTION',
                { count: count, collection: RestHelper.getTitle(node) },
            );
        } else {
            bridge.showTemporaryMessage(
                MessageType.info,
                'WORKSPACE.TOAST.ADDED_TO_COLLECTION_' + scope,
                { count: count, collection: RestHelper.getTitle(node) },
                {
                    link: {
                        caption: 'WORKSPACE.TOAST.VIEW_COLLECTION',
                        callback: () => UIHelper.goToCollection(router, node),
                    },
                },
            );
        }
    }

    static prepareMetadatasets(translate: TranslateService, mdsSets: MdsInfo[]) {
        for (let i = 0; i < mdsSets.length; i++) {
            if (mdsSets[i].id == 'mds')
                mdsSets[i].name = translate.instant('DEFAULT_METADATASET', {
                    name: mdsSets[i].name,
                });
        }
    }

    /**
     * handles adding nodes to a collection
     * @param nodeHelper
     * @param collectionService
     * @param router
     * @param bridge
     * @param collection
     * @param nodes
     * @param callback
     * @param allowDuplicate false (default) will trigger a confirmation if duplicate was detected, true will always create a duplicate and 'ignore' works as false but will not trigger a confirmation but silently abort
     */

    static addToCollection(
        nodeHelper: NodeHelperService,
        collectionService: RestCollectionService,
        router: Router,
        bridge: BridgeService,
        collection: Node,
        nodes: Node[],
        asProposal = false,
        callback: (nodes: CollectionReference[]) => void = null,
        allowDuplicate: boolean | 'ignore' = false,
    ) {
        observableForkJoin(
            nodes.map((node) =>
                collectionService
                    .addNodeToCollection(
                        collection.ref.id,
                        node.ref.id,
                        node.ref.repo,
                        allowDuplicate === true,
                        asProposal,
                    )
                    .pipe(catchError((error) => of({ error, node }))),
            ),
        ).subscribe(async (results) => {
            const success: NodeWrapper[] = results.filter((r) => !(r as any).error);
            const failed: { node: Node; error: any }[] = results.filter(
                (r) => !!(r as any).error,
            ) as { node: Node; error: any }[];
            if (success.length > 0) {
                UIHelper.showAddedToCollectionInfo(
                    bridge,
                    router,
                    collection,
                    success.length,
                    asProposal,
                );
            }
            if (failed.length > 0) {
                const duplicated = failed.filter(
                    ({ error }) => error.status === RestConstants.DUPLICATE_NODE_RESPONSE,
                );
                if (duplicated.length > 0 && !asProposal) {
                    if (allowDuplicate !== 'ignore') {
                        const dialogRef = await bridge.openGenericDialog({
                            title: 'COLLECTIONS.ADD_TO.DUPLICATE_TITLE',
                            message: 'COLLECTIONS.ADD_TO.DUPLICATE_MESSAGE',
                            messageParameters: { count: duplicated.length.toString() },
                            buttons: YES_OR_NO,
                        });
                        dialogRef.afterClosed().subscribe((response) => {
                            if (response === 'YES') {
                                UIHelper.addToCollection(
                                    nodeHelper,
                                    collectionService,
                                    router,
                                    bridge,
                                    collection,
                                    duplicated.map((d) => d.node),
                                    false,
                                    (nodes) =>
                                        // Invoke `callback` with both, the nodes successfully added
                                        // before and the duplicate nodes added now.
                                        callback?.([
                                            ...success.map((n) => n.node as CollectionReference),
                                            ...nodes,
                                        ]),
                                    true,
                                );
                            } else if (response === 'NO') {
                                // Invoke `callback` only with the nodes successfully added
                                // before.
                                callback?.(success.map((n) => n.node as CollectionReference));
                            } else {
                                // Dialog was canceled by the user.
                                //
                                // TODO: should we invoke `callback` here?
                                bridge.closeModalDialog();
                            }
                        });
                        return;
                    }
                } else {
                    nodeHelper.handleNodeError(
                        RestHelper.getTitle(failed[0].node),
                        failed[0].error,
                    );
                }
            }

            if (callback) {
                callback(success.map((n) => n.node as CollectionReference));
            }
        });
    }

    static openConnector(
        connector: RestConnectorsService,
        iam: RestIamService,
        events: FrameEventsService,
        toast: Toast,
        node: Node,
        type: Filetype = null,
        win: Window = null,
        connectorType: Connector = null,
        newWindow = true,
        parameters: { [key in string]: string[] } = {},
    ) {
        if (connectorType == null) {
            connectorType = connector.connectorSupportsEdit(node);
        }
        let isCordova = connector.getRestConnector().getBridgeService().isRunningCordova();
        if (win == null && newWindow) {
            win = UIHelper.getNewWindow(connector.getRestConnector());
        }

        connector.nodeApi.isLocked(node.ref.id).subscribe(
            (result: NodeLock) => {
                if (result.isLocked) {
                    toast.error(null, 'TOAST.NODE_LOCKED');
                    if (win) win.close();
                    return;
                }
                iam.getCurrentUserAsync().then(
                    (user) => {
                        if (
                            user.person.quota.enabled &&
                            user.person.quota.sizeCurrent >= user.person.quota.sizeQuota
                        ) {
                            void toast.openGenericDialog({
                                title: 'CONNECTOR_QUOTA_REACHED_TITLE',
                                message: 'CONNECTOR_QUOTA_REACHED_MESSAGE',
                                buttons: OK,
                            });
                            if (win) win.close();
                            return;
                        }
                        connector.generateToolUrl(connectorType, type, node, parameters).subscribe(
                            (url: string) => {
                                if (win) {
                                    win.location.href = url;
                                } else if (isCordova) {
                                    UIHelper.openUrl(
                                        url,
                                        connector.getRestConnector().getBridgeService(),
                                        OPEN_URL_MODE.Blank,
                                    );
                                } else {
                                    window.location.replace(url);
                                }
                                if (win) {
                                    events.addWindow(win);
                                }
                            },
                            (error) => {
                                toast.error(null, error);
                                if (win) win.close();
                            },
                        );
                    },
                    (error) => {
                        toast.error(null, error);
                        if (win) win.close();
                    },
                );
            },
            (error: any) => {
                toast.error(error);
                if (win) win.close();
            },
        );
    }

    static openLTIResourceLink(w: any, node: Node) {
        //let w = window.open('');
        if (w == null) {
            console.log('window is null');
            w = window.open('');
        }
        w.location.href =
            '/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationFormResourceLink?nodeId=' +
            node.ref.id;

        /*let w = window.open(
            '/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationFormResourceLink?nodeId=' +
                node.ref.id,
            '_blank',
        );
        if (!w) {
            window.alert('popups are disabled');
        }*/
    }

    static setFocusOnCard() {
        let elements = document.getElementsByClassName('card')[0].getElementsByTagName('*');
        this.focusElements(elements);
    }

    static setFocusOnDropdown(ref: ElementRef) {
        // the first element(s) might be currently invisible, so try to focus from bottom to top
        if (ref && ref.nativeElement) {
            let elements = ref.nativeElement.getElementsByTagName('a');
            this.focusElements(elements);
        }
    }

    private static focusElements(elements: any) {
        for (let i = elements.length - 1; i >= 0; i--) {
            elements[i].focus();
        }
    }

    static getDefaultCollectionColumns() {
        let columns = [];
        columns.push(new ListItem('COLLECTION', 'title'));
        columns.push(new ListItem('COLLECTION', 'info'));
        columns.push(new ListItem('COLLECTION', 'scope'));
        return columns;
    }

    static goToDefaultLocation(
        router: Router,
        platformLocation: PlatformLocation,
        configService: ConfigurationService,
        replaceUrl = false,
    ) {
        let defaultLocation = configService.instant('loginDefaultLocation', 'workspace');
        if (!defaultLocation.match(/https?:\/\/*/)) {
            defaultLocation = UIConstants.ROUTER_PREFIX + defaultLocation;
        }
        RouterHelper.navigateToAbsoluteUrl(platformLocation, router, defaultLocation, replaceUrl);
    }

    static openUrl(url: string, bridge: BridgeService, mode = OPEN_URL_MODE.Current) {
        if (bridge.isRunningCordova()) {
            if (mode == OPEN_URL_MODE.BlankSystemBrowser) {
                return bridge.getCordova().openBrowser(url);
            } else {
                return bridge.getCordova().openInAppBrowser(url);
            }
        } else {
            if (mode == OPEN_URL_MODE.Current) {
                window.location.href = url;
                return;
            } else {
                return window.open(url, '_blank');
            }
        }
    }

    /**
     * open a window (blank) to prevent popup blocking
     * @param {RestConnectorService} connector
     * @returns {any}
     */
    public static getNewWindow(connector: RestConnectorService) {
        if (connector.getBridgeService().isRunningCordova()) return null;
        return window.open('');
    }

    /**
     * dynamically inject an angular component into a regular html dom element
     * @param componentFactoryResolver The resolver service
     * @param viewContainerRef The viewContainerRef service
     * @param componentName The name of the angular component (e.g. SpinnerComponent)
     * @param targetElement The target element of the dom. If the element is null (not found), nothing is done
     * @param bindings Optional bindings (inputs & outputs) to the given component
     * @param delay Optional inflating delay in ms(some components may need some time to "init" the layout)
     * @param replace Whether to replace to previous `innerHTML` of `targetElement`
     * @param injector (to fetch templates for the component)
     */
    public static injectAngularComponent<T>(
        componentFactoryResolver: ComponentFactoryResolver,
        viewContainerRef: ViewContainerRef,
        componentName: Type<T>,
        targetElement: Element,
        bindings: { [key: string]: any } = null,
        { delay = 0, replace = true } = {},
        injector?: Injector,
    ): ComponentRef<T> {
        if (targetElement == null) {
            return null;
        }
        const factory = componentFactoryResolver.resolveComponentFactory(componentName);
        const component: ComponentRef<T> = viewContainerRef.createComponent(
            factory,
            undefined,
            injector,
        );
        if (bindings) {
            const instance: { [key: string]: any } = component.instance;
            for (const key in bindings) {
                const binding = bindings[key];
                if (binding instanceof Function) {
                    // subscribe so callback can properly invoked
                    instance[key].subscribe((value: any) => binding(value));
                } else {
                    instance[key] = binding;
                    // `ngOnChanges` won't be called on the component like this. Consider doing
                    // something like this:
                    // https://scm.edu-sharing.com/edu-sharing/projects/oeh-redaktion/ng-meta-widgets/-/blob/1603fb2dedadd3952401385bcbd91a4bd8407643/src/app/app.module.ts#L66-79
                }
            }
        }

        // 3. Get DOM element from component
        const domElem = (component.hostView as EmbeddedViewRef<any>).rootNodes[0] as HTMLElement;
        domElem.style.display = 'none';
        if (replace) {
            targetElement.innerHTML = null;
        }
        targetElement.appendChild(domElem);
        setTimeout(() => {
            domElem.style.display = null;
        }, delay);
        return component;
    }

    /**
     * returns common active route parameters that should be keeped
     *
     * @deprecated Handled by `AppLocationStrategy` now.
     */
    // TODO: remove this function and all usages.
    static getCommonParameters(route: ActivatedRoute) {
        return new Observable<any>((observer: Observer<any>) => {
            route.queryParams
                .pipe()
                .pipe(first())
                .subscribe((queryParams) => {
                    let result: any = {};
                    UIHelper.COPY_URL_PARAMS.forEach((params) => {
                        if (queryParams[params]) {
                            result[params] = queryParams[params];
                        }
                    });
                    observer.next(result);
                    observer.complete();
                });
        });
    }

    /**
     * returns true if the error message includes the given string
     * @param error
     * @param {string} data
     * @returns {boolean}
     */
    static errorContains(error: any, data: string) {
        try {
            return (
                error.error?.details?.cause?.includes(data) || error.error?.message?.includes(data)
            );
        } catch (e) {}
        return false;
    }

    /**
     * waits until the given component/object is not null and available
     * @param ngZone NgZone Service
     * @param clz the class where the component is attached (usually "this")
     * @param componentName The name of the property
     */
    static waitForComponent(ngZone: NgZone, clz: any, componentName: string) {
        return ngZone.runOutsideAngular(() => {
            return new Observable((observer: Observer<any>) => {
                let interval = setInterval(() => {
                    if (clz[componentName]) {
                        observer.next(clz[componentName]);
                        observer.complete();
                        clearInterval(interval);
                    } else if (!clz) {
                        clearInterval(interval);
                    }
                }, 1000 / 60);
            });
        });
    }

    //http://stackoverflow.com/questions/25099409/copy-to-clipboard-as-plain-text
    static copyElementToClipboard(input: HTMLTextAreaElement) {
        input.focus({ preventScroll: true });
        input.select();
        document.execCommand('SelectAll');
        document.execCommand('Copy', false, null);
    }

    static copyToClipboard(text: string) {
        let input = document.createElement('textarea') as HTMLTextAreaElement;
        input.innerHTML = text;
        document.body.appendChild(input);
        UIHelper.copyElementToClipboard(input);
        document.body.removeChild(input);
    }

    static mergePermissions(source: (Ace | ExtendedAce)[], add: Ace[]) {
        const merge = source;
        for (const p2 of add) {
            // do only add new, unique permissions
            if (merge.filter((p1) => Helper.objectEquals(p1, p2)).length === 0) {
                merge.push(p2);
            }
        }
        return merge;
    }

    /**
     * merge two permission sets
     * If a user/group is duplicated, the one with the highest permission will win
     * Consumer < Collaborator < Coordinator
     * @param source
     * @param add
     */
    static mergePermissionsWithHighestPermission(source: Ace[], add: Ace[]) {
        const result = Helper.deepCopyArray(source);
        for (const p2 of add) {
            const map = source.filter(
                (s) =>
                    s.authority.authorityName === p2.authority.authorityName &&
                    s.authority.authorityType === s.authority.authorityType,
            );
            if (map.length === 1) {
                const perm1 = map[0].permissions.filter(
                    (p) => RestConstants.BASIC_PERMISSIONS.indexOf(p) !== -1,
                );
                const perm2 = p2.permissions.filter(
                    (p) => RestConstants.BASIC_PERMISSIONS.indexOf(p) !== -1,
                );
                if (UIHelper.permissionIsGreaterThan(perm2[0], perm1[0])) {
                    result.splice(result.indexOf(map[0]), 1);
                    result.push(p2);
                }
            } else {
                // add new permission to list
                result.push(p2);
            }
        }
        return result;
    }
    static permissionIsGreaterThanOrEqual(p1: string, p2: string) {
        return (
            RestConstants.BASIC_PERMISSIONS.indexOf(p1) >=
            RestConstants.BASIC_PERMISSIONS.indexOf(p2)
        );
    }

    static permissionIsGreaterThan(p1: string, p2: string) {
        return (
            RestConstants.BASIC_PERMISSIONS.indexOf(p1) >
            RestConstants.BASIC_PERMISSIONS.indexOf(p2)
        );
    }

    static isParentElementOfElement(target: HTMLElement, possibleParent: HTMLElement) {
        let e = target;
        while (e != null) {
            if (e === possibleParent) {
                return true;
            }
            e = e.parentElement;
        }
        return false;
    }
}
