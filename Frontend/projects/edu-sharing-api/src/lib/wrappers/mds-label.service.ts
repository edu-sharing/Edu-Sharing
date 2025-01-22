import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MdsValue } from '../api/models';
import { MdsIdentifier, MdsService } from './mds.service';

export interface LabeledValue {
    value: string;
    label: string;
    mdsValue?: MdsValue;
}

export type LabeledValuesDict = { [property: string]: LabeledValue[] };
export type RawValuesDict = { [property: string]: string[] };

/**
 * Enriches MDS values with translated labels, that are looked up on the MDS widget definitions.
 */
@Injectable({
    providedIn: 'root',
})
export class MdsLabelService {
    constructor(private mds: MdsService) {}

    /** Converts a dictionary of raw value arrays to a dictionary of labeled value arrays. */
    labelValuesDict(mdsId: MdsIdentifier, values: RawValuesDict): Observable<LabeledValuesDict> {
        const entries = Object.entries(values);
        if (entries.length === 0) {
            return rxjs.of({});
        }
        return rxjs.forkJoin(
            entries.reduce((acc, [property, values]) => {
                acc[property] = this.labelValues(mdsId, property, values);
                return acc;
            }, {} as { [property: string]: Observable<LabeledValue[]> }),
        );
    }

    /**
     * Converts a dictionary of labeled value arrays to a dictionary of raw value arrays.
     *
     * Reverses `labelValuesDict`.
     */
    getRawValuesDict(labeledValuesDict: LabeledValuesDict): RawValuesDict {
        return Object.entries(labeledValuesDict).reduce((acc, [property, labeledValues]) => {
            acc[property] = labeledValues.map(({ value }) => value);
            return acc;
        }, {} as { [property: string]: string[] });
    }

    /** Converts an array of raw values to an array of labeled values. */
    labelValues(
        mdsId: MdsIdentifier,
        property: string,
        values: string[],
    ): Observable<LabeledValue[]> {
        if (!values || values.length === 0) {
            return rxjs.of([]);
        }
        return rxjs.forkJoin(
            values.map((value) =>
                this.getLabel(mdsId, property, value).pipe(map((label) => ({ value, ...label }))),
            ),
        );
    }

    /** Gets a label for a single value. */
    getLabel(
        mdsId: MdsIdentifier,
        property: string,
        value: string,
    ): Observable<{
        mdsValue: MdsValue | undefined;
        label: string;
    }> {
        return this.findValueById(mdsId, property, value).pipe(
            map((mdsValue) => {
                return { label: mdsValue?.caption ?? value, mdsValue };
            }),
        );
    }

    /** Returns the first mds-value definition for the given id. */
    findValueById(
        mdsId: MdsIdentifier,
        property: string,
        id: string,
    ): Observable<MdsValue | undefined> {
        return this.findValue(
            mdsId,
            property,
            (value) => value.id === id || value.alternativeIds?.includes(id),
        );
    }

    /** Returns the first mds-value definition fulfilling the predicate. */
    findValue(
        mdsId: MdsIdentifier,
        property: string,
        predicate: (value: MdsValue) => unknown,
    ): Observable<MdsValue | undefined> {
        return this.getValueDefinitions(mdsId, property).pipe(
            map((definitions) => definitions?.find(predicate)),
        );
    }

    /**
     * Gets values for a given property from the respective mds widget definitions.
     */
    getValueDefinitions(mdsId: MdsIdentifier, property: string): Observable<MdsValue[] | null> {
        return this.mds.getMetadataSet(mdsId).pipe(
            map(
                (mds) =>
                    mds.widgets?.find(
                        (widget) =>
                            widget.id === property &&
                            // Values are defined on the general widget and not on special
                            // configurations for specific view.
                            !widget.template,
                    )?.values ?? null,
            ),
        );
    }
}
