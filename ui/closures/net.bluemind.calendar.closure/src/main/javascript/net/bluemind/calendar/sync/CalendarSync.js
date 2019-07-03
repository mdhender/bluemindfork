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
goog.provide("net.bluemind.calendar.sync.CalendarSync");

goog.require("goog.log");
goog.require("net.bluemind.calendar.sync.CalendarSyncClient");
goog.require("net.bluemind.container.sync.ContainerSync");

/**
 * Synchronize celendars data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.calendar.sync.CalendarSync = function(ctx) {
  goog.base(this, ctx);
  this.logger = goog.log.getLogger('net.bluemind.calendar.sync.CalendarSync');
  this.type = 'calendar';
};
goog.inherits(net.bluemind.calendar.sync.CalendarSync, net.bluemind.container.sync.ContainerSync);

/** @override */
net.bluemind.calendar.sync.CalendarSync.prototype.getClient = function(uid) {
  return new net.bluemind.calendar.sync.CalendarSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.calendar.sync.CalendarSync.prototype.getContainerService = function() {
  return this.ctx.service('calendar').cs_;
};

/** @override */
net.bluemind.calendar.sync.CalendarSync.prototype.getContainersService = function() {
  return this.ctx.service('calendars').css_;
};

/** @override */
net.bluemind.calendar.sync.CalendarSync.prototype.containersToSyncList = function() {
  return this.ctx.service('calendars').list('calendar');
};

/** @override */
net.bluemind.calendar.sync.CalendarSync.prototype.getName = function() {
  return 'Calendar';
};

/** @override */
net.bluemind.calendar.sync.CalendarSync.prototype.adaptItem = function(item) {
  return this.ctx.service('calendar').sanitize(item);
};
