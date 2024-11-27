import { Component, signal } from '@angular/core';
import { OptionsHelperDataService } from 'ngx-edu-sharing-ui';
import { ActivatedRoute } from '@angular/router';
import { combineLatest } from 'rxjs';
import { MainNavService } from '../../main/navigation/main-nav.service';

@Component({
    selector: 'es-render2-page',
    templateUrl: 'render2-page.component.html',
    styleUrls: ['render2-page.component.scss'],
    providers: [OptionsHelperDataService],
})
export class Render2PageComponent {
    nodeId = signal<string>(null);
    constructor(private route: ActivatedRoute, private mainNav: MainNavService) {
        this.mainNav.setMainNavConfig({
            show: true,
            showNavigation: false,
            currentScope: 'render',
        });
        combineLatest([this.route.params, this.route.queryParams]).subscribe(
            ([params, queryParams]) => {
                this.nodeId.set(params.node);
            },
        );
    }
}
