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

/** @fileoverview Presenter for the application search bar */

goog.provide("net.bluemind.calendar.tasks.TasksPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.string");
goog.require("net.bluemind.calendar.ColorPalette");
goog.require("net.bluemind.calendar.tasks.TasksView");
goog.require("net.bluemind.calendar.navigation.events.EventType");
goog.require("net.bluemind.container.service.ContainerService.EventType");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.tag.service.TagService");
goog.require("net.bluemind.calendar.tasks.events.EventType");
goog.require("net.bluemind.calendar.vtodo.TodolistsManager");
goog.require("net.bluemind.calendar.Messages");
goog.require("net.bluemind.task.Messages");
/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.tasks.TasksPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.calendar.tasks.TasksView(ctx);
  this.registerDisposable(this.view_);
  this.todolists_ = new net.bluemind.calendar.vtodo.TodolistsManager(ctx);
  this.skipNextRefresh = false;
};
goog.inherits(net.bluemind.calendar.tasks.TasksPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.calendar.tasks.TasksView}
 * @private
 */
net.bluemind.calendar.tasks.TasksPresenter.prototype.view_;

/**
 * @type {net.bluemind.calendar.vtodo.TodolistsManager}
 * @private
 */
net.bluemind.calendar.tasks.TasksPresenter.prototype.todolists_;

/** @override */
net.bluemind.calendar.tasks.TasksPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('content-right'));

  this.handler.listen(this.view_, net.bluemind.calendar.tasks.events.EventType.TOGGLE_VIEW, this.handleToggleView_);
  this.handler.listen(this.view_, net.bluemind.calendar.tasks.events.EventType.TOGGLE_STATUS, this.markAsDone_);
  this.handler.listen(this.view_, net.bluemind.calendar.tasks.events.EventType.DELETE, this.handleDelete_);
  this.handler.listen(this.view_, net.bluemind.calendar.tasks.events.EventType.SUMMARY_CHANGE,
      this.handleSummaryChange_);
  this.handler.listen(this.ctx.service('todolist'), net.bluemind.container.service.ContainerService.EventType.CHANGE,
      this.setup);
  this.handler.listen(this.ctx.service('todolists'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.setup);
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.tasks.TasksPresenter.prototype.setup = function() {
  if (this.skipNextRefresh) {
    this.skipNextRefresh = false;
    return;
  }

  var model = {
    'late' : [],
    'today' : [],
    'tomorrow' : [],
    'this-week' : [],
    'this-month' : [],
    'no-due' : []
  };

  var cookies = new goog.net.Cookies(window.document);
  model.show = cookies.get('show-task') == "true";
  if (!model.show) {
    this.view_.setModel(model);
    this.view_.refresh();
    return goog.Promise.resolve();
  }

  var rangeLate = new net.bluemind.date.DateRange(goog.date.DateRange.MINIMUM_DATE, new net.bluemind.date.Date());
  var rangeToday = new net.bluemind.date.DateRange.today();
  var rangeTommorow = new net.bluemind.date.DateRange(new net.bluemind.date.Date(), new net.bluemind.date.Date());
  var rangeThisWeek = new net.bluemind.date.DateRange.thisWeek();
  var rangeThisMonth = new net.bluemind.date.DateRange.thisMonth();
  return this.todolists_.getTodolistsModelView().then(function(todolists) {
    todolists = goog.array.filter(todolists, function(todolist) {
      return todolist.states.visible;
    }, this);

    var all = goog.array.map(todolists, function(todolist) {
      return this.ctx.service('todolist').getItems(todolist.uid).then(function(tasks) {
        return goog.array.map(goog.array.filter(tasks, function(task) {
          // filter out completed tasks
          return task['value']['status'] != 'Completed';
        }), function(task) {
          return this.modelToView_(task, todolist);
        }, this);
      }, null, this)
    }, this);
    return goog.Promise.all(all);
  }, null, this).then(function(tasks) {
    tasks = goog.array.flatten(tasks);
    goog.array.forEach(tasks, function(t) {
      if (t.due) {
        if (rangeToday.contains(t.due)) {
          model['today'].push(t);
        } else if (rangeLate.contains(t.due)) {
          model['late'].push(t);
        } else if (rangeTommorow.contains(t.due)) {
          model['tomorrow'].push(t);
        } else if (rangeThisWeek.contains(t.due)) {
          model['this-week'].push(t);
        } else if (rangeThisMonth.contains(t.due)) {
          model['this-month'].push(t);
        }
      } else {
        model['no-due'].push(t);
      }
    }, this);
    this.view_.setModel(model);
    this.view_.refresh();
  }, null, this).thenCatch(function(error) {
    this.ctx.notifyError(net.bluemind.calendar.Messages.errorLoading(error), error);
  }, this);
};

/** @override */
net.bluemind.calendar.tasks.TasksPresenter.prototype.exit = function() {
  // FIXME is it the right place to do that

  // hide content-right panel
  var main = this.view_.getDomHelper().getElement('content-body');
  goog.style.setStyle(main, "margin-right", "0px");
  goog.style.setStyle(this.view_.getDomHelper().getElement('content-right'), "width", "0px");

  return goog.Promise.resolve();
};

net.bluemind.calendar.tasks.TasksPresenter.prototype.modelToView_ = function(vtodo, todolist) {
  // FIXME copy/paste from VTodoManager.js
  var model = {};
  model.writable = todolist['writable'];
  model.id = vtodo['uid'];
  model.uid = vtodo['value']['uid'];
  model.container = vtodo['container'];
  model.summary = vtodo['value']['summary'];
  model.description = vtodo['value']['description'];
  model.location = vtodo['value']['location'];
  var helper = this.ctx.helper('date');
  if (vtodo['value']['dtstart']) {
    model.start = this.ctx.helper('date').create(vtodo['value']['dtstart']);
  }
  if (vtodo['value']['due']) {
    model.due = this.ctx.helper('date').create(vtodo['value']['due']);
  }
  if (vtodo['value']['completed']) {
    model.completed = this.ctx.helper('date').create(vtodo['value']['completed']);
  }
  model.percent = vtodo['value']['percent'];

  model.status = vtodo['value']['status'];

  model.priority = vtodo['value']['priority'];

  if (vtodo['value']['alarm']) {
    model.alarm = goog.array.map(vtodo['value']['alarm'], function(alarm) {
      if (alarm['trigger'] != null) {
        alarm['trigger'] = alarm['trigger'] * -1;
      }
      return {
        action : alarm['action'],
        trigger : alarm['trigger']
      }
    })
  }

  model.tags = goog.array.map(vtodo['value']['categories'] || [], function(tag) {
    return {
      id : tag['itemUid'],
      container : tag['containerUid'],
      label : tag['label'],
      color : tag['color']
    };
  });

  model.color = todolist.color.background;
  return model;
}

net.bluemind.calendar.tasks.TasksPresenter.prototype.markAsDone_ = function(model) {
  this.ctx.service('todolist').getItem(model.containerUid, model.uid).then(function(task) {
    if (task) {
      var currentStatus = task['value']['status'];
      if (currentStatus == 'Completed') {
        task['value']['status'] = 'InProcess';
      } else {
        task['value']['status'] = 'Completed';
      }
      task['container'] = model.containerUid;
      this.skipNextRefresh = true;
      return this.ctx.service('todolist').update(task);
    }
  }, null, this).then(function() {
    this.ctx.notifyInfo(net.bluemind.task.Messages.successUpdate());
  }, function(error) {
    this.ctx.notifyError(net.bluemind.task.Messages.errorUpdate(error), error);
  }, this)
}

net.bluemind.calendar.tasks.TasksPresenter.prototype.handleToggleView_ = function() {
  var cookies = new goog.net.Cookies(window.document);
  var current = cookies.get('show-task') == "true";
  cookies.set('show-task', (!current) ? "true" : "false", 60 * 60 * 24 * 5, '/cal', null, goog.string.startsWith(window.location.protocol, 'https'));
  this.setup();
}

net.bluemind.calendar.tasks.TasksPresenter.prototype.handleSummaryChange_ = function(model) {
  this.ctx.service('todolist').getItem(model.containerUid, model.uid).then(function(task) {
    if (task) {
      task['value']['summary'] = model.text;
      task['container'] = model.containerUid;
      return this.ctx.service('todolist').update(task);
    } else {
      console.log("No task for " + model.containerUid + ":" + model.uid);
    }
  }, null, this).then(function() {
    this.ctx.notifyInfo(net.bluemind.task.Messages.successUpdate());
    this.ctx.helper('url').reload();
  }, function(error) {
    this.ctx.notifyError(net.bluemind.task.Messages.errorUpdate(error), error);
  }, this);
}

net.bluemind.calendar.tasks.TasksPresenter.prototype.handleDelete_ = function(model) {
  this.ctx.service('todolist').deleteItem(model.containerUid, model.uid).then(function() {
    this.ctx.notifyInfo(net.bluemind.task.Messages.successDelete());
    // FIXME: location
    this.ctx.helper('url').goTo('/', 'container');
  }, function(error) {
    this.ctx.notifyError(net.bluemind.task.Messages.errorDelete(error), error);
  }, this);

}
