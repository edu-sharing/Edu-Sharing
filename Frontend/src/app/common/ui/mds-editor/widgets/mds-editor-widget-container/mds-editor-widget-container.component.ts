import { animate, group, state, style, transition, trigger } from '@angular/animations';
import {
    AfterContentInit,
    ChangeDetectorRef,
    Component,
    ContentChild,
    Directive,
    ElementRef,
    HostBinding,
    Injectable,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { MatRipple } from '@angular/material/core';
import { MatFormField, MatFormFieldControl } from '@angular/material/form-field';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { BehaviorSubject, combineLatest, Subject } from 'rxjs';
import { distinctUntilChanged, map, startWith, takeUntil } from 'rxjs/operators';
import { UIAnimation } from '../../../../../core-module/ui/ui-animation';
import { BulkBehavior } from '../../../mds/mds.component';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { NativeWidgetComponent } from '../../mds-editor-view/mds-editor-view.component';
import { BulkMode, EditorBulkMode, InputStatus, RequiredMode } from '../../types';
import { ViewInstanceService } from '../../mds-editor-view/view-instance.service';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';

// This is a Service-Directive combination to get hold of the `MatFormField` before it initializes
// its `FormFieldControl`.
//
// This is needed so we can pass inputs (representing a `FormFieldControl`) through `<ng-content>`
// into a `MatFormField` (see https://github.com/angular/components/issues/9411).
//
// Initialization of the `FormFieldControl` happens on `ngAfterContentInit` in `MatFormField`, so we
// will be too late if we were to do this on `ngAfterViewInit`. Therefore, we hook into
// `MatFormField` with a Directive and register our `FormFieldControl` in the constructor, which is
// early enough to be picked up.
@Injectable()
export class FormFieldRegistrationService {
    private callback: (formField: MatFormField) => void;

    register(formField: MatFormField): void {
        this.callback(formField);
    }

    onRegister(callback: (formField: MatFormField) => void): void {
        this.callback = callback;
    }
}

@Directive({
    selector: '[appRegisterFormField]',
})
export class RegisterFormFieldDirective {
    constructor(formField: MatFormField, formFieldRegistration: FormFieldRegistrationService) {
        formFieldRegistration.register(formField);
    }
}

@Component({
    selector: 'app-mds-editor-widget-container',
    templateUrl: './mds-editor-widget-container.component.html',
    styleUrls: ['./mds-editor-widget-container.component.scss'],
    providers: [FormFieldRegistrationService],
    animations: [
        trigger('showHideExtended', [
            state(
                'hidden',
                style({
                    height: '0px',
                    margin: '0px',
                    opacity: '0',
                    visibility: 'hidden',
                    overflow: 'hidden', // Needed for transition only
                }),
            ),
            transition('hidden => shown', [
                group([
                    // Animate hight, margin, and opacity to original values
                    animate(UIAnimation.ANIMATION_TIME_FAST),
                    // Keep `overflow: hidden` until the widget is fully expanded
                    animate(UIAnimation.ANIMATION_TIME_FAST, style({ overflow: 'hidden' })),
                ]),
            ]),
            transition('shown => hidden', [
                // Set `overflow: hidden` at the beginning of the animation
                style({ overflow: 'hidden' }),
                // Animate to 'hidden' style
                animate('.2s'),
            ]),
        ]),
    ],
})
export class MdsEditorWidgetContainerComponent
    implements OnInit, OnChanges, AfterContentInit, OnDestroy
{
    readonly RequiredMode = RequiredMode;
    readonly ValueType = ValueType;
    @ViewChild(MatRipple) ripple: MatRipple;

    @Input() widget: Widget;
    @Input() injectedView: MdsEditorWidgetBase | NativeWidgetComponent;
    @Input() valueType: ValueType;
    @Input() label: string | boolean;
    @Input() control: AbstractControl;
    /**
     * Whether to wrap in a `mat-form-field`.
     *
     * Defaults to `true` if `control` is set, otherwise `false`.
     */
    @Input() wrapInFormField: boolean;
    /**
     * Whether the content should be semantically grouped and labelled using ARIA.
     *
     * Applies only when not wrapping in a `mat-form-field`.
     *
     * Should be set to false when
     * - a labelled grouping of elements is already provided, e.g. with a `radiogroup`, or
     * - there is only a single element, for which the label and description is provided.
     */
    @Input() wrapInGroup = true;

    @ContentChild(MatFormFieldControl) formFieldControl: MatFormFieldControl<any>;

    @HostBinding('class.disabled') isDisabled = false;
    @HostBinding('@showHideExtended') get showHideExtendedState(): string {
        return this.isHidden ? 'hidden' : 'shown';
    }

    readonly editorBulkMode: EditorBulkMode;
    readonly labelId: string;
    readonly descriptionId: string;
    bulkMode: BehaviorSubject<BulkMode>;
    missingRequired: RequiredMode | null;
    isHidden: boolean;

    private readonly destroyed$ = new Subject<void>();

    constructor(
        private elementRef: ElementRef,
        private mdsEditorInstance: MdsEditorInstanceService,
        private cdr: ChangeDetectorRef,
        private formFieldRegistration: FormFieldRegistrationService,
        private viewInstance: ViewInstanceService,
    ) {
        this.editorBulkMode = this.mdsEditorInstance.editorBulkMode;
        const id = Math.random().toString(36).substr(2);
        this.labelId = id + '_label';
        this.descriptionId = id + '_description';
    }

    ngAfterContentInit() {
        this.formFieldRegistration.onRegister((formField) => {
            formField._control = this.formFieldControl;
        });
        // This seems to be the only way to prevent changed-after-checked errors when dealing with
        // formControls across components.
        this.cdr.detach();
        setTimeout(() => {
            this.cdr.detectChanges();
            this.cdr.reattach();
        });
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes.widget) {
            this.registerIsHidden();
        }
    }

    ngOnInit(): void {
        if (this.label === true) {
            this.label = this.widget.definition.caption;
        }
        if (this.widget && this.editorBulkMode?.isBulk) {
            this.bulkMode = new BehaviorSubject(
                this.editorBulkMode.bulkBehavior === BulkBehavior.Replace ? 'replace' : 'no-change',
            );
            this.bulkMode.subscribe((bulkMode) => this.widget.setBulkMode(bulkMode));
        }
        if (this.control) {
            this.initFormControl(this.control);
        }
        this.wrapInFormField = this.wrapInFormField ?? !!this.control;
        if (this.widget) {
            this.widget.focusTrigger
                .pipe(takeUntil(this.destroyed$))
                .subscribe(() => this.injectedView?.focus());
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
    }

    private registerIsHidden(): void {
        const shouldShowFactors = [this.widget.meetsDynamicCondition];
        if (
            this.widget.definition.isExtended === 'true' ||
            this.widget.definition.isExtended === true
        ) {
            shouldShowFactors.push(this.mdsEditorInstance.shouldShowExtendedWidgets$);
        }
        combineLatest(shouldShowFactors)
            .pipe(
                map((factors) => factors.every((f) => f)),
                distinctUntilChanged(),
            )
            .subscribe((shouldShow) => {
                this.isHidden = !shouldShow;
                if (shouldShow) {
                    this.ripple?.launch({ centered: true, animation: { exitDuration: 5000 } });
                }
            });
    }

    onBulkEditToggleChange(event: MatSlideToggleChange): void {
        this.bulkMode.next(event.checked ? 'replace' : 'no-change');
    }

    shouldShowError(): boolean {
        return !!this.control?.invalid && (this.control.touched || this.control.dirty);
    }

    shouldShowBulkEditToggle(): boolean {
        return (
            this.editorBulkMode?.isBulk &&
            this.editorBulkMode?.bulkBehavior === BulkBehavior.Default &&
            !!this.widget
        );
    }

    private initFormControl(formControl: AbstractControl): void {
        this.widget.observeIsDisabled().subscribe((isDisabled) => this.setDisabled(isDisabled));
        formControl.statusChanges
            .pipe(startWith(formControl.status), distinctUntilChanged())
            .subscribe((status: InputStatus) => {
                this.handleStatus(status);
            });
        this.widget.registerShowMissingRequired((shouldScrollIntoView) => {
            formControl.markAllAsTouched();
            if (formControl.errors?.required && shouldScrollIntoView) {
                this.scrollIntoViewAndFocus();
                return true;
            } else {
                return false;
            }
        });
    }

    private scrollIntoViewAndFocus(): void {
        new Promise((resolve) => {
            // Expand section (view) if needed.
            if (this.viewInstance.isExpanded$.value) {
                resolve(null);
            } else {
                this.viewInstance.isExpanded$.next(true);
                setTimeout(() => resolve(null));
            }
        }).then(() => {
            this.elementRef.nativeElement.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
            });
            this.injectedView?.focus();
        });
    }

    private setDisabled(isDisabled: boolean): void {
        this.isDisabled = isDisabled;
        if (isDisabled) {
            this.control.disable();
        } else {
            this.control.enable();
        }
    }

    private handleStatus(status: InputStatus): void {
        if (this.control.errors?.required) {
            if (
                this.widget.definition.isRequired === RequiredMode.MandatoryForPublish &&
                Object.keys(this.control.errors).length === 1
            ) {
                status = 'VALID'; // downgrade to warning
            }
            this.missingRequired = this.widget.definition.isRequired;
        } else {
            this.missingRequired = null;
        }
        this.widget.setStatus(status);
    }
}
