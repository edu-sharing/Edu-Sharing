import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

// Define as object, so we can derive the type `SkipTarget` and the values `skipTargets` from the
// same source.
const skipTargets_ = {
    MAIN_CONTENT: {},
    ACCESSIBILITY_SETTINGS: {},
};

export type SkipTarget = keyof typeof skipTargets_;
const skipTargets: SkipTarget[] = Object.keys(skipTargets_) as SkipTarget[];

type TargetsRegisterEntry =
    | {
          kind: 'element';
          element: HTMLElement;
      }
    | {
          kind: 'action';
          action: () => void;
      };

/**
 * Manages targets for skip navigation.
 *
 * For internal use by `SkipNavComponent` and `SkipTargetDirective`.
 */
@Injectable({
    providedIn: 'root',
})
export class SkipNavService {
    private targetsRegister: { [target in SkipTarget]?: TargetsRegisterEntry } = {};
    private availableTargetsSubject = new BehaviorSubject<SkipTarget[]>([]);

    constructor() {}

    register(target: SkipTarget, element: HTMLElement): void;
    register(target: SkipTarget, action: () => void): void;
    register(target: SkipTarget, elementOrAction: HTMLElement | (() => void)): void {
        if (this.targetsRegister[target]) {
            throw new Error(`Tried to register skip target ${target}, but was already registered.`);
        }
        if (typeof elementOrAction === 'function') {
            this.registerAction(target, elementOrAction);
        } else {
            this.registerElement(target, elementOrAction);
        }
        this.updateAvailableTargets();
    }

    unregister(target: SkipTarget): void {
        this.targetsRegister[target] = null;
        this.updateAvailableTargets();
    }

    getAvailableTargets(): Observable<SkipTarget[]> {
        return this.availableTargetsSubject.asObservable();
    }

    skipTo(target: SkipTarget): void {
        const registerEntry = this.targetsRegister[target];
        if (registerEntry) {
            switch (registerEntry.kind) {
                case 'element':
                    if (!registerEntry.element.hasAttribute('tabindex')) {
                        registerEntry.element.setAttribute('tabindex', '-1');
                    }
                    registerEntry.element.focus();
                    break;
                case 'action':
                    registerEntry.action();
            }
        }
    }

    private registerElement(target: SkipTarget, element: HTMLElement): void {
        if (!element.hasAttribute('tabindex')) {
            element.addEventListener('blur', (event) =>
                (event.target as HTMLElement).removeAttribute('tabindex'),
            );
        }
        this.targetsRegister[target] = { kind: 'element', element };
    }

    private registerAction(target: SkipTarget, action: () => void): void {
        this.targetsRegister[target] = { kind: 'action', action };
    }

    private updateAvailableTargets(): void {
        const availableTargets: SkipTarget[] = [];
        for (const target of skipTargets) {
            if (this.targetsRegister[target]) {
                availableTargets.push(target);
            }
        }
        void Promise.resolve().then(() => this.availableTargetsSubject.next(availableTargets));
    }
}
