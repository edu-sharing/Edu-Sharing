/** This component is also used in extensions and therefore shared! */
import { Component, Input } from '@angular/core';
import { ListItem } from 'ngx-edu-sharing-ui';
import { Node } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-small-collection',
    templateUrl: 'small-collection.component.html',
    styleUrls: ['small-collection.component.scss'],
})
/**
 * This component ~~uses the same height as the secondary bar height and~~ can be used to display a
 * collection at this position
 */
export class SmallCollectionComponent {
    /**
     * Custom title rendering.
     *
     * Use {{title}} in your string to replace it with the title
     */
    @Input() titleLabel: string;
    /**
     * Custom title rendering for mobile / small layout.
     *
     * Use {{title}} in your string to replace it with the title.
     */
    @Input() titleLabelShort: string;
    @Input() collection: Node;

    readonly scopeItem = new ListItem('COLLECTION', 'scope');
}
