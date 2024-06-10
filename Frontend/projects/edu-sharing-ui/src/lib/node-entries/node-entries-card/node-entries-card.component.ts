import {
    ApplicationRef,
    Component,
    Input,
    OnChanges,
    OnInit,
    Optional,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { MatMenuTrigger } from '@angular/material/menu';
import { ClickSource, InteractionType } from '../entries-model';
import { NodeEntriesTemplatesService } from '../node-entries-templates.service';
import { CustomFieldSpecialType, NodeEntriesGlobalService } from '../node-entries-global.service';
import { Target } from '../../types/option-item';
import { NodeEntriesService } from '../../services/node-entries.service';
import { NodeHelperService } from '../../services/node-helper.service';
import { AuthenticationService, ConfigService, Node, RestConstants } from 'ngx-edu-sharing-api';
import { ColorHelper, PreferredColor } from '../../util/color-helper';
import { take } from 'rxjs/operators';
import { DropdownComponent } from '../../dropdown/dropdown.component';
import { Toast } from '../../services/abstract/toast.service';
import { BehaviorSubject } from 'rxjs';

@Component({
    selector: 'es-node-entries-card',
    templateUrl: 'node-entries-card.component.html',
    styleUrls: ['node-entries-card.component.scss'],
})
export class NodeEntriesCardComponent<T extends Node> implements OnChanges, OnInit {
    readonly InteractionType = InteractionType;
    readonly Target = Target;
    readonly ClickSource = ClickSource;
    readonly CustomFieldSpecialType = CustomFieldSpecialType;
    @Input() dropdown: DropdownComponent;

    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    @Input() node: T;
    dropdownLeft: number;
    dropdownTop: number;
    showRatings = new BehaviorSubject(false);
    isCollection: boolean;
    constructor(
        public entriesService: NodeEntriesService<T>,
        public nodeHelper: NodeHelperService,
        public applicationRef: ApplicationRef,
        public configService: ConfigService,
        public authenticationService: AuthenticationService,
        public templatesService: NodeEntriesTemplatesService,
        public nodeEntriesGlobalService: NodeEntriesGlobalService,
        @Optional() private toast: Toast,
    ) {}

    ngOnChanges(changes: SimpleChanges): void {
        this.isCollection = this.nodeHelper.isNodeCollection(changes.node?.currentValue);
    }

    getTextColor() {
        return ColorHelper.getPreferredColor(this.node.collection.color) === PreferredColor.Black
            ? '#000'
            : '#fff';
    }
    optionsOnCard() {
        const options = this.entriesService.options?.[Target.List];
        const always = options?.filter((o) => o.showAlways);
        if (always?.some((o) => o.showCallback(this.node))) {
            return always;
        }
        // we do NOT show any additional actions
        return [];
        // return options.filter((o) => o.showAsAction && o.showCallback(this.node)).slice(0, 3);
    }

    openContextmenu(event: MouseEvent | Event) {
        event.stopPropagation();
        event.preventDefault();
        if (!this.dropdown) {
            // Call `preventDefault()` even when there is no menu, so we can use `cdkDrag` with a
            // start delay without being interrupted by the standard long-tap action.
            return;
        }
        if (event instanceof MouseEvent) {
            ({ clientX: this.dropdownLeft, clientY: this.dropdownTop } = event);
        } else {
            ({ x: this.dropdownLeft, y: this.dropdownTop } = (
                event.target as HTMLElement
            ).getBoundingClientRect());
        }
        if (!this.entriesService.selection.selected.includes(this.node)) {
            this.entriesService.selection.clear();
            this.entriesService.selection.select(this.node);
        }
        // Wait for the menu to reflect changed options.
        setTimeout(() => {
            if (this.dropdown.canShowDropdown()) {
                this.menuTrigger.openMenu();
            } else {
                this.toast.toast('NO_AVAILABLE_OPTIONS');
            }
        });
    }

    getVisibleColumns() {
        return this.entriesService.columns?.filter((c) => c.visible);
    }

    async openMenu(node: T) {
        this.entriesService.selection.clear();
        this.entriesService.selection.select(node);
        this.entriesService.selection.clickSource = ClickSource.Dropdown;
        await this.applicationRef.tick();
        this.dropdown.menu.focusFirstItem();
    }

    async ngOnInit() {
        await this.configService.observeConfig().pipe(take(1)).toPromise();
        this.showRatings.next(
            this.configService.instant('rating.mode', 'none') !== 'none' &&
                (await this.authenticationService.hasToolpermission(
                    RestConstants.TOOLPERMISSION_RATE_READ,
                )),
        );
    }

    getTemplate(name: CustomFieldSpecialType) {
        return this.nodeEntriesGlobalService.getCustomFieldTemplate(
            {
                type: 'NODE',
                name,
            },
            this.node as Node,
        );
    }
}
