export interface Env {
    EDU_SHARING_API_URL?: string;
}

export interface ExtendedWindow extends Window {
    __env?: Env;
}

declare var window: ExtendedWindow;

export const environment = {
    production: true,
    eduSharingApiUrl: window.__env.EDU_SHARING_API_URL,
};
