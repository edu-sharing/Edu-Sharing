import { Injectable } from '@angular/core';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { LocalEventsService } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { ConfigurationService, RestConstants, RestNodeService } from '../core-module/core.module';
import { Toast } from './toast';
import { VirtualNode } from 'ngx-edu-sharing-ui';
import {
    FileData,
    LinkData,
} from '../features/dialogs/dialog-modules/add-material-dialog/add-material-dialog-data';
import { DialogsService } from '../features/dialogs/dialogs.service';
import { DialogType } from '../features/management-dialogs/management-dialogs.component';
import { BulkBehavior } from '../features/mds/types/types';
import { merge } from 'rxjs';
import { filter, map, toArray } from 'rxjs/operators';
import { MdsEditorDialogResult } from '../features/dialogs/dialog-modules/mds-editor-dialog/mds-editor-dialog-data';

/**
 * Provides high-level methods to allow uploading and saving new material.
 */
@Injectable({
    providedIn: 'root',
})
export class UploadDialogService {
    constructor(
        private config: ConfigurationService,
        private dialogs: DialogsService,
        private localEvents: LocalEventsService,
        private nodeService: RestNodeService,
        private nodeApi: NodeService,
        private toast: Toast,
    ) {}

    /**
     * Opens an upload dialog that allows the user to either upload new files or provide a web link.
     *
     * After the user confirms their choice, files will be uploaded (if any) and the required nodes
     * will be added to the repository.
     *
     * A dialog for editing metadata will be shown to the user. If the user cancels this dialog,
     * already created nodes will be removed from the repository.
     *
     * @returns the created nodes or null if the process was cancelled by the user
     */
    async openUploadDialog({
        parent,
        chooseParent,
        multiple = true,
    }: {
        parent: Node;
        chooseParent: boolean;
        multiple?: boolean;
    }): Promise<Node[] | null> {
        const dialogRef = await this.dialogs.openAddMaterialDialog({
            chooseParent,
            parent,
            multiple,
            showLti: true,
        });
        const result = await dialogRef.afterClosed().toPromise();
        if (result) {
            switch (result.kind) {
                case 'file':
                    return this.uploadFilesAndCreateNodes(result);
                case 'link':
                    return this.createLinkNode(result);
            }
        } else {
            return null;
        }
    }

    /**
     * Creates a new node for a web link in the repository.
     */
    async createLinkNode(data: LinkData): Promise<Node[] | null> {
        const urlData = this._createUrlLink(data);
        this.toast.showProgressSpinner();
        const { node } = await this.nodeService
            .createNode(
                data.parent?.ref.id,
                RestConstants.CCM_TYPE_IO,
                urlData.aspects,
                urlData.properties,
                true,
                RestConstants.COMMENT_MAIN_FILE_UPLOAD,
            )
            .toPromise();
        this.toast.closeProgressSpinner();
        return this._showMetadataAfterUpload([node]);
    }

    /**
     * Uploads new files and creates respective new nodes in the repository.
     */
    async uploadFilesAndCreateNodes(data: FileData): Promise<Node[] | null> {
        const dialogRef = await this.dialogs.openFileUploadProgressDialog(data);
        const nodes = await dialogRef.afterClosed().toPromise();
        if (nodes) {
            return this._onFilesUploaded(nodes);
        } else {
            return null;
        }
    }

    private async _onFilesUploaded(nodes: Node[]): Promise<Node[] | null> {
        const shouldShowLicenseDialog = await this.config
            .get('licenseDialogOnUpload', false)
            .toPromise();
        if (shouldShowLicenseDialog) {
            const dialogRef = await this.dialogs.openLicenseDialog({ kind: 'nodes', nodes });
            nodes = await dialogRef.afterClosed().toPromise();
        }
        if (nodes) {
            return this._showMetadataAfterUpload(nodes);
        } else {
            return null;
        }
    }

    private async _showMetadataAfterUpload(nodes: Node[]): Promise<Node[] | null> {
        const dialogType = await this.config
            .get('upload.postDialog', DialogType.SimpleEdit)
            .toPromise();
        if (dialogType === DialogType.SimpleEdit) {
            return this._openSimpleEditor(nodes);
        } else if (dialogType === DialogType.Mds) {
            return this._openMdsEditor(nodes);
        } else {
            console.error('Invalid configuration for upload.postDialog: ' + dialogType);
            return null;
        }
    }

    private async _openSimpleEditor(nodes: Node[]): Promise<Node[] | null> {
        const dialogRef = await this.dialogs.openSimpleEditDialog({ nodes, fromUpload: true });
        const updatedNodes = await dialogRef.afterClosed().toPromise();
        await this._afterMetadataEditDone(nodes, updatedNodes);
        return updatedNodes;
    }

    private async _openMdsEditor(nodes: Node[]): Promise<Node[] | null> {
        const dialogRef = await this.dialogs.openMdsEditorDialogForNodes({
            nodes,
            bulkBehavior: BulkBehavior.Replace,
        });
        let updatedNodes = await dialogRef.afterClosed().toPromise();
        updatedNodes = (await this._afterMetadataEditDone(nodes, updatedNodes)) as Node[];
        return updatedNodes;
    }

    private async _afterMetadataEditDone(
        originalNodes: Node[],
        updatedNodes: MdsEditorDialogResult,
    ) {
        // save was triggered but no changes were made - ignoring it
        if (updatedNodes === 'NO-CHANGES') {
            return originalNodes.map((n) => {
                (n as VirtualNode).virtual = true;
                return n;
            });
        }
        if (!updatedNodes) {
            // only delete these original nodes which are actually new
            // to check whether a node is new check that it only has one version:
            const nodesWithOneVersion = await merge(
                ...originalNodes.map((node) =>
                    this.nodeApi.getVersions(node.ref.repo, node.ref.id),
                ),
            )
                .pipe(
                    filter((versions) => versions.versions.length === 1),
                    map((versions) => versions.versions[0].node.id),
                    toArray(),
                )
                .toPromise();
            const nodesToDelete = originalNodes.filter((node) => {
                return nodesWithOneVersion.includes(node.ref.id);
            });
            await this._deleteNodes(nodesToDelete);
            this.localEvents.nodesDeleted.emit(nodesToDelete);
        }
        if (updatedNodes) {
            this.localEvents.nodesChanged.emit(updatedNodes as Node[]);
        }
        return updatedNodes;
    }

    private async _deleteNodes(nodes: Node[]): Promise<void> {
        this.toast.showProgressSpinner();
        console.log('delete', nodes);
        await rxjs
            .forkJoin(nodes.map((n) => this.nodeService.deleteNode(n.ref.id, false)))
            .toPromise();
        this.toast.closeProgressSpinner();
    }

    private _createUrlLink(data: LinkData): {
        properties: Node['properties'];
        aspects: string[];
        url: string;
    } {
        const properties: Node['properties'] = {};
        const aspects: string[] = [];
        const url = this._addHttpIfRequired(data.link);
        properties[RestConstants.CCM_PROP_IO_WWWURL] = [url];
        if (data.lti) {
            aspects.push(RestConstants.CCM_ASPECT_TOOL_INSTANCE_LINK);
            properties[RestConstants.CCM_PROP_TOOL_INSTANCE_KEY] = [data.lti.consumerKey];
            properties[RestConstants.CCM_PROP_TOOL_INSTANCE_SECRET] = [data.lti.sharedSecret];
        }
        properties[RestConstants.CCM_PROP_LINKTYPE] = [RestConstants.LINKTYPE_USER_GENERATED];
        return { properties, aspects, url };
    }

    private _addHttpIfRequired(link: string): string {
        if (!link.includes('://')) {
            return 'http://' + link;
        }
        return link;
    }
}
