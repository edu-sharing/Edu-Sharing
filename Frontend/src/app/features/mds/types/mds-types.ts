import {
    Constraints,
    InputStatus,
    MdsEditorWidgetComponent,
    MdsWidgetType,
    NativeWidgetType,
    Values
} from './types';
import {
    MdsEditorWidgetPreviewComponent
} from '../mds-editor/widgets/mds-editor-widget-preview/mds-editor-widget-preview.component';
import {
    MdsEditorWidgetAuthorComponent
} from '../mds-editor/widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import {
    MdsEditorWidgetVersionComponent
} from '../mds-editor/widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import {
    MdsEditorWidgetChildobjectsComponent
} from '../mds-editor/widgets/mds-editor-widget-childobjects/mds-editor-widget-childobjects.component';
import {
    MdsEditorWidgetLinkComponent
} from '../mds-editor/widgets/mds-editor-widget-link/mds-editor-widget-link.component';
import {
    MdsEditorWidgetLicenseComponent
} from '../mds-editor/widgets/mds-editor-widget-license/mds-editor-widget-license.component';
import {
    MdsEditorWidgetFileUploadComponent
} from '../mds-editor/widgets/mds-editor-widget-file-upload/mds-editor-widget-file-upload.component';
import {
    MdsEditorWidgetTextComponent
} from '../mds-editor/widgets/mds-editor-widget-text/mds-editor-widget-text.component';
import {
    MdsEditorWidgetTinyMCE
} from '../mds-editor/widgets/mds-editor-widget-wysiwyg-html/mds-editor-widget-tinymce.component';
import {
    MdsEditorWidgetVCardComponent
} from '../mds-editor/widgets/mds-editor-widget-vcard/mds-editor-widget-vcard.component';
import {
    MdsEditorWidgetCheckboxComponent
} from '../mds-editor/widgets/mds-editor-widget-checkbox/mds-editor-widget-checkbox.component';
import {
    MdsEditorWidgetRadioButtonComponent
} from '../mds-editor/widgets/mds-editor-widget-radio-button/mds-editor-widget-radio-button.component';
import {
    MdsEditorWidgetCheckboxesComponent
} from '../mds-editor/widgets/mds-editor-widget-checkboxes/mds-editor-widget-checkboxes.component';
import {
    MdsEditorWidgetChipsComponent
} from '../mds-editor/widgets/mds-editor-widget-chips/mds-editor-widget-chips.component';
import {
    MdsEditorWidgetAuthorityComponent
} from '../mds-editor/widgets/mds-editor-widget-authority/mds-editor-widget-authority.component';
import {
    MdsEditorWidgetSelectComponent
} from '../mds-editor/widgets/mds-editor-widget-select/mds-editor-widget-select.component';
import {
    MdsEditorWidgetSliderComponent
} from '../mds-editor/widgets/mds-editor-widget-slider/mds-editor-widget-slider.component';
import {
    MdsEditorWidgetDurationComponent
} from '../mds-editor/widgets/mds-editor-widget-duration/mds-editor-widget-duration.component';
import {
    MdsEditorWidgetTreeComponent
} from '../mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree.component';
import {
    MdsEditorWidgetFacetListComponent
} from '../mds-editor/widgets/mds-editor-widget-facet-list/mds-editor-widget-facet-list.component';
import {BehaviorSubject, Observable} from 'rxjs';
import {Node} from '../../../core-module/rest/data-object';
import {Type} from '@angular/core';
import {Metadata} from 'ngx-edu-sharing-graphql';

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
 */
export type EditorMode = 'nodes' | 'search' | 'form' | 'inline';

export const NativeWidgets: {
    [widgetType in NativeWidgetType]: NativeWidgetClass;
} = {
    preview: MdsEditorWidgetPreviewComponent,
    author: MdsEditorWidgetAuthorComponent,
    version: MdsEditorWidgetVersionComponent,
    childobjects: MdsEditorWidgetChildobjectsComponent,
    maptemplate: MdsEditorWidgetLinkComponent,
    contributor: MdsEditorWidgetLinkComponent,
    license: MdsEditorWidgetLicenseComponent,
    fileupload: MdsEditorWidgetFileUploadComponent,
    workflow: null as null,
};

export const WidgetComponents: {
    [type in MdsWidgetType]: MdsEditorWidgetComponent;
} = {
    [MdsWidgetType.Text]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Number]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Email]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Date]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Month]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Color]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.Textarea]: MdsEditorWidgetTextComponent,
    [MdsWidgetType.TinyMCE]: MdsEditorWidgetTinyMCE,
    [MdsWidgetType.VCard]: MdsEditorWidgetVCardComponent,
    [MdsWidgetType.Checkbox]: MdsEditorWidgetCheckboxComponent,
    [MdsWidgetType.RadioHorizontal]: MdsEditorWidgetRadioButtonComponent,
    [MdsWidgetType.RadioVertical]: MdsEditorWidgetRadioButtonComponent,
    [MdsWidgetType.CheckboxHorizontal]: MdsEditorWidgetCheckboxesComponent,
    [MdsWidgetType.CheckboxVertical]: MdsEditorWidgetCheckboxesComponent,
    [MdsWidgetType.MultiValueBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueSuggestBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueFixedBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueAuthorityBadges]: MdsEditorWidgetAuthorityComponent,
    [MdsWidgetType.Singleoption]: MdsEditorWidgetSelectComponent,
    [MdsWidgetType.Slider]: MdsEditorWidgetSliderComponent,
    [MdsWidgetType.Range]: MdsEditorWidgetSliderComponent,
    [MdsWidgetType.Duration]: MdsEditorWidgetDurationComponent,
    [MdsWidgetType.SingleValueTree]: MdsEditorWidgetTreeComponent,
    [MdsWidgetType.MultiValueTree]: MdsEditorWidgetTreeComponent,
    [MdsWidgetType.DefaultValue]: null,
    [MdsWidgetType.FacetList]: MdsEditorWidgetFacetListComponent,
};

export interface NativeWidgetComponent {
    hasChanges: BehaviorSubject<boolean>;
    onSaveNode?: (nodes: Node[]) => Promise<Node[]>;
    getValues?: (values: Values, node: Node) => Promise<Values>;
    getValuesGraphql?: (values: Metadata, node: Metadata) => Promise<Metadata>;
    status?: Observable<InputStatus>;
    focus?: () => void;
}

export type NativeWidgetClass = {
    constraints: Constraints;
    // ids of fields in dot-notation this widget requires for displaying the node data
    graphqlIds?: string[];
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
