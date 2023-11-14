import { NgModule } from '@angular/core';
import { MdsModule } from '../../features/mds/mds.module';
import { SharedModule } from '../../shared/shared.module';
import { CollectionContentComponent } from './collection-content/collection-content.component';
import { CollectionInfoBarComponent } from './collection-info-bar/collection-info-bar.component';
import { CollectionNewComponent } from './collection-new/collection-new.component';
import { CollectionsPageRoutingModule } from './collections-page-routing.module';
import { CollectionsPageComponent } from './collections-page.component';
import { InfobarComponent } from './infobar/infobar.component';

@NgModule({
    declarations: [
        CollectionContentComponent,
        CollectionInfoBarComponent,
        CollectionNewComponent,
        CollectionsPageComponent,
        InfobarComponent,
    ],
    imports: [SharedModule, MdsModule, CollectionsPageRoutingModule],
})
export class CollectionsPageModule {}
