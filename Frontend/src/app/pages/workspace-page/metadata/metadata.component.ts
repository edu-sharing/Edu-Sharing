import {
    Component,
    ElementRef,
    EventEmitter,
    Input,
    OnInit,
    Output,
    ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService, NodeService, NodeVersion, NodeVersionEntries } from 'ngx-edu-sharing-api';
import {
    DurationHelper,
    FormatDatePipe,
    InteractionType,
    ListItem,
    NodeDataSource,
    NodeEntriesDisplayType,
    NodeImageSizePipe,
    UIConstants,
    VCard,
} from 'ngx-edu-sharing-ui';
import { BehaviorSubject } from 'rxjs';
import { distinctUntilChanged, filter } from 'rxjs/operators';
import {
    ConfigurationHelper,
    IamUser,
    Node,
    NodePermissions,
    Permission,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    RestSearchService,
    RestUsageService,
    Usage,
    UsageList,
} from '../../../core-module/core.module';
import { NodeHelperService } from '../../../services/node-helper.service';
import { UIHelper } from '../../../core-ui-module/ui-helper';
import {
    BarController,
    BarElement,
    CategoryScale,
    Chart,
    ChartType,
    LinearScale,
    PointElement,
    Tooltip,
    Title,
} from 'chart.js';

Chart.register(BarController, BarElement, CategoryScale, PointElement, Tooltip, LinearScale, Title);

interface Stats {
    labels: string[];
    colors: string[];
    points: number[];
    pointsIcons: string[];
}

@Component({
    selector: 'es-workspace-metadata',
    templateUrl: 'metadata.component.html',
    styleUrls: ['metadata.component.scss'],
})
export class WorkspaceMetadataComponent implements OnInit {
    private _canvas: ElementRef<HTMLCanvasElement>;
    private currentChart: Chart<ChartType, number[], string>;
    @ViewChild('canvas')
    get canvas(): ElementRef<HTMLCanvasElement> {
        return this._canvas;
    }
    set canvas(value: ElementRef<HTMLCanvasElement>) {
        this._canvas = value;
        this.drawBarChart();
    }

    @Input() isAdmin: boolean;
    @Input() set node(node: Node) {
        this.nodeSubject.next(node);
    }

    @Output() onEditMetadata = new EventEmitter();
    @Output() onDownload = new EventEmitter();
    @Output() onDisplay = new EventEmitter();
    @Output() onClose = new EventEmitter();
    @Output() onRestore = new EventEmitter();

    readonly NodeEntriesDisplayType = NodeEntriesDisplayType;
    readonly InteractionType = InteractionType;
    readonly INFO = 'INFO';
    readonly PROPERTIES = 'PROPERTIES';
    readonly VERSIONS = 'VERSIONS';
    data: any;
    loading = true;
    permissions: any;
    usagesCollection = new NodeDataSource();
    usagesCollectionData = this.usagesCollection.connect();
    nodeObject: Node;
    versions: NodeVersion[];
    versionsLoading = false;
    columns: ListItem[] = [];
    columnsCollections: ListItem[] = [];
    statsTotalPoints: number;
    forkedParent: Node;
    forkedChildren = new NodeDataSource();
    stats: Stats = {
        labels: [],
        points: [],
        pointsIcons: ['input', 'layers', 'cloud_download', 'remove_red_eye'],
        colors: [
            'rgba(230, 178, 71, .8)',
            'rgba(151, 91, 93, .8)',
            'rgba(27, 102, 49, .8)',
            'rgba(102,167,217,.8)',
        ],
    };

    private usages: Usage[];
    private nodeSubject = new BehaviorSubject<Node>(null);

    constructor(
        private translate: TranslateService,
        private config: ConfigService,
        private nodeHelper: NodeHelperService,
        private router: Router,
        private iamApi: RestIamService,
        private nodeApi: RestNodeService,
        private nodeService: NodeService,
        private searchApi: RestSearchService,
        private usageApi: RestUsageService,
    ) {
        this.columns.push(new ListItem('NODE', RestConstants.CM_NAME));
        this.columnsCollections.push(new ListItem('COLLECTION', 'title'));
        this.columnsCollections.push(new ListItem('COLLECTION', 'info'));
        this.columnsCollections.push(new ListItem('COLLECTION', 'scope'));
    }

    ngOnInit(): void {
        this.nodeSubject
            .pipe(
                filter((node) => node !== null),
                // map((node) => node.ref.id),
                // TODO: check if distinct still working
                distinctUntilChanged(),
            )
            .subscribe((node) => this.load(node));
    }

    private async load(node: Node) {
        this.versions = null;
        this.versionsLoading = true;
        this.resetStats();
        this.loading = true;
        // use temporary the given data to show headers
        this.data = this.format(node);
        this.nodeObject = (
            await this.nodeApi.getNodeMetadata(node.ref.id, [RestConstants.ALL]).toPromise()
        ).node;
        this.loading = false;
        this.data = this.format(this.nodeObject);
        const currentNode = this.nodeObject;
        this.nodeService
            .getVersionsMetadata(this.nodeObject.ref.repo, this.nodeObject.ref.id)
            .subscribe((data: NodeVersionEntries) => {
                if (currentNode !== this.nodeObject) return;
                this.versions = data.versions.reverse();
                for (const version of this.versions) {
                    if (version.comment) {
                        if (
                            version.comment === RestConstants.COMMENT_MAIN_FILE_UPLOAD ||
                            version.comment === RestConstants.COMMENT_METADATA_UPDATE ||
                            version.comment === RestConstants.COMMENT_CONTRIBUTOR_UPDATE ||
                            version.comment === RestConstants.COMMENT_CONTENT_UPDATE ||
                            version.comment === RestConstants.COMMENT_LICENSE_UPDATE ||
                            version.comment === RestConstants.COMMENT_NODE_PUBLISHED ||
                            version.comment === RestConstants.COMMENT_PREVIEW_CHANGED ||
                            version.comment.startsWith(RestConstants.COMMENT_EDITOR_UPLOAD)
                        ) {
                            const parameters = version.comment.split(',');
                            let editor = '';
                            if (parameters.length > 1)
                                editor = this.translate.instant(
                                    'CONNECTOR.' + parameters[1] + '.NAME',
                                );
                            version.comment = this.translate.instant(
                                'WORKSPACE.METADATA.COMMENT.' + parameters[0],
                                { editor },
                            );
                        }
                    }
                }
                let i = 0;
                for (const version of this.versions) {
                    if (this.isCurrentVersion(version)) {
                        this.versions.splice(i, 1);
                        this.versions.splice(0, 0, version);
                        break;
                    }
                    i++;
                }
                this.versionsLoading = false;
            });
        this.iamApi.getUser().subscribe((login: IamUser) => {
            this.nodeApi
                .getNodePermissions(this.nodeObject.ref.id)
                .subscribe((data: NodePermissions) => {
                    this.permissions = this.formatPermissions(login, data);
                });
        });
        this.usages = null;
        this.forkedParent = null;
        this.forkedChildren.reset();
        if (this.nodeObject.properties[RestConstants.CCM_PROP_FORKED_ORIGIN]) {
            this.nodeApi
                .getNodeMetadata(
                    RestHelper.removeSpacesStoreRef(
                        this.nodeObject.properties[RestConstants.CCM_PROP_FORKED_ORIGIN][0],
                    ),
                    [RestConstants.ALL],
                )
                .subscribe(
                    (parent) => {
                        this.forkedParent = parent.node;
                    },
                    (error) => {},
                );
        }
        const request = {
            propertyFilter: [RestConstants.ALL],
        };
        this.nodeService.getForkedChilds(node).subscribe((childs) => {
            this.forkedChildren.setData(childs.nodes);
        });
        this.usageApi.getNodeUsages(this.nodeObject.ref.id).subscribe((usages: UsageList) => {
            this.usages = usages.usages;
            this.usageApi
                .getNodeUsagesCollection(this.nodeObject.ref.id)
                .subscribe((collection) => {
                    this.usagesCollection.setData(
                        collection
                            .filter((c) => c.collectionUsageType === 'ACTIVE')
                            .map((c) => c.collection),
                    );
                    this.getStats();
                });
        });
    }

    isCurrentVersion(version: NodeVersion): boolean {
        if (!this.nodeObject) return false;
        const prop = this.nodeObject.properties[RestConstants.LOM_PROP_LIFECYCLE_VERSION];
        if (!prop) return false;

        return prop[0] == version.version.major + '.' + version.version.minor;
    }

    display(version: string = null) {
        this.nodeObject.version = version;
        this.onDisplay.emit(this.nodeObject);
    }

    displayNode(node: Node) {
        this.router.navigate([UIConstants.ROUTER_PREFIX + 'render', node.ref.id]);
    }

    displayCollection(collection: Node) {
        UIHelper.goToCollection(this.router, collection);
    }

    private openPermalink() {
        this.displayNode(this.nodeObject);
    }

    displayVersion(version: NodeVersion) {
        if (this.isCurrentVersion(version)) this.display();
        else this.display(version.version.major + '.' + version.version.minor);
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
        if (data.keywords && data.keywords.length == 1 && !data.keywords[0]) data.keywords = null;
        //data["creator"]=node.properties[RestConstants.CM_CREATOR];
        data.creator = ConfigurationHelper.getPersonWithConfigDisplayName(
            node.createdBy,
            this.config,
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
        data.mediatype = node.mediatype == 'file' ? node.mimetype : node.mediatype;
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

    close() {
        this.onClose.emit();
    }

    edit() {
        this.onEditMetadata.emit(this.nodeObject);
    }

    restoreVersion(restore: NodeVersion) {
        this.onRestore.emit({ version: restore, node: this.nodeObject });
    }

    canRevert() {
        return this.nodeObject && this.nodeObject.access.indexOf(RestConstants.ACCESS_WRITE) != -1;
    }

    isAnimated() {
        return this.nodeHelper.hasAnimatedPreview(this.nodeObject);
    }

    private formatPermissions(login: IamUser, permissions: NodePermissions): any {
        const data: any = {};
        data.users = [];
        data.groups = [];
        if (!permissions.permissions) return data;
        for (const permission of permissions.permissions.inheritedPermissions) {
            if (
                permission.authority.authorityName == login.person.authorityName ||
                permission.authority.authorityType == RestConstants.AUTHORITY_TYPE_OWNER
            ) {
            } else if (permission.authority.authorityType == RestConstants.AUTHORITY_TYPE_USER) {
                data.users.push(permission);
            } else {
                data.groups.push(permission);
            }
        }
        for (const permission of permissions.permissions.localPermissions.permissions) {
            if (
                permission.authority.authorityName == login.person.authorityName ||
                permission.authority.authorityType == RestConstants.AUTHORITY_TYPE_OWNER
            ) {
            } else if (permission.authority.authorityType == RestConstants.AUTHORITY_TYPE_USER) {
                if (!this.containsPermission(data.groups, permission)) data.users.push(permission);
            } else {
                if (!this.containsPermission(data.groups, permission)) data.groups.push(permission);
            }
        }
        return data;
    }

    private toVCards(properties: any[]) {
        const vcards: string[] = [];
        if (properties) {
            for (const p of properties) {
                vcards.push(new VCard(p).getDisplayName());
            }
        }
        return vcards;
    }

    private containsPermission(permissions: Permission[], permission: Permission) {
        for (const perm of permissions) {
            if (perm.authority.authorityName == permission.authority.authorityName) return true;
        }
        return false;
    }

    resetStats() {
        this.stats.labels = [];
        this.stats.points = [];
        this.statsTotalPoints = null;
    }

    getStats() {
        this.resetStats();
        this.stats.labels.push(this.translate.instant('WORKSPACE.METADATA.USAGE_TYPE.LMS'));
        this.stats.labels.push(this.translate.instant('WORKSPACE.METADATA.USAGE_TYPE.COLLECTION'));
        this.stats.labels.push(this.translate.instant('WORKSPACE.METADATA.USAGE_TYPE.DOWNLOAD'));
        this.stats.labels.push(this.translate.instant('WORKSPACE.METADATA.USAGE_TYPE.VIEW'));

        this.stats.points.push(this.usages.length - this.usagesCollection.getData().length);
        this.stats.points.push(this.usagesCollection.getData().length);
        this.stats.points.push(
            propertyToNumber(this.nodeObject.properties[RestConstants.CCM_PROP_TRACKING_DOWNLOADS]),
        );
        this.stats.points.push(
            propertyToNumber(this.nodeObject.properties[RestConstants.CCM_PROP_TRACKING_VIEWS]),
        );
        this.statsTotalPoints = this.stats.points.reduce((a, b) => a + b);
        this.drawBarChart();
    }

    private drawBarChart() {
        const canvas = this.canvas?.nativeElement;
        if (!canvas) {
            return;
        }
        const ctx = canvas.getContext('2d');
        // FontFamily
        // Chart.defaults.global.defaultFontFamily = 'open_sansregular';
        const statsMax = Math.max(...this.stats.points);
        if (!this.stats.labels?.length) {
            return;
        }
        if (this.currentChart) {
            this.currentChart.destroy();
        }
        console.log(this.stats);
        this.currentChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: this.stats.labels,
                datasets: [
                    {
                        data: this.stats.points,
                        backgroundColor: this.stats.colors,
                        borderWidth: 0.2,
                    },
                ],
            },
            options: {
                indexAxis: 'x',
                plugins: {
                    legend: {
                        display: false,
                    },
                    tooltip: {},
                },
                responsive: false,
                layout: {
                    padding: {
                        left: 0,
                        right: 0,
                        top: 20,
                        bottom: 0,
                    },
                },
                scales: {
                    x: {
                        ticks: {
                            display: false,
                        },
                    },
                    y: {
                        type: 'linear',
                        beginAtZero: true,
                        max: Math.max(Math.round(statsMax * 1.25), 6),
                    },
                },
            },
        });
    }

    canEdit() {
        return this.nodeObject && this.nodeObject.access.indexOf(RestConstants.ACCESS_WRITE) !== -1;
    }

    staticDataSource(node: Node) {
        return new NodeDataSource([node]);
    }
}

function propertyToNumber(property: string[]): number {
    if (property?.length > 0) {
        return parseInt(property[0]);
    } else {
        return 0;
    }
}
