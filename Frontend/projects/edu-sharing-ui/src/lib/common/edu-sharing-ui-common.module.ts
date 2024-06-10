import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatRippleModule } from '@angular/material/core';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterModule } from '@angular/router';
import { TranslateModule } from '@ngx-translate/core';
import { ActionbarComponent } from '../actionbar/actionbar.component';
import { BorderBoxObserverDirective } from '../directives/border-box-observer.directive';
import { CheckTextOverflowDirective } from '../directives/check-text-overflow.directive';
import { FocusStateDirective } from '../directives/focus-state.directive';
import { IconDirective } from '../directives/icon.directive';
import { InfiniteScrollDirective } from '../directives/infinite-scroll.directive';
import { DropdownComponent } from '../dropdown/dropdown.component';
import { NodeUrlComponent } from '../node-url/node-url.component';
import { FormatSizePipe } from '../pipes/file-size.pipe';
import { FormatDatePipe } from '../pipes/format-date.pipe';
import { NodeIconPipe } from '../pipes/node-icon.pipe';
import { NodeImageSizePipe } from '../pipes/node-image-size.pipe';
import { NodeImagePipe } from '../pipes/node-image.pipe';
import { NodePersonNamePipe } from '../pipes/node-person-name.pipe';
import { NodeTitlePipe } from '../pipes/node-title.pipe';
import { OptionTooltipPipe } from '../pipes/option-tooltip.pipe';
import { ReplaceCharsPipe } from '../pipes/replace-chars.pipe';
import { VCardNamePipe } from '../pipes/vcard-name.pipe';
import { SortDropdownComponent } from '../sort-dropdown/sort-dropdown.component';
import { SpinnerComponent } from '../spinner/spinner.component';
import { PropertySlugPipe } from '../pipes/property-slug.pipe';

@NgModule({
    declarations: [
        ActionbarComponent,
        BorderBoxObserverDirective,
        CheckTextOverflowDirective,
        DropdownComponent,
        FocusStateDirective,
        FormatDatePipe,
        FormatSizePipe,
        IconDirective,
        InfiniteScrollDirective,
        PropertySlugPipe,
        NodeIconPipe,
        NodeImagePipe,
        NodeImageSizePipe,
        NodePersonNamePipe,
        NodeTitlePipe,
        NodeUrlComponent,
        OptionTooltipPipe,
        ReplaceCharsPipe,
        SortDropdownComponent,
        VCardNamePipe,
    ],
    imports: [
        CommonModule,
        MatMenuModule,
        SpinnerComponent,
        MatButtonModule,
        MatTooltipModule,
        MatRippleModule,
        TranslateModule,
        RouterModule,
    ],
    exports: [
        ActionbarComponent,
        BorderBoxObserverDirective,
        CheckTextOverflowDirective,
        DropdownComponent,
        FocusStateDirective,
        FormatDatePipe,
        FormatSizePipe,
        IconDirective,
        InfiniteScrollDirective,
        PropertySlugPipe,
        NodeIconPipe,
        NodeImagePipe,
        NodeImageSizePipe,
        NodePersonNamePipe,
        NodeTitlePipe,
        NodeUrlComponent,
        SortDropdownComponent,
        SpinnerComponent,
        VCardNamePipe,
    ],
})
export class EduSharingUiCommonModule {}
