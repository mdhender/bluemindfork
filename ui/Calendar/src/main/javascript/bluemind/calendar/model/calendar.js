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
 * @fileoverview Calendar.
 */

goog.provide('bluemind.calendar.model.Calendar');

goog.require('goog.dom.classes');

/**
 * This is calendar
 *
 * @param {number} id calendar id.
 * @param {text} label calendar label.
 * @param {text} email calendar email.
 * @param {number} ownerId calendar ownerId.
 * @param {number} picture calendar picture id.
 * @param {text} klass class.
 * @param {text} type type.
 * @param {text} workingDays working days.
 * @param {text} dayStart day start.
 * @param {text} dayEnd day end.
 * @constructor
 */
bluemind.calendar.model.Calendar =
  function(id, label, email, ownerId, picture,
    klass, type, workingDays, dayStart, dayEnd) {
  this.id_ = id;
  this.label_ = label;
  this.email_ = email;
  this.ownerId_ = ownerId;
  this.visibility_ = true;
  this.picture_ = picture;
  this.class_ = 0;
  this.type_ = 'user';
  this.class_ = klass;
  this.type_ = type;
  this.workingDays_ = workingDays;
  this.dayStart_ = dayStart;
  this.dayEnd_ = dayEnd;
};

/**
 * Calendar id.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.id_;

/**
 * Calendar owner id.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.ownerId_;

/**
 * Calendar label.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.label_;

/**
 * Calendar email.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.email_;

/**
 * Calendar rights
 * @type {Array}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.rights_;

/**
 * Calendar dom element
 * @type {Element}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.element_;

/**
 * Is calendar visible,
 * @type {boolean}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.visibility_;

/**
 * Calendar CSS class index,
 * @type {number}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.class_;

/**
 * Calendar picture id.
 * @type {number}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.picture_;

/**
 * Calendar type.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.type_;

/**
 * Calendar working days.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.workingDays_;

/**
 * Calendar day end.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.dayStart_;

/**
 * Calendar day start.
 * @type {text}
 * @private
 */
bluemind.calendar.model.Calendar.prototype.dayEnd_;

/**
 * @return {number} get calendar id.
 */
bluemind.calendar.model.Calendar.prototype.getId = function() {
  return this.id_;
};

/**
 * @return {number} get calendar owner id.
 */
bluemind.calendar.model.Calendar.prototype.getOwnerId = function() {
  return this.ownerId_;
};

/**
 * @return {string} get calendar label.
 */
bluemind.calendar.model.Calendar.prototype.getLabel = function() {
  return this.label_;
};

/**
 * @return {string} get calendar email.
 */
bluemind.calendar.model.Calendar.prototype.getEmail = function() {
  return this.email_;
};

/**
 * @return {number} get calendar picture id.
 */
bluemind.calendar.model.Calendar.prototype.getPicture = function() {
  return this.picture_;
};

/**
 * @return {string} get calendar type.
 */
bluemind.calendar.model.Calendar.prototype.getType = function() {
  return this.type_;
};

/**
 * @return {string} get calendar working days.
 */
bluemind.calendar.model.Calendar.prototype.getWorkingDays = function() {
  return this.workingDays_;
};

/**
 * @return {string} get calendar day start.
 */
bluemind.calendar.model.Calendar.prototype.getDayStart = function() {
  return this.dayStart_;
};

/**
 * @return {string} get calendar day end.
 */
bluemind.calendar.model.Calendar.prototype.getDayEnd = function() {
  return this.dayEnd_;
};

/**
 * Return the Calendar CSS class index
 * @return {number} .
 */
bluemind.calendar.model.Calendar.prototype.getClass = function() {
  return this.class_;
};

/**
 * Set the Calendar CSS class index
 * @param {number} klass class index.
 */
bluemind.calendar.model.Calendar.prototype.setClass = function(klass) {
  this.class_ = klass % 20;
};

/**
 * @return {boolean} get event is visible.
 */
bluemind.calendar.model.Calendar.prototype.isVisible = function() {
  return this.visibility_;
};

/**
 * @param {boolean} v event visibility.
 */
bluemind.calendar.model.Calendar.prototype.setVisible = function(v) {
  this.visibility_ = v;
};

/**
 * Add to calendars list
 */
bluemind.calendar.model.Calendar.prototype.createDom = function() {
  var container = goog.dom.getElement('bm-calendars-items');
  var data = {
    id: this.id_,
    klass: bluemind.fixCssName(this.getClass(), false),
    label: this.label_,
    picture: this.picture_,
    type: this.type_,
    email: this.email_,
    sid: bluemind.me['sid']};
  this.element_ = soy.renderAsFragment(bluemind.calendar.template.calendar,
    data);

  goog.dom.appendChild(container, this.element_);

  // Hide calendar
  goog.events.listen(this.element_, goog.events.EventType.CLICK,
    bluemind.manager.toggleCalendarVisibility, false, this);

  // Remove calendar
  var removeElem = goog.dom.getElement('remove' + this.id_);
  goog.events.listen(removeElem, goog.events.EventType.CLICK,
    bluemind.manager.unregisterCalendar, false, this);

};

/**
 * Remove calendar dom element.
 */
bluemind.calendar.model.Calendar.prototype.removeDom = function() {
  goog.dom.removeNode(this.element_);
};

/**
 * Remove calendar element opacity.
 */
bluemind.calendar.model.Calendar.prototype.toggleVisibility = function() {
  goog.dom.classes.toggle(this.element_, goog.getCssName('disabled'));
  this.visibility_ = !this.visibility_;
};

/**
 * Caledar to Map
 * @return {goog.structs.Map} map.
 */
bluemind.calendar.model.Calendar.prototype.toMap = function() {
  var map = new goog.structs.Map();
  map.set('id', this.id_);
  map.set('label', this.label_);
  map.set('email', this.email_);
  map.set('ownerId', this.ownerId_);
  map.set('picture', this.picture_);
  map.set('class', this.class_);
  map.set('type', this.type_);
  map.set('workingDays', this.workingDays_);
  map.set('dayStart', this.dayStart_);
  map.set('dayEnd', this.dayEnd_);
  return map;
};

/**
 * Create a calendar object from an hashmap of data.
 * @param {Object} data Hashmap containing calendar data.
 * @return {bluemind.calendar.model.Calendar} Calendar build from hashmap data.
 */
bluemind.calendar.model.Calendar.parse = function(c) {
   var cal = new bluemind.calendar.model.Calendar(
          c['id'], c['label'], c['email'], c['ownerId'],
          c['picture'], c['class'], c['type'], c['workingDays'],
          c['dayStart'], c['dayEnd'], c['minDuration']);

   return cal;
};
