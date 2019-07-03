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
 * @fileoverview CalendarView.
 * @suppress {accessControls|checkTypes|checkVars|missingProperties|undefinedNames|undefinedVars|unknownDefines|uselessCode|visibility}
 * @suppress {*}
 */

goog.provide('bluemind.calendar.model.CalendarView');

goog.require('goog.structs.Map');

/**
 * Calendar view
 *
 * @param {number} id calendar id.
 * @param {string} label calendar label.
 * @param {Array} calendars calendars list.
 * @param {Object} props view properties.
 * @constructor
 */
bluemind.calendar.model.CalendarView = function(id, label, calendars, props) {
  this.id_ = id;
  this.label_ = label;
  this.calendars_ = new Array();
  this.properties_ = new goog.structs.Map();
  goog.array.forEach(calendars, function(c) {
    var cal = new bluemind.calendar.model.Calendar(c['calendar'],
      c['displayName'], c['email'], c['ownerId'], c['picture'], c['class'],
      c['type'], c['workingDays'], c['dayStart'], c['dayEnd'],
      c['minDuration']);
    goog.array.insert(this.calendars_, cal);
  }, this);
  goog.object.forEach(props, function(v, k) {
    this.properties_.set(k, v);
  }, this);
};

/**
 * CalendarView id.
 * @type {number}
 * @private
 */
bluemind.calendar.model.CalendarView.prototype.id_;

/**
 * CalendarView label.
 * @type {string}
 * @private
 */
bluemind.calendar.model.CalendarView.prototype.label_;

/**
 * CalendarView calendars.
 * @type {Array}
 * @private
 */
bluemind.calendar.model.CalendarView.prototype.calendars_;

/**
 * CalendarView properties.
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.model.CalendarView.prototype.properties_;

/**
 * @return {number} get calendar id.
 */
bluemind.calendar.model.CalendarView.prototype.getId = function() {
  return this.id_;
};

/**
 * @return {string} get calendar label.
 */
bluemind.calendar.model.CalendarView.prototype.getLabel = function() {
  return this.label_;
};

/**
 * @return {number} get calendars.
 */
bluemind.calendar.model.CalendarView.prototype.getCalendars = function() {
  return this.calendars_;
};

/**
 * @return {string} get properties.
 */
bluemind.calendar.model.CalendarView.prototype.getProperties = function() {
  return this.properties_;
};
