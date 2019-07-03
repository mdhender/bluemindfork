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
 * @fileoverview Renderer for {@link net.bluemind.ui.List}s.
 * 
 */

goog.provide("net.bluemind.ui.style.ListRenderer");

goog.require("goog.dom");
goog.require("goog.a11y.aria");
goog.require("goog.a11y.aria.Role");
goog.require("goog.a11y.aria.State");
goog.require("goog.ui.ContainerRenderer");
goog.require("goog.ui.Separator");
goog.require("goog.ui.registry");

/**
 * Default renderer for {@link net.bluemind.ui.List}s, based on {@link
 * goog.ui.ContainerRenderer}.
 * 
 * @constructor
 * @extends {goog.ui.ContainerRenderer}
 */
net.bluemind.ui.style.ListRenderer = function() {
  goog.base(this);
};

goog.inherits(net.bluemind.ui.style.ListRenderer, goog.ui.ContainerRenderer);
goog.addSingletonGetter(net.bluemind.ui.style.ListRenderer);

/** @override */
net.bluemind.ui.style.ListRenderer.CSS_CLASS = goog.getCssName('bm-list');

/** @override */
net.bluemind.ui.style.ListRenderer.prototype.getAriaRole = function() {
  return goog.a11y.aria.Role.LIST;
};

/** @override */
net.bluemind.ui.style.ListRenderer.prototype.canDecorate = function(element) {
  return element.tagName == 'UL' || goog.base(this, 'canDecorate', element);
};

/** @override */
net.bluemind.ui.style.ListRenderer.prototype.getDecoratorForChild = function(
    element) {
  return element.tagName == 'HR' ? new goog.ui.Separator() : goog.base(this,
      'getDecoratorForChild', element);
};

/**
 * Returns whether the given element is contained in the list's DOM.
 * 
 * @param {net.bluemind.ui.List} list The list to test.
 * @param {Element} element The element to test.
 * @return {boolean} Whether the given element is contained in the list.
 */
net.bluemind.ui.style.ListRenderer.prototype.containsElement = function(list,
    element) {
  return goog.dom.contains(list.getElement(), element);
};

/** @override */
net.bluemind.ui.style.ListRenderer.prototype.initializeDom = function(container) {
  goog.base(this, 'initializeDom', container);
  var element = container.getElement();
  if (element) {
    goog.a11y.aria.setState(element, goog.a11y.aria.State.HASPOPUP, 'true');
  }
};

/** @override */
net.bluemind.ui.style.ListRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.style.ListRenderer.CSS_CLASS;
};

/**
 * Register a decorator factory function for net.bluemind.ui.style.ListRenderer.
 */
goog.ui.registry.setDecoratorByClassName(
    net.bluemind.ui.style.ListRenderer.CSS_CLASS, function() {
      return new net.bluemind.ui.List(net.bluemind.ui.style.ListRenderer
          .getInstance());
    });
