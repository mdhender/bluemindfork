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

/**
 * @fileoverview Provide services for task lists
 */

goog.provide("net.bluemind.calendar.CalendarsMgmt");

goog.require("goog.events.EventHandler");
goog.require("goog.events.EventTarget");
goog.require("goog.structs.Map");
goog.require("goog.array");
goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.core.container.api.ContainerManagementClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

/**
 * Service proxy to provide calendar listing adapted to calendar application usage.
 * Add color and other metada to calendar.
 * Filter calendars with visibility.
 * ...
 * 
 * Todolist as calendar are also handled with this service
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @constructor
 * @extends {net.bluemind.container.service.ContainersService}
 */
net.bluemind.calendar.CalendarsMgmt = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.css_ = ctx.service('calendars').css_;
  this.cache_ = new goog.structs.Map();
  this.handler = new goog.events.EventHandler(this);
  var todolists = this.ctx.service('todolists');
  var calendars = this.ctx.service('calendars');
  this.handler.listen(calendars, net.bluemind.container.service.ContainersService.EventType.CHANGE, function() {
    this.cache_.remove('displayed-calendars');
  });
  this.handler.listen(todolists, net.bluemind.container.service.ContainersService.EventType.CHANGE, function() {
    this.cache_.remove('displayed-calendars');
  });
};
goog.inherits(net.bluemind.calendar.CalendarsMgmt, goog.events.EventTarget);

/**
 * 
 * @param states
 * @param params
 * @returns
 */
net.bluemind.calendar.CalendarsMgmt.prototype.handleByState = function(states, params) {
  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params);
};

/**
 * Does service use local storage
 * @returns {boolean}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.isLocal = function() {
  return this.css_.available();
};

/**
 * Set the visible calendars in the calendar application
 * 
 * @param {Array.<Object>} calendars Calendars to add
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setCalendars = function(calendars) {
  var uids = goog.array.map(calendars, function(c) {
    return c['uid'];
  });
  return this.ctx.service('auth').set('calendar.calendars', uids).then(function() {
    return this.ctx.service('folders').getFolders('calendar');
  }, null, this).then(function(syncedCalendars) {
    this.cache_.remove('displayed-calendars');
    syncedCalendars = goog.array.filter(syncedCalendars || [], function(calendar) {
      return !goog.array.contains(uids, calendar['uid']);
    });
    var storedCalendars = goog.array.concat(calendars, syncedCalendars);
    return this.handleByState({
      'local,remote' : this.setCalendarsLocal_, //
      'local' : this.setCalendarsLocal_, //
      'remote' : this.setCalendarsPublic_
    }, [ storedCalendars ]);
  }, null, this);

}

/**
 * Set the calendars used by the calendar application
 * 
 * @param {Array.<Object>} calendars Calendars to add
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setCalendarsLocal_ = function(calendars) {
  return this.css_.sync('calendar', calendars);
}

/**
 * Set the calendars used by the calendar application
 * 
 * @param {Array.<Object>} calendars Calendars to add
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setCalendarsPublic_ = function(calendars) {
  return this.ctx.service('todolists').list().then(function(todolists) {
    goog.array.extend(calendars, goog.array.filter(todolists, function(todolist) {
      return goog.array.findIndex(calendars, function(calendar) {
        return calendar['uid'] == todolist['uid'];
      }) < 0;
    }));
    this.cache_.set('stored-calendars', calendars);
  }, null, this);
}

/**
 * Remove a calendar
 * 
 * @param {string} uid
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.removeCalendar = function(uid) {
  return this.handleByState({
    'local,remote' : this.removeCalendarLocal_,
    'local' : this.removeCalendarLocal_,
    'remote' : this.removeCalendarPublic_
  }, [ uid ]).then(function() {
    return this.ctx.service('auth').get('calendar.calendars');
  }, null, this).then(function(displayed) {
    goog.array.remove(displayed, uid);
    return this.ctx.service('auth').set('calendar.calendars', displayed);
  }, null, this).then(function() {
    this.cache_.remove('displayed-calendars');
  }, null, this);

}

net.bluemind.calendar.CalendarsMgmt.prototype.removeCalendarLocal_ = function(uid) {
  return this.ctx.service('folders').getFolder(uid).then(function(folder) {
    if (!folder) {
      return this.ctx.service('calendars').removeCalendar(uid);
    }
  }, null, this);
}

net.bluemind.calendar.CalendarsMgmt.prototype.removeCalendarPublic_ = function(uid) {
  var calendars = this.cache_.get('stored-calendars');
  calendars = goog.array.filter(calendars, function(calendar) {
    return calendar['uid'] != uid;
  });
  this.cache_.set('stored-calendars', calendars);
  return new goog.Promise.resolve();
}

/**
 * Add a set of calendars to storage
 * 
 * @param {Array.<Object>} calendars Calendars to add
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.addCalendars = function(calendars) {
  var uids = goog.array.map(calendars, function(calendar) {
    return calendar['uid'];
  })
  return this.list('calendar').then(function(current) {
    goog.array.forEach(current, function(calendar) {
      if (!goog.array.contains(uids, calendar['uid'])) {
        calendars.push(calendar);
      }
    });
    return this.setCalendars(calendars);
  }, null, this);
}

/**
 * Get a visible calendar
 * @param {string} uid Calendar uid
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.get = function(uid) {
  return this.list().then(function(calendars) {
    return goog.array.find(calendars, function(calendar) {
      return calendar['uid'] == uid;
    })
  });
};

/**
 * Get all visible calendars
 * @param {string=} opt_type Optional calendar type filter
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.list = function(opt_type) {
  if (this.cache_.containsKey('displayed-calendars')) {
    var promise = goog.Promise.resolve(this.cache_.get('displayed-calendars'));
  } else {
    var promise = this.handleByState({
      'local,remote' : this.listLocal_,
      'local' : this.listLocal_,
      'remote' : this.listPublic_
    }, []).then(function(calendars) {
      this.cache_.set('displayed-calendars', calendars);
      return calendars;
    }, null, this);

  }
  if (opt_type) {
    promise = promise.then(function(calendars) {
      return goog.array.filter(calendars, function(calendar) {
        return calendar['type'] == opt_type;
      })
    })
  }
  return promise;
};

/**
 * Get all visible calendars
 * 
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.CalendarsMgmt.prototype.listLocal_ = function() {
  var data = {};
  return this.ctx.service('calendars').list().then(function(calendars) {
    data.calendars = calendars;
    return this.ctx.service('auth').get('calendar.calendars');
  }, null, this).then(function(displayed) {
    if (displayed) {
      data.calendars = goog.array.filter(data.calendars, function(calendar) {
        return goog.array.contains(displayed, calendar['uid'])
      });
    }
    return goog.Promise.all(goog.array.map(data.calendars, this.loadImage_, this));
  }, null, this).then(function(calendars) {
    return this.ctx.service('todolists').list();
  }, null, this).then(function(todolists) {
    goog.array.extend(data.calendars, todolists);
    return this.ctx.service('metadataMgmt').synchronize(data.calendars);
  }, null, this);
}

/**
 * Load calendar image
 * @private
 */
net.bluemind.calendar.CalendarsMgmt.prototype.loadImage_ = function(calendar) {
  if (!this.ctx.online) {
    return goog.Promise.resolve(calendar);
  }
  var dir = new net.bluemind.directory.api.DirectoryClient(this.ctx.rpc, '', calendar['domainUid']);
  return dir.getEntryIcon(calendar['owner']).then(function(photo) {
    if (goog.isDefAndNotNull(photo) && goog.isString(photo) && goog.string.trim(photo).length > 0) {
      calendar['photo'] = "data:image/png;base64," + photo;
    }
    return calendar;
  });
};

/**
 * Get all visible calendars
 * 
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.listPublic_ = function() {
  var calendars = this.cache_.get('stored-calendars')
  return this.ctx.service('auth').get('calendar.calendars').then(function(displayed) {
    if (displayed) {
      calendars = goog.array.filter(calendars, function(calendar) {
        return calendar['type'] != 'calendar' || goog.array.contains(displayed, calendar['uid']);
      });
    }
    return calendars
  }, null, this).then(function(calendars) {
    return this.ctx.service('metadataMgmt').synchronize(calendars);
  }, null, this);
}

/**
 * Set calendar color
 * 
 * @param {string} uid Calendar uid
 * @param {string} color Color Hex code
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setColor = function(calUid, color) {
  var settings = {
    'bm_color' : color
  }
  return this.get(calUid).then(function(calendar) {
    if (calendar['type'] == 'calendar') {
      return this.ctx.service('calendars').setSettings(calUid, settings)
    } else if (calendar['type'] == 'todolist') {
      return this.ctx.service('todolists').setSettings(calUid, settings)
    }
  }, null, this).then(function() {
    return this.handleByState({
      'local,remote' : this.setColorLocal_,
      'local' : this.setColorLocal_,
      'remote' : this.setColorPublic_
    }, [ calUid, color ]);
  }, null, this).then(function() {
    this.cache_.remove('displayed-calendars');
  }, null, this);
}

/**
 * Set calendar color
 * 
 * @param {string} uid Calendar uid
 * @param {string} color Color Hex code
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setColorLocal_ = function(calUid, color) {
  return goog.Promise.resolve();
};

/**
 * Set calendar color
 * 
 * @param {string} uid Calendar uid
 * @param {string} color Color Hex code
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setColorPublic_ = function(calUid, color) {
  var calendar = goog.array.find(this.cache_.get('stored-calendars'), function(calendar) {
    return calUid == calendar['uid'];
  });
  calendar['settings']['bm_color'] = color;
  return goog.Promise.resolve();
};

/**
 * Set calendar visibility
 * 
 * @param {string} uid Calendar uid
 * @param {boolean} visibility Is calendar visible
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setVisibility = function(calUid, visible) {
  return this.ctx.service('auth').get('calendar.visibility').thenCatch(function() {
    return {};
  }, this).then(function(visibility) {
    if (visible == null) {
      goog.object.remove(visibility, calUid);
    } else {
      visibility[calUid] = visible;
    }
    return this.ctx.service('auth').set('calendar.visibility', visibility);
  }, null, this).then(function() {
    return this.handleByState({
      'local,remote' : this.setVisibilityLocal_,
      'local' : this.setVisibilityLocal_,
      'remote' : this.setVisibilityPublic_
    }, [ calUid, visible ]);
  }, null, this).then(function() {
    this.cache_.remove('displayed-calendars');
  }, null, this);
}

/**
 * Set calendar visibility
 * 
 * @param {string} uid Calendar uid
 * @param {boolean} visibility Is calendar visible
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setVisibilityLocal_ = function(calUid, visible) {
  return goog.Promise.resolve();
}

/**
 * Set calendar visibility
 * 
 * @param {string} uid Calendar uid
 * @param {boolean} visibility Is calendar visible
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.CalendarsMgmt.prototype.setVisibilityPublic_ = function(calUid, visible) {
  return goog.Promise.resolve();
}

/**
 * Get default calendar
 * 
 * @return {goog.Promise}
 */
net.bluemind.calendar.CalendarsMgmt.prototype.getDefaultCalendar = function() {
  return this.list().then(function(calendars) {
    var def = goog.array.find(calendars, function(calendar) {
      return calendar['uid'] && calendar['defaultContainer'] && calendar['owner'] == this.ctx.user['uid'];
    }) || goog.array.find(calendars, function(calendar) {
      return calendar['uid'] && calendar['defaultContainer'] && calendar['writable'];
    }) || calendars[0];
    return def;
  }, null, this);
};
