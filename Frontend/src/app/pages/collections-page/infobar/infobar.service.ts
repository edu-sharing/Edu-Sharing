import { Overlay, OverlayRef } from '@angular/cdk/overlay';
import { ComponentPortal } from '@angular/cdk/portal';
import { Injectable } from '@angular/core';
import { DialogButton } from '../../../util/dialog-button';
import { GenericDialogButton } from '../../../features/dialogs/dialog-modules/generic-dialog/generic-dialog-data';
import { InfobarComponent } from './infobar.component';

@Injectable({ providedIn: 'root' })
export class InfobarService {
    overlayRef: OverlayRef | null;

    constructor(private overlay: Overlay) {}

    /**
     * Shows an info bar with the given configuration.
     *
     * If there are any info bars visible, they are replaced by this one.
     */
    open<R extends string>({
        title,
        message,
        buttons,
    }: {
        title: string;
        message: string;
        buttons: GenericDialogButton<R>[];
    }): Promise<R | null> {
        return new Promise((resolve) => {
            if (this.overlayRef) {
                this.overlayRef.dispose();
            }
            this.overlayRef = this.overlay.create();
            const portal = new ComponentPortal(InfobarComponent);
            const componentRef = this.overlayRef.attach(portal);
            componentRef.instance.title = title;
            componentRef.instance.message = message;
            componentRef.instance.buttons = buttons.map(
                (button) =>
                    new DialogButton(button.label, button.config, () => {
                        resolve(button.label);
                        this.overlayRef.dispose();
                    }),
            );
            componentRef.instance.isCancelable = true;
            componentRef.instance.cancelInfobar.subscribe(() => this.overlayRef.dispose());
            componentRef.onDestroy(() => {
                this.overlayRef = null;
                resolve(null);
            });
        });
    }

    /**
     * Closes the currently visible info bar, if any.
     */
    close(): void {
        this.overlayRef?.dispose();
    }
}
