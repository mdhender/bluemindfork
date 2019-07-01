/*
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

goog.provide("net.bluemind.calendar.vevent.ac.AttendeeAutocomplete");

goog.require("goog.ui.ac.AutoComplete");
goog.require("goog.ui.ac.InputHandler");
goog.require("goog.ui.ac.Renderer");
goog.require("net.bluemind.calendar.vevent.ac.AttendeeMatcher");
goog.require("net.bluemind.calendar.vevent.ac.AttendeeRowRenderer");

/**
 * @constructor
 * 
 * @param {Object} matcher
 * @param {goog.events.EventTarget} renderer
 * @param {Object} selectionHandler
 * @extends {goog.ui.ac.AutoComplete}
 */
net.bluemind.calendar.vevent.ac.AttendeeAutocomplete = function(ctx) {
  var matcher = new net.bluemind.calendar.vevent.ac.AttendeeMatcher(ctx);
  var renderer = new goog.ui.ac.Renderer(null, new net.bluemind.calendar.vevent.ac.AttendeeRowRenderer());
  var handler = new goog.ui.ac.InputHandler(null, null, false, 250);
  handler.setUpdateDuringTyping(false);
  goog.base(this, matcher, renderer, handler);
  handler.attachAutoComplete(this);
};
goog.inherits(net.bluemind.calendar.vevent.ac.AttendeeAutocomplete, goog.ui.ac.AutoComplete);

/** @override */
net.bluemind.calendar.vevent.ac.AttendeeAutocomplete.prototype.attachInputs = function(input) {
  this.renderer_.setWidthProvider(input);
  goog.base(this, 'attachInputs', input);
};

/**
 * Set attendees already present in attendee selection.
 * 
 * @param {Array.<Object>} attendees;
 */
net.bluemind.calendar.vevent.ac.AttendeeAutocomplete.prototype.setAttendees = function(attendees) {
  this.matcher_.attendees = attendees;
};

/**
 * Set an attendee group already present in attendee selection.
 * 
 * @param {Object} attendee;
 * @param {Array.<Object>} attendees;
 */
net.bluemind.calendar.vevent.ac.AttendeeAutocomplete.prototype.addGroupAttendee = function(attendee, attendees) {
  var groupAttendee = {
    "uri" : attendee,
    "attendees" : attendees
  }
  this.matcher_.groupAttendees.push(groupAttendee);
};

/**
 * Sanitizes the group attendees after an attendee has been removed
 * 
 * @param {Object} attendee;
 */
net.bluemind.calendar.vevent.ac.AttendeeAutocomplete.prototype.sanitizeGroups = function(attendee) {
  this.matcher_.attendeeRemoved(attendee);
};

/** @override */
net.bluemind.calendar.vevent.ac.AttendeeAutocomplete.prototype.setToken = function(token, opt_fullString) {
  if (!token) {
    this.setTokenInternal('');
    this.dismiss();
  } else {
    goog.base(this, 'setToken', token, opt_fullString);
  }
};
