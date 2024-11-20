import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable()
export class ViewInstanceService {
    readonly isExpanded$ = new BehaviorSubject(true);
    /**
     * The heading level from 1 to 6 to use for widget labels, equivalent to `h1` to `h6`.
     *
     * If not set, widget labels are not marked as headings and an invisible colon is added between
     * labels and values, that will be read out by screen readers.
     */
    headingLevel: number | null = null;
    /**
     * how to display tree entries
     * flat (only display the last entry)
     * full (display the full tree path)
     */
    treeDisplay: 'flat' | 'path' = 'flat';
}
