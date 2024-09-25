import { Injectable } from '@angular/core';
import { MdsHelperService } from '../mds/mds-helper.service';
import { MdsService, MdsWidget } from 'ngx-edu-sharing-api';

export type Values = { [p: string]: string[] };

@Injectable({
    providedIn: 'root',
})
export class SearchHelperService {
    static readonly MAX_QUERY_CONCAT_PARAMS = 400;

    constructor() {}

    /**
     * converts given filter criteria and converts them for a body to be used for a search request
     * @param properties
     * @param mdsWidgets
     * @param unfoldTrees
     */
    convertCritieria(properties: Values, mdsWidgets: MdsWidget[], unfoldTrees = true) {
        const criteria: {
            property: string;
            values: string[];
        }[] = [];
        // deep copy
        properties = JSON.parse(JSON.stringify(properties));
        for (const property in properties) {
            let widget = MdsHelperService.getWidget(property, undefined, mdsWidgets);
            if (widget && widget.type == 'multivalueTree' && unfoldTrees) {
                let attach = MdsService.unfoldTreeChilds(properties[property], widget);
                if (attach) {
                    if (attach.length > SearchHelperService.MAX_QUERY_CONCAT_PARAMS) {
                        console.info(
                            'param ' +
                                property +
                                ' has too many unfold childs (' +
                                attach.length +
                                '), falling back to basic prefix-based search',
                        );
                    } else {
                        properties[property] = properties[property].concat(attach);
                    }
                }
            }
            if (properties[property]?.length && properties[property].every((p) => p != null && !!p))
                criteria.push({
                    property: property,
                    values: properties[property],
                });
        }
        return criteria;
    }
}
