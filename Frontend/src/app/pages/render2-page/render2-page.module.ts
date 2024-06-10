import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { Render2PageRoutingModule } from './render2-page-routing.module';
import { Render2PageComponent } from './render2-page.component';

/**
 * new module for (kotlin based) rendering backend
 */
@NgModule({
    declarations: [Render2PageComponent],
    imports: [SharedModule, Render2PageRoutingModule],
})
export class Render2PageModule {}
