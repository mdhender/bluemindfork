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
 * @fileoverview Interface for attendee model management.
 */


goog.provide('bluemind.calendar.model.attendee.IAttendeeHome');

goog.require('bluemind.calendar.model.Attendee');
goog.require('goog.async.Deferred');

/**
 * Interface for attendee storage.
 * @interface
 */
bluemind.calendar.model.attendee.IAttendeeHome = function() {
};

/**
 * Init storage.
 * @return {goog.async.Deferred} Deferred init state.
 */
bluemind.calendar.model.attendee.IAttendeeHome.prototype.init = function() {
};


/**
 * Remove an attendee.
 * @param {bluemind.calendar.model.Attendee} attendee attendee to remove.
 * @return {goog.async.Deferred} Deferred attendee object.
 */
bluemind.calendar.model.attendee.IAttendeeHome.prototype.remove = function(attendee) {
};

/**
 * Store attendee information into the storage.
 * @param {bluemind.calendar.model.Attendee} attendee Attendee to store.
 * @return {goog.async.Deferred} Deferred attendee object.
 */
bluemind.calendar.model.attendee.IAttendeeHome.prototype.store = function(attendee) {
};

/**
 * Store attendees information into the storage.
 * @param {Array} toStore Attendees to store.
 * @return {goog.async.Deferred} Deferred attendee object.
 */
bluemind.calendar.model.attendee.IAttendeeHome.prototype.storeAttendees =
  function(toStore) {
};

/**
 * Get an attendee from storage.
 * @param {string} id Attendee id.
 * @param {bluemind.calendar.model.AttendeeType=} opt_type Attendee type.
 * @return {goog.async.Deferred} Deferred attendee object.
 */
bluemind.calendar.model.attendee.IAttendeeHome.prototype.get = function(id, opt_type) {
};

/**
 * Get attendeeslist
 * @return {Array.<Object>} Array of serialized attendees.
 * @return {goog.async.Deferred} Deferred array of serialized attendees.
 */
bluemind.calendar.model.attendee.IAttendeeHome.prototype.list = function() {
};

