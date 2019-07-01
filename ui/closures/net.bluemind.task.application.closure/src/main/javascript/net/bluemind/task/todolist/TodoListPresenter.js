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
goog.provide("net.bluemind.task.todolist.TodoListPresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("goog.date.Date");
goog.require("goog.date.DateRange");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeFormat.Format");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.task.todolist.TodoListView");

/**
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.task.todolist.TodoListPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.ctx_ = ctx;
  this.view_ = new net.bluemind.task.todolist.TodoListView(ctx);
  this.ranges_ = {};
  this.ranges_.yesterday = goog.date.DateRange.yesterday();
  this.ranges_.today = goog.date.DateRange.today();
  this.ranges_.thisWeek = goog.date.DateRange.thisWeek();
  this.ranges_.thisMonth = goog.date.DateRange.thisMonth();
  this.ranges_.past = new goog.date.DateRange(goog.date.DateRange.MINIMUM_DATE, this.ranges_.today.getStartDate());
  this.ranges_.later = new goog.date.DateRange(this.ranges_.thisMonth.getEndDate(), goog.date.DateRange.MAXIMUM_DATE);

  this.format_ = new goog.i18n.DateTimeFormat(goog.i18n.DateTimeFormat.Format.FULL_DATE);

  this.handler.listen(this.view_, net.bluemind.task.todolist.TodoListView.EventType.IMPORT, this.handleImport_);
  this.handler.listen(this.view_, net.bluemind.task.todolist.TodoListView.EventType.EXPORT, this.handleExport_);

  this.handler.listen(this.ctx_.service('todolist'),
      net.bluemind.container.service.ContainerService.EventType.CHANGE, this.handleChanged_);

}
goog.inherits(net.bluemind.task.todolist.TodoListPresenter, net.bluemind.mvp.Presenter);

/**
 * Constants
 */
net.bluemind.task.todolist.TodoListPresenter.COMPLETED = 0;
net.bluemind.task.todolist.TodoListPresenter.LATE = 1;
net.bluemind.task.todolist.TodoListPresenter.NO_DUE_DATE = 2;
net.bluemind.task.todolist.TodoListPresenter.TODAY = 3;
net.bluemind.task.todolist.TodoListPresenter.TOMOROWS = 4;
net.bluemind.task.todolist.TodoListPresenter.THIS_WEEKS = 5;
net.bluemind.task.todolist.TodoListPresenter.THIS_MONTHS = 6;
net.bluemind.task.todolist.TodoListPresenter.LATER = 7;

/**
 * Date range for sorting task.
 * 
 * @type {Object.<goog.date.DateRange>}
 * @private
 */
net.bluemind.task.todolist.TodoListPresenter.prototype.range_;

/**
 * @type {goog.ui.Component}
 * @private
 */
net.bluemind.task.todolist.TodoListPresenter.prototype.view_;

/** @override */
net.bluemind.task.todolist.TodoListPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('sub-navigation'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.task.todolist.TodoListPresenter.prototype.setup = function() {
  return this.load_();
};

/**
 * @private
 * @return {goog.Promise}
 */
net.bluemind.task.todolist.TodoListPresenter.prototype.load_ = function() {
  var container = this.ctx.session.get('container');
  var vtodos;
  return this.ctx.service('todolist').getItems(container).then(function(items) {
    vtodos = items;
    return this.ctx.service('todolist').getLocalChangeSet(container);
  }, null, this).then(function(changes) {
    this.view_.setModel(this.toModelView_(vtodos, changes))
    if (this.ctx.params.get('uid')) {
      this.view_.getChild('todos').setSelected(this.ctx.params.get('uid'));
    }
  }, null, this).thenCatch(function(e) {
    this.ctx.notifyError(net.bluemind.task.Messages.errorLoadingLists(e), e);
  }, this);
}

/** @override */
net.bluemind.task.todolist.TodoListPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Handle change on tasklists
 * 
 * @param {goog.events.Event} evt Change event.
 */
net.bluemind.task.todolist.TodoListPresenter.prototype.handleChanged_ = function(evt) {
  this.load_();
};

/**
 * Get a model for Tasklists widget.
 * 
 * @return {Array.<Object>} TaskLists view mode.
 * @private
 */
net.bluemind.task.todolist.TodoListPresenter.prototype.toModelView_ = function(todos, changes) {
  var that = this;
  var todolist = goog.array.find(this.ctx.session.get('todolists'), function(tdl) {
    return tdl['uid'] == that.ctx.session.get('container');
  });
  var canWrite = todolist && todolist['writable'];
  /**
   * I18N
   */
  /** @meaning tasks.totolist.completed */
  var MSG_COMPLETED = goog.getMsg('Completed');
  /** @meaning tasks.totolist.late */
  var MSG_LATE = goog.getMsg('Late');
  /** @meaning tasks.totolist.noDueDate */
  var MSG_NO_DUE_DATE = goog.getMsg('No due date');
  /** @meaning tasks.totolist.today */
  var MSG_TODAY = goog.getMsg('Today');
  /** @meaning tasks.totolist.tomorrow */
  var MSG_TOMORROW = goog.getMsg('Tomorrow');
  /** @meaning tasks.totolist.thisWeek */
  var MSG_THIS_WEEK = goog.getMsg('This week');
  /** @meaning tasks.totolist.thisMonth */
  var MSG_THIS_MONTH = goog.getMsg('This month');
  /** @meaning tasks.totolist.later */
  var MSG_LATER = goog.getMsg('Later');
  /** @meaning tasks.totolist.yesterday */
  var MSG_YESTERDAY = goog.getMsg('Yesterday');

  var mv = [ {
    label : MSG_COMPLETED,
    collapse : true,
    entries : []
  }, {
    label : MSG_LATE,
    warning : true,
    entries : []
  }, {
    label : MSG_NO_DUE_DATE,
    entries : []
  }, {
    label : MSG_TODAY,
    entries : []
  }, {
    label : MSG_TOMORROW,
    entries : []
  }, {
    label : MSG_THIS_WEEK,
    entries : []
  }, {
    label : MSG_THIS_MONTH,
    entries : []
  }, {
    label : MSG_LATER,
    entries : []
  } ];
  for (var i = 0; i < todos.length; i++) {
    var tags = goog.array.map(todos[i]['value']['categories'] || [], function(tag) {
      return {
        id : tag['itemUid'],
        container : tag['containerUid'],
        label : tag['label'],
        color : tag['color']
      };
    });
    var change = goog.array.find(changes, function(change) {
      return todos[i]['uid'] == change['itemId'];
    });
    var t = {
      uid : todos[i]['uid'],
      label : todos[i]['value']['summary'],
      container : todos[i]['container'],
      tags : tags,
      writable : canWrite,
      status : todos[i]['value']['status'],
      synced : !goog.isDefAndNotNull(change)
    };

    var end = todos[i]['value']['due'] ? this.ctx.helper('date').create(todos[i]['value']['due']) : null;
    if (todos[i]['value']['status'] == 'Completed') {
      mv[net.bluemind.task.todolist.TodoListPresenter.COMPLETED].entries.push(t);
      if (this.ctx_.params.get('uid') && this.ctx_.params.get('uid') == todos[i]['uid']) {
        mv[net.bluemind.task.todolist.TodoListPresenter.COMPLETED].collapse = false;
      }
    } else if (end == null) {
      mv[net.bluemind.task.todolist.TodoListPresenter.NO_DUE_DATE].entries.push(t);
    } else if (this.ranges_.today.contains(end)) {
      mv[net.bluemind.task.todolist.TodoListPresenter.TODAY].entries.push(t);
    } else if (this.ranges_.yesterday.contains(end)) {
      t.date = MSG_YESTERDAY;
      mv[net.bluemind.task.todolist.TodoListPresenter.LATE].entries.push(t);
    } else if (this.ranges_.past.contains(end)) {
      t.date = this.format_.format(end); // FORMAT!
      mv[net.bluemind.task.todolist.TodoListPresenter.LATE].entries.push(t);
    } else if (this.ranges_.thisWeek.contains(end)) {
      t.date = this.format_.format(end); // FORMAT!
      mv[net.bluemind.task.todolist.TodoListPresenter.THIS_WEEKS].entries.push(t);
    } else if (this.ranges_.thisMonth.contains(end)) {
      t.date = this.format_.format(end); // FORMAT!
      mv[net.bluemind.task.todolist.TodoListPresenter.THIS_MONTHS].entries.push(t);
    } else if (this.ranges_.later.contains(end)) {
      t.date = this.format_.format(end); // FORMAT!
      mv[net.bluemind.task.todolist.TodoListPresenter.LATER].entries.push(t);
    }
  }
  return mv;
};

/**
 * Export action
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.task.todolist.TodoListPresenter.prototype.handleExport_ = function(evt) {
  var container = this.ctx.params.get('container');
  if (container) {
    var query = new goog.Uri.QueryData();
    query.set('containerUid', container);
    goog.global.window.open('export-vtodo?' + query.toString());
  } else {
    // FIXME export SEARCH?
  }
};
/**
 * Export action
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.task.todolist.TodoListPresenter.prototype.handleImport_ = function(evt) {
  var query = new goog.Uri.QueryData;
  query.set("containerUid", this.ctx.params.get('container'));
  var uri = "import-vtodo?" + query;

  var form = evt.target.getElement().getElementsByTagName('form').item(0);
  var io = new goog.net.IframeIo();
  this.handler.listenOnce(io, goog.net.EventType.SUCCESS, function(e) {
    var resp = goog.global.JSON.parse(io.getResponseText());
    this.monitor_(resp);
  }, this);
  io.sendFromForm(form, uri);
};
