import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { NetworkService, NodeService, RelationData, RelationService } from 'ngx-edu-sharing-api';
import { ListItem } from 'ngx-edu-sharing-ui';
import { forkJoin as observableForkJoin } from 'rxjs';
import { Node } from '../../../core-module/rest/data-object';
import { RestConstants } from '../../../core-module/rest/rest-constants';
import { RestHelper } from '../../../core-module/rest/rest-helper';
import { TranslateService } from '@ngx-translate/core';
import { first } from 'rxjs/operators';

@Component({
    selector: 'es-mds-node-relations-widget',
    templateUrl: 'node-relations-widget.component.html',
    styleUrls: ['node-relations-widget.component.scss'],
})
export class MdsNodeRelationsWidgetComponent implements OnInit, OnChanges {
    loading = true;
    @Input() node: Node;
    relations: RelationData[];
    columns = [new ListItem('NODE', RestConstants.LOM_PROP_TITLE)];
    versions: Node[];
    forkedOrigin: Node;
    forkedChilds: Node[];
    isFromHomeRepo: boolean;

    constructor(
        private translate: TranslateService,
        private relationService: RelationService,
        private nodeService: NodeService,
        private networkService: NetworkService,
    ) {}

    ngOnInit(): void {}

    async ngOnChanges(changes?: SimpleChanges) {
        if (this.node) {
            this.isFromHomeRepo = await this.networkService
                .isFromHomeRepository(this.node)
                .pipe(first())
                .toPromise();
            if (!this.isFromHomeRepo) {
                this.loading = false;
                return;
            }
            observableForkJoin([
                this.nodeService.getForkedChilds(this.node),
                this.nodeService.getPublishedCopies(this.node.ref.id),
                this.relationService.getRelations(this.node.ref.id),
            ]).subscribe(async (result) => {
                this.forkedChilds = result[0].nodes;
                this.versions = result[1].nodes.reverse();
                this.relations = result[2].relations;
                // is a forked child
                if (this.node.properties[RestConstants.CCM_PROP_FORKED_ORIGIN]) {
                    this.nodeService
                        .getNode(
                            RestHelper.removeSpacesStoreRef(
                                this.node.properties[RestConstants.CCM_PROP_FORKED_ORIGIN][0],
                            ),
                        )
                        .subscribe(
                            (node) => {
                                this.forkedOrigin = node;
                                this.loading = false;
                            },
                            (error) => {
                                // soft error, do not trigger toast
                                error.preventDefault();
                                this.loading = false;
                            },
                        );
                } else {
                    this.loading = false;
                }
            });
        }
    }

    getRelationKeys() {
        return [...new Set(this.relations?.map((r) => r.type))].sort();
    }

    getRelations(key: string) {
        return this.relations.filter((r) => r.type === key);
    }

    isCurrentVersion(version: Node) {
        // collection refs always refer to the latest version
        if (this.node.aspects.includes(RestConstants.CCM_ASPECT_IO_REFERENCE)) {
            const publishedVersion = this.versions.find(
                (v) => v.ref.id === this.node.properties[RestConstants.CCM_PROP_IO_ORIGINAL]?.[0],
            );
            if (publishedVersion) {
                return publishedVersion === version;
            }
            return this.versions.indexOf(version) === 0;
        }
        return (
            this.node.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0] ===
            version.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION]?.[0]
        );
    }
}
