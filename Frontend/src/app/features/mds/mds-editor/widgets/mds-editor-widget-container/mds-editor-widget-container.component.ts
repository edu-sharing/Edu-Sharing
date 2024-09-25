import { animate, group, state, style, transition, trigger } from '@angular/animations';
import {
    AfterContentInit,
    ChangeDetectorRef,
    Component,
    ContentChild,
    ElementRef,
    HostBinding,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    SimpleChanges,
    ViewChild,
} from '@angular/core';
import { AbstractControl } from '@angular/forms';
import { MatRipple } from '@angular/material/core';
import { MatFormFieldControl } from '@angular/material/form-field';
import { MatSlideToggleChange } from '@angular/material/slide-toggle';
import { MdsWidget } from 'ngx-edu-sharing-api';
import { BehaviorSubject, combineLatest, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, map, startWith, takeUntil } from 'rxjs/operators';
import { MdsEditorInstanceService, Widget } from '../../mds-editor-instance.service';
import { ViewInstanceService } from '../../mds-editor-view/view-instance.service';
import {
    BulkBehavior,
    BulkMode,
    EditorBulkMode,
    InputStatus,
    NativeWidgetComponent,
} from '../../../types/types';
import { MdsEditorWidgetBase, ValueType } from '../mds-editor-widget-base';
import { FormFieldRegistrationService } from './form-field-registration.service';
import { UIService } from '../../../../../core-module/rest/services/ui.service';
import { UIAnimation } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-mds-editor-widget-container',
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
    readonly ValueType = ValueType;
    @ViewChild(MatRipple) ripple: MatRipple;
    @ViewChild('nativeElement') nativeElement: ElementRef;

    @Input() widget: Widget;
    @Input() injectedView: MdsEditorWidgetBase | NativeWidgetComponent;
    @Input() valueType: ValueType;
    @Input() label: string | boolean;
    /*+
        display the bottom caption, if present on the widget
     */
    @Input() showBottomCaption = true;
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

    /**
     * should a progress spinner be shown
     * (only applicable for widgets with non-native material fields)
     */
    @Input() showSpinner: boolean;

    @ContentChild(MatFormFieldControl) formFieldControl: MatFormFieldControl<any>;

    @HostBinding('class.disabled') isDisabled = false;
    expandedState$ = new BehaviorSubject<'disabled' | 'expanded' | 'collapsed'>('disabled');
    @HostBinding('@showHideExtended') get showHideExtendedState(): string {
        return this.isHidden ? 'hidden' : 'shown';
    }

    readonly editorBulkMode: EditorBulkMode;
    readonly labelId: string;
    readonly descriptionId: string;
    bulkMode: BehaviorSubject<BulkMode>;
    missingRequired: MdsWidget['isRequired'] | null;
    isHidden: boolean;

    private readonly destroyed$ = new Subject<void>();

    constructor(
        private elementRef: ElementRef,
        private uiService: UIService,
        public mdsEditorInstance: MdsEditorInstanceService,
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
            this.expandedState$.next(this.widget.definition?.expandable);
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
        if (this.widget.definition.isExtended) {
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
            !!this.widget &&
            this.widget.definition.interactionType !== 'None'
        );
    }

    private initFormControl(formControl: AbstractControl): void {
        this.widget
            .observeIsDisabled()
            .pipe(
                // debounce cause form control might not yet initialized
                debounceTime(10),
            )
            .subscribe((isDisabled) => this.setDisabled(isDisabled));
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
        }).then(async () => {
            await this.uiService.scrollSmoothElementToChild(this.elementRef.nativeElement);
            /*this.elementRef.nativeElement.scrollIntoView({
                behavior: 'smooth',
                block: 'start',
            });*/
            console.log(this.injectedView, this.widget.definition.id);
            setTimeout(() => this.injectedView?.focus());
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
                this.widget.definition.isRequired === 'mandatoryForPublish' &&
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
