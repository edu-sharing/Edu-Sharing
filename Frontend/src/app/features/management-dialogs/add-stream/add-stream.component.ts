import { Component, EventEmitter, Input, Output } from '@angular/core';
import {
    AuthorityProfile,
    ConfigurationService,
    DialogButton,
    LoginResult,
    Node,
    RestConnectorService,
    RestConstants,
    RestHelper,
    RestIamService,
    RestNodeService,
    RestStreamService,
    STREAM_STATUS,
} from '../../../core-module/core.module';
import { Toast } from '../../../services/toast';
import { trigger } from '@angular/animations';
import { Helper } from '../../../core-module/rest/helper';
import { UIAnimation } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-add-stream',
    templateUrl: 'add-stream.component.html',
    styleUrls: ['add-stream.component.scss'],
    animations: [
        trigger('overlay', UIAnimation.openOverlay(UIAnimation.ANIMATION_TIME_FAST)),
        trigger('fade', UIAnimation.fade()),
        trigger('cardAnimation', UIAnimation.cardAnimation()),
    ],
})
export class AddStreamComponent {
    private streamEntry: any = {};
    reloadMds = new Boolean(true);
    AUDIENCE_MODE_EVERYONE = '0';
    AUDIENCE_MODE_CUSTOM = '1';
    audienceMode = this.AUDIENCE_MODE_EVERYONE;
    _nodes: any;
    invite: AuthorityProfile[] = [];
    buttons: DialogButton[];
    @Input() set nodes(nodes: Node[]) {
        this._nodes = nodes;
    }
    @Output() onCancel = new EventEmitter();
    @Output() onLoading = new EventEmitter();
    @Output() onDone = new EventEmitter();
    constructor(
        private connector: RestConnectorService,
        private streamApi: RestStreamService,
        private toast: Toast,
    ) {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('SAVE', { color: 'primary' }, () => this.save()),
        ];
        this.connector.isLoggedIn(false).subscribe((data: LoginResult) => {});
    }
    public cancel() {
        this.onCancel.emit();
    }
    public done() {
        this.onDone.emit();
    }
    public addInvite(event: AuthorityProfile) {
        if (Helper.indexOfObjectArray(this.invite, 'authorityName', event.authorityName) == -1)
            this.invite.push(event);
    }
    public removeInvite(event: AuthorityProfile) {
        this.invite.splice(this.invite.indexOf(event), 1);
    }
    public save() {
        let values = null; // this.mdsRef.getValues();
        if (!values) {
            return;
        }
        if (this.audienceMode == this.AUDIENCE_MODE_CUSTOM && this.invite.length == 0) {
            this.toast.error(null, 'ADD_TO_STREAM.ERROR.NO_PERSON_INVITED');
            return;
        }
        this.onLoading.emit(true);
        this.streamEntry.title = values['add_to_stream_title'][0];
        this.streamEntry.priority = 5; //values['add_to_stream_priority'][0];
        this.streamEntry.description = values['add_to_stream_description']
            ? values['add_to_stream_description'][0]
            : null;
        this.streamEntry.properties = values;
        this.streamEntry.nodes = RestHelper.getNodeIds(this._nodes);
        this.streamApi.addEntry(this.streamEntry).subscribe(
            (data: any) => {
                let id = data.id;
                if (this.audienceMode == this.AUDIENCE_MODE_EVERYONE) {
                    this.streamApi
                        .updateStatus(id, RestConstants.AUTHORITY_EVERYONE, STREAM_STATUS.OPEN)
                        .subscribe(() => {
                            this.onLoading.emit(false);
                            this.onDone.emit();
                            this.toast.toast('ADD_TO_STREAM.SUCCESSFUL');
                        });
                } else {
                    this.invitePersons(id);
                }
            },
            (error: any) => {
                this.onLoading.emit(false);
                this.toast.error(error);
            },
        );
    }

    private invitePersons(id: string, position = 0) {
        if (position == this.invite.length) {
            this.onLoading.emit(false);
            this.onDone.emit();
            return;
        }
        this.streamApi
            .updateStatus(id, this.invite[position].authorityName, STREAM_STATUS.OPEN)
            .subscribe(() => {
                this.invitePersons(id, position + 1);
            });
    }
}
