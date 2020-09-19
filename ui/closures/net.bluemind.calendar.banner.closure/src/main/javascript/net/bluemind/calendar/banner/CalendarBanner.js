goog.provide("net.bluemind.calendar.banner.CalendarBanner");

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

goog.require("goog.log");
goog.require("goog.date.DateTime");
goog.require("net.bluemind.date.DateTime");
goog.require("net.bluemind.calendar.api.CalendarClient");
goog.require("net.bluemind.calendar.api.CalendarsClient");
goog.require("net.bluemind.date.DateHelper");
goog.require("net.bluemind.container.service.ContainerObserver");
goog.require("net.bluemind.container.service.ContainersObserver");
goog.require('net.bluemind.core.container.api.ContainersClient');
goog.require("goog.async.Throttle");
goog.require("goog.Timer");
goog.require("goog.math");

net.bluemind.calendar.banner.CalendarBanner = function() {

  if (goog.global['bmcSessionInfos']['domain'] == 'global.virt') {
    return;
  }

  this.containersObserver = new net.bluemind.container.service.ContainersObserver();
  this.handler = new goog.events.EventHandler(this);
  this.rpc = new relief.rpc.RPCService(new relief.cache.Cache(), new goog.structs.Map({
    'X-BM-ApiKey' : goog.global['bmcSessionInfos']['sid']
  }));

  this.handler.listen(this.containersObserver, net.bluemind.container.service.ContainersObserver.EventType.CHANGE,
      this.changed);
  this.calendars = [];
  this.calendarsCounts = [];
  this.waitingCalendars = [];
  this.refreshThrottle = new goog.async.Throttle( function() {
    var cs = this.waitingCalendars;
    this.waitingCalendars = [];
    this.retrievePendingActionsForMany(cs);
  }, 5000, this);
  this.observeCalendars();
}

net.bluemind.calendar.banner.CalendarBanner.prototype.changed = function(e) {
  if (e.containerType == 'calendar') {
    this.waitingCalendars.push(e.container);
    goog.Timer.callOnce( function() {
      this.refreshThrottle.fire();
    }, goog.math.randomInt(5000), this);
  }
}

net.bluemind.calendar.banner.CalendarBanner.prototype.getFolderContainerUid_ = function() {
  return 'folders_' + goog.global['bmcSessionInfos']['userId'];
}

net.bluemind.calendar.banner.CalendarBanner.prototype.observeCalendars = function() {

  var containersClient = new net.bluemind.core.container.api.ContainersClient(this.rpc, '');
  containersClient.all({
    'type' : 'calendar',
    'verb' : [ 'All' ]
  }).then(function(containers) {
    this.waitingCalendars = [];
    var defaultCalendars = goog.array.filter(containers, function(cal) {
      return cal['defaultContainer'];
    });

    var cals = goog.array.map(defaultCalendars, function(container) {
      return container['uid'];
    });
    this.calendarsCounts = [];
    goog.array.forEach(defaultCalendars, function(container) {
      this.calendarsCounts[container['uid']] = 0;
    }, this);
    this.calendars = defaultCalendars;
    this.containersObserver.observerContainers('calendar', cals);
    console.log("watched calendars ", cals);
    this.retrievePendingActions();
  }, null, this);
}

net.bluemind.calendar.banner.CalendarBanner.prototype.retrievePendingActionsForMany = function(containerUids) {

  var defaultCalendars = goog.array.filter(this.calendars, function(c) {
    return goog.array.contains(containerUids, c['uid']);
  });
  
  goog.array.forEach(containerUids, function(containerUid) {
    this.calendarsCounts[containerUid] = 0;  
  }, this);

  this.retrievePendingActionsCals(defaultCalendars).then(function(totals) {
    var total = 0;
    goog.object.forEach(totals, function(ctotal, key) {
      this.calendarsCounts[key] = ctotal;
    }, this);

    goog.object.forEach(this.calendarsCounts, function(ctotal) {
      total += ctotal;
    });

    return total;
  }, null, this).then(function(total) {
    net.bluemind.calendar.banner.CalendarBanner.notifyListener(total);
  }, null, this);
}

net.bluemind.calendar.banner.CalendarBanner.prototype.retrievePendingActions = function() {
  goog.array.forEach(this.calendars, function(container) {
    this.calendarsCounts[container['uid']] = 0;
  }, this);

  this.retrievePendingActionsCals(this.calendars).then(function(totals) {
    var total = 0;
    goog.object.forEach(totals, function(ctotal, key) {
      this.calendarsCounts[key] = ctotal;
    }, this);
    goog.object.forEach(this.calendarsCounts, function(ctotal) {
      total += ctotal;
    });
    return total;
  }, null, this).then(function(total) {
    net.bluemind.calendar.banner.CalendarBanner.notifyListener(total);
  }, null, this);
}

net.bluemind.calendar.banner.CalendarBanner.prototype.retrievePendingActionsCals = function(cals) {

  var today = new net.bluemind.date.DateTime();

  var defaultCalendars = cals;

  var calendarsClient = new net.bluemind.calendar.api.CalendarsClient(this.rpc, '');
  var query = {
    'containers' : goog.array.map(defaultCalendars, function(cal) {
      return cal['uid'];
    }),
    'eventQuery' : {
      'dateMin' : new net.bluemind.date.DateHelper().toBMDateTime(today),
      'attendee' : {
        'dir' : null,
        'calendarOwnerAsDir' : true,
        'partStatus' : 'NeedsAction'
      }
    }
  };

  console.log("query ", query, calendarsClient);
  var future = calendarsClient.search(query).then(
      function(res) {
        console.log("res ", res);
        var total = 0;
        var totals = [];
        goog.array.forEach(res, function(series) {
          var cal = goog.array.find(defaultCalendars, function(c) {
            return c['uid'] == series["containerUid"]
          });
          console.log("cal ", cal, " for ", series, " from ", defaultCalendars);
          if (series["value"]["main"]
              && net.bluemind.calendar.banner.CalendarBanner.attends(series["value"]["main"], 'bm://'
                  + cal['ownerDirEntryPath'])) {
            if (!totals[cal['uid']]) {
              totals[cal['uid']] = 0;
            }
            totals[cal['uid']] += 1;
          }
          goog.array.forEach(series["value"]["occurrences"], function(vevent) {
            if (net.bluemind.calendar.banner.CalendarBanner.attends(vevent, 'bm://' + cal['ownerDirEntryPath'])) {
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

net.bluemind.calendar.banner.CalendarBanner.notifyListener = function(pendingActions) {
  if (net.bluemind.calendar.banner.CalendarBanner.listener) {
    net.bluemind.calendar.banner.CalendarBanner.listener.apply(null, [ pendingActions ]);
  }
}

net.bluemind.calendar.banner.CalendarBanner.attends = function(vevent, dir) {
  var attendee = goog.array.find(vevent["attendees"], function(attendee) {
    return dir == attendee["dir"];
  }, this);

  if (attendee && attendee['partStatus'] == 'NeedsAction') {
    return true;
  } else {
    return false;
  }
}
net.bluemind.calendar.banner.CalendarBanner.listener;

goog.global['calendarPendingActions'] = function(receiver) {
  if (net.bluemind.calendar.banner.CalendarBanner.listener) {
      net.bluemind.calendar.banner.CalendarBanner.listener = receiver;
      net.bluemind.calendar.banner.CalendarBanner.widget.retrievePendingActions();
      console.error('Calendar pending listener is already registered. It should not be called more than once.')
  } else {
    net.bluemind.calendar.banner.CalendarBanner.listener = receiver;
    net.bluemind.calendar.banner.CalendarBanner.widget = new net.bluemind.calendar.banner.CalendarBanner();
  }
}
