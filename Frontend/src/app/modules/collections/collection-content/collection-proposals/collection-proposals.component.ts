import {
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import {
    InteractionType,
    ListEventInterface,
    NodeEntriesDisplayType,
} from '../../../../features/node-entries/entries-model';
import { Scope } from '../../../../core-ui-module/option-item';
import { NodeDataSource } from '../../../../features/node-entries/node-data-source';
import { CollectionReference, ProposalNode } from '../../../../core-module/rest/data-object';
import { Node } from 'ngx-edu-sharing-api';
import { RestCollectionService } from '../../../../core-module/rest/services/rest-collection.service';
import { ListItem } from '../../../../core-module/ui/list-item';
import { RestConstants } from '../../../../core-module/rest/rest-constants';
import {
    ManagementEvent,
    ManagementEventType,
} from '../../../management-dialogs/management-dialogs.component';
import { MainNavService } from '../../../../main/navigation/main-nav.service';
import { OptionsHelperService } from '../../../../core-ui-module/options-helper.service';
import { ActionbarComponent } from '../../../../shared/components/actionbar/actionbar.component';

@Component({
    selector: 'es-collection-proposals',
    templateUrl: 'collection-proposals.component.html',
    styleUrls: ['collection-proposals.component.scss'],
    providers: [OptionsHelperService],
})
export class CollectionProposalsComponent implements OnChanges {
    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly Scope = Scope;
    proposalColumns = [
        new ListItem('NODE', RestConstants.CM_PROP_TITLE),
        new ListItem('NODE_PROPOSAL', RestConstants.CM_CREATOR, { showLabel: false }),
        new ListItem('NODE_PROPOSAL', RestConstants.CM_PROP_C_CREATED, { showLabel: false }),
    ];
    @ViewChild('listProposals') listProposals: ListEventInterface<ProposalNode>;
    @ViewChild(ActionbarComponent) actionbar: ActionbarComponent;
    dataSourceCollectionProposals = new NodeDataSource<ProposalNode>();

    @Input() collection: Node;
    @Input() canEdit: boolean;

    @Output() onContentClick = new EventEmitter<ProposalNode>();

    constructor(
        private collectionService: RestCollectionService,
        private mainNavService: MainNavService,
        private optionsHelperService: OptionsHelperService,
    ) {
        this.mainNavService.getDialogs().onEvent.subscribe((event: ManagementEvent) => {
            if (event.event === ManagementEventType.AddCollectionNodes) {
                this.refreshProposals();
            }
        });
    }
    private refreshProposals() {
        this.dataSourceCollectionProposals.reset();
        this.dataSourceCollectionProposals.isLoading = true;
        if (this.canEdit) {
            this.collectionService
                .getCollectionProposals(this.collection.ref.id)
                .subscribe((proposals) => {
                    proposals.nodes = proposals.nodes.map((p) => {
                        p.proposalCollection = this.collection;
                        return p;
                    });
                    this.dataSourceCollectionProposals.setData(
                        proposals.nodes,
                        proposals.pagination,
                    );
                    this.dataSourceCollectionProposals.isLoading = false;
                    setTimeout(() => {
                        this.listProposals?.initOptionsGenerator({
                            actionbar: this.actionbar,
                        });
                    });
                });
        }
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.collection?.currentValue !== null && changes.canEdit?.currentValue !== null) {
            this.refreshProposals();
        }
    }
}
