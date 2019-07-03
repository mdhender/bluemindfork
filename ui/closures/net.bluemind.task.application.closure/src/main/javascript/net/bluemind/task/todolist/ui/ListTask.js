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
 * @fileoverview A class for representing entrys in lists.
 * @see bluemind.ui.ListItem
 * 
 */

goog.provide("net.bluemind.task.todolist.ui.ListTask");

goog.require("goog.ui.Checkbox");
goog.require("goog.soy");
goog.require("goog.ui.Button");
goog.require("goog.ui.Control");
goog.require("goog.ui.FlatButtonRenderer");
goog.require("net.bluemind.task.todolist.ui.ListTaskRenderer");
goog.require("net.bluemind.ui.ListItem");
goog.require("net.bluemind.task.EventType");
goog.require("net.bluemind.task.todolist.templates");// FIXME - unresolved
// required symbol

/**
 * Class representing a task in a list.
 * 
 * @param {Object} task Model object
 * @param {net.bluemind.task.todolist.ui.ListTaskRenderer=} opt_renderer
 *                Optional renderer.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper used for
 *                document interactions.
 * @constructor
 * @extends {net.bluemind.ui.ListItem}
 */
net.bluemind.task.todolist.ui.ListTask = function(task, opt_renderer, opt_domHelper) {
  var renderer = opt_renderer || net.bluemind.task.todolist.ui.ListTaskRenderer.getInstance();
  var model = task || {};
  goog.base(this, null, model, renderer, opt_domHelper);
  var content = goog.soy.renderAsElement(net.bluemind.task.todolist.templates.label, {
    task : this.getModel()
  }, null, this.getDomHelper());
  var label = new goog.ui.Control(content, null, this.getDomHelper());
  label.addClassName(goog.ui.INLINE_BLOCK_CLASSNAME);
  label.setId('label');
  this.addChild(label, true);
  if (model.writable) {
    var button = new goog.ui.Checkbox();
    button.setId('markAsDone');
    button.addClassName('markAsDone');
    this.addChild(button, true);
    if (model.status == "Completed") {
      button.setChecked(true);
    }
  }
  if (!model.synced) {
    this.addClassName(goog.getCssName('not-synchronized'))
  }
  this.setId(model.uid);
};
goog.inherits(net.bluemind.task.todolist.ui.ListTask, net.bluemind.ui.ListItem);

/** Override */
net.bluemind.task.todolist.ui.ListTask.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (null != this.getChild('markAsDone')) {
    this.getHandler().listen(this.getChild('markAsDone'), goog.ui.Component.EventType.ACTION, function(e) {
      this.dispatchEvent(net.bluemind.task.EventType.MARK_AS_DONE);
    });
  }
};

/** Override */
net.bluemind.task.todolist.ui.ListTask.prototype.handleMouseDown = function(e) {
  if (null == this.getChild('markAsDone') || !this.getChild('markAsDone').isActive()) {
    goog.base(this, 'handleMouseDown', e);
  }
};
