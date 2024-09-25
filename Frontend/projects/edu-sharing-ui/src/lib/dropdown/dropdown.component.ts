import { Component, Input, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { MatMenu, MatMenuTrigger } from '@angular/material/menu';
import { OptionItem } from '../types/option-item';
import { Helper } from '../util/helper';
import { UIService } from '../services/ui.service';
import { BehaviorSubject } from 'rxjs';

/**
 * The dropdown is one base component of the action bar (showing more actions),
 * but can also be used standalone.
 */
@Component({
    selector: 'es-dropdown',
    templateUrl: 'dropdown.component.html',
    styleUrls: ['dropdown.component.scss'],
})
export class DropdownComponent implements OnChanges {
    @ViewChild('dropdown', { static: true }) menu: MatMenu;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;

    @Input() position: 'left' | 'right' = 'left';
    @Input() options: OptionItem[];
    options$ = new BehaviorSubject<OptionItem[]>([]);

    /**
     * The object that should be returned via the option's callback.
     *
     * Can be null
     */
    @Input() callbackObject: any;

    /**
     * Should disabled ("greyed out") options be shown or hidden?
     */
    @Input() showDisabled = true;

    /**
     * An additional class to add to the `mat-menu` instance.
     *
     * This is needed to customize the menu styling since the menu contents are
     * taken out of the host container by angular.
     */
    @Input() menuClass: string;

    constructor(private ui: UIService) {}

    ngOnChanges(changes?: SimpleChanges): void {
        if (changes == null || changes?.options || changes?.callbackObject) {
            this.options$.next(this.ui.filterValidOptions(Helper.deepCopyArray(this.options)));
            if (this.callbackObject) {
                this.ui.updateOptionEnabledState(this.options$, this.callbackObject);
            }
        }
    }

    click(option: OptionItem) {
        if (!option.isEnabled) {
            return;
        }
        setTimeout(() => option.callback(this.callbackObject));
    }

    isNewGroup(i: number) {
        if (i > 0) {
            return this.options$.value[i].group !== this.options$.value[i - 1].group;
        }
        return false;
    }

    /** Whether there are any enabled options so we can open the menu. */
    canShowDropdown(): boolean {
        // We can only open the dropdown menu, when there is at least one enabled option. Even when
        // there are options with `showDisabled: true`, showing a menu with no selectable option
        // causes a11y issues.
        return this.options$.value?.some((o) => o.isEnabled);
    }
}
