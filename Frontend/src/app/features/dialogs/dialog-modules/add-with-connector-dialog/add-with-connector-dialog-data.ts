import { Connector, ConnectorFileType } from 'ngx-edu-sharing-api';

export class AddWithConnectorDialogData {
    connector: Connector;
    name?: string;
}

export interface AddWithConnectorDialogResult {
    name: string;
    type: ConnectorFileType;
    window: Window;
}
