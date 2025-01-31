import {
    MdsEditorWidgetComponent,
    MdsWidgetType,
    NativeWidgetClass,
    NativeWidgetType,
} from './types';
import { MdsEditorWidgetPreviewComponent } from '../mds-editor/widgets/mds-editor-widget-preview/mds-editor-widget-preview.component';
import { MdsEditorWidgetAuthorComponent } from '../mds-editor/widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import { MdsEditorWidgetVersionComponent } from '../mds-editor/widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import { MdsEditorWidgetChildobjectsComponent } from '../mds-editor/widgets/mds-editor-widget-childobjects/mds-editor-widget-childobjects.component';
import { MdsEditorWidgetLinkComponent } from '../mds-editor/widgets/mds-editor-widget-link/mds-editor-widget-link.component';
import { MdsEditorWidgetLicenseComponent } from '../mds-editor/widgets/mds-editor-widget-license/mds-editor-widget-license.component';
import { MdsEditorWidgetFileUploadComponent } from '../mds-editor/widgets/mds-editor-widget-file-upload/mds-editor-widget-file-upload.component';
import { MdsEditorWidgetTextComponent } from '../mds-editor/widgets/mds-editor-widget-text/mds-editor-widget-text.component';
import { MdsEditorWidgetTinyMCEComponent } from '../mds-editor/widgets/mds-editor-widget-wysiwyg-html/mds-editor-widget-tinymce.component';
import { MdsEditorWidgetVCardComponent } from '../mds-editor/widgets/mds-editor-widget-vcard/mds-editor-widget-vcard.component';
import { MdsEditorWidgetCheckboxComponent } from '../mds-editor/widgets/mds-editor-widget-checkbox/mds-editor-widget-checkbox.component';
import { MdsEditorWidgetRadioButtonComponent } from '../mds-editor/widgets/mds-editor-widget-radio-button/mds-editor-widget-radio-button.component';
import { MdsEditorWidgetCheckboxesComponent } from '../mds-editor/widgets/mds-editor-widget-checkboxes/mds-editor-widget-checkboxes.component';
import {
    MdsEditorWidgetChipsComponent,
    MdsEditorWidgetChipsRangedValueComponent,
} from '../mds-editor/widgets/mds-editor-widget-chips/mds-editor-widget-chips.component';
import { MdsEditorWidgetAuthorityComponent } from '../mds-editor/widgets/mds-editor-widget-authority/mds-editor-widget-authority.component';
import { MdsEditorWidgetSelectComponent } from '../mds-editor/widgets/mds-editor-widget-select/mds-editor-widget-select.component';
import {
    MdsEditorWidgetSliderComponent,
    MdsEditorWidgetSliderRangeComponent,
} from '../mds-editor/widgets/mds-editor-widget-slider/mds-editor-widget-slider.component';
import { MdsEditorWidgetDurationComponent } from '../mds-editor/widgets/mds-editor-widget-duration/mds-editor-widget-duration.component';
import { MdsEditorWidgetTreeComponent } from '../mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree.component';
import { MdsEditorWidgetFacetListComponent } from '../mds-editor/widgets/mds-editor-widget-facet-list/mds-editor-widget-facet-list.component';
import { MdsEditorWidgetCollectionsComponent } from '../mds-editor/widgets/mds-editor-widget-collections/mds-editor-widget-collections.component';

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
    // rendering specific
    collections: MdsEditorWidgetCollectionsComponent,
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
    [MdsWidgetType.TinyMCE]: MdsEditorWidgetTinyMCEComponent,
    [MdsWidgetType.VCard]: MdsEditorWidgetVCardComponent,
    [MdsWidgetType.Checkbox]: MdsEditorWidgetCheckboxComponent,
    [MdsWidgetType.RadioHorizontal]: MdsEditorWidgetRadioButtonComponent,
    [MdsWidgetType.RadioVertical]: MdsEditorWidgetRadioButtonComponent,
    [MdsWidgetType.CheckboxHorizontal]: MdsEditorWidgetCheckboxesComponent,
    [MdsWidgetType.CheckboxVertical]: MdsEditorWidgetCheckboxesComponent,
    [MdsWidgetType.MultiValueBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.SingleValueSuggestBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueSuggestBadges]: MdsEditorWidgetChipsComponent,
    [MdsWidgetType.MultiValueFixedBadges]: MdsEditorWidgetChipsRangedValueComponent,
    [MdsWidgetType.MultiValueAuthorityBadges]: MdsEditorWidgetAuthorityComponent,
    [MdsWidgetType.Singleoption]: MdsEditorWidgetSelectComponent,
    [MdsWidgetType.Slider]: MdsEditorWidgetSliderComponent,
    [MdsWidgetType.Range]: MdsEditorWidgetSliderRangeComponent,
    [MdsWidgetType.Duration]: MdsEditorWidgetDurationComponent,
    [MdsWidgetType.SingleValueTree]: MdsEditorWidgetTreeComponent,
    [MdsWidgetType.MultiValueTree]: MdsEditorWidgetTreeComponent,
    [MdsWidgetType.DefaultValue]: null,
    [MdsWidgetType.FacetList]: MdsEditorWidgetFacetListComponent,
};
