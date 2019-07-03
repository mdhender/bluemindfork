/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

/**
 * @fileoverview Application bootstrap.
 */

goog.provide("net.bluemind.mvp.Application");

goog.require("goog.array");
goog.require("goog.log");
goog.require("goog.debug.Console");
goog.require("goog.net.Cookies");
goog.require("goog.structs.Map");
goog.require('goog.Timer');
goog.require("net.bluemind.commons.ui.Loader");
goog.require("net.bluemind.authentication.schema");
goog.require("net.bluemind.authentication.service.AuthService");
goog.require("net.bluemind.authentication.api.AuthClient");
goog.require("net.bluemind.container.service.ContainersObserver");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("net.bluemind.date.DateHelper");
goog.require("net.bluemind.elasticsearch.ElasticSearchHelper");
goog.require("net.bluemind.events.CallToCTIHandler");
goog.require("net.bluemind.events.CallToDefaultHandler");
goog.require("net.bluemind.events.LinkHandler");
goog.require("net.bluemind.events.MailToDefaultHandler");
goog.require("net.bluemind.events.MailToWebmailHandler");
goog.require("net.bluemind.i18n.DateTimeHelper");
goog.require("net.bluemind.mvp.ApplicationContext");
goog.require("net.bluemind.mvp.Router");
goog.require("net.bluemind.mvp.helper.URLHelper");
goog.require("net.bluemind.tag.service.TagService");
goog.require("net.bluemind.timezone.TimeZoneHelper");
goog.require("relief.cache.Cache");
goog.require("relief.rpc.RPCService");
goog.require("bluemind.storage.StorageHelper");
goog.require("net.bluemind.container.persistance.DBItemHome");
goog.require("net.bluemind.persistance.DatabaseService");
goog.require("goog.net.Cookies");
goog.require('net.bluemind.net.OnlineHandler');
goog.require("net.bluemind.debug.RemoteLogger");


/**
 * Application
 * 
 * @param {string} application Application id
 * @param {string} base Application base url
 * @param {Array.<net.bluemind.mvp.Router.Route>} routes Application routes
 * @constructor
 */
net.bluemind.mvp.Application = function(application, base, routes) {

  var loader = new net.bluemind.commons.ui.Loader();
  loader.start();

  if (goog.DEBUG) {
    var debugConsole = new goog.debug.Console();
    debugConsole.setCapturing(true);
  }

  net.bluemind.debug.RemoteLogger.getInstance().setCapturing(true);
  var ctx = new net.bluemind.mvp.ApplicationContext();
  ctx.application = application;
  ctx.base = base;
  ctx.cookies = new goog.net.Cookies(document);
  if (!goog.global['bmcSessionInfos']) {
    goog.global['bmcSessionInfos'] = {
      'sid' : null,
      'bmVersion' : null,
      'userId' : null
    };
  }
  ctx.version = goog.global['applicationVersion'] || null;
  ctx.rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid'],
    'Accept' : 'application/json'
  }));

  ctx.privacy = (new goog.net.Cookies(document).get('BMPRIVACY')) != 'false';
  ctx.databaseAvailable = true;

  var router = new net.bluemind.mvp.Router(ctx, routes);
  var helper = new net.bluemind.mvp.helper.URLHelper(router);
  ctx.helper('url', helper);
  this.registerFilters(router);

  ctx.online = true;

  this.bootstrap(ctx).then(function() {
    loader.stop();
    router.start();
    this.postBootstrap(ctx);
  }, null, this);

};

/**
 * @protected
 * @type {goog.debug.Logger}
 */
net.bluemind.mvp.Application.prototype.logger = goog.log.getLogger('net.bluemind.mvp.Application');

/**
 * Application bootstrap
 * 
 * @suppress {missingProperties}
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 */
net.bluemind.mvp.Application.prototype.bootstrap = function(ctx) {
  this.registerClients(ctx);
  var online = net.bluemind.net.OnlineHandler.getInstance();
  goog.events.listen(online, ['online', 'offline'], function(state) {
    ctx.online = state.type == 'online';
  });
  ctx.online = online.isOnline();
  var waitForConnection = new goog.Promise(function(resolve) {
    if (!ctx.privacy && !ctx.online) {
      goog.log.warning(this.logger, "Public mode, we must wait to be online to proceed");
      goog.events.listenOnce(online, 'online', resolve);
    } else {
      resolve();
    }
  }, this);
  ctx.service('database', net.bluemind.persistance.DatabaseService);
  goog.log.info(this.logger, 'Initializing databases');

  return ctx.service('database').initialize().then(function() {
    return ctx.service('database').regsiterSchemas([ {
      name : 'context',
      schema : net.bluemind.authentication.schema,
      options : {
        mechanisms : [ 'database', 'webstorage' ]
      }
    } ]);
  }, null, this).then(function() {
    return ctx.service('database').regsiterSchemas(this.getDbSchemas(ctx));
  }, null, this).then(function() {
    return ctx.service('database').checkVersionAndUser();
  }, null, this).thenCatch(function(e) {
    goog.log.error(this.logger, 'No database, synchronous mode used', e);
  }, this).then(function() {
    goog.log.info(this.logger, 'Initializing authentication context');
    ctx.service('auth', net.bluemind.authentication.service.AuthService);
    return ctx.service('auth').loadContext(goog.global['bmcSessionInfos']['userId'], ctx);
  }, null, this).then(function() {
    this.registerServices(ctx);
    this.registerHelpers(ctx);
    this.registerHandlers(ctx);
  }, null, this).thenCatch(function(e) {
    // FIXME ...
    goog.log.error(this.logger, 'error during initialisation', e);
    ctx.notifyError("error during initiliasiation", e);
  }).then(function() {
    return waitForConnection;
  }, null, this);
};

/**
 * Actions after routing start
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @protected
 */
net.bluemind.mvp.Application.prototype.postBootstrap = function(ctx) {
};

/**
 * Register navigation filters.
 * 
 * @param {net.bluemind.mvp.Router} router Application router
 * @protected
 */
net.bluemind.mvp.Application.prototype.registerFilters = function(router) {
};

/**
 * Register clients in context
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @protected
 */
net.bluemind.mvp.Application.prototype.registerClients = function(ctx) {
  ctx.client('auth', net.bluemind.authentication.api.AuthClient);
  ctx.client('containers', net.bluemind.core.container.api.ContainersClient);
};

/**
 * Register service in context
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @protected
 */
net.bluemind.mvp.Application.prototype.registerServices = function(ctx) {
  ctx.service("containersObserver", net.bluemind.container.service.ContainersObserver);
};

/**
 * Register helpers in context
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @protected
 */
net.bluemind.mvp.Application.prototype.registerHelpers = function(ctx) {
  var helper = new net.bluemind.i18n.DateTimeHelper(ctx.settings.get('date'), ctx.settings.get('timeformat'));
  ctx.helper('dateformat', helper);
  helper = new net.bluemind.timezone.TimeZoneHelper(ctx.settings.get('timezone'));
  ctx.helper('timezone', helper);
  helper = new net.bluemind.date.DateHelper(helper);
  ctx.helper('date', helper);
  var esHelper = new net.bluemind.elasticsearch.ElasticSearchHelper();
  console.log('adding ES helper');
  ctx.helper('elasticsearch', esHelper);
};

/**
 * Register handlers in context
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @protected
 */
net.bluemind.mvp.Application.prototype.registerHandlers = function(ctx) {
  var links = new net.bluemind.events.LinkHandler(goog.global.document.body);
  if (goog.array.contains(ctx.user['roles'], 'hasMail')) {
    links.registerProtocolHandler('mailto', new net.bluemind.events.MailToWebmailHandler());
  } else {
    links.registerProtocolHandler('mailto', new net.bluemind.events.MailToDefaultHandler());
  }

  if (goog.array.contains(ctx.user['roles'], 'hasCTI')) {
    links.registerProtocolHandler('tel', new net.bluemind.events.CallToCTIHandler(ctx));
  } else {
    links.registerProtocolHandler('tel', new net.bluemind.events.CallToDefaultHandler());
  }
  ctx.handler('link', links);
};

/**
 * Register databases in context
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @protected
 */
net.bluemind.mvp.Application.prototype.getDbSchemas = function(ctx) {
  return [];
}

/**
 * @private
 * @type {boolean}
 */
net.bluemind.mvp.Application.reloadPending_ = false;
/**
 * @private
 * @type {boolean}
 */
net.bluemind.mvp.Application.lock_ = false;

/**
 * Naive lock mechanism for refresh prevention
 */
net.bluemind.mvp.Application.lock = function(enabled) {
  if (!enabled && net.bluemind.mvp.Application.reloadPending_) {
    goog.dom.getWindow().location.reload();
  }
  net.bluemind.mvp.Application.lock_ = enabled;
}

/**
 * Reload the all application (location.reload().
 * This method is mean to add a lock to prevent refresh during sensitive operation
 * 
 * @protected
 */
net.bluemind.mvp.Application.reload = function() {
  if (net.bluemind.mvp.Application.lock_) {
    net.bluemind.mvp.Application.reloadPending_ = true;
  } else {
    goog.dom.getWindow().location.reload();
  }
}
