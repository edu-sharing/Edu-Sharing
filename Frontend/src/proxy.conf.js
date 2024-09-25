require('dotenv').config();

if (!process.env.BACKEND_URL) {
    throw new Error(
        'Missing environment variable `BACKEND_URL`.' +
            '\n\nTo get started, run' +
            '\n\n    cp .env.example .env' +
            '\n\nand edit `.env`.' +
            '\n',
    );
}

const PROXY_CONFIG = [
    {
        context: [
            '/edu-sharing/rest',
            '/edu-sharing/graphql',
            '/edu-sharing/eduservlet',
            '/edu-sharing/preview',
            '/edu-sharing/themes',
            '/edu-sharing/ccimages',
            '/edu-sharing/oauth2',
            '/edu-sharing/shibboleth',
        ],
        target: process.env.BACKEND_URL,
        secure: false,
        changeOrigin: true,
        onProxyRes: function (proxyRes, req, res) {
            proxyRes.headers['X-Edu-Sharing-Proxy-Target'] = process.env.BACKEND_URL;
            const cookies = proxyRes.headers['set-cookie'];
            if (cookies) {
                proxyRes.headers['set-cookie'] = cookies.map((cookie) =>
                    cookie
                        .replace('; Path=/edu-sharing', '; Path=/')
                        // We serve on a non-HTTPS connection, so 'Secure' cookies won't work.
                        .replace('; Secure', '')
                        // 'SameSite=None' is only allowed on 'Secure' cookies.
                        .replace('; SameSite=None', ''),
                );
            }
        },
    },
    {
        context: ['/rest', '/eduservlet', '/preview', '/themes'],
        target: process.env.BACKEND_URL + '/edu-sharing',
        secure: false,
        changeOrigin: true,
        onProxyRes: function (proxyRes, req, res) {
            proxyRes.headers['X-Edu-Sharing-Proxy-Target'] = process.env.BACKEND_URL;
            const cookies = proxyRes.headers['set-cookie'];
            if (cookies) {
                proxyRes.headers['set-cookie'] = cookies.map((cookie) =>
                    cookie
                        .replace('; Path=/edu-sharing', '; Path=/')
                        // We serve on a non-HTTPS connection, so 'Secure' cookies won't work.
                        .replace('; Secure', '')
                        // 'SameSite=None' is only allowed on 'Secure' cookies.
                        .replace('; SameSite=None', ''),
                );
            }
        },
    },
];

module.exports = PROXY_CONFIG;
