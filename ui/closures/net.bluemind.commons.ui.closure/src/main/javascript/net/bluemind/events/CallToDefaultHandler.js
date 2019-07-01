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

goog.provide("net.bluemind.events.CallToDefaultHandler");

goog.require("net.bluemind.events.LinkHandler.ProtocolHandler");

/**
 * Handle link matching the pattern #protocol:value
 * 
 * @constructor
 * @extends {net.bluemind.events.LinkHandler.ProtocolHandler}
 */
net.bluemind.events.CallToDefaultHandler = function() {
};
goog.inherits(net.bluemind.events.CallToDefaultHandler, net.bluemind.events.LinkHandler.ProtocolHandler);

/**
 * @override
 */
net.bluemind.events.CallToDefaultHandler.prototype.handleUri = function(uri) {
  return false;
};
