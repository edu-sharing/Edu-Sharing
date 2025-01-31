import { trigger } from '@angular/animations';
import {
    ChangeDetectorRef,
    Component,
    ElementRef,
    HostListener,
    NgZone,
    ViewChild,
} from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { UIAnimation } from 'ngx-edu-sharing-ui';
import { first } from 'rxjs/operators';
import { ConfigurationService, RestConnectorService } from '../../core-module/core.module';
import {
    EventListener,
    FrameEventsService,
} from '../../core-module/rest/services/frame-events.service';
import { LoadingScreenService } from '../../main/loading-screen/loading-screen.service';
import { RocketChatService } from './rocket-chat.service';

@Component({
    selector: 'es-rocketchat',
    templateUrl: 'rocketchat.component.html',
    styleUrls: ['rocketchat.component.scss'],
    animations: [trigger('toggle', UIAnimation.openOverlayBottom())],
})
/**
 * An edu-sharing file-picker modal dialog
 */
export class RocketchatComponent implements EventListener {
    onEvent(event: string, data: any): void {
        if (
            event == FrameEventsService.EVENT_USER_LOGGED_IN ||
            event == FrameEventsService.EVENT_USER_LOGGED_OUT
        )
            void this.initalize();
    }
    @ViewChild('frame') frame: ElementRef;
    @HostListener('document:keydown', ['$event'])
    handleKeyboardEvent(event: KeyboardEvent) {
        if (event.code == 'Escape' && this.fullscreen) {
            event.preventDefault();
            event.stopPropagation();
            this.fullscreen = false;
            return;
        }
    }
    src: SafeResourceUrl;
    fullscreen = false;
    loaded = false;
    constructor(
        private sanitizer: DomSanitizer,
        private connector: RestConnectorService,
        private configuration: ConfigurationService,
        private ngZone: NgZone,
        private changes: ChangeDetectorRef,
        private events: FrameEventsService,
        private loadingScreen: LoadingScreenService,
        public rocketChat: RocketChatService,
    ) {
        this.events.addSelfListener(this);
        void this.initalize();
        this.ngZone.runOutsideAngular(() => {
            window.addEventListener(
                'message',
                (event: any) => {
                    if (event.source !== window.self) {
                        if (event.data.eventName === 'startup') {
                            this.loaded = true;
                            this.changes.detectChanges();
                            this.frame.nativeElement.contentWindow.postMessage(
                                {
                                    externalCommand: 'login-with-token',
                                    token: this.rocketChat._data.token,
                                },
                                this.rocketChat._data.url,
                            );
                        } else if (
                            event.data.eventName === 'new-message' &&
                            !this.rocketChat.opened
                        ) {
                            this.rocketChat.unread++;
                            this.changes.detectChanges();
                        }
                    }
                },
                false,
            );
        });
    }
    getFrameUrl() {
        //return this.sanitizer.bypassSecurityTrustResourceUrl(this._data.url+'/channel/general?layout=embedded');
        return this.sanitizer.bypassSecurityTrustResourceUrl(
            this.rocketChat._data.url + '/channel/general',
        );
    }

    private async initalize() {
        this.rocketChat._data = null;
        this.src = null;
        this.rocketChat.opened = await this.configuration
            .get('remote.rocketchat.shouldOpen', false)
            .toPromise();
        this.fullscreen = false;
        if (this.loadingScreen.getIsLoading()) {
            this.loadingScreen
                .observeIsLoading()
                .pipe(first((isLoading) => !isLoading))
                .subscribe(() => this.initalize());
            return;
        }
        const login = this.connector.getCurrentLogin();
        if (login?.remoteAuthentications?.ROCKETCHAT) {
            this.rocketChat._data = login.remoteAuthentications.ROCKETCHAT;
            this.src = this.getFrameUrl();
        }
    }
}
