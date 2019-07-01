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
goog.provide("net.bluemind.calendar.sync.CalendarViewSync");

goog.require("goog.log");
goog.require("net.bluemind.calendar.sync.CalendarViewSyncClient");
goog.require("net.bluemind.container.sync.UnitaryContainerSync");
goog.require("goog.async.Deferred");
/**
 * Synchronize celendars data with bm-core.
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.calendar.sync.CalendarViewSync = function(ctx) {
  goog.base(this, ctx, "calendarview:" + ctx.user['uid']);
  this.logger = goog.log.getLogger('net.bluemind.calendar.sync.CalendarViewSync');
  this.type = 'calendarview';
  this.css_ = new net.bluemind.container.service.ContainersService(ctx, 'calendarview');
};
goog.inherits(net.bluemind.calendar.sync.CalendarViewSync, net.bluemind.container.sync.UnitaryContainerSync);

/** @override */
net.bluemind.calendar.sync.CalendarViewSync.prototype.getClient = function(uid) {
  return new net.bluemind.calendar.sync.CalendarViewSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.calendar.sync.CalendarViewSync.prototype.getContainerService = function() {
  return this.ctx.service('calendarviews').cs_;
};

/** @override */
net.bluemind.calendar.sync.CalendarViewSync.prototype.getContainersService = function() {
  return this.css_;
};

/** @override */
net.bluemind.calendar.sync.CalendarViewSync.prototype.getName = function() {
  return 'calendarview';
};
