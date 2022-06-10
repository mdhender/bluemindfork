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
 * @fileoverview Provide services for tasks
 */
goog.provide('net.bluemind.calendar.sync.CalendarViewSyncClient');

goog.require("net.bluemind.calendar.api.UserCalendarViewsClient");
goog.require("net.bluemind.container.sync.ContainerSyncClient");

net.bluemind.calendar.sync.CalendarViewSyncClient = function(ctx, mailboxUid) {
  this.calClient = new net.bluemind.calendar.api.UserCalendarViewsClient(ctx.rpc, '', ctx.user['domainUid'], ctx.user['uid']);
};
goog.inherits(net.bluemind.calendar.sync.CalendarViewSyncClient, net.bluemind.container.sync.ContainerSyncClient);

net.bluemind.calendar.sync.CalendarViewSyncClient.prototype.changeset = function(version) {
  return this.calClient.changeset(version);
};

net.bluemind.calendar.sync.CalendarViewSyncClient.prototype.updates = function(updates) {
  return this.calClient.updates(updates);
};

net.bluemind.calendar.sync.CalendarViewSyncClient.prototype.retrieve = function(uids) {
  return this.calClient.multipleGet(uids);
};