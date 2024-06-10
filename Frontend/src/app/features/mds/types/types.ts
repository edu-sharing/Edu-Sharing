import { Type } from '@angular/core';
import { MdsEditorWidgetBase } from '../mds-editor/widgets/mds-editor-widget-base';
import { MdsWidget } from 'ngx-edu-sharing-api';
import { BehaviorSubject, Observable } from 'rxjs';
import { Node } from '../../../core-module/rest/data-object';

export {
    MdsDefinition,
    MdsGroup,
    MdsValue as MdsWidgetValue,
    MdsView,
    MdsWidget,
    MdsWidgetCondition,
} from 'ngx-edu-sharing-api';

/** Error with a translatable message that is suitable to be shown to the user. */
export class UserPresentableError extends Error {
    constructor(message: string) {
        super(message);
        // Apparently, this is what it's gonna be for now...
        // https://github.com/Microsoft/TypeScript/wiki/Breaking-Changes#extending-built-ins-like-error-array-and-map-may-no-longer-work
        Object.setPrototypeOf(this, UserPresentableError.prototype);
        this.name = 'UserPresentableError';
    }
}

export interface Constraints {
    supportsInlineEditing?: boolean;
    requiresNode?: boolean;
    supportsBulk?: boolean;
    /**
     * shall the widget show an error or only be hidden
     */
    onConstraintFailed?: 'showError' | 'hide';
}

export type Values = { [property: string]: string[] | null };

/** User-selectable Bulk mode per field */
export type BulkMode = 'no-change' | 'replace';

/** Bulk mode and -behavior of the editor. */
export type EditorBulkMode =
    | {
          isBulk: false;
      }
    // The user toggles editing per-field.
    | {
          isBulk: true;
          bulkBehavior: BulkBehavior.Default;
      }
    // All fields are replaced.
    | {
          isBulk: true;
          bulkBehavior: BulkBehavior.Replace;
      };

export type InputStatus = 'VALID' | 'INVALID' | 'DISABLED' | 'PENDING';

export enum MdsType {
    Io = 'io',
    IoBulk = 'io_bulk',
    Map = 'map',
    MapRef = 'map_ref',
    IoChildObject = 'io_childobject',
    Collection = 'collection',
    ToolDefinition = 'tool_definition',
    ToolInstance = 'tool_instance',
    SavedSearch = 'saved_search',
}

export enum MdsWidgetType {
    Text = 'text',
    Number = 'number',
    Email = 'email',
    Date = 'date',
    Month = 'month',
    Color = 'color',
    Textarea = 'textarea',
    TinyMCE = 'tinyMCE',
    VCard = 'vcard',
    Checkbox = 'checkbox',
    RadioHorizontal = 'radioHorizontal',
    RadioVertical = 'radioVertical',
    CheckboxHorizontal = 'checkboxHorizontal',
    CheckboxVertical = 'checkboxVertical',
    MultiValueBadges = 'multivalueBadges',
    MultiValueFixedBadges = 'multivalueFixedBadges',
    MultiValueSuggestBadges = 'multivalueSuggestBadges',
    MultiValueAuthorityBadges = 'multivalueAuthorityBadges',
    Singleoption = 'singleoption',
    Slider = 'slider',
    Range = 'range',
    Duration = 'duration',
    SingleValueTree = 'singlevalueTree',
    SingleValueSuggestBadges = 'singlevalueSuggestBadges',
    MultiValueTree = 'multivalueTree',
    DefaultValue = 'defaultvalue',
    FacetList = 'facetList',
}

// Entries must be lowercase only.
export enum NativeWidgetType {
    Preview = 'preview',
    Version = 'version',
    ChildObjects = 'childobjects',
    Maptemplate = 'maptemplate',
    License = 'license',
    FileUpload = 'fileupload',
    Workflow = 'workflow',
    Author = 'author',
    Contributor = 'contributor',
}

export type MdsEditorWidgetComponent = {
    mapGraphqlId: (definition: MdsWidget) => string[] | null;
    /**
     *     required suggestion fields for graphql suggestions
     *     should return empty array if not supported by the widget
     */
    mapGraphqlSuggestionId: (definition: MdsWidget) => string[];
} & Type<MdsEditorWidgetBase>;

export type EditorType = 'angular' | 'legacy';

export interface MdsList {
    columns: MdsListColumn[];
    id: string;
}

export interface MdsListColumn {
    format: null;
    id: string;
    showDefault: boolean;
}

export enum RequiredMode {
    Mandatory = 'mandatory',
    MandatoryForPublish = 'mandatoryForPublish',
    Recommended = 'recommended',
    Optional = 'optional',
    Ignore = 'ignore',
}

export function assertUnreachable(x: never): never {
    throw new Error('Did not expect to get here');
}

export enum BulkBehavior {
    Default, // default equals no replace on choose, but show options
    Replace, // Don't display settings, simply replace for all (usefull after uploads)
}

/**
 * - `nodes`:
 *   - Supports bulk.
 *   - Returns only changed values.
 * - `search`:
 *   - No bulk.
 *   - All values returned.
 *   - Trees sub-children are auto-selected if root is selected.
 *   - Required errors and -warnings are disabled.
 * - `form`:
 *   - No bulk.
 *   - All values returned.
 * - `inline`
 *   - No bulk
 *   - Editing individual values on demand
 *   - default apperance is read only
 * - `viewer`
 *   - No editing
 *   - Read only
 *   - Triggered via mds-viewer
 */
export type EditorMode = 'nodes' | 'search' | 'form' | 'inline' | 'viewer';

export interface NativeWidgetComponent {
    hasChanges: BehaviorSubject<boolean>;
    onSaveNode?: (nodes: Node[]) => Promise<Node[]>;
    getValues?: (values: Values, node: Node) => Promise<Values>;
    status?: Observable<InputStatus>;
    focus?: () => void;
}

export type NativeWidgetClass = {
    constraints: Constraints;
} & Type<NativeWidgetComponent>;

/**
 * NativeWidget and Widget
 */
export interface GeneralWidget {
    status: Observable<InputStatus>;
    viewId: string;
}

// TODO: use this object for data properties and register it with the component.
export interface NativeWidget extends GeneralWidget {
    component: NativeWidgetComponent;
}
