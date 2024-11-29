import { NgModule } from '@angular/core';
import { RenderComponent, RenderingServiceLibModule } from 'ngx-rendering-service-lib';
import { RenderWrapperComponent } from './render-wrapper.component';
import { CommonModule } from '@angular/common';
import { MdsModule } from '../../../features/mds/mds.module';
import { EduSharingUiModule, TranslationsModule } from 'ngx-edu-sharing-ui';
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
        RenderComponent,
        TranslationsModule,
        RenderingServiceLibModule,
        MdsModule,
    ],
    exports: [RenderWrapperComponent],
})
export class RenderWrapperModule {}
