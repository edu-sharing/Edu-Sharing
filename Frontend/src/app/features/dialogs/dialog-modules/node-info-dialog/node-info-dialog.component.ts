import { AfterViewInit, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Ace, Acl, ConfigService, NodeServiceUnwrapped, Person } from 'ngx-edu-sharing-api';
import {
    ActionbarComponent,
    OptionsHelperDataService,
    Scope,
    UIConstants,
} from 'ngx-edu-sharing-ui';
import { forkJoin } from 'rxjs';
import {
    ConfigurationHelper,
    Node,
    NodeList,
    Permissions,
    RestConstants,
    RestNodeService,
    UserProfile,
} from '../../../../core-module/core.module';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { BreadcrumbsService } from '../../../../shared/components/breadcrumbs/breadcrumbs.service';
import { CARD_DIALOG_DATA } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { CardDialogUtilsService } from '../../card-dialog/card-dialog-utils.service';

export interface NodeInfoDialogData {
    nodes: Node[];
}
type RawPermissions = {
    inherited: boolean;
    aces: {
        authority: string;
        authorityType: string;
        permission: string;
    }[];
    user: any;
    group: any;
};

@Component({
    selector: 'es-node-info-dialog',
    templateUrl: 'node-info-dialog.component.html',
    styleUrls: ['node-info-dialog.component.scss'],
    providers: [BreadcrumbsService, OptionsHelperDataService],
})
/**
 * A node info dialog (useful primary for admin stuff)
 */
export class NodeInfoDialogComponent implements OnInit, AfterViewInit {
    @ViewChild(ActionbarComponent) actionbarComponent: ActionbarComponent;
    _nodes: Node[];
    _children: Node[];
    _permissions: RawPermissions;
    _parentPermissions: RawPermissions;
    _properties: any[];
    _creator: string;
    _json: string;
    saving: boolean;
    customProperty: string[] = [];
    editMode: boolean;

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodeInfoDialogData,
        private dialogRef: CardDialogRef,
        private cardDialogUtils: CardDialogUtilsService,
        private config: ConfigService,
        private nodeApi: RestNodeService,
        private nodeServiceUnwrapped: NodeServiceUnwrapped,
        private router: Router,
        private breadcrumbsService: BreadcrumbsService,
        private optionsHelperDataService: OptionsHelperDataService,
        private toast: Toast,
        private translate: TranslateService,
    ) {}

    async ngAfterViewInit() {
        console.log(this.actionbarComponent);
        await this.optionsHelperDataService.initComponents(this.actionbarComponent);
        this.optionsHelperDataService.refreshComponents();
    }

    ngOnInit(): void {
        this.setNodes(this.data.nodes);
    }

    private setNodes(nodes: Node[]): void {
        this._nodes = nodes;
        this.translate
            .get('NODE_INFO.TITLE', { name: this._nodes[0].name })
            .subscribe((title) => this.dialogRef.patchConfig({ title }));
        void this.cardDialogUtils
            .configForNodes(nodes)
            .then((config) => this.dialogRef.patchConfig(config));
        this._properties = [];
        nodes
            .filter((n) => n.properties)
            .forEach((n) => {
                for (let k of Object.keys(n.properties).sort()) {
                    if (n.properties[k].join('')) {
                        const value = n.properties[k].join(', ');
                        const current = this._properties.filter((n) => n[0] === k);
                        if (!current.length) {
                            this._properties.push([k, value]);
                        } else if (current[0][1] === value) {
                            // do nothing
                        } else {
                            this._properties.splice(this._properties.indexOf(current[0]), 1);
                            this._properties.push([k, '[VARYING VALUES]']);
                        }
                    }
                }
            });
        if (this._nodes.length === 1) {
            const node = nodes[0];
            this._creator = ConfigurationHelper.getPersonWithConfigDisplayName(
                node.createdBy,
                this.config,
            );
            this._json = JSON.stringify(node, null, 4);
            this.nodeApi.getNodeParents(node.ref.id, true).subscribe((data: NodeList) => {
                this.breadcrumbsService.setNodePath(data.nodes.reverse());
            });
            this.nodeApi
                .getChildren(node.ref.id, [RestConstants.FILTER_SPECIAL], {
                    propertyFilter: [RestConstants.ALL],
                    count: RestConstants.COUNT_UNLIMITED,
                })
                .subscribe((data: NodeList) => {
                    this._children = data.nodes;
                });
            this.nodeServiceUnwrapped
                .getRawPermission({
                    repository: node.ref.repo,
                    node: node.ref.id,
                })
                .subscribe((data) => {
                    this._permissions = data as unknown as RawPermissions;
                });
            if (node.parent?.id) {
                this.nodeServiceUnwrapped
                    .getRawPermission({
                        repository: node.parent.repo,
                        node: node.parent.id,
                    })
                    .subscribe((data) => {
                        this._parentPermissions = data as unknown as RawPermissions;
                    });
            }
        }
        console.log(this.actionbarComponent);
        this.optionsHelperDataService.setData({
            scope: Scope.Admin,
            activeObjects: this._nodes,
            selectedObjects: this._nodes,
        });
    }

    openNodes(nodes: Node[]) {
        this.breadcrumbsService.setNodePath(null);
        this._children = null;
        this.setNodes(nodes);
    }
    openNodeWorkspace(node: Node) {
        this.router.navigate([UIConstants.ROUTER_PREFIX, 'workspace'], {
            queryParams: { id: node.parent.id, file: node.ref.id },
        });
        this.dialogRef.close();
    }
    openBreadcrumb(pos: number) {
        let node = this.breadcrumbsService.breadcrumbs$.value[pos - 1];
        this.breadcrumbsService.setNodePath([]);
        this._children = null;
        this.setNodes([node]);
        //this.router.navigate([UIConstants.ROUTER_PREFIX,"workspace"],{queryParams:{id:node.ref.id}});
        //this.close();
    }

    canEdit() {
        return this._nodes?.every((n) => n.access?.indexOf(RestConstants.ACCESS_WRITE) != -1);
    }

    addProperty() {
        if (this.customProperty[0]) {
            this.saving = true;
            forkJoin(
                this._nodes.map((n) =>
                    this.nodeApi.editNodeProperty(
                        n.ref.id,
                        this.customProperty[0],
                        this.customProperty[1].split(','),
                        n.ref.repo,
                    ),
                ),
            ).subscribe(
                () => {
                    this.customProperty = [];
                    this.refreshMeta();
                },
                (error) => {
                    this.toast.error(error);
                    this.saving = false;
                },
            );
        }
    }

    saveProperty(property: string[]) {
        this.saving = true;
        forkJoin(
            this._nodes.map((n) =>
                this.nodeApi.editNodeProperty(
                    n.ref.id,
                    property[0],
                    property[1].split(','),
                    n.ref.repo,
                ),
            ),
        ).subscribe(
            () => {
                this.customProperty = [];
                this.refreshMeta();
            },
            (error) => {
                this.toast.error(error);
                this.saving = false;
            },
        );
    }

    private refreshMeta() {
        forkJoin(
            this._nodes.map((n) =>
                this.nodeApi.getNodeMetadata(n.ref.id, [RestConstants.ALL], n.ref.repo),
            ),
        ).subscribe((nodes) => {
            this.saving = false;
            this.openNodes(nodes.map((n) => n.node));
        });
    }

    copyNodeIdToClipboard(node: Node) {
        UIHelper.copyToClipboard(node.ref.id);
        this.toast.toast('ADMIN.APPLICATIONS.COPIED_CLIPBOARD');
    }
}
