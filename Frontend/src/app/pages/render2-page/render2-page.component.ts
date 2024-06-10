import { trigger } from '@angular/animations';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { OptionsHelperDataService, UIAnimation } from 'ngx-edu-sharing-ui';
import { EventListener } from '../../core-module/core.module';

@Component({
    selector: 'es-render2-page',
    templateUrl: 'render2-page.component.html',
    styleUrls: ['render2-page.component.scss'],
    providers: [OptionsHelperDataService],
    animations: [trigger('fadeFast', UIAnimation.fade(UIAnimation.ANIMATION_TIME_FAST))],
})
export class Render2PageComponent {}
