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
 * @fileoverview Attendee model.
 */
goog.provide('bluemind.calendar.model.Attendee');
goog.provide('bluemind.calendar.model.AttendeeParticipation');
goog.provide('bluemind.calendar.model.AttendeeParticipationRole');
goog.provide('bluemind.calendar.model.AttendeeType');

/**
 * Attendee model class
 * @constructor
 */
bluemind.calendar.model.Attendee = function() {};

/**
 * Attendee display name
 * @type {string}
 * @private
 */
bluemind.calendar.model.Attendee.prototype.displayName_;

/**
 * Attendee email
 * @type {string}
 * @private
 */
bluemind.calendar.model.Attendee.prototype.email_;

/**
 * Attendee id
 * @type {number}
 * @private
 */
bluemind.calendar.model.Attendee.prototype.id_;

/**
 * Attendee calendar id
 * @type {number}
 * @private
 */
bluemind.calendar.model.Attendee.prototype.calendarId_;

/**
 * Attendee participation state
 * @type {bluemind.calendar.model.AttendeeParticipation}
 */
bluemind.calendar.model.Attendee.prototype.participation_;

/**
 * Attendee participation role
 * @type {bluemind.calendar.model.AttendeeParticipationRole}
 */
bluemind.calendar.model.Attendee.prototype.participationRole_;

/**
 * Attendee working days
 * @type {text}
 */
bluemind.calendar.model.Attendee.prototype.workingDays_;

/**
 * Attendee day start
 * @type {number}
 */
bluemind.calendar.model.Attendee.prototype.dayStart_;

/**
 * Attendee day end
 * @type {number}
 */
bluemind.calendar.model.Attendee.prototype.dayEnd_;

/**
 * Attendee picture id
 * @type {number}
 * @private
 */
bluemind.calendar.model.Attendee.prototype.picture_;

/**
 * Attendee type
 * @type {bluemind.calendar.model.AttendeeType}
 * @private
 */
bluemind.calendar.model.Attendee.prototype.type_;

/**
 * Attendee notification status
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Attendee.prototype.notified_;

/**
 * Display name setter
 * @param {string} displayName Display name.
 */
bluemind.calendar.model.Attendee.prototype.setDisplayName =
  function(displayName) {
  this.displayName_ = displayName;
};

/**
 * Display name getter
 * @return {string} Display name.
 */
bluemind.calendar.model.Attendee.prototype.getDisplayName = function() {
  return this.displayName_;
};

/**
 * Email setter
 * @param {string} email Email.
 */
bluemind.calendar.model.Attendee.prototype.setEmail = function(email) {
  this.email_ = email;
};

/**
 * Email getter
 * @return {string} Attendee email.
 */
bluemind.calendar.model.Attendee.prototype.getEmail = function() {
  return this.email_;
};

/**
 * Id setter
 * @param {number} id Attendee id.
 */
bluemind.calendar.model.Attendee.prototype.setId = function(id) {
  if (parseInt(id) == id) {
    this.id_ = id + '';
  } else {
    this.id_ = null;
  }
};

/**
 * Id getter
 * @return {number} Attendee id.
 */
bluemind.calendar.model.Attendee.prototype.getId = function() {
  return this.id_;
};

/**
 * calendarId setter
 * @param {number} cid Attendee calendar id.
 */
bluemind.calendar.model.Attendee.prototype.setCalendarId = function(cid) {
  this.calendarId_ = cid + '';
};

/**
 * calendarId getter
 * @return {number} Attendee calendar id.
 */
bluemind.calendar.model.Attendee.prototype.getCalendarId = function() {
  return this.calendarId_;
};

/**
 * Participation setter
 * @param {bluemind.calendar.model.AttendeeParticipation} part Attendee
 *   participation.
 */
bluemind.calendar.model.Attendee.prototype.setParticipation = function(part) {
  this.participation_ = part;
};

/**
 * Participation getter
 * @return {bluemind.calendar.model.AttendeeParticipation} Attendee
 *   participation.
 */
bluemind.calendar.model.Attendee.prototype.getParticipation = function() {
  return this.participation_;
};

/**
 * Participation role setter
 * @param {bluemind.calendar.model.AttendeeParticipationRole} role Attendee
 *   participation role.
 */
bluemind.calendar.model.Attendee.prototype.setParticipationRole =
  function(role) {
  this.participationRole_ = role;
};

/**
 * Participation getter
 * @return {bluemind.calendar.model.AttendeeParticipationRole} Attendee
 *   participation role.
 */
bluemind.calendar.model.Attendee.prototype.getParticipationRole = function() {
  return this.participationRole_;
};

/**
 * Working days setter
 * @param {text} workingDays working days.
 */
bluemind.calendar.model.Attendee.prototype.setWorkingDays =
  function(workingDays) {
  this.workingDays_ = workingDays;
};

/**
 * Working days getter
 * @return {text} workingDays working days.
 */
bluemind.calendar.model.Attendee.prototype.getWorkingDays = function() {
  return this.workingDays_;
};

/**
 * Day start setter
 * @param {number} dayStart day start.
 */
bluemind.calendar.model.Attendee.prototype.setDayStart = function(dayStart) {
  this.dayStart_ = dayStart;
};

/**
 * Day start getter
 * @return {number} dayStart day start.
 */
bluemind.calendar.model.Attendee.prototype.getDayStart = function() {
  return this.dayStart_;
};

/**
 * Day end setter
 * @param {number} dayEnd day end.
 */
bluemind.calendar.model.Attendee.prototype.setDayEnd = function(dayEnd) {
  this.dayEnd_ = dayEnd;
};

/**
 * Day end getter
 * @return {number} dayEnd day end.
 */
bluemind.calendar.model.Attendee.prototype.getDayEnd = function() {
  return this.dayEnd_;
};

/**
 * Picture setter
 * @param {number} picture Attendee picture id.
 */
bluemind.calendar.model.Attendee.prototype.setPicture = function(picture) {
  this.picture_ = picture;
};

/**
 * Picture getter
 * @return {number} picture Attendee picture id.
 */
bluemind.calendar.model.Attendee.prototype.getPicture = function() {
  return this.picture_;
};

/**
 * Type setter
 * @param {bluemind.calendar.model.AttendeeType} type Attendee type.
 */
bluemind.calendar.model.Attendee.prototype.setType = function(type) {
  this.type_ = type;
};

/**
 * Type getter
 * @return {bluemind.calendar.model.AttendeeType} Attendee type.
 */
bluemind.calendar.model.Attendee.prototype.getType = function() {
  return this.type_;
};

/**
 * notified setter
 * @param {boolean} notified Attendee notification status.
 */
bluemind.calendar.model.Attendee.prototype.setNotified = function(notified) {
  this.notified_ = notified;
};

/**
 * notified getter
 * @return {boolean} Attendee notification status.
 */
bluemind.calendar.model.Attendee.prototype.isNotified = function() {
  return this.notified_;
};

/**
 * @return {string} A string describing the attende.
 */
bluemind.calendar.model.Attendee.prototype.toString = function() {
  return this.displayName_ + ' <' + this.email_ + '>';
};

/**
 * Transform attendee to an easily serializable object
 * @return {Object} Attendee hashmap.
 */
bluemind.calendar.model.Attendee.prototype.toMap = function() {
  //FIXME: The label part should be removed
  var map = {
    'picture': this.getPicture(),
    'email': this.getEmail(),
    'label': this.getDisplayName(),
    'displayName': this.getDisplayName(),
    'type': this.getType(),
    'calendar': this.getCalendarId(),
    'id': this.getId(),
    'workingDays': this.getWorkingDays(),
    'dayStart': this.getDayStart(),
    'dayEnd': this.getDayEnd(),
    'notified': this.isNotified()
  };

  if (this.getParticipation()) {
    map['participation'] = this.getParticipation();
  }

  if (this.getParticipationRole()) {
    map['role'] = this.getParticipationRole();
  } else {
    map['role'] = bluemind.calendar.model.AttendeeParticipationRole.REQ;
  }
  return map;
};
/**
 * Attendee type
 * @enum
 */
bluemind.calendar.model.AttendeeType = {
  USER: 'user',
  CONTACT: 'contact',
  RESOURCE: 'resource'
};

/**
 * Attendee participation
 * @enum
 */
bluemind.calendar.model.AttendeeParticipation = {
  ACCEPTED: 'ACCEPTED',
  NEEDSACTION: 'NEEDS-ACTION',
  DECLINED: 'DECLINED'
};

/**
 * Attendee part role
 * @enum
 */
bluemind.calendar.model.AttendeeParticipationRole = {
  CHAIR: 'CHAIR',
  REQ: 'REQ',
  OPT: 'OPT',
  NON: 'NON'
};

/**
 *
 */
bluemind.calendar.model.AttendeeParticipation.getFromInt = function(i) {
  var ret = bluemind.calendar.model.AttendeeParticipation.ACCEPTED; 
  switch(i) {
    case '0':
      ret = bluemind.calendar.model.AttendeeParticipation.ACCEPTED;
      break;
    case '1':
      ret = bluemind.calendar.model.AttendeeParticipation.NEEDSACTION;
      break;
    case '2':
      ret = bluemind.calendar.model.AttendeeParticipation.DECLINED;
      break;
    default:
      ret = bluemind.calendar.model.AttendeeParticipation.ACCEPTED; 
  }
  return ret;
};

/**
 *
 */
bluemind.calendar.model.AttendeeParticipationRole.getFromInt = function(i) {
  var ret = bluemind.calendar.model.AttendeeParticipationRole.CHAIR;
  switch(i) {
    case '0':
      ret = bluemind.calendar.model.AttendeeParticipationRole.CHAIR;
      break;
    case '1':
      ret = bluemind.calendar.model.AttendeeParticipationRole.REQ;
      break;
    case '2':
      ret = bluemind.calendar.model.AttendeeParticipationRole.OPT;
      break;
    case '2':
      ret = bluemind.calendar.model.AttendeeParticipationRole.NON;
      break;
    default:
      ret = bluemind.calendar.model.AttendeeParticipationRole.CHAIR;
  }
  return ret;
};
