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

/** @fileoverview Presenter for the list view */

goog.provide('net.bluemind.calendar.vevent.VEventCounterPresenter');

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.object");
goog.require("goog.date.Date");
goog.require("goog.date.Interval");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventSeriesAdaptor");
goog.require("net.bluemind.calendar.vevent.VEventActions");
goog.require("net.bluemind.calendar.vevent.ui.CounterForm");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.date.DateTime");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.mvp.UID");
goog.require('bluemind.storage.StorageHelper');

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.vevent.VEventCounterPresenter = function(ctx) {
  goog.base(this, ctx);
  this.adaptor_ = new net.bluemind.calendar.vevent.VEventSeriesAdaptor(ctx);
  this.actions_ = new net.bluemind.calendar.vevent.VEventActions(ctx, this.adaptor_);
  this.registerDisposable(this.actions_);

};
goog.inherits(net.bluemind.calendar.vevent.VEventCounterPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.vevent.VEventView}
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.view_;

/**
 * @type {net.bluemind.calendar.vevent.VEventAdaptor}
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.adaptor_;

/** @override */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.init = function() {
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.setup = function() {

  var data = {};
  var containerUid = this.ctx.params.get('container');
  data.recurrenceId = this.ctx.params.get('recurrence-id');
  data.selectedPartStatus = this.ctx.params.get('selected-part-status');

  return this.ctx.service('calendarsMgmt').list('calendar').then(function(calendars) {
    return this.loadContainers_(data, calendars, containerUid);
  }, null, this).then(function() {
    this.actions_.setCalendars(data.calendars);
    return this.loadItem_(data.container)
  }, null, this).then(function(vseries) {
    return this.loadModelView_(vseries, data.container);
  }, null, this).then(function(mv) {
    data.model = mv;
    this.loadView_(data, mv.eventAttendees, mv.eventDtStart, mv.eventDtEnd);
  }, null, this).thenCatch(function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

/**
 * Load View
 * 
 * @param {Object} data view data
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.loadView_ = function(data, eventAttendees, eventDtStart, eventDtEnd) {
  this.view_ = new net.bluemind.calendar.vevent.ui.CounterForm(this.ctx);
  this.view_.selectedPartStatus = data.selectedPartStatus;
  this.view_.eventAttendees_ = eventAttendees;
  this.view_.eventDtStart_ = eventDtStart;
  this.view_.eventDtEnd_ = eventDtEnd;
  this.view_.getChild('freebusy').freebusyRequest = goog.bind(this.freeBusyRequest, this)
  var e = net.bluemind.calendar.vevent.EventType.SEND;
  this.handler.listen(this.view_, e, this.send_);
  e = net.bluemind.calendar.vevent.EventType.CANCEL;
  this.handler.listen(this.view_, e, this.back_);
  e = net.bluemind.calendar.vevent.EventType.REMOVE;
  this.handler.listenWithScope(this.view_, e, this.remove_);
  var calendars = goog.array.filter(data.calendars, function(calendar) {
    if (calendar.settings && calendar.settings.type == 'externalIcs' && data.container != calendar) {
      return false;
    }
    return calendar.states.writable;
  });
  
  var e = net.bluemind.calendar.vevent.EventType.BACK
  this.handler.listen(this.view_, e, this.back_);

  this.registerDisposable(this.view_);
  this.view_.range = this.ctx.session.get('range');
  this.view_.calendars = calendars;
  this.view_.date = this.ctx.session.get('date');
  this.view_.getChild('freebusy').range = this.view_.range;
  this.view_.setModel(data.model);
  this.view_.render(goog.dom.getElement('full'));
  this.actions_.setFeatures([ 'notification' ]);
  this.actions_.injectPopups(this.view_);
};

/**
 * @param {Object} attendee
 * @param {goog.date.Range} range
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.freeBusyRequest = function(attendee, range, opt_excludeThisEvent) {
  var promise;
  var exclusions = opt_excludeThisEvent && this.ctx.params.get('uid') != null ? [ this.ctx.params.get('uid') ] : [];

  var isKnownByBM = attendee['dir'] != null && goog.string.startsWith(attendee['dir'], 'bm://'); 
  var dirEntryKind = isKnownByBM ? attendee['dir'].substring('bm://'.length).split("/")[1] : null;

  var containerBasedFreebusy = dirEntryKind == "users" || dirEntryKind == "resources";
  var emailBasedFreebusy = attendee["mailto"] && (!isKnownByBM || dirEntryKind == "externaluser");

  if (containerBasedFreebusy) {
    var uid = attendee['dir'].substring('bm://'.length).split("/")[2];
    var fb = new net.bluemind.calendar.api.VFreebusyClient(this.ctx.rpc, '', 'freebusy:' + uid);
    promise = fb.get({
      'dtstart' : this.ctx.helper('date').toBMDateTime(new net.bluemind.date.Date(range.getStartDate())),
      'dtend' : this.ctx.helper('date').toBMDateTime(new net.bluemind.date.Date(range.getEndDate())),
      'excludedEvents' : exclusions
    });
  } else if (emailBasedFreebusy) {
    var fb = new net.bluemind.calendar.api.PublicFreebusyClient(this.ctx.rpc, '');
    promise = fb.get(attendee['mailto'], this.ctx.user['uid'], this.ctx.user['domainUid'], {
      'dtstart' : this.ctx.helper('date').toBMDateTime(new net.bluemind.date.Date(range.getStartDate())),
      'dtend' : this.ctx.helper('date').toBMDateTime(new net.bluemind.date.Date(range.getEndDate())),
      'excludedEvents' : exclusions
    });
  } else {
    promise = goog.Promise.resolve();
  }
  return promise.then(function(vfreebusy) {
    if (vfreebusy == null) {
      throw 'Empty freebusy';
    }
    var slots = new Array(24 * 2 * 7);

    goog.array.forEach(vfreebusy['slots'], function(slot) {
      if (slot['type'] == 'FREE') {
        return;
      }
      var dtstart = this.ctx.helper('date').fromBMDateTime(slot['dtstart']);
      var dtend = this.ctx.helper('date').fromBMDateTime(slot['dtend']);
      dtstart = new net.bluemind.date.DateTime(range.isAfter(dtstart) ? range.getStartDate() : dtstart);
      dtend = new net.bluemind.date.DateTime(range.isBefore(dtend) ? range.getEndDate() : dtend);

      var step = new goog.date.Interval(goog.date.Interval.MINUTES, 30);
      while (goog.date.Date.compare(dtstart, dtend) < 0) {
        var i = ((dtstart.getDay() * 24) + dtstart.getHours()) * 2 + Math.floor(dtstart.getMinutes() / 30);
        slots[i] = slot['type'];
        dtstart.add(step)
      }
    }, this);
    return slots;
  }, function() {
    return goog.array.repeat('NOTAVAILABLE', 24 * 2 * 7);
  }, this);
};


/**
 * Load containers
 * 
 * @param {Object} data View datas
 * @param {Array.<Object>} containers Containers list
 * @param {String} containerUid Current container uid
 * @return {Object} data
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.loadContainers_ = function(data, containers, containerUid) {
  data.calendars = [];
  for (var i = 0; i < containers.length; i++) {
    var mv = this.calendarToMV_(containers[i])
    if (mv.uid == containerUid) {
      data.container = mv;
    }
    data.calendars.push(mv);
  }

  goog.array.sort(data.calendars, function(c1, c2) {
    return goog.string.caseInsensitiveCompare(c1.name, c2.name);
  });
  if (!goog.isDefAndNotNull(data.container)) {
    throw 'Container not found';
  }
  return goog.Promise.resolve(data);
};

/**
 * Load item
 * 
 * @param {Object} container Container object
 * @param {String} uid Item uid
 * @return {*} data
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.loadItem_ = function(container) {
  var service = this.ctx.service('calendar');
  var uid = this.ctx.params.get('uid');
  var recurrenceId = this.ctx.params.get('recurrence-id');

  return service.getItem(container.uid, uid).then(function(vseries) {
    if (vseries == null) {
      return null;
    } else if (recurrenceId && !this.adaptor_.getRawOccurrence(recurrenceId, vseries)) {
      return this.updateVSeries_(vseries, recurrenceId);
    }
    return vseries;
  }, null, this);
};

/**
 * Load template
 * 
 * @param {Object} item Item object
 * @param {Object} value Item uid
 * @param {Object} calendar container
 * @return {*} data
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.loadModelView_ = function(vseries, calendar) {
  var changes; 
  return this.ctx.service('calendar').getLocalChangeSet(calendar.uid).then(function(veventChanges) {
    changes = veventChanges;
    return this.vseriesToMV_(calendar, vseries);
  }, null, this).then(function (model) {
    model = this.adaptModelView_(changes, model);
    model.recurrenceId = this.ctx.params.get('recurrence-id');
    var availableCounters = this.adaptor_.getCounterByOccurrence(model.recurrenceId, model);
    var correspondingOccurrence = this.adaptor_.getOccurrence(model.recurrenceId, model);
    var counterModel;
    if (availableCounters.length == 0){
      counterModel = {};  
      counterModel.counter = correspondingOccurrence;
      counterModel.eventAttendees = counterModel.counter.attendees;
      counterModel.eventDtStart = correspondingOccurrence.dtstart;
      counterModel.eventDtEnd = correspondingOccurrence.dtend;
      counterModel.counter.attendees = goog.array.filter(counterModel.counter.attendees, function(attendee) {
        return counterModel.counter.attendee.id == attendee.dir;
      });
      counterModel.originator = {};
      counterModel.originator.commonName = counterModel.counter.attendee.name;
      counterModel.originator.email = counterModel.counter.attendee.mailto;
      return counterModel;
    }
    counterModel = availableCounters[0];
    counterModel.eventDtStart = correspondingOccurrence.dtstart;
    counterModel.eventDtEnd = correspondingOccurrence.dtend;;
    counterModel.eventAttendees = this.adaptor_.getOccurrence(model.recurrenceId, model).attendees;
    return counterModel;
  }, null, this);
};

/**
 * Tear down current view on navigate
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.tearDown = function() {
  if (this.view_) {
    this.view_.dispose();
    this.handler.removeAll();
  }
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Build calendar model for view
 * 
 * @param {Object} vevent Vevent json
 * @param {Object} calendar Calendar model view
 * @param {Array} changes
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.vseriesToMV_ = function(calendar, vseries) {
  var uid = this.ctx.params.get('uid');
  var model = this.adaptor_.toModelView(vseries, calendar);
  return model;
}

net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.adaptModelView_ = function(changes, model) {

  var change = goog.array.find(changes, function(change) {
    return change['itemId'] == model.uid && change['container'] == model.calendar;
  });

  model.states.synced = !goog.isDefAndNotNull(change);
  model.states.error = !model.states.synced && change['type'] == 'error';
  model.error = model.states.error && {
    code : change['errorCode'],
    message : change['errorMessage']
  };
  var modifier = 0;
  goog.array.forEach(model.flat, function(vevent, index) {
    this.adaptVEvent_(vevent, model);
  }, this)
  return model;
};

/**
 * Build calendar model for view
 * 
 * @param {Object} vevent Vevent json
 * @param {Object} calendar Calendar model view
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.adaptVEvent_ = function(vevent, vseries) {
  vevent.states.synced = vseries.states.synced;
  vevent.states.error = vseries.states.error;
  vevent.error = vseries.error;
  vevent.states.repeatable = vevent.states.main && vevent.states.master;
};
/**
 * Build calendar model for view
 * 
 * @param {Object} calendar
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.calendarToMV_ = function(calendar) {
  var mv = {};
  mv.name = calendar['name'];
  mv.uid = calendar['uid'];
  mv.states = {};
  mv.states.writable = !!calendar['writable'] && !calendar['readOnly'];
  mv.states.defaultCalendar = calendar['defaultContainer'];
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
 * Build event model
 * 
 * @param {Object} container
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.updateVSeries_ = function(vseries, recurrenceId) {
  var timezone = this.ctx.helper('timezone').getDefaultTimeZone();
  var vevent = JSON.parse(JSON.stringify(vseries['value']['main']));
  delete vevent['rrule'];
  var dtstart = this.ctx.helper('date').create(vevent['dtstart']);
  var dtend = this.ctx.helper('date').create(vevent['dtend']);
  var duration = dtend.getTime() - dtstart.getTime();

  vevent['dtstart']['iso8601'] = recurrenceId;

  dtstart = this.ctx.helper('date').create(vevent['dtstart']);
  dtend.setTime(dtstart.getTime() + duration);

  vevent['dtend'] = this.ctx.helper('date').toBMDateTime(dtend);
  vevent['dtstart'] = this.ctx.helper('date').toBMDateTime(dtstart);
  vevent['recurid'] = JSON.parse(JSON.stringify(vevent['dtstart']));
  vseries['value']['occurrences'].push(vevent);
  return vseries;
};

/**
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.back_ = function() {
  this.ctx.helper('url').back(this.ctx.session.get('history'), '/');
};

/**
 * @param {goog.events.Event} e
 * @private
 */
net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.send_ = function(e) {
  e.vevent.sendNotification = true;
  this.save_(e).then(this.back_, null, this);
}

net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.remove_ = function(e) {
  e.vevent.sendNotification = true;
  this.actions_.remove(e).then(this.back_, null, this);
}


net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.save_ = function(e) {
  var model = e.vevent;
  if (!this.view_.checkDate_()) {
      return goog.Promise.reject();
  }
  var calendar = goog.array.find(this.view_.calendars, function(calendar) {
    return model.calendar == calendar.uid;
  }, this);

  return this.actions_.saveCounter(e);
};


net.bluemind.calendar.vevent.VEventCounterPresenter.prototype.resourceDescRequest = function(resourceUid, organizer) {
	
};
