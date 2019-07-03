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

goog.provide("net.bluemind.calendar.search.SearchPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
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
goog.require("net.bluemind.calendar.search.SearchView");
goog.require("net.bluemind.calendar.vevent.VEventSeriesAdaptor");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.calendar.Messages");
/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.search.SearchPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.search.SearchView(ctx);
  this.registerDisposable(this.view_);
  this.adaptor_ = new net.bluemind.calendar.vevent.VEventSeriesAdaptor(ctx);
  this.todolists_ = new net.bluemind.calendar.vtodo.TodolistsManager(ctx);
};
goog.inherits(net.bluemind.calendar.search.SearchPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.search.SearchView}
 * @private
 */
net.bluemind.calendar.search.SearchPresenter.prototype.view_;

/**
 * @type {net.bluemind.calendar.vevent.VEventAdaptor}
 * @private
 */
net.bluemind.calendar.search.SearchPresenter.prototype.adaptor_;

/**
 * @type {net.bluemind.calendar.vtodo.TodolistsManager}
 * @private
 */
net.bluemind.calendar.search.SearchPresenter.prototype.todolists_;

/** @override */
net.bluemind.calendar.search.SearchPresenter.prototype.init = function() {
  this.handler.listen(this.view_, goog.ui.DatePicker.Events.CHANGE, this.goTo_);
  this.handler.listen(this.view_, 'decreaseLowerLimit', this.decreaseLowerLimit_);
  this.handler.listen(this.view_, 'increaseUpperLimit', this.increaseUpperLimit_);
  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.ONLINE, this.setup);
  this.handler.listen(net.bluemind.net.OnlineHandler.getInstance(), goog.net.NetworkStatusMonitor.EventType.OFFLINE, this.setup);

  this.view_.dateFormat = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.FULL_DATE);
  this.view_.timeFormat = this.ctx.helper('dateformat').formatter.time;
  this.view_.render(goog.dom.getElement('content-body'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.search.SearchPresenter.prototype.setup = function() {
  // FIXME nearly duplicate code (DayPresenter)
  var pattern = this.ctx.params.get('pattern');
  pattern = this.ctx.helper('elasticsearch').escape(pattern);
  var date = this.ctx.session.get('date');
  var from = date.clone(), to = date.clone();
  this.ctx.params.containsKey('from');
  from.add(new goog.date.Interval(0, 0, this.ctx.params.get('from', -365)));
  to.add(new goog.date.Interval(0, 0, this.ctx.params.get('to', 365)));
  var range = new net.bluemind.date.DateRange(from, to);
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
    return this.ctx.service('calendars').search(data.visibleUids, pattern, [ from, to ], null, 100);
  }, null, this).then(function(series) {
    var ocsHelper = new net.bluemind.rrule.OccurrencesHelper();
    data.vevents = goog.array.flatten(goog.array.map(series, function(vseries) {
      return this.vseriesToMV_(data.calendars, data.changes, ocsHelper.expandSeries(this.ctx, vseries, range));
    }, this));
  }, null, this).then(function() {
    return this.todolists_.getTodolistsModelView();
  }, null, this).then(function(todolists) {
    goog.array.extend(data.calendars, todolists);
    return this.todolists_.searchTodolistsVTodos(todolists, pattern, this.searchLimits_, date);
  }, null, this).then(function(items) {
    vtodos = items;
    return this.ctx.service('todolists').getLocalChangeSet();
  }, null, this).then(function(changes) {
    goog.array.extend(data.vevents, goog.array.map(vtodos, goog.partial(this.adaptVTodo_, changes), this));
    this.view_.calendars = data.calendars;
    this.view_.date = this.ctx.session.get('date');
    this.view_.limits = [ from, to ];
    data.vevents = this.filterEventsByTag(data.vevents);
    this.view_.setModel(this.buildModelView_(data.vevents));
    this.view_.refresh();
  }, null, this).thenCatch(function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

net.bluemind.calendar.search.SearchPresenter.prototype.filterEventsByTag = function(events) {
  if (this.ctx.session.get('selected-tag') && this.ctx.session.get('selected-tag').length > 0) {
    events = goog.array.filter(events, function(event) {
      for (var i = 0; i < this.ctx.session.get('selected-tag').length; i++) {
        var tag = this.ctx.session.get('selected-tag')[i];
        for (var k = 0; k < event.tags.length; k++) {
          if (tag == event.tags[k].id) {
            return true;
          }
        }
      }
      return false;
    }, this);
  }
  return events;
}

/** @override */
net.bluemind.calendar.search.SearchPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Build event model
 * 
 * @param {Array. <Object>} events Events list;
 * @return {Object} Model View
 */
net.bluemind.calendar.search.SearchPresenter.prototype.buildModelView_ = function(events) {
  var mv = [];
  goog.array.sort(events, function(e1, e2) {
    return goog.date.Date.compare(e1.dtstart, e2.dtstart);
  });
  goog.array.forEach(events, function(event) {
    var range = new net.bluemind.date.DateRange(event.dtstart, event.dtend);
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
net.bluemind.calendar.search.SearchPresenter.prototype.calendarToMV_ = function(calendar) {
  var mv = {};
  mv.name = calendar['name'];
  mv.uid = calendar['uid'];
  mv.states = {};
  mv.states.visible = true;

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
  mv.states.main = (calendar['defaultContainer'] && this.ctx.user['uid'] == calendar['owner']);
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
net.bluemind.calendar.search.SearchPresenter.prototype.vseriesToMV_ = function(calendar, changes, vseries) {
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
 * Build event model for view
 * 
 * @param {Object} item
 * @param {goog.date.Date} date
 * @private
 */
net.bluemind.calendar.search.SearchPresenter.prototype.adaptVEvent_ = function(vevent, vseries) {
  vevent.start = vevent.dtstart;
  vevent.end = vevent.dtend;
  vevent.states.short = (vevent.duration < (7200 / this.slot));
  vevent.states.synced = vseries.states.synced;
  vevent.states.error = vseries.states.error;
  vevent.tooltip = vseries.tooltip || vevent.summary;
  return vevent;
};

/**
 * Adapt the vtodo model for day view
 * 
 * @param {Object} model VTodo model
 * @return {Object} VTodo model adapted for the day view
 * @private
 */
net.bluemind.calendar.search.SearchPresenter.prototype.adaptVTodo_ = function(changes, model) {
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
      return (change['itemId'] == model.id && change['container'] == model.calendar);
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

net.bluemind.calendar.search.SearchPresenter.prototype.decreaseLowerLimit_ = function() {
  var from = parseInt(this.ctx.params.get('from', -365));
  from -= 365;
  this.ctx.helper('url').redirect("?from=" + from, true);
};

net.bluemind.calendar.search.SearchPresenter.prototype.increaseUpperLimit_ = function() {
  var to = parseInt(this.ctx.params.get('to', 365), 10);
  to += 365;
  this.ctx.helper('url').redirect("?to=" + to, true);
}
