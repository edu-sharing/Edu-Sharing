import { Injectable } from '@angular/core';
import * as rxjs from 'rxjs';
import { Observable } from 'rxjs';
import { first, map, switchMap } from 'rxjs/operators';
import { NetworkV1Service } from '../api/services';
import { HOME_REPOSITORY } from '../constants';
import { Node } from '../models';
import { shareReplayReturnValue } from '../utils/decorators/share-replay-return-value';
import { AuthenticationService } from './authentication.service';
import { Repo } from '../api/models/repo';

type Repository = Repo;

interface NetworkRepositories {
    repositories: Repository[];
}

@Injectable({
    providedIn: 'root',
})
export class NetworkService {
    constructor(
        private networkV1: NetworkV1Service,
        private authentication: AuthenticationService,
    ) {}

    @shareReplayReturnValue()
    getRepositories(): Observable<Repository[]> {
        return this.authentication.observeLoginInfo().pipe(
            first((login) => login.isValidLogin),
            switchMap(() => this.networkV1.getRepositories()),
            map((repos) => repos.repositories),
        );
    }

    getRepository(id: string): Observable<Repository | null> {
        return this.getRepositories().pipe(
            map((repositories) => {
                if (id === HOME_REPOSITORY) {
                    return repositories.find((r) => r.isHomeRepo) ?? null;
                } else {
                    return repositories.find((r) => r.id === id) ?? null;
                }
            }),
        );
    }

    getHomeRepository(): Observable<Repository> {
        return this.getRepositories().pipe(
            map((repositories) => repositories.find((r) => r.isHomeRepo)!),
        );
    }

    isHomeRepository(id: string): Observable<boolean> {
        return this.getRepository(id).pipe(map((repository) => repository?.isHomeRepo ?? false));
    }

    isFromHomeRepository(node: Node): Observable<boolean> {
        if (node.ref.isHomeRepo) {
            return rxjs.of(true);
        } else {
            return this.isHomeRepository(node.ref.repo);
        }
    }

    getRepositoryOfNode(node: Node): Observable<Repository | null> {
        if (node.ref.isHomeRepo) {
            return this.getHomeRepository();
        } else {
            return this.getRepository(node.ref.repo);
        }
    }
}
