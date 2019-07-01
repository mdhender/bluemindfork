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
 * @fileoverview Interface for protocol handler.
 */

goog.provide('bluemind.events.IProtocolHandler');

goog.require('goog.Uri');

/**
 * Handle link matching the pattern #protocol:value
 * @constructor
 */
bluemind.events.IProtocolHandler = function() {
};

/**
 * Handle the link.
 * The return value tell to the default action associated to this link should be prevented
 * or if the browser should handle it.
 *
 * @param {goog.Uri} uri URI to handle.
 * @return {boolean} True if the default action should be prevented, false else.
 */
bluemind.events.IProtocolHandler.prototype.handleUri = goog.abstractMethod;
