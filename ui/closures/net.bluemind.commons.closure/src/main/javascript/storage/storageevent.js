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
*/

/**
 * @fileoverview Event for storage action.
 */

goog.provide('bluemind.storage.StorageEvent');

goog.require('goog.events.Event');

/**
 * Object representing a drag event
 * @param {string} type Event type.
 * @param {Object} stored Stored object.
 * @extends {goog.events.Event}
 * @constructor
 */
bluemind.storage.StorageEvent = function(type, stored) {
  goog.events.Event.call(this, type);

  this.stored = stored;
};
goog.inherits(bluemind.storage.StorageEvent, goog.events.Event);

/**
 * Object inserted/removed/updated into the storage
 * @type {Object}
 */
bluemind.storage.StorageEvent.prototype.stored;
