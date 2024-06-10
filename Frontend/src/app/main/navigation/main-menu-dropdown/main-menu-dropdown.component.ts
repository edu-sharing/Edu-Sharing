import {
    AfterViewInit,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    OnDestroy,
    Output,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { ConfigEntry } from '../../../services/node-helper.service';
import { MainMenuEntriesService } from '../main-menu-entries.service';
import { DropdownComponent, OptionItem } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-main-menu-dropdown',
    templateUrl: './main-menu-dropdown.component.html',
    styleUrls: ['./main-menu-dropdown.component.scss'],
})
export class MainMenuDropdownComponent implements OnChanges, AfterViewInit, OnDestroy {
    @ViewChild('dropdown', { static: true }) dropdown: DropdownComponent;

    @Input() currentScope: string;

    private readonly destroyed$ = new Subject();
    optionItems$: Observable<OptionItem[]>;
    @Output() onClose = new EventEmitter<void>();

    constructor(private mainMenuEntries: MainMenuEntriesService) {}

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    ngAfterViewInit(): void {
        this.dropdown.menu.closed
            .pipe(takeUntil(this.destroyed$))
            .subscribe(() => this.onClose.emit());
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.currentScope) {
            this.setOptionItems();
        }
    }

    private setOptionItems() {
        this.optionItems$ = this.mainMenuEntries.entries$.pipe(
            map((entries) => this.toOptionItems(entries)),
        );
    }

    private toOptionItems(entries: ConfigEntry[]): OptionItem[] {
        return entries.map((entry) => {
            const optionItem = new OptionItem(entry.name, entry.icon, entry.open);
            optionItem.isSeparate = entry.isSeparate;
            optionItem.isEnabled = !entry.isDisabled;
            optionItem.isSelected = this.currentScope === entry.scope;
            return optionItem;
        });
    }
}
