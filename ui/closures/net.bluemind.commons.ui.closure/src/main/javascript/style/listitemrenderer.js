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
 * @fileoverview Renderer for {@link net.bluemind.ui.ListItem}s.
 */

goog.provide("net.bluemind.ui.style.ListItemRenderer");

goog.require("goog.dom");
goog.require("goog.a11y.aria.Role");
goog.require("goog.ui.ControlRenderer");
goog.require("goog.ui.registry");

/**
 * Default renderer for {@link net.bluemind.ui.ListItem}s. Each item has the
 * following structure:
 * 
 * <pre>
 *   &lt;div class=&quot;listitem&quot;&gt;
 *     &lt;div class=&quot;listitem-content&quot;&gt;
 *       ...(list item contents)...
 *     &lt;/div&gt;
 *   &lt;/div&gt;
 * </pre>
 * 
 * @constructor
 * @extends {goog.ui.ControlRenderer}
 */
net.bluemind.ui.style.ListItemRenderer = function() {
  goog.base(this);
};
goog.inherits(net.bluemind.ui.style.ListItemRenderer, goog.ui.ControlRenderer);
goog.addSingletonGetter(net.bluemind.ui.style.ListItemRenderer);

/**
 * CSS class name the renderer applies to list item elements.
 * 
 * @type {string}
 */
net.bluemind.ui.style.ListItemRenderer.CSS_CLASS = goog.getCssName('bm-listitem');

/** @return {goog.a11y.aria.Role} The ARIA role. */
net.bluemind.ui.style.ListItemRenderer.prototype.getAriaRole = function() {
  return goog.a11y.aria.Role.LISTITEM;
};

/** @override */
net.bluemind.ui.style.ListItemRenderer.prototype.createDom = function(item) {

  var element = item.getDomHelper().createDom('div', this.getClassNames(item).join(' '),
      this.createContent(item.getDomHelper()));
  var label = item.getDomHelper().createDom('div', '', this.createContent(item.getDomHelper()));
  element.appendChild(label);
  if (item.model_ && item.model_.title) {
    var title = item.getDomHelper().createDom('div', 'scroll-title', item.model_.title);
    label.appendChild(title);
  }
  this.setContent(element, item.getContent());
  return element;
};

net.bluemind.ui.style.ListItemRenderer.prototype.getStructuralCssClass = function() {
  return goog.getCssName(net.bluemind.ui.style.ListItemRenderer.CSS_CLASS, 'base');
};

/** @override */
net.bluemind.ui.style.ListItemRenderer.prototype.getContentElement = function(element) {
  return (/** @type {Element} */
  (element && goog.dom.getElementByClass(goog.getCssName(this.getStructuralCssClass(), 'content'), element)));
};

/** @override */
net.bluemind.ui.style.ListItemRenderer.prototype.decorate = function(item, element) {
  if (!this.hasContentStructure(element)) {
    element.appendChild(this.createContent(item.getDomHelper()));
    this.setContent(element, element.childNodes);
  }
  return goog.base(this, 'decorate', item, element);
};

/**
 * Returns true if the element appears to have a proper list item structure by
 * checking whether its first child has the appropriate structural class name.
 * 
 * @param {Element} element Element to check.
 * @return {boolean} Whether the element appears to have a proper list item DOM.
 * @protected
 */
net.bluemind.ui.style.ListItemRenderer.prototype.hasContentStructure = function(element) {
  var child = goog.dom.getFirstElementChild(element);
  var contentClassName = goog.getCssName(this.getStructuralCssClass(), 'content');
  return !!child && child.className.indexOf(contentClassName) != -1;
};

/**
 * Wraps the given text caption or existing DOM node(s) in a structural element
 * containing the list item's contents.
 * 
 * @param {goog.dom.DomHelper} dom DOM helper for document interaction.
 * @return {Element} List item content element.
 * @protected
 */
net.bluemind.ui.style.ListItemRenderer.prototype.createContent = function(dom) {
  var contentClassName = goog.getCssName(this.getStructuralCssClass(), 'content');
  var contentElem = dom.createDom('div', contentClassName);

  return contentElem;
};

/** @override */
net.bluemind.ui.style.ListItemRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.style.ListItemRenderer.CSS_CLASS;
};

/**
 * Takes a button's root element, and returns its tooltip text.
 * 
 * @param {Element} element The button's root element.
 * @return {string|undefined} The tooltip text.
 */
net.bluemind.ui.style.ListItemRenderer.prototype.getTooltip = function(element) {
  return element.title;
};

/**
 * Takes a button's root element and a tooltip string, and updates the element
 * with the new tooltip.
 * 
 * @param {Element} element The button's root element.
 * @param {string} tooltip New tooltip text.
 * @protected
 */
net.bluemind.ui.style.ListItemRenderer.prototype.setTooltip = function(element, tooltip) {
  // Don't set a title attribute if there isn't a tooltip. Blank title
  // attributes can be interpreted incorrectly by screen readers.
  if (element && tooltip) {
    element.title = tooltip;
  }
};

/**
 * Register a decorator factory function for
 * net.bluemind.ui.style.ListItemRenderer.
 */
goog.ui.registry.setDecoratorByClassName(net.bluemind.ui.style.ListItemRenderer.CSS_CLASS, function() {
  return new net.bluemind.ui.ListItem(null, null, net.bluemind.ui.style.ListItemRenderer.getInstance());
});
