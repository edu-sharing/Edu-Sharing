import { NgModule } from '@angular/core';
import { RenderComponent, RenderingModule } from 'ngx-rendering-service';
import { RenderingServiceApiModule } from 'ngx-rendering-service-api';
import { RenderWrapperComponent } from './render-wrapper.component';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';
import { MdsModule } from '../../../features/mds/mds.module';
import { EduSharingUiModule } from 'ngx-edu-sharing-ui';
import { MatButtonModule } from '@angular/material/button';

/**
 * new module for (kotlin based) rendering backend
 */
@NgModule({
    declarations: [RenderWrapperComponent],
    imports: [
        CommonModule,
        EduSharingUiModule,
        MatButtonModule,
        RenderingModule,
        RenderComponent,
        RenderingServiceApiModule.forRoot({
            //rootUrl: environment.rsUrl
        }),
        MdsModule,
    ],
    exports: [RenderWrapperComponent],
})
export class RenderWrapperModule {}
