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

goog.provide("net.bluemind.calendar.vtodo.VTodoAdaptor");

goog.require("goog.array");
goog.require("goog.i18n.DateTimeSymbols_en");
goog.require("net.bluemind.mvp.UID");

/**
 * Adaptor for VTodo BM-Core JSON to and from view model in calendar application
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @constructor
 */
net.bluemind.calendar.vtodo.VTodoAdaptor = function(ctx) {
  this.ctx_ = ctx;
};

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.vtodo.VTodoAdaptor.prototype.ctx_;

/**
 * Adapt a VTodo from BM-Core JSON to a object usable by views
 * 
 * @param {Object} vtodo vtodo json
 * @param {Object} todolist Todolist model view
 * @return {Object} Adapted vtodo for usage in the view.
 */
net.bluemind.calendar.vtodo.VTodoAdaptor.prototype.toModelView = function(vtodo, todolist) {
  var value = vtodo['value'];
  var helper = this.ctx_.helper("date");

  var model = {};
  model.type = 'vtodo';
  model.id = vtodo['uid'];
  model.uid = vtodo['uid'];

  if (vtodo['value']['dtstart']) {
    model.dtstart = this.ctx_.helper('date').create(vtodo['value']['dtstart']);
  }
  if (vtodo['value']['due']) {
    model.due = this.ctx_.helper('date').create(vtodo['value']['due']);
  }
  if (vtodo['value']['completed']) {
    model.completed = this.ctx_.helper('date').create(vtodo['value']['completed']);
  }
  model.percent = vtodo['value']['percent'];

  model.summary = value['summary'];
  model.attendees = goog.array.map(value['attendees'] || [], this.attendeeToModelView_, this);
  model.location = value['location'];
  model.organizer = value['organizer'];
  model.calendar = vtodo['container'];
  model.description = value['description'];
  model.transp = value['transparency'];
  model.class = value['classification'];
  model.status = value['status'];
  model.priority = value['priority'];
  model.tags = goog.array.map(value['categories'] || [], function(tag) {
    return {
      id : tag['itemUid'],
      container : tag['containerUid'],
      label : tag['label'],
      color : tag['color']
    };
  });

  model.alarm = goog.array.map(value['alarm'] || [], function(alarm) {
    return {
      'trigger' : (null != alarm.trigger) ? alarm.trigger * -1 : alarm.trigger,
      action : alarm['action']
    }
  });

  if (goog.isDefAndNotNull(value['recurid'])) {
    model.recurrenceId = helper.create(value['recurid']);
  }

  var attendee = goog.array.find(model.attendees, function(attendee) {
    return this.ownTodolist_(attendee, todolist.dir);
  }, this);

  if (attendee) {
    model.participation = attendee['partStatus'];
    model.attendee = {
      id : attendee['dir'],
      name : attendee['commonName']
    }
  }

  model.rrule = this.parseRRule_(vtodo);
  model.exdate = goog.array.map(vtodo['value']['exdate'] || [], function(exdate) {
    return helper.create(exdate);
  }, this);

  return this.updateStates(model, todolist);
};

/**
 * Convert an attendee
 * 
 * @param {Object} attendee
 * @return {Object}
 */
net.bluemind.calendar.vtodo.VTodoAdaptor.prototype.attendeeToModelView_ = function(attendee) {
  var ret = {
    'cutype' : attendee['cutype'],
    'member' : attendee['member'],
    'role' : attendee['role'],
    'partStatus' : attendee['partStatus'],
    'rsvp' : attendee['rsvp'],
    'delTo' : attendee['delTo'],
    'delFrom' : attendee['delFrom'],
    'sentBy' : attendee['sentBy'],
    'commonName' : attendee['commonName'],
    'lang' : attendee['lang'],
    'mailto' : attendee['mailto'],
    'dir' : attendee['dir'],
    'uri' : attendee['uri'],
    'internal' : attendee['internal']
  };

  if (ret['dir'] && goog.string.startsWith(ret['dir'], 'bm://')) {
    ret.icon = '/api/directory/' + this.ctx_.user['domainUid'] + '/_icon/'
        + encodeURIComponent(goog.string.removeAt(attendee['dir'], 0, 5));
  }
  return ret;
}

/**
 * Set or update states depending on model data.
 * 
 * @param {Object} model
 * @param {Object} todolist Todolist model view
 * @return {Object} updated model
 */
net.bluemind.calendar.vtodo.VTodoAdaptor.prototype.updateStates = function(model, todolist) {
  model.states = {};

  model.states.allday = !(model.dtstart instanceof goog.date.DateTime);
  model.states.private_ = (model.class != 'Public');
  model.states.busy = (model.transp == 'Opaque');
  model.states.meeting = !!model.attendees.length;
  model.states.master = !model.organizer && !model.states.meeting;
  model.states.master = model.states.master || this.ownTodolist_(model.organizer, todolist.dir);
  model.states.pending = (model.participation == 'NeedsAction');
  model.states.tentative = (model.participation == 'Tentative');
  model.states.declined = (model.participation == 'Declined');
  model.states.repeat = !!(model.rrule && model.rrule.freq);
  model.states.exception = goog.isDefAndNotNull(model.recurrenceId) && !model.states.repeat;
  model.states.forever = (model.states.repeat && !model.rrule.until && !model.rrule.count);
  model.states.past = model.due && model.due.getTime() < goog.now();
  model.states.updatable = (todolist.states.writable) && (!model.states.private_ || (todolist.owner == this.ctx_.user['uid']) || this.canAll_(todolist.verbs)) ;  
  model.states.attendee = goog.isDefAndNotNull(model.attendee) && model.states.meeting && !model.states.master
  model.states.removable = model.states.updatable && !!model.id;
  model.states.updating = model.states.updatable && !!model.id;
  model.states.done = model.status == 'Completed';
  return model;
};

net.bluemind.calendar.vtodo.VTodoAdaptor.prototype.canAll_ = function(verbs) {
  return verbs && goog.array.contains(verbs, 'All');
}
/**
 * Parse rrule
 * 
 * @param {Object} vtodo vtodoJson
 * @return {Object} Parsed rrule
 * @private
 */
net.bluemind.calendar.vtodo.VTodoAdaptor.prototype.parseRRule_ = function(vtodo) {
  if (!vtodo['value']['rrule']) {
    return null;
  }
  var rrule = {
    freq : vtodo['value']['rrule']['frequency'],
    count : vtodo['value']['rrule']['count'],
    until : null,
    interval : vtodo['value']['rrule']['interval'],
    bysecond : vtodo['value']['rrule']['bySecond'],
    byminute : vtodo['value']['rrule']['byMinute'],
    byhour : vtodo['value']['rrule']['byHour'],
    bymonthday : vtodo['value']['rrule']['byMonthDay'],
    byyearday : vtodo['value']['rrule']['byYearDay'],
    byweekno : vtodo['value']['rrule']['byWeekNo']
  };
  if (vtodo['value']['rrule']['byDay']) {
    rrule.byday = [];
    var map = [ 'SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA' ];
    var week = goog.i18n.DateTimeSymbols_en.STANDALONEWEEKDAYS;
    rrule.byDayDisplay = '';
    goog.array.forEach(vtodo['value']['rrule']['byDay'], function(byday) {
      rrule.byday.push({
        day : week[goog.array.indexOf(map, byday['day'])],
        offset : byday['offset']
      });
      rrule.byDayDisplay += rrule.byday.slice(-1)[0]['day'];
      rrule.byDayDisplay += ',';
    }, this);
    if (/,$/.test(rrule.byDayDisplay)) {
      rrule.byDayDisplay = rrule.byDayDisplay.slice(0, -1);
    }
  }
  if (vtodo['value']['rrule']['byMonth']) {
    rrule.bymonth = vtodo['value']['rrule']['byMonth'][0] - 1;
  }
  if (vtodo['value']['rrule']['until']) {
    rrule.until = this.ctx_.helper('date').create(vtodo['value']['rrule']['until']);
  }
  return rrule;
};

/**
 * Test if the given item container is owned by a given attendee
 * 
 * @param {Object} attendee Attendee to test
 * @param {Object} dir vtodo todolist owner
 * @return {boolean}
 * @private
 */
net.bluemind.calendar.vtodo.VTodoAdaptor.prototype.ownTodolist_ = function(attendee, dir) {
  if (attendee && goog.isDefAndNotNull(attendee['dir']) && goog.isDefAndNotNull(dir)) {
    return dir.replace('bm://', '') == attendee['dir'].replace('bm://', '');
  } else {
    return false;
  }
};
