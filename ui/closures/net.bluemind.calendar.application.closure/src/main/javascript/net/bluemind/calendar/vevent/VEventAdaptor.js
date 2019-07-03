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

goog.provide("net.bluemind.calendar.vevent.VEventAdaptor");

goog.require("goog.array");
goog.require("goog.i18n.DateTimeSymbols_en");
goog.require("net.bluemind.mvp.UID");

/**
 * Adaptor for Vevent BM-Core JSON to and from view model
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @constructor
 */
net.bluemind.calendar.vevent.VEventAdaptor = function(ctx) {
  this.ctx_ = ctx;
};

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.ctx_;

/**
 * Adapt a VEvent from BM-Core JSON to a object usable by views
 * 
 * @param {Object} vevent Vevent json
 * @param {Object} calendar Calendar model view
 * @return {Object} Adapted vevent for usage in the view.
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.toModelView = function(vevent, calendar) {
  var helper = this.ctx_.helper("date");

  var model = {};
  model.oldValue = vevent;
  model.type = 'vevent';

  // TODO: Create or not... in form and card the date must have event tz, in
  // views it must have context tz
  model.dtstart = helper.create(vevent['dtstart'], this.ctx_.helper('timezone').getDefaultTimeZone());
  model.dtend = helper.create(vevent['dtend'], this.ctx_.helper('timezone').getDefaultTimeZone());
  model.timezones = {
    start : vevent['dtstart']['timezone'] && this.ctx_.helper('timezone').getTimeZone(vevent['dtstart']['timezone']),
    end : vevent['dtend']['timezone'] && this.ctx_.helper('timezone').getTimeZone(vevent['dtend']['timezone'])
  }
  model.summary = vevent['summary'];
  model.attendees = goog.array.map(vevent['attendees'] || [], this.attendeeToModelView, this);
  model.location = vevent['location'];
  model.url = vevent['url'];
  model.organizer = vevent['organizer'];
  model.description = vevent['description'];
  model.transp = vevent['transparency'];
  model.class = vevent['classification'];
  model.status = vevent['status'];
  model.priority = vevent['priority'];
  model.tags = goog.array.map(vevent['categories'] || [], function(tag) {
    return {
      id : tag['itemUid'],
      container : tag['containerUid'],
      label : tag['label'],
      color : tag['color']
    };
  });

  model.alarm = goog.array.map(vevent['alarm'] || [], function(alarm) {
    return {
      trigger : (null != alarm['trigger']) ? alarm['trigger'] * -1 : alarm['trigger'],
      action : alarm['action']
    }
  });

  if (goog.isDefAndNotNull(vevent['recurid'])) {
    model.recurrenceId = helper.create(vevent['recurid']);
    model.timezones.recurrence = vevent['recurid']['timezone'] && this.ctx_.helper('timezone').getTimeZone(vevent['recurid']['timezone'])
  }


  if (calendar.states.defaultCalendar) {
    var attendee = goog.array.find(model.attendees, function(attendee) {
      return this.ownCalendar_(attendee, calendar.dir);
    }, this);

    if (attendee) {
      model.participation = attendee['partStatus'];
      model.attendee = {
        id : attendee['dir'],
        name : attendee['commonName'],
        responseComment : attendee['responseComment'],
        rsvp : attendee['rsvp']
      }
    }
  }

  model.rrule = this.parseRRule_(vevent);
  model.exdate = goog.array.map(vevent['exdate'] || [], function(exdate) {
    return helper.create(exdate);
  }, this);

  model.states = {};
  model = this.updateStates(model, calendar);

  if (!(calendar.owner == this.ctx_.user['uid']) && model.class == "Private" && !model.states.updatable) {
    /** @meaning calendar.event.privacy.private */ 
    var MSG_PRIVATE = goog.getMsg('Private');
    model.summary = MSG_PRIVATE;
  }

  return model;
};

net.bluemind.calendar.vevent.VEventAdaptor.prototype.attendeeToModelView = function(attendee) {
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
    'internal' : attendee['internal'],
    'responseComment' : attendee['responseComment']
  };

  if (ret['dir'] && goog.string.startsWith(ret['dir'], 'bm://')) {
    ret['icon'] = '/api/directory/' + this.ctx_.user['domainUid'] + '/_icon/'
        + encodeURIComponent(goog.string.removeAt(attendee['dir'], 0, 5));
  }
  return ret;
}

net.bluemind.calendar.vevent.VEventAdaptor.prototype.attendeeFromModelView = function(attendee) {
  return {
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
    'internal' : attendee['internal'],
    'responseComment' : attendee['responseComment']
  };
}

/**
 * Set or update states depending on model data.
 * 
 * @param {Object} model
 * @param {Object} calendar Calendar model view
 * @return {Object} updated model
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.updateStates = function(model, calendar) {
  model.states.defaultCalendar = calendar.states.defaultCalendar;
  model.states.allday = !(model.dtstart instanceof goog.date.DateTime);
  model.states.private_ = (model.class != 'Public');
  model.states.busy = (model.transp == 'Opaque');
  model.states.meeting = !!model.attendees.length;
  model.states.master = model.states.master || !model.organizer && !model.states.meeting;
  model.states.master = model.states.master || this.ownCalendar_(model.organizer, calendar.dir);
  model.states.pending = (model.participation == 'NeedsAction');
  model.states.tentative = (model.participation == 'Tentative');
  model.states.declined = (model.participation == 'Declined');
  model.states.repeat = !!(model.rrule && model.rrule.freq);
  model.states.main = !goog.isDefAndNotNull(model.recurrenceId);
  model.states.occurrence = !model.states.main && model.states.repeat;
  model.states.exception = !model.states.main && !model.states.repeat;
  model.states.forever = (model.states.repeat && !model.rrule.until && !model.rrule.count);
  model.states.count = (model.states.repeat && model.rrule && goog.isDefAndNotNull(model.rrule.count)) ? model.rrule.count : null;
  model.states.past = model.dtend.getTime() < goog.now();
  model.states.updatable = (calendar.states.writable) && (!model.states.private_ || (calendar.owner == this.ctx_.user['uid']) || this.canAll_(calendar.verbs)) ;
  model.states.attendee = goog.isDefAndNotNull(model.attendee) && model.states.meeting && !model.states.master
  model.states.removable = model.states.updatable && !!model.id;

  return model;
};

net.bluemind.calendar.vevent.VEventAdaptor.prototype.canAll_ = function(verbs) {
  return verbs && goog.array.contains(verbs, 'All');
}

/**
 * Build a new event model from view model
 * 
 * @param {Object} vevent
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.toQueryData = function(model) {

  var vevent = {
    'value' : {
      'dtstart' : this.ctx_.helper('date').toBMDateTime(model.dtstart),
      'dtend' : this.ctx_.helper('date').toBMDateTime(model.dtend),
      'summary' : model.summary,
      'transparency' : model.transp || (model.states.busy ? 'Opaque' : 'Transparent')
    }
  };
  if (model.recurrenceId) {
    vevent['recurid'] = this.ctx_.helper('date').toBMDateTime(model.recurrenceId);
    vevent['rrule'] = {};
  }
  if (goog.isDefAndNotNull(model.parentId)) {
    vevent['uid'] = model.parentId;
    vevent['container'] = model.calendar;
  }
  return goog.global['JSON'].stringify(vevent);
};

/**
 * Parse rrule
 * 
 * @param {Object} vevent VeventJson
 * @return {Object} Parsed rrule
 * @private
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.parseRRule_ = function(vevent) {
  if (!vevent['rrule']) {
    return null;
  }
  var rrule = {
    freq : vevent['rrule']['frequency'],
    count : vevent['rrule']['count'],
    until : null,
    interval : vevent['rrule']['interval'],
    bysecond : vevent['rrule']['bySecond'],
    byminute : vevent['rrule']['byMinute'],
    byhour : vevent['rrule']['byHour'],
    bymonthday : vevent['rrule']['byMonthDay'],
    byyearday : vevent['rrule']['byYearDay'],
    byweekno : vevent['rrule']['byWeekNo']
  };
  if (vevent['rrule']['byDay']) {
    rrule.byday = [];
    var map = [ 'SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA' ];
    var week = goog.i18n.DateTimeSymbols_en.STANDALONEWEEKDAYS;
    rrule.byDayDisplay = '';
    goog.array.forEach(vevent['rrule']['byDay'], function(byday) {
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
  if (vevent['rrule']['byMonth'] && !goog.array.isEmpty(vevent['rrule']['byMonth'])) {
    rrule.bymonth = vevent['rrule']['byMonth'][0] - 1;
  }
  if (vevent['rrule']['until']) {
    rrule.until = this.ctx_.helper('date').create(vevent['rrule']['until'],
        this.ctx_.helper('timezone').getDefaultTimeZone());
  }
  return rrule;
};

/**
 * Test if the given item container is owned by a given attendee
 * 
 * @param {Object} attendee Attendee to test
 * @param {Object} calendar Vevent calendar
 * @return {boolean}
 * @private
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.ownCalendar_ = function(attendee, dir) {
  if (attendee && goog.isDefAndNotNull(attendee['dir']) && goog.isDefAndNotNull(dir)) {
    return dir.replace('bm://', '') == attendee['dir'].replace('bm://', '');
  } else {
    return false;
  }
};

/**
 * Build a BM-Core Vevent JSON from model view
 * 
 * @param {Object} model Model view
 * @return {Object}
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.fromModelView = function(model) {
  var timezone = model.states.allday ? null : this.ctx_.settings.get('timezone');
  var precision = model.states.allday ? 'Date' : 'DateTime';


  var tags = goog.array.map(model.tags, function(tag) {
    return {
      'itemUid' : tag.id,
      'containerUid' : tag.container,
      'label' : tag.label,
      'color' : tag.color
    };

  });
 
  var vevent = {};
  vevent['dtstart'] = this.ctx_.helper('date').toBMDateTime(model.dtstart, model.timezones.start);
  vevent['dtend'] = this.ctx_.helper('date').toBMDateTime(model.dtend, model.timezones.end);
  vevent['summary'] = model.summary;
  vevent['classification'] = model.class || 'Public';
  vevent['transparency'] = model.transp || (model.states.busy ? 'Opaque' : 'Transparent');
  vevent['description'] = model.description || '';
  vevent['location'] = model.location || '';
  vevent['url'] = model.url || '';
  vevent['priority'] = model.priority || 5;
  vevent['status'] = model.status || 'Confirmed';
  vevent['exdate'] = null;
  vevent['categories'] = tags;
  vevent['rrule'] = this.composeRRule_(model);

  if (goog.isDefAndNotNull(model.recurrenceId)) {
    vevent['recurid'] = this.ctx_.helper('date').toBMDateTime(model.recurrenceId, model.timezones.recurrence);
  }
  if (goog.isArray(model.exdate)) {
    vevent['exdate'] = goog.array.map(model.exdate, function(exdate) {
      return this.ctx_.helper('date').toBMDateTime(exdate, model.timezones.start)
    }, this);
  }

  if (goog.isArray(model.alarm)) {
    vevent['alarm'] = goog.array.map(model.alarm, function(alarm) {
      return {
        'action' : alarm.action,
        'trigger' : (null != alarm.trigger) ? alarm.trigger * -1 : alarm.trigger
      }
    });
  }

  if (goog.isArray(model.attendees) && model.attendees.length > 0) {
    vevent['attendees'] =

    goog.array.map(goog.array.map(model.attendees, this.attendeeFromModelView, this), function(attendee) {
      var result = attendee;
      if (model.attendee && result['dir'] == model.attendee.id) {
        result['partStatus'] = model.participation;
        result['responseComment'] = model.attendee.responseComment;
      }
      return result;
    }, this)

    if (model.organizer) {
      vevent['organizer'] = model.organizer;
    }
  }

  return vevent;
};

/**
 * Compose rrule from model view rrule
 * 
 * @param {Object} model Model view
 * @return {Object} Composed rrule json
 * @private
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.composeRRule_ = function(model) {

  if (!model.rrule) {
    return null;
  }
  var rrule = {};
  if (goog.isDefAndNotNull(model.rrule.freq)) {
    rrule['frequency'] = model.rrule.freq; 
  }

  if (goog.isDefAndNotNull(model.rrule.interval)) {
    rrule['interval'] = model.rrule.interval; 
  }

  if (goog.isDefAndNotNull(model.rrule.freqbysecond)) {
    rrule['bySecond'] = model.rrule.bysecond; 
  }

  if (goog.isDefAndNotNull(model.rrule.byminute)) {
    rrule['byMinute'] = model.rrule.byminute; 
  }

  if (goog.isDefAndNotNull(model.rrule.byhour)) {
    rrule['byHour'] = model.rrule.byhour; 
  }

  if (goog.isDefAndNotNull(model.rrule.bymonthday)) {
    rrule['byMonthDay'] = model.rrule.bymonthday; 
  }

  if (goog.isDefAndNotNull(model.rrule.byyearda)) {
    rrule['byYearDay'] = model.rrule.byyearday; 
  }

  if (goog.isDefAndNotNull(model.rrule.byweekno)) {
    rrule['byWeekNo'] = model.rrule.byweekno; 
  }

  if (model.rrule.byday && model.rrule.byday.length > 0) {
    rrule['byDay'] = [];
    var map = [ 'SU', 'MO', 'TU', 'WE', 'TH', 'FR', 'SA' ];
    var week = goog.i18n.DateTimeSymbols_en.STANDALONEWEEKDAYS;
    goog.array.forEach(model.rrule.byday, function(byday) {
      var day = map[goog.array.indexOf(week, byday.day)];
      rrule['byDay'].push({
        'day' : day,
        'offset' : byday.offset
      });
    }, this);
  }
  if (goog.isDefAndNotNull(model.rrule.bymonth)) {
    rrule['byMonth'] = [ model.rrule.bymonth + 1 ];
  }

  if (model.rrule.until) {
    rrule['until'] = this.adaptUntil(model.dtstart, model.rrule.until)
  } else if (model.rrule.count) {
    rrule['count'] = model.rrule.count;

  }
  return rrule;
};

/**
 * Compose rrule from model view rrule
 * 
 * @param {goog.date.Date} dtstart
 * @param {goog.date.Date} until
 * @return {goog.date.Date} until
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.adaptUntil = function(dtstart, until) {
  if (dtstart instanceof goog.date.DateTime) {
    until.setHours(dtstart.getHours());
    until.setMinutes(dtstart.getMinutes());
    until.setSeconds(dtstart.getSeconds());
    until.setMilliseconds(dtstart.getMilliseconds());
    var date = this.ctx_.helper('date').toTimeZone(until, 'UTC');
    return this.ctx_.helper('date').toBMDateTime(date);
  } else {
    var value = this.ctx_.helper('date').toBMDateTime(until);
    value['timezone'] = null;
    return value;
  }
}

/**
 * Test if the modification needs to send a notification or not
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.isPublicChanges = function(series, remote, modified) {
  var isMeeting = modified.states.meeting;
  isMeeting = isMeeting || (remote && remote['attendees'] && remote['attendees'].length > 0);
  isMeeting = isMeeting || (modified.states.master && series && goog.array.some(series['value']['occurrences'], function(occurrence) {
    return occurrence['attendees'] && occurrence['attendees'].length > 0;
  }));
  if (!isMeeting) {
    return false;
  }
  if (modified.states.master) {
    return this.isModified(remote, modified);
  }
  if (!modified.attendee) {
    return false;
  }
  if (!remote || !modified.states.updatable) {
    return true;
  }
  var attendee = goog.array.find(remote['attendees'], function(attendee) {
    return this.ownCalendar_(attendee, modified.attendee.id);
  }, this);
  return attendee && (attendee['partStatus'] != modified.participation);
};

/**
 * Test if the vevent has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.isModified = function(remote, modified) {
  return true;
};
