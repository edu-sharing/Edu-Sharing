import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, firstValueFrom } from 'rxjs';
import { distinctUntilChanged, filter } from 'rxjs/operators';
import { Node, RestConstants, UsageV1Service } from 'ngx-edu-sharing-api';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import {
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
} from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-mds-editor-widget-collections',
    templateUrl: './mds-editor-widget-collections.component.html',
    styleUrls: ['./mds-editor-widget-collections.component.scss'],
})
export class MdsEditorWidgetCollectionsComponent implements OnInit, NativeWidgetComponent {
    readonly DisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    hasChanges = new BehaviorSubject<boolean>(false);

    dataSource = new NodeDataSource<Node>();
    columns = ListItem.getCollectionDefaults();

    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        private usageService: UsageV1Service,
    ) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$
            .pipe(
                distinctUntilChanged((a, b) => a?.[0]?.ref?.id === b?.[0]?.ref?.id),
                filter((n) => n != null),
            )
            .subscribe(async (nodes) => {
                if (nodes?.length === 1) {
                    let nodeId = nodes[0].ref.id;
                    if (this.isCollectionRef(nodes[0])) {
                        nodeId = nodes[0].properties?.[RestConstants.CCM_PROP_IO_ORIGINAL]?.[0];
                    }
                    this.dataSource.isLoading = true;
                    const result = (
                        (await firstValueFrom(
                            this.usageService.getUsagesByNodeCollections({ nodeId }),
                        )) as unknown as any[]
                    ).map((n) => n.collection) as Node[];
                    this.dataSource.setData(result);
                    this.dataSource.isLoading = false;
                }
            });
    }
    isCollectionRef(node: Node) {
        return node.aspects.indexOf(RestConstants.CCM_ASPECT_IO_REFERENCE) !== -1;
    }
}
