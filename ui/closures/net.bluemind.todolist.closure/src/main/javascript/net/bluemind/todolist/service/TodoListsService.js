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

goog.provide("net.bluemind.todolist.service.TodoListsService");

goog.require("goog.events.EventHandler");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.core.container.api.ContainerManagementClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.sync.SyncEngine");
goog.require("net.bluemind.todolist.api.TodoListsClient");
goog.require("net.bluemind.container.service.ContainerService");

/**
 * Service provdier object for TaskLists
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 */
net.bluemind.todolist.service.TodoListsService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.handler = new goog.events.EventHandler(this);
  this.css_ = new net.bluemind.container.service.ContainersService(ctx, 'todolist');
  this.handler.listen(this.ctx.service('folders'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.synchronizeFolders);
  this.handler.listen(this.css_, net.bluemind.container.service.ContainersService.EventType.CHANGE, function() {
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  });
};
goog.inherits(net.bluemind.todolist.service.TodoListsService, goog.events.EventTarget);
/**
 * Event handler
 * 
 * @type {goog.events.EventHandler}
 */
net.bluemind.todolist.service.TodoListsService.prototype.handler_;

net.bluemind.todolist.service.TodoListsService.prototype.isLocal = function() {
  return this.css_.available();
};

/**
 * Execute the right method depending on application state.
 * 
 * @param {Object.<string, Function>} states
 * @param {Array} params Array of function parameters
 * @return {!goog.Promise}
 */
net.bluemind.todolist.service.TodoListsService.prototype.handleByState = function(states, params) {
  var localState = [];
  if (this.css_.available()) {
    localState.push('local');
  }
  if (this.ctx.online) {
    localState.push('remote');
  }

  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
};

net.bluemind.todolist.service.TodoListsService.prototype.synchronizeFolders = function() {
  this.ctx.service('folders').getFolders('todolist').then(function(todolists) {
    this.ctx.service('containersObserver').observerContainers('todolist', goog.array.map(todolists, function(td) {
      return td['uid'];
    }));
    this.css_.sync('todolist', todolists);
  }, null, this);
}

net.bluemind.todolist.service.TodoListsService.prototype.list = function() {
  return this.handleByState({
    'local,remote' : this.listLocal, //
    'local' : this.listLocal, //
    'remote' : this.listRemote
  }, []);
};

net.bluemind.todolist.service.TodoListsService.prototype.listLocal = function() {
  return this.css_.list('todolist');
}

net.bluemind.todolist.service.TodoListsService.prototype.listRemote = function() {
  return this.ctx.service('folders').getFolders('todolist');
}

net.bluemind.todolist.service.TodoListsService.prototype.get = function(uid) {
  return this.handleByState({
    'local,remote' : this.getLocal, //
    'local' : this.getLocal, //
    'remote' : this.getRemote
  }, [ uid ]);
};

net.bluemind.todolist.service.TodoListsService.prototype.getLocal = function(uid) {
  return this.css_.get(uid);
}

net.bluemind.todolist.service.TodoListsService.prototype.getRemote = function(uid) {
  var client = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
  return client.get(uid);
}

net.bluemind.todolist.service.TodoListsService.prototype.create = function(uid, descriptor) {
  var client = new net.bluemind.todolist.api.TodoListsClient(this.ctx.rpc, '');
  return client.create(uid, descriptor).then(function() {
    net.bluemind.sync.SyncEngine.getInstance().execute();
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  }, null, this);
};

net.bluemind.todolist.service.TodoListsService.prototype.update = function(uid, descriptor) {
  var client = this.ctx.client("containers");
  return client.update(uid, descriptor).then(function() {
    net.bluemind.sync.SyncEngine.getInstance().execute();
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  }, null, this);
}
net.bluemind.todolist.service.TodoListsService.prototype.delete_ = function(uid) {
  var client = new net.bluemind.todolist.api.TodoListsClient(this.ctx.rpc, '');
  return client.delete_(uid).then(function() {
    net.bluemind.sync.SyncEngine.getInstance().execute();
  });
};

/**
 * Set todolist settings
 * 
 * @param {string} uid Todolist uid
 * @param {Object.<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListsService.prototype.setSettings = function(uid, settings) {
  return this.handleByState({
    'local,remote' : this.setSettingsLocalRemote, //
    'local' : this.setSettingsLocal, //
    'remote' : this.setSettingsRemote
  }, [ uid, settings ]);
};

/**
 * Send todolist settings to BM Server, then set it into local storage.
 * 
 * @param {string} uid Todolist uid
 * @param {Object.<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListsService.prototype.setSettingsLocalRemote = function(uid, settings) {
  var cMgmt = new net.bluemind.core.container.api.ContainerManagementClient(this.ctx.rpc, '', uid);
  return cMgmt.setPersonalSettings(settings).then(function() {
    return this.css_.setSettingsWithoutChangeLog(uid, settings);
  }, null, this);
};

/**
 * Set settings into local storage. The settings might never be sent to server.
 * 
 * @param {string} uid Todolist uid
 * @param {Object.<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListsService.prototype.setSettingsLocal = function(uid, settings) {
  return this.css_.setSettings(uid, settings);
};

/**
 * Send todolist settings to BM Server.
 * 
 * @param {string} uid Todolist uid
 * @param {Object.<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListsService.prototype.setSettingsRemote = function(uid, settings) {
  var cMgmt = new net.bluemind.core.container.api.ContainerManagementClient(this.ctx.rpc, '', uid);
  return cMgmt.setPersonalSettings(settings);
};

/**
 * Retrieve local changes
 * 
 * @param {string} containerId Container id
 * @return {goog.Promise.<Array.<Object>>} Changes object matching request
 */
net.bluemind.todolist.service.TodoListsService.prototype.getLocalChangeSet = function() {
  return this.handleByState({
    'local,remote' : function() {
      return this.css_.getLocalChangeSet();
    }, //
    'local' : function() {
      return this.css_.getLocalChangeSet();
    }, //
    'remote' : function() {
      return goog.Promise.resolve([]);
    }
  }, []);
};

/**
 * Return all vevent inside a range
 * 
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @param {Array.<string>=} opt_containers Containers uids
 * @return {goog.Promise<Array<Object>>} Vevents object matching request
 */
net.bluemind.todolist.service.TodoListsService.prototype.getVTodos = function(range, opt_containers) {
  return this.handleByState({
    'local' : this.getVTodosLocal, //
    'remote' : this.getVTodosRemote
  }, [ range, opt_containers ]);
};

/**
 * Return all vevent inside a range
 * 
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @param {Array.<string>=} opt_containers Containers uids
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListsService.prototype.getVTodosLocal = function(range, opt_containers) {
  var query = [];
  query.push([ 'end', '>=', range.getStartDate().toIsoString() ]);
  query.push([ 'start', '<', range.getEndDate().toIsoString() ]);
  return this.css_.searchItems(query).then(function(vtodos) {
    if (opt_containers) {
      return goog.array.filter(vtodos, function(vtodo) {
        return goog.array.contains(opt_containers, vtodo['container']);
      })
    }
    return vtodos;

  }, null, this);
};

/**
 * Return all vevent inside a range
 * 
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @param {Array.<string>=} opt_containers Containers uids
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListsService.prototype.getVTodosRemote = function(range, opt_containers) {
  var client = new net.bluemind.todolist.api.TodoListsClient(this.ctx.rpc, '');

  var query = {
    'containers' : opt_containers,
    'vtodoQuery' : {
      'dateMin' : new net.bluemind.date.DateHelper().toBMDateTime(range.getStartDate()),
      'dateMax' : new net.bluemind.date.DateHelper().toBMDateTime(range.getEndDate())
    }
  };

  return client.search(query).then(function(res) {
    return goog.array.map(res, function(item) {
      item['container'] = item['containerUid'];
      return item;
    })
  });
};
