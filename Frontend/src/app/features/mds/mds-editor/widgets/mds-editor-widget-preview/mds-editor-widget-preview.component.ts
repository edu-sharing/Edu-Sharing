import { Component } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RepoUrlService } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { BehaviorSubject, forkJoin, forkJoin as observableForkJoin, of } from 'rxjs';
import { catchError, map, take, takeWhile } from 'rxjs/operators';
import { Node } from '../../../../../core-module/rest/data-object';
import { RestNodeService } from '../../../../../core-module/rest/services/rest-node.service';
import { Toast } from '../../../../../services/toast';
import { Constraints, NativeWidgetComponent } from '../../../types/types';
import { MdsEditorInstanceService } from '../../mds-editor-instance.service';

@Component({
    selector: 'es-mds-editor-widget-preview',
    templateUrl: './mds-editor-widget-preview.component.html',
    styleUrls: ['./mds-editor-widget-preview.component.scss'],
})
export class MdsEditorWidgetPreviewComponent implements NativeWidgetComponent {
    static readonly constraints: Constraints = {
        requiresNode: true,
        supportsBulk: false,
        onConstraintFailed: 'hide',
    };

    hasChanges = new BehaviorSubject<boolean>(false);
    src: SafeResourceUrl | string;
    nodeSrc: string;
    file: File;
    node: Node;
    delete = false;

    constructor(
        private mdsEditorValues: MdsEditorInstanceService,
        private nodeService: RestNodeService,
        private toast: Toast,
        private repoUrlService: RepoUrlService,
        private sanitizer: DomSanitizer,
    ) {
        forkJoin([this.mdsEditorValues.nodes$.pipe(take(1))])
            .pipe(takeUntilDestroyed())
            .subscribe(([nodes]) => {
                if (nodes?.length === 1) {
                    this.node = nodes[0];
                    this.nodeSrc =
                        this.node.preview.url + '&crop=true&width=400&height=300&dontcache=:cache';
                }
                if (nodes?.length === 1) {
                    this.updateSrc();
                    // we need to reload the image since we don't know if the image (e.g. video file) is still being processed
                    rxjs.interval(5000)
                        .pipe(
                            takeUntilDestroyed(),
                            takeWhile(() => !this.file),
                        )
                        .subscribe(() => this.updateSrc());
                }
            });
    }

    setPreview(event: Event): void {
        this.file = (event.target as HTMLInputElement).files[0];
        this.delete = false;
        this.updateSrc();
    }

    async updateSrc() {
        if (this.file) {
            this.src = this.sanitizer.bypassSecurityTrustResourceUrl(
                window.URL.createObjectURL(this.file),
            );
        } else {
            const src = this.nodeSrc.replace(':cache', new Date().getTime().toString());
            if (this.node) {
                this.src = await this.repoUrlService.getRepoUrl(src, this.node);
            } else {
                this.src = src;
            }
        }
        this.hasChanges.next(this.file != null || this.delete);
    }

    onSaveNode(nodes: Node[]) {
        if (this.delete) {
            return observableForkJoin(
                nodes.map((n) => this.nodeService.deleteNodePreview(n.ref.id)),
            )
                .pipe(map(() => nodes))
                .toPromise();
        }
        if (this.file == null) {
            return null;
        }
        return observableForkJoin(
            nodes.map((n) => this.nodeService.uploadNodePreview(n.ref.id, this.file, false)),
        )
            .pipe(
                map(() => nodes),
                catchError((e) => {
                    this.toast.error(null, 'MDS.ERROR_PREVIEW');
                    return of(nodes);
                }),
            )
            .toPromise();
    }

    getType() {
        return this.node?.preview?.type;
    }
}
