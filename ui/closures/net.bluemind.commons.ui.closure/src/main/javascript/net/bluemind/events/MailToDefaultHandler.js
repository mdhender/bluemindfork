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
 * @fileoverview Abstract class for protocol handler.
 */

goog.provide("net.bluemind.events.MailToDefaultHandler");

goog.require("goog.Timer");
goog.require("goog.math");
goog.require("goog.window");
goog.require("goog.userAgent.product");
goog.require("net.bluemind.events.LinkHandler.ProtocolHandler");

/**
 * Handle link matching the pattern #protocol:value
 * 
 * @constructor
 * @extends {net.bluemind.events.LinkHandler.ProtocolHandler}
 */
net.bluemind.events.MailToDefaultHandler = function() {
};
goog.inherits(net.bluemind.events.MailToDefaultHandler, net.bluemind.events.LinkHandler.ProtocolHandler);

/**
 * @override
 */
net.bluemind.events.MailToDefaultHandler.prototype.handleUri = function(uri) {
  var recipient = uri.getPath();
  if (recipient) {
    if (goog.userAgent.product.IE || goog.userAgent.product.SAFARI) {
      window.location.href = uri.toString();
    } else {
      var options = {
        width : 1100,
        height : 600
      };
      var win = goog.window.open(uri.toString(), options);
      goog.Timer.callOnce(function() {
        if (win && win.location && win.location.href == 'about:blank') {
          win.close();
        }
      }, 2500);
    }
  }
  return true;
};
