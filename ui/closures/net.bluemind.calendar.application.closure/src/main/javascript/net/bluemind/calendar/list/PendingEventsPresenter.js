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

/** @fileoverview Presenter for the list view */

goog.provide("net.bluemind.calendar.list.PendingEventsPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.date");
goog.require("goog.dom");
goog.require("goog.iter");
goog.require("goog.object");
goog.require("goog.date.Date");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeFormat.Format");
goog.require("goog.structs.Map");
goog.require("goog.ui.DatePicker.Events");
goog.require("net.bluemind.calendar.list.ListView");
goog.require("net.bluemind.calendar.vevent.VEventSeriesAdaptor");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.calendar.list.PendingEventsView");
goog.require("net.bluemind.calendar.day.ui.SendNotificationDialog");
goog.require("net.bluemind.rrule.OccurrencesHelper");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.list.PendingEventsPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.list.PendingEventsView(ctx);
  this.registerDisposable(this.view_);

  this.adaptor_ = new net.bluemind.calendar.vevent.VEventSeriesAdaptor(ctx);
  this.actions_ = new net.bluemind.calendar.vevent.VEventActions(ctx, this.adaptor_, function() {
    ctx.helper('url').reload();
  });

  this.actions_.setFeatures([ 'private', 'recurring', 'notification', 'note' ]);
  this.actions_.injectPopups(this.view_);
  this.registerDisposable(this.actions_);
};
goog.inherits(net.bluemind.calendar.list.PendingEventsPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.vevent.VEventActions}
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.actions_;

/**
 * @type {net.bluemind.calendar.list.PendingEventsView}
 * @private
 */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.view_;

/**
 * @type {net.bluemind.calendar.vevent.VEventAdaptor}
 * @private
 */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.adaptor_;

/** @override */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.init = function() {
  this.view_.dateFormat = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.FULL_DATE);
  this.view_.timeFormat = this.ctx.helper('dateformat').formatter.time;
  this.view_.render(goog.dom.getElement('content-body'));

  this.handler.listenWithScope(this.view_, net.bluemind.calendar.vevent.EventType.PART, this.actions_.participation, false,
      this.actions_);
  this.handler.listen(this.ctx.service('calendar'), net.bluemind.container.service.ContainerService.EventType.CHANGE,
      this.setup);
  this.handler.listen(this.ctx.service('calendars'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.setup);


  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.ONLINE, this.setup);
  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.OFFLINE, this.setup);
  this.handler.listen(this.ctx.service("pendingEventsMgmt"), 'change', this.setup);
  this.handler.listen(this.view_, net.bluemind.calendar.vevent.EventType.COUNTER_DETAILS, function(e) {
    this.actions_.counter(e);
  });
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.setup = function() {
  var data = {};
  var ocsHelper = new net.bluemind.rrule.OccurrencesHelper();
  return this.ctx.service('calendarsMgmt').list('calendar').then(function(applicationCalendars) {
    data.app = applicationCalendars;
     var pending = goog.array.map(this.ctx.service("pendingEventsMgmt").getCalendars(), function(cal) {
      return cal['uid']
    });
    return this.ctx.service('folders').getFoldersRemote(null, pending)
  }, null, this).then(function(calendars) {
    var calendars = goog.array.filter(calendars, function(cal) {
      return cal['defaultContainer'];
    });
    data.calendars = goog.array.map(calendars, function(calendar) {
      var cal = goog.array.find(data.app, function(cal) {
        return cal['uid'] == calendar['uid']
      });
      if (!cal) {
        return this.calendarToMV_(calendar);
      }
      return this.calendarToMV_(cal);

    }, this);
    this.actions_.setCalendars(data.calendars);
    var futures = goog.array.map(data.calendars, function(calendar) {
      return this.ctx.service('pendingEventsMgmt').getPendingEvents(calendar.container).then(function(series) {
        return goog.array.flatten(goog.array.map(series, function(vseries) {
          return this.vseriesToMV_(vseries, calendar);
        }, this));
      }, null, this);
    }, this);
    return goog.Promise.all(futures);
  }, null, this).then(function(results) {

    data.events = goog.array.flatten(results);

    if (data.events.length == 0) {
      this.ctx.helper('url').redirect('/', true);
    } else {
      goog.array.sort(data.events, function(e1, e2) {
        return goog.date.Date.compare(e1.dtstart, e2.dtstart);
      });
      this.view_.calendars = goog.array.filter(data.calendars, function(cal) {
        return cal.color;
      });
      this.view_.setModel(data.events);
      this.view_.refresh();
    }
  }, function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
  // Do not block rendering process with presenter renderer
};

/**
 * Build calendar model for view
 * 
 * @param {Object} calendar
 * @private
 */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.calendarToMV_ = function(calendar) {
  var mv = {};
  mv.name = calendar['name'];
  mv.uid = calendar['uid'];
  mv.states = {};
  mv.states.writable = !!calendar['writable'] && !calendar['readOnly'];
  mv.states.defaultCalendar = calendar['defaultContainer'];
  mv.states.main = (calendar['defaultContainer'] && this.ctx.user['uid'] == calendar['owner']);
  mv.owner = calendar['owner'];
  if (calendar['dir'] && calendar['dir']['path']) {
    var dir = 'bm://' + calendar['dir']['path'];
    mv.dir = dir.toString();
  }
  mv.states.visible = false;

  if (calendar['metadata']) {
    mv.states.visible = calendar['metadata']['visible'];
    mv.color = {
      background : calendar['metadata']['color'],
      foreground : net.bluemind.calendar.ColorPalette.textColor(calendar['metadata']['color'], -0.3)
    };
  }

  if( !this.ctx.online && !calendar['offlineSync']) {
    mv.states.visible = false;
  }

  mv.container = calendar;
  mv.settings = calendar['settings'];
  mv.verbs = calendar['verbs'];
  return mv;
};


/**
 * Build calendar model for view
 * 
 * @param {Object} vevent Vevent json
 * @param {Object} calendar Calendar model view
 * @param {Array} changes
 * @private
 */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.vseriesToMV_ = function(vseries, calendar) {
  var model = this.adaptor_.toModelView(vseries, calendar);

  var vevents = goog.array.filter(model.flat, function(vevent) {
    return vevent.participation == 'NeedsAction';
  })
  goog.array.forEach(vevents, function(vevent) {
    vevent.acceptCounters = model.acceptCounters;
    this.adaptVEvent_(vevent);
  }, this);
  return vevents;
};

/**
 * Build event model for view
 * 
 * @param {Object} item
 * @param {goog.date.Date} date
 * @private
 */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.adaptVEvent_ = function(item) {
  item.start = item.dtstart;
  item.end = item.dtend;
  item.states.short = (item.duration < (7200 / this.slot));

  return item;
};

/** @override */
net.bluemind.calendar.list.PendingEventsPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};
