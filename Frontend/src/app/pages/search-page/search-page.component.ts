import { animate, state, style, transition, trigger } from '@angular/animations';
import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, HostBinding, OnDestroy, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { ConfigService } from 'ngx-edu-sharing-api';
import { notNull, Scope, UIAnimation } from 'ngx-edu-sharing-ui';
import * as rxjs from 'rxjs';
import { combineLatest, Observable, Subject } from 'rxjs';
import { filter, map, switchMap, take, takeUntil, tap } from 'rxjs/operators';
import { Repository, RestConstants, UIConstants } from '../../core-module/core.module';
import { CardDialogRef } from '../../features/dialogs/card-dialog/card-dialog-ref';
import { DialogsService } from '../../features/dialogs/dialogs.service';
import { MainNavService } from '../../main/navigation/main-nav.service';
import { BreadcrumbsService } from '../../shared/components/breadcrumbs/breadcrumbs.service';
import { NavigationScheduler } from './navigation-scheduler';
import { SearchPageService } from './search-page.service';

@Component({
    selector: 'es-search-page',
    templateUrl: './search-page.component.html',
    styleUrls: ['./search-page.component.scss'],
    providers: [SearchPageService],
    animations: [
        trigger('fadeOut', [
            state('visible', style({ opacity: 1 })),
            state('hidden', style({ opacity: 0 })),
            transition('visible => hidden', [animate(UIAnimation.ANIMATION_TIME_NORMAL)]),
        ]),
    ],
})
export class SearchPageComponent implements OnInit, OnDestroy {
    readonly Scope = Scope;

    @ViewChild('filtersDialogResetButton', { static: true })
    filtersDialogResetButton: TemplateRef<HTMLElement>;
    @ViewChild('filtersDialogContent', { static: true }) filtersDialogContent: TemplateRef<unknown>;

    @HostBinding('class.has-tab-bar') tabBarIsVisible: boolean = null;
    progressBarIsVisible = false;
    queryParamsAllRepositories: { [key: string]: string };

    readonly availableRepositories = this.searchPage.availableRepositories;
    readonly activeRepository = this.searchPage.activeRepository;
    readonly showingAllRepositories = this.searchPage.showingAllRepositories;
    readonly filterBarIsVisible = this.searchPage.filterBarIsVisible;
    readonly searchString = this.searchPage.searchString;
    readonly searchFilters = this.searchPage.searchFilters;
    readonly loadingProgress = this.searchPage.loadingProgress;
    readonly isMobileScreen = this.getIsMobileScreen();
    private readonly destroyed = new Subject<void>();

    constructor(
        private breadcrumbsService: BreadcrumbsService,
        private breakpointObserver: BreakpointObserver,
        private dialogs: DialogsService,
        private mainNav: MainNavService,
        private navigationScheduler: NavigationScheduler,
        private route: ActivatedRoute,
        private searchPage: SearchPageService,
        private configService: ConfigService,
        private translate: TranslateService,
    ) {
        this.searchPage.init();
    }

    ngOnInit(): void {
        this.initMainNav();
        this.breadcrumbsService.setNodePath(null);
        this.availableRepositories
            .pipe(
                filter(notNull),
                map((availableRepositories) => availableRepositories.length > 1),
            )
            .subscribe((tabBarIsVisible) => (this.tabBarIsVisible = tabBarIsVisible));
        this.registerProgressBarIsVisible();
        this.registerFilterDialog();
        this.registerQueryParamsAllRepositories();
        this.registerConfigBehaviours();
    }

    registerConfigBehaviours() {
        combineLatest([
            this.configService.observeConfig({
                forceUpdate: false,
            }),
            this.isMobileScreen,
        ])
            .pipe(take(1))
            .subscribe(([config, isMobileScreen]) => {
                if (
                    config?.searchSidenavMode === 'always' ||
                    (config?.searchSidenavMode === 'auto' && !isMobileScreen)
                ) {
                    this.filterBarIsVisible.setSystemValue(true);
                }
            });
    }

    ngOnDestroy(): void {
        this.destroyed.next();
        this.destroyed.complete();
    }

    goToRepository(repository: Repository): void {
        this.activeRepository.setUserValue(repository.id);
        this.navigationScheduler.scheduleNavigation({
            route: [UIConstants.ROUTER_PREFIX, 'search'],
        });
    }

    private registerFilterDialog(): void {
        let dialogRefPromise: Promise<CardDialogRef<unknown>>;
        let isMobileScreen: boolean;
        rxjs.combineLatest([
            this.searchPage.filterBarIsVisible.observeValue().pipe(),
            this.isMobileScreen.pipe(tap((value) => (isMobileScreen = value))),
        ])
            .pipe(takeUntil(this.destroyed))
            .subscribe(async ([filterBarIsVisible]) => {
                if (isMobileScreen && filterBarIsVisible && !dialogRefPromise) {
                    dialogRefPromise = this.openFilterDialog();
                    const dialogRef = await dialogRefPromise;
                    dialogRef.afterClosed().subscribe(() => {
                        dialogRefPromise = null;
                        if (isMobileScreen) {
                            this.filterBarIsVisible.setUserValue(false);
                        }
                    });
                } else if (!isMobileScreen || !filterBarIsVisible) {
                    void dialogRefPromise?.then((dialogRef) => dialogRef.close());
                }
            });
    }

    private registerQueryParamsAllRepositories(): void {
        rxjs.combineLatest([this.route.queryParams, this.searchString.observeQueryParamEntry()])
            .pipe(
                takeUntil(this.destroyed),
                map(([queryParams, searchStringEntry]) => ({
                    addToCollection: queryParams.addToCollection,
                    ...searchStringEntry,
                })),
            )
            .subscribe((params) => (this.queryParamsAllRepositories = params));
    }

    private async openFilterDialog(): Promise<CardDialogRef<unknown>> {
        const dialogRef = await this.dialogs.openGenericDialog({
            title: 'SEARCH.FILTERS',
            contentTemplate: this.filtersDialogContent,
            minWidth: 350,
            customHeaderBarContent: this.filtersDialogResetButton,
        });
        this.searchPage.results.totalResults
            .pipe(
                switchMap((results) => this.translate.get('SEARCH.NUMBER_RESULTS', { results })),
                takeUntil(dialogRef.afterClosed()),
            )
            .subscribe((numberResults) => {
                dialogRef.patchConfig({ subtitle: numberResults.toString() });
            });
        return dialogRef;
    }

    private getIsMobileScreen() {
        return this.breakpointObserver
            .observe(['(max-width: 900px)'])
            .pipe(map(({ matches }) => matches));
    }

    private initMainNav(): void {
        this.mainNav.setMainNavConfig({
            title: 'SEARCH.TITLE',
            currentScope: 'search',
            canOpen: true,
            onCreate: (nodes) => this.searchPage.results.addNodes(nodes),
        });
        const activeRepositoryIsHome: Observable<boolean> = rxjs
            .combineLatest([this.availableRepositories, this.activeRepository.observeValue()])
            .pipe(
                filter(
                    ([availableRepositories, activeRepository]) =>
                        notNull(availableRepositories) && notNull(activeRepository),
                ),
                map(
                    ([availableRepositories, activeRepository]) =>
                        activeRepository === RestConstants.HOME_REPOSITORY ||
                        availableRepositories.find((r) => r.id === activeRepository).isHomeRepo,
                ),
            );
        activeRepositoryIsHome.subscribe((isHome) =>
            this.mainNav.patchMainNavConfig({
                create: { allowed: isHome, allowBinary: true },
            }),
        );
    }

    onProgressBarAnimationEnd(): void {
        if (this.searchPage.loadingProgress.value >= 100) {
            this.progressBarIsVisible = false;
        }
    }

    private registerProgressBarIsVisible(): void {
        this.searchPage.loadingProgress
            .pipe(filter((progress) => progress < 100))
            .subscribe(() => (this.progressBarIsVisible = true));
    }
}
