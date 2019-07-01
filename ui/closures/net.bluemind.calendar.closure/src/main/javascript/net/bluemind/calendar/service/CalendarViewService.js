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

/**
 * @fileoverview Provide services for calendars lists
 */

goog.provide("net.bluemind.calendar.service.CalendarViewService");

goog.require("net.bluemind.calendar.api.CalendarViewClient");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

/**
 * Service provider object for Calendar
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * 
 * @constructor
 * @extends {goog.events.EventTarget}
 */
net.bluemind.calendar.service.CalendarViewService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'calendarview');

  this.handler_ = new goog.events.EventHandler(this);
  this.handler_.listen(this.cs_, net.bluemind.container.service.ContainerService.EventType.CHANGE, this.handleChange_);
};
goog.inherits(net.bluemind.calendar.service.CalendarViewService, goog.events.EventTarget);

net.bluemind.calendar.service.CalendarViewService.prototype.handleByState = function(states, params) {
  var localState = [];
  if (this.cs_.available()) {
    localState.push('local');
  }
  if (this.ctx.online) {
    localState.push('remote');
  }
  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
};

net.bluemind.calendar.service.CalendarViewService.prototype.isLocal = function() {
  return this.cs_.available();
};

net.bluemind.calendar.service.CalendarViewService.prototype.getViews = function() {
  return this.handleByState({
    'local' : this.getViewsLocal, //
    'remote' : this.getViewsRemote
  }, []);

}

net.bluemind.calendar.service.CalendarViewService.prototype.getViewsLocal = function() {
  return this.cs_.getItems("calendarview:" + this.ctx.user['uid']);
}
net.bluemind.calendar.service.CalendarViewService.prototype.getViewsRemote = function() {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.list().then(function(r) {
    return r['values'];
  });
}

net.bluemind.calendar.service.CalendarViewService.prototype.getView = function(id) {
  return this.handleByState({
    'local' : this.getViewLocal, //
    'remote' : this.getViewRemote
  }, [ id ]);

}

net.bluemind.calendar.service.CalendarViewService.prototype.getViewLocal = function(id) {
  return this.cs_.getItem("calendarview:" + this.ctx.user['uid'], id);
}
net.bluemind.calendar.service.CalendarViewService.prototype.getViewRemote = function(id) {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.getComplete(id);
}

net.bluemind.calendar.service.CalendarViewService.prototype.createView = function(uid, value) {
  return this.handleByState({
    'local,remote' : this.createViewLocalRemote, //
    'local' : this.createViewLocal, //
    'remote' : this.createViewRemote
  }, [ uid, value ]);
}

net.bluemind.calendar.service.CalendarViewService.prototype.createViewLocalRemote = function(uid, value) {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.create(uid, value).then(function() {
    return this.cs_.storeItemWithoutChangeLog({
      'uid' : uid,
      'container' : "calendarview:" + this.ctx.user['uid'],
      'name': value['label'],
      'value' : value
    });
  }, null, this);

}

net.bluemind.calendar.service.CalendarViewService.prototype.createViewLocal = function(uid, value) {
  var view = {
    'uid' : uid,
    'container' : "calendarview:" + this.ctx.user['uid'],
    'value' : value
  };
  return this.cs_.storeItem(view);
}

net.bluemind.calendar.service.CalendarViewService.prototype.createViewRemote = function(uid, value) {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.create(uid, value);
}

net.bluemind.calendar.service.CalendarViewService.prototype.updateView = function(uid, value) {
  return this.handleByState({
    'local,remote' : this.updateViewLocalRemote, //
    'local' : this.updateViewLocal, //
    'remote' : this.updateViewRemote
  }, [ uid, value ]);
}

net.bluemind.calendar.service.CalendarViewService.prototype.updateViewLocalRemote = function(uid, value) {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.update(uid, value).then(function() {
    return this.cs_.storeItemWithoutChangeLog({
      'uid' : uid,
      'container' : "calendarview:" + this.ctx.user['uid'],
      'value' : value
    });
  }, null, this);

}

net.bluemind.calendar.service.CalendarViewService.prototype.updateViewLocal = function(uid, value) {
  var view = {
    'uid' : uid,
    'container' : "calendarview:" + this.ctx.user['uid'],
    'value' : value
  };
  return this.cs_.storeItem(view);
}

net.bluemind.calendar.service.CalendarViewService.prototype.updateViewRemote = function(uid, value) {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.update(uid, value);
}

net.bluemind.calendar.service.CalendarViewService.prototype.deleteView = function(uid, value) {
  return this.handleByState({
    'local,remote' : this.deleteViewLocalRemote, //
    'local' : this.deleteViewLocal, //
    'remote' : this.deleteViewRemote
  }, [ uid, value ]);
}

net.bluemind.calendar.service.CalendarViewService.prototype.deleteViewLocalRemote = function(uid, value) {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.delete_(uid).then(function() {
    return this.cs_.deleteItemWithoutChangeLog("calendarview:" + this.ctx.user['uid'], uid);
  }, null, this);

}

net.bluemind.calendar.service.CalendarViewService.prototype.deleteViewLocal = function(uid, value) {
  return this.cs_.deleteItemWithoutChangeLog("calendarview:" + this.ctx.user['uid'], uid);
}

net.bluemind.calendar.service.CalendarViewService.prototype.deleteViewRemote = function(uid, value) {
  var client = new net.bluemind.calendar.api.CalendarViewClient(this.ctx.rpc, '', "calendarview:"
      + this.ctx.user['uid']);
  return client.delete_(uid);
}

/**
 * If an event storage is raised to notify that the on calendar container has
 * changed (added, removed, renamed, ..) then this method rise a foldersChanged
 * (sick) event.
 * 
 * @param {goog.events.BrowserEvent} evt Storage event.
 */
net.bluemind.calendar.service.CalendarViewService.prototype.handleChange_ = function(evt) {
  this.dispatchEvent(net.bluemind.container.service.ContainerService.EventType.CHANGE);
}
