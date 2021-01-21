goog.provide("net.bluemind.calendar.vevent.VEventSeriesAdaptor");

goog.require("net.bluemind.calendar.vevent.VEventAdaptor");

/**
 * Adaptor for VEventSeries BM-Core JSON to and from view model
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @constructor
 *
 */
/**
 * @author mehdi
 *
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor = function(ctx) {
  this.ctx_ = ctx;
  this.veventAdaptor_ = new net.bluemind.calendar.vevent.VEventAdaptor(ctx);

};

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.ctx_;

/**
 * @type {net.bluemind.calendar.vevent.VEventAdaptor}
 * @private
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.veventAdaptor_;

/**
 * Adapt a VEventSeries from BM-Core JSON to a object usable by views
 * 
 * @param {Object} vseries VEventSeries json
 * @param {Object} calendar Calendar model view
 * @return {Object} Adapted VEventSeries for usage in the view.
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.toModelView = function(vseries, calendar) {
  var value = vseries['value'];

  var model = {};
  model.old = value;
  model.type = 'vevent';

  model.uid = vseries['uid'];
  model.calendar = vseries['container'];
  model.counters = vseries['value']['counters'] ? vseries['value']['counters'] : [];
  model.states = {};
  model.acceptCounters = vseries['value']['acceptCounters'];
  model.main = this.veventToModelView(vseries['value']['main'], calendar, model);
  model.occurrences = goog.array.map(vseries['value']['occurrences'], function(occurrence) {
    return this.veventToModelView(occurrence, calendar, model);
  }, this);
  model.counters = goog.array.map(model.counters, function(counter) {
    var counterModel = {};
    counterModel.originator = {};
    counterModel.originator.commonName = counter['originator']['commonName'];
    counterModel.originator.email = counter['originator']['email'];
    counterModel.counter = this.veventToModelView(counter['counter'], calendar, model);
    return counterModel;
  }, this);
  model.flat = goog.array.clone(model.occurrences);
  if (goog.isDefAndNotNull(model.main)) {
    model.flat.unshift(model.main);
  }

  model = this.setStates_(model, calendar);

  return model;
};

/**
 * 
 * @param vevent
 * @param calendar
 * @param vseries
 * @returns
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.veventToModelView = function(vevent, calendar, vseries) {
  if (vevent != null) {
    var model = this.veventAdaptor_.toModelView(vevent, calendar, vseries);
    model.uid = vseries.uid;
    model.calendar = vseries.calendar;
    model.type = vseries.type;
    return model;
  } else {
    return null;
  }
}

/**
 * 
 * @param recurrence
 * @param vseries
 * @returns
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.getOccurrence = function(recurrence, vseries) {
  if (recurrence != null && goog.isString(recurrence)) {
    return goog.array.find(vseries.occurrences, function(occurrence) {
      return this.ctx_.helper('date').fromIsoString(recurrence).getTime() == occurrence.recurrenceId.getTime();
    }, this);
  } else if (recurrence != null && goog.isDateLike(recurrence)) {
    return goog.array.find(vseries.occurrences, function(occurrence) {
      return occurrence.recurrenceId.getTime() == recurrence.getTime();
    }, this);
  }
  return vseries.main;
};

net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.getOccurrenceApi = function(recurrence, vseries) {
  if (recurrence != null && goog.isString(recurrence)) {
    return goog.array.find(vseries['occurrences'], function(occurrence) {
      return this.ctx_.helper('date').fromIsoString(recurrence).getTime() == occurrence['recurid'].getTime();
    }, this);
  } else if (recurrence != null && goog.isDateLike(recurrence)) {
    return goog.array.find(vseries['occurrences'], function(occurrence) {
      if (!occurrence['recurid']){
        return false;
      }
      return this.veventAdaptor_.dateToModel(occurrence['recurid']).getTime() == recurrence.getTime();
    }, this);
  }
  return vseries['main'];
};

/**
 * 
 * @param recurrence
 * @param vseries
 * @returns
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.getCounterByOccurrence = function(recurrence, vseries) {
  if (recurrence != null && goog.isString(recurrence)) {
    var recTime = this.ctx_.helper('date').fromIsoString(recurrence).getTime();
    return goog.array.filter(vseries.counters, function(counter) {
      if (!counter.counter.recurrenceId){
        return false;
      }
      return recTime == counter.counter.recurrenceId.getTime();
    }, this);
  } else if (recurrence != null && goog.isDateLike(recurrence)) {
    return goog.array.filter(vseries.counters, function(counter) {
      if (!counter.counter.recurrenceId){
        return false;
      }
      return counter.counter.recurrenceId.getTime() == recurrence.getTime();
    }, this);
  } else {
    return goog.array.filter(vseries.counters, function(counter) {
      return counter.counter.recurrenceId == null;
    });
  }
};

net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.getCounterByOccurrenceApi = function(recurrence, vseries) {
  if (recurrence != null && goog.isString(recurrence)) {
    return goog.array.filter(vseries['counters'], function(counter) {
      if (!counter['counter']['recurid']){
        return false;
      }
      return this.ctx_.helper('date').fromIsoString(recurrence).getTime() == counter['counter']['recurid'].getTime();
    }, this);
  } else if (recurrence != null && goog.isDateLike(recurrence)) {
    return goog.array.filter(vseries['counters'], function(counter) {
      if (!counter['counter']['recurid']){
        return false;
      }
      return this.ctx_.helper('date').create(counter['counter']['recurid']).getTime() == recurrence.getTime();
    }, this);
  } else {
    return goog.array.filter(vseries['counters'], function(counter) {
      return counter['counter']['recurid'] == null;
    });
  }
};

net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.getCounterByOccurrenceMixed = function(recurrence, vseries) {
  if (recurrence != null && goog.isString(recurrence)) {
    return goog.array.filter(vseries.counters, function(counter) {
      if (!counter['counter']['recurid']){
        return false;
      }
      return this.ctx_.helper('date').fromIsoString(recurrence).getTime() == counter['counter']['recurid'].getTime();
    }, this);
  } else if (recurrence != null && goog.isDateLike(recurrence)) {
    return goog.array.filter(vseries.counters, function(counter) {
      if (!counter['counter']['recurid']){
        return false;
      }
      return this.ctx_.helper('date').create(counter['counter']['recurid']).getTime() == recurrence.getTime();
    }, this);
  } else {
    return goog.array.filter(vseries.counters, function(counter) {
      return counter['counter']['recurid'] == null;
    });
  }
};


/**
 * 
 * @param recurrence
 * @param vseries
 * @returns
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.getRawOccurrence = function(recurrence, vseries) {
  if (recurrence != null && goog.isDateLike(recurrence)) {
    return goog.array.find(vseries['value']['occurrences'], function(occurrence) {
      return this.ctx_.helper('date').create(occurrence['recurid']).getTime() == recurrence.getTime();
    }, this);
  } else if (recurrence != null && goog.isString(recurrence)) {
    return goog.array.find(vseries['value']['occurrences'], function(occurrence) {
      return occurrence['recurid']['iso8601'] == recurrence;
    }, this);
  }
  return vseries['value']['main'];
};

/**
 * 
 * @param recurrence
 * @param vseries
 * @returns
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.addExdate = function(exdate, vseries) {
  vseries['value']['main']['exdate'] = vseries['value']['main']['exdate'] || [];
  vseries['value']['main']['exdate'].push(exdate);
  this.deleteRawOccurrences(exdate, vseries);
};

/**
 * 
 * @param recurrence
 * @param vseries
 * @returns
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.deleteRawOccurrences = function(exdates, vseries) {
  exdates = goog.isArray(exdates) ? exdates : [ exdates ];
  vseries['value']['occurrences'] = goog.array.filter(vseries['value']['occurrences'], function(occurrence) {
    return !goog.array.some(exdates, function(exdate) {
      return exdate['iso8601'] == occurrence['recurid']['iso8601'];
    })
  })
};

/**
 * Test if the modification needs to send a notification or not
 * 
 * @param {Object=} remote Stored version of the vseries
 * @param {Object} modified Modified version of the vevent
 * @return {boolean} Is notification needed
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.isPublicChanges = function(remote, modified) {
  var vevent = null;
  if (remote) {
    vevent = this.getRawOccurrence(modified.recurrenceId, remote) || remote['value']['main'];
  }
  return this.veventAdaptor_.isPublicChanges(vevent, modified, remote);
};

/**
 * Set or update states depending on model data.
 * 
 * @private
 * @param {Object} model
 * @param {Object} calendar Calendar model view
 * @return {Object} updated model
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.setStates_ = function(model, calendar) {
  model.states.defaultCalendar = calendar.states.defaultCalendar;
  return model;
};


/**
 *
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.adaptUntil = function(dtstart, until) {
  return this.veventAdaptor_.adaptUntil(dtstart, until)
};

/**
 * Set or update states depending on model data.
 * 
 * @private
 * @param {Object} model
 * @param {Object} calendar Calendar model view
 * @return {Object} updated model
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.updateVEventStates = function(vevent, calendar) {
  this.veventAdaptor_.updateStates(vevent, calendar);
};

/**
 * Set or update states depending on model data.
 * 
 * @private
 * @param {Object} model Vevent model view
 * @param {Object} opt_vseries VSeries model
 * @return {Object} updated vseries
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.fromVEventModelView = function(model, opt_vseries) {
  var vseries = opt_vseries || this.newVSeries(model.calendar, model.summary, model.uid);
  vseries['draft'] = model.draft;
  if (model.states.main) {
    vseries = this.adaptSeriesFromMainChanges_(vseries, model)
  } else {
    var vevent = this.veventAdaptor_.fromModelView(model);
    vevent['rrule'] = null;
    vevent['exdate'] = null;
    vevent['rdate'] = null;
    this.deleteRawOccurrences(vevent['recurid'], vseries);
    vseries['value']['occurrences'].push(vevent);
  }
  return vseries;
};

net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.adaptCounterChanges_ = function(vseries, counter, calendar) {
  var existingCounter = this.getCounterByOccurrenceApi(counter.counter.recurrenceId, vseries['value']);

  var adaptedCounter = this.veventAdaptor_.fromModelView(counter.counter);
  if (existingCounter.length == 0){
    var veventCounter = {
      "originator" : {
        "commonName" : counter.originator.commonName,
        "email" : counter.originator.email
      },
      "counter" : adaptedCounter
    };
    vseries['value']['counters'].push(veventCounter);  
  } else {
    existingCounter[0]['counter']['dtstart'] = this.ctx_.helper('date').toBMDateTime(counter.counter.dtstart, counter.counter.dtstart.timezone);
    existingCounter[0]['counter']['dtend'] = this.ctx_.helper('date').toBMDateTime(counter.counter.dtend, counter.counter.dtend.timezone);
    existingCounter[0]['counter']['attendees'][0]['partStatus'] = counter.counter.attendees[0].partStatus;
  }
  
  var correspondingEvent = this.getOccurrenceApi(counter.counter.recurrenceId, vseries['value']);
  if (correspondingEvent){
    var seriesAttendee = goog.array.find(correspondingEvent['attendees'], function(att) {
      return att.dir == counter.counter.attendees[0].dir;
    });
    seriesAttendee['partStatus'] = counter.counter.attendees[0].partStatus;
  } else {
    var utc = net.bluemind.timezone.UTC;
    var occurrence = JSON.parse(JSON.stringify(adaptedCounter));
    var start = this.ctx_.helper("date").create(vseries['value']['main']["dtstart"]);
    var end = this.ctx_.helper("date").create(vseries['value']['main']['dtend']);
    var duration = end.getTime(utc) - start.getTime(utc);
    occurrence['dtstart'] = occurrence['recurid'];
    start = this.ctx_.helper("date").create(occurrence["dtstart"]);
    var date = start.clone();
    date.setTime(date.getTime() + duration);
    occurrence["dtend"]["timezone"] = occurrence['dtstart']['timezone'];
    occurrence["dtend"]["precision"] = occurrence['dtstart']['precision'];
    occurrence["dtend"]["iso8601"] = date.toIsoString(true, true);
    vseries['value']['occurrences'] = vseries['value']['occurrences'] ? vseries['value']['occurrences'] : []; 
    vseries['value']['occurrences'].push(occurrence); 
  }
  return vseries;
}

/**
 * Use the main event modification to modify exception
 * 
 * @param {Object=} old event model
 * @param {Object} new event model
 * @return {boolean} recurring event exception
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.adaptSeriesFromMainChanges_ = function(vseries, modified) {
  if (typeof modified.acceptCounters !== 'undefined'){
    vseries['value']['acceptCounters'] = modified.acceptCounters;
  }
  var old = vseries['value']['main']
  vseries['value']['main'] = this.veventAdaptor_.fromModelView(modified);
  if (!old) {
    return vseries;
  }
  if (this.veventAdaptor_.dateHasBeenModified(old, modified)) {
    vseries['value']['main']['exdate'] = [];
    vseries['value']['occurrences'] = [];
  } else {
    //FIXME : Recurrence : If until has change, only occurrence after until should be deleted.
    this.deleteRawOccurrences(vseries['value']['main']['exdate'], vseries);
  }
  goog.array.forEach(vseries['value']['occurrences'], function(occurrence) {
    this.propagateInException_(vseries['value']['main'], old, occurrence)
  }, this)
  return vseries;
}

net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.propagateInException_ = function(vevent, old, exception) {
  
  exception['location'] = this.adjustEventValues_(old['location'], vevent['location'], exception['location']);
  exception['summary'] = this.adjustEventValues_(old['summary'], vevent['summary'], exception['summary']);
  exception['classification'] = this.adjustEventValues_(old['classification'], vevent['classification'],
  exception['classification']);
  exception['organizer'] = this.adjustEventValues_(old['organizer'], vevent['organizer'], exception['organizer']);
  exception['description'] = this.adjustEventValues_(old['description'], vevent['description'],
  exception['description']);
  exception['categories'] = this.adjustArrayValues_(old['categories'], vevent['categories'], exception['categories']);
  exception['attendees'] = this.adjustArrayValues_(old['attendees'], vevent['attendees'], exception['attendees'],
  function(a, b) {
    return (!!a['mailto'] && a['mailto'] == b['mailto']) || !!a['dir'] && a['dir'] == b['dir'] || !!a['uri']
    && a['uri'] == b['uri'];
  });
  
  return exception;
}

/**
 * @private
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.adjustEventValues_ = function(oldValue, newValue,
  exceptionValue) {
   
  if (oldValue === newValue) {
    // value not modified
    return exceptionValue;
  }
   
  if (oldValue !== exceptionValue) {
    // value has already been modified in exception, don't overwrite
    return exceptionValue;
  }
   
  // updating value
  return newValue;
    
}  
/**
 * @private
 * @param {Array.<*>} oldValue Old array value
 * @param {Array.<*>} newValue New array value
 * @param {Array.<*>} exceptionValue Exception array value
 * @return {Array.<*>} Modified exception array value
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.adjustArrayValues_ = function(oldValue, newValue,
  exceptionValue, opt_compare) {
  var compare = opt_compare || goog.array.defaultCompareEquality;
  oldValue = oldValue || [];
  newValue = newValue || [];
  exceptionValue = exceptionValue || [];
  
  goog.array.forEach(newValue, function(obj) {
    var equalObj = goog.partial(compare, obj)
    if (goog.array.findIndex(oldValue, equalObj) < 0 && goog.array.findIndex(exceptionValue, equalObj) < 0) {
      exceptionValue.push(obj);
    }
  });
  goog.array.filter(oldValue, function(obj) {
    var equalObj = goog.partial(compare, obj)
    if (goog.array.findIndex(newValue, equalObj) < 0) {
      goog.array.removeIf(exceptionValue, equalObj);
    }
  });
  return exceptionValue;
}

/**
 * create a empty series without event
 * 
 * @private
 * @param {Object} container Serie container
 * @param {string} name VSeries name
 * @param {string=} opt_uid VSeries optional uid
 * @return {Object}  vseries
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.newVSeries = function(container, name, opt_uid) {
  return {
    'container' : container,
    'uid' : opt_uid || net.bluemind.mvp.UID.generate(),
    'name' : name,
    'value' : {
      'icsUid' : net.bluemind.mvp.UID.generate(),
      'main' : null,
      'occurrences' : []
    }
  }
};
/**
 * create a empty series with an empty main event
 * 
 * @private
 * @param {Object} container Serie container
 * @param {string=} opt_uid VSeries optional uid
 * @return {Object}  vseries
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.createSeries = function(container, opt_uid) {
  var template = {
    'uid' : opt_uid || net.bluemind.mvp.UID.generate(),
    'container' : container,
    'value' : {
      'icsUid' : net.bluemind.mvp.UID.generate(),
      'main' : this.veventAdaptor_.createVEvent(),
      'occurrences' : []
    }
  };
  return template;
};

/**
 * Auto set event end after a dtstart change
 * 
 * @param {Object} model Vevent Model view object
 * @param {Object} old Old date
 */
net.bluemind.calendar.vevent.VEventSeriesAdaptor.prototype.adjustDTend = function(model, dtstart) {
  return this.veventAdaptor_.adtjustDTend(model, dtstart);
}
