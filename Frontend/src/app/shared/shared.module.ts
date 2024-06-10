import { A11yModule } from '@angular/cdk/a11y';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { OverlayModule } from '@angular/cdk/overlay';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatBadgeModule } from '@angular/material/badge';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatNativeDateModule, MatOptionModule, MatRippleModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatMenuModule } from '@angular/material/menu';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSortModule } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTreeModule } from '@angular/material/tree';
import { RouterModule } from '@angular/router';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { NgxSliderModule } from 'ngx-slider-v2';
import { environment } from '../../environments/environment';
import { AuthorityRowComponent } from './components/authority-search-input/authority-row/authority-row.component';
import { AuthoritySearchInputComponent } from './components/authority-search-input/authority-search-input.component';
import { BreadcrumbsComponent } from './components/breadcrumbs/breadcrumbs.component';
import { CalendarComponent } from './components/calendar/calendar.component';
import { CardComponent } from './components/card/card.component';
import { CollectionChooserComponent } from './components/collection-chooser/collection-chooser.component';
import { FooterComponent } from './components/footer/footer.component';
import { GlobalProgressComponent } from './components/global-progress/global-progress.component';
import { InfoMessageComponent } from './components/info-message/info-message.component';
import { InputPasswordComponent } from './components/input-password/input-password.component';
import { LicenseSourceComponent } from './components/license-source/license-source.component';
import { LinkComponent } from './components/link/link.component';
import { MultiLineLabelComponent } from './components/multi-line-label/multi-line-label.component';
import { PoweredByComponent } from './components/powered-by/powered-by.component';
import { SpinnerSmallComponent } from './components/spinner-small/spinner-small.component';
import { TutorialComponent } from './components/tutorial/tutorial.component';
import { UserAvatarComponent } from './components/user-avatar/user-avatar.component';
import { UserQuotaComponent } from './components/user-quota/user-quota.component';
import { CreateLtitoolComponent } from './dialogs/create-ltitool/create-ltitool.component';
import { ElementRefDirective } from './directives/element-ref.directive';
import { EscapeHtmlPipe } from './directives/escape-html.pipe';
import { FileDropDirective } from './directives/file-drop';
import { ImageConfigDirective } from './directives/image-config.directive';
import { OnAttributeChangeDirective } from './directives/on-attribute-change.directive';
import { RegisterCustomPropertyDirective } from './directives/register-custom-property.directive';
import { SkipTargetDirective } from './directives/skip-target.directive';
import { TitleDirective } from './directives/title.directive';
import { ToolpermissionCheckDirective } from './directives/toolpermission-check.directive';
import { AssetsPathPipe } from './pipes/assets-path.pipe';
import { AuthorityAffiliationPipe } from './pipes/authority-affiliation.pipe';
import { AuthorityColorPipe } from './pipes/authority-color.pipe';
import { AuthorityNamePipe } from './pipes/authority-name.pipe';
import { BitwisePipe } from './pipes/bitwise.pipe';
import { PermissionNamePipe } from './pipes/permission-name.pipe';
import { SafeHtmlPipe } from './pipes/safe-html.pipe';
import { SplitNewLinesPipe } from './pipes/split-new-lines.pipe';
import { VersionLabelPipe } from './pipes/version-label.pipe';
import { SmallCollectionComponent } from './components/small-collection/small-collection.component';
import { ImprintPrivacyComponent } from './components/imprint-privacy-footer/imprint-privacy.component';

@NgModule({
    declarations: [
        AssetsPathPipe,
        AuthorityAffiliationPipe,
        AuthorityColorPipe,
        AuthorityNamePipe,
        AuthorityRowComponent,
        AuthoritySearchInputComponent,
        BitwisePipe,
        BreadcrumbsComponent,
        CalendarComponent,
        CardComponent,
        CollectionChooserComponent,
        CreateLtitoolComponent,
        ElementRefDirective,
        EscapeHtmlPipe,
        FileDropDirective,
        FooterComponent,
        GlobalProgressComponent,
        ImageConfigDirective,
        InfoMessageComponent,
        InputPasswordComponent,
        LicenseSourceComponent,
        LinkComponent,
        MultiLineLabelComponent,
        OnAttributeChangeDirective,
        PermissionNamePipe,
        PoweredByComponent,
        RegisterCustomPropertyDirective,
        SafeHtmlPipe,
        SkipTargetDirective,
        SmallCollectionComponent,
        SpinnerSmallComponent,
        SplitNewLinesPipe,
        TitleDirective,
        ToolpermissionCheckDirective,
        TutorialComponent,
        UserAvatarComponent,
        ImprintPrivacyComponent,
        UserQuotaComponent,
        VersionLabelPipe,
    ],
    imports: [
        A11yModule,
        CommonModule,
        DragDropModule,
        FormsModule,
        MatAutocompleteModule,
        MatButtonModule,
        MatButtonToggleModule,
        MatCardModule,
        MatDatepickerModule,
        MatInputModule,
        MatMenuModule,
        MatOptionModule,
        MatProgressSpinnerModule,
        MatRippleModule,
        MatSelectModule,
        MatTooltipModule,
        NgxSliderModule,
        ReactiveFormsModule,
        RouterModule,
        EduSharingUiModule.forRoot({
            production: environment.production,
        }),
    ],
    exports: [
        A11yModule,
        AssetsPathPipe,
        AuthorityAffiliationPipe,
        AuthorityColorPipe,
        AuthorityNamePipe,
        AuthorityRowComponent,
        AuthoritySearchInputComponent,
        BitwisePipe,
        BreadcrumbsComponent,
        CalendarComponent,
        CardComponent,
        CollectionChooserComponent,
        CommonModule,
        CreateLtitoolComponent,
        DragDropModule,
        EduSharingUiModule,
        ElementRefDirective,
        EscapeHtmlPipe,
        FileDropDirective,
        FooterComponent,
        FormsModule,
        GlobalProgressComponent,
        ImageConfigDirective,
        ImprintPrivacyComponent,
        InfoMessageComponent,
        InputPasswordComponent,
        LicenseSourceComponent,
        LinkComponent,
        MatAutocompleteModule,
        MatBadgeModule,
        MatButtonModule,
        MatButtonToggleModule,
        MatCardModule,
        MatCheckboxModule,
        MatChipsModule,
        MatDatepickerModule,
        MatExpansionModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatMenuModule,
        MatNativeDateModule,
        MatOptionModule,
        MatPaginatorModule,
        MatProgressBarModule,
        MatProgressSpinnerModule,
        MatRadioModule,
        MatRippleModule,
        MatSelectModule,
        MatSidenavModule,
        MatSlideToggleModule,
        MatSnackBarModule,
        MatSortModule,
        MatTableModule,
        MatTabsModule,
        MatTooltipModule,
        MatTreeModule,
        MultiLineLabelComponent,
        NgxSliderModule,
        OnAttributeChangeDirective,
        OverlayModule,
        PermissionNamePipe,
        PoweredByComponent,
        ReactiveFormsModule,
        RegisterCustomPropertyDirective,
        RouterModule,
        SafeHtmlPipe,
        SkipTargetDirective,
        SmallCollectionComponent,
        SpinnerSmallComponent,
        SplitNewLinesPipe,
        TitleDirective,
        ToolpermissionCheckDirective,
        TutorialComponent,
        UserAvatarComponent,
        UserQuotaComponent,
        VersionLabelPipe,
    ],
})
export class SharedModule {}
