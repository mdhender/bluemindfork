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
 * @fileoverview Action on vevent event .
 */
goog.provide('net.bluemind.calendar.vevent.VEventEvent');

goog.require('goog.events.Event');


/**
 * Object representing a action on a vevent.
 * @param {string|!goog.events.EventId} type Event Type.
 * @param {Object} vevent Vevent object
 * @param {Object=} opt_target Reference to the object that is the target of
 *     this event. It has to implement the {@code EventTarget} interface
 *     declared at {@link http://developer.mozilla.org/en/DOM/EventTarget}.
 * @param {boolean=} opt_force 
 * @extends {goog.events.Event}
 * @constructor
 */
net.bluemind.calendar.vevent.VEventEvent = function(type, vevent, opt_target, opt_force) {
  goog.base(this, type, opt_target);
  this.vevent = vevent;
};
goog.inherits(net.bluemind.calendar.vevent.VEventEvent, goog.events.Event);

/**
 * @type {Object} 
 */
net.bluemind.calendar.vevent.VEventEvent.prototype.vevent;


/**
 * Action has been confirmed or forced
 * @type {boolean} 
 */
net.bluemind.calendar.vevent.VEventEvent.prototype.force;