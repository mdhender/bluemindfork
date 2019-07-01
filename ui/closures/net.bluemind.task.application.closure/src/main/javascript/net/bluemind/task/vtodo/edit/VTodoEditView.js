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
goog.provide("net.bluemind.task.vtodo.edit.VTodoEditView");

goog.require("goog.iter");
goog.require("goog.ui.Component");
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.ToolbarButton");
goog.require("goog.ui.ToolbarMenuButton");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.MenuButtonRenderer");
goog.require("net.bluemind.task.vtodo.edit.ui.VTodoForm");
goog.require("bluemind.ui.style.DangerousActionButtonRenderer");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");
goog.require("net.bluemind.history.HistoryDialog");



/**
 * @constructor
 * 
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.task.vtodo.edit.VTodoEditView = function(ctx) {
  this.ctx_ = ctx;
  goog.ui.Component.call(this);
  this.createToolBar();
  var child = new goog.ui.Control();
  child.setId('notice');
  child.addClassName(goog.getCssName('notification'));
  this.addChild(child, true);
  child = new net.bluemind.task.vtodo.edit.ui.VTodoForm(ctx);
  child.setId('form');
  this.addChild(child, true);
  var history = new net.bluemind.history.HistoryDialog(ctx);
  history.setId('history-dialog');
  this.addChild(history, true);
}
goog.inherits(net.bluemind.task.vtodo.edit.VTodoEditView, goog.ui.Component);

net.bluemind.task.vtodo.edit.VTodoEditView.prototype.createToolBar = function() {
  var button, menu, toolbar = new goog.ui.Toolbar();
  toolbar.setId('toolbar');
  this.addChild(toolbar, true);

  /** @meaning general.save */
  var MSG_SAVE = goog.getMsg('Save');
  button = new goog.ui.ToolbarButton(MSG_SAVE, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  button.setId('save');
  toolbar.addChild(button, true);

  /** @meaning general.markAsDone */
  var MSG_MARK_AS_DONE = goog.getMsg('Mark as done');
  button = new goog.ui.ToolbarButton(MSG_MARK_AS_DONE, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  button.setId('markAsDone');
  button.setVisible(false);
  toolbar.addChild(button, true);
  
  /** @meaning general.history */
  var MSG_HISTORY = goog.getMsg('History');
  button = new goog.ui.ToolbarButton(MSG_HISTORY, goog.ui.style.app.ButtonRenderer.getInstance());
  button.setId('history');
  button.setVisible(false);
  toolbar.addChild(button, true);

  /** @meaning general.copy */
  var MSG_COPY_TO = goog.getMsg('Copy to...')
  menu = new goog.ui.Menu();
  button = new goog.ui.ToolbarMenuButton(MSG_COPY_TO, menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  button.setId('copy');
  button.setVisible(false);
  toolbar.addChild(button, true);

  /** @meaning general.move */
  var MSG_MOVE_TO = goog.getMsg('Move to...')
  menu = new goog.ui.Menu();
  button = new goog.ui.ToolbarMenuButton(MSG_MOVE_TO, menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  button.setId('move');
  button.setVisible(false);
  toolbar.addChild(button, true);

  /** @meaning general.delete */
  var MSG_DELETE = goog.getMsg('Delete');
  button = new goog.ui.ToolbarButton(MSG_DELETE, bluemind.ui.style.DangerousActionButtonRenderer.getInstance());
  button.setId('delete');
  button.setVisible(false);
  toolbar.addChild(button, true);

}

/**
 * Create the form toolbar.
 * 
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.adaptToolBar = function(model) {
  var isPersistent = (model.uid != null);
  this.getChild('toolbar').getChild('markAsDone').setVisible(isPersistent);
  this.getChild('toolbar').getChild('copy').setVisible(isPersistent);
  this.getChild('toolbar').getChild('move').setVisible(isPersistent);
  this.getChild('toolbar').getChild('delete').setVisible(isPersistent);
  this.getChild('toolbar').getChild('history').setVisible(isPersistent);
  if (isPersistent) {
    this.getChild('toolbar').getChild('markAsDone').setVisible(model.status != 'Completed');
  }
};

/** @override */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(this.getChild('toolbar'), goog.ui.Component.EventType.ACTION, this.dispatchAction_);
};

net.bluemind.task.vtodo.edit.VTodoEditView.prototype.showHistory = function(entries) {
  this.getChild('history-dialog').show(entries);
}


/** @override */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.getModel = function() {
  return this.getChild('form').getModel();
};

/** @override */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.setModel = function(model) {
  this.getChild('form').setModel(model);
  this.showSynchronizationNotice_(model);
  this.adaptToolBar(model);
};

/**
 * Set the folder list for toolbar menu
 * 
 * @param {Array} todolists Set of todolist.
 */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.setTodoLists = function(todolists) {
  var copy = this.getChild('toolbar').getChild('copy');
  var move = this.getChild('toolbar').getChild('move');
  copy.getMenu().removeChildren(true);
  move.getMenu().removeChildren(true);
  goog.iter.forEach(todolists, function(todolist) {
    this.addTodoListToAction_(todolist, copy);
    this.addTodoListToAction_(todolist, move);
  }, this);

  copy.setVisible(copy.getMenu().hasChildren())
  move.setVisible(move.getMenu().hasChildren())
};

/**
 * Set tags
 * 
 * @param {Array} tags
 */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.setTags = function(tags) {
  this.getChild('form').setTags(tags);
};
/**
 * @param {object} todolist
 * @param {string} action
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.addTodoListToAction_ = function(todolist, menu) {
  if (todolist.states.writable && menu.getMenu().getChild(todolist.uid) == null
      && todolist.uid != this.getModel().container) {
    var item = new goog.ui.MenuItem(todolist.name);
    item.setId(todolist.uid);
    item.setModel(todolist);
    menu.getMenu().addChild(item, true);
  }
}

/**
 * Dispatch an event to the controller
 * 
 * @param {goog.events.Event} evt Action event
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.dispatchAction_ = function(evt) {
  if (evt.target instanceof goog.ui.MenuItem) {
    evt.type = evt.target.getParent().getParent().getId();
  } else {
    evt.type = evt.target.getId();
  }
  this.dispatchEvent(evt);
};

/**
 * Show synchronization state notice
 * 
 * @private
 */
net.bluemind.task.vtodo.edit.VTodoEditView.prototype.showSynchronizationNotice_ = function(model) {
  var notice = this.getChild('notice');
  if (model.states.synced) {
    notice.setVisible(false);
    return;
  }
  if (model.states.error && model.error.message) {
    /** @meaning general.error.synchronization */
    var MSG_SYNC_ERROR = goog.getMsg("Synchronization failed : '{$message}'", {
      'message' : model.error.message
    });
    notice.setContent(MSG_SYNC_ERROR);
    notice.enableClassName(goog.getCssName('notice'), false);
    notice.enableClassName(goog.getCssName('error'), true);
  } else if (model.states.error) {
    /** @meaning general.error.synchronization.unkown */
    var MSG_UNKNOWN_SYNC_ERROR = goog.getMsg("Synchronization failed, a new attempt will be made later. Please contact support if this error persists.");
    notice.setContent(MSG_SYNC_ERROR);
    notice.enableClassName(goog.getCssName('notice'), false);
    notice.enableClassName(goog.getCssName('error'), true);
  } else {
    /** @meaning general.notice.notSynchronized */
    var MSG_NOT_SYNCHRONIZED = goog.getMsg("Not all modifications are synchronized with the server yet.");
    notice.setContent(MSG_NOT_SYNCHRONIZED);
    notice.enableClassName(goog.getCssName('notice'), true);
    notice.enableClassName(goog.getCssName('error'), false);
  }
  notice.setVisible(true);
};
