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
 * @fileoverview Provide services for calendars lists
 */

goog.provide("net.bluemind.calendar.service.CalendarService");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.date.Interval");
goog.require("goog.events.EventHandler");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.calendar.api.CalendarClient");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.date.DateHelper");
goog.require("net.bluemind.date.DateTime");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

/**
 * Service provider object for Calendar
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @extends {goog.events.EventTarget}
 * @constructor
 */
net.bluemind.calendar.service.CalendarService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'calendar');
  this.handler_ = new goog.events.EventHandler(this);
  this.handler_.listen(this.cs_, net.bluemind.container.service.ContainerService.EventType.CHANGE, function() {
    this.dispatchEvent(net.bluemind.container.service.ContainerService.EventType.CHANGE);
  });
};
goog.inherits(net.bluemind.calendar.service.CalendarService, goog.events.EventTarget);

net.bluemind.calendar.service.CalendarService.prototype.isLocal = function() {
  return this.cs_.available();
};

/**
 * Execute the right method depending on application state.
 * 
 * @param {Object.<string, Function>} states
 * @param {Array.<*>} params Array of function parameters
 * @return {!goog.Promise<*>}
 */
net.bluemind.calendar.service.CalendarService.prototype.handleByState = function(containerId, states, params) {
  return this.ctx.service('folders').getFolder(containerId).then(function(folder) {
    var localState = [];
    if (this.cs_.available() && folder && folder['offlineSync']) {
      localState.push('local');
    }
    if (this.ctx.online) {
      localState.push('remote');
    }

    return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
  }, null, this);
};

/**
 * Get item
 * 
 * @param {string} containerId Container uid
 * @param {string} id Item uid
 * @return {!goog.Promise<Object>}
 */
net.bluemind.calendar.service.CalendarService.prototype.getItem = function(containerId, id) {
  return this.handleByState(containerId, {
    'local' : this.getItemLocal, //
    'remote' : this.getItemRemote
  //
  }, [ containerId, id ]);
};

/**
 * Get item by event#value.uid
 * 
 * @param {string} containerId Container uid
 * @param {string} uid Event uid
 * @return {!goog.Promise<Object>}
 */
net.bluemind.calendar.service.CalendarService.prototype.getItemByEventUid = function(containerId, uid) {
  return this.handleByState(containerId, {
    'local' : this.getItemByEventUidLocal, //
    'remote' : this.getItemByEventUidRemote
  //
  }, [ containerId, uid ]);
};

/**
 * Get item by uid from local storage
 * 
 * @param {string} containerId
 * @param {string} uid
 * @return {!goog.Promise<Object>}
 */
net.bluemind.calendar.service.CalendarService.prototype.getItemByEventUidLocal = function(containerId, uid) {
  var query = [ [ 'value.uid', '=', uid ] ];
  return this.cs_.searchItems(containerId, query);
};

net.bluemind.calendar.service.CalendarService.prototype.getItemByEventUidRemote = function(containerId, uid) {
  var query = {
    'eventUid' : uid,
    'anyRecurId' : true
  }

  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', containerId);
  return client.search(query).then(function(result) {
    return goog.array.map(result['values'], function(value) {
      value['container'] = containerId;
      value['name'] = value['displayName'];
      return value;
    });
  })
};

/**
 * Get item from local storage
 * 
 * @param {string} containerId
 * @param {string} id
 * @return {!goog.Promise<Object>}
 */
net.bluemind.calendar.service.CalendarService.prototype.getItemLocal = function(containerId, id) {
  return this.cs_.getItem(containerId, id);
};

net.bluemind.calendar.service.CalendarService.prototype.getItemRemote = function(containerId, id) {
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', containerId);
  return client.getComplete(id).then(function(item) {
    if (goog.isDefAndNotNull(item['value'])) {
      item['name'] = item['displayName'];
      item['container'] = containerId;
      return item;
    }
    return null;
  });
};

/**
 * Create Vevent.
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.create = function(event, sendNotification) {
  this.sanitize(event);
  return this.handleByState( event['container'], {
    'local,remote' : this.createLocalRemote,
    'local' : this.createLocal,
    'remote' : this.createRemote
  }, [ event, sendNotification ]);
};

/**
 * Send item to BM Server then create item in local storage
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.createLocalRemote = function(event, sendNotification) {
  var value = event['value'];
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', event['container']);
  return client.create(event['uid'], value, sendNotification).then(function() {
    return this.cs_.storeItemWithoutChangeLog(event);
  }, null, this);
};

/**
 * Get the item history
 * 
 * @param {string} container id
 * @param {string} item uid
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.getItemHistory = function(containerId, uid) {
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', containerId);
  return client.itemChangelog(uid, 0);
};

/**
 * Send item creation to BM server.
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.createRemote = function(event, sendNotification) {
  var value = event['value'];
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', event['container']);
  return client.create(event['uid'], value, sendNotification);
};

/**
 * Create item in local storage. The item will be sent to server during next
 * sync.
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.createLocal = function(event, sendNotification) {
  event['sendNotification'] = sendNotification;
  return this.cs_.storeItem(event);
};

/**
 * Update VEvent
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.update = function(vseries, sendNotification) {
  this.sanitize(vseries);
  vseries['']
  return this.handleByState( vseries['container'], {
    'local,remote' : this.updateLocalRemote, //
    'local' : this.updateLocal, //
    'remote' : this.updateRemote
  }, [ vseries, sendNotification ]);
};

/**
 * Send vevent update to BM Server then store data into the local storage
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.updateLocalRemote = function(vseries, sendNotification) {
  var value = vseries['value'];
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', vseries['container']);
  return client.update(vseries['uid'], value, sendNotification).then(function() {
    return this.cs_.storeItemWithoutChangeLog(vseries);
  }, null, this);
};

/**
 * Send vevent update to BM Server.
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.updateRemote = function(vseries, sendNotification) {
  var value = vseries['value'];
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', vseries['container']);
  return client.update(vseries['uid'], value, sendNotification);
};

/**
 * Update item in local storage. The item will be sent to server during next
 * sync.
 * 
 * @param {Object} event Event object
 * @param {boolean} sendNotification Must send notification
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.updateLocal = function(vseries, sendNotification) {
  vseries['sendNotification'] = sendNotification;
  return this.cs_.storeItem(vseries);
};

/**
 * Return all vevent inside a range
 * 
 * @param {string} containerId Container id
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @return {goog.Promise<Array<Object>>} Vevents object matching request
 */
net.bluemind.calendar.service.CalendarService.prototype.getSeries = function(containerId, range) {
  return this.handleByState( containerId, {
    'local' : this.getSeriesLocal, //
    'remote' : this.getSeriesRemote
  }, [ containerId, range ]);
};

/**
 * Return all container event inside a range
 * 
 * @param {string} containerId Container id
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.getSeriesLocal = function(containerId, range) {
  var query = [];
  query.push([ 'container, end', '>=', [ containerId, range.getStartDate().toIsoString() ], '<=',
    [ containerId, this.ctx.helper('date').getIsoEndOfTime() ] ]);
  query.push([ 'start', '<', range.getEndDate().toIsoString() ]);
  return this.cs_.searchItems(null, query);
};

net.bluemind.calendar.service.CalendarService.prototype.getSeriesRemote = function(containerId, range) {
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', containerId);

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


/**
 * Delete item
 * 
 * @param {string} containerId
 * @param {string} id
 * @param {boolean} sendNotification
 * @param {boolean=} opt_restore If it's a exception restore original value
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.deleteItem = function(containerId, id, sendNotification,
    opt_restoreParentValue) {
  if (!opt_restoreParentValue) {
    var promise = this.getItem(containerId, id).then(function(item) {
      if (item['value']['recurid']) {
        return this.getItemByEventUid(containerId, item['value']['uid']).then(function(items) {
          var parent = goog.array.find(items, function(item) {
            return !(item['value']['recurid']) && !!item['value']['rrule'];
          });
          if (parent) {
            parent['value']['exdate'] = parent['value']['exdate'] || [];
            parent['value']['exdate'].push(item['value']['recurid']);
            return this.update(parent);
          }
        }, null, this)
      }
    }, null, this);
  } else {
    var promise = goog.Promise.resolve();
  }
  return promise.then(function() {
    return this.handleByState(containerId, {
      'local,remote' : this.deleteItemLocalRemote, //
      'local' : this.deleteItemLocal, //
      'remote' : this.deleteItemRemote
    }, [ containerId, id, sendNotification ]);
  }, null, this);
};

/**
 * Send item removal to BM server then remove it from local storage.
 * 
 * @param {string} containerId
 * @param {string} id
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.deleteItemLocalRemote = function(containerId, id,
    sendNotification) {

  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', containerId);
  return client.delete_(id, sendNotification).then(function() {
    return this.cs_.deleteItemWithoutChangeLog(containerId, id);
  }, null, this);
};

/**
 * Remove item from local storage, removal will be sent to server during next
 * sync.
 * 
 * @param {string} containerId
 * @param {string} id
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.deleteItemLocal = function(containerId, id, sendNotification) {
  return this.cs_.deleteItem(containerId, id);
};

/**
 * Send item removal to BM server.
 * 
 * @param {string} containerId
 * @param {string} id
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.deleteItemRemote = function(containerId, id, sendNotification) {
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', containerId);
  return client.delete_(id, sendNotification);
}

/**
 * Sanitize an item by adding automatically calculated field.
 * 
 * @param {Object} entry
 */
net.bluemind.calendar.service.CalendarService.prototype.sanitize = function(entry) {
  if (null == entry['value'] || entry['value']['main'] == null && goog.array.isEmpty(entry['value']['occurrences'])) {
    return entry;
  }
  var main = entry['value']['main'] || entry['value']['occurrences'][0];
  var helper = this.ctx.helper('date');
  entry['name'] = entry['displayName'] || main['summary'];

  var occurrences = this.flatten_(entry);
  var start = goog.array.reduce(occurrences, function(start, occurrence) {
    if (goog.isDefAndNotNull(occurrence)) {
      return (start < occurrence['dtstart']['iso8601']) ? start : occurrence['dtstart']['iso8601'];
    }
    return start
  });
  start = helper.fromIsoString(start);
  entry['order'] = start.toIsoString();
  entry['start'] = start.toIsoString();
  var end = goog.array.reduce(occurrences, function(end, occurrence) {
    if (goog.isDefAndNotNull(occurrence)) {
      return (end > occurrence['dtend']['iso8601']) ? end : occurrence['dtend']['iso8601'];
    }
    return end
  }, (main && main['rrule'] && (main['rrule']['until'] && main['rrule']['until']['iso8601'] || helper.getIsoEndOfTime(true)) || main['dtend']['iso8601']));
  end = helper.fromIsoString(end);
  end.add(new goog.date.Interval(0, 0, 0, 0, 0, -1));
  entry['end'] = end.toIsoString();
  return entry;
};

/**
 * If an event storage is raised to notify that the on calendar container has
 * changed (added, removed, renamed, ..) then this method rise a foldersChanged
 * (sick) event.
 * 
 * @param {goog.events.BrowserEvent} evt Storage event.
 */
net.bluemind.calendar.service.CalendarService.prototype.handleStorageChange_ = function(evt) {
  var e = evt.getBrowserEvent();
  if (e.key == 'calendars-sync') {
    this.dispatchEvent(net.bluemind.container.service.ContainerService.EventType.CHANGE);
  }
};

/**
 * Search vevents
 * 
 * @param {String} containerId Container id
 * @param {String} pattern Search pattern
 * @param {Array.<goog.date.Date>=} opt_limit
 * @param {net.bluemind.date.Date=} opt_date Today
 * @param {boolean} opt_recur Process recurrences
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarService.prototype.search = function(containerId, pattern, opt_limit, opt_date) {
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
    'dateMin' : this.ctx.helper('date').toBMDateTime(limit[0]),
    'dateMax' : this.ctx.helper('date').toBMDateTime(limit[1])
  }

  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', containerId);
  return client.search(query).then(function(result) {
    return goog.array.map(result['values'], function(value) {
      value['container'] = containerId;
      value['name'] = value['displayName'];
      return value;
    });
  })
};

/**
 * Retrieve local changes
 * 
 * @param {string} containerId Container id
 * @return {goog.Promise<Array<Object>>} Changes object matching request
 */
net.bluemind.calendar.service.CalendarService.prototype.getLocalChangeSet = function(containerId) {
  return this.handleByState(containerId, {
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
};

/**
 * 
 */
net.bluemind.calendar.service.CalendarService.prototype.flatten_ = function(vseries) {
  var occurrences = goog.array.clone(vseries['value']['occurrences']);
  if (goog.isDefAndNotNull(vseries['value']['main'])) {
    occurrences.unshift(vseries['value']['main']);
  }
  return occurrences;
};
