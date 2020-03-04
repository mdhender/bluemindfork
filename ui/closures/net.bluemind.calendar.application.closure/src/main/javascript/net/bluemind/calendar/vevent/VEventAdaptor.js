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
goog.require("goog.date.Interval");
goog.require("net.bluemind.mvp.UID");
goog.require("net.bluemind.date.DateTime");
goog.require('net.bluemind.calendar.vevent.defaultValues');

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
  model.old = vevent;
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
  model.sequence = vevent['sequence'] || 0;
  model.draft = vevent['draft'] || false;
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

  model.attachments = this.parseAttachments_(vevent);
  
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
  model.states.hasAttachments = model.attachments.length > 0;
  model.states.draft = !!model.draft;
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

net.bluemind.calendar.vevent.VEventAdaptor.prototype.parseAttachments_ = function(vevent) {
  if (!vevent['attachments']) {
    return [];
  }
  
  var attachments = goog.array.map(vevent['attachments'], function(attachment) {
    return {
      publicUrl : attachment['publicUrl'],
      name : attachment['name']
    };
  }, this);
  
  for (var i = 0; i < attachments.length; i++) { 
    attachments[i].index = i;
  } 
  
  return attachments;
}

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
  vevent['draft'] = !! model.draft;
  vevent['sequence'] = model.sequence || 0;
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
  
  if (goog.isArray(model.attachments)) {
    vevent['attachments'] = goog.array.map(model.attachments, function(attachment) {
      return {
        'publicUrl' : attachment.publicUrl,
        'name' : attachment.name
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

net.bluemind.calendar.vevent.VEventAdaptor.prototype.isPublic = function(remote, modified, opt_series) {
  var isMeeting = modified.states.meeting;
  isMeeting = isMeeting || (remote && remote['attendees'] && remote['attendees'].length > 0);
  return isMeeting || (modified.states.master && opt_series && goog.array.some(opt_series['value']['occurrences'], function(occurrence) {
    return occurrence['attendees'] && occurrence['attendees'].length > 0;
  }));
}
/**
 * Test if the modification needs to send a notification or not
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.isPublicChanges = function(remote, modified, opt_series) {
  var isMeeting = this.isPublic(remote, modified, opt_series);
  if (!isMeeting) {
    return false;
  }
  if (isMeeting && modified.states.draft) {
    return true;
  }
  if (!modified.states.updatable) {
    return true;
  }
  if (modified.states.master) {
    return this.isModified(remote, modified);
  }
  if (!modified.attendee) {
    return false;
  }
  var attendee = goog.array.find(remote['attendees'], function(attendee) {
    return this.ownCalendar_(attendee, modified.attendee.id);
  }, this);
  return attendee && (attendee['partStatus'] != modified.participation);
};


/**
 * @param {net.bluemind.calendar.vevent.VEventEvent} oldVersion
 * @param {net.bluemind.calendar.vevent.VEventEvent} newVersion
 * @private
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.isSignificantlyModified = function(remote, modified) {
  if (!modified.states.meeting) return false;
  var reset = (remote == null);
  reset = reset || (remote['location'] != modified.location);
  reset = reset || this.dateHasBeenModified(remote, modified);

  return reset;
}

/**
 * Test if the vevent has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.isModified = function(remote, modified) {
  var ret = this.contentHasBeenModified(remote, modified);
  ret = ret || this.attendeesHasBeenModified(remote, modified);
  return ret;
}


/**
 * Test if the vevent content (everything but attendees) has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.contentHasBeenModified = function(remote, modified) {
  var isModified = remote == null;
  isModified = isModified || remote['location'] != modified.location;
  isModified = isModified || remote['summary'] != modified.summary;
  isModified = isModified || remote['description'] != modified.description;
  isModified = isModified || remote['classification'] != modified.class;
  isModified = isModified || remote['priority'] != modified.priority;
  isModified = isModified || remote['url'] != modified.url;
  isModified = isModified || this.organiserHasBeeModified(remote, modified);
  isModified = isModified || this.dateHasBeenModified(remote, modified);
  isModified = isModified || this.attachmentsHasBeenModified(remote, modified);
  isModified = isModified || this.exdatesHasBeenModified(remote, modified);
  return isModified;
}

/**
 * Test if the vevent's organizer has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.organiserHasBeeModified = function(remote, modified) {
  if (!modified.states.meeting) {
    return false;
  }
  if (!remote['organizer'] && modified.organizer || !modified.organizer && remote['organizer']) {
    return true;
  }
  return remote['organizer'] != modified.organizer && remote['organizer']['dir'] != modified.organizer['dir'];
}

/**
 * Test if the vevent's attachments has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.attachmentsHasBeenModified = function(remote, modified) {
  if ((remote['attachments'] == null || remote['attachments'].length == 0) && (modified.attachments == null || modified.attachments.length == 0)) {
    return false;
  } else if ((remote['attachments'] == null && modified.attachments != null) || (remote['attachments'] != null && modified.attachments == null)) {
    return true;
  } else if (remote['attachments'].length != modified.attachments.length) {
    return true;
  } else if (modified.attachment.length == 0) {
    return false;
  } else {
    return !goog.array.equals(remote['attachments'], modified.attachments, function(remoteAttachment, modifiedAttachment) {
      return remoteAttachment['name'] == modifiedAttachment['name']
          && remoteAttachment['publicUrl'] == modifiedAttachment['publicUrl'];
    });
  }
}

/**
 * Test if the vevent's exdates has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.exdatesHasBeenModified = function(remote, modified) {
  var remoteLength = goog.isArray(remote['exdate']) ? remote['exdate'].length : 0;
  var modifiedLength = goog.isArray(modified.exdate) ? modified.exdate.length : 0;
  return modifiedLength != remoteLength;
}

/**
 * Test if the vevent's attendees has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.attendeesHasBeenModified = function(remote, modified) {
  if ((remote['attendees'] == null || remote['attendees'].length == 0) && (modified.attendees == null || modified.attendees.length == 0)) {
    return false;
  } else if ((remote['attendees'] == null && modified.attendees != null) || (remote['attendees'] != null && modified.attendees == null)) {
    return true;
  } else if (remote['attendees'].length != modified.attendees.length) {
    return true;
  } else {
    return !goog.array.equals(remote['attendees'], modified.attendees, function(remoteAttendee, modifiedAttendee) {
      return remoteAttendee['mailto'] == modifiedAttendee['mailto']
          && remoteAttendee['partStatus'] == modifiedAttendee['partStatus'];
    });
  }
}

/**
 * Test if the vevent's dates or rrule has been modified
 * 
 * @param {Object=} remote Stored version of the vevent
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.dateHasBeenModified = function(remote, modified) {
  var equalFn = function(a, b) {
    if (!goog.isDefAndNotNull(a) && !goog.isDefAndNotNull(b)) {
      return true;
    }
    if (!goog.isObject(a) || !goog.isObject(b)) {
      return a === b;
    }
    var equal = goog.object.every(a, function(v, k) {
      return (k in b || v === null) && equalFn(v, b[k]);
    }) ;
    equal = equal && goog.object.every(b, function(v, k) {
      return k in a || !goog.isDefAndNotNull(v) || goog.array.isEmpty(v);
    });
    return equal;
  };
  var adapt = this.ctx_.helper('date').toBMDateTime.bind(this.ctx_.helper('date'));

  var reset = !equalFn(adapt(modified.dtstart, modified.timezones.start), remote['dtstart']);
  reset = reset || !equalFn(adapt(modified.dtend, modified.timezones.end), remote['dtend']);
  reset = reset || !equalFn(this.composeRRule_(modified), remote['rrule']);
  return reset;
}

/**
 * Generate an empty event
 * @return {Object} Vevent
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.createVEvent = function() {
  var timezone = this.ctx_.helper('timezone').getDefaultId();

  var dtstart = new net.bluemind.date.DateTime();
  dtstart.add(new goog.date.Interval(0, 0, 0, 2));
  dtstart.setMinutes(0);
  dtstart.setSeconds(0);
  dtstart.setMilliseconds(0);
  var dtend = dtstart.clone();
  dtend.add(new goog.date.Interval(goog.date.Interval.HOURS, 1));

  var evt = {
    'dtstart' : {
      'precision' : 'DateTime',
      'iso8601' : dtstart.toIsoString(true, true),
      'timezone' : timezone
    },
    'dtend' : {
      'precision' : 'DateTime',
      'iso8601' : dtend.toIsoString(true, true),
      'timezone' : timezone
    },
    'summary' : '',
    'draft': true,
    'sequence': 0,
    'classification' : 'Public',
    'transparency' : 'Opaque',
    'description' : '',
    'location' : '',
    'priority' : 5,
    'status' : 'Tentative',
    'exdate' : null,
    'categories' : [],
    'rrule' : null
  };

  if (this.ctx_.settings.get('default_event_alert') && !isNaN(parseInt(this.ctx_.settings.get('default_event_alert')))) {
    evt['alarm'] = [ {
      'trigger' : -1 * this.ctx_.settings.get('default_event_alert'),
      'action' : net.bluemind.calendar.vevent.defaultValues.action
    } ];
  }

  return evt;
};



/**
 * Auto set event end after a dtstart change
 * 
 * @param {Object} model Vevent Model view object
 * @param {Object} old Old date
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.adjustDTend = function(model, old) {
  var diff = (model.dtstart.getTime() - old.getTime()) / 1000;
  if (!model.states.allday) {
    model.dtend.add(new goog.date.Interval(goog.date.Interval.SECONDS, diff));
  } else {
    diff = Math.round(diff / 86400);
    model.dtend.add(new goog.date.Interval(goog.date.Interval.DAYS, diff));
  }
}


/**
 * Auto set the repetition day
 * 
 * @param {Object} model Vevent Model view object
 * @param {Object} old Old date
 */
net.bluemind.calendar.vevent.VEventAdaptor.prototype.adjustRepeatDays = function(model, old) {
  if (! model.rrule.byday || model.rrule.byday.length == 0) {
    return;
  }
  var weekdays = goog.array.clone(goog.i18n.DateTimeSymbols_en.WEEKDAYS);
  var fdow = (goog.i18n.DateTimeSymbols.FIRSTDAYOFWEEK + 1) % 7;

  goog.array.rotate(weekdays, -fdow);
  if (!goog.date.isSameDay(old, model.dtstart)) {
    var day = weekdays[model.dtstart.getWeekday()];
    if (model.rrule.freq == 'WEEKLY') {
      if(goog.array.findIndex(model.rrule.byday, function(element) {return element.day == day}) < 0) {
        model.rrule.byday.push({day : day, offset : 0});
        day = weekdays[old.getWeekday()];
        goog.array.removeIf(model.rrule.byday, function(element) {return element.day == day});
      }
    } else if (model.rrule.freq = 'MONTHLY' || model.rrule.freq == 'YEARLY') {
        model.rrule.byday = [];
        var pos = Math.ceil(model.dtstart.getDate() / 7);
        if (pos == 5) pos = -1
        model.rrule.byday = [ {day : day, offset : pos} ];
        if (model.rrule.freq == 'YEARLY') {
          model.rrule.bymonth = model.dtstart.getMonth();
        }
    }
  }
}