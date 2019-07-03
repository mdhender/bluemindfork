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
 * @fileoverview Event.
 */

goog.provide('bluemind.calendar.model.Event');

goog.require('bluemind.calendar.model.AttendeeParticipation');
goog.require('bluemind.date.DateTime');
goog.require('bluemind.fx.Dragger');
goog.require('bluemind.fx.Dragger.EventType');
goog.require('bluemind.fx.HeightResizer');
goog.require('bluemind.fx.HorizontalDragger');
goog.require('bluemind.html.sanitize');
goog.require('bluemind.ui.editor.Image');
goog.require('goog.array');
goog.require('goog.events.EventHandler');
goog.require('goog.fx.DragEvent');
goog.require('goog.fx.DragScrollSupport');
goog.require('goog.fx.Dragger.EventType');
goog.require('goog.i18n.DateTimeFormat');
goog.require('goog.math.Size');
goog.require('goog.ui.Tooltip');

/**
 * Event.
 * @param {Array} ev event.
 * @constructor
 */
bluemind.calendar.model.Event = function(ev) {
  this.id_ = ev['id'];
  this.usercreate_ = ev['uc'];
  this.userupdate_ = ev['uu'];
  this.timecreate_ = ev['tc'];
  this.timeupdate_ = ev['tu'];
  this.extId_ = ev['extId'];
  var date = new bluemind.date.DateTime();
  date.setTime(ev['date']);
  this.setDate(date);
  this.duration_ = ev['duration'];
  this.end_ = date.clone();
  this.end_.add(new goog.date.Interval(0, 0, 0, 0, 0, this.duration_));
  this.title_ = ev['title'] || '';
  this.ownerId_ = ev['ownerId'] || bluemind.me['id'];
  this.ownerDisplayName_ = ev['ownerDN'];
  this.calendar_ = ev['calendar'] || bluemind.me['calendar'];
  this.ownerCalendarId_ = ev['ownerCalId']; // owner is a contact is ownerCalendarId_ == 0
  this.calendarLabel_ = ev['calendarLabel'];
  this.klass_ = ev['klass'];
  this.allday_ =
    (ev['allday'] == 'true' || ev['allday'] == true) ? true : false;
  this.left_ = false;
  this.right_ = false;
  this.multiweeks = false;
  this.location_ = ev['location'] || '';
  this.desc_ = ev['desc'] || '';
  this.desc_ = bluemind.html.sanitize(this.desc_);
  this.opacity_ = (ev['opacity'] != 'TRANSPARENT');
  this.private_ =
    (ev['private'] == 'true' || ev['private'] == true) ? true : false;
  this.alert_ = ev['alert'] >= 0 ? ev['alert'] : -1;
  this.attendees_ = ev['attendees'];
  this.children_ = new Array();
  this.updatable_ =
    (!this.private_ && goog.array.contains(bluemind.writableCalendars,
    this.ownerCalendarId_ + '') && this.ownerCalendarId_ > 0) || this.ownerId_ == bluemind.me['id'];

  this.isAttend_ = false;
  if (this.attendees_ != null) {
    if (this.attendees_.length == 1 && goog.array.contains(bluemind.writableCalendars,
      this.attendees_[0]['calendar']) && this.ownerCalendarId_ > 0) {
      this.updatable_ = true;
    }
    for (var i = 0; i < this.attendees_.length; i++) {
      var att = this.attendees_[i];
      if (att['type'] == 'user' && att['id'] == bluemind.me['id']) {
        this.isAttend_ = true;
        this.participation_ = att['participation'];
      }
    }
  }
  this.setInitialValues();

  // Recurrence
  this.repeatKind_ = ev['repeatKind'] || 'none';
  if (ev['repeatEnd'] != null) {
    this.repeatEnd_ = new bluemind.date.DateTime();
    this.repeatEnd_.setTime(ev['repeatEnd']);
  } else {
    this.repeatEnd_ = null;
  }
  this.repeatFreq_ = ev['repeatFreq'] || 1;
  this.repeatDays_ = ev['repeatDays'];
  if (ev['exceptions'] != null) {
    this.exceptions_ = ev['exceptions'];
  } else {
    this.exceptions_ = new Array();
  }
  if (ev['exceptionExtIds'] != null) {
    this.exceptionExtIds_ = ev['exceptionExtIds'];
  } else {
    this.exceptionExtIds_ = new Array();
  }
  this.recurrenceId_ = ev['recurrenceId'] || null;

  this.attendeesAlerts_ = ev['alerts'];

  if (ev['occurrences']) {
    this.occurrences_ = ev['occurrences'];
  }
  this.tags_ = new goog.structs.Map();
  if (ev['tags']) {
    for (var i = 0; i < ev['tags'].length; i++) {
      var tag = bluemind.model.Tag.parse(ev['tags'][i]);
      this.addTag(tag);
    }
  }

  this.confidential_ =
    (ev['confidential'] == 'true' || ev['confidential'] == true) ? true : false;

  this.updatable_ = this.updatable_ && !this.confidential_;

};

/**
 * event id.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.id_;

/**
 * event user create.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Event.prototype.usercreate_;

/**
 * event user update.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Event.prototype.userupdate_;

/**
 * event time create.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.timecreate_;

/**
 * event time update.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.timeupdate_;

/**
 * event ext-id.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.extId_;

/**
 * event date.
 * @type {bluemind.date.DateTime}
 * @private
 */
bluemind.calendar.model.Event.prototype.date_;

/**
 * event initial date.
 * @type {bluemind.date.DateTime}
 * @private
 */
bluemind.calendar.model.Event.prototype.initialDate_;

/**
 * event end date.
 * @type {bluemind.date.DateTime}
 * @private
 */
bluemind.calendar.model.Event.prototype.end_;

/**
 * event day.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.day_;


/**
 * event duration.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.duration_;

/**
 * event initial duration.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.initialDuration_;

/**
 * event initial title.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Event.prototype.initialTitle_;

/**
 * event duration.
 * @type {string}
 * @private
 */
bluemind.calendar.model.Event.prototype.title_;

/**
 * event owner.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.ownerId_;

/**
 * event owner display name.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Event.prototype.ownerDisplayName_;


/**
 * event owner calendar id.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.ownerCalendarId_;

/**
 * event participation
 * @type {bluemind.calendar.model.AttendeeParticipation}
 * @private
 */
bluemind.calendar.model.Event.prototype.participation_;

/**
 * event allday flag.
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.allday_;

/**
 * event repeat kind.
 * @type {string}
 * @private
 */
bluemind.calendar.model.Event.prototype.repeatKind_;

/**
 * event repeat kind.
 * @type {?goog.date.Date}
 * @private
 */
bluemind.calendar.model.Event.prototype.repeatEnd_;

/**
 * event repeat freq.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.repeatFreq_;

/**
 * event repeat days.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.repeatDays_;


/**
 * event location.
 * @type {string}
 * @private
 */
bluemind.calendar.model.Event.prototype.location_;

/**
 * event desc.
 * @type {string}
 * @private
 */
bluemind.calendar.model.Event.prototype.desc_;

/**
 * is event opacity (free or busy).
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.opacity_;

/**
 * is event private.
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.private_;

/**
 * is event private.
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.confidential_;

/**
 * is user attend to event.
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.isAttend_;

/**
 * event alert.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.alert_;

/**
 * event attendees alerts.
 * @type {Array}
 * @private
 */
bluemind.calendar.model.Event.prototype.attendeesAlerts_;

/**
 * event attendees list.
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.model.Event.prototype.attendees_;


/**
 * event dom element.
 * @type {Element}
 * @private
 */
bluemind.calendar.model.Event.prototype.element_;

/**
 * event left extension.
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.left_;

/**
 * event right extension.
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.right_;

/**
 * event exception date.
 * @type {Array}
 * @private
 */
bluemind.calendar.model.Event.prototype.exceptions_;

/**
 * event exception event extID.
 * @type {Array}
 * @private
 */
bluemind.calendar.model.Event.prototype.exceptionExtIds_;

/**
 * event recurrenceId
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.recurrenceId_;

/**
 * event allday size.
 * @type {number}
 */
bluemind.calendar.model.Event.prototype.alldaySize;

/**
 * event start day, for ui.
 * @type {number}
 */
bluemind.calendar.model.Event.prototype.startDay;

/**
 * event start week, for ui.
 * @type {number}
 */
bluemind.calendar.model.Event.prototype.startWeek;

/**
 * is event is multiweek, for ui
 * @type {number}
 */
bluemind.calendar.model.Event.prototype.multiweeks;

/**
 * event calendar.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.calendar_;

/**
 * event calendar label.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Event.prototype.calendarLabel_;

/**
 * event class.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Event.prototype.klass_;

/**
 * Event handler.
 * @type {goog.events.EventHandler}
 * @private
 */
bluemind.calendar.model.Event.prototype.handler_;

/**
 * Resize handler.
 * @type {bluemind.fx.HeightResizer}
 * @private
 */
bluemind.calendar.model.Event.prototype.resize_;

/**
 * Drag handler.
 * @type {bluemind.fx.Dragger}
 * @private
 */
bluemind.calendar.model.Event.prototype.drag_;

/**
 * Scroll handler
 * @type {goog.fx.DragScrollSupport}
 * @private
 */
bluemind.calendar.model.Event.prototype.scroll_;

/**
 * fake event object, to draw a multi week event
 * @type {Array}
 * @private
 */
bluemind.calendar.model.Event.prototype.children_;

/**
 * Real event used to create this fake event
 * @type {bluemind.calendar.model.Event}
 * @private
 */
bluemind.calendar.model.Event.prototype.parent_;

/**
 * evt occurrences
 * @type {Array}
 * @private
 */
bluemind.calendar.model.Event.prototype.occurrences_;

/**
 * evt is updatable
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Event.prototype.updatable_;

/**
 * evt tags
 * @type {Array}
 * @private
 */
bluemind.calendar.model.Event.prototype.tags_;


/**
 * @return {number} event id.
 */
bluemind.calendar.model.Event.prototype.getId = function() {
  return this.id_;
};

/**
 * @param {number} id event id.
 */
bluemind.calendar.model.Event.prototype.setId = function(id) {
  this.id_ = id;
};

/**
 * @return {number} event user create.
 */
bluemind.calendar.model.Event.prototype.getUserCreate = function() {
  return this.usercreate_;
};

/**
 * @return {number} event user update.
 */
bluemind.calendar.model.Event.prototype.getUserUpdate = function() {
  return this.userupdate_;
};

/**
 * @return {number} event time create.
 */
bluemind.calendar.model.Event.prototype.getTimeCreate = function() {
  return this.timecreate_;
};

/**
 * @return {number} event time update.
 */
bluemind.calendar.model.Event.prototype.getTimeUpdate = function() {
  return this.timeupdate_;
};

/**
 * @return {number} event ext id.
 */
bluemind.calendar.model.Event.prototype.getExtId = function() {
  return this.extId_;
};

/**
 * @param {number} extId event ext id.
 */
bluemind.calendar.model.Event.prototype.setExtId = function(extId) {
  this.extId_ = extId;
};

/**
 * @return {number} event day of week.
 */
bluemind.calendar.model.Event.prototype.getDay = function() {
  return this.day_;
};

/**
 * @return {bluemind.date.DateTime} event date.
 */
bluemind.calendar.model.Event.prototype.getDate = function() {
  return this.date_;
};

/**
 * @return {bluemind.date.DateTime} event initial date.
 */
bluemind.calendar.model.Event.prototype.getInitialDate = function() {
  return this.initialDate_;
};

/**
 * set event initial date.
 * @param {bluemind.date.DateTime} d occurrence date begin.
 */
bluemind.calendar.model.Event.prototype.setInitialDate = function(d) {
  this.initialDate_ = d;
};

/**
 * set event date
 * @param {bluemind.date.DateTime} d event date.
 */
bluemind.calendar.model.Event.prototype.setDate = function(d) {
  if (this.getParent()) {
    this.getParent().setDate(d);
  }
  this.date_ = d;
  this.date_.setMilliseconds(0);
  this.day_ = d.getDay();
  this.startDay = d.getDay();
  this.startWeek = this.date_.getWeekNumber();
};

/**
 * @return {bluemind.date.DateTime} event end date.
 */
bluemind.calendar.model.Event.prototype.getEnd = function() {
  return this.end_;
};

/**
 * @param {bluemind.date.DateTime} e event end date.
 */
bluemind.calendar.model.Event.prototype.setEnd = function(e) {
  if (this.getParent()) {
    this.getParent().setEnd(e);
  }
  this.end_ = e;
};

/**
 * @return {number} event duration.
 */
bluemind.calendar.model.Event.prototype.getDuration = function() {
  return this.duration_;
};

/**
 * @return {number} event reminder.
 */
bluemind.calendar.model.Event.prototype.getAlert = function() {
  return this.alert_;
};

/**
 * @param {number} time event reminder.
 */
bluemind.calendar.model.Event.prototype.setAlert = function(time) {
  this.alert_ = time;
};

/**
 * @return {Array} event attendees alerts.
 */
bluemind.calendar.model.Event.prototype.getAttendeesAlerts = function() {
  return this.attendeesAlerts_;
};

/**
 * @param {Array} alerts attendees alerts.
 */
bluemind.calendar.model.Event.prototype.setAttendeesAlerts = function(alerts) {
  this.attendeesAlerts_ = alerts;
};

/**
 * @return {Array} event attendees.
 */
bluemind.calendar.model.Event.prototype.getAttendees = function() {
  return this.attendees_;
};

/**
 * @param {Array} attendees Set event attendees.
 */
bluemind.calendar.model.Event.prototype.setAttendees = function(attendees) {
  this.attendees_ = attendees;
};

/**
 * @param {number} duration event duration.
 */
bluemind.calendar.model.Event.prototype.setDuration = function(duration) {
  this.duration_ = duration;
};

/**
 * @return {number} event duration.
 */
bluemind.calendar.model.Event.prototype.getInitialDuration = function() {
  return this.initialDuration_;
};

/**
 * @return {number} event title.
 */
bluemind.calendar.model.Event.prototype.getInitialTitle = function() {
  return this.initialTitle_;
};

/**
 * @return {string} event title.
 */
bluemind.calendar.model.Event.prototype.getTitle = function() {
  return this.title_;
};

/**
 * @param {string} title Event title.
 */
bluemind.calendar.model.Event.prototype.setTitle = function(title) {
  this.title_ = title;
};

/**
 * @return {number} event owner id.
 */
bluemind.calendar.model.Event.prototype.getOwnerId = function() {
  return this.ownerId_;
};

/**
 * @param {number} ownerId event owner id.
 */
bluemind.calendar.model.Event.prototype.setOwnerId = function(ownerId) {
  this.ownerId_ = ownerId;
};

/**
 * @return {text} event owner display name.
 */
bluemind.calendar.model.Event.prototype.getOwnerDisplayName = function() {
  return this.ownerDisplayName_;
};

/**
 * @param {text} owner event owner display name.
 */
bluemind.calendar.model.Event.prototype.setOwnerDisplayName = function(owner) {
  this.ownerDisplayName_ = owner;
};

/**
 * @return {number} event owner calendar id.
 */
bluemind.calendar.model.Event.prototype.getOwnerCalendarId = function() {
  return this.ownerCalendarId_;
};

/**
 * @param {number} cid event owner calendar id.
 */
bluemind.calendar.model.Event.prototype.setOwnerCalendarId = function(cid) {
  this.ownerCalendarId_ = cid;
};

/**
 * @return {boolean} event allday flag.
 */
bluemind.calendar.model.Event.prototype.isAllday = function() {
  return this.allday_;
};

/**
 * @param {boolean} allday event allday flag.
 */
bluemind.calendar.model.Event.prototype.setAllday = function(allday) {
  this.allday_ = allday;
};

/**
 * @return {text} event location.
 */
bluemind.calendar.model.Event.prototype.getLocation = function() {
  return this.location_;
};

/**
 * @param {text} place event location.
 */
bluemind.calendar.model.Event.prototype.setLocation = function(place) {
  this.location_ = place;
};

/**
 * @return {text} event desc.
 */
bluemind.calendar.model.Event.prototype.getDescription = function() {
  return this.desc_;
};

/**
 * @param {text} description event desc.
 */
bluemind.calendar.model.Event.prototype.setDescription =
  function(description) {
  this.desc_ = bluemind.html.sanitize(description);
};

/**
 * @return {Array} event occurrences.
 */
bluemind.calendar.model.Event.prototype.getOccurrences = function() {
  return this.occurrences_;
};

/**
 * @param {Array} occurrences event occurrences.
 */
bluemind.calendar.model.Event.prototype.setOccurrences =
  function(occurrences) {
  this.occurrences_ = occurrences;
};

/**
 * Return event opacity
 * @return {string} opacity.
 */
bluemind.calendar.model.Event.prototype.getOpacity = function() {
  return (this.opacity_ ? 'OPAQUE' : 'TRANSPARENT');
};

/**
 * Set event opacity
 * @param {string} opacity Event opacity.
 */
bluemind.calendar.model.Event.prototype.setOpacity = function(opacity) {
  this.opacity_ = (opacity == 'OPAQUE');
};

/**
 * Return event privacy
 * @return {boolean} privacy.
 */
bluemind.calendar.model.Event.prototype.isPrivate = function() {
  return this.private_;
};

/**
 * Set event privacy
 * @param {boolean} privacy Event privacy state.
 */
bluemind.calendar.model.Event.prototype.setPrivate = function(privacy) {
  this.private_ = privacy;
};

/**
 * Return event confidential
 * @return {boolean} confidential.
 */
bluemind.calendar.model.Event.prototype.isConfidential = function() {
  return this.confidential_;
};

/**
 * Set event confidential
 * @param {boolean} confidential Event confidential state.
 */
bluemind.calendar.model.Event.prototype.setConfidential = function(confidential) {
  this.confidential_ = confidential;
};

/**
 * Return user attend to event
 * @return {boolean} attendee.
 */
bluemind.calendar.model.Event.prototype.isAttend = function() {
  return this.isAttend_;
};

/**
 * Set user attend to event
 * @param {boolean} attendee set is user attend to event.
 */
bluemind.calendar.model.Event.prototype.setIsAttend = function(attend) {
  this.isAttend_ = attend;
};

/**
 * @return {number} event class.
 */
bluemind.calendar.model.Event.prototype.getKlass = function() {
  return this.klass_;
};

/**
 * @param {number} klass event class.
 */
bluemind.calendar.model.Event.prototype.setKlass = function(klass) {
  this.klass_ = klass;
};

/**
 * @return {text} event repeat kind.
 */
bluemind.calendar.model.Event.prototype.getRepeatKind = function() {
  return this.repeatKind_;
};

/**
 * @param {text} kind event repeat kind.
 */
bluemind.calendar.model.Event.prototype.setRepeatKind = function(kind) {
  this.repeatKind_ = kind;
};

/**
 * @return {bluemind.date.DateTime} event repeat end.
 */
bluemind.calendar.model.Event.prototype.getRepeatEnd = function() {
  return this.repeatEnd_;
};

/**
 * @param {bluemind.date.DateTime} date event repeat end.
 */
bluemind.calendar.model.Event.prototype.setRepeatEnd = function(date) {
  this.repeatEnd_ = date;
};

/**
 * @return {number} event repeat freq.
 */
bluemind.calendar.model.Event.prototype.getRepeatFreq = function() {
  return this.repeatFreq_;
};

/**
 * @param {number} freq event repeat freq.
 */
bluemind.calendar.model.Event.prototype.setRepeatFreq = function(freq) {
  this.repeatFreq_ = freq;
};

/**
 * @return {number} event repeat days.
 */
bluemind.calendar.model.Event.prototype.getRepeatDays = function() {
  return this.repeatDays_;
};

/**
 * @param {number} days event repeat days.
 */
bluemind.calendar.model.Event.prototype.setRepeatDays = function(days) {
  this.repeatDays_ = days;
};

/**
 * @return {number} event recurrence id.
 */
bluemind.calendar.model.Event.prototype.getRecurrenceId = function() {
  return this.recurrenceId_;
};

/**
 * @param {number} recId event recurrence id.
 */
bluemind.calendar.model.Event.prototype.setRecurrenceId = function(recId) {
  this.recurrenceId_ = recId;
};

/**
 * @return {bolean} is event updatable.
 */
bluemind.calendar.model.Event.prototype.isUpdatable = function() {
  return this.updatable_;
};

/**
 * @return {goog.dom} event dom.
 */
bluemind.calendar.model.Event.prototype.getElement = function() {
  return this.element_;
};

/**
 * @return {bluemind.calendar.model.Event} event parent.
 */
bluemind.calendar.model.Event.prototype.getParent = function() {
  return this.parent_;
};

/**
 * @param {bluemind.calendar.model.Event} p parent.
 */
bluemind.calendar.model.Event.prototype.setParent = function(p) {
  this.parent_ = p;
};

/**
 * Set participation
 * @param {text} p participation.
 */
bluemind.calendar.model.Event.prototype.setParticipation = function(p) {
  this.participation_ = p;
};

/**
 * get participation
 * @return {text} p participation.
 */
bluemind.calendar.model.Event.prototype.getParticipation = function() {
  return this.participation_;
};

/**
 * Store initial date and duration.
 */
bluemind.calendar.model.Event.prototype.setInitialValues = function() {
  this.initialTitle_ = this.title_;
  this.initialDuration_ = this.duration_;
  this.initialDate_ = goog.object.clone(this.date_);
};

/**
 * Restore initial date and duration.
 */
bluemind.calendar.model.Event.prototype.restoreInitialValues = function() {
  this.title_ = this.initialTitle_;
  this.duration_ = this.initialDuration_;
  this.setDate(this.initialDate_.clone());
  var date = this.initialDate_.clone();
  date.add(new goog.date.Interval(0, 0, 0, 0, 0, this.duration_));
  this.setEnd(date);
  this.updateTitleDom();
};

/**
 * Add a new fake event clone
 * @return {bluemind.calendar.model.Event} event clone.
 */
bluemind.calendar.model.Event.prototype.giveBirth = function() {
  var child = goog.object.clone(this);
  child.element_ = null;
  child.drag_ = null;
  child.resize_ = null;
  child.scroll_ = null;
  child.handler_ = null;
  goog.removeUid(child);
  child.emancipate();
  child.setParent(this);
  this.children_.push(child);
  return child;
};

/**
 * Remove all child without destroying
 */
bluemind.calendar.model.Event.prototype.emancipate = function() {
  this.children_ = new Array();
};

/**
 * Destroy and remove all event childs
 * @param {number} opt_heir Optional uid of the unique surviving child.
 */
bluemind.calendar.model.Event.prototype.infanticide = function(opt_heir) {
  var heir = null;
  for (var i = 0; i < this.children_.length; i++) {
    var child = this.children_[i];
    if (goog.getUid(child) == opt_heir) {
      heir = child;
    } else {
      child.dispose();
      delete child;
    }
  }
  this.children_ = new Array();
  if (heir != null) {
    this.children_.push(heir);
  }
};

/**
 * Destroy and remove all event childs
 */
bluemind.calendar.model.Event.prototype.fratricide = function() {
  if (this.getParent()) {
    this.getParent().infanticide(goog.getUid(this));
  }
};
/**
 * Returns the lazily created event handler
 * @return {!goog.events.EventHandler} Event handler for this component.
 * @protected
 */
bluemind.calendar.model.Event.prototype.getHandler = function() {
  return this.handler_ ||
         (this.handler_ = new goog.events.EventHandler(this));
};

/**
 * Return the calendar containing this event
 * @return {number} calendar.
 */
bluemind.calendar.model.Event.prototype.getCalendar = function() {
  return this.calendar_;
};

/**
 * @param {number} calendar calendar id.
 */
bluemind.calendar.model.Event.prototype.setCalendar = function(calendar) {
  this.calendar_ = calendar;
};

/**
 * Return the calendar label containing this event
 * @return {text} calendar.
 */
bluemind.calendar.model.Event.prototype.getCalendarLabel = function() {
  return this.calendarLabel_;
};

/**
 * Return exceptions date
 * @param {bluemind.date.DateTime} d exception date.
 */
bluemind.calendar.model.Event.prototype.addExceptionDate = function(d) {
  goog.array.insert(this.exceptions_, d.getTime());
};

/**
 * Return the calendar label containing this event
 * @param {Array} e exception date.
 */
bluemind.calendar.model.Event.prototype.setExceptionDate = function(e) {
  this.exceptions_ = e;
};

/**
 * Return the calendar label containing this event
 * @return {Array} exception date.
 */
bluemind.calendar.model.Event.prototype.getExceptionDate = function() {
  return this.exceptions_;
};

/**
 * Return event exception extid
 * @param {Text} extId exception extId.
 */
bluemind.calendar.model.Event.prototype.addExceptionExtId = function(extId) {
  goog.array.insert(this.exceptionExtIds_, extId);
};

/**
 * Return the calendar label containing this event
 * @param {Array} e exception extIds.
 */
bluemind.calendar.model.Event.prototype.setExceptionExtIds = function(e) {
  this.exceptionExtIds_ = e;
};

/**
 * Return the calendar tag list
 * @return {Array} exception date.
 */
bluemind.calendar.model.Event.prototype.getTags = function() {
  return this.tags_.getValues();
};


/**
 * Add event tag
 * @param {bluemind.model.Tag} t tag.
 */
bluemind.calendar.model.Event.prototype.addTag = function(t) {
  var tag = bluemind.manager.getTag(t.getLabel());
  if (tag != null) {
    this.tags_.set(tag.getLabel(), tag);
  } else {
    this.tags_.set(t.getLabel(), t);
  }
};

/**
 * Return the calendar label containing this event
 * @param {Array} tags tags.
 */
bluemind.calendar.model.Event.prototype.setTags = function(tags) {
  this.tags_.clear();
  for(var i = 0; i < tags.length; i++) {
    this.addTag(tags[i]);
  }
};

/**
 * create event dom
 * @param {number} unit unit.
 * @param {number} position pos.
 * @param {number} size size.
 */
bluemind.calendar.model.Event.prototype.createDom =
  function(unit, position, size) {
  var container = goog.dom.getElement('d' + this.startDay);
  var title = this.getHeaderText_();

  var pending = '';
  if (this.participation_ ==
      bluemind.calendar.model.AttendeeParticipation.NEEDSACTION) {
    pending = goog.getCssName('pending');
  }

  var declined = '';
  if (this.participation_ ==
    bluemind.calendar.model.AttendeeParticipation.DECLINED) {
    declined = goog.getCssName('declined');
  }

  var updatable = '';
  if (this.updatable_) updatable = goog.getCssName('updatable');

  var dim = (goog.date.Date.compare(this.end_,
      new bluemind.date.DateTime()) < 0);
  var opacity = this.opacity_ ?
    goog.getCssName('opaque') : goog.getCssName('transparent');
  var bevel = (this.duration_ < 3600) ? goog.getCssName('no-bevel') : '';

  var klass = bluemind.fixCssName(this.klass_, dim) +
    ' ' + goog.getCssName('event') +
    ' ' + goog.getCssName('inDayEvent') + ' ' + pending + ' ' + declined +
    ' ' + updatable + ' ' + bevel + ' ' + opacity;

  var tooltip = this.title_;
  if (this.location_ != null && this.location_ != '') {
    tooltip += ', ' + this.location_;
  }
  var tags = [];
  goog.iter.forEach(this.tags_, function(tag) {
    if (bluemind.manager.isTagVisible(tag)) {
      tags.push(tag.serialize());
    }
  }, this);

  var data = {
    id: 'event_' + goog.getUid(this),
    tooltip: tooltip,
    date: title,
    title: this.getContentText_(),
    repeat: (this.repeatKind_ != 'none'),
    klass: klass,
    left: this.left_,
    right: this.right_,
    opaque: this.opacity_ ? 'opaque' : 'transparent',
    updatable: this.updatable_,
    isPrivate: this.private_,
    isAttend: this.isAttend_,
    isMeeting: (this.attendees_.length > 1),
    duration: this.duration_,
    location: this.location_ || '',
    tags: tags};
  this.element_ = soy.renderAsFragment(bluemind.calendar.template.event, data);
  this.updateDraw(unit, position, size);
  goog.dom.appendChild(container, this.element_);
  if (this.updatable_) {
    this.addResizeHandlers_();
    this.addDragHandlers_();
  } else {
    this.addReadOnlyListener_();
  }
};


/**
 * Add resize handler on event component
 * @private
 */
bluemind.calendar.model.Event.prototype.addResizeHandlers_ = function() {
  this.resize_ = new bluemind.fx.HeightResizer(this.element_,
    goog.dom.getElementByClass(goog.getCssName('ev-resizer'), this.element_));
  this.resize_.setHysteresis(2);
  goog.object.set(this.resize_, 'scroll', null);
  this.getHandler().listen(
    goog.dom.getElementByClass(goog.getCssName('ev-resizer'), this.element_),
    goog.events.EventType.MOUSEDOWN, function(e) { e.stopPropagation(); });
  this.resize_.setScrollTarget(goog.dom.getElement('gridContainer'));
  this.getHandler().listen(this.resize_,
    bluemind.fx.Dragger.EventType.BEFORE_START,
    this.setResizeLimit_, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.START,
    this.setActiveState, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.DRAG,
    this.updateDateAndDuration_, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.END,
    this.updateComplete_, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.EARLY_CANCEL,
    bluemind.calendar.Controller.getInstance().updateEventBubble, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.EARLY_CANCEL,
    function(e) {goog.dispose(this.scroll_);}, false, this);
};

/**
 * Add drag handler on event component
 * @private
 */
bluemind.calendar.model.Event.prototype.addDragHandlers_ = function() {
  this.drag_ = new bluemind.fx.Dragger(this.element_);
  this.drag_.setHysteresis(2);
  goog.object.set(this.drag_, 'scroll', null);
  this.getHandler().listen(this.element_, goog.events.EventType.MOUSEDOWN,
    function(e) {e.stopPropagation();});
  this.drag_.setScrollTarget(goog.dom.getElement('gridContainer'));
  this.getHandler().listen(this.drag_,
    bluemind.fx.Dragger.EventType.BEFORE_START,
    this.setDragLimit_, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.START,
    this.setActiveState, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.DRAG,
    this.updateDate_, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.END,
    this.updateComplete_, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.EARLY_CANCEL,
    bluemind.calendar.Controller.getInstance().updateEventBubble, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.EARLY_CANCEL,
    function(e) {goog.dispose(this.scroll_);}, false, this);
};

/**
 * Set limit and grid for resizing fx
 * @private
 */
bluemind.calendar.model.Event.prototype.setResizeLimit_ = function() {
  this.resize_.setGrid(new goog.math.Size(1, 21));
  var top = this.getElement().offsetTop;
  this.resize_.setLimits(new goog.math.Rect(0, top, 0, 1008 - top));
  this.scroll_ =
    new goog.fx.DragScrollSupport(goog.dom.getElement('gridContainer'), 10);
};

/**
 * Set limit and grid for resizing fx
 * @private
 */
bluemind.calendar.model.Event.prototype.setDragLimit_ = function() {
  //TODO : Should be determined by the view
  // getContainerBounds for exemple
  var box = goog.style.getSize(this.element_);
  var container = goog.dom.getElementByClass(goog.getCssName('dayContainer'),
      goog.dom.getElement('bodyContainer'));
  var margin = goog.style.getSize(goog.dom.getElement('leftPanelHour')).width;
  var size = goog.style.getSize(container);
  var position = goog.style.getRelativePosition(container, this.element_);
  var width = goog.style.getSize(goog.dom.getElement('bodyContent')).width -
    margin - size.width;
  this.drag_.setGrid(new goog.math.Size(size.width, 21));
  this.drag_.setLimits(
    new goog.math.Rect(position.x + this.element_.offsetLeft - 6, 0,
    width + 6, 1008 - box.height));
  this.scroll_ =
    new goog.fx.DragScrollSupport(goog.dom.getElement('gridContainer'), 10);

};

/**
 * Set style for currently updated event.
 * TODO: Find a better name ^^
 * @param {goog.fx.DragEvent} opt_e If called by an event.
 */
bluemind.calendar.model.Event.prototype.setActiveState = function(opt_e) {
  var width = goog.style.getSize(
    goog.dom.getAncestorByClass(
      this.element_, goog.getCssName('dayContainer'))).width - 2;
  this.fratricide();
  this.element_.style.width = width + 'px';
  this.element_.style.zIndex = '1000';
  this.element_.style.left = '0';
  goog.style.setOpacity(this.element_, .7);
  if (opt_e) {
    opt_e.dragger.resetDrag(opt_e);
  }
};

/**
 * Refresh event title dom
 */
bluemind.calendar.model.Event.prototype.updateTitleDom = function() {
  var header = goog.dom.getElementByClass(goog.getCssName('ev-header'),
    this.element_);
  var content = goog.dom.getElementByClass(goog.getCssName('ev-content'),
    this.element_);
  goog.dom.setTextContent(header, this.getHeaderText_());
  if (content) {
    goog.dom.setTextContent(content, this.getContentText_());
  }
};

/**
 * Calculate header text
 * @private
 * @return {string}  header text.
 */
bluemind.calendar.model.Event.prototype.getHeaderText_ = function() {
  var timeformat = bluemind.i18n.DateTimeHelper.getInstance();
  var start = timeformat.formatTime(this.date_);
  var end = '';
  if (this.duration_ >= 3600) {
    end = timeformat.formatTime(this.end_);
  } else {
    if (!this.private_ ||
      this.private_ && this.updatable_ && this.isAttend_) {
      end = this.title_;
    }
  }
  return start + ' - ' + end;
};

/**
 * Calculate title text
 * @return {string} content text.
 * @private
 */
bluemind.calendar.model.Event.prototype.getContentText_ = function() {
  if (this.duration_ >= 3600) {
    return this.title_;
  }
  return '';
};

/**
 * Redraw view after a event was moved
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
bluemind.calendar.model.Event.prototype.updateComplete_ = function(e) {
  goog.style.setOpacity(this.element_, 1);
  this.element_.style.zIndex = '';
  goog.dispose(this.scroll_);
  bluemind.calendar.Controller.getInstance().quickUpdateEvent(this);
};

/**
 * Update event dom
 * @param {number} unit unit.
 * @param {number} position pos.
 * @param {number} size size.
 */
bluemind.calendar.model.Event.prototype.updateDraw =
  function(unit, position, size) {
  var topPosition = this.date_.getHours() + this.date_.getMinutes() / 60;
  this.element_.style.cssText = 'left: ' + 100 / unit * position + '%;' +
    'top: ' + (topPosition * 42) + 'px;' +
    'width: ' + (100 / unit * size) + '%;' +
    'height: ' + ((this.duration_ / 3600 * 42) - 1) + 'px';
};

/**
 * create all day event dom
 * @param {number} idx row index.
 */
bluemind.calendar.model.Event.prototype.createAlldayDom = function(idx) {
  var trId = 'weekBgTr_' + this.startWeek;
  var container =
    goog.dom.getElement('dayEventContainer_' +
      this.startWeek + '_' + this.startDay + '_' + idx);
  if (container) {
    goog.dom.setProperties(container, {'colspan': this.alldaySize});
    var tr = goog.dom.getElement(trId + '_' + idx);
    for (var i = 0; i < (this.alldaySize - 1); i++) {
      var td = goog.dom.getNextElementSibling(container);
      if (goog.dom.contains(tr, td)) {
        goog.dom.removeNode(td);
      }
    }

    var title = this.title_;
    if (!this.allday_) {
      title = this.date_.toIsoTimeString(false) + ' ' + this.title_;
    }

    var pending = '';
    if (this.participation_ ==
        bluemind.calendar.model.AttendeeParticipation.NEEDSACTION) {
      pending = goog.getCssName('pending');
    }
    var declined = '';
    if (this.participation_ ==
      bluemind.calendar.model.AttendeeParticipation.DECLINED) {
      declined = goog.getCssName('declined');
    }

    var updatable = '';
    if (this.updatable_) updatable = goog.getCssName('updatable');

    var dim = (goog.date.Date.compare(this.end_,
        new bluemind.date.DateTime()) < 0);

    var klass;
    var ico = '';
    if (bluemind.view.getView().getName() != 'month' ||
      (bluemind.view.getView().getName() == 'month' && this.allday_) ||
      (bluemind.view.getView().getName() == 'month' &&
        !this.allday_ && this.alldaySize > 1)) {
      klass = bluemind.fixCssName(this.klass_, dim);
    } else {
      klass = bluemind.fixMonthCssName(this.klass_, dim);
      ico = 'dark';
    }

    klass += ' ' + goog.getCssName('event') +
      ' ' + goog.getCssName('allDayEvent') + ' ' + pending +
      ' ' + declined + ' ' + updatable;

    var tooltip = this.title_;
    if (this.location_ != null && this.location_ != '') {
      tooltip += ', ' + this.location_;
    }
    var tags = [];
    goog.iter.forEach(this.tags_, function(tag) {
      if (bluemind.manager.isTagVisible(tag)) {
        tags.push(tag.serialize());
      }
    }, this);

    var data = {
      id: 'event_' + goog.getUid(this),
      date: '',
      tooltip: tooltip,
      title: title,
      ico: ico,
      repeat: (this.repeatKind_ != 'none'),
      klass: klass,
      left: this.left_,
      right: this.right_,
      opaque: this.opacity_ ? 'opaque' : 'transparent',
      updatable: this.updatable_,
      isPrivate: this.private_,
      isAttend: this.isAttend_,
      isMeeting: (this.attendees_.length > 1),
      location: this.location_ || '',
      tags: tags};

    this.element_ = soy.renderAsFragment(
      bluemind.calendar.template.eventAllday, data);
    goog.dom.appendChild(container, this.element_);
    if (this.updatable_) {
      this.addAlldayDragHandlers_();
    } else {
      this.addReadOnlyListener_();
    }
  }
};

/**
 * Add drag handler on all day event component
 * @private
 */
bluemind.calendar.model.Event.prototype.addAlldayDragHandlers_ = function() {
  if (bluemind.view.getView().getName() == 'month') {
    this.drag_ = new bluemind.fx.Dragger(this.element_);
  } else {
    this.drag_ = new bluemind.fx.HorizontalDragger(this.element_);
  }
  this.drag_.setHysteresis(2);
  this.getHandler().listen(this.element_, goog.events.EventType.MOUSEDOWN,
    function(e) {e.stopPropagation();});
  this.getHandler().listen(this.element_, goog.events.EventType.CLICK,
    function(e) {e.stopPropagation();});
  this.getHandler().listen(this.drag_,
    bluemind.fx.Dragger.EventType.BEFORE_START,
    this.setAlldayDragLimit_, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.START,
    this.setAlldayActiveState, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.DRAG,
    this.updateAllDayDate_, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.END,
    this.updateComplete_, false, this);
  this.getHandler().listen(this.drag_, goog.fx.Dragger.EventType.EARLY_CANCEL,
    bluemind.calendar.Controller.getInstance().updateEventBubble, false, this);
};

/**
 * Add readonly event listener.
 * @private
 */
bluemind.calendar.model.Event.prototype.addReadOnlyListener_ = function() {
  this.getHandler().listen(this.element_, goog.events.EventType.MOUSEDOWN,
    function(e) {e.stopPropagation();});
  this.getHandler().listen(this.element_,
    goog.events.EventType.CLICK,
    bluemind.calendar.Controller.getInstance().consultEvent, false, this);
};

/**
 * Set limit and grid for resizing fx
 * @private
 */
bluemind.calendar.model.Event.prototype.setAlldayDragLimit_ = function() {
  //TODO : Should be determined by the view
  // getContainerBounds for exemple
  if (bluemind.view.getView().getName() == 'month') {
    var container = goog.dom.getElement('gridContainer');
    var row = goog.dom.getAncestorByClass(this.element_,
      goog.getCssName('month-row'));
    var height = goog.style.getSize(row).height;
    var width = goog.style.getSize(
      goog.dom.getElementByClass(goog.getCssName('mg-daynum'))).width + 1;
  } else {
   var height = 1;
   var container = goog.dom.getElement('weekBg');
   var width = goog.style.getSize(goog.dom.getFirstElementChild(
    goog.dom.getLastElementChild(container))).width;
  }
  var box = goog.style.getSize(this.element_);
  var size = goog.style.getSize(container);
  var position = goog.style.getRelativePosition(container, this.element_);
  this.drag_.setGrid(new goog.math.Size(width, height));
  this.drag_.setLimits(
    new goog.math.Rect(((position.x + this.element_.offsetLeft) - 6),
    (position.y + this.element_.offsetTop),
    ((size.width - width) + 6), (size.height - box.height)));

};

/**
 * Set style for currently updated event.
 * @param {goog.events.Event} e Drage Event.
 * TODO: Find a better name ^^.
 */
bluemind.calendar.model.Event.prototype.setAlldayActiveState = function(e) {
  var width;
  if (bluemind.view.getView().getName() == 'month') {
    width = goog.style.getSize(
      goog.dom.getElementByClass(goog.getCssName('mg-daynum'))).width;
  } else {
    width = goog.style.getSize(goog.dom.getFirstElementChild(
    goog.dom.getLastElementChild(goog.dom.getElement('weekBg')))).width;
  }
  this.fratricide();
  this.element_.style.width = (width - 8) + 'px';
  this.element_.style.zIndex = '1000';
  var client = goog.style.getClientPosition(this.element_);
  var left = (e.clientX - client.x);
  left -= (e.clientX - client.x) % width;
  this.element_.style.left = left + 'px';
  goog.style.setOpacity(this.element_, .7);
  if (this.getParent()) {
    this.getParent().multiweeks = false;
    this.getParent().right_ = false;
    this.getParent().left_ = false;
  }
  this.multiweeks = false;
  this.left_ = false;
  this.right_ = false;
  e.dragger.resetDrag(e);
};

/**
 * Update event date from position
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
bluemind.calendar.model.Event.prototype.updateAllDayDate_ = function(e) {
  var position = goog.style.getPageOffset(this.element_);
  var date = new bluemind.date.DateTime(
    bluemind.view.getView().getDateForCoordinate(position));
  date.setHours(this.date_.getHours());
  date.setMinutes(this.date_.getMinutes());
  date.setSeconds(this.date_.getSeconds());
  this.setDate(date.clone());
  date.add(new goog.date.Interval(0, 0, 0, 0, 0, this.duration_));
  this.setEnd(date);
};

/**
 * Update event date and duration from position and height
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
bluemind.calendar.model.Event.prototype.updateDateAndDuration_ = function(e) {
  var position = goog.style.getPageOffset(this.element_);
  var size = goog.style.getSize(this.element_);
  var start = bluemind.view.getView().getDateTimeForCoordinate(position);
  position.y += size.height + 1;
  var end = bluemind.view.getView().getDateTimeForCoordinate(position);
  this.setEnd(end);
  this.setDate(start);
  this.duration_ = (this.end_ - this.date_) / 1000;
  this.updateTitleDom();
};

/**
 * Update event date and duration from position and height
 * @param {goog.fx.DragEvent} e The event.
 * @private
 */
bluemind.calendar.model.Event.prototype.updateDate_ = function(e) {
  var position = goog.style.getPageOffset(this.element_);
  var size = goog.style.getSize(this.element_);
  var start = bluemind.view.getView().getDateTimeForCoordinate(position);
  position.y += size.height + 1;
  var end = bluemind.view.getView().getDateTimeForCoordinate(position);
  this.setEnd(end);
  this.setDate(start);
  this.updateTitleDom();
};

/**
 * Add left arrow.
 */
bluemind.calendar.model.Event.prototype.addLeftExtension = function() {
  this.left_ = true;
};

/**
 * Add right arrow.
 */
bluemind.calendar.model.Event.prototype.addRightExtension = function() {
  this.right_ = true;
};

/**
 * Dispose event
 */
bluemind.calendar.model.Event.prototype.dispose = function() {
  for (var i = 0; i < this.children_.length; i++) {
    this.children_[i].dispose();
  }
  if (this.handler_) {
    this.handler_.removeAll();
    this.handler_ = null;
  }
  if (this.drag_) {
    this.drag_.dispose();
    this.drag_ = null;
  }
  if (this.scroll_) {
    this.scroll_.dispose();
    this.scroll_ = null;
  }
  if (this.resize_) {
    this.resize_.dispose();
    this.resize_ = null;
  }
  goog.dom.removeNode(this.element_);
  this.element_ = null;
};


/**
 * create event dom during creation process
 * @param {number} unit unit.
 * @param {number} position pos.
 * @param {number} size size.
 */
bluemind.calendar.model.Event.prototype.createDummyDom =
  function(unit, position, size) {
  var container = goog.dom.getElement('d' + this.startDay);
  var title = this.getHeaderText_();
  var bevel = (this.duration_ < 3600) ? goog.getCssName('no-bevel') : '';

  var klass = bluemind.fixCssName(this.klass_, false) +
    ' ' + goog.getCssName('event') +
    ' ' + goog.getCssName('inDayEvent') + ' ' + bevel;
  var data = {
    id: 'event_' + goog.getUid(this),
    date: title,
    title: this.getContentText_(),
    tooltip: '',
    repeat: false,
    klass: klass,
    left: false,
    right: false,
    opaque: 'opaque',
    updatable: true,
    tags: []};
  this.element_ = soy.renderAsFragment(bluemind.calendar.template.event, data);
  this.updateDraw(unit, position, size);
  goog.dom.appendChild(container, this.element_);
  this.element_.visibility = 'hidden';
  this.addDummyResizeHandlers_();
  this.setActiveState();
};


/**
 * Add resize handler on event component during creation process
 * @private
 */
bluemind.calendar.model.Event.prototype.addDummyResizeHandlers_ = function() {
  this.resize_ = new bluemind.fx.HeightResizer(this.element_,
    goog.dom.getElementByClass(goog.getCssName('ev-resizer'), this.element_));
  this.resize_.setHysteresis(2);
  goog.object.set(this.resize_, 'scroll', null);
  this.getHandler().listen(
    goog.dom.getElementByClass(goog.getCssName('ev-resizer'), this.element_),
    goog.events.EventType.MOUSEDOWN, function(e) { e.stopPropagation(); });
  this.resize_.setScrollTarget(goog.dom.getElement('gridContainer'));
  this.getHandler().listen(this.resize_,
    bluemind.fx.Dragger.EventType.BEFORE_START,
    this.setDummyResizeLimit_, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.START,
    function(e) {
      this.element_.style.visibility = 'visible';
    }, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.DRAG,
    this.updateDateAndDuration_, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.END,
     bluemind.calendar.Controller.getInstance().createEvent, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.END,
     function(e) {goog.dispose(this.scroll_);}, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.EARLY_CANCEL,
    function(e) {
      this.duration_ = 1800;
      this.updateDraw(1, 0, 1);
      this.setActiveState();
      this.element_.style.visibility = 'visible';
      goog.dispose(this.scroll_);
    }, false, this);
  this.getHandler().listen(this.resize_, goog.fx.Dragger.EventType.EARLY_CANCEL,
    bluemind.calendar.Controller.getInstance().createEvent, false, this);
};


/**
 * Set limit and grid for resizing fx on dummy event
 * @private
 */
bluemind.calendar.model.Event.prototype.setDummyResizeLimit_ = function() {
  this.resize_.setGrid(new goog.math.Size(1, 21));
  this.resize_.setLimits(new goog.math.Rect(0, 0, 0, 1008));
  this.scroll_ =
    new goog.fx.DragScrollSupport(goog.dom.getElement('gridContainer'), 10);
};

/**
 * @return {goog.structs.Map} map representation of an event.
 */
bluemind.calendar.model.Event.prototype.toMap = function() {
  var map = new goog.structs.Map();
  map.set('id', this.id_);
  map.set('extId', this.extId_);
  map.set('title', this.title_);
  map.set('duration', this.duration_);
  map.set('location', this.location_);
  map.set('private', this.private_);
  map.set('confidential', this.confidential_);
  map.set('alert', this.alert_);
  map.set('desc', this.desc_);
  map.set('ownerId', this.ownerId_);
  map.set('ownerDN', this.ownerDisplayName_);
  map.set('date', this.date_.getTime());
  map.set('initialDate', this.initialDate_.getTime());
  map.set('opacity', (this.opacity_ ? 'OPAQUE' : 'TRANSPARENT'));
  map.set('allday', this.allday_);
  if (this.attendees_ != null) {
    var attendees = [];
    for (var i = 0; i < this.attendees_.length; i++) {
      var attendee = this.attendees_[i];
      var a = {
        'calendar': attendee['calendar'],
        'participation': attendee['participation'],
        'role': attendee['role'],
        'id': attendee['id'],
        'type': attendee['type'],
        'notified': attendee['notified'],
        'email': attendee['email'],
        'displayName': attendee['displayName']
      };
      attendees.push(a);
    }
    map.set('attendees', attendees);
  }
  map.set('alerts', this.attendeesAlerts_);

  map.set('repeatKind', this.repeatKind_);
  if (this.repeatEnd_ != null) {
    map.set('repeatEnd', this.repeatEnd_.getTime());
  } else {
    map.set('repeatEnd', null);
  }
  map.set('repeatDays', this.repeatDays_);
  map.set('repeatFreq', this.repeatFreq_);
  map.set('exceptions', this.exceptions_);
  map.set('exceptionExtIds', this.exceptionExtIds_);
  map.set('recurrenceId', this.recurrenceId_);
  map.set('timezone', bluemind.me['timezone']);

  if (this.tags_ != null) {
    var tags = [];
    var t = this.tags_.getValues();
    for (var i = 0; i < t.length; i++) {
      tags.push(t[i].serialize());
    }
    map.set('tags', tags);
  }

  map.set('uc', this.usercreate_);
  map.set('uu', this.userupdate_);
  map.set('tc', this.timecreate_);
  map.set('tu', this.timeupdate_);

  map.set('ownerCalId', this.ownerCalendarId_);
  return map;
};

/**
 * Compare 2 events.
 * @param {bluemind.calendar.model.Event} evt event.
 * @return {boolean} are events equals.
 */
bluemind.calendar.model.Event.prototype.equals = function(evt) {
  return (evt.getExtId() == this.getExtId() &&
    evt.getDate().getTime() == this.getDate().getTime() &&
    evt.getDuration() == this.getDuration());
};

/**
 * @return {boolean} has event changed.
 */
bluemind.calendar.model.Event.prototype.hasChanged = function() {
  return (this.duration_ != this.initialDuration_ ||
    this.date_.getTime() != this.initialDate_.getTime() ||
    this.initialTitle_ != this.title_);
};

/**
 * @return {boolean} evts are equals.
 */
bluemind.calendar.model.Event.prototype.hasImportantChanges = function(evt) {
  return (
    goog.date.Date.compare(this.date_, evt.getDate()) != 0 ||
    this.duration_ != evt.getDuration() ||
    this.location_ != evt.getLocation());
};

/**
 * @return {boolean} evts are equals.
 */
bluemind.calendar.model.Event.prototype.hasAttendeesListUpdated = function(evt) {
  if (this.attendees_.length != evt.getAttendees().length) {
    return true;
  }

  var ret = false;
  var currentCalendar = new Array();
  goog.array.forEach(this.attendees_, function(a) {
    goog.array.insert(currentCalendar, a['calendar']);
  });

  goog.array.forEach(evt.getAttendees(), function(a) {
    if (!goog.array.contains(currentCalendar, a['calendar'])) {
      ret = true;
    }
  });

  return ret;
};

/**
 * Clear event recurrency
 */
bluemind.calendar.model.Event.prototype.clearRecurrence = function() {
  this.repeatKind_ = 'none';
  this.repeatFreq_ = null;
  this.repeatDays_ = null;
  this.exceptions_ = new Array();
  this.exceptionExtIds_ = new Array();
  this.repeatEnd_ = null;
  this.occurrences_ = null;
};

/**
 * @return {boolean} event is in the past.
 */
bluemind.calendar.model.Event.prototype.isInThePast = function() {
  var ret = false;
  var now = goog.now();
  if (this.getRepeatKind() != 'none') {
    if (this.getRepeatEnd() == null) {
      return false;
    } else {
      return this.getRepeatEnd().getTime() < now;
    }
  }
  return this.getEnd().getTime() < now;
};

/**
 * Yet another onlyMyself method style
 */
bluemind.calendar.model.Event.prototype.onlyMyselfAsAttendee = function() {
  return (this.attendees_.length == 1 && this.attendees_[0]['type'] == 'user' &&
    (this.attendees_[0]['id'] == bluemind.me['id'] || goog.array.contains(bluemind.writableCalendars,
      this.attendees_[0]['calendar'])));
};

/**
 * Create a event object from an hashmap of data.
 * @param {Object} data Hashmap containing event data.
 * @return {bluemind.calendar.model.Event} Event build from hashmap data.
 */
bluemind.calendar.model.Event.parse = function(data) {
  return new bluemind.calendar.model.Event(data);
};

/**
 * Clone this event.
 * @return {bluemind.calendar.model.Event} evt clone.
 */
bluemind.calendar.model.Event.prototype.clone = function() {
  var clone = new bluemind.calendar.model.Event();
  for (var key in this) {
    clone[key] = goog.object.unsafeClone(this[key]);
  }
  goog.removeUid(clone);
  return clone;
};

/**
 * create popup all day event.
 */
bluemind.calendar.model.Event.prototype.createAlldayPopup = function(start, end) {
  var title = this.title_;
  if (!this.allday_) {
    title = this.date_.toIsoTimeString(false) + ' ' + this.title_;
  }

  var pending = '';
  if (this.participation_ ==
      bluemind.calendar.model.AttendeeParticipation.NEEDSACTION) {
    pending = goog.getCssName('pending');
  }
  var declined = '';
  if (this.participation_ ==
    bluemind.calendar.model.AttendeeParticipation.DECLINED) {
    declined = goog.getCssName('declined');
  }

  var dim = (goog.date.Date.compare(this.end_,
      new bluemind.date.DateTime()) < 0);

  var klass;
  var ico = '';
  if (bluemind.view.getView().getName() != 'month' ||
    (bluemind.view.getView().getName() == 'month' && this.allday_) ||
    (bluemind.view.getView().getName() == 'month' &&
      !this.allday_ && this.alldaySize > 1)) {
    klass = bluemind.fixCssName(this.klass_, dim);
  } else {
    klass = bluemind.fixMonthCssName(this.klass_, dim);
    ico = 'dark';
  }

  klass += ' ' + goog.getCssName('event') +
    ' ' + goog.getCssName('allDayEvent') + ' ' + pending +
    ' ' + declined;
  var tooltip = this.title_;

  var left = this.date_.getTime() < start.getTime();
  var right = this.end_.getTime() > end.getTime();

  var leftEl = null;
  if (left) {
    var d = bluemind.i18n.DateTimeHelper.getInstance().formatDate(this.date_);
    var data = {
      id: 'left_event_' + goog.getUid(this),
      date: '',
      tooltip: '',
      title: d,
      ico: '',
      repeat: false,
      klass: klass,
      left: true,
      right: false,
      opaque: true,
      updatable: false,
      isPrivate: false,
      isAttend: this.isAttend_,
      isMeeting: false,
      location: this.location_ || '',
      tags: []
    };
    leftEl = soy.renderAsFragment(
        bluemind.calendar.template.eventAllday, data);

  }

  var rightEl = null;
  if (right) {
    var d = bluemind.i18n.DateTimeHelper.getInstance().formatDate(this.end_);
    var data = {
      id: 'right_event_' + goog.getUid(this),
      date: '',
      tooltip: '',
      title: d,
      ico: '',
      repeat: false,
      klass: klass,
      left: false,
      right: true,
      opaque: true,
      updatable: false,
      isPrivate: false,
      isAttend: this.isAttend_,
      isMeeting: false,
      location: this.location_ || '',
      tags: []
    };
    rightEl = soy.renderAsFragment(
        bluemind.calendar.template.eventAllday, data);
  }

  var data = {
    id: 'event_' + goog.getUid(this),
    date: '',
    tooltip: tooltip,
    title: title,
    ico: ico,
    repeat: (this.repeatKind_ != 'none'),
    klass: klass,
    left: false,
    right: false,
    opaque: this.opacity_ ? 'opaque' : 'transparent',
    updatable: false,
    isPrivate: this.private_,
    isAttend: this.isAttend_,
    isMeeting: false, //fixme
    location: this.location_ || '',
    tags: []
  };
  var el = soy.renderAsFragment(
    bluemind.calendar.template.eventAllday, data);

  this.element_ = goog.dom.createDom('table');
  goog.dom.classes.add(this.element_, goog.getCssName('alldayEvents'));

  var tr = goog.dom.createDom('tr');
  goog.dom.appendChild(this.element_, tr);

  var td = goog.dom.createDom('td');
  goog.dom.classes.add(td, goog.getCssName('left'));
  goog.dom.appendChild(tr, td);
  if (left) {
    goog.dom.appendChild(td, leftEl);
  }

  td = goog.dom.createDom('td');
  goog.dom.appendChild(td, el);
  goog.dom.appendChild(tr, td);

  var td = goog.dom.createDom('td');
  goog.dom.classes.add(td, goog.getCssName('right'));
  goog.dom.appendChild(tr, td);
  if (right) {
    goog.dom.appendChild(td, rightEl);
  }
};






