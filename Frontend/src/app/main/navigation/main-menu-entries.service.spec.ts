import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthenticationService } from 'ngx-edu-sharing-api';
import { Observable, of } from 'rxjs';
import { BridgeService } from '../../services/bridge.service';
import {
    ConfigurationService,
    FrameEventsService,
    RestConnectorService,
    RestConstants,
    RestMediacenterService,
    RestOrganizationService,
    UIService,
} from '../../core-module/core.module';
import { MainMenuEntriesService } from './main-menu-entries.service';

// FIXME: some tests fail with a timeout
describe('MainMenuEntriesService', () => {
    let service: MainMenuEntriesService;
    let authenticationStub: AuthenticationService;
    let configurationStub: { getAll: () => Observable<any> };
    let restConnectorStub: {
        isLoggedIn: () => Observable<{
            toolPermissions?: string[];
            isValidLogin?: boolean;
            isGuest?: boolean;
            isAdmin?: boolean;
        }>;
        hasAccessToScope: () => Observable<any>;
    };
    let restMediacentersStub: {
        getMediacenters: () => Observable<any>;
    };
    let restOrganizationStub: {
        getOrganizations: () => Observable<{ organizations: any[] }>;
    };
    let uiStub: {
        isMobile: () => boolean;
    };

    beforeEach(() => {
        authenticationStub = {
            observeHasAccessToScope: (scope: string) => of(true),
            observeLoginInfo: () => of({}),
        } as unknown as AuthenticationService;
        configurationStub = {
            getAll: () => of({}),
        };
        restConnectorStub = {
            isLoggedIn: () => of({}),
            hasAccessToScope: () => of({}),
        };
        restMediacentersStub = {
            getMediacenters: () => of([]),
        };
        restOrganizationStub = {
            getOrganizations: () => of({ organizations: [] }),
        };
        uiStub = {
            isMobile: () => false,
        };
    });

    function setUp() {
        TestBed.configureTestingModule({
            providers: [
                MainMenuEntriesService,
                {
                    provide: AuthenticationService,
                    useValue: authenticationStub,
                },
                {
                    provide: BridgeService,
                    useValue: {},
                },
                {
                    provide: ConfigurationService,
                    useValue: configurationStub,
                },
                {
                    provide: FrameEventsService,
                    useValue: {},
                },
                {
                    provide: RestConnectorService,
                    useValue: restConnectorStub,
                },
                {
                    provide: RestMediacenterService,
                    useValue: restMediacentersStub,
                },
                {
                    provide: RestOrganizationService,
                    useValue: restOrganizationStub,
                },
                {
                    provide: ActivatedRoute,
                    useValue: {},
                },
                {
                    provide: Router,
                    useValue: {},
                },
                {
                    provide: UIService,
                    useValue: uiStub,
                },
            ],
        });
        service = TestBed.inject(MainMenuEntriesService);
    }

    it('should be created', () => {
        setUp();
        void expect(service).toBeTruthy();
    });

    it('should provide an empty array when not logged in', async () => {
        setUp();
        const entries = await service.entries$.toPromise();
        void expect(entries).toEqual([]);
    });

    xit('should provide entries', async () => {
        restConnectorStub.isLoggedIn = () =>
            of({
                isValidLogin: true,
                toolPermissions: [RestConstants.TOOLPERMISSION_WORKSPACE],
            });
        setUp();
        const entries = await service.entries$.toPromise();
        void expect(entries).toEqual([
            jasmine.objectContaining({
                name: 'SIDEBAR.WORKSPACE',
                icon: 'cloud',
                scope: 'workspace',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
            jasmine.objectContaining({
                name: 'SIDEBAR.SEARCH',
                icon: 'search',
                scope: 'search',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
            jasmine.objectContaining({
                name: 'SIDEBAR.COLLECTIONS',
                icon: 'layers',
                scope: 'collections',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
        ]);
    });

    xit('should provide guest entries', async () => {
        restConnectorStub.isLoggedIn = () =>
            of({
                isValidLogin: true,
                isGuest: true,
                toolPermissions: [],
            });
        setUp();
        const entries = await service.entries$.toPromise();
        void expect(entries).toEqual([
            jasmine.objectContaining({
                name: 'SIDEBAR.SEARCH',
                icon: 'search',
                scope: 'search',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
            jasmine.objectContaining({
                name: 'SIDEBAR.COLLECTIONS',
                icon: 'layers',
                scope: 'collections',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
            jasmine.objectContaining({
                name: 'SIDEBAR.LOGIN',
                icon: 'person',
                scope: 'login',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
        ]);
    });

    xit('should provide mobile entries', async () => {
        restConnectorStub.isLoggedIn = () =>
            of({
                isValidLogin: true,
                toolPermissions: [],
            });
        uiStub.isMobile = () => true;
        setUp();
        const entries = await service.entries$.toPromise();
        void expect(entries).toEqual([
            jasmine.objectContaining({
                name: 'SIDEBAR.SEARCH',
                icon: 'search',
                scope: 'search',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
            jasmine.objectContaining({
                name: 'SIDEBAR.COLLECTIONS',
                icon: 'layers',
                scope: 'collections',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
        ]);
    });

    xit('should provide admin entries', async () => {
        restConnectorStub.isLoggedIn = () =>
            of({
                isValidLogin: true,
                isAdmin: true,
                toolPermissions: [],
            });
        setUp();
        const entries = await service.entries$.toPromise();
        void expect(entries).toEqual([
            jasmine.objectContaining({
                name: 'SIDEBAR.SEARCH',
                icon: 'search',
                scope: 'search',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
            jasmine.objectContaining({
                name: 'SIDEBAR.COLLECTIONS',
                icon: 'layers',
                scope: 'collections',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
            jasmine.objectContaining({
                name: 'SIDEBAR.ADMIN',
                icon: 'settings',
                scope: 'admin',
                isDisabled: false,
                isSeparate: false,
                isCustom: false,
            }),
        ]);
    });

    it('should hide entries', async () => {
        configurationStub.getAll = () =>
            of({
                hideMainMenu: ['workspace'],
            });
        restConnectorStub.isLoggedIn = () =>
            of({
                toolPermissions: [RestConstants.TOOLPERMISSION_WORKSPACE],
            });
        setUp();
        const entries = await service.entries$.toPromise();
        void expect(entries).toEqual([]);
    });

    it('should insert custom entries', async () => {
        configurationStub.getAll = () =>
            of({
                menuEntries: [
                    {
                        name: 'test name',
                        icon: 'test icon',
                        scope: 'test scope',
                        url: 'http://test.test',
                        position: 0,
                    },
                ],
            });
        setUp();
        const entries = await service.entries$.toPromise();
        void expect(entries.length).toBe(1);
        void expect(entries[0]).toEqual(
            jasmine.objectContaining({
                name: 'test name',
                icon: 'test icon',
                scope: 'test scope',
                isDisabled: false,
                isSeparate: false,
                isCustom: true,
            }),
        );
    });
});
