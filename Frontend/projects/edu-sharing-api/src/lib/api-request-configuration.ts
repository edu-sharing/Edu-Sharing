import { HttpRequest } from '@angular/common/http';
import { EventEmitter, Injectable } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { ApiConfiguration } from './api/api-configuration';

/**
 * Configuration for the performed HTTP requests
 */
@Injectable()
export class ApiRequestConfiguration {
    private authForNextRequest: string | null = null;
    private locale: string | null = null;

    /** Emits each time, an API request is performed. */
    readonly apiRequest = new EventEmitter<void>();

    constructor(private apiConfiguration: ApiConfiguration) {}

    setLocale(locale: string): void {
        this.locale = locale;
    }

    setBasicAuthForNextRequest(auth: { username: string; password: string }): void {
        this.authForNextRequest = 'Basic ' + btoa(auth.username + ':' + auth.password);
    }
    setBearerAuthForNextRequest(accessToken?: string): void {
        this.authForNextRequest = 'Bearer ' + accessToken;
    }

    setEduTicketAuthForNextRequest(ticket?: string): void {
        this.authForNextRequest = 'EDU-TICKET ' + ticket;
    }

    /**
     * Applies configuration to the given request.
     *
     * - Applies the current headers.
     * - Sets `credentials: 'include'`.
     *   This is needed for the application to send cookies to the backend when in an embedded
     *   context, e.g., as a web component in a third-party page.
     */
    apply(req: HttpRequest<any>): HttpRequest<any> {
        const headers: { [key: string]: string | string[] } = {};
        const isAPICall = req.url.startsWith(this.apiConfiguration.rootUrl);
        if (!isAPICall) {
            return req;
        }
        this.apiRequest.emit();
        headers['X-Client-Trace-Id'] = this.generateTraceId();
        if (this.locale) {
            headers.locale = this.locale;
        }
        if (this.authForNextRequest) {
            headers.Authorization = this.authForNextRequest;
            this.authForNextRequest = null;
        }
        return req.clone({
            setHeaders: headers,
            withCredentials: true,
        });
    }

    private generateTraceId(): string {
        return uuidv4();
    }
}
