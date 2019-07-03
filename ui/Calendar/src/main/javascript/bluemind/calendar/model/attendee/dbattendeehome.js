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
 * @fileoverview Get attendee data from DB.
 */

goog.provide('bluemind.calendar.model.attendee.DBAttendeeHome');

goog.require('bluemind.calendar.model.attendee.WSAttendeeHome');
goog.require('bluemind.storage.StorageHelper');
goog.require('goog.async.DeferredList');
goog.require('goog.events');
goog.require('goog.events.EventTarget');
goog.require('ydn.db.Storage');

/**
 * Ask session storage for attendee data.
 * @implements {bluemind.calendar.model.attendee.IAttendeeHome}
 * @constructor
 * @extends {bluemind.calendar.model.attendee.WSAttendeeHome}
 */
bluemind.calendar.model.attendee.DBAttendeeHome = function() {
  this.storage_ = bluemind.storage.StorageHelper.getWebStorage();
  this.serializer_ = new bluemind.storage.Serializer();
  this.cache_ = new goog.structs.Map();

  this.attendeeStore_ = 'attendee';
  this.db_ = bluemind.storage.StorageHelper.getStorage();
};
goog.inherits(bluemind.calendar.model.attendee.DBAttendeeHome,
  bluemind.calendar.model.attendee.WSAttendeeHome);

/**
 * @type {goog.debug.Logger}
 * @protected
 */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.logger =
  goog.debug.Logger.getLogger('bluemind.calendar.model.attendee.DBAttendeeHome');

/**
 * attendee store name
 * @type {text}
 * @private
 */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.attendeeStore_;

/**
 * db
 * @type {ydn.db}
 * @private
 */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.db_;

/** @override */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.storeAttendees =
  function(toStore) {
  var attendees = new Array();
  goog.array.forEach(toStore, function(a) {
    var attendee = a.toMap();
    attendees.push(attendee);
  });
  return this.addAttendees_(attendees);
};

/** @override */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.addAttendees_ =
  function(toAdd, attendees) {
  return this.db_.put(this.attendeeStore_, toAdd);
};

/** @override */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.addAttendee_ =
  function(attendee) {
  return this.addAttendees_([attendee.toMap()]);
};

/** @override */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.get = function(cal, opt_type) {
  var deferred = new goog.async.Deferred();
  cal = cal + '';
  var attendee = this.cache_.get(cal);
  if (attendee != null) {
    goog.log.fine(this.logger, 'Get attendee ' + cal + ' from cache');
    deferred.callback(attendee);
  } else {
    this.db_.get(this.attendeeStore_, cal).addCallback(function(a) {
      this.cache_.set(cal, a);
      deferred.callback(a);
    }, this);
  }
  return deferred;
};

/** @override */
bluemind.calendar.model.attendee.DBAttendeeHome.prototype.getAttendees_ = function() {
  return this.storage_.get(this.getAttendeesKey_()) || {};
};

