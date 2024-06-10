import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { WorkspaceExplorerComponent } from './explorer/explorer.component';
import { WorkspaceMetadataBlockComponent } from './metadata/metadata-block.component';
import { MetadataSidebarComponent } from './metadata/metadata-sidebar.component';
import { WorkspaceMetadataComponent } from './metadata/metadata.component';
import { RecycleMainComponent } from './recycle/recycle.component';
import { WorkspaceSubTreeComponent } from './sub-tree/sub-tree.component';
import { WorkspaceTreeComponent } from './tree/tree.component';
import { WorkspacePageRoutingModule } from './workspace-page-routing.module';
import { WorkspacePageComponent } from './workspace-page.component';

@NgModule({
    declarations: [
        MetadataSidebarComponent,
        RecycleMainComponent,
        WorkspaceExplorerComponent,
        WorkspaceMetadataBlockComponent,
        WorkspaceMetadataComponent,
        WorkspacePageComponent,
        WorkspaceSubTreeComponent,
        WorkspaceTreeComponent,
    ],
    imports: [SharedModule, WorkspacePageRoutingModule],
    exports: [WorkspaceMetadataBlockComponent],
})
export class WorkspacePageModule {}
