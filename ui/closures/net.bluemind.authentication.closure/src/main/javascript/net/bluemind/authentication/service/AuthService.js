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

goog.provide("net.bluemind.authentication.service.AuthService");

goog.require("goog.events.EventTarget");
goog.require("goog.structs.Map");
goog.require("net.bluemind.authentication.persistence.AuthHome");
/**
 * Authentication service
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.authentication.service.AuthService = function(ctx) {
  goog.events.EventTarget.call(this);
  this.client_ = (
  /** @type {net.bluemind.authentication.api.AuthClient} */
  (ctx.client('auth'))//
  );
  this.backend_ = new net.bluemind.authentication.persistence.AuthHome(ctx);
};
goog.inherits(net.bluemind.authentication.service.AuthService, goog.events.EventTarget);

/**
 * @type {net.bluemind.authentication.api.AuthClient}
 * @private
 */
net.bluemind.authentication.service.AuthService.prototype.client_;

/**
 * @type {net.bluemind.authentication.persistence.AuthHome}
 * @private
 */
net.bluemind.authentication.service.AuthService.prototype.backend_;

/**
 * Check if the current user is the stored user.
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.login = function(uid) {
  return this.backend_.get('global.user').then(function(user) {
    if (!user) {
      var error = new Error();
      error.name = 'NotFoundError';
      throw error;
    } else if (user['uid'] != uid && uid) {
      var error = new Error();
      error.name = 'InvalidParameterError';
      throw error;
    } else {
      return user;
    }
  }, null, this)
};

/**
 * Return current user
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.getCurrentUser = function() {
  return this.backend_.get('global.user');
};

/**
 * Return current user
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.storeUser = function(user) {
  return this.backend_.set('global.user', user);
};

/**
 * Return current user
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.storeSettings = function(settings) {
  return this.backend_.set('global.settings', settings);
};

/**
 * Return current user
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.getSettings = function() {
  return this.backend_.get('global.settings');
};

/**
 * Return a settings from it's namespace and property name
 * 
 * @param {string} namespace Property name or long name (namespace.property)
 * @param {string=} property Property namespace
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.get = function(namespace, property) {
  return this.backend_.get(namespace, property);
};

/**
 * Define a settings from it's namespace and property name
 * 
 * @param {string} property Property name or long name (namespace.property)
 * @param {*} value Property value.
 * @param {string=} namespace Property namespace
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.set = function(property, value, namespace) {
  return this.backend_.set(property, value, namespace);
};

/**
 * Define a settings from it's namespace and property name
 * 
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.reset = function() {
  return this.backend_.clear().then(function() {
    return this.set('global.version', goog.now());
  }, function() {
    return this.set('global.version', goog.now());
  }, this);

};

/**
 * Load context for given user
 * 
 * @param {String} uid User id
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @return {goog.Promise}
 */
net.bluemind.authentication.service.AuthService.prototype.loadContext = function(uid, ctx) {
  // Initialise database.
  return ctx.client('auth').getCurrentUser().then(function(user) {
    return user;
  }, function(e) {
    return this.login(uid);
  }, this).then(function(user) {
    return this.storeUser(user);
  }, null, this).then(function(user) {
    ctx.user = user;
    var d = ctx.user['domainUid'];
    var u = ctx.user['uid'];
    return ctx.client('auth').getSettings(d, u);
  }, null, this).then(function(settings) {
    return this.storeSettings(settings);
  }, null, this).thenCatch(function() {
    // Server unreachable.
    return this.getSettings();
  }, this).then(function(settings) {
    ctx.settings = new goog.structs.Map(settings);
    return ctx;
  }).thenCatch(function(error) {
    window.alert("AuthService.loadContext error: " + error);
    // TODO : Server unreachable on a 'new' database
  }, this);

};
