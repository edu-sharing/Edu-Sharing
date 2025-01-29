import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
    ConfigurationHelper,
    NodePermissions,
    Permission,
    RestConnectorService,
    RestConstants,
    RestNodeService,
} from '../../../core-module/core.module';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService, Node } from 'ngx-edu-sharing-api';
import { DurationHelper, FormatDatePipe, NodeImageSizePipe, VCard } from 'ngx-edu-sharing-ui';
import { NodeHelperService } from '../../../services/node-helper.service';

// Charts.js
declare var Chart: any;

@Component({
    selector: 'es-workspace-metadata-block',
    templateUrl: 'metadata-block.component.html',
    styleUrls: ['metadata-block.component.scss'],
})
export class WorkspaceMetadataBlockComponent {
    @Input() set node(node: Node) {
        void this.load(node);
    }
    @Output() onEditMetadata = new EventEmitter();
    @Output() onDisplay = new EventEmitter();
    permissions: any;
    data: any;
    _node: Node;

    private async load(node: Node) {
        this._node = node;
        this.data = await this.format(node);
        this.nodeApi.getNodePermissions(node.ref.id).subscribe((data: NodePermissions) => {
            this.permissions = this.formatPermissions(data);
        });
    }
    private format(node: Node): any {
        const data: any = {};
        data.name = node.name;
        data.title = node.title;
        data.isDirectory = node.isDirectory;
        data.isCollection = node.collection != null;
        data.description = node.properties[RestConstants.LOM_PROP_GENERAL_DESCRIPTION];
        data.preview = node.preview.url;
        data.keywords = node.properties[RestConstants.LOM_PROP_GENERAL_KEYWORD];
        if (data.keywords && data.keywords.length === 1 && !data.keywords[0]) data.keywords = null;
        // data["creator"]=node.properties[RestConstants.CM_CREATOR];
        data.creator = ConfigurationHelper.getPersonWithConfigDisplayName(
            node.createdBy,
            this.configService,
        );
        data.createDate = new FormatDatePipe(this.translate).transform(node.createdAt);
        data.duration = DurationHelper.getDurationFormatted(
            node.properties[RestConstants.LOM_PROP_TECHNICAL_DURATION]?.[0],
        );
        data.author = this.toVCards(
            node.properties[RestConstants.CCM_PROP_LIFECYCLECONTRIBUTER_AUTHOR],
        ).join(', ');
        data.author_freetext = node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT]
            ? node.properties[RestConstants.CCM_PROP_AUTHOR_FREETEXT][0]
            : null;
        data.mediatype = node.mediatype === 'file' ? node.mimetype : node.mediatype;
        data.mimetype = node.mimetype;
        data.size = node.size;
        if (node.properties[RestConstants.EXIF_PROP_DATE_TIME_ORIGINAL]) {
            data.exifDate = new FormatDatePipe(this.translate).transform(
                node.properties[RestConstants.EXIF_PROP_DATE_TIME_ORIGINAL][0],
            );
        }

        data.dimensions = new NodeImageSizePipe().transform(node);

        data.license = this.nodeHelper.getLicenseIcon(node);
        data.licenseName = this.nodeHelper.getLicenseName(node);

        data.properties = [];
        data.aspects = node.aspects.sort();

        for (const k of Object.keys(node.properties).sort()) {
            data.properties.push([k, node.properties[k].join(', ')]);
        }
        return data;
    }
    constructor(
        public connector: RestConnectorService,
        private nodeApi: RestNodeService,
        private translate: TranslateService,
        private configService: ConfigService,
        private nodeHelper: NodeHelperService,
    ) {}
    isAnimated() {
        return this.nodeHelper.hasAnimatedPreview(this._node);
    }
    private formatPermissions(permissions: NodePermissions): any {
        const currentAuth = this.connector.getCurrentLogin()?.authorityName;
        const data: any = {};
        data.users = [];
        data.groups = [];
        if (!permissions.permissions) return data;
        for (const permission of permissions.permissions.inheritedPermissions) {
            if (
                permission.authority.authorityName === currentAuth ||
                permission.authority.authorityType === RestConstants.AUTHORITY_TYPE_OWNER
            ) {
            } else if (permission.authority.authorityType === RestConstants.AUTHORITY_TYPE_USER) {
                data.users.push(permission);
            } else {
                data.groups.push(permission);
            }
        }
        for (const permission of permissions.permissions.localPermissions.permissions) {
            if (
                permission.authority.authorityName === currentAuth ||
                permission.authority.authorityType === RestConstants.AUTHORITY_TYPE_OWNER
            ) {
            } else if (permission.authority.authorityType === RestConstants.AUTHORITY_TYPE_USER) {
                if (!this.containsPermission(data.groups, permission)) data.users.push(permission);
            } else {
                if (!this.containsPermission(data.groups, permission)) data.groups.push(permission);
            }
        }
        return data;
    }

    toVCards(properties: any[]) {
        const vcards: string[] = [];
        if (properties) {
            for (const p of properties) {
                vcards.push(new VCard(p).getDisplayName());
            }
        }
        return vcards;
    }

    containsPermission(permissions: Permission[], permission: Permission) {
        for (const perm of permissions) {
            if (perm.authority.authorityName == permission.authority.authorityName) return true;
        }
        return false;
    }
    canEdit() {
        return this._node && this._node.access.indexOf(RestConstants.ACCESS_WRITE) !== -1;
    }
}
