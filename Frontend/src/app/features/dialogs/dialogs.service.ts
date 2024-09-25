import { Injectable } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DialogButton, RestConnectorService, UIConstants } from '../../core-module/core.module';
import { Closable } from './card-dialog/card-dialog-config';
import { CardDialogRef } from './card-dialog/card-dialog-ref';
import { CardDialogUtilsService } from './card-dialog/card-dialog-utils.service';
import { CardDialogService } from './card-dialog/card-dialog.service';
import {
    AddFolderDialogData,
    AddFolderDialogResult,
} from './dialog-modules/add-folder-dialog/add-folder-dialog-data';
import {
    AddMaterialDialogData,
    AddMaterialDialogResult,
} from './dialog-modules/add-material-dialog/add-material-dialog-data';
import {
    AddWithConnectorDialogData,
    AddWithConnectorDialogResult,
} from './dialog-modules/add-with-connector-dialog/add-with-connector-dialog-data';
import {
    ContributorEditDialogData,
    ContributorEditDialogResult,
} from './dialog-modules/contributor-edit-dialog/contributor-edit-dialog-data';
import {
    ContributorsDialogData,
    ContributorsDialogResult,
} from './dialog-modules/contributors-dialog/contributors-dialog-data';
import {
    CreateMapLinkDialogData,
    CreateMapLinkDialogResult,
} from './dialog-modules/create-map-link-dialog/create-map-link-dialog-data';
import {
    CreateVariantDialogData,
    CreateVariantDialogResult,
} from './dialog-modules/create-variant-dialog/create-variant-dialog-data';
import {
    DeleteNodesDialogData,
    DeleteNodesDialogResult,
} from './dialog-modules/delete-nodes-dialog/delete-nodes-dialog-data';
import {
    FileChooserDialogData,
    FileChooserDialogResult,
} from './dialog-modules/file-chooser-dialog/file-chooser-dialog-data';
import {
    FileUploadProgressDialogData,
    FileUploadProgressDialogResult,
} from './dialog-modules/file-upload-progress-dialog/file-upload-progress-dialog-data';
import {
    GenericDialogButton,
    GenericDialogConfig,
    GenericDialogData,
} from './dialog-modules/generic-dialog/generic-dialog-data';
import {
    InputDialogConfig,
    InputDialogData,
    InputDialogResult,
} from './dialog-modules/input-dialog/input-dialog-data';
import {
    JoinGroupDialogData,
    JoinGroupDialogResult,
} from './dialog-modules/join-group-dialog/join-group-dialog-data';
import {
    LicenseAgreementDialogData,
    LicenseAgreementDialogResult,
} from './dialog-modules/license-agreement-dialog/license-agreement-dialog-data';
import {
    LicenseDialogData,
    LicenseDialogResult,
} from './dialog-modules/license-dialog/license-dialog-data';
import {
    MdsEditorDialogData,
    MdsEditorDialogDataGraphql,
    MdsEditorDialogDataNodes,
    MdsEditorDialogDataValues,
    MdsEditorDialogResultNodes,
    MdsEditorDialogResultValues,
} from './dialog-modules/mds-editor-dialog/mds-editor-dialog-data';
import { NodeEmbedDialogData } from './dialog-modules/node-embed-dialog/node-embed-dialog.component';
import { NodeInfoDialogData } from './dialog-modules/node-info-dialog/node-info-dialog.component';
import {
    NodeRelationsDialogData,
    NodeRelationsDialogResult,
} from './dialog-modules/node-relations-dialog/node-relations-dialog-data';
import { NodeReportDialogData } from './dialog-modules/node-report-dialog/node-report-dialog.component';
import {
    NodeTemplateDialogData,
    NodeTemplateDialogResult,
} from './dialog-modules/node-template-dialog/node-template-dialog-data';
import {
    PinnedCollectionsDialogData,
    PinnedCollectionsDialogResult,
} from './dialog-modules/pinned-collections-dialog/pinned-collections-dialog-data';
import { QrDialogData } from './dialog-modules/qr-dialog/qr-dialog.component';
import {
    SaveSearchDialogData,
    SaveSearchDialogResult,
} from './dialog-modules/save-search-dialog/save-search-dialog-data';
import {
    SavedSearchesDialogData,
    SavedSearchesDialogResult,
} from './dialog-modules/saved-searches-dialog/saved-searches-dialog-data';
import {
    SendFeedbackDialogData,
    SendFeedbackDialogResult,
} from './dialog-modules/send-feedback-dialog/send-feedback-dialog-data';
import {
    ShareDialogData,
    ShareDialogResult,
} from './dialog-modules/share-dialog/share-dialog-data';
import {
    ShareHistoryDialogData,
    ShareHistoryDialogResult,
} from './dialog-modules/share-history-dialog/share-history-dialog-data';
import {
    ShareLinkDialogData,
    ShareLinkDialogResult,
} from './dialog-modules/share-link-dialog/share-link-dialog-data';
import {
    SimpleEditDialogData,
    SimpleEditDialogResult,
} from './dialog-modules/simple-edit-dialog/simple-edit-dialog-data';
import {
    WorkflowDialogData,
    WorkflowDialogResult,
} from './dialog-modules/workflow-dialog/workflow-dialog-data';
import {
    XmlAppPropertiesDialogData,
    XmlAppPropertiesDialogResult,
} from './dialog-modules/xml-app-properties-dialog/xml-app-properties-dialog-data';
import { NotificationDialogComponent } from '../../main/navigation/top-bar/notification-dialog/notification-dialog.component';
import { CardComponent } from '../../shared/components/card/card.component';
import { Node } from 'ngx-edu-sharing-api';
import { DropSource, DropTarget, NodeRoot, NodeTitlePipe } from 'ngx-edu-sharing-ui';

@Injectable({
    providedIn: 'root',
})
export class DialogsService {
    get openDialogs() {
        return this.cardDialog.openDialogs;
    }

    constructor(
        private cardDialog: CardDialogService,
        private cardDialogUtils: CardDialogUtilsService,
        private translate: TranslateService,
        // TODO: Move the methods we use of `RestConnectorService` to a utils function if possible.
        private restConnector: RestConnectorService,
    ) {}

    async openGenericDialog<R extends string>(
        config: GenericDialogConfig<R>,
    ): Promise<CardDialogRef<GenericDialogData<R>, R>> {
        const {
            title,
            subtitle,
            avatar,
            nodes,
            minWidth,
            maxWidth,
            customHeaderBarContent,
            closable,
            ...data
        } = {
            ...new GenericDialogConfig<R>(),
            ...config,
        };
        const { GenericDialogComponent } = await import(
            './dialog-modules/generic-dialog/generic-dialog.module'
        );
        return this.cardDialog.open(GenericDialogComponent, {
            title,
            subtitle,
            avatar,
            ...(nodes ? await this.cardDialogUtils.configForNodes(nodes) : {}),
            minWidth,
            maxWidth,
            customHeaderBarContent,
            closable,
            data,
        });
    }

    async openInputDialog(
        config: InputDialogConfig,
    ): Promise<CardDialogRef<InputDialogData, InputDialogResult>> {
        const { title, subtitle, avatar, ...data } = {
            ...new InputDialogConfig(),
            ...config,
        };
        const { InputDialogComponent } = await import(
            './dialog-modules/input-dialog/input-dialog.module'
        );
        return this.cardDialog.open(InputDialogComponent, {
            title,
            subtitle,
            avatar,
            width: 600,
            closable: Closable.Standard,
            data,
        });
    }

    async openQrDialog(data: QrDialogData): Promise<CardDialogRef<QrDialogData, void>> {
        const { QrDialogComponent } = await import('./dialog-modules/qr-dialog/qr-dialog.module');
        return this.cardDialog.open(QrDialogComponent, {
            title: 'OPTIONS.QR_CODE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            contentPadding: 0,
            data,
        });
    }

    async openNodeEmbedDialog(
        data: NodeEmbedDialogData,
    ): Promise<CardDialogRef<NodeEmbedDialogData, void>> {
        const { NodeEmbedDialogComponent } = await import(
            './dialog-modules/node-embed-dialog/node-embed-dialog.module'
        );
        return this.cardDialog.open(NodeEmbedDialogComponent, {
            title: 'OPTIONS.EMBED',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            // Set size via NodeEmbedDialogComponent, so it can choose fitting values for its
            // responsive layouts.
            contentPadding: 0,
            data,
        });
    }

    async openNodeReportDialog(
        data: NodeReportDialogData,
    ): Promise<CardDialogRef<NodeReportDialogData, void>> {
        const { NodeReportDialogComponent } = await import(
            './dialog-modules/node-report-dialog/node-report-dialog.module'
        );
        return this.cardDialog.open(NodeReportDialogComponent, {
            title: 'NODE_REPORT.TITLE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            data,
        });
    }

    async openNodeInfoDialog(
        data: NodeInfoDialogData,
    ): Promise<CardDialogRef<NodeInfoDialogData, void>> {
        const { NodeInfoComponent } = await import(
            './dialog-modules/node-info-dialog/node-info-dialog.module'
        );
        return this.cardDialog.open(NodeInfoComponent, {
            // Header will be configured by the component
            data,
        });
    }

    async openNodeStoreDialog(): Promise<CardDialogRef<void, void>> {
        const { SearchNodeStoreComponent } = await import(
            './dialog-modules/node-store-dialog/node-store-dialog.module'
        );
        return this.cardDialog.open(SearchNodeStoreComponent, {
            title: 'SEARCH.NODE_STORE.TITLE',
            width: 400,
            minHeight: 600,
            contentPadding: 0,
        });
    }

    async openLicenseAgreementDialog(
        data: LicenseAgreementDialogData,
    ): Promise<CardDialogRef<LicenseAgreementDialogData, LicenseAgreementDialogResult>> {
        const { LicenseAgreementDialogComponent } = await import(
            './dialog-modules/license-agreement-dialog/license-agreement-dialog.module'
        );
        return this.cardDialog.open(LicenseAgreementDialogComponent, {
            title: 'LICENSE_AGREEMENT.TITLE',
            closable: Closable.Disabled,
            width: 900,
            data,
        });
    }

    async openAccessibilityDialog(): Promise<CardDialogRef<void, void>> {
        const { AccessibilityDialogComponent } = await import(
            './dialog-modules/accessibility-dialog/accessibility-dialog.module'
        );
        return this.cardDialog.open(AccessibilityDialogComponent, {
            title: 'ACCESSIBILITY.TITLE',
            subtitle: 'ACCESSIBILITY.SUBTITLE',
            avatar: { kind: 'icon', icon: 'accessibility' },
            width: 700,
        });
    }

    async openNotificationDialog(): Promise<CardDialogRef<void, void>> {
        const { NotificationDialogComponent } = await import(
            '../../main/navigation/top-bar/notification-dialog/notification-dialog.module'
        );
        return this.cardDialog.open(NotificationDialogComponent, {
            title: 'NOTIFICATION.TITLE',
            subtitle: 'NOTIFICATION.SUBTITLE',
            avatar: { kind: 'icon', icon: 'notifications' },
            width: 700,
        });
    }

    async openFileChooserDialog(
        data: Partial<FileChooserDialogData>,
    ): Promise<CardDialogRef<FileChooserDialogData, FileChooserDialogResult>> {
        const { FileChooserDialogComponent } = await import(
            './dialog-modules/file-chooser-dialog/file-chooser-dialog.module'
        );
        return this.cardDialog.open(FileChooserDialogComponent, {
            contentPadding: 0,
            height: 800,
            data: { ...new FileChooserDialogData(), ...(data ?? {}) },
        });
    }

    async openAddFolderDialog(
        data: AddFolderDialogData,
    ): Promise<CardDialogRef<AddFolderDialogData, AddFolderDialogResult>> {
        const { AddFolderDialogComponent } = await import(
            './dialog-modules/add-folder-dialog/add-folder-dialog.module'
        );
        return this.cardDialog.open(AddFolderDialogComponent, {
            title: 'WORKSPACE.ADD_FOLDER_TITLE',
            ...(await this.cardDialogUtils.configForNode(data.parent)),
            avatar: { kind: 'image', url: this.restConnector.getThemeMimeIconSvg('folder.svg') },
            width: 600,
            data,
        });
    }

    async openXmlAppPropertiesDialog(
        data: XmlAppPropertiesDialogData,
    ): Promise<CardDialogRef<XmlAppPropertiesDialogData, XmlAppPropertiesDialogResult>> {
        const title = await this.translate
            .get('ADMIN.APPLICATIONS.EDIT_APP', { xml: data.appXml })
            .toPromise();
        const { XmlAppPropertiesDialogComponent } = await import(
            './dialog-modules/xml-app-properties-dialog/xml-app-properties-dialog.module'
        );
        return this.cardDialog.open(XmlAppPropertiesDialogComponent, {
            title,
            width: 600,
            data,
        });
    }

    async openContributorEditDialog(
        data: ContributorEditDialogData,
    ): Promise<CardDialogRef<ContributorEditDialogData, ContributorEditDialogResult>> {
        const title = await this.translate
            .get('WORKSPACE.CONTRIBUTOR.' + (data.vCard ? 'EDIT' : 'ADD') + '_TITLE')
            .toPromise();
        const { ContributorEditDialogComponent } = await import(
            './dialog-modules/contributor-edit-dialog/contributor-edit-dialog.module'
        );
        return this.cardDialog.open(ContributorEditDialogComponent, {
            title,
            contentPadding: 0,
            width: 600,
            height: 900,
            closable: Closable.Standard,
            data,
        });
    }

    async openContributorsDialog(
        data: ContributorsDialogData,
    ): Promise<CardDialogRef<ContributorsDialogData, ContributorsDialogResult>> {
        const { ContributorsDialogComponent } = await import(
            './dialog-modules/contributors-dialog/contributors-dialog.module'
        );
        return this.cardDialog.open(ContributorsDialogComponent, {
            title: 'WORKSPACE.CONTRIBUTOR.TITLE',
            width: 500,
            height: 700,
            data,
        });
    }

    async openLicenseDialog(
        data: LicenseDialogData,
    ): Promise<CardDialogRef<LicenseDialogData, LicenseDialogResult>> {
        const { LicenseDialogComponent } = await import(
            './dialog-modules/license-dialog/license-dialog.module'
        );
        return this.cardDialog.open(LicenseDialogComponent, {
            title: 'WORKSPACE.LICENSE.TITLE',
            ...(data.kind === 'nodes' ? await this.cardDialogUtils.configForNodes(data.nodes) : {}),
            width: 700,
            height: 1100,
            closable: Closable.Standard,
            data,
        });
    }

    async openShareDialog(
        data: ShareDialogData,
    ): Promise<CardDialogRef<ShareDialogData, ShareDialogResult>> {
        const { ShareDialogComponent } = await import(
            './dialog-modules/share-dialog/share-dialog.module'
        );
        return this.cardDialog.open(ShareDialogComponent, {
            title: 'WORKSPACE.SHARE.TITLE',
            contentPadding: 0,
            width: 900,
            height: 800,
            closable: Closable.Standard,
            data: { ...new ShareDialogData(), ...data },
        });
    }

    async openShareHistoryDialog(
        data: ShareHistoryDialogData,
    ): Promise<CardDialogRef<ShareHistoryDialogData, ShareHistoryDialogResult>> {
        const { ShareHistoryDialogComponent } = await import(
            './dialog-modules/share-history-dialog/share-history-dialog.module'
        );
        return this.cardDialog.open(ShareHistoryDialogComponent, {
            title: 'WORKSPACE.SHARE.HISTORY.TITLE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            contentPadding: 0,
            minWidth: 500,
            minHeight: 300,
            data,
        });
    }

    async openShareLinkDialog(
        data: ShareLinkDialogData,
    ): Promise<CardDialogRef<ShareLinkDialogData, ShareLinkDialogResult>> {
        const { ShareLinkDialogComponent } = await import(
            './dialog-modules/share-link-dialog/share-link-dialog.module'
        );
        return this.cardDialog.open(ShareLinkDialogComponent, {
            title: 'WORKSPACE.SHARE_LINK.TITLE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            width: 500,
            height: 700,
            data,
        });
    }

    async openCreateMapLinkDialog(
        data: CreateMapLinkDialogData,
    ): Promise<CardDialogRef<CreateMapLinkDialogData, CreateMapLinkDialogResult>> {
        const { CreateMapLinkDialogComponent } = await import(
            './dialog-modules/create-map-link-dialog/create-map-link-dialog.module'
        );
        return this.cardDialog.open(CreateMapLinkDialogComponent, {
            title: 'MAP_LINK.TITLE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            closable: Closable.Standard,
            width: 600,
            data,
        });
    }

    async openNodeRelationsDialog(
        data: NodeRelationsDialogData,
    ): Promise<CardDialogRef<NodeRelationsDialogData, NodeRelationsDialogResult>> {
        const { NodeRelationsDialogComponent } = await import(
            './dialog-modules/node-relations-dialog/node-relations-dialog.module'
        );
        return this.cardDialog.open(NodeRelationsDialogComponent, {
            title: 'NODE_RELATIONS.TITLE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            closable: Closable.Standard,
            width: 700,
            minHeight: 700,
            data,
        });
    }

    async openNodeTemplateDialog(
        data: NodeTemplateDialogData,
    ): Promise<CardDialogRef<NodeTemplateDialogData, NodeTemplateDialogResult>> {
        const { NodeTemplateDialogComponent } = await import(
            './dialog-modules/node-template-dialog/node-template-dialog.module'
        );
        return this.cardDialog.open(NodeTemplateDialogComponent, {
            title: 'OPTIONS.TEMPLATE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            width: 600,
            minHeight: 700,
            data,
            closable: Closable.Standard,
        });
    }

    async openMdsEditorDialogForNodes(
        data: MdsEditorDialogDataNodes,
        mode: 'graphql' | 'rest' = 'graphql',
    ): Promise<CardDialogRef<MdsEditorDialogData, MdsEditorDialogResultNodes>> {
        const { MdsEditorDialogComponent } = await import(
            './dialog-modules/mds-editor-dialog/mds-editor-dialog.module'
        );
        if (mode === 'graphql') {
            data = {
                graphqlIds: data.nodes.map((n) => n.ref.id),
                ...data,
            } as MdsEditorDialogDataGraphql;
        }
        return this.cardDialog.open(MdsEditorDialogComponent, {
            title: 'MDS.TITLE',
            ...(await this.cardDialogUtils.configForNodes(data.nodes)),
            minWidth: 600,
            maxWidth: 900,
            minHeight: 700,
            contentPadding: 0,
            data,
        });
    }

    async openMdsEditorDialogForValues(
        data: MdsEditorDialogDataValues,
    ): Promise<CardDialogRef<MdsEditorDialogDataValues, MdsEditorDialogResultValues>> {
        const { MdsEditorDialogComponent } = await import(
            './dialog-modules/mds-editor-dialog/mds-editor-dialog.module'
        );
        data = { ...new MdsEditorDialogDataValues(), ...data };
        return this.cardDialog.open(MdsEditorDialogComponent, {
            title: 'MDS.TITLE',
            minWidth: 600,
            maxWidth: 900,
            minHeight: 700,
            contentPadding: 0,
            data,
        });
    }

    async openSimpleEditDialog(
        data: SimpleEditDialogData,
    ): Promise<CardDialogRef<SimpleEditDialogData, SimpleEditDialogResult>> {
        const { SimpleEditDialogComponent } = await import(
            './dialog-modules/simple-edit-dialog/simple-edit-dialog.module'
        );
        return this.cardDialog.open(SimpleEditDialogComponent, {
            ...(await this.cardDialogUtils.configForNodes(data.nodes)),
            title: 'SIMPLE_EDIT.TITLE',
            // minHeight: 700,
            width: 600,
            data,
            closable: Closable.Standard,
        });
    }

    async openSendFeedbackDialog(
        data: SendFeedbackDialogData,
    ): Promise<CardDialogRef<SendFeedbackDialogData, SendFeedbackDialogResult>> {
        const { SendFeedbackDialogComponent } = await import(
            './dialog-modules/send-feedback-dialog/send-feedback-dialog.module'
        );
        return this.cardDialog.open(SendFeedbackDialogComponent, {
            title: 'FEEDBACK.TITLE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            minWidth: 500,
            data,
            closable: Closable.Standard,
        });
    }

    async openThirdPartyLicensesDialog(): Promise<CardDialogRef<void, void>> {
        const { ThirdPartyLicensesDialogComponent } = await import(
            './dialog-modules/third-party-licenses-dialog/third-party-licenses-dialog.module'
        );
        return this.cardDialog.open(ThirdPartyLicensesDialogComponent, {
            title: 'LICENSE_INFORMATION',
            avatar: { kind: 'icon', icon: 'copyright' },
            minWidth: 800,
            minHeight: 800,
        });
    }

    async openSaveSearchDialog(
        data: SaveSearchDialogData,
    ): Promise<CardDialogRef<SaveSearchDialogData, SaveSearchDialogResult>> {
        const { SaveSearchDialogComponent } = await import(
            './dialog-modules/save-search-dialog/save-search-dialog.module'
        );
        return this.cardDialog.open(SaveSearchDialogComponent, {
            title: 'SEARCH.SAVE_SEARCH.TITLE',
            avatar: { kind: 'icon', icon: 'search' },
            width: 600,
            data,
            closable: Closable.Standard,
        });
    }

    async openSavedSearchesDialog(
        data: SavedSearchesDialogData,
    ): Promise<CardDialogRef<SavedSearchesDialogData, SavedSearchesDialogResult>> {
        const { SavedSearchesDialogComponent } = await import(
            './dialog-modules/saved-searches-dialog/saved-searches-dialog.module'
        );
        return this.cardDialog.open(SavedSearchesDialogComponent, {
            title: 'SEARCH.SAVED_SEARCHES.TITLE',
            avatar: { kind: 'icon', icon: 'search' },
            contentPadding: 0,
            minHeight: 500,
            width: 600,
            data,
            closable: Closable.Casual,
        });
    }

    async openDeleteNodesDialog(
        data: DeleteNodesDialogData,
    ): Promise<CardDialogRef<DeleteNodesDialogData, DeleteNodesDialogResult>> {
        const { DeleteNodesDialogComponent } = await import(
            './dialog-modules/delete-nodes-dialog/delete-nodes-dialog.module'
        );
        return this.cardDialog.open(DeleteNodesDialogComponent, {
            ...(await this.cardDialogUtils.configForNodes(data.nodes)),
            minHeight: 240,
            width: 500,
            data,
            closable: Closable.Casual,
        });
    }

    async openAddMaterialDialog(
        data: AddMaterialDialogData,
    ): Promise<CardDialogRef<AddMaterialDialogData, AddMaterialDialogResult>> {
        const { AddMaterialDialogComponent } = await import(
            './dialog-modules/add-material-dialog/add-material-dialog.module'
        );
        return this.cardDialog.open(AddMaterialDialogComponent, {
            title: 'WORKSPACE.ADD_OBJECT_TITLE',
            subtitle: 'WORKSPACE.ADD_OBJECT_SUBTITLE',
            avatar: { kind: 'icon', icon: 'cloud_upload' },
            minHeight: 700,
            width: 600,
            data,
            autoFocus: '[autofocus]',
            closable: Closable.Casual,
        });
    }

    async openAddWithConnectorDialog(
        data: AddWithConnectorDialogData,
    ): Promise<CardDialogRef<AddWithConnectorDialogData, AddWithConnectorDialogResult>> {
        const { AddWithConnectorDialogComponent } = await import(
            './dialog-modules/add-with-connector-dialog/add-with-connector-dialog.module'
        );
        return this.cardDialog.open(AddWithConnectorDialogComponent, {
            width: 600,
            data,
            closable: Closable.Casual,
        });
    }

    async openFileUploadProgressDialog(
        data: FileUploadProgressDialogData,
    ): Promise<CardDialogRef<FileUploadProgressDialogData, FileUploadProgressDialogResult>> {
        const { FileUploadProgressDialogComponent } = await import(
            './dialog-modules/file-upload-progress-dialog/file-upload-progress-dialog.module'
        );
        return this.cardDialog.open(FileUploadProgressDialogComponent, {
            title: 'WORKSPACE.UPLOAD_TITLE',
            avatar: { kind: 'icon', icon: 'cloud_upload' },
            width: 700,
            data,
            closable: Closable.Disabled,
        });
    }

    async openCreateVariantDialog(
        data: CreateVariantDialogData,
    ): Promise<CardDialogRef<CreateVariantDialogData, CreateVariantDialogResult>> {
        const { CreateVariantDialogComponent } = await import(
            './dialog-modules/create-variant-dialog/create-variant-dialog.module'
        );
        return this.cardDialog.open(CreateVariantDialogComponent, {
            title: 'NODE_VARIANT.TITLE',
            ...(await this.cardDialogUtils.configForNode(data.node)),
            width: 600,
            data,
            closable: Closable.Casual,
        });
    }

    async openJoinGroupDialog(): Promise<
        CardDialogRef<JoinGroupDialogData, JoinGroupDialogResult>
    > {
        const { JoinGroupDialogComponent } = await import(
            './dialog-modules/join-group-dialog/join-group-dialog.module'
        );
        return this.cardDialog.open(JoinGroupDialogComponent, {
            title: 'SIGNUP_GROUP.TITLE',
            subtitle: 'SIGNUP_GROUP.SUBTITLE',
            avatar: { kind: 'icon', icon: 'group_add' },
            width: 500,
            minHeight: 600,
            closable: Closable.Casual,
        });
    }

    async openPinnedCollectionsDialog(
        data: PinnedCollectionsDialogData,
    ): Promise<CardDialogRef<PinnedCollectionsDialogData, PinnedCollectionsDialogResult>> {
        const { PinnedCollectionsDialogComponent } = await import(
            './dialog-modules/pinned-collections-dialog/pinned-collections-dialog.module'
        );
        return this.cardDialog.open(PinnedCollectionsDialogComponent, {
            title: 'COLLECTIONS.PINNING.TITLE',
            avatar: { kind: 'icon', icon: 'edu-pin' },
            width: 400,
            minHeight: 400,
            contentPadding: 0,
            data,
            closable: Closable.Standard,
        });
    }

    async openWorkflowDialog(
        data: WorkflowDialogData,
    ): Promise<CardDialogRef<WorkflowDialogData, WorkflowDialogResult>> {
        const { WorkflowDialogComponent } = await import(
            './dialog-modules/workflow-dialog/workflow-dialog.module'
        );
        return this.cardDialog.open(WorkflowDialogComponent, {
            title: 'WORKSPACE.WORKFLOW.TITLE',
            ...(await this.cardDialogUtils.configForNodes(data.nodes)),
            width: 700,
            // is handled in component
            autoFocus: false,
            data,
            closable: Closable.Standard,
        });
    }

    async openCopyMoveDialog(
        oldParent: Node | NodeRoot,
        source: DropSource<Node>,
        target: DropTarget,
        allowedModes = ['copy', 'move'],
    ) {
        let buttons: GenericDialogButton<string>[];
        if (source.mode === 'move') {
            buttons = [];
            if (allowedModes.includes('copy')) {
                buttons.push({
                    config: DialogButton.TYPE_CANCEL,
                    label: 'WORKSPACE.COPY_MOVE.COPY',
                    callback: async (ref) => {
                        return true;
                    },
                });
            }
            if (allowedModes.includes('move')) {
                buttons.push({
                    config: DialogButton.TYPE_PRIMARY,
                    label: 'WORKSPACE.COPY_MOVE.MOVE',
                    callback: async (ref) => {
                        ref.close('move');
                        return true;
                    },
                });
            }
        } else {
            buttons = [];
            if (allowedModes.includes('move')) {
                buttons.push({
                    config: DialogButton.TYPE_CANCEL,
                    label: 'WORKSPACE.COPY_MOVE.MOVE',
                    callback: async (ref) => {
                        ref.close('move');
                        return true;
                    },
                });
            }
            if (allowedModes.includes('copy')) {
                buttons.push({
                    config: DialogButton.TYPE_PRIMARY,
                    label: 'WORKSPACE.COPY_MOVE.COPY',
                    callback: async (ref) => {
                        return true;
                    },
                });
            }
        }
        const nodeTitle = new NodeTitlePipe(this.translate);
        return await this.openGenericDialog({
            buttons,
            nodes: source.element?.slice(),
            title: 'WORKSPACE.COPY_MOVE.TITLE',
            message:
                'WORKSPACE.COPY_MOVE.MESSAGE' +
                (source.element?.length === 1 ? '_SINGLE' : '_MULTIPLE'),
            messageParameters: {
                count: source.element?.length.toString(),
                element: nodeTitle.transform(source.element[0]),
                source: nodeTitle.transform(oldParent),
                target: nodeTitle.transform(target),
            },
        });
    }
}
