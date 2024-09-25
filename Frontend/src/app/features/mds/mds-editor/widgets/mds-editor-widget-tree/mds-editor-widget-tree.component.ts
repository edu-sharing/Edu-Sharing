import { CdkConnectedOverlay, ConnectedPosition } from '@angular/cdk/overlay';
import {
    AfterViewInit,
    ChangeDetectorRef,
    Component,
    ElementRef,
    OnDestroy,
    OnInit,
    QueryList,
    ViewChild,
    ViewChildren,
} from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { BehaviorSubject, ReplaySubject } from 'rxjs';
import { startWith } from 'rxjs/operators';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';
import { MdsWidget, MdsWidgetType } from '../../../types/types';
import { DisplayValue } from '../DisplayValues';
import {
    MdsEditorWidgetBase,
    MdsEditorWidgetChipsSuggestionBase,
    ValueType,
} from '../mds-editor-widget-base';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { Tree } from './tree';
import { MatChip, MatChipOption, MatChipRow } from '@angular/material/chips';
import { UIService } from '../../../../../core-module/rest/services/ui.service';
import { MatButton } from '@angular/material/button';
import { UIHelper } from '../../../../../core-ui-module/ui-helper';
import { MdsService } from 'ngx-edu-sharing-api';

@Component({
    selector: 'es-mds-editor-widget-tree',
    templateUrl: './mds-editor-widget-tree.component.html',
    styleUrls: ['./mds-editor-widget-tree.component.scss'],
})
export class MdsEditorWidgetTreeComponent
    extends MdsEditorWidgetChipsSuggestionBase
    implements OnInit, AfterViewInit, OnDestroy
{
    add(value: DisplayValue): void {
        const treeNode = this.tree.findById(value.key);
        // old values are may not available in tree, so check for null
        if (treeNode) {
            treeNode.isChecked = true;
            treeNode.isIndeterminate = false;
        }
        const values: DisplayValue[] = this.chipsControl.value;
        this.chipsControl.setValue([...values, value]);
        this.preventOverlayOpen = true;
        setTimeout(() => {
            this.preventOverlayOpen = false;
        });
    }
    toDisplayValue(value: string): DisplayValue {
        return this.tree.toDisplayValue(value);
    }
    @ViewChild(CdkConnectedOverlay) overlay: CdkConnectedOverlay;
    @ViewChild('chipList', { read: ElementRef }) chipList: ElementRef<HTMLElement>;
    @ViewChild('treeRef') treeRef: MdsEditorWidgetTreeCoreComponent;
    @ViewChild('openButton') openButtonRef: MatButton;
    @ViewChild('inputElement') inputElement: ElementRef<HTMLInputElement>;
    @ViewChild('box') boxRef: ElementRef<HTMLElement>;
    @ViewChild(MdsEditorWidgetTreeCoreComponent)
    treeCoreComponent: MdsEditorWidgetTreeCoreComponent;
    @ViewChildren('chip') chips: QueryList<MatChipRow>;

    valueType: ValueType;
    tree: Tree;
    indeterminateValues$: BehaviorSubject<string[]>;
    overlayIsVisible = false;
    /**
     * Briefly set to `true` in situations where the input field might get focus as result of a
     * user's action, but we don't want to open the overlay.
     */
    preventOverlayOpen = false;
    readonly overlayPositions: ConnectedPosition[] = [
        {
            originX: 'start',
            originY: 'bottom',
            offsetX: 0,
            offsetY: -34,
            overlayX: 'start',
            overlayY: 'top',
        },
        {
            originX: 'start',
            originY: 'top',
            offsetX: 0,
            offsetY: 0,
            overlayX: 'start',
            overlayY: 'bottom',
        },
    ];

    private destroyed$: ReplaySubject<boolean> = new ReplaySubject(1);

    constructor(
        mdsEditorInstance: MdsEditorInstanceService,
        translate: TranslateService,
        private changeDetectorRef: ChangeDetectorRef,
        public uiService: UIService,
    ) {
        super(mdsEditorInstance, translate);
    }

    async ngOnInit() {
        this.chipsControl = new UntypedFormControl(null, this.getStandardValidators());
        if (this.widget.definition.type === MdsWidgetType.SingleValueTree) {
            this.valueType = ValueType.String;
        } else if (this.widget.definition.type === MdsWidgetType.MultiValueTree) {
            this.valueType = ValueType.MultiValue;
        } else {
            throw new Error('Unexpected widget type: ' + this.widget.definition.type);
        }
        this.tree = Tree.generateTree(
            this.widget.definition.values,
            (await this.widget.getInitalValuesAsync()).jointValues ?? [],
            (await this.widget.getInitalValuesAsync()).individualValues,
        );
        super.initSuggestions();
        this.chipsControl = new UntypedFormControl(
            [
                ...((await this.widget.getInitalValuesAsync()).jointValues ?? []),
                ...((await this.widget.getInitalValuesAsync()).individualValues ?? []),
            ].map((value) => this.tree.toDisplayValue(value)),
            this.getStandardValidators(),
        );
        this.indeterminateValues$ = new BehaviorSubject(
            (await this.widget.getInitalValuesAsync()).individualValues,
        );
        this.chipsControl.valueChanges.subscribe((values: DisplayValue[]) => {
            // temporary hack if you want to apply all
            // this.setValue(values.map((value) => value.key).concat(MdsService.unfoldTreeChilds(values.map((value) => value.key), this.widget.definition)));
            this.setValue(values.map((value) => value.key));
        });
        this.indeterminateValues$.subscribe((indeterminateValues) =>
            this.widget.setIndeterminateValues(indeterminateValues),
        );
        this.registerValueChanges(this.chipsControl);
    }

    ngAfterViewInit(): void {
        // We mark all chips as selected for better screen-reader output. However, since selection
        // doesn't do anything, we disable toggling the selection.
        this.chips.changes
            .pipe(startWith(this.chips))
            .subscribe((chips: QueryList<MatChipOption>) =>
                chips.forEach((chip) => (chip.toggleSelected = () => true)),
            );
    }

    ngOnDestroy() {
        this.destroyed$.next(true);
        this.destroyed$.complete();
    }

    revealInTree(value: DisplayValue): void {
        this.openOverlay();
        setTimeout(() => {
            this.treeCoreComponent.revealInTree(this.tree.findById(value.key));
        });
    }
    focus() {
        this.openOverlay();
    }
    openOverlay(event?: FocusEvent): void {
        if (this.chipsControl.disabled) {
            return;
        }
        if (!event) {
            if (this.overlayIsVisible) {
                this.overlayIsVisible = false;
                this.changeDetectorRef.detectChanges();
                setTimeout(() => (document.activeElement as HTMLElement)?.blur());
                return;
            }
        }
        if (this.overlayIsVisible) {
            this.treeRef.input.nativeElement.focus();
            return;
        }
        this.overlayIsVisible = true;
        this.changeDetectorRef.detectChanges();
        setTimeout(() => this.treeRef.input.nativeElement.focus());
    }

    closeOverlay(event?: FocusEvent): void {
        // prevent directly closing because cdk outside click might trigger
        if (
            UIHelper.isParentElementOfElement(
                event?.target as HTMLElement,
                this.boxRef.nativeElement,
            )
        ) {
            return;
        }
        this.overlayIsVisible = false;
        this.openButtonRef.focus();
        this.onBlur.emit();
    }

    onOverlayKeydown(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            event.stopPropagation();
            this.closeOverlay();
        } else {
            const wasHandledByTree = this.treeCoreComponent.handleKeydown(event.code);
            if (wasHandledByTree) {
                event.preventDefault();
            }
        }
    }

    remove(toBeRemoved: DisplayValue): void {
        const treeNode = this.tree.findById(toBeRemoved.key);
        // old values are may not available in tree, so check for null
        if (treeNode) {
            treeNode.isChecked = false;
            treeNode.isIndeterminate = false;
        }
        const values: DisplayValue[] = this.chipsControl.value;
        if (values.includes(toBeRemoved)) {
            this.chipsControl.setValue(values.filter((value) => value !== toBeRemoved));
        }
        if (this.indeterminateValues$.value?.includes(toBeRemoved.key)) {
            this.indeterminateValues$.next(
                this.indeterminateValues$.value.filter((value) => value !== toBeRemoved.key),
            );
        }
        this.preventOverlayOpen = true;
        setTimeout(() => {
            this.preventOverlayOpen = false;
        });
    }

    onValuesChange(values: DisplayValue[]): void {
        this.chipsControl.setValue(values);
        this.changeDetectorRef.detectChanges();
    }

    blur(event: FocusEvent) {
        if (event.relatedTarget === this.treeRef.input.nativeElement) {
            return;
        }
        this.onBlur.emit();
    }
    public static mapGraphqlId(definition: MdsWidget) {
        // attach the "RangedValue" graphql Attributes
        return MdsEditorWidgetBase.attachGraphqlSelection(definition, ['id', 'value']);
    }
}
