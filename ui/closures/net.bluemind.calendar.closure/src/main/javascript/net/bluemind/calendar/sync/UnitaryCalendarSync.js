/* BEGIN LICENSE
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
 * @fileoverview Calendar synchronization service.
 */
goog.provide("net.bluemind.calendar.sync.UnitaryCalendarSync");

goog.require("goog.log");
goog.require("net.bluemind.calendar.sync.CalendarSyncClient");
goog.require("net.bluemind.container.sync.UnitaryContainerSync");

/**
 * Synchronize celendars data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @param {string} containerUid container uid to synchronize
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.calendar.sync.UnitaryCalendarSync = function(ctx, containerUid) {
  goog.base(this, ctx, containerUid);
  this.logger = goog.log.getLogger('net.bluemind.calendar.sync.UnitaryCalendarSync');
};
goog.inherits(net.bluemind.calendar.sync.UnitaryCalendarSync, net.bluemind.container.sync.UnitaryContainerSync);

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.getClient = function(uid) {
  return new net.bluemind.calendar.sync.CalendarSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.getContainerService = function() {
  return this.ctx.service('calendar').cs_;
};

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.getContainersService = function() {
  return this.ctx.service('calendars').css_;
};

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.getName = function() {
  return 'Calendar (' + this.containerUid + ')';
};

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.adaptItem = function(item) {
  return this.ctx.service('calendar').sanitize(item);
};

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.adaptCreate = function(entry) {
  return {
    'uid' : entry['uid'],
    'value' : entry['value'],
    'sendNotification' : entry['sendNotification']
  };
};

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.adaptDelete = function(entry) {
  var itemDelete = goog.base(this, 'adaptDelete', entry);
  itemDelete['sendNotification'] = goog.isDefAndNotNull(entry['sendNotification']) || true;
  return itemDelete;
};

/** @override */
net.bluemind.calendar.sync.UnitaryCalendarSync.prototype.syncContainerItems = function() {
  var calendar;
  var ret = this.ctx.service('folders').getFoldersRemote(null, [this.containerUid]).then(function(cals) {
    if( cals.length != 1) {
      return this.ctx.service('calendars').removeCalendar(this.containerUid).then(function() {
        throw "ContainerNotFound";  
      });
    } else {
      return cals[0];
    }
  }, null, this).then(function(remote) {
    calendar = remote;
    return this.ctx.service('calendars').get(calendar['uid']);
  }, null, this).then(function(localCalendar) {
      if (calendar['name'] != localCalendar['name'] || calendar['writable'] != localCalendar['writable'] || calendar['offlineSync'] != localCalendar['offlineSync']) {
        return this.ctx.service('calendars').updateCalendars([calendar]);
      }
  }, null, this).then(function() {
    return net.bluemind.calendar.sync.UnitaryCalendarSync.superClass_.syncContainerItems.call(this);
  }, null, this);
  
  return goog.async.Deferred.fromPromise(ret);
};
