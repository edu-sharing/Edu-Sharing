import { animate, AnimationEvent, state, style, transition, trigger } from '@angular/animations';
import {
    ConfigurableFocusTrap,
    ConfigurableFocusTrapFactory,
    FocusMonitor,
    InteractivityChecker,
} from '@angular/cdk/a11y';
import { BreakpointObserver } from '@angular/cdk/layout';
import { _getFocusedElementPierceShadowDom } from '@angular/cdk/platform';
import { CdkPortalOutlet, ComponentPortal } from '@angular/cdk/portal';
import { DOCUMENT } from '@angular/common';
import {
    Component,
    ComponentRef,
    ElementRef,
    EventEmitter,
    HostBinding,
    HostListener,
    Inject,
    NgZone,
    OnDestroy,
    OnInit,
    Optional,
    ViewChild,
} from '@angular/core';
import { Subject } from 'rxjs';
import { map, takeUntil } from 'rxjs/operators';
import { DialogButton } from '../../../../core-module/core.module';
import { UIAnimation } from 'ngx-edu-sharing-ui';
import { JumpMark, JumpMarksService } from '../../../../services/jump-marks.service';
import { CardDialogConfig, Closable } from '../card-dialog-config';
import { CardDialogRef } from '../card-dialog-ref';
import { AutoSavingState } from '../card-dialog-state';
import { CardDialogService } from '../card-dialog.service';

let idCounter = 0;

// A lot of this code is copied from
// https://github.com/angular/components/blob/13.3.x/src/material/dialog/dialog-container.ts.

/** Event that captures the state of dialog container animations. */
interface DialogAnimationEvent {
    state: 'opened' | 'opening' | 'closing' | 'closed';
    totalTime: number;
}

type CardState = 'void' | 'enter' | 'exit';

@Component({
    selector: 'es-card-dialog-container',
    templateUrl: './card-dialog-container.component.html',
    styleUrls: ['./card-dialog-container.component.scss'],
    providers: [JumpMarksService],
    animations: [
        trigger('defaultAnimation', [
            state('void, exit', style({ opacity: 0, transform: 'scale(0.7)' })),
            state('enter', style({ transform: 'none' })),
            transition('* => enter', [
                style({
                    transform: 'translateY(-15%)',
                    opacity: 0,
                }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({ transform: 'translateY(0)', opacity: 1 }),
                ),
            ]),
            transition('* => void, * => exit', [
                style({ opacity: 1 }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({
                        transform: 'scale(.95)',
                        opacity: 0,
                    }),
                ),
            ]),
        ]),
        trigger('mobileAnimation', [
            state('void, exit', style({ opacity: 0, transform: 'scale(0.7)' })),
            state('enter', style({ transform: 'none' })),
            transition('* => enter', [
                style({
                    transform: 'translateY(15%)',
                    opacity: 0,
                }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({ transform: 'translateY(0)', opacity: 1 }),
                ),
            ]),
            transition('* => void, * => exit', [
                style({ opacity: 1 }),
                animate(
                    UIAnimation.ANIMATION_TIME_NORMAL + 'ms ease',
                    style({
                        transform: 'translateY(15%)',
                        opacity: 0,
                    }),
                ),
            ]),
        ]),
        trigger('expandContent', [
            state('locked', style({ height: '{{height}}', width: '{{width}}' }), {
                params: { height: '0px', width: '0px' },
            }),
            transition('* => expand', [
                animate(
                    UIAnimation.ANIMATION_TIME_SLOW + 'ms ease',
                    style({ height: '*', width: '*' }),
                ),
            ]),
        ]),
        trigger('slideInFromRight', [
            state('void', style({ transform: 'translateX(100%)', width: 0 })),
            transition(':enter, :leave', [animate(UIAnimation.ANIMATION_TIME_SLOW + 'ms ease')]),
        ]),
    ],
})
export class CardDialogContainerComponent implements OnInit, OnDestroy {
    readonly id = idCounter++;
    @HostBinding('attr.aria-modal') readonly ariaModal = 'true';
    @HostBinding('attr.role') readonly role = 'dialog';
    // Make the container focusable, so keyboard shortcuts keep working when the user clicked some
    // non-interactive element.
    @HostBinding('attr.tabindex') readonly tabIndex = '-1';
    @HostBinding('attr.aria-labelledby') readonly ariaLabelledby = `card-dialog-title-${this.id}`;
    @HostBinding('attr.aria-describedby')
    readonly ariaDescribedby = `card-dialog-subtitle-${this.id}`;
    @HostBinding('@defaultAnimation') defaultAnimation: CardState | null = null;
    @HostBinding('@mobileAnimation') mobileAnimation: CardState | null = null;

    @ViewChild(CdkPortalOutlet, { static: true }) portalOutlet: CdkPortalOutlet;
    @ViewChild('cardContent', { static: true }) cardContent: ElementRef<HTMLElement>;

    config: CardDialogConfig<unknown> = {};
    buttons: DialogButton[];
    isLoading = false;
    closeButtonTemporarilyDisabled = false;
    autoSavingState: AutoSavingState = null;
    isMobile: boolean;
    activeJumpMark: JumpMark | null = null;
    expandAnimation = {
        state: 'initial' as 'initial' | 'locked' | 'expand',
        height: '0px',
        width: '0px',
    };
    readonly hideJumpMarks = this.breakpointObserver
        .observe(['(max-width: 800px)'])
        .pipe(map((result) => result.matches));

    /** Emits when an animation state changes. */
    readonly animationStateChanged = new EventEmitter<DialogAnimationEvent>();

    // Cannot be injected because the dialogRef is available only after this container is created.
    // Will be set right after construction.
    dialogRef!: CardDialogRef<unknown, unknown>;

    private focusTrap: ConfigurableFocusTrap;
    /** Element that was focused before the dialog was opened. Save this to restore upon close. */
    private elementFocusedBeforeDialogWasOpened: HTMLElement | null = null;
    private readonly destroyed$ = new Subject<void>();

    constructor(
        // @Inject(CARD_DIALOG_STATE) private dialogState: CardDialogState,
        // @Inject(CARD_DIALOG_OVERLAY_REF) private overlayRef: OverlayRef,
        @Optional() @Inject(DOCUMENT) private document: any,
        private elementRef: ElementRef<HTMLElement>,
        private focusTrapFactory: ConfigurableFocusTrapFactory,
        private cardDialogService: CardDialogService,
        private _interactivityChecker: InteractivityChecker,
        private _ngZone: NgZone,
        private breakpointObserver: BreakpointObserver,
        private focusMonitor?: FocusMonitor,
    ) {}

    private initObservables() {
        this.config = this.dialogRef.config;
        this.dialogRef
            .observeConfig()
            .pipe(takeUntil(this.destroyed$))
            .subscribe((cardConfig) =>
                Promise.resolve().then(() => {
                    this.config = cardConfig;
                    this.updateButtons();
                }),
            );
        this.dialogRef
            .observeState('viewMode')
            .pipe(takeUntil(this.destroyed$))
            .subscribe((viewMode) => (this.isMobile = viewMode === 'mobile'));
        this.dialogRef
            .observeState('isLoading')
            .pipe(takeUntil(this.destroyed$))
            .subscribe((isLoading) => {
                this.updateButtons();
                this.isLoading = isLoading;
                this.triggerExpandAnimation();
            });
        this.dialogRef
            .observeState('autoSavingState')
            .pipe(takeUntil(this.destroyed$))
            .subscribe((autoSavingState) => {
                this.updateButtons();
                this.autoSavingState = autoSavingState;
            });
    }

    ngOnInit(): void {
        this.setState('enter');
    }

    private updateButtons(): void {
        if (this.dialogRef.state.isLoading || this.dialogRef.state.autoSavingState === 'saving') {
            this.buttons = this.config.buttons?.map((button) => ({
                ...button,
                disabled: true,
            }));
        } else {
            this.buttons = this.config.buttons;
        }
    }

    private triggerExpandAnimation() {
        const element = this.cardContent.nativeElement;
        if (this.expandAnimation.state === 'initial' && this.isLoading) {
            // Wait for the container to assume its initial dimensions, then lock these until
            // loading stops.
            setTimeout(() => {
                this.expandAnimation.state = 'locked';
                this.expandAnimation.height = element.scrollHeight + 'px';
                this.expandAnimation.width = element.scrollWidth + 'px';
            });
        } else if (this.expandAnimation.state === 'locked' && !this.isLoading) {
            this.expandAnimation.state = 'expand';
        }
    }

    ngOnDestroy(): void {
        this.destroyed$.next();
        this.destroyed$.complete();
        this.dialogRef = null;
    }

    attachComponentPortal<T>(portal: ComponentPortal<T>): ComponentRef<T> {
        return this.portalOutlet.attachComponentPortal(portal);
    }

    initializeWithAttachedContent() {
        this.initObservables();
        this.focusTrap = this.focusTrapFactory.create(this.elementRef.nativeElement);
        this.cardDialogService.registerFocusTrap(this.focusTrap);

        // Save the previously focused element. This element will be re-focused
        // when the dialog closes.
        if (this.document) {
            this.elementFocusedBeforeDialogWasOpened = _getFocusedElementPierceShadowDom();
        }
        void this.trapFocus();
    }

    @HostListener('@defaultAnimation.start', ['$event'])
    @HostListener('@mobileAnimation.start', ['$event'])
    onAnimationStart({ toState, totalTime }: AnimationEvent) {
        if (toState === 'enter') {
            this.animationStateChanged.next({ state: 'opening', totalTime });
        } else if (toState === 'exit' || toState === 'void') {
            this.animationStateChanged.next({ state: 'closing', totalTime });
        }
    }

    @HostListener('@defaultAnimation.done', ['$event'])
    @HostListener('@mobileAnimation.done', ['$event'])
    onAnimationDone({ toState, totalTime }: AnimationEvent) {
        if (toState === 'enter') {
            // if (this._config.delayFocusTrap) {
            //     this._trapFocus();
            // }
            this.animationStateChanged.next({ state: 'opened', totalTime });
        } else if (toState === 'exit') {
            this.restoreFocus();
            this.animationStateChanged.next({ state: 'closed', totalTime });
        }
    }

    onCloseButtonClick(): void {
        this.dialogRef.tryCancel('x-button');
    }

    shouldShowCloseButton(): boolean {
        return this.config.closable <= Closable.Confirm;
    }

    /** Starts the dialog exit animation. */
    startExitAnimation(): void {
        this.setState('exit');

        // Mark the container for check so it can react if the
        // view container is using OnPush change detection.
        // this._changeDetectorRef.markForCheck();
    }

    private setState(state: CardState): void {
        switch (this.dialogRef.state.viewMode) {
            case 'default':
                this.defaultAnimation = state;
                break;
            case 'mobile':
                this.mobileAnimation = state;
                break;
        }
    }

    /** Restores focus to the element that was focused before the dialog opened. */
    private restoreFocus() {
        const previousElement = this.elementFocusedBeforeDialogWasOpened;

        // We need the extra check, because IE can set the `activeElement` to null in some cases.
        if (
            // this._config.restoreFocus &&
            previousElement &&
            typeof previousElement.focus === 'function'
        ) {
            const activeElement = _getFocusedElementPierceShadowDom();
            const element = this.elementRef.nativeElement;

            // Make sure that focus is still inside the dialog or is on the body (usually because a
            // non-focusable element like the backdrop was clicked) before moving it. It's possible that
            // the consumer moved it themselves before the animation was done, in which case we shouldn't
            // do anything.
            if (
                !activeElement ||
                activeElement === this.document.body ||
                activeElement === element ||
                element.contains(activeElement)
            ) {
                if (this.focusMonitor) {
                    //   this._focusMonitor.focusVia(previousElement, this._closeInteractionType);
                    this.focusMonitor.focusVia(previousElement, 'program');
                    //   this._closeInteractionType = null;
                } else {
                    previousElement.focus();
                }
            }
        }

        if (this.focusTrap) {
            this.focusTrap.destroy();
            this.cardDialogService.unregisterFocusTrap(this.focusTrap);
        }
    }

    /**
     * Moves the focus inside the focus trap. When autoFocus is not set to 'dialog', if focus
     * cannot be moved then focus will go to the dialog container.
     */
    async trapFocus() {
        const element = this.elementRef.nativeElement;
        // Disable the close button, so initial focus will go the the next focusable element.
        this.closeButtonTemporarilyDisabled = true;
        // If were to attempt to focus immediately, then the content of the dialog would not yet be
        // ready in instances where change detection has to run first. To deal with this, we simply
        // wait for the microtask queue to be empty when setting focus when autoFocus isn't set to
        // dialog. If the element inside the dialog can't be focused, then the container is focused
        // so the user can't tab into other elements behind it.
        switch (this.config.autoFocus) {
            case false:
            case 'dialog':
                // Ensure that focus is on the dialog container. It's possible that a different
                // component tried to move focus while the open animation was running. See:
                // https://github.com/angular/components/issues/16215. Note that we only want to do this
                // if the focus isn't inside the dialog already, because it's possible that the consumer
                // turned off `autoFocus` in order to move focus themselves.
                if (!this.containsFocus()) {
                    element.focus();
                }
                break;
            case true:
            case 'first-tabbable':
                const focusedSuccessfully = await this.focusTrap.focusInitialElementWhenReady();
                // If we weren't able to find a focusable element in the dialog, then focus the dialog
                // container instead.
                if (!focusedSuccessfully) {
                    this._focusDialogContainer();
                }
                break;
            case 'first-heading':
                this._focusByCssSelector('h1, h2, h3, h4, h5, h6, [role="heading"]');
                break;
            default:
                this._focusByCssSelector(this.config.autoFocus!);
                break;
        }
        this._ngZone.runOutsideAngular(() =>
            setTimeout(() => {
                this.closeButtonTemporarilyDisabled = false;
            }),
        );
    }

    /**
     * Focuses the first element that matches the given selector within the focus trap.
     * @param selector The CSS selector for the element to set focus to.
     */
    private _focusByCssSelector(selector: string, options?: FocusOptions) {
        let elementToFocus = this.elementRef.nativeElement.querySelector(
            selector,
        ) as HTMLElement | null;
        if (elementToFocus) {
            this._forceFocus(elementToFocus, options);
        }
    }

    /** Returns whether focus is inside the dialog. */
    private containsFocus() {
        const element = this.elementRef.nativeElement;
        const activeElement = _getFocusedElementPierceShadowDom();
        return element === activeElement || element.contains(activeElement);
    }

    /** Focuses the dialog container. */
    private _focusDialogContainer() {
        // Note that there is no focus method when rendering on the server.
        if (this.elementRef.nativeElement.focus) {
            this.elementRef.nativeElement.focus();
        }
    }

    /**
     * Focuses the provided element. If the element is not focusable, it will add a tabIndex
     * attribute to forcefully focus it. The attribute is removed after focus is moved.
     * @param element The element to focus.
     */
    private _forceFocus(element: HTMLElement, options?: FocusOptions) {
        if (!this._interactivityChecker.isFocusable(element)) {
            element.tabIndex = -1;
            // The tabindex attribute should be removed to avoid navigating to that element again
            this._ngZone.runOutsideAngular(() => {
                const callback = () => {
                    element.removeEventListener('blur', callback);
                    element.removeEventListener('mousedown', callback);
                    element.removeAttribute('tabindex');
                };

                element.addEventListener('blur', callback);
                element.addEventListener('mousedown', callback);
            });
        }
        element.focus(options);
    }
}
