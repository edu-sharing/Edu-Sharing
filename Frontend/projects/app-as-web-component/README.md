# Edu-Sharing as Web Component

## Build

```sh
# Serve a self-updating development build.
npm start
# Serve a one-time compiled production build.
npm run preview
# Build for production.
npm run build
```

## Configuration

Set the backend URL for edu-sharing with a global config object on `window`:

```js
window.__env = {
    EDU_SHARING_API_URL: 'http://repository.127.0.0.1.nip.io:8100/edu-sharing/rest',
};
```

For testing, you can proxy requests to the URL given in the file `.env` by setting `EDU_SHARING_API_URL` to `/edu-sharing/rest`.

## Installation

1. Add it in your package.json

```json
  "dependencies": {
    "ngx-edu-sharing-app-as-web-component": "^9.1.0"
  },
```

2. Copy the files into your assets/vendor part via `angular.json`

```json
 "assets": [
    {
      "glob": "**/*",
      "input": "node_modules/ngx-edu-sharing-app-as-web-component/",
      "output": "./vendor/edu-sharing"
    }
]
```

3. Make an html file that looks like this

```html
<html class="no-js" lang="">
    <base href="/" />
    <script>
        // Provide the backend URL for edu-sharing.
        //
        // For testing with development builds use '/edu-sharing/rest' for a proxy to the URL
        // configured the `.env` file.
        //
        window.__env = {
            //EDU_SHARING_API_URL: '/edu-sharing/rest',
            EDU_SHARING_API_URL: 'http://repository.127.0.0.1.nip.io:8100/edu-sharing/rest',
        };
    </script>
    <script src="vendor/edu-sharing/runtime.js" type="module"></script>
    <script src="vendor/edu-sharing/polyfills.js" type="module"></script>
    <!-- Alternatively to loading `scripts.js`, provide your own versions of jQuery.  -->
    <script src="vendor/edu-sharing/scripts.js" defer></script>
    <script src="vendor/edu-sharing/main.js" type="module"></script>
    <!-- Only for dev builds. Could also set `vendorChunk: false` in `angular.json` to omit
altogether. -->
    <script src="vendor/edu-sharing/vendor.js" type="module"></script>
    <link rel="stylesheet" href="vendor/edu-sharing/styles.css" />
    <body>
        <edu-sharing-app search-string="Test" style="height: 100vh"></edu-sharing-app>
    </body>
</html>
```
