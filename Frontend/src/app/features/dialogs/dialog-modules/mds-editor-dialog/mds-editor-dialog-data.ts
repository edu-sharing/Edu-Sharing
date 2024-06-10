import { Node, RestConstants } from '../../../../core-module/core.module';
import { BulkBehavior, EditorMode, Values } from '../../../mds/types/types';

class SharedData {
    groupId?: string;
    immediatelyShowMissingRequiredWidgets? = false;
}

abstract class MdsEditorDialogDataAbstract extends SharedData {
    bulkBehavior? = BulkBehavior.Default;
}
export class MdsEditorDialogDataNodes extends MdsEditorDialogDataAbstract {
    nodes: Node[];
}
export class MdsEditorDialogDataGraphql extends MdsEditorDialogDataNodes {
    graphqlIds: string[];
}

export class MdsEditorDialogDataValues extends SharedData {
    values: Values;
    repository?: string = RestConstants.HOME_REPOSITORY;
    setId: string;
    editorMode: EditorMode;
}

export function hasGraphql(data: MdsEditorDialogData): data is MdsEditorDialogDataNodes {
    return !!(data as MdsEditorDialogDataGraphql).graphqlIds;
}
export function hasNodes(data: MdsEditorDialogData): data is MdsEditorDialogDataNodes {
    return !!(data as MdsEditorDialogDataNodes).nodes;
}

export function hasValues(data: MdsEditorDialogData): data is MdsEditorDialogDataValues {
    return !!(data as MdsEditorDialogDataValues).values;
}

export type MdsEditorDialogData =
    | MdsEditorDialogDataNodes
    | MdsEditorDialogDataGraphql
    | MdsEditorDialogDataValues;

export type MdsEditorDialogResultNodes = Node[] | null;
export type MdsEditorDialogResultValues = Values | null;
export type MdsEditorDialogResult =
    | MdsEditorDialogResultNodes
    | MdsEditorDialogResultValues
    | 'NO-CHANGES'
    | 'CANCEL';
