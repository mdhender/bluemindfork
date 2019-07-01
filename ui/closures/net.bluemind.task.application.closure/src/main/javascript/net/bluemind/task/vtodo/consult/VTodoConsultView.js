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
goog.provide("net.bluemind.task.vtodo.consult.VTodoConsultView");

goog.require("goog.iter");
goog.require("goog.ui.Component");
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.ToolbarMenuButton");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.MenuButtonRenderer");
goog.require("net.bluemind.todolist.ui.vtodo.VTodoCard");

/**
 * @constructor
 * 
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.task.vtodo.consult.VTodoConsultView = function(ctx) {
  goog.ui.Component.call(this);
  this.addToolbar_();
  var child = new net.bluemind.todolist.ui.vtodo.VTodoCard();
  child.setId('card');
  this.addChild(child, true);
  this.ctx_ = ctx;
}
goog.inherits(net.bluemind.task.vtodo.consult.VTodoConsultView, goog.ui.Component);

/**
 * Create the form toolbar.
 * 
 * @private
 */
net.bluemind.task.vtodo.consult.VTodoConsultView.prototype.addToolbar_ = function() {
  var button, menu, toolbar = new goog.ui.Toolbar();
  toolbar.setId('toolbar');
  this.addChild(toolbar, true);

  /** @meaning general.copy */
  var MSG_COPY_TO = goog.getMsg('Copy to...')
  menu = new goog.ui.Menu();
  button = new goog.ui.ToolbarMenuButton(MSG_COPY_TO, menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  button.setId('copy');
  toolbar.addChild(button, true);

};

/** @override */
net.bluemind.task.vtodo.consult.VTodoConsultView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('toolbar'), goog.ui.Component.EventType.ACTION, this.dispatchAction_);
};

/** @override */
net.bluemind.task.vtodo.consult.VTodoConsultView.prototype.getModel = function() {
  return this.getChild('card').getModel();
};

/** @override */
net.bluemind.task.vtodo.consult.VTodoConsultView.prototype.setModel = function(model) {
  var that = this;
  this.ctx_.service('todolists').get(model.container).then(function (container){
    model['containerName'] = container.name;
    that.getChild('card').setModel(model);
  });
};

/**
 * Set the folder list for toolbar menu
 * 
 * @param {Array} todolists Set of todolist.
 */
net.bluemind.task.vtodo.consult.VTodoConsultView.prototype.setTodoLists = function(todolists) {
  var copy = this.getChild('toolbar').getChild('copy');
  copy.getMenu().removeChildren();
  goog.iter.forEach(todolists, function(todolist) {
    this.addTodoLitToAction_(todolist, copy);
  }, this);
  copy.setVisible(copy.getMenu().hasChildren())
};

/**
 * @param {object} todolist
 * @param {string} action
 * @private
 */
net.bluemind.task.vtodo.consult.VTodoConsultView.prototype.addTodoLitToAction_ = function(todolist, menu) {
  if (todolist.states.writable && menu.getChild(todolist.uid) == null && todolist.uid != this.getModel().container) {
    var item = new goog.ui.MenuItem(todolist.name);
    item.setId(todolist.uid);
    item.setModel(todolist);
    menu.addChild(item, true);
  }
}

/**
 * Dispatch an event to the controller
 * 
 * @param {goog.events.Event} evt Action event
 * @private
 */
net.bluemind.task.vtodo.consult.VTodoConsultView.prototype.dispatchAction_ = function(evt) {
  if (evt.target instanceof goog.ui.MenuItem) {
    evt.type = evt.target.getParent().getParent().getId();
  } else {
    evt.type = evt.target.getId();
  }
  this.dispatchEvent(evt);
};
