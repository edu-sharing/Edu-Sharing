import { NgModule } from '@angular/core';
import { Render2PageRoutingModule } from './render2-page-routing.module';
import { Render2PageComponent } from './render2-page.component';
import { RenderWrapperModule } from './render-wrapper-component/render-wrapper.module';
import { CommonModule } from '@angular/common';

/**
 * new module for (kotlin based) rendering backend
 */
@NgModule({
    declarations: [Render2PageComponent],
    imports: [CommonModule, Render2PageRoutingModule, RenderWrapperModule],
})
export class Render2PageModule {}
