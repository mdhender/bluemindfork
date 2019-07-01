/*
 * BEGIN LICENSE
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

/** @fileoverview Route request to multiple handler */

goog.provide('net.bluemind.mvp.Router');
goog.provide('net.bluemind.mvp.Router.Route');

goog.require('goog.array');
goog.require('goog.events.EventHandler');
goog.require('goog.events.EventTarget');
goog.require("goog.log");
goog.require('goog.history.EventType');
goog.require('goog.History');
goog.require('goog.Uri');
goog.require('goog.dom');
goog.require('goog.Promise');
goog.require('goog.string');
goog.require('goog.structs.Map');
goog.require('net.bluemind.mvp.ApplicationContext');
goog.require('net.bluemind.mvp.Filter');
goog.require('net.bluemind.mvp.helper.URLHelper');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application process
 * @param {Array.<net.bluemind.mvp.Router.Route>} routes Application routes.
 * @extends {goog.events.EventTarget}
 * @constructor
 */
net.bluemind.mvp.Router = function(ctx, routes) {
  goog.base(this);
  this.ctx = ctx;

  this.filters_ = [];
  this.actives_ = {};

  this.routes_ = new goog.structs.Map();
  this.parseRoutes_(routes);
  this.handler_ = new goog.events.EventHandler(this);
};
goog.inherits(net.bluemind.mvp.Router, goog.events.EventTarget);

/**
 * @type {goog.events.EventHandler}
 * @private
 */
net.bluemind.mvp.Router.prototype.handler_;

/**
 * @type {goog.History}
 * @private
 */
net.bluemind.mvp.Router.prototype.history_;

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.mvp.Router.prototype.ctx;

/**
 * @type {Array.<net.bluemind.mvp.Filter>}
 */
net.bluemind.mvp.Router.prototype.filters_;

/**
 * @type {Object.<string, net.bluemind.mvp.Handler>}
 */
net.bluemind.mvp.Router.prototype.actives_;
/**
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.mvp.Router.prototype.routes_;

/**
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.mvp.Router.prototype.routes_;

/**
 * @private
 * @type {goog.debug.Logger}
 */
net.bluemind.mvp.Router.prototype.logger = goog.log.getLogger('net.bluemind.mvp.Router');

/**
 * Start navigation
 */
net.bluemind.mvp.Router.prototype.start = function() {
  var input = (
  /** @type {HTMLInputElement} */
  (goog.dom.createDom('input', {
    'name' : 'history',
    'id' : 'history',
    'style' : 'display:none'
  })) //
  );

  var iframe = (
  /** @type {HTMLIFrameElement} */
  (goog.dom.createDom('iframe', {
    'id' : 'history_iframe',
    'style' : 'display:none'
  }))//
  );
  this.history_ = new goog.History(false, undefined, input, iframe);
  this.handler_.listen(this.history_, goog.history.EventType.NAVIGATE, this.route_);
  this.history_.setEnabled(true);
  goog.log.info(this.logger, 'Application routing started');
};

/**
 * Add a new route (or enlarge an existing one).
 * 
 * @param {net.bluemind.mvp.Router.Route} route Route to add
 */
net.bluemind.mvp.Router.prototype.addRoute = function(route) {
  var path = route.path;
  if (!goog.string.startsWith(path, '^'))
    path = '^' + path;
  if (goog.string.endsWith(path, '/'))
    path = path + '.*$';
  else if (!goog.string.endsWith(path, '$'))
    path = path + '$';
  var key = new RegExp(path);
  var handlers = route.handlers;
  if (!this.routes_.containsKey(key)) {
    this.routes_.set(key, handlers);
  } else {
    var existing = this.routes_.get(key);
    goog.array.extend(existing, handlers);
    goog.array.removeDuplicates(existing);
  }
};

/**
 * Add a new route (or enlarge an existing one).
 * 
 * @param {net.bluemind.mvp.Router.Route} route Route to remove
 */
net.bluemind.mvp.Router.prototype.removeRoute = function(route) {
  var path = route.path;
  if (!goog.string.startsWith(path, '^'))
    path = '^' + path;
  if (goog.string.endsWith(path, '/'))
    path = path + '.*$';
  else if (!goog.string.endsWith(path, '$'))
    path = path + '$';
  var key = new RegExp(path);
  var handlers = route.handlers;
  if (this.routes_.containsKey(key)) {
    if (!handlers) {
      this.routes_.remove(key);
    } else {
      var existing = this.routes_.get(key);
    }
    var existing = this.routes_.get(key);
    goog.array.extend(existing, handlers);
    goog.array.removeDuplicates(existing);
  }
};

/**
 * Parse application routes.
 * 
 * @param {Array.<net.bluemind.mvp.Router.Route>} routes Application routes.
 * @private
 */
net.bluemind.mvp.Router.prototype.parseRoutes_ = function(routes) {
  for (var i = 0; i < routes.length; i++) {
    this.addRoute(routes[i]);
  }
};

/**
 * Add a new filter.
 * 
 * @param {net.bluemind.mvp.Filter} filter Filter to add
 */
net.bluemind.mvp.Router.prototype.addFilter = function(filter) {
  goog.array.binaryInsert(this.filters_, filter, net.bluemind.mvp.Filter.cmp);
};

/**
 * Determines the appropriate handlers based on the path, either creates a new
 * one or gets an existing one.
 * 
 * @param {goog.history.Event} e The navigation event.
 * @private
 */
net.bluemind.mvp.Router.prototype.route_ = function(e) {
  this.parseUri_();

  this.filter_().then(function() {
    this.parseUri_();
    return this.matchingHandlers_();
  }, null, this).then(function(handlers) {
    return this.navigate_(handlers);
  }, null, this).then(function(handlers) {
    return this.disposal_(handlers)
  }, null, this).then(function(handlers) {
    return this.handle_(handlers)
  }, function(e) {
    goog.log.error(this.logger, e.toString(), e);
  }, this)

};

/**
 * Filter execution
 * 
 * @return {goog.Promise} Filter result.
 * @private
 */
net.bluemind.mvp.Router.prototype.filter_ = function() {
  var promise = goog.Promise.resolve(this.ctx);
  goog.array.forEach(this.filters_, function(filter) {
    var fn = goog.partial(filter.filter, this.ctx);
    promise = promise.then(fn, null, filter);
  }, this);

  return promise;
};

/**
 * Call on Navigation on handler already active
 * 
 * @param {Array} handlers
 * @return {goog.Promise} Navigation result.
 * @private
 */
net.bluemind.mvp.Router.prototype.navigate_ = function(handlers) {
  var promise = goog.Promise.resolve(this.ctx);
  for ( var uid in this.actives_) {
    var h = this.actives_[uid];
    var exit = !goog.isDefAndNotNull(handlers[uid]);
    var fn = goog.partial(h.onNavigation, exit);
    promise = promise.then(fn, null, h);
  }
  return promise.then(function() {
    return handlers;
  }, function() {
    this.rollback_();
    throw 'RollbackError';
  }, this);
};

/**
 * Actives handler which do not handle new path are disposed
 * 
 * @param {Array} handlers
 * @return {goog.Promise} Disposal result.
 * @private
 */
net.bluemind.mvp.Router.prototype.disposal_ = function(handlers) {
  var promise = goog.Promise.resolve(this.ctx);

  for ( var uid in this.actives_) {
    if (!goog.isDefAndNotNull(handlers[uid])) {
      var h = this.actives_[uid];
      promise = promise.then(h.exit, null, h).thenAlways(h.dispose, h);
      delete this.actives_[uid];
    }

  }
  return promise.then(function() {
    return handlers;
  });
};

/**
 * Call setup on new handlers and handle on already active handler
 * 
 * @param {Array} handlers
 * @return {goog.Promise} Setup result
 * @private
 */
net.bluemind.mvp.Router.prototype.handle_ = function(handlers) {
  var promise = goog.Promise.resolve(this.ctx);
  for ( var uid in handlers) {
    if (!goog.isDefAndNotNull(this.actives_[uid])) {
      var h = new handlers[uid](this.ctx);
      this.actives_[uid] = h;
      promise = promise.then(h.setup, h.error, h);
    } else {
      var h = this.actives_[uid];
      promise = promise.then(h.handle, h.error, h);
    }
  }
  return promise.then(function() {
    return handlers;
  });
};

/**
 * Stop navigation process and go back to the current (previous) page. Typically
 * used for 'You have unsaved changes...' warnings.
 * 
 * @private
 */
net.bluemind.mvp.Router.prototype.rollback_ = function() {
  this.handler_.removeAll();
  this.handler_.listenOnce(this.history_, goog.history.EventType.NAVIGATE, function(e) {
    var uri = new goog.Uri(e.token);
    this.ctx.uri = uri;
    this.ctx.params = uri.getQueryData();
    this.handler_.listen(this.history_, goog.history.EventType.NAVIGATE, this.route_);
  });
  goog.global.history.back();
};

/**
 * Analyse URI to detect current action/module
 * 
 * @private
 */
net.bluemind.mvp.Router.prototype.parseUri_ = function() {
  var token = this.history_.getToken() || goog.global.location.hash.replace(/^#/, '');
  var uri = new goog.Uri(token);
  this.ctx.uri = uri;
  this.ctx.params = uri.getQueryData();
  if (uri.hasPath()) {
    var path = uri.getPath().split('/');
    path = goog.array.filter(path, function(part) {
      return (part != '');
    });
    this.ctx.module = path[0] || '';
    this.ctx.action = path[1] || '';
  } else {
    this.ctx.module = '';
    this.ctx.action = '';
  }
};

/**
 * Check Handlers that match curent uri.
 * 
 * @return {Object.<string, function(new: net.bluemind.mvp.Handler)>} Matching
 *         handlers
 * @private
 */
net.bluemind.mvp.Router.prototype.matchingHandlers_ = function() {
  var handlers = {};
  var path = this.ctx.uri.getPath();
  this.routes_.forEach(function(values, pattern) {
    if (pattern.test(path)) {
      for (var i = 0; i < values.length; i++) {
        handlers[goog.getUid(values[i])] = values[i];
      }
    }
  }, this);
  return handlers;
};

/**
 * Sets the history state. When user visible states are used, the URL fragment
 * will be set to the provided token.
 * 
 * @param {goog.Uri} uri The history state identifier.
 */
net.bluemind.mvp.Router.prototype.setURL = function(uri) {
  this.history_.setToken(uri.toString());
};

/**
 * Return current URI
 * 
 * @return {goog.Uri}
 */
net.bluemind.mvp.Router.prototype.getURL = function() {
  return new goog.Uri(this.history_.getToken());
};

/**
 * Modify current url. This will not trigger a navigation event. This is used by
 * filter to redirect without
 * 
 * @param {goog.Uri} uri Replacment uri.
 */
net.bluemind.mvp.Router.prototype.modifyURL = function(uri) {
  this.handler_.unlisten(this.history_, goog.history.EventType.NAVIGATE, this.route_);
  this.history_.replaceToken(uri.toString());
  this.handler_.listen(this.history_, goog.history.EventType.NAVIGATE, this.route_);
};

/**
 * Define a route
 * 
 * @typedef {{path: string, handlers: Array.<function(new:net.bluemind.mvp.Handler)>}}
 */
net.bluemind.mvp.Router.Route;