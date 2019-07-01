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

/** @fileoverview Presenter for the list view */

goog.provide("net.bluemind.calendar.list.ListPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.async.Delay");
goog.require("goog.date");
goog.require("goog.dom");
goog.require("goog.iter");
goog.require("goog.object");
goog.require("goog.date.Date");
goog.require("net.bluemind.date.DateRange");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeFormat.Format");
goog.require("goog.structs.Map");
goog.require("goog.ui.DatePicker.Events");
goog.require("net.bluemind.calendar.list.ListView");
goog.require("net.bluemind.calendar.vevent.VEventSeriesAdaptor");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.rrule.OccurrencesHelper");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.calendar.Messages");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.list.ListPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.list.ListView(ctx);
  this.registerDisposable(this.view_);
  this.adaptor_ = new net.bluemind.calendar.vevent.VEventSeriesAdaptor(ctx);
  this.todolists_ = new net.bluemind.calendar.vtodo.TodolistsManager(ctx);
  this.vtodos_ = new net.bluemind.calendar.vtodo.VTodoAdaptor(ctx);
  this.refreshView_ = new goog.async.Delay(this.setup, 100, this);
  this.registerDisposable(this.refreshView_);

};
goog.inherits(net.bluemind.calendar.list.ListPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.list.ListView}
 * @private
 */
net.bluemind.calendar.list.ListPresenter.prototype.view_;

/**
 * @type {net.bluemind.calendar.vevent.VEventAdaptor}
 * @private
 */
net.bluemind.calendar.list.ListPresenter.prototype.adaptor_;

/**
 * @type {net.bluemind.calendar.vtodo.TodolistsManager}
 * @private
 */
net.bluemind.calendar.list.ListPresenter.prototype.todolists_;

/**
 * @type {net.bluemind.calendar.vtodo.VTodoAdaptor}
 * @private
 */
net.bluemind.calendar.list.ListPresenter.prototype.vtodos_;

/** @override */
net.bluemind.calendar.list.ListPresenter.prototype.init = function() {
  this.handler.listen(this.view_, goog.ui.DatePicker.Events.CHANGE, this.goTo_)
  this.view_.dateFormat = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.FULL_DATE);
  this.view_.timeFormat = this.ctx.helper('dateformat').formatter.time;
  this.view_.render(goog.dom.getElement('content-body'));

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
net.bluemind.calendar.list.ListPresenter.prototype.setup = function() {
  // FIXME nearly duplicate code (DayPresenter)
  var range = this.ctx.session.get('range');
  var data = {}, vtodos;

  return this.ctx.service('calendarsMgmt').list('calendar').then(function(cals) {
    data.calendars = goog.array.map(cals, this.calendarToMV_, this);
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
    this.view_.calendars = data.calendars;
    this.view_.range = this.ctx.session.get('range');
    this.view_.date = this.ctx.session.get('date');
    this.view_.setModel(this.buildModelView_(data.vevents, range));
    this.view_.refresh();

  }, null, this).thenCatch(function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

/** @override */
net.bluemind.calendar.list.ListPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Build event model
 * 
 * @param {Array. <Object>} events Events list;
 * @param {net.bluemind.date.DateRange} range
 * @return {Object} Model View
 */
net.bluemind.calendar.list.ListPresenter.prototype.buildModelView_ = function(events, range) {
  var start = range.getStartDate();
  var end = range.getEndDate();
  var mv = [];

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

  goog.array.sort(events, function(e1, e2) {
    return goog.date.Date.compare(e1.dtstart, e2.dtstart);
  });
  goog.array.forEach(events, function(event) {
    var dtend = goog.date.min(end, event.dtend).clone();
    var dtstart = goog.date.max(start, event.dtstart).clone();

    if (!event.states.allday && goog.date.Date.compare(dtstart, dtend) == 0) {
      dtend.add(new goog.date.Interval(goog.date.Interval.MINUTES, 30));
    }

    var range = new net.bluemind.date.DateRange(dtstart, dtend);
    goog.iter.forEach(range.iterator(), function(date) {
      var entry = goog.object.clone(event);
      entry.date = date;
      mv.push(entry);
    }, this)
  }, this);
  return mv;
};

/**
 * Build calendar model for view
 * 
 * @param {Object} calendar
 * @private
 */
net.bluemind.calendar.list.ListPresenter.prototype.calendarToMV_ = function(calendar, index) {
  var mv = {};
  mv.name = calendar['name'];
  mv.uid = calendar['uid'];
  mv.states = {};
  mv.states.visible = calendar['metadata']['visible'];
  if( !this.ctx.online && !calendar['offlineSync']) {
    mv.states.visible = false;
  }
  mv.color = {
    background : calendar['metadata']['color'],
    foreground : net.bluemind.calendar.ColorPalette.textColor(calendar['metadata']['color'], -0.3)
  };
  mv.states.writable = !!calendar['writable'] && !calendar['readOnly'];
  mv.states.defaultCalendar = calendar['defaultContainer'];
  mv.states.main = !!calendar['defaultContainer'];
  mv.owner = calendar['owner'];
  if (calendar['dir'] && calendar['dir']['path']) {
    var dir = 'bm://' + calendar['dir']['path'];
    mv.dir = dir.toString();
  }
  mv.settings = calendar['settings'];
  mv.verbs = calendar['verbs'];
  return mv;
};

/**
 * Build event model for view
 * 
 * @param {Object} item
 * @param {goog.date.Date} date
 * @private
 */
net.bluemind.calendar.list.ListPresenter.prototype.vseriesToMV_ = function(calendar, changes, vseries) {
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
    model.tooltip = MSG_NOT_SYNCHRONIZED + " : " + model.summary;
  }
  goog.array.forEach(model.flat, function(vevent) {
    this.adaptVEvent_(vevent, model);
  }, this);
  return model.occurrences;
};

net.bluemind.calendar.list.ListPresenter.prototype.adaptVEvent_ = function(vevent, vseries) {
  vevent.start = vevent.dtstart;
  vevent.end = vevent.dtend;
  vevent.states.synced = vseries.states.synced;
  vevent.states.error = vseries.states.error;
  vevent.tooltip = vseries.tooltip;
  return vevent;
};

/**
 * Build calendar model for view
 * 
 * @param {Object} vevent Vevent json
 * @param {Object} calendar Calendar model view
 * @param {Object} calendar
 * @private
 */
net.bluemind.calendar.list.ListPresenter.prototype.adaptVTodo_ = function(changes, model) {
  if (model.due) {
    model.dtstart = model.due.clone();
    model.dtend = model.dtstart.clone();
    if (model.states.allday) {
      model.dtend.add(new goog.date.Interval(0, 0, 1));
    }
    model.start = model.dtstart;
    model.end = model.dtend;
    model.states.short = (model.duration < (7200 / this.slot));

    var change = goog.array.find(changes, function(change) {
      return (change['itemId'] == model.uid && change['container'] == model.calendar);
    })
    model.states.synced = !goog.isDefAndNotNull(change);
    model.states.error = !model.states.synced && change['type'] == 'error';
    if (!model.states.synced) {
      /** @meaning general.notice.notSynchronized */
      var MSG_NOT_SYNCHRONIZED = goog.getMsg("Not all modifications are synchronized with the server yet.");
      model.tooltip = MSG_NOT_SYNCHRONIZED + ":" + model.summary;
    }
  }
  return model
};
