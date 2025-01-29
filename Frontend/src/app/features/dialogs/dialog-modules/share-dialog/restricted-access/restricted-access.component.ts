import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { Node, NodeService } from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';

@Component({
    selector: 'es-share-dialog-restricted-access',
    templateUrl: 'restricted-access.component.html',
    styleUrls: ['restricted-access.component.scss'],
})
export class ShareDialogRestrictedAccessComponent implements OnInit, OnChanges {
    constructor(private nodeService: NodeService) {}
    ngOnChanges(changes: SimpleChanges): void {
        this.restrictedAccess =
            this.node.properties[RestConstants.CCM_PROP_RESTRICTED_ACCESS]?.[0] === 'true' || false;
        this.restrictedAccessPermissions = {};
        const permissions =
            this.node.properties[RestConstants.CCM_PROP_RESTRICTED_ACCESS_PERMISSIONS] || [];
        this.RESTRICTED_ACCESS_PERMISSIONS.forEach((perm) => {
            this.restrictedAccessPermissions[perm] = permissions.includes(perm);
        });
    }
    readonly RESTRICTED_ACCESS_PERMISSIONS = ['ReadAll', 'DownloadContent'];
    @Input() node: Node;
    restrictedAccess: boolean;
    restrictedAccessPermissions: { [key in string]: boolean };
    ngOnInit(): void {}

    async save() {
        const properties = {
            [RestConstants.CCM_PROP_RESTRICTED_ACCESS]: [this.restrictedAccess + ''],
            [RestConstants.CCM_PROP_RESTRICTED_ACCESS_PERMISSIONS]: Object.keys(
                this.restrictedAccessPermissions,
            ).filter((key) => this.restrictedAccessPermissions[key] === true),
        };
        this.node.properties = {
            ...this.node.properties,
            ...properties,
        };
        await this.nodeService.editNodeMetadata(this.node.ref.id, properties).toPromise();
    }
}
