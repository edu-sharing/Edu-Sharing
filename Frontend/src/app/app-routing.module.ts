import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UIConstants } from './core-module/core.module';
import { extensionRoutes } from './extension/extension-routes';
import { DialogsNavigationGuard } from './features/dialogs/dialogs-navigation.guard';
import { StartupComponent } from './main/startup.component';

const routes: Routes = [
    // Overrides and additional routes
    ...extensionRoutes,

    // Entrypoint
    { path: '', component: StartupComponent },

    // App login
    {
        path: UIConstants.ROUTER_PREFIX + 'app',
        loadChildren: () =>
            import('./pages/app-login-page/app-login-page.module').then(
                (m) => m.AppLoginPageModule,
            ),
    },

    // App share
    {
        path: UIConstants.ROUTER_PREFIX + 'app/share',
        loadChildren: () =>
            import('./pages/app-share-page/app-share-page.module').then(
                (m) => m.AppSharePageModule,
            ),
    },

    // Render
    {
        path: UIConstants.ROUTER_PREFIX + 'render',
        loadChildren: () =>
            import('./pages/render2-page/render2-page.module').then((m) => m.Render2PageModule),
    },

    // Apply to LMS
    {
        path: UIConstants.ROUTER_PREFIX + 'apply-to-lms',
        loadChildren: () =>
            import('./pages/apply-to-lms-page/apply-to-lms-page.module').then(
                (m) => m.ApplyToLmsPageModule,
            ),
    },

    // Search
    {
        path: UIConstants.ROUTER_PREFIX + 'search',
        loadChildren: () =>
            import('./pages/search-page/search-page.module').then((m) => m.SearchPageModule),
    },

    // Workspace
    {
        path: UIConstants.ROUTER_PREFIX + 'workspace',
        loadChildren: () =>
            import('./pages/workspace-page/workspace-page.module').then(
                (m) => m.WorkspacePageModule,
            ),
    },

    // Collections
    {
        path: UIConstants.ROUTER_PREFIX + 'collections',
        loadChildren: () =>
            import('./pages/collections-page/collections-page.module').then(
                (m) => m.CollectionsPageModule,
            ),
    },

    // Login
    {
        path: UIConstants.ROUTER_PREFIX + 'login',
        loadChildren: () =>
            import('./pages/login-page/login-page.module').then((m) => m.LoginPageModule),
    },

    // Register
    {
        path: UIConstants.ROUTER_PREFIX + 'register',
        loadChildren: () =>
            import('./pages/register-page/register-page.module').then((m) => m.RegisterPageModule),
    },

    // Upload
    {
        path: UIConstants.ROUTER_PREFIX + 'upload',
        loadChildren: () =>
            import('./pages/upload-page/upload-page.module').then((m) => m.UploadPageModule),
    },

    // Admin
    {
        path: UIConstants.ROUTER_PREFIX + 'admin',
        loadChildren: () =>
            import('./pages/admin-page/admin-page.module').then((m) => m.AdminPageModule),
    },

    // User management
    {
        path: UIConstants.ROUTER_PREFIX + 'permissions',
        loadChildren: () =>
            import('./pages/user-management-page/user-management-page.module').then(
                (m) => m.UserManagementPageModule,
            ),
    },

    // OER
    {
        path: UIConstants.ROUTER_PREFIX + 'oer',
        loadChildren: () => import('./pages/oer-page/oer-page.module').then((m) => m.OerPageModule),
    },

    // Stream
    {
        path: UIConstants.ROUTER_PREFIX + 'stream',
        loadChildren: () =>
            import('./pages/stream-page/stream-page.module').then((m) => m.StreamPageModule),
    },

    // Profile
    {
        path: UIConstants.ROUTER_PREFIX + 'profiles',
        loadChildren: () =>
            import('./pages/profile-page/profile-page.module').then((m) => m.ProfilePageModule),
    },

    // Link share
    {
        path: UIConstants.ROUTER_PREFIX + 'sharing',
        loadChildren: () =>
            import('./pages/sharing-page/sharing-page.module').then((m) => m.SharingPageModule),
    },

    // Embed
    {
        path: UIConstants.ROUTER_PREFIX + 'embed',
        loadChildren: () =>
            import('./pages/embed-page/embed-page.module').then((m) => m.EmbedPageModule),
    },

    // LTI
    {
        path: UIConstants.ROUTER_PREFIX + 'lti',
        loadChildren: () => import('./pages/lti-page/lti-page.module').then((m) => m.LtiPageModule),
    },

    // Error page / 404
    {
        path: '',
        loadChildren: () =>
            import('./pages/error-page/error-page.module').then((m) => m.ErrorPageModule),
    },
];

@NgModule({
    imports: [
        RouterModule.forRoot(
            [
                // Add a `canDeactivate` guard to all routes, that closes any open dialogs before
                // allowing navigation.
                {
                    path: '',
                    canDeactivate: [DialogsNavigationGuard],
                    children: routes,
                    runGuardsAndResolvers: 'paramsOrQueryParamsChange',
                },
            ],
            {
                // scrollPositionRestoration: 'enabled' emulated via
                // ScrollPositionRestorationService. This prevents the browser history from getting
                // messed up when navigation attempts are cancelled by guards.
                canceledNavigationResolution: 'computed',
            },
        ),
    ],
    exports: [RouterModule],
})
export class AppRoutingModule {}
