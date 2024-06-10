import { trigger } from '@angular/animations';
import {
    Component,
    EventEmitter,
    Input,
    OnDestroy,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ConnectorService, LtiPlatformService, Tool, Tools } from 'ngx-edu-sharing-api';
import {
    Constrain,
    DateHelper,
    DefaultGroups,
    DropdownComponent,
    ElementType,
    OptionItem,
    OptionsHelperDataService,
    Scope,
    Target,
    UIAnimation,
} from 'ngx-edu-sharing-ui';
import { Observable, Subject } from 'rxjs';
import { delay, takeUntil } from 'rxjs/operators';
import { BridgeService } from '../../../services/bridge.service';
import {
    Connector,
    Filetype,
    FrameEventsService,
    Node,
    NodeWrapper,
    RestConnectorService,
    RestConnectorsService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    UIConstants,
} from '../../../core-module/core.module';
import { Helper } from '../../../core-module/rest/helper';
import { CardService } from '../../../services/card.service';
import { NodeHelperService } from '../../../services/node-helper.service';
import { Toast } from '../../../services/toast';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import { AddFolderDialogResult } from '../../../features/dialogs/dialog-modules/add-folder-dialog/add-folder-dialog-data';
import { AddWithConnectorDialogResult } from '../../../features/dialogs/dialog-modules/add-with-connector-dialog/add-with-connector-dialog-data';
import { OK } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { DialogsService } from '../../../features/dialogs/dialogs.service';
import { PasteService } from '../../../services/paste.service';
import { UploadDialogService } from '../../../services/upload-dialog.service';
import { CardComponent } from '../../../shared/components/card/card.component';
import { MainNavConfig, MainNavService } from '../main-nav.service';

@Component({
    selector: 'es-create-menu',
    templateUrl: 'create-menu.component.html',
    styleUrls: ['create-menu.component.scss'],
    animations: [trigger('dialog', UIAnimation.switchDialog(UIAnimation.ANIMATION_TIME_FAST))],
    providers: [OptionsHelperDataService],
})
export class CreateMenuComponent implements OnInit, OnDestroy {
    @ViewChild('dropdown', { static: true }) dropdown: DropdownComponent;

    /**
     * Currently allowed to drop files?
     */
    @Input() allowed = true;
    /**
     * Allow upload of binary files
     */
    @Input() allowBinary = true;
    @Input() scope: string;
    private mainNavConfig: MainNavConfig;

    /**
     * Parent location. If null, the folder picker will be shown
     */
    @Input() set parent(parent: Node) {
        this._parent = parent;
        this.showPicker = parent == null || this.nodeHelper.isNodeCollection(parent);
        this.updateOptions();
    }

    /**
     * can a folder be created
     */
    @Input() folder = true;

    /**
     * Fired when elements are created or uploaded
     */
    @Output() onCreate = new EventEmitter<Node[]>();

    _parent: Node = null;

    connectorList: Connector[];
    fileIsOver = false;
    cardHasOpenModals$: Observable<boolean>;
    options: OptionItem[];
    tools: Tools;
    createToolType: Tool;

    showPicker: boolean; // keep public - used by extensions
    private params: Params;
    private destroyed = new Subject<void>();

    constructor(
        public bridge: BridgeService,
        private cardService: CardService,
        private connector: RestConnectorService,
        private connectorApi: ConnectorService,
        private mainNavService: MainNavService,
        private connectors: RestConnectorsService,
        private dialogs: DialogsService,
        private event: FrameEventsService,
        private iam: RestIamService,
        private iamService: RestIamService,
        private ltiPlatformService: LtiPlatformService, //private paste: PasteService,
        private nodeHelper: NodeHelperService,
        private nodeService: RestNodeService,
        private optionsService: OptionsHelperDataService,
        private paste: PasteService,
        private route: ActivatedRoute,
        private router: Router,
        private toast: Toast,
        private translate: TranslateService,
        private uploadDialog: UploadDialogService,
    ) {
        this.route.queryParams.subscribe((params) => {
            this.params = params;
            this.updateOptions();
        });
        this.connectorApi
            .observeConnectorList()
            .pipe(takeUntil(this.destroyed))
            .subscribe((list) => {
                this.connectorList = this.connectors.filterConnectors(list?.connectors);
                this.updateOptions();
            });
        this.connector.isLoggedIn(false).subscribe((login) => {
            if (login.statusCode === RestConstants.STATUS_CODE_OK) {
            }
        });
        this.cardHasOpenModals$ = this.cardService.hasOpenModals.pipe(delay(0));
        this.mainNavService
            .observeMainNavConfig()
            .pipe(takeUntil(this.destroyed))
            .subscribe((config) => {
                this.mainNavConfig = config;
                this.updateOptions();
            });
        this.ltiPlatformService.getTools().subscribe((t) => {
            this.tools = t;
            this.updateOptions();
        });
    }

    ngOnInit(): void {
        this.optionsService.virtualNodesAdded
            .pipe(takeUntil(this.destroyed))
            .subscribe((nodes) => this.onCreate.emit(nodes));
        this.paste
            .observeUrlPasteOnPage()
            .pipe(takeUntil(this.destroyed))
            .subscribe((url) => this.onUrlPasteOnPage(url));
        this.paste
            .observeNonTextPageOnPage()
            .pipe(takeUntil(this.destroyed))
            .subscribe(() => this.toast.error(null, 'CLIPBOARD_DATA_UNSUPPORTED'));
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private async onUrlPasteOnPage(url: string) {
        if (!this.allowed || !this.allowBinary) {
            return;
        }
        if (CardComponent.getNumberOfOpenCards() > 0) {
            return;
        }
        const nodes = await this.uploadDialog.createLinkNode({
            link: url,
            parent: await this.getParent(),
        });
        if (nodes) {
            this.onCreate.emit(nodes);
        }
    }

    async updateOptions() {
        this.options = [];
        if (this.allowBinary && this.folder) {
            const pasteNodes = new OptionItem('OPTIONS.PASTE', 'content_paste', (node) =>
                this.optionsService.pasteNode(),
            );
            pasteNodes.elementType = [ElementType.Unknown];
            pasteNodes.constrains = [
                Constrain.NoSelection,
                Constrain.ClipboardContent,
                Constrain.AddObjects,
                Constrain.User,
            ];
            pasteNodes.toolpermissions = [
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS,
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
            ];
            pasteNodes.keyboardShortcut = {
                keyCode: 'KeyV',
                modifiers: ['Ctrl/Cmd'],
            };
            pasteNodes.group = DefaultGroups.Primary;
            this.options.push(pasteNodes);
        }
        if (this._parent && this.nodeHelper.isNodeCollection(this._parent)) {
            const newCollection = new OptionItem('OPTIONS.NEW_COLLECTION', 'layers', (node) =>
                UIHelper.goToCollection(this.router, this._parent, 'new'),
            );
            newCollection.elementType = [ElementType.Unknown];
            newCollection.constrains = [Constrain.NoSelection, Constrain.User];
            newCollection.toolpermissions = [
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_COLLECTIONS,
            ];
            newCollection.group = DefaultGroups.Create;
            newCollection.priority = 5;
            this.options.push(newCollection);
        }
        if (this.allowBinary) {
            if (this._parent && this.nodeHelper.isNodeCollection(this._parent)) {
                const search = new OptionItem('OPTIONS.SEARCH_OBJECT', 'redo', () =>
                    this.pickMaterialFromSearch(),
                );
                search.elementType = [ElementType.Unknown];
                search.group = DefaultGroups.Create;
                search.priority = 7.5;
                this.options.push(search);
            }
            const upload = new OptionItem('OPTIONS.ADD_OBJECT', 'cloud_upload', () =>
                this.openUploadSelect(),
            );
            upload.elementType = [ElementType.Unknown];
            upload.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
            upload.group = DefaultGroups.Create;
            upload.priority = 10;
            this.options.push(upload);
            // handle connectors
            if (this.connectorList) {
                this.options = this.options.concat(
                    this.connectorList.map((connector, i) => {
                        const option = new OptionItem(
                            'CONNECTOR.' + connector.id + '.NAME',
                            connector.icon,
                            () => this.showCreateConnector(connector),
                        );
                        option.elementType = [ElementType.Unknown];
                        option.group = DefaultGroups.CreateConnector;
                        option.priority = i;
                        return option;
                    }),
                );
            }
            if (this.mainNavConfig?.customCreateOptions) {
                this.options.push(...this.mainNavConfig?.customCreateOptions);
            }
            // handle app
            if (this.bridge.isRunningCordova()) {
                const camera = new OptionItem('WORKSPACE.ADD_CAMERA', 'camera_alt', () =>
                    this.openCamera(),
                );
                camera.elementType = [ElementType.Unknown];
                camera.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES];
                camera.group = DefaultGroups.Create;
                camera.priority = 20;
                this.options.push(camera);
            }

            if (this.tools?.tools.length > 0) {
                this.options = this.options.concat(
                    this.tools.tools.map((tool, i) => {
                        const option = new OptionItem(tool.name, 'create', () =>
                            this.showCreateLtiTool(tool),
                        );
                        option.elementType = [ElementType.Unknown];
                        option.group = DefaultGroups.CreateLtiTools;
                        option.priority = i;
                        return option;
                    }),
                );
            }
        }
        if (this.folder) {
            const addFolder = new OptionItem('WORKSPACE.ADD_FOLDER', 'create_new_folder', () =>
                this.openAddFolderDialog(),
            );
            addFolder.elementType = [ElementType.Unknown];
            addFolder.toolpermissions = [RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FOLDERS];
            addFolder.group = DefaultGroups.Create;
            addFolder.priority = 30;
            this.options.push(addFolder);
        }
        this.optionsService.setData({
            scope: Scope.CreateMenu,
            parent: this._parent,
        });
        this.options = await this.optionsService.filterOptions(this.options, Target.CreateMenu);

        // If the menu was open, we just removed all its items, leaving focus on <body>.
        setTimeout(() => {
            this.dropdown?.menu.focusFirstItem();
        });
    }

    private async openAddFolderDialog(name?: string) {
        const dialogRef = await this.dialogs.openAddFolderDialog({
            name,
            parent: await this.getParent(),
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                this.addFolder(result);
            }
        });
    }

    async openUploadSelect(): Promise<void> {
        const nodes = await this.uploadDialog.openUploadDialog({
            parent: await this.getParent(),
            chooseParent: this.showPicker,
        });
        if (nodes && Array.isArray(nodes)) {
            this.onCreate.emit(nodes);
        }
    }

    public hasUsableOptions() {
        return this.options.some((o) => o.isEnabled);
    }

    private async getParent() {
        return this._parent && !this.nodeHelper.isNodeCollection(this._parent)
            ? this._parent
            : this.nodeHelper.getDefaultInboxFolder().toPromise();
    }

    async addFolder(folder: AddFolderDialogResult) {
        this.toast.showProgressSpinner();
        const properties = RestHelper.createNameProperty(folder.name);
        if (folder.metadataSet) {
            properties[RestConstants.CM_PROP_METADATASET_EDU_METADATASET] = [folder.metadataSet];
            properties[RestConstants.CM_PROP_METADATASET_EDU_FORCEMETADATASET] = ['true'];
        }
        this.nodeService
            .createNode(
                (await this.getParent()).ref.id,
                RestConstants.CM_TYPE_FOLDER,
                [],
                properties,
            )
            .subscribe(
                (data: NodeWrapper) => {
                    this.toast.closeProgressSpinner();
                    this.onCreate.emit([data.node]);
                    this.toast.toast('WORKSPACE.TOAST.FOLDER_ADDED');
                },
                (error: any) => {
                    this.toast.closeProgressSpinner();
                    if (
                        this.nodeHelper.handleNodeError(folder.name, error) ===
                        RestConstants.DUPLICATE_NODE_RESPONSE
                    ) {
                        void this.openAddFolderDialog(folder.name);
                    }
                },
            );
    }

    uploadFiles(files: FileList) {
        this.onFileDrop(files);
    }

    onFileDrop(files: FileList) {
        if (!this.allowed) {
            this.toast.error(null, 'WORKSPACE.TOAST.NOT_POSSIBLE_GENERAL');
            return;
        }
        if (
            !this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
            )
        ) {
            this.toast.toolpermissionError(RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES);
            return;
        }
        void this.openUpload(files);
    }

    private async openUpload(files: FileList): Promise<void> {
        const nodes = await this.uploadDialog.uploadFilesAndCreateNodes({
            parent: await this.getParent(),
            files,
        });
        if (nodes) {
            if (this.params.reurl) {
                this.nodeHelper.addNodeToLms(nodes[0], this.params.reurl);
            }
            this.onCreate.emit(nodes);
        }
    }

    async showCreateConnector(connector: Connector, name?: string) {
        const user = await this.iamService.getCurrentUserAsync();
        if (
            user.person.quota.enabled &&
            user.person.quota.sizeCurrent >= user.person.quota.sizeQuota
        ) {
            await this.dialogs.openGenericDialog({
                title: 'CONNECTOR_QUOTA_REACHED_TITLE',
                message: 'CONNECTOR_QUOTA_REACHED_MESSAGE',
                buttons: OK,
            });
        } else {
            const dialogRef = await this.dialogs.openAddWithConnectorDialog({ connector, name });
            dialogRef.afterClosed().subscribe((result) => {
                if (result) {
                    void this.createConnector(connector, result);
                }
            });
        }
    }

    async showCreateLtiTool(tool: Tool) {
        this.createToolType = tool;
    }

    private openCamera() {
        this.bridge.getCordova().getPhotoFromCamera(
            async (data: string) => {
                const name =
                    this.translate.instant('SHARE_APP.IMAGE') +
                    ' ' +
                    DateHelper.formatDate(this.translate, new Date().getTime(), {
                        showAlwaysTime: true,
                        useRelativeLabels: false,
                    }) +
                    '.jpg';
                const blob: any = Helper.base64toBlob(data, 'image/jpeg');
                blob.name = name;
                const fakeFileList = [blob] as unknown as FileList;
                fakeFileList.item = (index: number) => blob;
                await this.openUpload(fakeFileList);
            },
            (error: any) => {
                console.warn(error);
                // this.toast.error(error);
            },
        );
    }

    private editConnector(
        node: Node = null,
        type: Filetype = null,
        win: any = null,
        connectorType: Connector = null,
    ) {
        UIHelper.openConnector(
            this.connectors,
            this.iam,
            this.event,
            this.toast,
            node,
            type,
            win,
            connectorType,
        );
    }

    pickMaterialFromSearch() {
        UIHelper.getCommonParameters(this.route).subscribe((params) => {
            params.addToCollection = this._parent.ref.id;
            this.router.navigate([UIConstants.ROUTER_PREFIX + 'search'], {
                queryParams: params,
            });
        });
    }

    private async createConnector(connector: Connector, event: AddWithConnectorDialogResult) {
        const prop = this.nodeHelper.propertiesFromConnector(event);
        let win: any;
        if (!this.bridge.isRunningCordova()) {
            win = window.open('');
        }
        this.nodeService
            .createNode((await this.getParent()).ref.id, RestConstants.CCM_TYPE_IO, [], prop, false)
            .subscribe(
                (data: NodeWrapper) => {
                    this.editConnector(data.node, event.type as Filetype, win, connector);
                    this.onCreate.emit([data.node]);
                },
                (error: any) => {
                    win.close();
                    if (
                        this.nodeHelper.handleNodeError(event.name, error) ===
                        RestConstants.DUPLICATE_NODE_RESPONSE
                    ) {
                        void this.showCreateConnector(connector, event.name);
                    }
                },
            );
    }

    createLtiTool(event: any) {
        if (this.createToolType.customContentOption) {
            this.createLtiContentOptionNode(event.name);
            return;
        } else {
            if (!event.nodes) {
                return;
            }
            this.afterCreateLtiTool(event.nodes, null, event.name);
        }
    }

    createLtiContentOptionNode(name: string) {
        // @TODO cordova handling
        //fix popup problem
        let w = window.open('');
        if (name == undefined) {
            return;
        }
        const properties = RestHelper.createNameProperty(name);
        this.getParent().then((parent) => {
            this.nodeService
                .createNode(parent.ref.id, RestConstants.CCM_TYPE_IO, [], properties)
                .subscribe(
                    (data: NodeWrapper) => {
                        this.ltiPlatformService
                            .convertToLtiResourceLink(data.node.ref.id, this.createToolType.appId)
                            .subscribe(
                                (result: any) => {
                                    let nodesArr: Node[] = [];
                                    nodesArr.push(data.node);
                                    this.afterCreateLtiTool(nodesArr, w, name);
                                },
                                (error: any) => {
                                    this.nodeHelper.handleNodeError(name, error);
                                    w.close();
                                },
                            );
                    },
                    (error: any) => {
                        this.nodeHelper.handleNodeError(name, error);
                        w.close();
                    },
                );
        });
    }

    afterCreateLtiTool(nodes: Node[], w: Window, name: string) {
        if (nodes) {
            nodes.forEach((n) => {
                if (this.createToolType.customContentOption == true) {
                    UIHelper.openLTIResourceLink(w, n);

                    this.onCreate.emit([n]);
                    this.createToolType = null;
                } else {
                    const prop = RestHelper.createNameProperty(n.name);
                    this.nodeService.editNodeMetadata(n.ref.id, prop).subscribe(
                        (data: NodeWrapper) => {
                            this.onCreate.emit([data.node]);
                            this.createToolType = null;
                        },
                        (error: any) => {
                            if (
                                this.nodeHelper.handleNodeError(n.name, error) ===
                                RestConstants.DUPLICATE_NODE_RESPONSE
                            ) {
                                // this.createConnectorName = name;
                            }
                        },
                    );
                }
            });
        }
    }

    cancelLtiTool(event: any) {
        let nodes: Node[] = event.nodes;
        if (nodes) {
            nodes.forEach((n) => {
                this.nodeService.deleteNode(n.ref.id, false).subscribe(
                    (data: NodeWrapper) => {},
                    (error) => {
                        this.nodeHelper.handleNodeError(n.name, error);
                    },
                );
            });
        }
        this.createToolType = null;
    }

    isAllowed() {
        return (
            this.allowed &&
            this.connector.hasToolPermissionInstant(
                RestConstants.TOOLPERMISSION_CREATE_ELEMENTS_FILES,
            )
        );
    }
}
