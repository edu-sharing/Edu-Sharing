/**
 * Created by Torsten on 13.01.2017.
 */

import { Component, OnInit, ViewChild } from '@angular/core';
import {
    About,
    AboutService,
    AuthenticationService,
    ME,
    Notification,
    NotificationV1Service,
} from 'ngx-edu-sharing-api';
import { RestConstants } from '../../../../../core-module/rest/rest-constants';
import { DialogsService } from '../../../../../features/dialogs/dialogs.service';
import { MatMenuTrigger } from '@angular/material/menu';
import { NodeDataSource, TranslationsService } from 'ngx-edu-sharing-ui';

@Component({
    selector: 'es-notification-list',
    templateUrl: 'notification-list.component.html',
    styleUrls: ['notification-list.component.scss'],
})
export class NotificationListComponent implements OnInit {
    static readonly NOTIFICATION_REFRESH_INTERVAL = 30 * 1000;
    static readonly NOTIFICATION_COUNT_ALL = 100;
    static readonly NOTIFICATION_COUNT_UNREAD = 25;
    @ViewChild('menuTrigger') menuTrigger: MatMenuTrigger;
    dataSource = new NodeDataSource<any>();
    unreadNotificationsCount: number;
    show = false;
    private about: About;
    viewRead = false;
    constructor(
        private aboutService: AboutService,
        private authenticationService: AuthenticationService,
        private translations: TranslationsService,
        private notificationService: NotificationV1Service,
        private dialogs: DialogsService,
    ) {}

    async ngOnInit() {
        this.dataSource.isLoading = true;
        await this.translations.waitForInit().toPromise();
        this.about = await this.aboutService.getAbout().toPromise();
        this.authenticationService.observeLoginInfo().subscribe((login) => {
            this.show =
                login.statusCode === RestConstants.STATUS_CODE_OK &&
                this.about.plugins?.filter((s) => s.id === 'kafka-notification-plugin').length > 0;
            if (this.show) {
                void this.loadNotifications();
            }
        });

        // auto refresh
        // this.ngZone.runOutsideAngular(() => {
        setInterval(() => {
            if (this.show && !this.menuTrigger?.menuOpen) {
                void this.loadNotifications();
            }
        }, NotificationListComponent.NOTIFICATION_REFRESH_INTERVAL);
        // })
    }

    async openSettings() {
        await this.dialogs.openNotificationDialog();
    }

    async setViewRead(viewRead: boolean) {
        this.viewRead = viewRead;
        await this.loadNotifications();
    }

    async markAllRead() {
        this.dataSource.getData().forEach((e) => (e.status = 'READ'));
        this.unreadNotificationsCount = 0;
        await this.notificationService
            .updateNotificationStatusByReceiverId({
                receiverId: ME,
            })
            .toPromise();
    }

    async loadNotifications() {
        let status: Array<'PENDING' | 'SENT' | 'READ'> = ['PENDING', 'SENT'];
        this.dataSource.reset();
        this.dataSource.isLoading = true;
        const notifications = await this.notificationService
            .getNotifications({
                receiverId: ME,
                status,
                sort: ['timestamp,desc'],
                page: 0,
                size: this.viewRead
                    ? NotificationListComponent.NOTIFICATION_COUNT_ALL
                    : NotificationListComponent.NOTIFICATION_COUNT_UNREAD,
            })
            .toPromise();
        this.unreadNotificationsCount = notifications.totalElements;

        if (this.viewRead) {
            // fetch all status
            status.push('READ');
            const notifications = await this.notificationService
                .getNotifications({
                    receiverId: ME,
                    status,
                    sort: ['timestamp,desc'],
                    page: 0,
                    size: this.viewRead
                        ? NotificationListComponent.NOTIFICATION_COUNT_ALL
                        : NotificationListComponent.NOTIFICATION_COUNT_UNREAD,
                })
                .toPromise();
            this.dataSource.setData(notifications.content, {
                from: 0,
                count: notifications.numberOfElements,
                total: notifications.totalElements,
            });
        } else {
            this.dataSource.setData(notifications.content, {
                from: 0,
                count: notifications.numberOfElements,
                total: notifications.totalElements,
            });
        }
        this.dataSource.isLoading = false;
        if (!this.viewRead) {
            // update the "bell" count of unread notifications if only unread where fetched
            this.unreadNotificationsCount = this.dataSource.getTotal();
        }
    }

    async updateStatus(entry: Notification, status: 'PENDING' | 'SENT' | 'READ') {
        await this.notificationService
            .updateNotificationStatus({
                id: entry._id,
                status,
            })
            .toPromise();
        entry.status = status;
        if (status === 'READ') {
            // client side update of unread notifications
            this.unreadNotificationsCount--;
        }
    }

    allowMarkRead() {
        return this.unreadNotificationsCount > 0;
    }
}
