import { TranslateService } from '@ngx-translate/core';
import {
    AuthenticationService,
    MdsDefinition,
    MdsSort,
    MdsWidget,
    RestConstants,
    Sort,
} from 'ngx-edu-sharing-api';
import { Injectable } from '@angular/core';
import { ListItem, ListItemType } from '../types/list-item';

@Injectable()
export class MdsHelperService {
    static getSortInfo(mdsSet: MdsDefinition, name: string): MdsSort {
        if (mdsSet) {
            if (mdsSet.sorts) {
                for (const list of mdsSet.sorts) {
                    if (list.id == name) {
                        return list;
                    }
                }
            }
            console.error('mds does not define sort info for ' + name + ', invalid configuration!');
        }
        return null;
    }
    static getColumns(translate: TranslateService, mdsSet: any, name: string) {
        let columns: ListItem[] = [];
        if (mdsSet) {
            for (const list of mdsSet.lists) {
                if (list.id === name) {
                    for (const column of list.columns) {
                        let type: ListItemType = 'NODE';
                        if (name === 'mediacenterGroups') {
                            type = 'GROUP';
                        } else if (name === 'searchCollections') {
                            type = 'COLLECTION';
                        }
                        // in this case, the type is included
                        if (column.id.includes('.')) {
                            const split = column.id.split('.');
                            type = split[0];
                            column.id = split[1];
                        }
                        const item = new ListItem(type, column.id);
                        item.format = column.format;
                        columns.push(item);
                    }
                    break;
                }
            }
        }
        if (!columns.length) {
            if (mdsSet !== null) {
                console.warn(
                    'mds does not define columns for ' + name + ', invalid configuration!',
                );
            }
            if (name === 'search' || name === 'collectionReferences') {
                columns.push(new ListItem('NODE', RestConstants.LOM_PROP_TITLE));
                columns.push(new ListItem('NODE', RestConstants.CM_MODIFIED_DATE));
                columns.push(new ListItem('NODE', RestConstants.CCM_PROP_LICENSE));
                columns.push(new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCE));
            } else if (name === 'mediacenterManaged') {
                columns.push(new ListItem('NODE', RestConstants.LOM_PROP_TITLE));
                columns.push(new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCEID));
                columns.push(new ListItem('NODE', RestConstants.CCM_PROP_REPLICATIONSOURCE));
            } else if (name === 'mediacenterGroups') {
                columns.push(new ListItem('GROUP', RestConstants.AUTHORITY_DISPLAYNAME));
                columns.push(new ListItem('GROUP', RestConstants.AUTHORITY_GROUPTYPE));
            } else if (name === 'searchCollections') {
                columns.push(new ListItem('COLLECTION', 'title'));
                columns.push(new ListItem('COLLECTION', 'info'));
                columns.push(new ListItem('COLLECTION', 'scope'));
            }
        }
        columns.map((c) => {
            const key = c.type + '.' + c.name;
            if (c.type === 'NODE' && translate.instant(key) === key) {
                c.label = mdsSet.widgets.filter((w: any) => w.id === c.name)?.[0]?.caption;
            }
            return c;
        });
        return columns;
    }

    /**
     * Finds the appropriate widget with the id, but will not check any widget conditions
     * @param cid
     * @param template
     * @param widgets
     */
    static getWidget(cid: string, template: string | undefined | null, widgets: MdsWidget[]) {
        if (widgets == null) {
            console.warn('Could not iterate widget ' + cid + ': no widgets data provided');
            return null;
        }
        for (let w of widgets) {
            if (w.id == cid) {
                if (template === undefined || w.template === template) {
                    return w;
                }
            }
        }
        return null;
    }

    constructor(private authentication: AuthenticationService) {}

    /**
     * Same as getWidget, but will also check the widget conditions
     * @param connector
     * @param properties
     * @param id
     * @param template
     * @param widgets
     */
    async getWidgetWithCondition(
        properties: any,
        id: string,
        template: string = null,
        widgets: any,
    ) {
        for (let w of widgets) {
            if (w.id == id) {
                if (
                    (template == null || w.template == template) &&
                    (await this.isWidgetConditionTrue(w, properties))
                ) {
                    return w;
                }
            }
        }
        return null;
    }

    async isWidgetConditionTrue(widget: any, properties: any) {
        if (!widget.condition) return true;
        let condition = widget.condition;
        if (condition.type == 'PROPERTY' && properties) {
            if (
                (!properties[condition.value] && !condition.negate) ||
                (properties[condition.value] && condition.negate)
            ) {
                return false;
            }
        }
        if (condition.type == 'TOOLPERMISSION') {
            let tp = await this.authentication.hasToolpermission(condition.value);
            if (tp == condition.negate) {
                return false;
            }
        }
        return true;
    }
    /**
     * Find a template by id in the given mds
     */
    static findTemplate(mds: MdsDefinition, id: string) {
        return (
            mds.views as Array<
                ArrayElement<MdsDefinition['views']> | ArrayElement<MdsDefinition['views']>
            >
        ).find((v) => v.id === id);
    }
    /**
     * Returns all widgets used by the given template
     */
    static getUsedWidgets(mds: MdsDefinition, template: string = null): any[] {
        const used: any = [];
        const templateData = MdsHelperService.findTemplate(mds, template);
        for (const w of mds.widgets) {
            if (
                templateData.html.indexOf('<' + w.id) !== -1 &&
                !used.find((w2: any) => w2.id === w.id)
            ) {
                used.push(w);
            }
        }
        return used;
    }
}

type ArrayElement<ArrayType extends readonly unknown[]> =
    ArrayType extends readonly (infer ElementType)[] ? ElementType : never;
