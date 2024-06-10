import { NgModule } from '@angular/core';
import { SharedModule } from '../../shared/shared.module';
import { Render2PageRoutingModule } from './render2-page-routing.module';
import { Render2PageComponent } from './render2-page.component';
import { RenderingModule, RenderComponent } from 'ngx-rendering-service';

/**
 * new module for (kotlin based) rendering backend
 */
@NgModule({
    declarations: [Render2PageComponent],
    imports: [SharedModule, Render2PageRoutingModule, RenderingModule, RenderComponent],
})
export class Render2PageModule {}
