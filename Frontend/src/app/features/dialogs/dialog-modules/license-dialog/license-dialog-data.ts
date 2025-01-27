import { Node } from 'ngx-edu-sharing-api';

export type LicenseDialogData =
    | {
          kind: 'nodes';
          nodes: Node[];
      }
    | {
          kind: 'properties';
          properties: any;
      };

export type LicenseDialogResult = Node[] | any | null;
