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

goog.provide("net.bluemind.calendar.navigation.ac.CalendarMatcher");

goog.require("goog.Disposable");

/**
 * @constructor
 * @param {net.bluemind.calendar.api.CalendarAutocompleteClient} client Auto
 * complete client.
 * @param {Array.<Object>=} opt_calendar Calendar already added
 * @extends {goog.Disposable}
 */
net.bluemind.calendar.navigation.ac.CalendarMatcher = function(ctx, opt_calendars) {
  this.ctx = ctx;
  this.client_ = ctx.client('calendar-autocomplete');
  this.calendars = opt_calendars || [];
  goog.base(this);
};
goog.inherits(net.bluemind.calendar.navigation.ac.CalendarMatcher, goog.Disposable);

/**
 * @type {net.bluemind.calendar.api.CalendarAutocompleteClient}
 * @private
 */
net.bluemind.calendar.navigation.ac.CalendarMatcher.prototype.client_;

/**
 * @type {Array}
 */
net.bluemind.calendar.navigation.ac.CalendarMatcher.prototype.calendars;

/**
 * Returns whether the suggestions should be updated? <b>Override this to
 * prevent updates eg - when token is empty.</b>
 * 
 * @param {string} token Current token in autocomplete.
 * @param {number} maxMatches Maximum number of matches required.
 * @param {string=} opt_fullString Complete text in the input element.
 * @return {boolean} Whether new matches be requested.
 * @protected
 */
net.bluemind.calendar.navigation.ac.CalendarMatcher.prototype.shouldRequestMatches = function(token, maxMatches,
    opt_fullString) {
  return true;
};

/**
 * Handles the XHR response.
 * 
 * @param {string} token The XHR autocomplete token.
 * @param {Function} matchHandler The AutoComplete match handler.
 * @param {Array.<Object>} result Search result.
 */
net.bluemind.calendar.navigation.ac.CalendarMatcher.prototype.onMatch = function(token, matchHandler, result) {
  var calendars = goog.array.filter(result, function(row) {
    return !goog.array.contains(this.calendars, row['uid']);
  }, this)
  matchHandler(token, calendars);
};

/**
 * Retrieve a set of matching rows from the server via ajax.
 * 
 * @param {string} token The text that should be matched; passed to the server
 * as the 'token' query param.
 * @param {number} maxMatches The maximum number of matches requested from the
 * server; passed as the 'max_matches' query param. The server is responsible
 * for limiting the number of matches that are returned.
 * @param {Function} matchHandler Callback to execute on the result after
 * matching.
 * @param {string=} opt_fullString The full string from the input box.
 */
net.bluemind.calendar.navigation.ac.CalendarMatcher.prototype.requestMatchingRows = function(token, maxMatches,
    matchHandler, opt_fullString) {

  if (!this.shouldRequestMatches(token, maxMatches, opt_fullString)) {
    return;
  }

  return this.client_.calendarLookup(token, 'Read').then(function(resp) {
    var mapped = goog.array.map(resp, function(cal) {
      var dir = new net.bluemind.directory.api.DirectoryClient(this.ctx.rpc, '', this.ctx.user['domainUid']);
      if (cal['ownerUid'] != null) {

        return dir.getEntryIcon(cal['ownerUid']).then(function(photo) {
          if (photo && photo.length > 0) {
            cal['photo'] = "data:image/png;base64," + photo;
          }

          return cal;
        }, function() {
          return cal;
        }, this);
      } else {
        return cal;
      }
    }, this);

    return goog.Promise.all(mapped);
  }, null, this).then(function(r) {
    return this.onMatch(token, matchHandler, r);
  }, null, this);
}
