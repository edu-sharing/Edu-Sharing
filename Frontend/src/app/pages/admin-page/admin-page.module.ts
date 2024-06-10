import { NgModule } from '@angular/core';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { MdsModule } from '../../features/mds/mds.module';
import { SharedModule } from '../../shared/shared.module';
import { AdminPageRoutingModule } from './admin-page-routing.module';
import { AdminPageComponent } from './admin-page.component';
import { AutocompleteComponent } from './autocomplete/autocomplete.component';
import { AdminConfigComponent } from './config/config.component';
import { AdminFrontpageComponent } from './frontpage/frontpage.component';
import { LtiAdminComponent } from './lti-admin/lti-admin.component';
import { LtitoolAdminComponent } from './ltitool-admin/ltitool-admin.component';
import { LuceneTemplateMemoryComponent } from './lucene-template-memory/lucene-template-memory.component';
import { AdminMediacenterComponent } from './mediacenter/mediacenter.component';
import { AdminPluginsComponent } from './plugins/plugins.component';
import { AdminStatisticsComponent } from './statistics/statistics.component';
import { CodeEditorComponent } from './code-editor/code-editor';

@NgModule({
    declarations: [
        AdminConfigComponent,
        AdminFrontpageComponent,
        AdminMediacenterComponent,
        AdminPageComponent,
        AdminPluginsComponent,
        AdminStatisticsComponent,
        AutocompleteComponent,
        CodeEditorComponent,
        LtiAdminComponent,
        LtitoolAdminComponent,
        LuceneTemplateMemoryComponent,
    ],
    imports: [
        SharedModule,
        AdminPageRoutingModule,
        MdsModule,
        MonacoEditorModule.forRoot({ baseUrl: './assets' }),
    ],
})
export class AdminPageModule {}
