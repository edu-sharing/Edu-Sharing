import { Connector, ConnectorFileType } from 'ngx-edu-sharing-api';

export class AddWithConnectorDialogData {
    connector: Connector;
    name?: string;
    data?: { [key in string]: string[] };
}

export interface AddWithConnectorDialogResult {
    name: string;
    type: ConnectorFileType;
    data: { [key in string]: string[] };
}
