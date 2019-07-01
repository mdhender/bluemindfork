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
goog.provide("net.bluemind.task.todolists.TodoListsView");

goog.require("goog.array");
goog.require("goog.dom.ViewportSizeMonitor");
goog.require("goog.events.EventType");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.List");
goog.require("net.bluemind.ui.ListItem");
goog.require("net.bluemind.ui.SubList");
goog.require("net.bluemind.ui.style.ListRenderer");

/**
 * @constructor
 * 
 * @param {net.bluemind.ui.style.ListRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.List}
 */
net.bluemind.task.todolists.TodoListsView = function() {
  var renderer = new net.bluemind.ui.style.ListRenderer();
  renderer.getClassNames = function(container) {
    var classes = net.bluemind.ui.style.ListRenderer.prototype.getClassNames.call(this, container);
    classes.push(goog.getCssName('bm-todolists'));
    return classes;
  };
  goog.base(this, renderer);
};
goog.inherits(net.bluemind.task.todolists.TodoListsView, net.bluemind.ui.List);

/** @override */
net.bluemind.task.todolists.TodoListsView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  var children = this.removeChildren(true);
  for (var i = 0; i < children.length; i++) {
    children[i].dispose();
  }
  if (model) {
    goog.array.forEach(model, function(section) {
      var s = new net.bluemind.ui.SubList(section.label);
      this.addChild(s, true);
      goog.array.forEach(section.entries, function(list) {
        var t = new net.bluemind.ui.ListItem(list.label);
        t.setId(list.uid);
        s.addChild(t, true);
        t.setTooltip(list.title);
      }, this);
    }, this);
  }
};

/** @override */
net.bluemind.task.todolists.TodoListsView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(this, goog.ui.Component.EventType.ACTION, this.onAction_)
  var monitor = goog.dom.ViewportSizeMonitor.getInstanceForWindow(this.getDomHelper().getWindow());
  this.getHandler().listen(monitor, goog.events.EventType.RESIZE, this.resize_);
  this.resize_();
};

/**
 * Resize list
 * 
 * @private
 */
net.bluemind.task.todolists.TodoListsView.prototype.resize_ = function() {
  var size = this.getDomHelper().getViewportSize();
  var height = size.height;
  var elSize = goog.style.getClientPosition(this.getElement());
  var top = this.getElement().offsetTop;
  if (height - elSize.y > 20) {
    this.getElement().style.height = (height - elSize.y - 40) + 'px';
  } else {
    this.getElement().style.height = '10px';
  }
};

/**
 * Called when a list item is selected
 * 
 * @param {goog.events.Event} e The event object.
 * @private
 */
net.bluemind.task.todolists.TodoListsView.prototype.onAction_ = function(e) {
  var control = e.target;
  if (control.isSupportedState(goog.ui.Component.State.SELECTED)) {
    var loc = this.getDomHelper().getWindow().location;
    loc.hash = "/?container=" + e.target.getId();
  }
};
