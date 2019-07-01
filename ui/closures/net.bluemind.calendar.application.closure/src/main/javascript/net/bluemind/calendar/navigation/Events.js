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
 * @fileoverview Navigation Events
 */

goog.provide('net.bluemind.calendar.navigation.events.EventType');
goog.provide('net.bluemind.calendar.navigation.events.SaveViewEvent');
goog.provide('net.bluemind.calendar.navigation.events.ShowViewEvent');
goog.provide('net.bluemind.calendar.navigation.events.DeleteViewEvent');
/** @enum {string} */
net.bluemind.calendar.navigation.events.EventType = {
  DELETE_VIEW : goog.events.getUniqueId('delete-view'),
  SAVE_VIEW : goog.events.getUniqueId('save-view'),
  SHOW_VIEW : goog.events.getUniqueId('show-calendar-view'),
  SHOW_MY_CALENDAR : goog.events.getUniqueId('show-my-calendar'),
  ADD_CALENDAR : goog.events.getUniqueId('add-calendar'),
  REMOVE_CALENDAR : goog.events.getUniqueId('remove-calendar'),
  CHANGE_CALENDAR_COLOR : goog.events.getUniqueId('change-calendar-color'),
  SHOW_CALENDAR : goog.events.getUniqueId('show-calendar'),
  HIDE_CALENDAR : goog.events.getUniqueId('hide-calendar'),
  TOGGLE_TAG : goog.events.getUniqueId('toggle-tag')
};

/**
 * Object representing a new incoming message event.
 * 
 * @param {string} label The raw message coming from the web socket.
 * @extends {goog.events.Event}
 * @constructor
 */
net.bluemind.calendar.navigation.events.SaveViewEvent = function(uid, label) {
  goog.base(this, net.bluemind.calendar.navigation.events.EventType.SAVE_VIEW);
  this.label = label;
  this.uid = uid;
};
goog.inherits(net.bluemind.calendar.navigation.events.SaveViewEvent, goog.events.Event);

/**
 * @type {string}
 */
net.bluemind.calendar.navigation.events.SaveViewEvent.prototype.label

/**
 * @type {string}
 */
net.bluemind.calendar.navigation.events.SaveViewEvent.prototype.uid

/**
 * Object representing a new incoming message event.
 * 
 * @param {string} viewUid view uid
 * @extends {goog.events.Event}
 * @constructor
 */
net.bluemind.calendar.navigation.events.ShowViewEvent = function(viewUid) {
  goog.base(this, net.bluemind.calendar.navigation.events.EventType.SHOW_VIEW);
  this.viewUid = viewUid;
};
goog.inherits(net.bluemind.calendar.navigation.events.ShowViewEvent, goog.events.Event);

/**
 * @type {string}
 */
net.bluemind.calendar.navigation.events.ShowViewEvent.prototype.viewUid;

/**
 * Object representing a new incoming message event.
 * 
 * @param {string} viewUid view uid
 * @extends {goog.events.Event}
 * @constructor
 */
net.bluemind.calendar.navigation.events.DeleteViewEvent = function(uid) {
  goog.base(this, net.bluemind.calendar.navigation.events.EventType.DELETE_VIEW);
  this.uid = uid;
};
goog.inherits(net.bluemind.calendar.navigation.events.DeleteViewEvent, goog.events.Event);

/**
 * @type {string}
 */
net.bluemind.calendar.navigation.events.DeleteViewEvent.prototype.uid;
