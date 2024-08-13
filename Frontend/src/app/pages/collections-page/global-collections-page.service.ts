import { Injectable, TemplateRef } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/**
 * Custom templates to replace or extend standard components within the search page.
 */
export interface CollectionsPageCustomTemplates {
    belowTabs?: TemplateRef<unknown>;
}

/**
 * Singleton service for public interfacing with the collections page.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalCollectionsPageService {
    constructor(private internal: GlobalCollectionsPageServiceInternal) {}

    /**
     * Register custom templates to replace or extend standard components within the search page.
     */
    setCustomTemplates(customTemplates: CollectionsPageCustomTemplates): void {
        this.internal.customTemplates.next(customTemplates);
    }
}

/**
 * Internal part of the `GlobalSearchPageService` for use within the search page component and
 * services only.
 */
@Injectable({
    providedIn: 'root',
})
export class GlobalCollectionsPageServiceInternal {
    readonly customTemplates = new BehaviorSubject<CollectionsPageCustomTemplates>({});
}
