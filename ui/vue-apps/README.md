# Applications JS pour BlueMind

## Code Style Guide

See [Code Style Guide](STYLEGUIDE.md).

## Format and linter

Projects use [Prettier](https://prettier.io/) for code formatting concerns, while letting [Eslint](https://eslint.org/) focus on code-quality concerns.

## Editor

For developing BlueMind Vue applications, we strongly recommend using [Visual Studio Code](https://code.visualstudio.com/). As projects are using [single-file components](https://vuejs.org/v2/guide/single-file-components.html) (SFCs), get the awesome [Vetur extension](https://github.com/vuejs/vetur), which provides many great features.

## Build

[Webpack](https://webpack.js.org/) manages builds with some loaders:

-   [vue-loader](https://vue-loader.vuejs.org)
-   and others

## Run Webapps in development environment

Add [webdev filter](https://jenkins2.bluemind.net/view/Addons/job/addons/job/devmode/) in your bm-webserver installation.

Create `/etc/bm/dev.json`:

```json
{
    "servers": {
        "webpack-dev-server-root": {
            "ip": "dev.bluemind.test",
            "port": 9181
        },
        "webpack-dev-server-mail": {
            "ip": "dev.bluemind.test",
            "port": 9180
        }
    },
    "filters": [
        {
            "serverId": "webpack-dev-server-mail",
            "search": "/webapp/js/net.bluemind.webapp.mail.js",
            "replace": "/js/net.bluemind.webapp.mail.js",
            "active": true
        },
        {
            "serverId": "webpack-dev-server-mail",
            "search": "/webapp/service-worker.js",
            "replace": "/service-worker.js",
            "active": true
        },
        {
            "serverId": "webpack-dev-server-root",
            "search": "/webapp/js/net.bluemind.webapp.root.js",
            "replace": "/js/net.bluemind.webapp.root.js",
            "active": true
        }
    ],
    "forwardPorts": [
        {
            "serverId": "webpack-dev-server-root",
            "src": 9181,
            "active": true
        },
        {
            "serverId": "webpack-dev-server-mail",
            "src": 9180,
            "active": true
        }
    ]
}
```

Update `/etc/hosts`:

```
127.0.0.1 localhost
::1 localhost

<ip of webpack-dev-server> dev.bluemind.test
```

### Service Worker

Many service workers features are now enabled by default in newer versions of supporting browsers.
You’ll also need to serve your code via HTTPS — Service workers are restricted to running across HTTPS for security reasons.
In order to facilitate local development, localhost is considered a secure origin by browsers as well.

## Debugging in vscode

Configuration is available in [`.vscode/launch.json`](.vscode/launch.json).

Needed extensions:

-   Chrome/Chromium : https://marketplace.visualstudio.com/items?itemName=msjsdiag.debugger-for-chrome
-   Firefox : https://marketplace.visualstudio.com/items?itemName=firefox-devtools.vscode-firefox-debug

Use `BM_HOST` environment variable to adapt your own environment.
For instance, with bash `export BM_HOST="http://bm4.local"` or with fish `set -x BM_HOST http://bm4.local`.
