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
 * @fileoverview Object that hold the synchronisation parmaters of a calendar.
 */

goog.provide('bluemind.calendar.model.SyncCalendar');
goog.provide('bluemind.calendar.model.SyncCalendar.SyncMode');

/**
 * This is meant to be use by the synchronization process
 * to send the calendar specific sync parameters.
 * @param {number} calendar calendar id.
 * @constructor
 */
bluemind.calendar.model.SyncCalendar = function(calendar) {
  this.calendar_ = calendar;
  this.mode_ = bluemind.calendar.model.SyncCalendar.SyncMode.CHANGED_AFTER_DATE;
  this.lastSync_ = 0;
  this.endSync_ = null;
};

/**
 * Method that will be used to calculate changes on bm-core.
 * @type {bluemind.calendar.model.SyncCalendar.SyncMode}
 * @private
 */
bluemind.calendar.model.SyncCalendar.prototype.mode_;

/**
 * Depending on the sync mode, date of the last synchronization or left
 * border for the sync period
 * @type {number}
 * @private
 */
bluemind.calendar.model.SyncCalendar.prototype.lastSync_;

/**
 * Depending on the sync mode, date of the last synchronization or left
 * border for the sync period
 * @type {number}
 * @private
 */
bluemind.calendar.model.SyncCalendar.prototype.endSync_;

/**
 * Calendar id
 * @type {number}
 * @private
 */
bluemind.calendar.model.SyncCalendar.prototype.calendar_;

/**
 * Return the method use to synchronize the calendar.
 * @return {bluemind.calendar.model.SyncCalendar.SyncMode} return sync mode.
 */
bluemind.calendar.model.SyncCalendar.prototype.getMode = function() {
  return this.mode_;
};

/**
 * Set the method use to synchronize the calendar.
 * @param {bluemind.calendar.model.SyncCalendar.SyncMode} mode
 *   How the changes on bm-core side will be calculated.
 */
bluemind.calendar.model.SyncCalendar.prototype.setMode = function(mode) {
  this.mode_ = mode;
};

/**
 * Depending on the sync mode set :
 * - Timestamp since when the last synchronization have been done successfully.
 * - Timestamp after which the events must occurs.
 * @return {number} Timestamp .
 */
bluemind.calendar.model.SyncCalendar.prototype.getLastSync = function() {
  return this.lastSync_;
};

/**
 * Depending on the sync mode set :
 * - Timestamp since when the last synchronization have been done successfully.
 * - Timestamp after which the events must occurs.
 * @param {number} lastSync Timestamp to determine the border of the sync.
 */
bluemind.calendar.model.SyncCalendar.prototype.setLastSync =
  function(lastSync) {
  if (lastSync) {
    this.lastSync_ = parseInt(lastSync);
  } else {
    this.lastSync_ = 0;
  }
};

/**
 * Depending on the sync mode set :
 * - events changed after timestamp will not be returneduccessfully.
 * - Timestamp before which the events must occurs.
 * @return {number} Timestamp .
 */
bluemind.calendar.model.SyncCalendar.prototype.getEndSync = function() {
  return this.endSync_;
};

/**
 * Depending on the sync mode return :
 * - events changed after timestamp will not be returneduccessfully.
 * - Timestamp before which the events must occurs.
 * @param {number} endSync Timestamp to determine the border of the sync.
 */
bluemind.calendar.model.SyncCalendar.prototype.setEndSync = function(endSync) {
  this.endSync_ = endSync;
};

/**
 * Set the id of the calendar that will be synced
 * @return {number} Calendar id.
 */
bluemind.calendar.model.SyncCalendar.prototype.getCalendar = function() {
  return this.calendar_;
};

/**
 * Get the id of the calendar that will be synced
 * @param {number} calendar Calendar id.
 */
bluemind.calendar.model.SyncCalendar.prototype.setCalendar =
  function(calendar) {
  this.calendar_ = calendar;
};

/**
 * Serialize sync calendar object as raw map.
 * A pattern should be used to handle the multiple kind of serialization
 * (serialize for network, template, local storage...), maybe strategy (bingo).
 * @return {Object.<String, *>} Serialized sync calendar.
 */
bluemind.calendar.model.SyncCalendar.prototype.serialize = function() {
  var s = {};
  s['calendar'] = this.calendar_;
  s['lastSync'] = this.lastSync_;
  s['mode'] = this.mode_;
  if (this.endSync_) {
    s['endSync'] = this.endSync_;
  }
  return s;
};

/**
 * sync mode.
 */
bluemind.calendar.model.SyncCalendar.SyncMode = {
  CHANGED_AFTER_DATE: 'CHANGED_AFTER_DATE',
  OCCURS_AFTER_DATE: 'OCCURS_AFTER_DATE'
};
