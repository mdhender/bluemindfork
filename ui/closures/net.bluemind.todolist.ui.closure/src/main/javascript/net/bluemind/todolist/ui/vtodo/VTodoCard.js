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
 * @fileoverview
 * 
 * Contact non-editable card.
 */

goog.provide("net.bluemind.todolist.ui.vtodo.VTodoCard");

goog.require("goog.dom.classlist");
goog.require("goog.ui.Component");
goog.require("net.bluemind.todolist.ui.vtodo.templates");

/**
 * Task card view.
 * 
 * @param {Object} model Task model to display.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.todolist.ui.vtodo.VTodoCard = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(net.bluemind.todolist.ui.vtodo.VTodoCard, goog.ui.Component);

/** @override */
net.bluemind.todolist.ui.vtodo.VTodoCard.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model)
  if (!this.getElement()) {
    this.createDom();
  }
  var card = this.getElementByClass(goog.getCssName('task-card'));
  if (!card) {
    card = this.getDomHelper().createElement('div');
  }
  
  card.innerHTML = net.bluemind.todolist.ui.vtodo.templates.card({
    task : model
  });
  goog.dom.classlist.add(card, goog.getCssName('task-card'));
  this.getDomHelper().appendChild(this.getElement(), card);
};
