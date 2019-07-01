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

/** @fileoverview Object thats hold application context */

goog.provide('net.bluemind.mvp.ApplicationContext');

goog.require('goog.structs.Map');
goog.require('goog.net.Cookies');

/**
 * Application context contains current uri, parameters, current user,
 * application settings, and eventually session variables. It's mean is to
 * 
 * @constructor
 */
net.bluemind.mvp.ApplicationContext = function() {
  this.session = new goog.structs.Map();
  this.services_ = new goog.structs.Map();
  this.clients_ = new goog.structs.Map();
  this.helpers_ = new goog.structs.Map();
  this.instances_ = new goog.structs.Map();
  this.handlers_ = new goog.structs.Map();
  this.logger_ = goog.log.getLogger('net.bluemind.mvp.ApplicationContext');

};

/**
 * RPC Connection pool
 * 
 * @type {relief.rpc.RPCService}
 */
net.bluemind.mvp.ApplicationContext.prototype.rpc;

/**
 * Base URL
 * 
 * @type {string}
 */
net.bluemind.mvp.ApplicationContext.prototype.base;

/**
 * Current URL
 * 
 * @type {goog.Uri}
 */
net.bluemind.mvp.ApplicationContext.prototype.uri;

/**
 * URL parameters
 * 
 * @type {goog.Uri.QueryData}
 */
net.bluemind.mvp.ApplicationContext.prototype.params;

/**
 * SESSION paramaters
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.mvp.ApplicationContext.prototype.session;

/**
 * Part between the first and the second (if present) slash of the uri
 * 
 * @type {string}
 */
net.bluemind.mvp.ApplicationContext.prototype.module;

/**
 * Part between the second and the third (if present) slash of the uri
 * 
 * @type {string}
 */
net.bluemind.mvp.ApplicationContext.prototype.action;

/**
 * User settings
 * 
 * @type {goog.structs.Map}
 */
net.bluemind.mvp.ApplicationContext.prototype.settings;

/**
 * database available flag
 * 
 * @type {boolean}
 */
net.bluemind.mvp.ApplicationContext.prototype.databaseAvailable;

/**
 * privacy
 * 
 * @type {boolean}
 */
net.bluemind.mvp.ApplicationContext.prototype.privacy;

/**
 * online flag
 * 
 * @type {boolean}
 */
net.bluemind.mvp.ApplicationContext.prototype.online;

/**
 * User data
 * 
 * @type {Object}
 */
net.bluemind.mvp.ApplicationContext.prototype.user;

/**
 * Objects instances
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.mvp.ApplicationContext.prototype.instances_;

/**
 * Service registry
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.mvp.ApplicationContext.prototype.services_;

/**
 * Client registry
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.mvp.ApplicationContext.prototype.clients_;

/**
 * Helpers registry
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.mvp.ApplicationContext.prototype.helpers_;

/**
 * Handlers registry
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.mvp.ApplicationContext.prototype.handlers_;

/**
 * cookies (miam)
 * 
 * @type {goog.net.Cookies}
 */

net.bluemind.mvp.ApplicationContext.prototype.cookies;

/**
 * Get / Set service object. Service are instanciated only once.
 * 
 * @param {string} name Service name
 * @param {function(new: goog.events.EventTarget,
 *                net.bluemind.mvp.ApplicationContext)=} opt_service Service
 *                constructor.
 * @return {net.bluemind.container.service.ContainerService|net.bluemind.container.service.ContainersService}
 * @suppress {missingProperties}
 */
net.bluemind.mvp.ApplicationContext.prototype.service = function(name, opt_service) {
  if (opt_service && !this.services_.containsKey(name)) {
    this.services_.set(name, new opt_service(this));
  }

  if (!this.services_.containsKey(name)) {
    return null;
  } else {
    return this.services_.get(name);
  }
};

/**
 * Get / Set client object. Client are instanciated each time.
 * 
 * @param {string} name Service name
 * @param {function(new: net.bluemind.api.BlueMindClient, relief.rpc.RPCService,
 *                string)=} opt_client Service constructor.
 * @return {net.bluemind.api.BlueMindClient}
 */
net.bluemind.mvp.ApplicationContext.prototype.client = function(name, opt_client) {
  if (opt_client && !this.clients_.containsKey(name)) {
    this.clients_.set(name, opt_client);
  }
  if (!this.clients_.containsKey(name)) {
    return null;
  } else {
    var ctor = this.clients_.get(name);
    return new ctor(this.rpc, '');
  }
};

/**
 * Get / Set helper. Helper must be instaciated.
 * 
 * @param {string} name Helper name
 * @param {Object=} opt_helper Helper to register
 */
net.bluemind.mvp.ApplicationContext.prototype.helper = function(name, opt_helper) {
  if (opt_helper && !this.helpers_.containsKey(name)) {
    this.helpers_.set(name, opt_helper);
  }
  if (!this.helpers_.containsKey(name)) {
    return false;
  } else {
    var helper = this.helpers_.get(name);
    return helper;
  }
};

/**
 * Get / Set handlers. Handler must be instanciated.
 * 
 * @param {string} name Helper name
 * @param {goog.events.EventTarget=} opt_handler Handler to register
 * @return {goog.events.EventTarget}
 */
net.bluemind.mvp.ApplicationContext.prototype.handler = function(name, opt_handler) {
  if (opt_handler && !this.handlers_.containsKey(name)) {
    this.handlers_.set(name, opt_handler);
  }
  if (!this.handlers_.containsKey(name)) {
    return null;
  } else {
    var handler = this.handlers_.get(name);
    return handler;
  }
};

/**
 * Notify an information in application DOM
 * 
 * @param {string} msg
 */
net.bluemind.mvp.ApplicationContext.prototype.notifyInfo = function(msg) {
  this.notify('info', msg);
}

/**
 * Notify an error in application DOM
 * 
 * @param {string} msg
 * @param {string | Error=} opt_err
 */
net.bluemind.mvp.ApplicationContext.prototype.notifyError = function(msg, opt_err) {
  this.notify('error', msg, opt_err);
}

/**
 * Event notification through dom.
 * 
 * @param {string} type Error type
 * @param {string} msg Erropr message
 * @param {string | Error=} opt_err Error
 */
net.bluemind.mvp.ApplicationContext.prototype.notify = function(type, msg, opt_err) {
  var doc = goog.dom.getDocument();

  if (opt_err && opt_err instanceof Error) {
    goog.log.error(this.logger_, msg, opt_err);
  } else if (opt_err) {
    goog.log.error(this.logger_, msg, new Error(opt_err))
  }

  if (opt_err == 401) {
    var uri = goog.global.location.pathname;
    uri = uri.substring(0, uri.indexOf('/', 1) + 1);
    goog.global.location.assign(uri);
  }

  var evt = doc.createEvent('Event');// new
  evt.initEvent('ui-notification', true, true);
  evt['detail'] = {
    'type' : type,
    'message' : msg
  };
  doc.dispatchEvent(evt);
}
