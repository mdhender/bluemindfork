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

goog.provide("net.bluemind.calendar.vtodo.TodolistsManager");

goog.require("goog.Promise");
goog.require("goog.Uri");
goog.require("goog.array");
goog.require("net.bluemind.calendar.ColorPalette");
goog.require("net.bluemind.calendar.vtodo.VTodoAdaptor");
goog.require("net.bluemind.rrule.OccurrencesHelper");

/**
 * Handle calendars action in navigation
 * 
 * @param {net.bluemind.mvp.ApplicationContect}
 * @constructor
 */
net.bluemind.calendar.vtodo.TodolistsManager = function(ctx) {
  this.ctx_ = ctx;
  this.adaptor_ = new net.bluemind.calendar.vtodo.VTodoAdaptor(ctx);
  this.occurrences_ = new net.bluemind.rrule.OccurrencesHelper();
};

/**
 * @type {net.bluemind.mvp.ApplicationContect}
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.ctx_;

/**
 * @type {net.bluemind.calendar.vtodo.VTodoAdaptor}
 * @private
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.adaptor_;

/**
 * List all calendars, and convert them to model view.
 * 
 * @return {goog.Promise}
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.getTodolistsModelView = function() {
  return this.ctx_.service('calendarsMgmt').list('todolist').then(function(todolists) {
    return goog.array.map(todolists, function(todolist) {
      return this.todolistToMoldeView_(todolist);
    }, this);
  }, null, this);
};

/**
 * get a todolist, and convert it to model view.
 * 
 * @return {goog.Promise}
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.getTodolistModelView = function(uid) {
  return this.ctx_.service('todolists').get(uid).then(function(todolist) {
    return this.todolistToMoldeView_(todolist, true);
  }, null, this);
};

/**
 * Convert todolist to model view.
 * 
 * @param {Object} todolist Json
 * @param {boolean=} opt_skipMetadata Do not load metadata
 * @return {Object}
 * @private
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.todolistToMoldeView_ = function(todolist, opt_skipMetadata) {
  var dir;
  if (todolist['dir'] && todolist['dir']['path']) {
    var dir = 'bm://' + todolist['dir']['path'];
  }

  var owner = null;
  if (todolist['owner'] && todolist['owner'] != this.ctx_.user['uid']) {
    owner = todolist['ownerDisplayname'];
  }
  var mv = {};
  mv.uid = todolist['uid'];
  mv.type = 'todolist';
  mv.writable = todolist['writable'];
  mv.name = todolist['name'];
  mv.owner = todolist['owner'];
  mv.ownerDisplayname = owner;
  mv.dir = dir;
  mv.photo = '/api/directory/' + todolist['domainUid'] + '/entry-uid/' + todolist['owner'] + '/icon';
  mv.states = {
    master : false,
    writable : false
  };
  if (!opt_skipMetadata) {
    mv.states.visible = todolist['metadata']['visible'];
    mv.color = {
      background : todolist['metadata']['color'],
      foreground : net.bluemind.calendar.ColorPalette.textColor(todolist['metadata']['color'], -0.3)
    };
  }
  return mv;
};

/**
 * search vtodos from a set of todolists in a defined range. Only visible
 * todolists will be proceed
 * 
 * @param {string} uid Container uid
 * @param {string} pattern Search pattern
 * @param {Array.<goog.date.Date>=} opt_limits
 * @param {goog.date.Date} date Start date range.
 * @return {goog.Promise}
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.searchTodolistsVTodos = function(todolists, pattern, limits,
    date) {
  var result = []
  var promise = goog.Promise.resolve(result);
  goog.array.forEach(todolists, function(todolist) {
    if (todolist.states.visible) {
      promise = promise.then(function() {
        return this.ctx_.service('todolist').search(todolist.uid, pattern, limits, date);
      }, null, this).then(function(vtodos) {
        goog.array.extend(result, goog.array.map(vtodos, function(vtodo) {
          return this.adaptor_.toModelView(vtodo, todolist);
        }, this));
        return result;
      }, null, this);
    }
  }, this);
  return promise;
};

/**
 * List vtodos from a set of todolists in a defined range. Only visible
 * todolists will be proceed
 * 
 * @param {string} uid Container uid
 * @param {net.bluemind.date.DateRange} range Date range.
 * @return {goog.Promise}
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.getTodolistsVTodos = function(todolists, range) {
  var uids = goog.array.map(goog.array.filter(todolists, function(todolist) {
    return todolist.states.visible;
  }), function(todolist) {
    return todolist.uid
  });
  
  return this.getVTodos_(range, uids).then(function(vtodos) {
    return goog.array.map(vtodos, function(vtodo) {
      var todolist = goog.array.find(todolists, function(todolist) {
        return todolist.uid == vtodo['container'];
      })
      return this.adaptor_.toModelView(vtodo, todolist);
    }, this);
  }, null, this);
};

/**
 * List vtodo  in a defined range. Only todos matching optional
 * view filters will be returned
 * 
 * @param {net.bluemind.date.DateRange} range Date range.
 * @param {Array.<string>} uids Containers uid
 * @return {goog.Promise}
 * @private
 */
net.bluemind.calendar.vtodo.TodolistsManager.prototype.getVTodos_ = function(range, uids) {

  return this.ctx_.service('todolists').getVTodos(range, uids).then(function(vtodos) {
    var tags = this.ctx_.session.get('selected-tag') || [];
    if (!goog.array.isEmpty(tags)) {
      vtodos = goog.array.filter(vtodos, function(vtodo) {
        return goog.array.findIndex(vtodo['value']['categories'] || [], function(cat) {
          return goog.array.contains(tags, cat['itemUid']);
        }) > 0;
      });
    }

    return this.occurrences_.getEventOccurrences(this.ctx_, vtodos, range);
  }, null, this);
};
