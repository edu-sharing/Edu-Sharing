import {
    Directive,
    ElementRef,
    EventEmitter,
    Input,
    NgZone,
    OnDestroy,
    OnInit,
    Output,
} from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { distinctUntilChanged } from 'rxjs/operators';

export interface Options {
    readAs?: string;
}

@Directive({ selector: '[esFileDrop]' })
export class FileDropDirective implements OnInit, OnDestroy {
    @Input() options: Options;
    /**
     * catch drag/drop of whole window
     */
    @Input() window = false;

    @Output() fileOver: EventEmitter<boolean> = new EventEmitter<boolean>();
    @Output() fileDrop: EventEmitter<FileList> = new EventEmitter<FileList>();

    /**
     * Sometimes browsers fire a dragenter event before the dragleave event. When the cursor moves
     * across different HTML elements while dragging, this results in a dragenter event followed by
     * a dragleave. This number represents how many more dragenter events than dragleave events we
     * received. A number > 1 means that we are currently in a drag-over state.
     */
    private dragEnterCount = 0;
    private destroyed = new Subject<void>();
    private fileOverSubject = new BehaviorSubject(false);

    constructor(private elementRef: ElementRef<HTMLElement>, private ngZone: NgZone) {}

    ngOnInit(): void {
        this.registerEvents();
        this.registerOutputs();
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    private registerEvents() {
        const target = this.getTarget();
        // All event handlers run outside Angular's zone. We only enter the zone again when emitting
        // on Outputs.
        this.ngZone.runOutsideAngular(() => {
            addEventListenerUntil(target, 'dragenter', this.onDragEnter, this.destroyed);
            addEventListenerUntil(target, 'dragover', this.onDragOver, this.destroyed);
            addEventListenerUntil(target, 'dragleave', this.onDragLeave, this.destroyed);
            addEventListenerUntil(target, 'drop', this.onDrop, this.destroyed);
        });
    }

    private registerOutputs() {
        // Avoid unnecessary change-detection cycles by only emitting on distinct values.
        this.fileOverSubject.pipe(distinctUntilChanged()).subscribe((value) => {
            this.ngZone.run(() => {
                this.fileOver.emit(value);
            });
        });
    }

    private getTarget(): EventTarget {
        if (this.window) {
            return window;
        } else {
            return this.elementRef.nativeElement;
        }
    }

    private onDragEnter = (event: DragEvent) => {
        ++this.dragEnterCount;
        const transfer = this.getDataTransfer(event);
        if (this.haveFiles(transfer.types)) {
            this.preventAndStop(event);
            transfer.dropEffect = 'copy';
            this.emitFileOver(true);
        }
    };

    private onDragOver = (event: DragEvent) => {
        const transfer = this.getDataTransfer(event);
        if (this.haveFiles(transfer.types)) {
            // If we don't call `preventDefault` on dragover events, we won't get notified of drop
            // events.
            this.preventAndStop(event);
            transfer.dropEffect = 'copy';
        }
    };

    private onDragLeave = () => {
        if (--this.dragEnterCount === 0) {
            this.emitFileOver(false);
        }
    };

    private onDrop = (event: DragEvent) => {
        const transfer = this.getDataTransfer(event);
        this.dragEnterCount = 0;
        this.emitFileOver(false);
        const hasFiles = this.haveFiles(transfer.types) && transfer.files.length;
        if (hasFiles) {
            this.preventAndStop(event);
            this.readFile(transfer.files);
        }
    };

    private readFile(file: FileList): void {
        const strategy = this.pickStrategy();

        if (!strategy) {
            this.emitFileDrop(file);
        } else {
            /*
      // XXX Waiting for angular/zone.js#358
      const method = `readAs${strategy}`;

      FileAPI[method](file, (event) => {
        if (event.type === 'load') {
          this.emitFileDrop(event.result);
        } else if (event.type === 'error') {
          throw new Error(`Couldn't read file '${file.name}'`);
        }
      });
      */
        }
    }

    private emitFileOver(isOver: boolean): void {
        this.fileOverSubject.next(isOver);
    }

    private emitFileDrop(file: FileList): void {
        this.ngZone.run(() => {
            this.fileDrop.emit(file);
        });
    }

    private pickStrategy(): string | void {
        if (!this.options) {
            return;
        }

        if (this.hasStrategy(this.options.readAs)) {
            return this.options.readAs;
        }
    }

    private hasStrategy(type: string): boolean {
        return ['DataURL', 'BinaryString', 'ArrayBuffer', 'Text'].indexOf(type) !== -1;
    }

    private getDataTransfer(event: any | any): DataTransfer {
        return event.dataTransfer ? event.dataTransfer : event.originalEvent.dataTransfer;
    }

    private preventAndStop(event: Event): void {
        event.preventDefault();
        event.stopPropagation();
    }

    private haveFiles(types: any): boolean {
        if (!types) {
            return false;
        }

        if (types.indexOf) {
            return types.indexOf('text/uri-list') === -1 && types.indexOf('Files') !== -1;
        }

        if (types.contains) {
            return types.contains('Files');
        }

        return false;
    }
}

function addEventListenerUntil<T extends Event>(
    target: EventTarget,
    eventName: string,
    callback: (event: T) => void,
    until: Observable<void>,
) {
    target.addEventListener(eventName, callback);
    until.subscribe(() => target.removeEventListener(eventName, callback));
}
