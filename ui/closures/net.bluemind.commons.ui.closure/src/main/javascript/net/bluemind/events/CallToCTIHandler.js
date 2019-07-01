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
 * @fileoverview Abstract class for protocol handler.
 */

goog.provide("net.bluemind.events.CallToCTIHandler");

goog.require("goog.async.Deferred");
goog.require("net.bluemind.events.LinkHandler.ProtocolHandler");
goog.require('net.bluemind.cti.api.ComputerTelephonyIntegrationClient');

/**
 * Handle link matching the pattern #protocol:value
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx The app's service provider.
 * @constructor
 * @extends {net.bluemind.events.LinkHandler.ProtocolHandler}
 */
net.bluemind.events.CallToCTIHandler = function(ctx) {
  this.ctx_ = ctx;
};
goog.inherits(net.bluemind.events.CallToCTIHandler, net.bluemind.events.LinkHandler.ProtocolHandler);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.events.CallToCTIHandler.prototype.ctx_;

/**
 * @override
 */
net.bluemind.events.CallToCTIHandler.prototype.handleUri = function(uri) {
  var deferred = new goog.async.Deferred();
  var client = new net.bluemind.cti.api.ComputerTelephonyIntegrationClient(this.ctx_.rpc, '',
      this.ctx_.user['domainUid'], this.ctx_.user['uid']);

  client.dial(uri.getPath()).addCallback(function(number) {
    // TODO: message
    // this.sp_.getNotificationHandler().ok(bluemind.i18n.data('Dialing ' +
    // number));
  }).addErrback(function(number) {
    // TODO: error management
    // this.sp_.getNotificationHandler().error(bluemind.i18n.data('Fail to call
    // ' + number));
  });
  return true;
};
