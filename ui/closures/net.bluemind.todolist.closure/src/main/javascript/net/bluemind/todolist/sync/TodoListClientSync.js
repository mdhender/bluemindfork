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
 * @fileoverview Provide services for tasks
 */
goog.provide('net.bluemind.todolist.sync.TodoListSyncClient');

goog.require("net.bluemind.todolist.api.TodoListClient");
goog.require("net.bluemind.container.sync.ContainerSyncClient");

net.bluemind.todolist.sync.TodoListSyncClient = function(ctx, mailboxUid) {
  this.client = new net.bluemind.todolist.api.TodoListClient(ctx.rpc, '', mailboxUid);
};
goog.inherits(net.bluemind.todolist.sync.TodoListSyncClient, net.bluemind.container.sync.ContainerSyncClient);

net.bluemind.todolist.sync.TodoListSyncClient.prototype.changeset = function(version) {
  return this.client.changeset(version);
};

net.bluemind.todolist.sync.TodoListSyncClient.prototype.updates = function(updates) {
  return this.client.updates(updates);
};

net.bluemind.todolist.sync.TodoListSyncClient.prototype.retrieve = function(uids) {
  return this.client.multipleGet(uids);
};