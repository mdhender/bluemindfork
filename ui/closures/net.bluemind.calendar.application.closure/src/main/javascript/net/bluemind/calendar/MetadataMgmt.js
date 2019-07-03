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
 * @fileoverview Provide services for task lists
 */

goog.provide("net.bluemind.calendar.MetadataMgmt");

goog.require("goog.array");
goog.require("goog.structs.Map");
goog.require("net.bluemind.calendar.ColorPalette");

/**
 * 
 *
 */
net.bluemind.calendar.MetadataMgmt = function(ctx) {
  this.ctx_ = ctx;
};

/**
 * @private {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.MetadataMgmt.prototype.ctx_;

/**
 * @private {Object.<string, string>}
 */
net.bluemind.calendar.MetadataMgmt.prototype.colors_;

/**
 * Synchronized metadatas from storage with metadata from calendars parameter.
 * 
 * @param {Array.<Object>} calendars
 * @returns {goog.Promise}
 */
net.bluemind.calendar.MetadataMgmt.prototype.add = function(calendars) {
  return this.synchronize(calendars);
}

/**
 * Synchronized metadatas from storage with metadata from calendar management.
 * 
 * @param {Array.<Object>} calendars
 * @return {goog.Promise}
 */
net.bluemind.calendar.MetadataMgmt.prototype.update = function() {
  return this.ctx_.service('calendars').list().then(function(calendars) {
    return this.synchronize(calendars);
  }, null, this).thenCatch(function(error) {
    this.ctx_.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);

};

/**
 * Synchronized metadatas from storage with metadata from calendars parameter.
 * 
 * @param {Array.<Object>} calendars
 * @private
 * @return {goog.Promise}
 */
net.bluemind.calendar.MetadataMgmt.prototype.synchronize = function(calendars) {
  return this.loadColors_(calendars).then(function(items) {
    return this.loadVisibility_(calendars);
  }, null, this).thenCatch(function(error) {
    this.ctx_.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
}

/**
 * Synchronize color
 * @param {Array.<Object>} calendars
 * @private
 * @return {goog.Promise}
 */
net.bluemind.calendar.MetadataMgmt.prototype.loadColors_ = function(calendars) {
  return this.ctx_.service('auth').get('calendar.colors').thenCatch(function() {
    return {};
  }, this).then(function(colors) {
    var uids = [];
    var colorless = [];
    goog.array.forEach(calendars, function(calendar) {
      if (calendar['settings']['bm_color']) {
        colors[calendar['uid']] = calendar['settings']['bm_color'];
      } else if (!goog.isDefAndNotNull(colors[calendar['uid']])) {
        colorless.push(calendar);
      }
      uids.push(calendar['uid']);
    });
    colors = goog.object.filter(colors, function(color, key) {
      return goog.array.contains(uids, key);
    });
    var palette = net.bluemind.calendar.ColorPalette.getColors(goog.object.getValues(colors));

    goog.array.forEach(colorless, function(calendar) {
      if (palette.length == 0) {
        palette = goog.array.clone(net.bluemind.calendar.ColorPalette.getColors());
      }
      colors[calendar['uid']] = palette.shift();
    });
    return this.ctx_.service('auth').set('calendar.colors', colors);
  }, null, this).then(function(colors) {
    goog.array.forEach(calendars, function(calendar) {
      if (!calendar['metadata']) calendar['metadata'] = {};
      calendar['metadata']['color'] = colors[calendar['uid']]
    });
    return calendars;
  }, null, this);

};

/**
 * Synchronize visibility
 * @param {Array.<Object>} calendars
 * @private
 * @return {goog.Promise}
 */
net.bluemind.calendar.MetadataMgmt.prototype.loadVisibility_ = function(calendars) {
  return this.ctx_.service('auth').get('calendar.visibility').thenCatch(function() {
    return {};
  }, this).then(function(visibility) {
    var uids = [];
    goog.array.forEach(calendars, function(calendar) {
      if (!goog.isDefAndNotNull(visibility[calendar['uid']])) {
        visibility[calendar['uid']] = true;
      }
      uids.push(calendar['uid']);
    });
    return this.ctx_.service('auth').set('calendar.visibility', visibility);
  }, null, this).then(function(visibility) {
    goog.array.forEach(calendars, function(calendar) {
      if (!calendar['metadata']) calendar['metadata'] = {};
      calendar['metadata']['visible'] = !!visibility[calendar['uid']];
    })
    return calendars;
  }, null, this);

};

