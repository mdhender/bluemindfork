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

goog.provide("net.bluemind.todolist.service.TodolistsSyncManager");

goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("goog.events.EventHandler");
goog.require("net.bluemind.sync.SyncEngine");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainersObserver.EventType");
goog.require("net.bluemind.todolist.sync.UnitaryTodoListSync");
/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 */
net.bluemind.todolist.service.TodolistsSyncManager = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  /** @private {goog.debug.Logger} */
  this.logger_ = goog.log.getLogger('net.bluemind.todolist.service.TodolistsSyncManager');
  this.containersSyncByUid = new goog.structs.Map();
  this.handler = new goog.events.EventHandler(this);
  this.handler.listen(this.ctx.service('todolists'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.refresh);

  this.handler.listen(this.ctx.service('containersObserver'),
      net.bluemind.container.service.ContainersObserver.EventType.CHANGE, function(e) {
        goog.log.info(this.logger_, 'Container ' + e.container + '(' + e.containerType + ') changed ');
        if (e.containerType == 'todolist') {
          var s = this.containersSyncByUid.get(e.container);
          if (s) {
            s.needSync();
          } else {
            goog.log.warning("No syncservice for todolist " + e.container);
          }
        }
      });

};
goog.inherits(net.bluemind.todolist.service.TodolistsSyncManager, goog.events.EventTarget);

net.bluemind.todolist.service.TodolistsSyncManager.prototype.refresh = function() {
  goog.log.info(this.logger_, 'Todolist changed ');
  if (!this.ctx.service('todolists').isLocal()) return;

  this.ctx.service('folders').getFolders('todolist').then(function(todolists) {
    goog.array.forEach(todolists, function(todolist) {
      if (!this.containersSyncByUid.containsKey(todolist['uid'])) {
        var sync = new net.bluemind.todolist.sync.UnitaryTodoListSync(this.ctx, todolist['uid']);
        net.bluemind.sync.SyncEngine.getInstance().registerService(sync);
        this.containersSyncByUid.set(todolist['uid'], sync);
        sync.needSync();
      }
    }, this);

    this.containersSyncByUid.forEach(function(v, k) {
      var i = goog.array.findIndex(todolists, function(ab) {
        return ab['uid'] == k;
      });

      if (i < 0) {
        this.containersSyncByUid.remove(k);
        net.bluemind.sync.SyncEngine.getInstance().unregisterService(v);
      }
    }, this);
  }, null, this);
  this.ctx.service("todolists").synchronizeFolders();

}
