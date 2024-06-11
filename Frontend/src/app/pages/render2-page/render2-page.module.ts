import { NgModule } from '@angular/core';
import { Render2PageRoutingModule } from './render2-page-routing.module';
import { Render2PageComponent } from './render2-page.component';
import { RenderingModule, RenderComponent } from 'ngx-rendering-service';
import { RenderingServiceApiModule } from 'ngx-rendering-service-api';
import { environment } from '../../../environments/environment';

/**
 * new module for (kotlin based) rendering backend
 */
@NgModule({
    declarations: [Render2PageComponent],
    imports: [
        Render2PageRoutingModule,
        RenderingModule,
        RenderComponent,
        RenderingServiceApiModule.forRoot({
            //rootUrl: environment.rsUrl
        }),
    ],
})
export class Render2PageModule {}
