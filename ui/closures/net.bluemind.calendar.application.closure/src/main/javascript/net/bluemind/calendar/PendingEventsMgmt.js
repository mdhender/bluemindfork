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

goog.provide("net.bluemind.calendar.PendingEventsMgmt");
goog.require("goog.events.EventHandler");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.core.container.api.ContainerManagementClient");
goog.require("net.bluemind.calendar.api.CalendarClient");
goog.require("net.bluemind.calendar.api.CalendarsClient");
goog.require("net.bluemind.container.service.ContainersObserver");
/**
 * Service provider object for PendingEventsMgmt
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.calendar.PendingEventsMgmt = function(ctx) {
  goog.base(this);
  this.ctx = ctx;

  this.containersObserver = new net.bluemind.container.service.ContainersObserver();
  this.handler = new goog.events.EventHandler(this);
  this.totalPendingEventsCache_ = null;
  this.totalPendingCountersCache_ = null;
  this.handler.listen(this.containersObserver, net.bluemind.container.service.ContainersObserver.EventType.CHANGE,
      function(e) {
        if (e.containerType == 'calendar') {
          this.retrievePendingActionsForOne(e.container).then(function() {
            this.retrievePendingCountersForOne(e.container).then(function() {
              this.dispatchEvent('change');
            }, null, this);
          }, null, this);
        }
      });

  this.calendars = [];
  this.calendarsCounts = [];
  this.calendarsPendingCounts = [];
  this.observeCalendars();
};
goog.inherits(net.bluemind.calendar.PendingEventsMgmt, goog.events.EventTarget);

/**
 * @type {number}
 * @private
 */
net.bluemind.calendar.PendingEventsMgmt.prototype.totalPendingEventsCache_;

/**
 * @type {number}
 * @private
 */
net.bluemind.calendar.PendingEventsMgmt.prototype.totalPendingCountersCache_;

net.bluemind.calendar.PendingEventsMgmt.prototype.getFolderContainerUid_ = function() {
  return 'folders_' + this.ctx.user['uid'];
}

net.bluemind.calendar.PendingEventsMgmt.prototype.observeCalendars = function() {

  var containersClient = new net.bluemind.core.container.api.ContainersClient(this.ctx.rpc, '');
  containersClient.all({
    'type' : 'calendar',
    'verb' : [ 'All' ]
  }).then(function(containers) {

    var defaultCalendars = goog.array.filter(containers, function(cal) {
      return cal['defaultContainer'];
    });

    var cals = goog.array.map(defaultCalendars, function(container) {
      return container['uid'];
    });

    this.calendars = defaultCalendars;
    this.calendarsCounts = [];
    goog.array.forEach(this.calendars, function(container) {
      this.calendarsCounts[container['uid']] = 0;
      this.calendarsPendingCounts[container['uid']] = 0;
    }, this);
    this.containersObserver.observerContainers('calendar', cals);
    this.retrievePendingActions_().then(function() {
      this.retrievePendingCounters_().then(function() {
        this.dispatchEvent('change');
      }, null, this);
    }, null, this);
  }, null, this);
}

net.bluemind.calendar.PendingEventsMgmt.prototype.getCalendars = function() {
  return this.calendars;
}

net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingActions = function() {
  return goog.Promise.resolve(this.totalPendingEventsCache_);
}

net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingCounters = function() {
  return goog.Promise.resolve(this.totalPendingCountersCache_);
}

net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingActions_ = function() {
  this.calendarsCounts = [];
  goog.array.forEach(this.calendars, function(container) {
    this.calendarsCounts[container['uid']] = 0;
  }, this);

  return this.retrievePendingActionsCals(this.calendars).then(function(totals) {
    var total = 0;
    goog.object.forEach(totals, function(ctotal, key) {
      this.calendarsCounts[key] = ctotal;
    }, this);

    goog.object.forEach(this.calendarsCounts, function(ctotal) {
      total += ctotal;
    });
    return total;
  }, null, this).then(function(total) {
    this.totalPendingEventsCache_ = total;
  }, null, this);
}

net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingCounters_ = function() {
  this.calendarsPendingCounts = [];
  goog.array.forEach(this.calendars, function(container) {
    this.calendarsPendingCounts[container['uid']] = 0;
  }, this);

  return this.retrievePendingCountersCals(this.calendars).then(function(totals) {
    var total = 0;
    goog.object.forEach(totals, function(ctotal, key) {
      this.calendarsPendingCounts[key] = ctotal;
    }, this);

    goog.object.forEach(this.calendarsPendingCounts, function(ctotal) {
      total += ctotal;
    });
    return total;
  }, null, this).then(function(total) {
    this.totalPendingCountersCache_ = total;
  }, null, this);
}

net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingActionsForOne = function(calUid) {
  this.calendarsCounts[calUid] = 0;
  var defaultCalendars = goog.array.filter(this.calendars, function(c) {
    return c['uid'] == calUid
  });

  return this.retrievePendingActionsCals(defaultCalendars).then(function(totals) {
    var total = 0;
    goog.object.forEach(totals, function(ctotal, key) {
      this.calendarsCounts[key] = ctotal;
    }, this);

    goog.object.forEach(this.calendarsCounts, function(ctotal) {
      total += ctotal;
    });


    return total;
  }, null, this).then(function(total) {
    this.totalPendingEventsCache_ = total;
  }, null, this);
}

net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingCountersForOne = function(calUid) {
  this.calendarsPendingCounts[calUid] = 0;
  var defaultCalendars = goog.array.filter(this.calendars, function(c) {
    return c['uid'] == calUid
  });

  return this.retrievePendingCountersCals(defaultCalendars).then(function(totals) {
    var total = 0;
    goog.object.forEach(totals, function(ctotal, key) {
      this.calendarsPendingCounts[key] = ctotal;
    }, this);

    goog.object.forEach(this.calendarsPendingCounts, function(ctotal) {
      total += ctotal;
    });


    return total;
  }, null, this).then(function(total) {
    this.totalPendingCountersCache_ = total;
  }, null, this);
}


net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingActionsCals = function(cals) {
  var today = new net.bluemind.date.DateTime();

  var defaultCalendars = cals;
  var calendarsClient = new net.bluemind.calendar.api.CalendarsClient(this.ctx.rpc, '');
  var query = {
    'containers' : goog.array.map(defaultCalendars, function(cal) {
      return cal['uid'];
    }),
    'eventQuery' : {
      'dateMin' : new net.bluemind.date.DateHelper().toBMDateTime(today),
      'attendee' : {
        'dir' : null,
        'partStatus' : 'NeedsAction',
        'calendarOwnerAsDir' : true
      }
    }
  };

  var future = calendarsClient.search(query).then(
      function(res) {
        var totals = [];
        goog.array.forEach(res, function(series) {
          var cal = goog.array.find(defaultCalendars, function(c) {
            return c['uid'] == series["containerUid"]
          });

          if (series["value"]["main"]
              && net.bluemind.calendar.PendingEventsMgmt.attends(series["value"]["main"], 'bm://'
                  + cal['ownerDirEntryPath'])) {
            if (!totals[cal['uid']]) {
              totals[cal['uid']] = 0;
            }
            totals[cal['uid']] += 1;
          }
          goog.array.forEach(series["value"]["occurrences"], function(vevent) {
            if (net.bluemind.calendar.PendingEventsMgmt.attends(vevent, 'bm://' + cal['ownerDirEntryPath'])) {
              if (!totals[cal['uid']]) {
                totals[cal['uid']] = 0;
              }
              totals[cal['uid']] += 1;
            }
          });
        });

        return totals;
      }, null, this);

  return future;
}

net.bluemind.calendar.PendingEventsMgmt.prototype.retrievePendingCountersCals = function(cals) {
  var defaultCalendars = cals;
  var calendarsClient = new net.bluemind.calendar.api.CalendarsClient(this.ctx.rpc, '');
  var calendars = goog.array.map(defaultCalendars, function(cal) {
      return cal['uid'];
    });

  var future = calendarsClient.searchPendingCounters(calendars).then(
      function(res) {
        var totals = [];
        goog.array.forEach(res, function(series) {
          var cal = goog.array.find(defaultCalendars, function(c) {
            return c['uid'] == series["containerUid"]
          });

          if (!totals[cal['uid']]) {
            totals[cal['uid']] = 0;  
          }
          totals[cal['uid']] += series['value']['counters'].length;
        });

        return totals;
      }, null, this);

  return future;
}

net.bluemind.calendar.PendingEventsMgmt.attends = function(vevent, dir) {
  var attendee = goog.array.find(vevent["attendees"], function(attendee) {
    return dir == attendee["dir"];
  }, this);

  if (attendee) {
    if (attendee['partStatus'] == 'NeedsAction') {
      return true;
    }
  }
  return false;
}

/**
 * Retrieve pending vevents
 * 
 * @param {Object} calendar calendar container
 * @return {goog.Promise<Array<Object>>} Vevents object matching request
 */
net.bluemind.calendar.PendingEventsMgmt.prototype.getPendingEvents = function(cal) {
  var today = new net.bluemind.date.DateTime();
  var query = {
    'dateMin' : new net.bluemind.date.DateHelper().toBMDateTime(today),
    'attendee' : {
      'dir' : 'bm://' + cal['ownerDirEntryPath'],
      'partStatus' : 'NeedsAction'
    }
  }
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', cal['uid']);
  return client.search(query).then(function(result) {
    return goog.array.map(result['values'], function(value) {
      value['container'] = cal['uid'];
      value['name'] = value['displayName'];
      return value;
    });
  })
};

/**
 * Retrieve pending counters
 * 
 * @param {Object} calendar calendar container
 * @return {goog.Promise<Array<Object>>} Vevents object matching request
 */
net.bluemind.calendar.PendingEventsMgmt.prototype.getPendingCounters = function(cal) {
  var client = new net.bluemind.calendar.api.CalendarClient(this.ctx.rpc, '', cal['uid']);
  return client.searchPendingCounters().then(function(result) {
    return goog.array.map(result['values'], function(value) {
      value['container'] = cal['uid'];
      value['name'] = value['displayName'];
      return value;
    });
  })
};
