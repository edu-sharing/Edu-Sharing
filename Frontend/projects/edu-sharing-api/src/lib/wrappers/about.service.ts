import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs/operators';
import { AboutService as AboutApiService } from '../api/services';
import { About } from '../models';
import { ApiConfiguration } from '../api/api-configuration';
import { HttpClient } from '@angular/common/http';

@Injectable({
    providedIn: 'root',
})
export class AboutService {
    private readonly about$;

    constructor(
        private about: AboutApiService,
        private config: ApiConfiguration,
        private http: HttpClient,
    ) {
        this.about$ = this.about.about().pipe(shareReplay(1));
    }

    getAbout(): Observable<About> {
        return this.about$;
    }

    /**
     * get the full openapi.json file as a json structure
     */
    getOpenapiJson(): Observable<any> {
        return this.http.get(this.config.rootUrl + '/openapi.json');
    }
}
