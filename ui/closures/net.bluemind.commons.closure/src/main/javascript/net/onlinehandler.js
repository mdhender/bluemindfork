/**
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

/**
 * @fileoverview Online status management.
 */
goog.provide('bluemind.net.OnlineHandler');
goog.provide('net.bluemind.net.OnlineHandler');

goog.require('bluemind.storage.StorageHelper');
goog.require('bluemind.cmd.net.Ping');
goog.require('goog.async.Deferred');
goog.require('goog.Timer');
goog.require('goog.Uri.QueryData');
goog.require('goog.log.Logger');
goog.require('goog.log');
goog.require('goog.events.BrowserFeature');
goog.require('goog.events.EventHandler');
goog.require('goog.net.NetworkStatusMonitor');
goog.require('goog.net.EventType');

/**
 * Manage the online status of the application. Two states are handled : -
 * Online : Application is connected to the network, and request are allowed. -
 * Offline : Request are forbidden (no network, or request forbiden).
 * 
 * @extends {goog.events.EventTarget}
 * @constructor
 */
bluemind.net.OnlineHandler = function() {
  goog.base(this);
  this.handler_ = new goog.events.EventHandler(this);

  var that = this;
  goog.global['bundleResolve']('net.bluemind.restclient.closure', function() {    
    that.online_ = that.isOnline();
    goog.global['restClient'].addListener(function() {
      that.handleChange_();
    })
  });

};
goog.inherits(bluemind.net.OnlineHandler, goog.events.EventTarget);

goog.addSingletonGetter(bluemind.net.OnlineHandler);

/**
 * @type {goog.log.Logger}
 * @protected
 */
bluemind.net.OnlineHandler.prototype.logger = goog.log.getLogger('bluemind.net.OnlineHandler');

/**
 * Storage key for request authorisation.
 * 
 * @type {string}
 * @private
 */
bluemind.net.OnlineHandler.STATUS_ = 'online';

/**
 * Event handler to simplify event listening.
 * 
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.net.OnlineHandler.prototype.handler_;

/**
 * Called when the online state changes. This dispatches the {@code ONLINE} and
 * {@code OFFLINE} events respectively.
 * 
 * @private
 */
bluemind.net.OnlineHandler.prototype.handleChange_ = function() {
  var type = this.isOnline() ? goog.net.NetworkStatusMonitor.EventType.ONLINE
      : goog.net.NetworkStatusMonitor.EventType.OFFLINE;
  this.dispatchEvent(type);
};

/**
 * Launch a test for bm core availability (use a command instead of the request
 * object).
 * 
 * @param {relief.rpc.RPCService} rpc Service provider
 * @return {goog.async.Deferred} Deferred result
 */
bluemind.net.OnlineHandler.prototype.init = function(rpc) {
  throw 'do not call this !! (bluemind.net.OnlineHandler.prototype.init)';
};

/**
 * Set if the online mode is enabled.
 * 
 * @param {boolean} enabled Request are authorized if true.
 */
bluemind.net.OnlineHandler.prototype.setEnabled = function(enabled) {
  throw 'do not call this !! (bluemind.net.OnlineHandler.prototype.setEnabled)';
};

/**
 * Get the online mode status.
 * 
 * @return {boolean} True if request are authorized, false if not.
 */
bluemind.net.OnlineHandler.prototype.isEnabled = function() {
  throw 'do not call this !! (bluemind.net.OnlineHandler.prototype.isEnabled)';
};

/**
 * Returns whether or not the system is online. This method works properly
 * regardless of whether or not the listener IsListening.
 * 
 * @return {boolean} Whether the browser is currently thinking it is online.
 */
bluemind.net.OnlineHandler.prototype.isOnline = function() {
  return goog.global['restClient'] ? goog.global['restClient'].online(): false;
};

/** @override */
bluemind.net.OnlineHandler.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.handler_.dispose();
  delete this.handler_;
};

net.bluemind.net.OnlineHandler = bluemind.net.OnlineHandler;
