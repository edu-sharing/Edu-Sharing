import { FocusKeyManager } from '@angular/cdk/a11y';
import { DOWN_ARROW, ESCAPE, UP_ARROW } from '@angular/cdk/keycodes';
import {
    AfterViewInit,
    Component,
    EventEmitter,
    HostListener,
    Input,
    OnDestroy,
    Output,
    QueryList,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { MatSlideToggle, MatSlideToggleChange } from '@angular/material/slide-toggle';
import * as rxjs from 'rxjs';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { FocusableOptionDirective } from './focusable-option.directive';

export type TypeResult = {
    permissions: string[];
    wasMain: boolean;
};
@Component({
    selector: 'es-share-dialog-choose-type',
    templateUrl: 'choose-type.component.html',
    styleUrls: ['choose-type.component.scss'],
})
export class ShareDialogChooseTypeComponent implements AfterViewInit, OnDestroy {
    @ViewChildren(FocusableOptionDirective)
    _menuItems: QueryList<FocusableOptionDirective>;
    @ViewChild('slideToggle') _slideToggle: MatSlideToggle;

    @Input() set selected(selected: string[]) {
        this._selected = selected;
        setTimeout(() => this._checkPublish(), 10);
    }
    @Input() isDirectory = false;
    @Input() canPublish = true;
    @Input('aria-label') ariaLabel: string;

    @Output() cancelType = new EventEmitter<void>();
    @Output() typeChosen = new EventEmitter<TypeResult>();

    readonly _options = [
        {
            id: 'Consumer',
            label: 'WORKSPACE.SHARE.TYPE_VIEWER',
            icon: 'remove_red_eye',
            description: 'WORKSPACE.SHARE.TYPE_VIEWER_DESCRIPTION',
        },
        {
            id: 'Collaborator',
            label: 'WORKSPACE.SHARE.TYPE_COWORKER',
            icon: 'edit',
            description: 'WORKSPACE.SHARE.TYPE_COWORKER_DESCRIPTION',
        },
        {
            id: 'Coordinator',
            label: 'WORKSPACE.SHARE.TYPE_COORDINATOR',
            icon: 'work',
            description: 'WORKSPACE.SHARE.TYPE_COORDINATOR_DESCRIPTION',
        },
    ];

    _publish: boolean;
    _publishDisabled: boolean;

    private _selected: string[];
    private _keyManager: FocusKeyManager<FocusableOptionDirective>;
    private _destroyed$ = new Subject<void>();

    ngAfterViewInit() {
        // angular 17 change
        this._slideToggle._switchElement.nativeElement.setAttribute('role', 'menuitemcheckbox');
        // this._slideToggle._inputElement.nativeElement.setAttribute('role', 'menuitemcheckbox');
        this._keyManager = new FocusKeyManager(this._menuItems).withWrap();
        this._keyManager.tabOut.pipe(takeUntil(this._destroyed$)).subscribe(() => this._cancel());
        this._keyManager.setActiveItem(this._getActiveIndex());
        // Update the key manager when an option is focused by other means by the user, e.g. via
        // mouse.
        rxjs.merge(...this._menuItems.map((item) => item.focused)).subscribe((item) =>
            this._keyManager.updateActiveItem(item),
        );
    }

    ngOnDestroy() {
        this._destroyed$.next();
        this._destroyed$.complete();
    }

    @HostListener('keydown', ['$event'])
    _handleKeydown(event: KeyboardEvent) {
        const keyCode = event.keyCode;
        const manager = this._keyManager;
        switch (keyCode) {
            case ESCAPE:
                event.preventDefault();
                event.stopPropagation();
                this._cancel();
                break;
            default:
                if (keyCode === UP_ARROW || keyCode === DOWN_ARROW) {
                    manager.setFocusOrigin('keyboard');
                }
                manager.onKeydown(event);
                return;
        }
    }

    _cancel() {
        this.cancelType.emit();
    }

    _setType(type: string) {
        for (const { id } of this._options) {
            if (this._contains(id)) {
                this._selected.splice(this._selected.indexOf(id), 1);
            }
        }
        this._selected.push(type);
        setTimeout(() => this._checkPublish(), 10);
        this.typeChosen.emit({ permissions: this._selected, wasMain: true });
    }

    _contains(type: string) {
        return this._selected.indexOf(type) != -1;
    }

    private _checkPublish() {
        this._publish = this._contains('CCPublish');
        this._publishDisabled = this._contains('Coordinator');
        if (this._contains('Coordinator')) {
            this._publish = true;
        }
    }

    _setPublish(event: MatSlideToggleChange) {
        if (event.checked) {
            if (this._contains('CCPublish')) return;
            this._selected.push('CCPublish');
        } else {
            if (!this._contains('CCPublish')) return;
            this._selected.splice(this._selected.indexOf('CCPublish'), 1);
        }
        this.typeChosen.emit({ permissions: this._selected, wasMain: false });
    }

    private _getActiveIndex(): number {
        return this._options.findIndex((option) => this._selected.includes(option.id));
    }
}
