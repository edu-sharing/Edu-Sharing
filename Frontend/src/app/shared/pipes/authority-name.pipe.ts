import { Pipe, PipeTransform } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Group, Permission, RestConstants, User } from '../../core-module/core.module';
import { VCard } from 'ngx-edu-sharing-ui';

@Pipe({ name: 'authorityName' })
export class AuthorityNamePipe implements PipeTransform {
    constructor(private translate: TranslateService) {}

    transform(
        authority: Permission | User | Group | any,
        args = { avatarShortcut: false },
    ): string {
        if (!authority) return 'invalid';
        if (authority.profile?.displayName) {
            if (args?.avatarShortcut) {
                return this.getFirstChar(authority.profile.displayName);
            }
            return authority.profile.displayName;
        }
        if (authority.profile?.firstName || authority.profile?.lastName) {
            if (args?.avatarShortcut) {
                return (
                    this.getFirstChar(authority.profile.firstName) +
                    this.getFirstChar(authority.profile.lastName)
                );
            }
            let title = '';
            try {
                let vcard;
                if (authority.profile?.vcard instanceof VCard) {
                    vcard = authority.profile?.vcard;
                } else {
                    vcard = new VCard(authority.profile?.vcard);
                }
                title = vcard.title || '';
            } catch (ignored) {}
            return (
                title +
                ' ' +
                authority.profile.firstName +
                ' ' +
                authority.profile.lastName
            ).trim();
        }
        if (authority.user?.firstName || authority.user?.lastName) {
            if (args?.avatarShortcut) {
                return (
                    this.getFirstChar(authority.user.firstName) +
                    this.getFirstChar(authority.user.lastName)
                );
            }
            return (authority.user.firstName + ' ' + authority.user.lastName).trim();
        }
        if (authority.user?.givenName || authority.user?.surname) {
            if (args?.avatarShortcut) {
                return (
                    this.getFirstChar(authority.user.givenName) +
                    this.getFirstChar(authority.user.surname)
                );
            }
            return (authority.user.givenName + ' ' + authority.user.surname).trim();
        }
        if (authority.group?.displayName) {
            if (args?.avatarShortcut) {
                return this.getFirstChar(authority.group.displayName);
            }
            return authority.group.displayName;
        }
        if (authority.displayName) {
            if (args?.avatarShortcut) {
                return this.getFirstChar(authority.displayName);
            }
            return authority.displayName;
        }
        if (authority.firstName || authority.lastName) {
            if (args?.avatarShortcut) {
                return (
                    this.getFirstChar(authority.firstName) + this.getFirstChar(authority.lastName)
                );
            }
            return authority.firstName + ' ' + authority.lastName;
        }
        if (args?.avatarShortcut) {
            return '-';
        }
        // for Permissions, the authority is encapsulated
        if (authority.authority) {
            authority = authority.authority;
        }
        if (authority.authorityType === RestConstants.AUTHORITY_TYPE_EVERYONE) {
            return this.translate.instant('GROUP_EVERYONE');
        }
        if (authority.authorityName) {
            if (authority.authorityName?.startsWith(RestConstants.AUTHORITY_DELETED_USER)) {
                // we could also add the date of the deletion (DELETED_USER_<timestamp>), but do we want that?
                return this.translate.instant('DELETED_USER');
            }
            return authority.authorityName;
        }
        return 'invalid';
    }
    getFirstChar(str: string) {
        str = str.toUpperCase();
        for (let i = 0; i < str.length; i++) {
            if (str.charAt(i).match(/[a-zäöü]/i)) {
                return str.charAt(i);
            }
        }
        return ' ';
    }
}
