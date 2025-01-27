import { Component, EventEmitter, Input, NgZone, OnInit, Output } from '@angular/core';
import { LtiPlatformService, Node, Tool } from 'ngx-edu-sharing-api';
import { DialogButton } from '../../../util/dialog-button';
import { RestNodeService } from '../../../core-module/core.module';
import { NodeHelperService } from '../../../services/node-helper.service';

@Component({
    selector: 'es-create-ltitool',
    templateUrl: './create-ltitool.component.html',
    styleUrls: ['./create-ltitool.component.scss'],
})
export class CreateLtitoolComponent implements OnInit {
    public _tool: Tool;
    public _parent: Node;
    buttons: DialogButton[];
    @Output() onCancel = new EventEmitter();
    @Output() onCreate = new EventEmitter();
    public _name = '';
    nodes: Node[] = [];

    constructor(
        private ngZone: NgZone,
        private nodeService: RestNodeService,
        private nodeHelper: NodeHelperService,
        private ltiPlatformService: LtiPlatformService,
    ) {
        this.buttons = [
            new DialogButton('CANCEL', { color: 'standard' }, () => this.cancel()),
            new DialogButton('CREATE', { color: 'primary' }, () => this.create()),
        ];
        (window as any)['angularComponentReference'] = {
            component: this,
            zone: this.ngZone,
            loadAngularFunction: (nodeIds: string[], titles: string[]) =>
                this.deeplinkResponse(nodeIds, titles),
        };
    }

    ngOnInit(): void {
        if (!this._tool) {
            return;
        }
        console.debug(
            'this._tool.appId:' +
                this._tool.appId +
                ' parentId:' +
                this._parent.ref.id +
                ' nodes:' +
                this.nodes.length,
        );
        if (!this._tool.customContentOption) {
            this.openDeepLinkFlow();
        }
    }

    @Input() set tool(tool: Tool) {
        this._tool = tool;
    }

    @Input() set parent(parent: Node) {
        this._parent = parent;
    }

    public cancel() {
        this.onCancel.emit({ nodes: this.nodes });
    }

    public create() {
        this.onCreate.emit({ nodes: this.nodes, name: this._name });
    }

    public openDeepLinkFlow() {
        let url =
            '/edu-sharing/rest/ltiplatform/v13/generateLoginInitiationForm?appId=' +
            this._tool.appId +
            '&parentId=' +
            this._parent.ref.id;
        let w = window.open(url, '_blank');

        if (!w) {
            window.alert('popups are disabled');
        }
    }

    public deeplinkResponse(nodeIds: string[], titles: string[]) {
        console.log('js function called ' + nodeIds + ' titles:' + titles + ' test');
        this._name = titles[0];

        let idx = 0;
        nodeIds.forEach((nodeId) => {
            let node = (this.nodes[idx] = {
                ref: { id: nodeId },
                name: titles[idx],
            } as Node);
            idx++;
        });
        console.log(this.nodes);
    }
}
