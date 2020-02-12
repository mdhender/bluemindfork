# Applications JS pour BlueMind

Actuellement l'environnement de développement nécessite d'utiliser un reverse proxy pour servir les fichiers des applications depuis son poste de développement.
Lors du déploiement et du packaging, nous utilisons [maven](https://maven.apache.org/), mais pour le développement d'applications ou modules existants, il n'est pas nécessaire d'en tenir compte.

Dans l'optique d'avoir un code le plus homogène possible, sont en place [eslint/prettier](https://prettier.io/docs/en/integrating-with-linters.html) et des configurations pour l'éditeur [vscode](https://code.visualstudio.com/). L'utilisation des [single file components](https://vuejs.org/v2/guide/single-file-components.html) de [vuejs](https://vuejs.org/v2/) impose également l'utilisation de loader webpack particulier.

## Configuration du reverse-proxy BlueMind

Le serveur web bluemind peut être complété par le [webdev filter](https://jenkins2.bluemind.net/view/Addons/job/addons/job/devmode/). Pour configurer ce dernier, créer un fichier `/etc/bm/dev.json` sur la vm qui fait tourner BlueMind:

```
{
  "servers": {
    "webpack-compile": {
      "ip": "dev.bluemind.test",
      "port": 9180
    },
    "webapp-server": {
      "ip": "dev.bluemind.test",
      "port": 9181
    },
  },
  "filters": [
    {
      "serverId": "webapp-server",
      "search": "/webapp/js/compile/net.bluemind.webapp.root.js",
      "replace": "/net.bluemind.webapp.root.js",
      "active": true
    },
    {
      "serverId": "webpack-compile",
      "search": "/webapp/js/compile/net.bluemind.webapp.mail.js",
      "replace": "/net.bluemind.webapp.mail.js",
      "active": true
    }
  ],
  "forwardPorts": [
    {
      "serverId": "webapp-server",
      "src": 9181,
      "active": true
    },
    {
      "serverId": "webpack-compile",
      "src": 9180,
      "active": true
    }
}
```

Il faut également modifier `/etc/hosts` pour ajouter une redirection de `dev.bluemind.test` vers l'IP de l'host qui fera tourner le contenu des apps.

-   `webapp-server` sert les fichiers communs et la bannière
-   `webpack-compile` sert les fichiers de l'application

## Développement avec transpilation automatique

Pour servir les fichiers de la bannière et de l'application, il faut démarrer deux `webpack-dev-server` disponible depuis les scripts npm :

-   `yarn root` ou `(cd root-webapp/net.bluemind.webapp.root.js && yarn dev)`
-   `yarn mail` ou `(cd mail-webapp/net.bluemind.webapp.mail.ui.js && yarn dev)`

## Configuration de l'environnement de développement

Il est recommandé d'utiliser vscode. Il est indispensable de suivre les guidelines et styleguides de développement (si une règle n'est pas définie ou n'est pas claire, il faudra l'expliciter et l'intégrer aux règles existantes).
