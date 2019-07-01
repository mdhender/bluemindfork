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
 * @fileoverview Provide services for task lists
 */

goog.provide("net.bluemind.calendar.service.CalendarsService");

goog.require("goog.events.EventHandler");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.core.container.api.ContainerManagementClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

/**
 * Service provdier object for Calendars
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.service.ContainersService}
 */
net.bluemind.calendar.service.CalendarsService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.handler_ = new goog.events.EventHandler(this);
  this.css_ = new net.bluemind.container.service.ContainersService(ctx, 'calendar');
  this.handler_.listen(this.css_, net.bluemind.container.service.ContainersService.EventType.CHANGE, function() {
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  });
};
goog.inherits(net.bluemind.calendar.service.CalendarsService, goog.events.EventTarget);

/**
 * Event handler
 * 
 * @type {goog.events.EventHandler}
 */
net.bluemind.calendar.service.CalendarsService.prototype.handler_;

/**
 * Is data locally stored
 * 
 * @return {boolean}
 */
net.bluemind.calendar.service.CalendarsService.prototype.isLocal = function() {
  return this.css_.available();
};

/**
 * Execute the right method depending on application state.
 * 
 * @param {Object.<string, Function>} states
 * @param {Array.<*>} params Array of function parameters
 * @return {!goog.Promise<*>}
 */
net.bluemind.calendar.service.CalendarsService.prototype.handleByState = function(states, params) {
  var localState = [];
  if (this.css_.available()) {
    localState.push('local');
  }
  if (this.ctx.online) {
    localState.push('remote');
  }

  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
};

/**
 * Get a set of calendars
 * 
 * @param {Array.<string>} uids Uids of calendar to get
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.listByUids = function(uids) {
  return this.handleByState({
    'local' : function(uids) {
      return this.css_.mget(uids);
    }, 
    'remote' : function(uids) {
      return this.ctx.service('folders').getFoldersRemote('calendar', uids);
    }
  }, [ uids ]).then(function(folders) {
    goog.array.sort(folders, function(a, b) {
      goog.array.defaultCompare(goog.array.indexOf(uids, a['uid']), goog.array.indexOf(uids, b['uid']));
    });
    return folders;
  });
};


/**
 * Get all stored calendars
 * 
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.list = function() {
  return this.css_.list('calendar');
}


net.bluemind.calendar.service.CalendarsService.prototype.get = function(uid) {
  return this.css_.get(uid);
};

net.bluemind.calendar.service.CalendarsService.prototype.removeCalendar = function(calUid) {
  return this.css_.remove(calUid);
}

net.bluemind.calendar.service.CalendarsService.prototype.updateCalendars = function(cals) {
  return this.css_.addMultiple(cals);
}
/** @override */
net.bluemind.calendar.service.CalendarsService.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.handler_.dispose();
  this.handler_ = null;
};

/**
 * Set calendar settings
 * 
 * @param {string} uid Calendar uid
 * @param {Object<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.setSettings = function(uid, settings) {
  return this.handleByState({
    'local,remote' : this.setSettingsLocalRemote, //
    'local' : this.setSettingsLocal, //
    'remote' : this.setSettingsRemote
  }, [ uid, settings ]);
};

/**
 * Send calendar settings to BM Server, then set it into local storage.
 * 
 * @param {string} uid Calendar uid
 * @param {Object<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.setSettingsLocalRemote = function(uid, settings) {
  var cMgmt = new net.bluemind.core.container.api.ContainerManagementClient(this.ctx.rpc, '', uid);
  return cMgmt.setPersonalSettings(settings).then(function() {
    return this.css_.setSettingsWithoutChangeLog(uid, settings);
  }, null, this);
};

/**
 * Set settings into local storage. The settings might never be sent to server.
 * 
 * @param {string} uid Calendar uid
 * @param {Object<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.setSettingsLocal = function(uid, settings) {
  return this.css_.setSettings(uid, settings);
};

/**
 * Send calendar settings to BM Server.
 * 
 * @param {string} uid Calendar uid
 * @param {Object<string, *>} settings Settings
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.setSettingsRemote = function(uid, settings) {
  var cMgmt = new net.bluemind.core.container.api.ContainerManagementClient(this.ctx.rpc, '', uid);
  return cMgmt.setPersonalSettings(settings);
};

/**
 * Search in calendars
 * 
 * @param {Object} query calendars query
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.search = function( containers, pattern, opt_limit, opt_date, opt_resultSize) {
  
  return this.handleByState({
    'local,remote' : this.searchRemote, //
    'local' : this.searchLocal, //
    'remote' : this.searchRemote
  }, [ containers,  pattern,  opt_limit, opt_date, opt_resultSize ]);
};

/**
 * Search in calendars
 * 
 * @param {Object} query calendars query
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.searchRemote = function(containers, pattern, opt_limit, opt_date, opt_resultSize) {
  var date = opt_date || new net.bluemind.date.Date();
  var limit = opt_limit || [];
  var limitSize = opt_resultSize || 100;
  if (!limit[0]) {
    limit[0] = date.clone();
    limit[0].add(new goog.date.Interval(-1));
  }
  if (!limit[1]) {
    limit[1] = date.clone();
    limit[1].add(new goog.date.Interval(1));
  }
  var query = {};
  if (goog.isArray(containers)) {
    query['containers'] = containers;
  } else {
    query['owner'] = containers;
  }
  query['eventQuery'] = {
    'query' : pattern,
    'dateMin' : this.ctx.helper('date').toBMDateTime(limit[0]),
    'dateMax' : this.ctx.helper('date').toBMDateTime(limit[1]),
    'size' : limitSize
  }
  var calendarsClient = new net.bluemind.calendar.api.CalendarsClient(this.ctx.rpc, '');
  return calendarsClient.search(query).then(function(result) {
    return goog.array.map(result, function(item) {
      item['container'] = item['containerUid'];
      return item;
    })
  }, null, this);
};

/**
 * Search in calendars
 * 
 * @param {Object} query calendars query
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.searchLocal = function(containers, pattern, opt_limit, opt_date) {
  var promise = goog.Promise.resolve({});
  return promise;
};



net.bluemind.calendar.service.CalendarsService.prototype.splitLocalRemote = function(containers) {
  if (this.css_.available()) {
    var lc = [];
    var rc = [];
    return goog.Promise.all(
    goog.array.map(containers, function(containerId) {
      return this.ctx.service('folders').getFolder(containerId).then(function(folder) {
        if( folder && folder['offlineSync']) {
          return { uid : containerId, offline : true };
        } else {
          return { uid : containerId, offline : false };
        }
      }, null, this);
    },this)).then(function(res) {
      goog.array.forEach(res, function( r) {
        if( r.offline == true) {
          lc.push(r.uid);
        } else {
          rc.push(r.uid);
        }
      });
      return { remote : rc, local : lc};
    });
  } else {
    return goog.Promise.resolve({ remote : rc, local : []});
  }
}
/**
 * Return all vevent inside a range
 * 
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @param {Array.<string>=} containers Containers uids
 * @return {goog.Promise<Array<Object>>} Vevents object matching request
 */
net.bluemind.calendar.service.CalendarsService.prototype.getSeries = function(range, containers) {
  return this.splitLocalRemote(containers).then( function( remoteAndLocal) {
    if (containers.length == 0) {
      return goog.Promise.resolve([]);
    } else if( remoteAndLocal.local.length == 0) {
      return this.getSeriesRemote(range,containers);
    } else if( remoteAndLocal.remote.length == 0) {
      return this.getSeriesLocal(range,containers);
    } else {
      return goog.Promise.all([this.getSeriesLocal(range, remoteAndLocal.local), this.getSeriesRemote(range, remoteAndLocal.remote)]).then(function(resArr) {
        return goog.array.flatten(resArr);
      }); 
    }
  }, null, this);
};

/**
 * Return all vevent inside a range
 * 
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @param {Array.<string>=} opt_containers Containers uids
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.getSeriesLocal = function(range, opt_containers) {
  var query = [], helper = this.ctx.helper("date"), tz = this.ctx.helper("timezone").getDefaultTimeZone();
  var start = range.getStartDate().clone(), end = range.getEndDate().clone();
  var isoStart = range.getStartDate().toIsoString(), isoEnd = range.getEndDate().toIsoString();
  start.add(new goog.date.Interval(0, 0, -1));
  end.add(new goog.date.Interval(0, 0, 1));
  query.push([ 'end', '>=', start.toIsoString() ]);
  query.push([ 'start', '<', end.toIsoString() ]);
  return this.css_.searchItems(query).then(function(series) {
    return goog.array.filter(series, function(vseries) {
      if (opt_containers && !goog.array.contains(opt_containers, vseries['container'])) {
        return false;
      }
      // Index might have been generated with a different timezone.
      var main = vseries['value']['main'] || vseries['value']['occurrences'][0];
      if (vseries['start'] >= isoEnd && !main['rrule'] && helper.create(main['dtstart'], tz).toIsoString() >= isoEnd) {
        return false;
      }
      if (vseries['end'] < isoStart && !main['rrule'] && helper.create(main['dtend'], tz).toIsoString() < isoStart) {
        return false;
      }
      return true;
    })
    return series;
     
  }, null, this);
};

/**
 * Return all vevent inside a range
 * 
 * @param {net.bluemind.date.DateRange} range Range of date to find events
 * @param {Array.<string>=} opt_containers Containers uids
 * @return {goog.Promise}
 */
net.bluemind.calendar.service.CalendarsService.prototype.getSeriesRemote = function(range, opt_containers) {
  var client = new net.bluemind.calendar.api.CalendarsClient(this.ctx.rpc, '');

  var query = {
    'containers' : opt_containers,
    'eventQuery' : {
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

/**
 * Retrieve local changes
 * 
 * @param {Array.<string>=} opt_containers Containers uids
 * @return {goog.Promise<Array<Object>>} Changes object matching request
 */
net.bluemind.calendar.service.CalendarsService.prototype.getLocalChangeSet = function(opt_containers) {
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
  }).then(function(changes) {
    if (opt_containers) {
      return goog.array.filter(changes, function(changes) {
        return goog.array.contains(opt_containers, changes['container']);
      })
    } 
    return changes;
  });
};

