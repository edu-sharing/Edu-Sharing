import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Target } from '../../types/option-item';
import { ClickSource, InteractionType } from '../entries-model';

import { NodeEntriesTemplatesService } from '../node-entries-templates.service';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { Node } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-node-entries-card-small',
    templateUrl: 'node-entries-card-small.component.html',
    styleUrls: ['node-entries-card-small.component.scss'],
})
export class NodeEntriesCardSmallComponent<T extends Node> implements OnChanges {
    readonly ClickSource = ClickSource;
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    @Input() node: T;
    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
        public templatesService: NodeEntriesTemplatesService,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {}
    optionsOnCard() {
        const options = this.entriesService.options[Target.List];
        const always = options.filter((o) => o.showAlways);
        if (always.some((o) => o.showCallback([this.node]))) {
            return always;
        }
        return options.filter((o) => o.showAsAction && o.showCallback([this.node])).slice(0, 3);
    }

    openContextmenu(event: MouseEvent | Event) {
        event.preventDefault();
        event.stopPropagation();
    }
}
