import { trigger } from '@angular/animations';
import {
    AfterContentInit,
    Component,
    ContentChild,
    ElementRef,
    EventEmitter,
    HostListener,
    Input,
    OnDestroy,
    Output,
    TemplateRef,
    ViewChild,
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { UniversalNode } from '../../../core-module/rest/definitions';
import {
    DialogButton,
    Group,
    Node,
    Organization,
    RestHelper,
    UIService,
} from '../../../core-module/core.module';
import { KeyEvents } from '../../../util/key-events';
import { UIAnimation } from 'ngx-edu-sharing-ui';
import { CardService } from '../../../services/card.service';
import { JumpMark, JumpMarksService } from '../../../services/jump-marks.service';
import { AuthorityNamePipe } from '../../pipes/authority-name.pipe';

/**
 * A common edu-sharing modal card
 */
@Component({
    selector: 'es-card',
    templateUrl: 'card.component.html',
    styleUrls: ['card.component.scss'],
    providers: [JumpMarksService],
    animations: [trigger('cardAnimation', UIAnimation.cardAnimation())],
})
export class CardComponent implements AfterContentInit, OnDestroy {
    @ViewChild('cardContainer') cardContainer: ElementRef<HTMLElement>;
    @ViewChild('jumpmarksRef') jumpmarksRef: ElementRef;
    @ViewChild('cardActions') cardActions: ElementRef<HTMLElement>;

    /**
     * the title of the card. Should be pre-translated
     */
    @Input() title: string;
    /**
     * custom css class(es)
     */
    @Input() class: string = '';
    /**
     * The subtitle of the card (optional)
     * You may also use the "node" binding to automatically fill this field
     */
    @Input() subtitle: string;
    /**
     * Should a "x" appear in the top right (don't forget to bind onCancel as an event)
     */
    @Input() isCancelable = true;
    /**
     * An optional image href that should be appear in the top left corner
     */
    @Input() avatar: string;
    /**
     * An optional icon that should be appear in the top left corner (use either avatar or icon!)
     */
    @Input() icon: string;
    @Input() width = 'normal';
    @Input() height = 'normal';
    /**
     * Hint that the layout contains mat-tab-group (relevant for correct scrolling, tabs will be fixed at top)
     */
    @Input() tabbed = false;
    /**
     * Should the dialog be modal (a dark background)
     * allowed values: always (default), auto, never
     * auto: Automatically make the dialog modal when viewed on very tiny screens (e.g. mobile), otherwise use non-modal view
     */
    @Input() modal: 'always' | 'auto' = 'always';
    /**
     * Should the heading be shown
     */
    @Input() header = true;
    /**
     * Apply a custom template for the button area (area above the buttons)
     */
    @ContentChild('beforeButton') beforeButtonRef: TemplateRef<any>;
    /**
     * Jumpmarks for the left side (used for the mds dialog)
     */
    @Input()
    jumpmarks: JumpMark[];
    @Input() priority = 0;

    /**
     * Optional, bind a Node or Node-Array to this element
     * If this is used, the subtitle and avatar is automatically set depending on the given data
     */
    @Input() set node(node: UniversalNode | UniversalNode[] | Group | Organization) {
        if (!node) {
            return;
        }
        let nodes: Node[] = node as any;
        if (!Array.isArray(nodes)) {
            nodes = [node as any];
        }
        if (nodes && nodes.length) {
            if (nodes.length === 1 && nodes[0]) {
                // Group
                if ((nodes[0] as any).profile) {
                    this.icon = 'group';
                    this.subtitle = new AuthorityNamePipe(this.translate).transform(nodes[0]);
                } else {
                    this.avatar = nodes[0].iconURL;
                    this.subtitle = RestHelper.getTitle(nodes[0]);
                }
            } else {
                this.avatar = null;
                this.subtitle = this.translate.instant('CARD_SUBTITLE_MULTIPLE', {
                    count: nodes.length,
                });
            }
        }
    }
    @Input() set buttons(buttons: DialogButton[]) {
        this._buttons = buttons?.filter((button) => button.position === 'standard');
        this._buttonsLeft = buttons?.filter((button) => button.position === 'opposite');
    }

    /**
     * Set the type of the card
     * this will automatically set the card icon
     */
    @Input() set type(type: CardType) {
        switch (type) {
            case CardType.Question:
                this.icon = 'help_outline';
                break;
            case CardType.Info:
                this.icon = 'info_outline';
                break;
        }
    }

    /**
     * Whether focus should be moved to the card upon creation and restored
     * after destruction.
     *
     * This should be `true` in most cases to set a sensible starting point for
     * keyboard navigation and prevent the focus from falling on elements that
     * are covered by the card.
     *
     * Exceptions are dialogs that the user cannot interact with and that are
     * shown out of order, e.g., show dialog A, show dialog B, hide dialog A. In
     * this example, focus cannot be restored correctly to dialog B, so dialog A
     * should not capture focus if possible.
     */
    @Input() captureFocus = true;

    @Output() onCancel = new EventEmitter();
    @Output() onScrolled = new EventEmitter();

    /** A list of buttons, see @DialogButton
     * Also use the DialogButton.getYesNo() and others if applicable!
     */
    _buttons: DialogButton[];
    /**
     * Buttons on the left side (Secondary)
     */
    _buttonsLeft: DialogButton[];
    jumpmarkActive: JumpMark;

    private static modalCards: CardComponent[] = [];

    private shouldUpdateJumpmarkOnScroll = true;

    /**
     * @Deprecated
     * Consider using the new CardDialogService.openDialogs
     */
    static getNumberOfOpenCards() {
        return CardComponent.modalCards.length;
    }

    constructor(
        private uiService: UIService,
        private translate: TranslateService,
        private cardService: CardService,
        private jumpMarksService: JumpMarksService,
    ) {
        CardComponent.modalCards.splice(0, 0, this);
        cardService.setNumberModalCards(CardComponent.modalCards.length);
        document.body.style.overflow = 'hidden';
        uiService.waitForComponent(this, 'jumpmarksRef').subscribe(() => {
            this.updateActiveJumpmark();
        });
        this.registerJumpMarkHandler();
    }

    ngAfterContentInit() {
        // Delay focus processing to not interfere with Angular's
        // initialization.
        setTimeout(() => this.setInitialFocus());
    }

    ngOnDestroy() {
        CardComponent.modalCards.splice(CardComponent.modalCards.indexOf(this), 1);
        if (CardComponent.modalCards.length === 0) {
            document.body.style.overflow = null;
        }
        this.cardService.setNumberModalCards(CardComponent.modalCards.length);
    }

    @HostListener('window:resize')
    onResize() {
        if (document.activeElement && this.cardContainer && this.cardContainer.nativeElement) {
            this.uiService.scrollSmoothElementToChild(
                document.activeElement,
                this.cardContainer.nativeElement,
            );
        }
    }
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        /*for (const card of CardComponent.modalCards) {
            if (card.handleEvent(event)) {
                return;
            }
        }*/
    }

    handleEvent(event: KeyboardEvent) {
        if (event.key === 'Escape') {
            event.stopPropagation();
            event.preventDefault();
            this.cancel();
            return true;
        }
        if (this.modal === 'always') {
            if (KeyEvents.isChildEvent(event, this.cardContainer)) {
                event.stopPropagation();
                return true;
            }
        }
        return true;
    }

    click(btn: DialogButton) {
        btn.callback();
    }

    cancel() {
        this.onCancel.emit();
    }

    scrolled() {
        this.onScrolled.emit();
    }

    onScroll() {
        if (this.shouldUpdateJumpmarkOnScroll) {
            this.updateActiveJumpmark();
        }
    }

    async scrollToJumpMark(jumpMark: JumpMark): Promise<void> {
        this.jumpMarksService.beforeScrollToJumpMark.next(jumpMark);
        const headingElement = document.getElementById(jumpMark.id);
        let pos = headingElement.offsetTop;
        // In case our headingElement is sticky, we cannot use its own offset since it moves
        // around. We require for the header to be the first child of its parent in that case.
        // We keep the simple position calculation from above for backwards compatibility.
        if (window.getComputedStyle(headingElement).position === 'sticky') {
            pos = headingElement.parentElement.offsetTop;
        }
        this.jumpmarkActive = jumpMark;
        this.shouldUpdateJumpmarkOnScroll = false;
        await this.uiService.scrollSmoothElement(pos, this.cardContainer.nativeElement, 0.5);
        // Leave a little time for the last scroll event to propagate before enabling updates
        // again.
        window.setTimeout(() => (this.shouldUpdateJumpmarkOnScroll = true), 20);
    }

    private registerJumpMarkHandler(): void {
        this.jumpMarksService.triggerScrollToJumpMark.subscribe(({ jumpMark }) => {
            if (typeof jumpMark === 'string') {
                jumpMark = this.jumpmarks.find((j) => j.id === jumpMark);
            }
            void this.scrollToJumpMark(jumpMark);
        });
    }

    private setInitialFocus() {
        const inputs = Array.from(this.cardContainer.nativeElement.getElementsByTagName('input'));
        if (inputs.some((el) => el.autofocus)) {
            // Focus the first input field that sets `autofocus`.
            inputs.find((el) => el.autofocus).focus();
            return;
        } else if (inputs.length) {
            // Else, focus the first input field.
            inputs[0].focus();
            return;
        } else if (this.cardActions) {
            // Else, focus the right-most action button that is not disabled.
            const actionButtons = Array.from(this.cardActions.nativeElement.children).map(
                (el) => el.children[0] as HTMLButtonElement,
            );
            const lastButton = actionButtons.reverse().find((el) => !el.disabled);
            if (lastButton) {
                lastButton.focus();
                return;
            }
        }
        // Else, focus will default to the 'X' button on the header bar for
        // dialogs that set `modal=always`.
        //
        // If this happens although there are buttons or inputs on the dialog,
        // make sure these are there from the beginning and not inserted later
        // on.
    }

    private updateActiveJumpmark(): void {
        try {
            this.jumpmarkActive = this.getActiveJumpmark();
        } catch (e) {
            console.warn('getActiveJumpmark failed:', e);
        }
    }

    /**
     * Returns the jumpmark that should be highlighted as active based on current scroll position.
     *
     * Returns `null` if no active jumpmark could be found.
     */
    private getActiveJumpmark(): JumpMark | null {
        // Check how much of any jumpmark section is visible on screen. Return either the first
        // (topmost) section that is visible completely or, if there is none, the one with that
        // covers most of the card's pixels.
        //
        // For sticky headings, We have an alternative approach of determining the active jumpmark:
        // the heading currently sticking to the top of the card is always active.
        const cardTop = this.cardContainer.nativeElement.getBoundingClientRect().top;
        const cardBottom = this.cardContainer.nativeElement.getBoundingClientRect().bottom;
        let activeJumpmark: JumpMark = null;
        let maxPixelsVisible = 0;
        for (const jumpmark of this.jumpmarks ?? []) {
            const headingElement = document.getElementById(jumpmark.id);
            const sectionTop = headingElement.getBoundingClientRect().top;
            if (window.getComputedStyle(headingElement).position === 'sticky') {
                if (sectionTop >= cardTop) {
                    return jumpmark;
                } else {
                    continue;
                }
            }
            const sectionBottom = headingElement.nextElementSibling.getBoundingClientRect().bottom;
            if (sectionTop >= cardTop && sectionBottom < cardBottom) {
                // The section is completely visible.
                return jumpmark;
            }
            const pixelsVisible =
                Math.min(sectionBottom, cardBottom) - Math.max(sectionTop, cardTop);
            if (pixelsVisible > maxPixelsVisible) {
                maxPixelsVisible = pixelsVisible;
                activeJumpmark = jumpmark;
            } else if (maxPixelsVisible > 0 && pixelsVisible <= 0) {
                // We are past the visible sections, no point in continuing.
                break;
            }
        }
        return activeJumpmark;
    }
}
export enum CardType {
    Info = 'Info',
    Question = 'Question',
    Warning = 'Warning',
    Error = 'Error',
}
