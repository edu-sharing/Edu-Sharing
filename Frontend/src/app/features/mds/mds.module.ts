import { NgxSliderModule } from 'ngx-slider-v2';
import { NgModule } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { EditorModule } from '@tinymce/tinymce-angular';
import { SharedModule } from '../../shared/shared.module';
import { MdsComponent } from './legacy/mds/mds.component';
import { MdsEditorCoreComponent } from './mds-editor/mds-editor-core/mds-editor-core.component';
import { MdsEditorEmbeddedComponent } from './mds-editor/mds-editor-embedded/mds-editor-embedded.component';
import { MdsEditorViewComponent } from './mds-editor/mds-editor-view/mds-editor-view.component';
import { MdsEditorWrapperComponent } from './mds-editor/mds-editor-wrapper/mds-editor-wrapper.component';
import { MdsEditorComponent } from './mds-editor/mds-editor.component';
import { SearchFieldFacetsComponent } from './mds-editor/search-field-facets/search-field-facets.component';
import { LabelPipe } from './mds-editor/shared/label.pipe';
import { MdsDurationPipe } from './mds-editor/shared/mds-duration.pipe';
import { MdsEditorWidgetAuthorComponent } from './mds-editor/widgets/mds-editor-widget-author/mds-editor-widget-author.component';
import { MdsEditorWidgetAuthorityComponent } from './mds-editor/widgets/mds-editor-widget-authority/mds-editor-widget-authority.component';
import { MdsEditorWidgetCheckboxComponent } from './mds-editor/widgets/mds-editor-widget-checkbox/mds-editor-widget-checkbox.component';
import { MdsEditorWidgetCheckboxesComponent } from './mds-editor/widgets/mds-editor-widget-checkboxes/mds-editor-widget-checkboxes.component';
import { MdsEditorWidgetChildobjectsComponent } from './mds-editor/widgets/mds-editor-widget-childobjects/mds-editor-widget-childobjects.component';
import {
    MdsEditorWidgetChipsComponent,
    MdsEditorWidgetChipsRangedValueComponent,
} from './mds-editor/widgets/mds-editor-widget-chips/mds-editor-widget-chips.component';
import { MdsEditorWidgetContainerComponent } from './mds-editor/widgets/mds-editor-widget-container/mds-editor-widget-container.component';
import { RegisterFormFieldDirective } from './mds-editor/widgets/mds-editor-widget-container/register-form-field.directive';
import { MdsEditorWidgetDurationComponent } from './mds-editor/widgets/mds-editor-widget-duration/mds-editor-widget-duration.component';
import { MdsEditorWidgetErrorComponent } from './mds-editor/widgets/mds-editor-widget-error/mds-editor-widget-error.component';
import { MdsEditorWidgetFacetListComponent } from './mds-editor/widgets/mds-editor-widget-facet-list/mds-editor-widget-facet-list.component';
import { MdsEditorWidgetFileUploadComponent } from './mds-editor/widgets/mds-editor-widget-file-upload/mds-editor-widget-file-upload.component';
import { LicenseDetailsComponent } from './mds-editor/widgets/mds-editor-widget-license/license-details/license-details.component';
import { MdsEditorWidgetLicenseComponent } from './mds-editor/widgets/mds-editor-widget-license/mds-editor-widget-license.component';
import { MdsEditorWidgetLinkComponent } from './mds-editor/widgets/mds-editor-widget-link/mds-editor-widget-link.component';
import { MdsEditorWidgetPreviewComponent } from './mds-editor/widgets/mds-editor-widget-preview/mds-editor-widget-preview.component';
import { MdsEditorWidgetRadioButtonComponent } from './mds-editor/widgets/mds-editor-widget-radio-button/mds-editor-widget-radio-button.component';
import { MdsEditorWidgetSearchSuggestionsComponent } from './mds-editor/widgets/mds-editor-widget-search-suggestions/mds-editor-widget-search-suggestions.component';
import { MdsEditorWidgetSelectComponent } from './mds-editor/widgets/mds-editor-widget-select/mds-editor-widget-select.component';
import {
    MdsEditorWidgetSliderComponent,
    MdsEditorWidgetSliderRangeComponent,
} from './mds-editor/widgets/mds-editor-widget-slider/mds-editor-widget-slider.component';
import { MdsEditorWidgetSuggestionChipsComponent } from './mds-editor/widgets/mds-editor-widget-suggestion-chips/mds-editor-widget-suggestion-chips.component';
import { MdsEditorWidgetTextComponent } from './mds-editor/widgets/mds-editor-widget-text/mds-editor-widget-text.component';
import { HighlightPipe } from './mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree-core/highlight.pipe';
import { MdsEditorWidgetTreeCoreComponent } from './mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree-core/mds-editor-widget-tree-core.component';
import { MdsEditorWidgetTreeComponent } from './mds-editor/widgets/mds-editor-widget-tree/mds-editor-widget-tree.component';
import { MdsEditorWidgetVCardComponent } from './mds-editor/widgets/mds-editor-widget-vcard/mds-editor-widget-vcard.component';
import { MdsEditorWidgetVersionComponent } from './mds-editor/widgets/mds-editor-widget-version/mds-editor-widget-version.component';
import { MdsEditorWidgetTinyMCE } from './mds-editor/widgets/mds-editor-widget-wysiwyg-html/mds-editor-widget-tinymce.component';
import { MdsViewerComponent } from './mds-viewer/mds-viewer.component';
import { MdsWidgetComponent } from './mds-viewer/widget/mds-widget.component';

@NgModule({
    declarations: [
        HighlightPipe,
        LabelPipe,
        LicenseDetailsComponent,
        MdsComponent,
        MdsEditorComponent,
        MdsEditorCoreComponent,
        MdsEditorEmbeddedComponent,
        MdsEditorViewComponent,
        MdsEditorWidgetAuthorComponent,
        MdsEditorWidgetAuthorityComponent,
        MdsEditorWidgetCheckboxComponent,
        MdsEditorWidgetCheckboxesComponent,
        MdsEditorWidgetChildobjectsComponent,
        MdsEditorWidgetChipsComponent,
        MdsEditorWidgetChipsRangedValueComponent,
        MdsEditorWidgetContainerComponent,
        MdsEditorWidgetDurationComponent,
        MdsEditorWidgetErrorComponent,
        MdsEditorWidgetFacetListComponent,
        MdsEditorWidgetFileUploadComponent,
        MdsEditorWidgetLicenseComponent,
        MdsEditorWidgetLinkComponent,
        MdsEditorWidgetPreviewComponent,
        MdsEditorWidgetRadioButtonComponent,
        MdsEditorWidgetSearchSuggestionsComponent,
        MdsEditorWidgetSelectComponent,
        MdsEditorWidgetSliderComponent,
        MdsEditorWidgetSliderRangeComponent,
        MdsEditorWidgetSuggestionChipsComponent,
        MdsEditorWidgetTextComponent,
        MdsEditorWidgetTinyMCE,
        MdsEditorWidgetTreeComponent,
        MdsEditorWidgetTreeCoreComponent,
        MdsEditorWidgetVCardComponent,
        MdsEditorWidgetVersionComponent,
        MdsEditorWrapperComponent,
        MdsViewerComponent,
        MdsWidgetComponent,
        RegisterFormFieldDirective,
        SearchFieldFacetsComponent,
        MdsDurationPipe,
    ],
    imports: [SharedModule, NgxSliderModule, EditorModule, MatFormFieldModule],
    exports: [
        MdsComponent,
        MdsEditorWidgetAuthorComponent,
        MdsEditorWrapperComponent,
        MdsViewerComponent,
        MdsEditorCoreComponent,
        SearchFieldFacetsComponent,
    ],
})
export class MdsModule {}
