import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslationsService } from 'ngx-edu-sharing-ui';
import { Node } from 'ngx-edu-sharing-api';
import {
    FrameEventsService,
    RestConnectorService,
    RestConstants,
} from '../../core-module/core.module';
import { NodeHelperService } from '../../services/node-helper.service';
import { UploadDialogService } from '../../services/upload-dialog.service';

@Component({
    selector: 'es-upload-page',
    templateUrl: 'upload-page.component.html',
    styleUrls: ['upload-page.component.scss'],
})
export class UploadPageComponent {
    loading = true;
    parent: Node;

    private reurl: string;

    constructor(
        private translations: TranslationsService,
        private nodeHelper: NodeHelperService,
        private connector: RestConnectorService,
        private events: FrameEventsService,
        private route: ActivatedRoute,
        private uploadDialogs: UploadDialogService,
    ) {
        this.translations.waitForInit().subscribe(() => {
            this.connector.isLoggedIn(false).subscribe((login) => {
                if (login.statusCode === RestConstants.STATUS_CODE_OK) {
                    this.nodeHelper.getDefaultInboxFolder().subscribe((inbox) => {
                        this.parent = inbox;
                        this.route.queryParams.subscribe((params) => {
                            this.reurl = params['reurl'];
                        });
                        this._openUploadDialog();
                    });
                }
            });
        });
    }

    private _openUploadDialog(): void {
        this.loading = false;
        void this.uploadDialogs
            .openUploadDialog({ parent: this.parent, chooseParent: true, multiple: false })
            .then((nodes) => {
                this._onDone(nodes);
            });
    }

    private _onDone(node: Node[]) {
        if (node == null) {
            this._cancel();
            return;
        }
        this.nodeHelper.addNodeToLms(node[0], this.reurl);
        window.close();
    }

    private _cancel() {
        this.events.broadcastEvent(FrameEventsService.EVENT_UPLOAD_CANCELED);
        window.close();
        // There have been different paths for handling a canceled dialog before migration to
        // overlay dialogs. One called `window.close()` like above, the other one opened the upload
        // dialog again. Not sure, whether `window.close()` ever works, so we fallback to
        // `_openUploadDialog` if the window is still open by now.
        this._openUploadDialog();
    }
}
