import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal, ComponentType } from '@angular/cdk/portal';
import { Injectable, Injector } from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject } from 'rxjs';
import { filter, map, pairwise, switchMap, takeUntil, tap } from 'rxjs/operators';
import { DialogsService } from '../dialogs.service';
import { CardDialogConfig, CARD_DIALOG_DATA } from './card-dialog-config';
import { CardDialogContainerComponent } from './card-dialog-container/card-dialog-container.component';
import { CardDialogRef } from './card-dialog-ref';
import { CardDialogState, ViewMode } from './card-dialog-state';
import { ConfigurableFocusTrap } from '@angular/cdk/a11y';

const MOBILE_BACKGROUND_CLASS = 'card-dialog-pane-mobile-background';
// export const CARD_DIALOG_STATE = new InjectionToken<CardDialogState>('CardDialogState');
// export const CARD_DIALOG_OVERLAY_REF = new InjectionToken<CardDialogState>('CardDialogOverlayRef');

/**
 * Provides cards for modal dialogs via overlays, similar to `MatDialog`.
 */
@Injectable({
    providedIn: 'root',
})
export class CardDialogService {
    private readonly openDialogsBeforeClosedSubject = new BehaviorSubject<readonly CardDialogRef[]>(
        [],
    );
    // FIXME: Do we need this, or could we always use `openDialogsBeforeClosedSubject`?
    private readonly openDialogsSubject = new BehaviorSubject<readonly CardDialogRef[]>([]);
    private focusTraps: ConfigurableFocusTrap[] = [];
    get openDialogs(): readonly CardDialogRef[] {
        return this.openDialogsSubject.value;
    }
    private readonly viewModeSubject = new BehaviorSubject<ViewMode>('default');

    constructor(
        private injector: Injector,
        private overlay: Overlay,
        private breakpointObserver: BreakpointObserver,
    ) {
        this.registerViewMode();
    }

    open<T, D, R>(component: ComponentType<T>, config?: CardDialogConfig<D>): CardDialogRef<D, R> {
        config = this.applyCardConfigDefaults(config);
        const overlayRef = this.createOverlay();
        const state = new CardDialogState();
        const containerRef = overlayRef.attach(new ComponentPortal(CardDialogContainerComponent));
        const dialogRef = new CardDialogRef<D, R>(
            overlayRef,
            containerRef.instance,
            config,
            state,
            this.injector.get(DialogsService),
        );
        containerRef.instance.dialogRef = dialogRef;
        const contentInjector = Injector.create({
            parent: containerRef.injector,
            providers: [
                { provide: CARD_DIALOG_DATA, useValue: config.data },
                { provide: CardDialogRef, useValue: dialogRef },
            ],
        });
        containerRef.instance.attachComponentPortal(
            new ComponentPortal(component, undefined, contentInjector),
        );
        // Notify the dialog container that the content has been attached.
        containerRef.instance.initializeWithAttachedContent();
        this.registerOpenDialog<T, D, R>(dialogRef);
        this.registerSizeAndPosition(overlayRef, dialogRef);
        return dialogRef;
    }

    private applyCardConfigDefaults<D>(config: CardDialogConfig<D> = {}): CardDialogConfig<D> {
        return {
            ...new CardDialogConfig<D>(),
            ...config,
        };
    }

    private createOverlay(): OverlayRef {
        return this.overlay.create({
            hasBackdrop: true,
            panelClass: 'card-dialog-pane',
            positionStrategy: this.overlay
                .position()
                .global()
                .centerHorizontally()
                .centerVertically(),
            scrollStrategy: this.overlay.scrollStrategies.block(),
        });
    }

    private registerSizeAndPosition(overlayRef: OverlayRef, dialogRef: CardDialogRef): void {
        // Update dialog size and position based on pre-defined view modes when the viewport size
        // changes.
        this.viewModeSubject
            .pipe(
                tap((viewMode) => dialogRef.patchState({ viewMode })),
                switchMap((mode) => {
                    if (mode === 'mobile') {
                        // On mobile, positioning depends on other dialogs, so we fire again when
                        // open dialogs change.
                        return this.openDialogsBeforeClosedSubject.pipe(map(() => mode));
                    } else {
                        return rxjs.of(mode);
                    }
                }),
                takeUntil(overlayRef.detachments()),
            )
            .subscribe((mode) => this.updateSizeAndPosition(mode, overlayRef, dialogRef));

        // Reflect size-related changes of config as long as we are in 'default' view mode.
        const sizeProperties: (keyof CardDialogConfig)[] = [
            'height',
            'minHeight',
            'maxHeight',
            'width',
            'minWidth',
            'maxWidth',
        ];
        dialogRef
            .observeConfig()
            .pipe(
                pairwise(),
                filter(([previous, current]) =>
                    sizeProperties.some((prop) => previous[prop] !== current[prop]),
                ),
                filter(() => dialogRef.state.viewMode === 'default'),
            )
            .subscribe(([_, current]) => this.setDefaultSizeAndPosition(current, overlayRef));
    }

    private updateSizeAndPosition(
        mode: ViewMode,
        overlayRef: OverlayRef,
        dialogRef: CardDialogRef,
    ): void {
        switch (mode) {
            case 'default':
                this.setDefaultSizeAndPosition(dialogRef.config, overlayRef);
                break;
            case 'mobile':
                // The 'mobile' view mode doesn't respect configured sizes as of now.
                this.setMobileSizeAndPosition(overlayRef, dialogRef);
                break;
        }
    }

    private registerOpenDialog<T, D, R>(dialogRef: CardDialogRef<D, R>) {
        this.registerOpenDialogInner<T, D, R>(
            dialogRef,
            this.openDialogsBeforeClosedSubject,
            'beforeClosed',
        );
        this.registerOpenDialogInner<T, D, R>(dialogRef, this.openDialogsSubject, 'afterClosed');
    }

    private registerOpenDialogInner<T, D, R>(
        dialogRef: CardDialogRef<D, R>,
        openDialogsSubject: BehaviorSubject<readonly CardDialogRef[]>,
        closed: 'beforeClosed' | 'afterClosed',
    ) {
        openDialogsSubject.next([...openDialogsSubject.value, dialogRef]);
        dialogRef[closed]().subscribe(() => {
            const index = openDialogsSubject.value.indexOf(dialogRef);
            if (index >= 0) {
                const openDialogs = openDialogsSubject.value.slice();
                openDialogs.splice(index, 1);
                openDialogsSubject.next(openDialogs);
            }
        });
    }

    private registerViewMode(): void {
        this.breakpointObserver
            .observe([Breakpoints.XSmall])
            .pipe(map(({ matches }) => (matches ? 'mobile' : 'default')))
            .subscribe(this.viewModeSubject);
    }

    private setDefaultSizeAndPosition(config: CardDialogConfig, overlayRef: OverlayRef): void {
        const MAX_HEIGHT = '95%';
        const MAX_WIDTH = '95%';
        const cssUnit = (value: string | number) =>
            typeof value === 'number' ? value + 'px' : value;
        const size = {
            height: config.height,
            minHeight: config.minHeight ? `min(${cssUnit(config.minHeight)}, ${MAX_HEIGHT})` : null,
            maxHeight: config.maxHeight
                ? `min(${cssUnit(config.maxHeight)}, ${MAX_HEIGHT})`
                : MAX_HEIGHT,
            width: config.width,
            minWidth: config.minWidth ? `min(${cssUnit(config.minWidth)}, ${MAX_WIDTH})` : null,
            maxWidth: config.maxWidth
                ? `min(${cssUnit(config.maxWidth)}, ${MAX_WIDTH})`
                : MAX_WIDTH,
        };
        overlayRef.updateSize(size);
        overlayRef.updatePositionStrategy(
            this.overlay.position().global().centerHorizontally().centerVertically(),
        );
        overlayRef.removePanelClass(MOBILE_BACKGROUND_CLASS);
    }

    private setMobileSizeAndPosition(overlayRef: OverlayRef, dialogRef: CardDialogRef): void {
        overlayRef.updateSize({
            height: 'calc(100% - 30px)',
            minHeight: null,
            maxHeight: null,
            width: '100%',
            minWidth: null,
            maxWidth: null,
        });
        overlayRef.updatePositionStrategy(this.overlay.position().global().bottom());
        const openDialogs = this.openDialogsBeforeClosedSubject.value;
        const isTopMostDialog = openDialogs[openDialogs.length - 1] === dialogRef;
        if (isTopMostDialog) {
            overlayRef.removePanelClass(MOBILE_BACKGROUND_CLASS);
        } else {
            overlayRef.addPanelClass(MOBILE_BACKGROUND_CLASS);
        }
    }

    getFocusTraps() {
        return this.focusTraps;
    }
    registerFocusTrap(focusTrap: ConfigurableFocusTrap) {
        this.focusTraps.push(focusTrap);
    }

    unregisterFocusTrap(focusTrap: ConfigurableFocusTrap) {
        this.focusTraps.splice(this.focusTraps.indexOf(focusTrap), 1);
    }
}
