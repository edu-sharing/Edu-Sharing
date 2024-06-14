import { LocationStrategy } from '@angular/common';
import { MockLocationStrategy } from '@angular/common/testing';
import { ApplicationRef, DoBootstrap, Injector, NgModule } from '@angular/core';
import { createCustomElement } from '@angular/elements';
import { ASSETS_BASE_PATH } from 'ngx-edu-sharing-ui';
import { AppComponent } from './app.component';
import { RenderWrapperModule } from '../../../../src/app/pages/render2-page/render-wrapper-component/render-wrapper.module';
import { BrowserModule } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';

@NgModule({
    declarations: [AppComponent],
    imports: [BrowserModule, RenderWrapperModule],
    bootstrap: [AppComponent],
    providers: [
        provideHttpClient(),
        {
            provide: LocationStrategy,
            useClass: MockLocationStrategy,
        },
        { provide: ASSETS_BASE_PATH, useValue: 'vendor/edu-sharing/' },
    ],
})
// export class AppModule {}
export class AppModule implements DoBootstrap {
    constructor(injector: Injector) {
        const embeddedApp = createCustomElement(AppComponent, { injector });
        customElements.define('edu-sharing-render', embeddedApp);
    }

    // eslint-disable-next-line @angular-eslint/no-empty-lifecycle-method
    ngDoBootstrap(appRef: ApplicationRef): void {
        // Do nothing.
    }
}
