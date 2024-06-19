import { ModuleWithProviders, NgModule } from '@angular/core';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import { EduSharingUiConfigurationParams, getConfigProvider } from './edu-sharing-ui-configuration';
import { ListItemsModule } from './list-items/list-items.module';
import { NodeEntriesModule } from './node-entries/node-entries.module';
import { EduSharingUiCommonModule } from './common/edu-sharing-ui-common.module';
import { NodeImageSizePipe } from './pipes/node-image-size.pipe';
import { FormatDatePipe } from './pipes/format-date.pipe';
import { ListItemLabelPipe } from './node-entries/list-item-label.pipe';
import { SortDropdownComponent } from './sort-dropdown/sort-dropdown.component';
import { MdsModule } from './mds/mds.module';

@NgModule({
    declarations: [],
    imports: [
        CommonModule,
        TranslateModule,
        EduSharingUiCommonModule,
        ListItemsModule,
        NodeEntriesModule,
        // Loading the TranslationsModule here causes errors for lazy-loaded pages like the search
        // page. For usage outside the context of edu-sharing, we probably need to import the
        // TranslationsModule at the main module that packages or uses this library.
    ],
    exports: [
        CommonModule,
        MdsModule,
        MatTooltipModule,
        TranslateModule,
        EduSharingUiCommonModule,
        ListItemsModule,
        NodeEntriesModule,
        NodeImageSizePipe,
        FormatDatePipe,
        ListItemsModule,
        ListItemLabelPipe,
        SortDropdownComponent,
    ],
})
export class EduSharingUiModule {
    public static forRoot(
        config: EduSharingUiConfigurationParams,
    ): ModuleWithProviders<EduSharingUiModule> {
        return {
            ngModule: EduSharingUiModule,
            providers: [getConfigProvider(config)],
        };
    }
}
