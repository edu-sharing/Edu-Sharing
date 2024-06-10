import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Render2PageComponent } from './render2-page.component';

const routes: Routes = [
    {
        path: ':node',
        component: Render2PageComponent,
    },
    {
        path: ':node/:version',
        component: Render2PageComponent,
    },
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule],
})
export class Render2PageRoutingModule {}
