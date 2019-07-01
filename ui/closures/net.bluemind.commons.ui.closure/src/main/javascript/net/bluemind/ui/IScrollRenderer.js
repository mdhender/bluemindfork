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
 * @fileoverview Renderer for infinite scrolling list.
 */

goog.provide("net.bluemind.ui.IScrollRenderer");

goog.require("goog.a11y.aria.Role");
goog.require("goog.ui.ContainerRenderer");

/**
 * Infinite scrolling default renderer
 * 
 * @extends {goog.ui.ContainerRenderer}
 * @constructor
 */
net.bluemind.ui.IScrollRenderer = function() {
  goog.base(this, goog.a11y.aria.Role.LIST);
};
goog.inherits(net.bluemind.ui.IScrollRenderer, goog.ui.ContainerRenderer);

goog.addSingletonGetter(net.bluemind.ui.IScrollRenderer);

/**
 * Default CSS class to be applied to the root element of containers rendered by
 * this renderer.
 * 
 * @type {string}
 */
net.bluemind.ui.IScrollRenderer.CSS_CLASS = goog.getCssName('iscroll');

/** @override */
net.bluemind.ui.IScrollRenderer.prototype.createDom = function(container) {
  var dom = container.getDomHelper();
  var el = dom.createDom('div', this.getClassNames(container).join(' '), dom.createDom('div', goog.getCssName(this
      .getCssClass(), 'view'), dom.createDom('div', goog.getCssName(this.getCssClass(), 'content')), dom.createDom(
      'div', {
        'class' : goog.getCssName(this.getCssClass(), 'spacer')
      })));
  // this.setScrollSize(container);
  return el;
};

/** @override */
net.bluemind.ui.IScrollRenderer.prototype.getContentElement = function(element) {
  return element && element.firstChild && /** @type {Element} */
  (element.firstChild.firstChild);
};

/**
 * Set scroll widget size. This is not mandatory, if no size is set, the widget
 * will only be scrolled via mouse wheel and goto links.
 * 
 * @param {net.bluemind.ui.IScroll} container Iscroll widget
 * @param {number} size Number of element.
 */
net.bluemind.ui.IScrollRenderer.prototype.setScrollSize = function(container, size) {
  if (container.isInDocument()) {
    var spacer = this.getSpacerElement(container.getElement());
    spacer.style.height = (size * container.getChildSize()) + 'px';
  }
};

/**
 * Returns the spacer DOM element
 * 
 * @param {Element} element Root element of the container whose content element
 *          is to be returned.
 * @return {Element} Spacer element.
 */
net.bluemind.ui.IScrollRenderer.prototype.getSpacerElement = function(element) {
  return element && element.firstChild && /** @type {Element} */
  (element.firstChild.lastChild);
};

/**
 * Returns the scroll DOM element
 * 
 * @param {Element} element Root element of the container whose content element
 *          is to be returned.
 * @return {Element} Scroll element.
 */
net.bluemind.ui.IScrollRenderer.prototype.getScrollElement = function(element) {
  return element && /** @type {Element} */
  (element.firstChild);
};

/** @override */
net.bluemind.ui.IScrollRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.IScrollRenderer.CSS_CLASS;
};
