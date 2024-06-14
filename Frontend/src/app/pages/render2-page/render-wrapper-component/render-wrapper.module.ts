import { NgModule } from '@angular/core';
import { RenderComponent, RenderingModule } from 'ngx-rendering-service';
import { RenderingServiceApiModule } from 'ngx-rendering-service-api';
import { RenderWrapperComponent } from './render-wrapper.component';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

/**
 * new module for (kotlin based) rendering backend
 */
@NgModule({
    declarations: [RenderWrapperComponent],
    imports: [
        BrowserModule,
        CommonModule,
        RenderingModule,
        RenderComponent,
        RenderingServiceApiModule.forRoot({
            //rootUrl: environment.rsUrl
        }),
    ],
    exports: [RenderWrapperComponent],
})
export class RenderWrapperModule {}
