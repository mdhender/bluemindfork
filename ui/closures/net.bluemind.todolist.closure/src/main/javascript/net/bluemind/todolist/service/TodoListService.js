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
goog.provide("net.bluemind.todolist.service.TodoListService");

goog.require("goog.array");
goog.require("goog.date.Interval");
goog.require("goog.events.EventHandler");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.date.DateHelper");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.todolist.api.TodoListClient");
/**
 * Service provdier object for Tasks
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
/**
 * 
 */
net.bluemind.todolist.service.TodoListService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.handler_ = new goog.events.EventHandler(this);
  this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'todolist');
  this.handler_.listen(this.cs_, net.bluemind.container.service.ContainerService.EventType.CHANGE, this.handleChange_);
};
goog.inherits(net.bluemind.todolist.service.TodoListService, goog.events.EventTarget);

net.bluemind.todolist.service.TodoListService.prototype.handleByState = function(states, params) {
  var localState = [];
  if (this.cs_.available()) {
    localState.push('local');
  }
  if (this.ctx.online) {
    localState.push('remote');
  }

  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
};

net.bluemind.todolist.service.TodoListService.prototype.isLocal = function() {
  return this.cs_.available();
};

net.bluemind.todolist.service.TodoListService.prototype.handleChange_ = function() {
  this.dispatchEvent(net.bluemind.container.service.ContainerService.EventType.CHANGE);
}
/**
 * Get all tasks from a container.
 * 
 * @param {string} uid Container uid
 * @param {number=} opt_offset Optional offset number (default 0).
 * @return {goog.async.Deferred} Deferred object containing folder list.
 *         override
 */
net.bluemind.todolist.service.TodoListService.prototype.getItems = function(uid, opt_offset) {
  return this.handleByState({
    'local' : this.getItemsLocal,
    'remote' : this.getItemsRemote
  }, [ uid, opt_offset ]);
}

net.bluemind.todolist.service.TodoListService.prototype.getItemsLocal = function(uid, opt_offset) {
  return this.cs_.getItems(uid, opt_offset);
};

net.bluemind.todolist.service.TodoListService.prototype.getItemsRemote = function(uid, opt_offset) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', uid);
  return client.search({
    'from' : opt_offset,
    'size' : 10000,
    'query' : null
  }).then(function(result) {
    return goog.array.map(result['values'], function(value) {
      value['container'] = uid;
      return value;
    })
  });
}

net.bluemind.todolist.service.TodoListService.prototype.create = function(vtodo) {
  this.sanitize(vtodo);
  return this.handleByState({
    'local,remote' : this.createLocalRemote, //
    'local' : this.createLocal, //
    'remote' : this.createRemote
  }, [ vtodo ]);
}

net.bluemind.todolist.service.TodoListService.prototype.createLocalRemote = function(vtodo) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', vtodo['container']);
  return client.create(vtodo['uid'], vtodo['value']).then(function() {
    return this.cs_.storeItemWithoutChangeLog(vtodo);
  }, null, this);
}

net.bluemind.todolist.service.TodoListService.prototype.createLocal = function(vtodo) {
  return this.cs_.storeItem(vtodo);
}

net.bluemind.todolist.service.TodoListService.prototype.createRemote = function(vtodo) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', vtodo['container']);
  return client.create(vtodo['uid'], vtodo['value']);
}

net.bluemind.todolist.service.TodoListService.prototype.update = function(task) {
  this.sanitize(task);
  return this.handleByState({
    'local,remote' : this.updateLocalRemote, //
    'local' : this.updateLocal, //
    'remote' : this.updateRemote
  }, [ task ]);
}

net.bluemind.todolist.service.TodoListService.prototype.updateLocalRemote = function(vtodo) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', vtodo['container']);
  return client.update(vtodo['uid'], vtodo['value']).then(function() {
    return this.cs_.storeItemWithoutChangeLog(vtodo);
  }, null, this);
}

net.bluemind.todolist.service.TodoListService.prototype.updateLocal = function(vtodo) {
  return this.cs_.storeItem(vtodo);
}

net.bluemind.todolist.service.TodoListService.prototype.updateRemote = function(vtodo) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', vtodo['container']);
  return client.update(vtodo['uid'], vtodo['value']);
}

net.bluemind.todolist.service.TodoListService.prototype.deleteItem = function(container, uid) {
  return this.handleByState({
    'local,remote' : this.deleteLocalRemote, //
    'local' : this.deleteLocal, //
    'remote' : this.deleteRemote
  }, [ container, uid ]);
}

net.bluemind.todolist.service.TodoListService.prototype.deleteLocalRemote = function(container, uid) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', container);
  return client.delete_(uid).then(function() {
    return this.cs_.deleteItemWithoutChangeLog(container, uid);
  }, null, this);
}

net.bluemind.todolist.service.TodoListService.prototype.deleteLocal = function(container, uid) {
  return this.cs_.deleteItem(container, uid);
}

net.bluemind.todolist.service.TodoListService.prototype.deleteRemote = function(container, uid) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', container);
  return client.delete_(uid);
}

/**
 * Get task by uid.
 * 
 * @param {string} container Task container uid.
 * @param {string} uid Task uid.
 * @return {goog.async.Deferred} With task object as result.
 */
net.bluemind.todolist.service.TodoListService.prototype.getItem = function(container, uid) {
  return this.handleByState({
    'local' : this.getItemLocal,
    'remote' : this.getItemRemote
  }, [ container, uid ]);
}
net.bluemind.todolist.service.TodoListService.prototype.getItemLocal = function(container, uid) {
  return this.cs_.getItem(container, uid);
}

net.bluemind.todolist.service.TodoListService.prototype.getItemRemote = function(container, uid) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', container);
  return client.getComplete(uid).then(function(value) {
    value['container'] = container;
    return value;
  });
}

net.bluemind.todolist.service.TodoListService.prototype.getItemHistory = function(container, uid) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', container);
  return client.itemChangelog(uid, 0);
}

/**
 * Return all vevent inside a range
 * 
 * @param {string} containerId Container id
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @return {goog.Promise.<Array.<Object>>} Vevents object matching request
 */
net.bluemind.todolist.service.TodoListService.prototype.getVTodos = function(containerId, range) {
  return this.handleByState({
    'local' : this.getVTodosLocal, //
    'remote' : this.getVTodosRemote
  }, [ containerId, range ]);
};

/**
 * Return all container event inside a range
 * 
 * @param {string} containerId Container id
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListService.prototype.getVTodosLocal = function(containerId, range) {
  var query = [];
  query.push([ 'container, end', '>=', [ containerId, range.getStartDate().toIsoString() ], '<=',
      [ containerId, this.ctx.helper('date').getIsoEndOfTime() ] ]);
  query.push([ 'start', '<', range.getEndDate().toIsoString() ]);
  return this.cs_.searchItems(null, query);
};

/**
 * Return all container todos inside a range
 * 
 * @param {string} containerId Container id
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @return {goog.Promise}
 */
net.bluemind.todolist.service.TodoListService.prototype.getVTodosRemote = function(containerId, range) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', containerId);

  var query = {
    'dateMin' : new net.bluemind.date.DateHelper().toBMDateTime(range.getStartDate()),
    'dateMax' : new net.bluemind.date.DateHelper().toBMDateTime(range.getEndDate())
  };

  return client.search(query).then(function(res) {
    return goog.array.map(res['values'], function(item) {
      item['container'] = containerId;
      return item;
    })
  });
};

net.bluemind.todolist.service.TodoListService.prototype.copyItem = function(containerId, id, toContainerId) {
  return this.handleByState({
    'local,remote' : this.copyItemLocalRemote, //
    'local' : this.copyItemLocal, //
    'remote' : this.copyItemRemote
  }, [ containerId, id, toContainerId ]);
  // FIXME handle remote/local

};

net.bluemind.todolist.service.TodoListService.prototype.copyItemLocal = function(containerId, id, toContainerId) {
  return this.cs_.copyItem(containerId, id, toContainerId);
}

net.bluemind.todolist.service.TodoListService.prototype.copyItemRemote = function(containerId, id, toContainerId) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', containerId);
  return client.copy([ id ], toContainerId);
}

net.bluemind.todolist.service.TodoListService.prototype.copyItemLocalRemote = function(containerId, id, toContainerId) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', containerId);
  return client.copy([ id ], toContainerId).then(function() {
    client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', toContainerId);
    return client.getComplete(id);
  }, null, this).then(function(value) {
    var entry = {};
    entry['uid'] = value['uid'];
    entry['container'] = toContainerId;
    entry['value'] = value['value'];
    entry['displayName'] = value['displayName'];
    entry['name'] = value['displayName'];
    return this.cs_.storeItemWithoutChangeLog(entry);
  }, null, this);
}

net.bluemind.todolist.service.TodoListService.prototype.moveItem = function(containerId, id, toContainerId) {
  return this.handleByState({
    'local,remote' : this.moveItemLocalRemote, //
    'local' : this.moveItemLocal, //
    'remote' : this.moveItemRemote
  }, [ containerId, id, toContainerId ]);
};

net.bluemind.todolist.service.TodoListService.prototype.moveItemLocal = function(containerId, id, toContainerId) {
  return this.cs_.moveItem(containerId, id, toContainerId);
}

net.bluemind.todolist.service.TodoListService.prototype.moveItemRemote = function(containerId, id, toContainerId) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', containerId);
  return client.move([ id ], toContainerId);
}

net.bluemind.todolist.service.TodoListService.prototype.moveItemLocalRemote = function(containerId, id, toContainerId) {
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', containerId);
  return client.move([ id ], toContainerId).then(function() {
    return this.cs_.deleteItemWithoutChangeLog(containerId, id);
  }, null, this).then(function() {
    client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', toContainerId);
    return client.getComplete(id);
  }, null, this).then(function(value) {
    var entry = {};
    entry['uid'] = value['uid'];
    entry['container'] = toContainerId;
    entry['value'] = value['value'];
    entry['displayName'] = value['displayName'];
    entry['name'] = value['displayName'];
    return this.cs_.storeItemWithoutChangeLog(entry);
  }, null, this);
}

/**
 * Search todo
 * 
 * @param {string} containerId Container id
 * @param {string} pattern Search pattern
 * @param {Array.<goog.date.Date>=} opt_limits
 * @param {net.bluemind.date.Date=} opt_date Today
 * @param {boolean} opt_recur Process recurrences
 * @return {goog.Promise.<Array.<Object>>} Vevents object matching request
 */
net.bluemind.todolist.service.TodoListService.prototype.search = function(containerId, pattern, opt_limit, opt_date,
    opt_recur) {
  var date = opt_date || new net.bluemind.date.Date();
  var limit = opt_limit || [];
  if (!limit[0]) {
    limit[0] = date.clone();
    limit[0].add(new goog.date.Interval(-1));
  }
  if (!limit[1]) {
    limit[1] = date.clone();
    limit[1].add(new goog.date.Interval(1));
  }
  var query = {
    'query' : pattern,
    'dateMin' : this.ctx.helper('date').toBMDateTime(new net.bluemind.date.DateTime(limit[0])),
    'dateMax' : this.ctx.helper('date').toBMDateTime(new net.bluemind.date.DateTime(limit[1]))
  }
  var client = new net.bluemind.todolist.api.TodoListClient(this.ctx.rpc, '', containerId);
  return client.search(query).then(function(result) {
    return goog.array.map(result['values'], function(value) {
      value['container'] = containerId;
      value['name'] = value['displayName'];
      return value;
    });
  })
};

net.bluemind.todolist.service.TodoListService.prototype.sanitize = function(vtodo) {
  var helper = this.ctx.helper('date');

  vtodo['name'] = vtodo['displayName'] || vtodo['value']['summary'];
  if (!vtodo['value']['due']) {
    return vtodo;
  }
  var start = helper.fromIsoString(vtodo['value']['due']['iso8601']);
  vtodo['order'] = start.toIsoString();
  vtodo['start'] = start.toIsoString();

  if (!!vtodo['value']['rrule']) {
    if (!!vtodo['value']['rrule']['until']) {
      var end = helper.fromIsoString(vtodo['value']['rrule']['until']['iso8601']);
      end.add(new goog.date.Interval(0, 0, 0, 0, 0, -1));
      vtodo['end'] = end.toIsoString();
    } else if (!!vtodo['value']['rrule']['count']) {
      // TODO: Try to figure a date instead.
      // This might require to use RRULE.
      vtodo['end'] = helper.getIsoEndOfTime();
    } else {
      vtodo['end'] = helper.getIsoEndOfTime();
    }
  } else {
    var end = start.clone();
    end.add(new goog.date.Interval(0, 0, 1));
    vtodo['end'] = end.toIsoString();
  }
  return vtodo;
};

/**
 * Retrieve local changes
 * 
 * @param {string} containerId Container id
 * @return {goog.Promise.<Array.<Object>>} Changes object matching request
 */
net.bluemind.todolist.service.TodoListService.prototype.getLocalChangeSet = function(containerId) {
  return this.handleByState({
    'local,remote' : function(containerId) {
      return this.cs_.getLocalChangeSet(containerId);
    }, //
    'local' : function(containerId) {
      return this.cs_.getLocalChangeSet(containerId);
    }, //
    'remote' : function(containerId) {
      return goog.Promise.resolve([]);
    }
  }, [ containerId ]);
  return;
};
