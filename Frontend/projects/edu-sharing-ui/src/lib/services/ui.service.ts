import {
    ComponentFactoryResolver,
    ComponentRef,
    EmbeddedViewRef,
    Injectable,
    Injector,
    NgZone,
    Type,
    ViewContainerRef,
} from '@angular/core';
import * as rxjs from 'rxjs';
import { BehaviorSubject, Observable } from 'rxjs';
import { UIConstants } from '../util/ui-constants';
import { OptionItem } from '../types/option-item';
import { distinctUntilChanged, map } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class UIService {
    isTouchSubject = new BehaviorSubject(false);
    private metaKeyPressedSubject = new BehaviorSubject(false);
    private shiftKeyPressedSubject = new BehaviorSubject(false);
    private ctrlKeyPressedSubject = new BehaviorSubject(false);

    get shiftKeyPressed() {
        return this.shiftKeyPressedSubject.value;
    }

    constructor(
        protected componentFactoryResolver: ComponentFactoryResolver,
        protected injector: Injector,
        protected ngZone: NgZone,
    ) {
        // HostListener not working, so use window
        this.ngZone.runOutsideAngular(() => {
            window.addEventListener('keydown', (event) => {
                this.onKeyDownOrKeyUp(event);
            });
            window.addEventListener('keyup', (event) => {
                this.onKeyDownOrKeyUp(event);
            });
            window.addEventListener('pointerdown', (event) => {
                // Usually, properties for modifier keys will be set correctly on keydown and keyup
                // events, but there are situations where the operating system intercepts key
                // presses, e.g. the Windows key on Linux systems, so we update again on mouse
                // clicks to be sure.
                this.updateModifierKeys(event);
            });
            window.addEventListener('pointerdown', (event) => {
                // Usually, properties for modifier keys will be set correctly on keydown and keyup
                // events, but there are situations where the operating system intercepts key
                // presses, e.g. the Windows key on Linux systems, so we update again on mouse
                // clicks to be sure.
                const isTouch = (event as PointerEvent).pointerType === 'touch';
                if (this.isTouchSubject.value !== isTouch) {
                    this.ngZone.run(() => this.isTouchSubject.next(isTouch));
                }
            });
        });
    }
    private onKeyDownOrKeyUp(event: KeyboardEvent) {
        // `event.metaKey` is not consistent across browsers on the actual keypress of the modifier
        // key. So we handle these events separately.
        if (event.key === 'Control') {
            this.ctrlKeyPressedSubject.next(event.type === 'keydown');
        } else if (event.key === 'Shift') {
            this.shiftKeyPressedSubject.next(event.type === 'keydown');
        } else if (event.key === 'Meta') {
            this.metaKeyPressedSubject.next(event.type === 'keydown');
        } else {
            // In case we miss modifier events because the browser didn't have focus during the
            // event, we update modifier keys on unrelated key events as well.
            this.updateModifierKeys(event);
        }
    }

    private updateModifierKeys(event: PointerEvent | KeyboardEvent) {
        this.metaKeyPressedSubject.next(event.metaKey);
        this.shiftKeyPressedSubject.next(event.shiftKey);
        this.ctrlKeyPressedSubject.next(event.ctrlKey);
    }

    observeCtrlOrCmdKeyPressedOutsideZone(): Observable<boolean> {
        return rxjs.combineLatest([this.metaKeyPressedSubject, this.ctrlKeyPressedSubject]).pipe(
            map(([metaKeyPressed, ctrlKeyPressed]) => metaKeyPressed || ctrlKeyPressed),
            distinctUntilChanged(),
        );
    }

    /**
     * @Deprecated
     * Prefer to subscribe to the isTouchSubject directly if viable
     *
     * Returns true if the current sessions seems to be running on a mobile device
     */
    public isMobile() {
        return this.isTouchSubject.value;
    }
    public static evaluateMediaQuery(type: string, value: number) {
        if (type == UIConstants.MEDIA_QUERY_MAX_WIDTH) return value > window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MIN_WIDTH) return value < window.innerWidth;
        if (type == UIConstants.MEDIA_QUERY_MAX_HEIGHT) return value > window.innerHeight;
        if (type == UIConstants.MEDIA_QUERY_MIN_HEIGHT) return value < window.innerHeight;
        console.warn('Unsupported media query ' + type);
        return true;
    }
    filterValidOptions(options: OptionItem[]) {
        if (options == null) return null;
        options = options.filter((value) => value != null);
        let optionsFiltered: OptionItem[] = [];
        for (let option of options) {
            if (
                (!option.onlyMobile || (option.onlyMobile && this.isMobile())) &&
                (!option.onlyDesktop || (option.onlyDesktop && !this.isMobile())) &&
                (!option.mediaQueryType ||
                    (option.mediaQueryType &&
                        UIService.evaluateMediaQuery(
                            option.mediaQueryType,
                            option.mediaQueryValue,
                        )))
            )
                optionsFiltered.push(option);
        }
        return optionsFiltered;
    }

    /**
     * helper that updates the "isEnabled" flag on all options for the given, selected node
     * can be used by dropdown or action menus to update the state for the current element
     * @param options
     */
    async updateOptionEnabledState(
        options: BehaviorSubject<OptionItem[]>,
        object: Node | any = null,
    ) {
        options.value.forEach((o) => {
            o.isEnabled = !o.customEnabledCallback;
            o.enabledCallback(object).then((result) => {
                o.isEnabled = result;
                options.next(options.value);
            });
        });
        options.next(options.value);
    }

    public filterToggleOptions(options: OptionItem[], toggle: boolean) {
        let result: OptionItem[] = [];
        for (let option of options) {
            if (option.isToggle == toggle) result.push(option);
        }
        return result;
    }
    /**
     * dynamically inject an angular component into a regular html dom element
     * @param componentFactoryResolver The resolver service
     * @param viewContainerRef The viewContainerRef service
     * @param componentName The name of the angular component (e.g. SpinnerComponent)
     * @param targetElement The target element of the dom. If the element is null (not found), nothing is done
     * @param bindings Optional bindings (inputs & outputs) to the given component
     * @param delay Optional inflating delay in ms(some components may need some time to "init" the layout)
     * @param replace Whether to replace to previous `innerHTML` of `targetElement`
     * @param injector (to fetch templates for the component)
     */
    public injectAngularComponent<T>(
        viewContainerRef: ViewContainerRef,
        componentName: Type<T>,
        targetElement: Element,
        bindings: { [key: string]: any } = null,
        { delay = 0, replace = true } = {},
    ): ComponentRef<T> {
        if (targetElement == null) {
            return null;
        }
        const factory = this.componentFactoryResolver.resolveComponentFactory(componentName);
        const component: ComponentRef<T> = viewContainerRef.createComponent(
            factory,
            undefined,
            this.injector,
        );
        if (bindings) {
            const instance: { [key: string]: any } = component.instance;
            for (const key in bindings) {
                const binding = bindings[key];
                if (binding instanceof Function) {
                    // subscribe so callback can properly invoked
                    instance[key].subscribe((value: any) => binding(value));
                } else {
                    instance[key] = binding;
                    // `ngOnChanges` won't be called on the component like this. Consider doing
                    // something like this:
                    // https://scm.edu-sharing.com/edu-sharing/projects/oeh-redaktion/ng-meta-widgets/-/blob/1603fb2dedadd3952401385bcbd91a4bd8407643/src/app/app.module.ts#L66-79
                }
            }
        }

        // 3. Get DOM element from component
        const domElem = (component.hostView as EmbeddedViewRef<any>).rootNodes[0] as HTMLElement;
        domElem.style.display = 'none';
        if (replace) {
            targetElement.innerHTML = null;
        }
        targetElement.appendChild(domElem);
        setTimeout(() => {
            domElem.style.display = null;
        }, delay);
        return component;
    }
}
