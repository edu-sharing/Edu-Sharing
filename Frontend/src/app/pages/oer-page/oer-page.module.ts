import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { OerPageRoutingModule } from './oer-page-routing.module';
import { OerPageComponent } from './oer-page.component';
import { FooterComponent } from '../../shared/components/footer/footer.component';

@NgModule({
    declarations: [OerPageComponent],
    imports: [SharedModule, OerPageRoutingModule, FooterComponent],
})
export class OerPageModule {}
