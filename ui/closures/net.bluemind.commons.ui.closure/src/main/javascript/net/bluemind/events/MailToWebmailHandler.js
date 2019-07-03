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

goog.provide("net.bluemind.events.MailToWebmailHandler");
goog.provide("net.bluemind.events.MailToWebmailHandler.RCubeHelper");

goog.require("goog.math");
goog.require("goog.window");
goog.require("net.bluemind.events.LinkHandler.ProtocolHandler");

/**
 * Handle link matching the pattern #protocol:value
 * 
 * @constructor
 * @extends {net.bluemind.events.LinkHandler.ProtocolHandler}
 */
net.bluemind.events.MailToWebmailHandler = function() {
};
goog.inherits(net.bluemind.events.MailToWebmailHandler, net.bluemind.events.LinkHandler.ProtocolHandler);

/** @override */
net.bluemind.events.MailToWebmailHandler.prototype.handleUri = function(uri) {
  net.bluemind.events.MailToWebmailHandler.RCubeHelper.mailTo(uri.getPath());
  return true;
};
/**
 * @constructor
 */
net.bluemind.events.MailToWebmailHandler.RCubeHelper = function() {
};

net.bluemind.events.MailToWebmailHandler.RCubeHelper.URL = '/webmail/';

/**
 * Open the compose window with the 'To' field fill with the email
 * 
 * @param {string} recipient Email of the contact or id of the list
 * @return {Window} Child window
 */
net.bluemind.events.MailToWebmailHandler.RCubeHelper.mailTo = function(recipient) {
  if (recipient) {
    var url;
    if (goog.string.startsWith(recipient, 'dlist:')) {
      // expand dlist
      recipient = goog.string.removeAt(recipient, 0, 6);
      url = net.bluemind.events.MailToWebmailHandler.RCubeHelper.URL + '?_task=mail&_action=compose&_gid=' + recipient;
    } else {
      url = net.bluemind.events.MailToWebmailHandler.RCubeHelper.URL + '?_task=mail&_action=compose&_to=' + recipient;
    }

    var options = {
      width : 1100,
      height : 600,
      target : 'rc_compose_child'
    };
    var win = goog.window.open(url, options);
    return win;
  } else {
    return null;
  }
};
