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

goog.provide("net.bluemind.calendar.service.CalendarsSyncManager");

goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("net.bluemind.addressbook.api.AddressBooksClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("goog.events.EventHandler");
goog.require("goog.Promise");
goog.require("net.bluemind.sync.SyncEngine");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainersObserver.EventType");
goog.require("net.bluemind.calendar.sync.UnitaryCalendarSync");
/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 */
net.bluemind.calendar.service.CalendarsSyncManager = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.containersSyncByUid = new goog.structs.Map();
  this.handler = new goog.events.EventHandler(this);
  this.handler.listen(this.ctx.service('calendars'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.refresh);

  this.handler.listen(this.ctx.service('containersObserver'),
      net.bluemind.container.service.ContainersObserver.EventType.CHANGE, function(e) {
        if (e.containerType == 'calendar') {
          var s = this.containersSyncByUid.get(e.container);
          if (s) {
            s.needSync();
          } else {
            console.log("warn, no syncservice for calendar " + e.container);
          }
        }
      });

  this.handler.listen(this.ctx.service('folders'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.refreshSynchronizedCalendars_);
};
goog.inherits(net.bluemind.calendar.service.CalendarsSyncManager, goog.events.EventTarget);

net.bluemind.calendar.service.CalendarsSyncManager.prototype.refresh = function() {
  if (!this.ctx.service('calendars').isLocal()) return;
  this.ctx.service('calendars').list().then(function(calendars) {
    var observeContainers = false;
    goog.array.forEach(calendars, function(calendar) {
      if (!this.containersSyncByUid.containsKey(calendar['uid']) && calendar['offlineSync']) {
        observeContainers = true;
        var sync = new net.bluemind.calendar.sync.UnitaryCalendarSync(this.ctx, calendar['uid']);
        net.bluemind.sync.SyncEngine.getInstance().registerService(sync);
        this.containersSyncByUid.set(calendar['uid'], sync);
        sync.needSync();
      }
    }, this);

    this.containersSyncByUid.forEach(function(syncKey, uid) {
      var calendar = goog.array.find(calendars, function(calendar) {
        return calendar['uid'] == uid;
      });

      if (!calendar || !calendar['offlineSync']) {
        observeContainers = true;
        this.containersSyncByUid.remove(uid);
        net.bluemind.sync.SyncEngine.getInstance().unregisterService(syncKey);
      }
    }, this);

    if (observeContainers) {
      this.ctx.service('containersObserver').observerContainers('calendar', goog.array.map(calendars, function(c) {
        return c['uid'];
      }));
    }
  }, null, this);
};

net.bluemind.calendar.service.CalendarsSyncManager.prototype.refreshSynchronizedCalendars_ = function() {
  if (!this.ctx.service('calendars').isLocal()) return;

  var folders;
  var calendars;
  // maintain backgroundSync flag on calendars containers
  return this.ctx.service('folders').getFolders('calendar').then(function(localFolders) {
    folders = localFolders;
    return this.ctx.service('calendars').list();
  }, null, this).then(function(localCalendars) {
    calendars = localCalendars;
    var removals = goog.array.map(goog.array.filter(calendars, function(calendar) {
      return goog.array.findIndex(folders, function(folder) {
        return folder['uid'] == calendar['uid'];
      }) < 0;
    }), function(calendar) {
      return this.ctx.service('calendars').removeCalendar(calendar['uid']);
    }, this)
    return goog.Promise.all(removals);
  }, null, this).then(function() {
    return this.ctx.service('calendars').updateCalendars(folders)
  }, null, this).then(function() {
    this.refresh();
  }, null, this);
};
