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
 * @fileoverview Renderer for {@link net.bluemind.task.todolist.ui.ListTask}s.
 * 
 */

goog.provide("net.bluemind.task.todolist.ui.ListTaskRenderer");

goog.require("goog.ui.registry");
goog.require("net.bluemind.ui.style.ListItemRenderer");

/**
 * Draw the content of the list item to display task informations
 * 
 * @constructor
 * @extends {net.bluemind.ui.style.ListItemRenderer}
 */
net.bluemind.task.todolist.ui.ListTaskRenderer = function() {
  goog.base(this);
};
goog.inherits(net.bluemind.task.todolist.ui.ListTaskRenderer, net.bluemind.ui.style.ListItemRenderer);
goog.addSingletonGetter(net.bluemind.task.todolist.ui.ListTaskRenderer);

/**
 * CSS class name the renderer applies to list item elements.
 * 
 * @type {string}
 */
net.bluemind.task.todolist.ui.ListTaskRenderer.CSS_CLASS = goog.getCssName('task-listtask');

/** @override */
net.bluemind.task.todolist.ui.ListTaskRenderer.prototype.getCssClass = function() {
  return net.bluemind.task.todolist.ui.ListTaskRenderer.CSS_CLASS;
};

/**
 * Register a decorator factory function for
 * net.bluemind.task.todolist.ui.ListTaskRenderer;
 */
goog.ui.registry.setDecoratorByClassName(net.bluemind.task.todolist.ui.ListTaskRenderer.CSS_CLASS,
    function() {
      return new net.bluemind.task.todolist.ui.ListTask(null, net.bluemind.task.todolist.ui.ListTaskRenderer
          .getInstance());
    });
