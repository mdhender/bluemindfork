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
 * @fileoverview TodoList synchrnisation service
 */
goog.provide("net.bluemind.todolist.sync.TodoListSync");

goog.require("goog.log");
goog.require("net.bluemind.container.sync.ContainerSync");
goog.require("net.bluemind.todolist.sync.TodoListSyncClient");
goog.require("goog.async.Deferred");
/**
 * Synchronize tags data with bm-core.
 * 
 * @param {relief.rpc.RPCService} rpc RPC Manager
 * @param {string} base Api base url
 * @constructor
 * @extends {net.bluemind.container.sync.ContainerSync}
 */
net.bluemind.todolist.sync.TodoListSync = function(ctx) {
  goog.base(this, ctx);
  this.logger = goog.log.getLogger('net.bluemind.todolist.sync.TodoListSync');
  this.type = 'todolist';

};
goog.inherits(net.bluemind.todolist.sync.TodoListSync, net.bluemind.container.sync.ContainerSync);

/** @override */
net.bluemind.todolist.sync.TodoListSync.prototype.getClient = function(uid) {
  return new net.bluemind.todolist.sync.TodoListSyncClient(this.ctx, uid);
};

/** @override */
net.bluemind.todolist.sync.TodoListSync.prototype.getName = function() {
  return 'Todolist';
};

/** @override */
net.bluemind.todolist.sync.TodoListSync.prototype.getContainerService = function() {
  return this.ctx.service('todolist').cs_;
};

/** @override */
net.bluemind.todolist.sync.TodoListSync.prototype.getContainersService = function() {
  return this.ctx.service('todolists').css_
};

/** @override */
net.bluemind.todolist.sync.TodoListSync.prototype.containersToSyncList = function() {
  return goog.async.Deferred.fromPromise(this.ctx.service('todolists').list());
}

/** @override */
net.bluemind.todolist.sync.TodoListSync.prototype.adaptItem = function(item) {
  return this.ctx.service('todolist').sanitize(item);
  ;
};
