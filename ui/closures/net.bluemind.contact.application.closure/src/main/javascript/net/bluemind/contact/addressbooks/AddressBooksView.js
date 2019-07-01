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
goog.provide("net.bluemind.contact.addressbooks.AddressBooksView");

goog.require("goog.array");
goog.require("goog.dom.ViewportSizeMonitor");
goog.require("goog.events.EventType");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.List");
goog.require("net.bluemind.ui.ListItem");
goog.require("net.bluemind.ui.SubList");
goog.require("net.bluemind.ui.style.ListRenderer");
// required symbol

/**
 * @constructor
 * 
 * @param {net.bluemind.ui.style.ListRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.List}
 */
net.bluemind.contact.addressbooks.AddressBooksView = function() {
  var renderer = new net.bluemind.ui.style.ListRenderer();
  renderer.getClassNames = function(container) {
    var baseClass = this.getCssClass();
    var classNames = [ baseClass, goog.getCssName(baseClass, 'horizontal'), goog.getCssName('addressbooks') ];
    if (!container.isEnabled()) {
      classNames.push(goog.getCssName(baseClass, 'disabled'));
    }
    return classNames;
  };
  goog.base(this, renderer);
};
goog.inherits(net.bluemind.contact.addressbooks.AddressBooksView, net.bluemind.ui.List);

/** @override */
net.bluemind.contact.addressbooks.AddressBooksView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  var children = this.removeChildren(true);
  for (var i = 0; i < children.length; i++) {
    children[i].dispose();
  }
  if (model) {
    var index = 0;
    goog.array.forEach(model, function(section) {
      var s = new net.bluemind.ui.SubList(section.label);
      s.setModel(section);
      this.addChild(s, true);
      goog.array.forEach(section.entries, function(list) {
        if (index == 0){
          if (list.title) {
            list.title = null;
          }
        }
        var t = new net.bluemind.ui.ListItem(list.label);
        t.setId(list.uid);
        t.setModel(list);
        s.addChild(t, true);
        t.setTooltip(list.title);
      }, this);
      index++;
    }, this);
  }
};

/** @override */
net.bluemind.contact.addressbooks.AddressBooksView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this, goog.ui.Component.EventType.ACTION, this.onAction_, true)
  var monitor = goog.dom.ViewportSizeMonitor.getInstanceForWindow(this.getDomHelper().getWindow());
  this.getHandler().listen(monitor, goog.events.EventType.RESIZE, this.resize_);
  this.resize_();
};

/**
 * Resize list
 * 
 * @private
 */
net.bluemind.contact.addressbooks.AddressBooksView.prototype.resize_ = function() {
  var size = this.getDomHelper().getViewportSize();
  var height = size.height;

  var top = this.getElement().offsetTop;
  if (height - top > 400) {
    this.getElement().style.height = (height - top - 95) + 'px';
  } else {
    this.getElement().style.height = '400px';
  }
};

/**
 * Called when a list item is selected
 * 
 * @param {goog.events.Event} e The event object.
 * @private
 */
net.bluemind.contact.addressbooks.AddressBooksView.prototype.onAction_ = function(e) {
  var control = e.target;
  if (!control.isSupportedState(goog.ui.Component.State.SELECTED)) {
    e.stopPropagation()
  }
};
