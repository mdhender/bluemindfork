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

/** @fileoverview Presenter for the application search bar */

goog.provide("net.bluemind.calendar.day.DayPresenter");

goog.require("goog.Promise");
goog.require("goog.Uri");
goog.require("goog.array");
goog.require("goog.async.Delay");
goog.require("goog.date");
goog.require("goog.dom");
goog.require("goog.date.Date");
goog.require("goog.date.Interval");
goog.require("net.bluemind.calendar.ColorPalette");
goog.require("net.bluemind.calendar.day.DayView");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventActions");
goog.require("net.bluemind.calendar.vevent.VEventSeriesAdaptor");
goog.require("net.bluemind.calendar.vtodo.TodolistsManager");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.date.DateRange");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.rrule.OccurrencesHelper");


/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.day.DayPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.day.DayView(ctx, ctx.helper('dateformat').formatter);
  this.registerDisposable(this.view_);
  this.adaptor_ = new net.bluemind.calendar.vevent.VEventSeriesAdaptor(ctx);
  this.actions_ = new net.bluemind.calendar.vevent.VEventActions(ctx, this.adaptor_, function() {
    ctx.helper('url').reload();
  });
  this.actions_.setFeatures([ 'private', 'recurring', 'notification', 'note' ]);
  this.actions_.injectPopups(this.view_);
  this.registerDisposable(this.actions_);
  this.todolists_ = new net.bluemind.calendar.vtodo.TodolistsManager(ctx);
  this.refreshView_ = new goog.async.Delay(this.setup, 100, this);
  this.registerDisposable(this.refreshView_);


};
goog.inherits(net.bluemind.calendar.day.DayPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {number}
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.slot = 2;

/**
 * @type {net.bluemind.calendar.day.DayView}
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.view_;

/**
 * @type {net.bluemind.calendar.vevent.VEventAdaptor}
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.adaptor_;

/**
 * @type {net.bluemind.calendar.vevent.VEventActions}
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.actions_;

/**
 * @type {net.bluemind.calendar.vtodo.TodolistsManager}
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.todolists_;

/** @override */
net.bluemind.calendar.day.DayPresenter.prototype.init = function() {
  this.view_.range = this.ctx.session.get('range');
  this.view_.length = this.ctx.session.get('length');
  this.view_.date = this.ctx.session.get('date');
  this.view_.slot = this.slot;

  this.view_.workingHours = [ this.ctx.settings.get('work_hours_start'), this.ctx.settings.get('work_hours_end') ];
  this.view_.workingDays = this.ctx.settings.get('working_days').split(',');
  this.view_.tz = this.ctx.helper('timezone').getDefaultTimeZone();
  this.view_.autoscroll = true;
  this.view_.render(goog.dom.getElement('content-body'));

  
  this.handler.listenWithScope(this.view_, net.bluemind.calendar.vevent.EventType.PART, 
      this.actions_.participation, false, this.actions_);
  this.handler.listenWithScope(this.view_, net.bluemind.calendar.vevent.EventType.SAVE, this.actions_.save, false,
      this.actions_);
  this.handler.listenWithScope(this.view_, net.bluemind.calendar.vevent.EventType.REMOVE, this.actions_.remove, false,
      this.actions_);
  this.handler.listen(this.view_, net.bluemind.calendar.vevent.EventType.DETAILS, function(e) {
    var vevent = e.vevent;
    if (vevent.type == 'vtodo') {
      this.ctx.helper('url').goTo('/vtodo/consult?uid=' + vevent.uid + '&container=' + vevent.calendar);
    } else {
      this.actions_.details(e);
    }
  });

  this.handler.listen(this.view_, net.bluemind.calendar.vevent.EventType.REFRESH, function() {
      this.ctx.helper('url').reload();
  });
  var refresh = function() {
    this.refreshView_.start();
  };
  this.handler.listen(this.ctx.service('calendars'), net.bluemind.container.service.ContainersService.EventType.CHANGE, refresh);
  this.handler.listen(this.ctx.service('calendar'), net.bluemind.container.service.ContainerService.EventType.CHANGE, refresh);
  this.handler.listen(this.ctx.service('todolist'), net.bluemind.container.service.ContainerService.EventType.CHANGE, refresh);
  this.handler.listen(this.ctx.service('todolists'), net.bluemind.container.service.ContainerService.EventType.CHANGE, refresh);
  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.ONLINE, refresh);
  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.OFFLINE, refresh);
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.day.DayPresenter.prototype.setup = function() {
  this.refreshView_.stop();
  // FIXME duplicate code (DayPresenter)
  var range = this.ctx.session.get('range');
  var data = {}, vtodos;
  return this.ctx.service('calendarsMgmt').list('calendar').then(function(cals) {
    data.calendars = goog.array.map(cals, this.calendarToMV_, this);
    this.actions_.setCalendars(data.calendars);
    data.visibleUids = goog.array.map(goog.array.filter(data.calendars, function(calendar) {
      return calendar.states.visible;
    }), function(calendar) {
      return calendar.uid
    });
    return this.ctx.service('calendars').getLocalChangeSet(data.visibleUids);
  }, null, this).then(function(changes) {
    data.changes = changes;
    return this.ctx.service('calendars').getSeries(range, data.visibleUids);
  }, null, this).then(function(series) {
    var ocsHelper = new net.bluemind.rrule.OccurrencesHelper();
    data.vevents = goog.array.flatten(goog.array.map(series, function(vseries) {
      return this.vseriesToMV_(data.calendars, data.changes, ocsHelper.expandSeries(this.ctx, vseries, range));
    }, this));
  }, null, this).then(function() {
    return this.todolists_.getTodolistsModelView();
  }, null, this).then(function(todolists) {
    goog.array.extend(data.calendars, todolists);
    return this.todolists_.getTodolistsVTodos(todolists, range);
  }, null, this).then(function(items) {
    vtodos = items;
    return this.ctx.service('todolists').getLocalChangeSet();
  }, null, this).then(function(changes) {
    goog.array.extend(data.vevents, goog.array.map(goog.array.filter(vtodos, function(t) {
      return t.due != null;
    }), goog.partial(this.adaptVTodo_, changes), this));
  }, null, this).then(function() {
    var range = this.ctx.session.get('range');
    this.view_.calendars = data.calendars;
    this.view_.setModel(this.buildEventModelView_(data.vevents, range));
    this.view_.show(range, this.ctx.session.get('date'))
  }, null, this);
};

/** @override */
net.bluemind.calendar.day.DayPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Build event model
 * 
 * @param {Array.<Object>} events Events list;
 * @param {net.bluemind.date.DateRange} range
 * @return {Object} Model View
 */
net.bluemind.calendar.day.DayPresenter.prototype.buildEventModelView_ = function(events, range) {
  var mv = {
    days : {},
    weeks : {},
    allday : {}
  };
  var tags = this.ctx.session.get('selected-tag') || [];
  if (!goog.array.isEmpty(tags)) {
    // filter by tag
    events = goog.array.filter(events, function(event) {
      return goog.array.find(event.tags || [], function(cat) {
        return goog.array.contains(tags, cat.id);
      }) != null;
    });
  }

  // filter declined events
  if (this.ctx.settings.get('show_declined_events') == 'false') {
    events = goog.array.filter(events, function(event) {
      return event.participation != 'Declined';
    });
  }

  goog.array.forEach(events, function(event) {

    var dtstart = event.dtstart.clone();
    var dtend = event.dtend.clone();

    if (goog.date.Date.compare(dtstart, range.getEndDate()) >= 0
        || goog.date.Date.compare(dtend, range.getStartDate()) <= 0) {
      return;
    }

    if (!event.states.allday && goog.date.Date.compare(dtstart, dtend) == 0) {
      dtend.add(new goog.date.Interval(goog.date.Interval.MINUTES, 30));
    }
    var duration = new net.bluemind.date.DateRange(dtstart, dtend);

    if (!event.states.allday && duration.count() == 1) {
      if (!mv.days[dtstart.getDay()]) mv.days[dtstart.getDay()] = {};
      var date = dtstart.clone();
      var date = dtstart.clone();
      date.add(new goog.date.Interval(goog.date.Interval.MINUTES, - (date.getMinutes() % (60 / this.slot))));

      var interval = new goog.date.Interval(goog.date.Interval.MINUTES, 60 / this.slot);
      while (goog.date.Date.compare(date, dtend) < 0) {
        var slot = Math.floor((date.getHours() + date.getMinutes() / 60) * this.slot);
        if (!mv.days[dtstart.getDay()][slot]) mv.days[dtstart.getDay()][slot] = [];
        mv.days[dtstart.getDay()][slot].push(event);
        date.add(interval)
      }
    } else {
      event.start = goog.date.max(range.getStartDate(), duration.getStartDate()).clone();
      event.end = goog.date.min(range.getEndDate(), duration.getEndDate()).clone();
      event.right = goog.date.Date.compare(dtend, event.end) > 0;
      event.left = goog.date.Date.compare(dtstart, event.start) < 0;
      event.size = new net.bluemind.date.DateRange(event.start, event.end).count();
      if (!mv.weeks[event.start.getWeekNumber()]) mv.weeks[event.start.getWeekNumber()] = [];
      mv.weeks[event.start.getWeekNumber()].push(event);

      var interval = new goog.date.Interval(goog.date.Interval.DAYS, 1);
      var date = event.start.clone();
      while (goog.date.Date.compare(date, event.end) < 0) {
        if (!mv.allday[date.getDay()]) mv.allday[date.getDay()] = [];
        mv.allday[date.getDay()].push(event);
        date.add(interval)
      }
    }
  }, this);
  return mv;
};

/**
 * Build calendar model for view
 * 
 * @param {Object} calendar
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.calendarToMV_ = function(calendar) {

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
  mv.states.visible = calendar['metadata']['visible'];
  if( !this.ctx.online && !calendar['offlineSync']) {
    mv.states.visible = false;
  }

  mv.color = {
    background : calendar['metadata']['color'],
    foreground : net.bluemind.calendar.ColorPalette.textColor(calendar['metadata']['color'], -0.3)
  };
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
net.bluemind.calendar.day.DayPresenter.prototype.vseriesToMV_ = function(calendar, changes, vseries) {
  if (goog.isArray(calendar)) {
    calendar = goog.array.find(calendar, function(cal) {
      return vseries['container'] == cal.uid;
    })
  }
  
  var model = this.adaptor_.toModelView(vseries, calendar);
  
  var change = goog.array.find(changes, function(change) {
    return change['itemId'] == model.uid && change['container'] == model.calendar;
  });
  
  model.states.synced = !goog.isDefAndNotNull(change);
  model.states.error = !model.states.synced && change['type'] == 'error';
  if (!model.states.synced) {
    /** @meaning general.notice.notSynchronized */
    var MSG_NOT_SYNCHRONIZED = goog.getMsg("Not all modifications are synchronized with the server yet.");
    model.tooltip = MSG_NOT_SYNCHRONIZED + " " + model.summary;
  }
  
  goog.array.forEach(model.flat, function(vevent) {
    this.adaptVEvent_(vevent, model);
  }, this);
  return model.occurrences;
};

/**
 * Build calendar model for view
 * 
 * @param {Object} vevent Vevent json
 * @param {Object} calendar Calendar model view
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.adaptVEvent_ = function(vevent, vseries) {
  vevent.duration = (vevent.dtend.getTime() - vevent.dtstart.getTime()) / 1000;
  vevent.start = vevent.dtstart;
  vevent.end = vevent.dtend;

  vevent.states.short = (vevent.duration < (7200 / this.slot));

  vevent.formatted = {};
  var formatter = this.ctx.helper('dateformat');
  vevent.formatted.tstart = '';
  if (!vevent.states.allday) {
    vevent.formatted.tstart = formatter.formatTime(vevent.dtstart);
  }
  vevent.formatted.dstart = formatter.formatDate(vevent.dtstart);
  vevent.formatted.dend = formatter.formatDate(vevent.dtend);
  vevent.states.synced = vseries.states.synced;
  vevent.states.error = vseries.states.error;
  vevent.tooltip = vseries.tooltip || vevent.summary;
  
};

/**
 * Adapt the vtodo model for day view
 * 
 * @param {Object} model VTodo model
 * @return {Object} VTodo model adapted for the day view
 * @private
 */
net.bluemind.calendar.day.DayPresenter.prototype.adaptVTodo_ = function(changes, model) {
  if (model.due) {
    model.dtstart = model.due.clone();
    model.dtend = model.dtstart.clone();

    if (model.states.allday) {
      model.dtend.add(new goog.date.Interval(0, 0, 1));
    }

    model.duration = (model.dtend.getTime() - model.dtstart.getTime()) / 1000;
    model.start = model.dtstart;
    model.end = model.dtend;
    model.tooltip = model.summary;
    model.states.short = (model.duration < (7200 / this.slot));

    var change = goog.array.find(changes, function(change) {
      return (change['itemId'] == model.uid && change['container'] == model.calendar);
    })
    model.states.synced = !goog.isDefAndNotNull(change);
    model.states.error = !model.states.synced && change['type'] == 'error';
    if (!model.states.synced) {
      /** @meaning general.notice.notSynchronized */
      var MSG_NOT_SYNCHRONIZED = goog.getMsg("Not all modifications are synchronized with the server yet.");
      model.tooltip = MSG_NOT_SYNCHRONIZED + " : " + model.summary;
    }
  }
  return model
};
