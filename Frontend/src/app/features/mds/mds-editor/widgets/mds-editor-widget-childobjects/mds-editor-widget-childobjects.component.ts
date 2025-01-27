import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, Observable, forkJoin as observableForkJoin } from 'rxjs';
import { distinctUntilChanged, filter } from 'rxjs/operators';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { RestHelper } from '../../../../../core-module/rest/rest-helper';
import { RestConnectorService } from '../../../../../core-module/rest/services/rest-connector.service';
import { RestNodeService } from '../../../../../core-module/rest/services/rest-node.service';
import { RestUtilitiesService } from '../../../../../core-module/rest/services/rest-utilities.service';
import { NodeHelperService } from '../../../../../services/node-helper.service';
import { Toast } from '../../../../../services/toast';
import { DialogsService } from '../../../../dialogs/dialogs.service';
import { Node } from 'ngx-edu-sharing-api';
import { Values } from '../../../types/types';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';

interface Childobject {
    icon: string;
    name: string;
    link?: string;
    node?: Node;
    file?: File;
    properties?: Values;
}
interface ChildobjectEdit {
    child: Childobject;
    properties: Values;
}
@Component({
    selector: 'es-mds-editor-widget-childobjects',
    templateUrl: './mds-editor-widget-childobjects.component.html',
    styleUrls: ['./mds-editor-widget-childobjects.component.scss'],
})
export class MdsEditorWidgetChildobjectsComponent implements OnInit, NativeWidgetComponent {
    static readonly constraints = {
        requiresNode: true,
        supportsBulk: false,
    };
    static readonly graphqlIds: string[] = [];
    hasChanges = new BehaviorSubject<boolean>(false);
    children: Childobject[] = [];
    childrenDelete: Node[] = [];
    isSupported: boolean;
    nodes: Node[];

    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        private nodeApi: RestNodeService,
        private connector: RestConnectorService,
        private utilities: RestUtilitiesService,
        private nodeHelper: NodeHelperService,
        public toast: Toast,
        private dialogs: DialogsService,
    ) {}

    ngOnInit(): void {
        this.mdsEditorValues.nodes$
            .pipe(
                distinctUntilChanged((a, b) => a?.[0]?.ref?.id === b?.[0]?.ref?.id),
                filter((n) => n != null),
            )
            .subscribe(async (nodes) => {
                if (nodes?.length === 1) {
                    this.children = (
                        await this.nodeApi.getNodeChildobjects(nodes[0].ref.id).toPromise()
                    ).nodes.map((n) => {
                        return {
                            icon: n.iconURL,
                            name: RestHelper.getTitle(n),
                            node: n,
                            properties: n.properties,
                        };
                    });
                    this.nodes = nodes;
                    this.isSupported = nodes[0].type === RestConstants.CCM_TYPE_IO;
                } else {
                    this.isSupported = false;
                }
            });
    }

    onChange(): void {
        this.hasChanges.next(true);
    }

    async openUploadSelectDialog(): Promise<void> {
        const dialogRef = await this.dialogs.openAddMaterialDialog({
            // FIXME: We have ported these options from implicit values of the legacy dialog system,
            // but didn't check whether the values make sense here.
            parent: null,
            chooseParent: false,
            multiple: false,
            showLti: false,
        });
        dialogRef.afterClosed().subscribe((result) => {
            if (result) {
                switch (result.kind) {
                    case 'file':
                        this.addFiles(result.files);
                        break;
                    case 'link':
                        this.addLink(result.link);
                        break;
                }
            }
        });
    }

    private addFiles(files: FileList) {
        for (let i = 0; i < files.length; i++) {
            const file = files.item(i);
            const child: Childobject = {
                icon: RestHelper.guessMediatypeIconForFile(this.connector, file),
                name: file.name,
                file,
            };
            this.children.push(child);
        }
        this.onChange();
    }

    private addLink(link: string) {
        link = this.nodeHelper.addHttpIfRequired(link);
        const properties = RestHelper.createNameProperty(link);
        properties[RestConstants.CCM_PROP_IO_WWWURL] = [link];
        properties[RestConstants.LOM_PROP_TITLE] = [link];
        const process = () => {
            const data: Childobject = {
                icon: this.connector.getThemeMimeIconSvg('link.svg'),
                name: RestHelper.getTitleFromProperties(properties),
                link,
                properties,
            };
            this.children.push(data);
            this.onChange();
        };
        this.utilities.getWebsiteInformation(link).subscribe(
            (info) => {
                if (info.title)
                    properties[RestConstants.LOM_PROP_TITLE] = [info.title + ' - ' + info.page];
                process();
            },
            (error) => {
                console.warn(error);
                process();
            },
        );
    }

    setProperties(newProperties: Values, edit: ChildobjectEdit) {
        // keep any existing license data
        if (!edit.child.properties) {
            edit.child.properties = edit.properties;
        }
        for (const key of Object.keys(newProperties)) {
            edit.child.properties[key] = newProperties[key];
        }
        edit.child.name = edit.child.properties[RestConstants.LOM_PROP_TITLE]?.[0]
            ? edit.child.properties[RestConstants.LOM_PROP_TITLE][0]
            : edit.child.properties[RestConstants.CM_NAME][0];
        this.onChange();
    }

    private getProperties(child: Childobject) {
        let props: Values;
        if (child.properties) {
            props = child.properties;
        } else if (child.file) {
            props = RestHelper.createNameProperty(child.name);
        } else {
            console.error('Invalid object state for childobject', child);
            return null;
        }
        props[RestConstants.CCM_PROP_CHILDOBJECT_ORDER] = [this.children.indexOf(child) + ''];
        return props;
    }

    async editMetadata(child: Childobject) {
        const properties = this.getProperties(child);
        const dialogRef = await this.dialogs.openMdsEditorDialogForValues({
            values: properties,
            groupId: 'io_childobject',
            editorMode: 'nodes',
            setId: this.nodes?.[0].metadataset,
        });
        dialogRef.afterClosed().subscribe((newProperties) => {
            if (newProperties) {
                this.setProperties(newProperties, { child, properties });
            }
        });
    }

    async editLicense(child: Childobject) {
        const properties = this.getProperties(child);
        const dialogRef = await this.dialogs.openLicenseDialog({ kind: 'properties', properties });
        dialogRef.afterClosed().subscribe((newProperties) => {
            if (newProperties) {
                this.setProperties(newProperties, { child, properties });
            }
        });
    }

    remove(child: Childobject) {
        this.children.splice(this.children.indexOf(child), 1);
        if (child.node) {
            this.childrenDelete.push(child.node);
        }
        this.onChange();
    }

    async onSaveNode(nodes: Node[]) {
        await observableForkJoin(
            this.children.map(
                (child) =>
                    new Observable<Node>((observer) => {
                        if (child.file) {
                            this.nodeApi
                                .createNode(
                                    nodes[0].ref.id,
                                    RestConstants.CCM_TYPE_IO,
                                    [RestConstants.CCM_ASPECT_IO_CHILDOBJECT],
                                    this.getProperties(child),
                                    true,
                                    '',
                                    RestConstants.CCM_ASSOC_CHILDIO,
                                )
                                .subscribe((data) => {
                                    this.nodeApi
                                        .uploadNodeContent(
                                            data.node.ref.id,
                                            child.file,
                                            RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                                        )
                                        .subscribe(
                                            () => {
                                                observer.complete();
                                            },
                                            (error) => {
                                                if (
                                                    RestHelper.errorMatchesAny(
                                                        error,
                                                        RestConstants.CONTENT_QUOTA_EXCEPTION,
                                                    )
                                                ) {
                                                    this.nodeApi
                                                        .deleteNode(data.node.ref.id, false)
                                                        .subscribe(() => {
                                                            observer.complete();
                                                        });
                                                    this.toast.error(
                                                        null,
                                                        'MDS.ADD_CHILD_OBJECT_QUOTA_REACHED',
                                                        {
                                                            name: child.name,
                                                        },
                                                    );
                                                }
                                            },
                                        );
                                });
                        } else if (child.link) {
                            let properties: any = {};
                            properties[RestConstants.CCM_PROP_IO_WWWURL] = [child.link];
                            this.nodeApi
                                .createNode(
                                    nodes[0].ref.id,
                                    RestConstants.CCM_TYPE_IO,
                                    [RestConstants.CCM_ASPECT_IO_CHILDOBJECT],
                                    this.getProperties(child),
                                    true,
                                    RestConstants.COMMENT_MAIN_FILE_UPLOAD,
                                    RestConstants.CCM_ASSOC_CHILDIO,
                                )
                                .subscribe(() => {
                                    observer.complete();
                                });
                        } else {
                            this.nodeApi
                                .editNodeMetadata(child.node.ref.id, this.getProperties(child))
                                .subscribe(() => {
                                    observer.complete();
                                });
                        }
                    }),
            ),
        ).toPromise();
        await this.deleteChildren();
        return nodes;
    }

    drop(event: CdkDragDrop<string[]>) {
        moveItemInArray(this.children, event.previousIndex, event.currentIndex);
        this.onChange();
    }

    private async deleteChildren() {
        if (this.childrenDelete.length) {
            await observableForkJoin(
                this.childrenDelete.map((node) => this.nodeApi.deleteNode(node.ref.id, false)),
            ).toPromise();
        }
    }
}
