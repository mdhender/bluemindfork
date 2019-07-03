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
 * @fileoverview Default renderer for {@link bluemind.ui.CartoucheBox}s.
 */
goog.provide('bluemind.ui.style.CartoucheBoxRenderer');

goog.require('goog.dom');
goog.require('goog.a11y.aria');
goog.require('goog.a11y.aria.Role');
goog.require('goog.a11y.aria.State');
goog.require('goog.ui.ContainerRenderer');
goog.require('goog.ui.Separator');
goog.require('goog.ui.registry');


/**
 * Default renderer for {@link bluemind.ui.CartoucheBox}s.  Extends the superclass with
 * the following input-specific API methods:
 * <ul>
 *   <li>{@code getValue} - returns the input element's value
 *   <li>{@code setValue} - updates the input element to reflect its new value
 *   <li>{@code getTooltip} - returns the input element's tooltip text
 *   <li>{@code setTooltip} - updates the input element's tooltip text
 * </ul>
 * @constructor
 * @extends {goog.ui.ContainerRenderer}
 */
bluemind.ui.style.CartoucheBoxRenderer = function() {
  goog.base(this);
};

goog.inherits(bluemind.ui.style.CartoucheBoxRenderer, goog.ui.ContainerRenderer);
goog.addSingletonGetter(bluemind.ui.style.CartoucheBoxRenderer);


/** @override */
bluemind.ui.style.CartoucheBoxRenderer.CSS_CLASS = goog.getCssName('cartouchebox');

/** @override */
bluemind.ui.style.CartoucheBoxRenderer.prototype.getAriaRole = function() {
  return goog.a11y.aria.Role.LIST;
};


/** @override */
bluemind.ui.style.CartoucheBoxRenderer.prototype.canDecorate = function(element) {
  return element.tagName == 'UL';
};

/**
 * Returns whether the given element is contained in the list's DOM.
 * @param {bluemind.ui.CartoucheBox} list The list to test.
 * @param {Element} element The element to test.
 * @return {boolean} Whether the given element is contained in the list.
 */
bluemind.ui.style.CartoucheBoxRenderer.prototype.containsElement = function(list, element) {
  return goog.dom.contains(list.getElement(), element);
};


/** @override */
bluemind.ui.style.CartoucheBoxRenderer.prototype.createDom = function(container) {
  return container.getDomHelper().createDom('ul',
      this.getClassNames(container).join(' '));
};

/** @override */
bluemind.ui.style.CartoucheBoxRenderer.prototype.decorate = function(container, element) {
  var el = goog.base(this, 'decorate', container, element);
  goog.dom.classlist.add.apply(null,
        goog.array.concat(el, this.getClassNames(container)));
  return el;
};

/** @override */
bluemind.ui.style.CartoucheBoxRenderer.prototype.getCssClass = function() {
  return bluemind.ui.style.CartoucheBoxRenderer.CSS_CLASS;
};


/**
 * Register a decorator factory function for
 * bluemind.ui.style.CartoucheBoxRenderer.
 */
goog.ui.registry.setDecoratorByClassName(
  bluemind.ui.style.CartoucheBoxRenderer.CSS_CLASS, function() {
  return new bluemind.ui.CartoucheBox(bluemind.ui.style.CartoucheBoxRenderer.getInstance());
});
