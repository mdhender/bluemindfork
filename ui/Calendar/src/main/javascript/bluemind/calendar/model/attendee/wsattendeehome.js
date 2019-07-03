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
 * @fileoverview Get attendee data from Web Storage.
 */

goog.provide('bluemind.calendar.model.attendee.WSAttendeeHome');

goog.require('bluemind.storage.Serializer');
goog.require('bluemind.storage.StorageHelper');
goog.require('goog.async.Deferred');

/**
 * Ask session storage for attendee data.
 * @implements {bluemind.calendar.model.attendee.IAttendeeHome}
 * @constructor
 */
bluemind.calendar.model.attendee.WSAttendeeHome = function() {
  this.storage_ = bluemind.storage.StorageHelper.getStorage();
  this.serializer_ = new bluemind.storage.Serializer();
  this.cache_ = new goog.structs.Map();
};

/**
 * @type {goog.debug.Logger}
 * @protected
 */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.logger =
  goog.debug.Logger.getLogger('bluemind.calendar.model.attendee.WSAttendeeHome');

/**
 * Standard storage whith the default mechanism.
 * @return {goog.storage.Storage} Storage accessor.
 * @private
 */
bluemind.calendar.model.attendee.WSAttendeeHome.storage_;

/**
 * @type {bluemind.storage.Serializer}
 * @private
 */
bluemind.calendar.model.attendee.WSAttendeeHome.serializer_;

/**
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.cache_;

/** @override */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.init = function() {
};

/** @override */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.remove = function(attendee) {
  var deferred = new goog.async.Deferred();
  goog.log.info(this.logger, 'Remove attendee ' + attendee.getDisplayName() +
    ' [' + attendee.getId() + ']');
  this.remove_(attendee);
  deferred.callback(attendee);
  return deferred;
};

/** @override */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.store = function(attendee) {
  var deferred = new goog.async.Deferred();
  var attendees = this.getAttendees_();
  var id = 'a-' + attendee.getCalendarId();

  if (attendees[id] == null) {
    goog.log.info(this.logger, 'Store attendee ' + attendee.getDisplayName() +
      ' [' + id + ']');
    this.addAttendee_(attendee);
  }
  deferred.callback(attendee);
  return deferred;
};

/** @override */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.storeAttendees =
  function(toStore) {
  var deferred = new goog.async.Deferred();
  var attendees = this.getAttendees_();
  var notInStorage = new Array();
  goog.array.forEach(toStore, function(a) {

    var id = 'a-' + a.getCalendarId();

    if (attendees[id] == null) {
      notInStorage.push(a);
    }
  });
  if (notInStorage.length > 0) {
    goog.log.info(this.logger, 'Store ' + notInStorage.length + ' attendees');
    this.addAttendees_(notInStorage, attendees);
  }
  deferred.callback(attendees);
  return deferred;
};

/** @override */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.get = function(id, opt_type) {
  var deferred = new goog.async.Deferred();
  var attendee = this.cache_.get(id);
  if (attendee != null) {
    goog.log.fine(this.logger, 'Get attendee ' + id + ' from cache');
  } else {
    var type = opt_type || bluemind.calendar.model.AttendeeType.USER;
    goog.log.fine(this.logger, 'Get attendee ' + id + ' of type ' + type);
    var attendees = this.getAttendees_();
    if (attendees[id]) {
      var a = this.serializer_.unserialize(attendees[id],
          bluemind.calendar.model.attendee.WSAttendeeHome.AttendeeSchema);
      attendee = new bluemind.calendar.model.Attendee();
      attendee.setDisplayName(a['displayName']);
      attendee.setEmail(a['email']);
      attendee.setCalendarId(a['calendar']);
      attendee.setId(a['id']);
      attendee.setType(a['type']);
      attendee.setPicture(a['picture']);
      attendee.setWorkingDays(a['workingDays']);
      attendee.setDayStart(a['dayStart']);
      attendee.setDayEnd(a['dayEnd']);
    }
    this.cache_.set(id, attendee);
  }
  deferred.callback(attendee);
  return deferred;
};

/** @override */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.list = function() {
  var deferred = new goog.async.Deferred();
  var attendees = this.getAttendees_();
  var a = [];
  for (var key in attendees) {
    a.push(this.serializer_.unserialize(attendees[key],
      bluemind.calendar.model.attendee.WSAttendeeHome.AttendeeSchema));
  }
  deferred.callback(a);
  return deferred;
};

/**
 * Get attendees storage data
 * @return {Object.<String, Object>} Attendees storage data.
 * @private
 */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.getAttendees_ = function() {
  return this.storage_.get(this.getAttendeesKey_()) || {};
};

/**
 * add an attendee to storage data
 * @param {bluemind.calendar.model.Attendee} attendee Attendee to add.
 * @private
 */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.addAttendee_ =
  function(attendee) {
  var data = this.serializer_.serialize(attendee.toMap(),
    bluemind.calendar.model.attendee.WSAttendeeHome.AttendeeSchema);
  var attendees = this.getAttendees_();

  var id = 'a-' + attendee.getCalendarId();

  attendees[id] = data;
  this.storage_.set(this.getAttendeesKey_(), attendees);
};

/**
 * add attendees to storage data
 * @param {Array} toAdd Attendees to add.
 * @param {Object.<String, Object>} attendees storage data.
 * @private
 */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.addAttendees_ =
  function(toAdd, attendees) {
  var serializer = this.serializer_;
  var logger = this.logger;
  goog.array.forEach(toAdd, function(a) {
    var data = serializer.serialize(a.toMap(),
      bluemind.calendar.model.attendee.WSAttendeeHome.AttendeeSchema);

    var id = 'a-' + a.getCalendarId();

    attendees[id] = data;
    goog.log.info(logger, 'Store attendee ' + a.getDisplayName() +
      ' [' + id + ']');
  });
  this.storage_.set(this.getAttendeesKey_(), attendees);
};

/**
 * Get attendees storage key
 * @return {string} The key for attendees in the local storage.
 * @private
 */
bluemind.calendar.model.attendee.WSAttendeeHome.prototype.getAttendeesKey_ = function() {
  return 'attendees';
};

/**
 * Attendee schema for storage serialization
 * @type {Object}
 */
bluemind.calendar.model.attendee.WSAttendeeHome.AttendeeSchema = {
  'id': {
    kind: bluemind.storage.Serializer.Type.INTEGER
  },
  'calendar': {
    kind: bluemind.storage.Serializer.Type.INTEGER
  },
  'picture' : {
    kind: bluemind.storage.Serializer.Type.STRING
  },
  'email': {
    kind: bluemind.storage.Serializer.Type.STRING
  },
  'label': {
    kind: bluemind.storage.Serializer.Type.STRING
  },
  'displayName': {
    kind: bluemind.storage.Serializer.Type.STRING
  },
  'type': {
    kind: bluemind.storage.Serializer.Type.ENUM,
    def: 'user',
    values: ['user', 'contact', 'resource']
  },
  'workingDays': {
    kind: bluemind.storage.Serializer.Type.STRING
  },
  'dayStart': {
    kind: bluemind.storage.Serializer.Type.STRING
  },
  'dayEnd': {
    kind: bluemind.storage.Serializer.Type.STRING
  }
};

