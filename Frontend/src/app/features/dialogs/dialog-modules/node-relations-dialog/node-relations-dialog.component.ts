import {
    ChangeDetectionStrategy,
    ChangeDetectorRef,
    Component,
    Inject,
    OnInit,
} from '@angular/core';
import { UntypedFormControl, UntypedFormGroup, Validators } from '@angular/forms';
import { Node, NodeService, RelationData, RelationService, UserService } from 'ngx-edu-sharing-api';
import { forkJoin } from 'rxjs';
import { first } from 'rxjs/operators';
import { UniversalNode } from '../../../../core-module/rest/definitions';
import { BridgeService } from '../../../../services/bridge.service';
import {
    DialogButton,
    RestConstants,
    RestHelper,
    SearchRequestCriteria,
} from '../../../../core-module/core.module';
import { NodeHelperService } from '../../../../services/node-helper.service';
import { Toast } from '../../../../services/toast';
import { UIHelper } from '../../../../core-ui-module/ui-helper';
import { CARD_DIALOG_DATA, Closable } from '../../card-dialog/card-dialog-config';
import { CardDialogRef } from '../../card-dialog/card-dialog-ref';
import { NodeRelationsDialogData, NodeRelationsDialogResult } from './node-relations-dialog-data';
import { ListItem, LocalEventsService, NodesRightMode, OPEN_URL_MODE } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-node-relations-dialog',
    templateUrl: './node-relations-dialog.component.html',
    styleUrls: ['./node-relations-dialog.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NodeRelationsDialogComponent implements OnInit {
    readonly Relations = Object.values(Relations);
    readonly RelationsInverted = {
        [Relations.isBasedOn]: 'isBasisFor',
        [Relations.isPartOf]: 'hasPart',
        [Relations.references]: 'references',
    };

    source: UniversalNode;
    relations: RelationData[];
    addRelations: RelationData[] = [];
    deleteRelations: RelationData[] = [];
    swapRelation: boolean;
    readonly form = new UntypedFormGroup({
        relation: new UntypedFormControl(Relations.isBasedOn, Validators.required),
    });
    permissions = [RestConstants.PERMISSION_WRITE];
    target: UniversalNode;
    columns = [new ListItem('NODE', RestConstants.LOM_PROP_TITLE)];

    private readonly buttons = [
        new DialogButton('CANCEL', DialogButton.TYPE_CANCEL, () => this.dialogRef.close(null)),
        new DialogButton('SAVE', DialogButton.TYPE_PRIMARY, () => this.save()),
    ];

    constructor(
        @Inject(CARD_DIALOG_DATA) public data: NodeRelationsDialogData,
        private dialogRef: CardDialogRef<NodeRelationsDialogData, NodeRelationsDialogResult>,
        private bridgeService: BridgeService,
        private cdr: ChangeDetectorRef,
        private localEvents: LocalEventsService,
        private nodeHelper: NodeHelperService,
        private nodeService: NodeService,
        private relationService: RelationService,
        private toast: Toast,
        private userService: UserService,
    ) {
        this.dialogRef.patchState({ isLoading: true });
    }

    ngOnInit(): void {
        this.dialogRef.patchConfig({ buttons: this.buttons });
        this.initNode(this.data.node);
        this.updateButtons();
    }

    getRelationKeys() {
        return [
            ...new Set(this.addRelations.concat(this.relations || [])?.map((r) => r.type)),
        ].sort();
    }

    swap() {
        this.swapRelation = !this.swapRelation;
    }

    getCriteria(): SearchRequestCriteria[] {
        return [
            {
                property: 'sourceNode',
                values: [this.source.ref.id],
            },
        ];
    }

    getAllExistingRelations() {
        return this.getAllRelations().filter((r) => !this.deleteRelations.includes(r));
    }

    getAllRelations() {
        return this.relations.concat(this.addRelations);
    }

    getRelations(
        key: 'isPartOf' | 'isBasedOn' | 'references' | 'hasPart' | 'isBasisFor',
    ): RelationData[] {
        return this.getAllRelations()
            .filter((r) => r.type === key)
            .sort((a, b) => (a.timestamp > b.timestamp ? 1 : -1));
    }

    openNode(node: UniversalNode) {
        UIHelper.openUrl(
            this.nodeHelper.getNodeUrl(node, {
                closeOnBack: true,
            }),
            this.bridgeService,
            OPEN_URL_MODE.Blank,
        );
    }

    removeRelation(relation: RelationData) {
        if (!this.deleteRelations.includes(relation)) {
            this.deleteRelations.push(relation);
        }
        this.updateButtons();
    }

    resolveRelationSendData(r: RelationData) {
        const inverted = this.isInverted(r);
        let source = this.source.ref.id;
        let target = r.node.ref.id;
        let type: string = r.type;
        if (inverted) {
            source = r.node.ref.id;
            target = this.source.ref.id;
            type = inverted;
        }
        return {
            source,
            target,
            type,
        };
    }

    private async save() {
        this.toast.showProgressSpinner();
        try {
            await forkJoin(
                this.addRelations.map((r) => {
                    const data = this.resolveRelationSendData(r);
                    return this.relationService.createRelation(
                        data.source,
                        data.target,
                        data.type as any,
                    );
                }),
            ).toPromise();
            await forkJoin(
                this.deleteRelations.map((r) => {
                    const data = this.resolveRelationSendData(r);
                    return this.relationService.deleteRelation(
                        data.source,
                        data.target,
                        data.type as any,
                    );
                }),
            ).toPromise();
            this.dialogRef.close(true);
            this.localEvents.nodesChanged.emit([this.data.node]);
        } catch (e) {}
        this.toast.closeProgressSpinner();
    }

    updateButtons() {
        const hasChanges = this.hasChanges();
        this.buttons[1].disabled = !hasChanges;
        if (hasChanges) {
            this.dialogRef.patchConfig({ closable: Closable.Confirm });
        } else if (this.target) {
            this.dialogRef.patchConfig({ closable: Closable.Standard });
        } else {
            this.dialogRef.patchConfig({ closable: Closable.Casual });
        }
    }

    getCurrentType() {}

    async createRelation() {
        let type = this.form.get('relation').value;
        if (this.swapRelation) {
            type = (this.RelationsInverted as any)[type];
        }
        if (
            this.getAllExistingRelations().find(
                (r) => r.node.ref.id === this.target.ref.id && r.type === type,
            )
        ) {
            this.toast.error(null, 'NODE_RELATIONS.RELATION_EXISTS');
            return;
        }
        this.addRelations.push({
            node: this.target,
            type,
            // @TODO: check if api model is invalid
            timestamp: new Date().getTime() as any,
            creator: (await this.userService.observeCurrentUser().pipe(first()).toPromise()).person,
        });
        this.form.reset();
        this.form.setValue({ relation: Relations.isBasedOn });
        this.swapRelation = false;
        this.target = null;
        this.updateButtons();
    }

    private isInverted(r: RelationData) {
        return Object.keys(this.RelationsInverted).find(
            (k) => (this.RelationsInverted as any)[k] === r.type && k !== r.type,
        );
    }

    isPublishedCopy() {
        return !!this.source.properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]?.[0];
    }

    isSwappable() {
        const relation = this.form.get('relation').value;
        return !((this.RelationsInverted as any)[relation] === relation);
    }

    canModify(relation: RelationData) {
        return this.nodeHelper.getNodesRight(
            [relation.node],
            RestConstants.PERMISSION_WRITE,
            NodesRightMode.Effective,
        );
    }

    private hasChanges() {
        return this.addRelations?.length || this.deleteRelations?.length;
    }

    private async initNode(node: Node): Promise<void> {
        // published original: we now need to switch to the original id!
        if (node.properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL]) {
            // switch to original node id!
            node = await this.nodeService
                .getNode(
                    RestHelper.removeSpacesStoreRef(
                        node.properties[RestConstants.CCM_PROP_PUBLISHED_ORIGINAL][0],
                    ),
                )
                .toPromise();
        }
        this.source = node;
        this.relationService.getRelations(node.ref.id).subscribe(
            (relations) => {
                this.relations = relations.relations;
                this.dialogRef.patchState({ isLoading: false });
                this.cdr.detectChanges();
            },
            (e) => {
                this.dialogRef.close(null);
            },
        );
    }
}

enum Relations {
    isBasedOn = 'isBasedOn',
    isPartOf = 'isPartOf',
    references = 'references',
}
