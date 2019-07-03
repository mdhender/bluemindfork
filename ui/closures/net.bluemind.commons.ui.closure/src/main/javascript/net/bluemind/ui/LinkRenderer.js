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
goog.provide("net.bluemind.ui.LinkRenderer");

goog.require("goog.a11y.aria.Role");
goog.require("goog.ui.ControlRenderer");

/**
 * @constructor
 * 
 * @extends {goog.ui.ControlRenderer}
 */
net.bluemind.ui.LinkRenderer = function() {
  goog.ui.ControlRenderer.call(this);
  // TODO Auto-generated constructor stub
}
goog.inherits(net.bluemind.ui.LinkRenderer, goog.ui.ControlRenderer);
goog.addSingletonGetter(net.bluemind.ui.LinkRenderer);

/**
 * Default CSS class to be applied to the root element of components rendered by
 * this renderer.
 * 
 * @type {string}
 */
net.bluemind.ui.LinkRenderer.CSS_CLASS = goog.getCssName('bluemind-link');

/**
 * Returns the ARIA role to be applied to links.
 * 
 * @return {goog.a11y.aria.Role|undefined} ARIA role.
 * @override
 */
net.bluemind.ui.LinkRenderer.prototype.getAriaRole = function() {
  return goog.a11y.aria.Role.LINK;
};

/** @override */
net.bluemind.ui.LinkRenderer.prototype.createDom = function(link) {
  var el = link.getDomHelper().createDom('a', this.getClassNames(link).join(' '), link.getContent());
  this.setAriaStates(link, el);
  this.setTooltip(el, link.getTooltip());

  var href = link.getHref();
  if (href) {
    this.setHref(el, href);
  }

  return el;
};

/** @override */
net.bluemind.ui.LinkRenderer.prototype.decorate = function(link, element) {
  element = goog.base(this, 'decorate', link, element);

  link.setHrefInternal(this.getHref(element));
  link.setTooltipInternal(this.getTooltip(element));

  return element;
};

/**
 * Takes a link's root element, and returns the href associated with it. No-op
 * in the base class.
 * 
 * @param {Element} element The link's root element.
 * @return {string|undefined} The link's href (undefined if none).
 */
net.bluemind.ui.LinkRenderer.prototype.getHref = function(element) {
  return element.href
};

/**
 * Takes a link's root element and a href, and updates the element to reflect
 * the new href. No-op in the base class.
 * 
 * @param {Element} element The link's root element.
 * @param {string} href New href.
 */
net.bluemind.ui.LinkRenderer.prototype.setHref = function(element, href) {
  element.href = href;
};

/**
 * Takes a link's root element, and returns its tooltip text.
 * 
 * @param {Element} element The link's root element.
 * @return {string|undefined} The tooltip text.
 */
net.bluemind.ui.LinkRenderer.prototype.getTooltip = function(element) {
  return element.title;
};

/**
 * Takes a link's root element and a tooltip string, and updates the element
 * with the new tooltip.
 * 
 * @param {Element} element The link's root element.
 * @param {string} tooltip New tooltip text.
 * @protected
 */
net.bluemind.ui.LinkRenderer.prototype.setTooltip = function(element, tooltip) {
  if (element) {
    // Don't set a title attribute if there isn't a tooltip. Blank title
    // attributes can be interpreted incorrectly by screen readers.
    if (tooltip) {
      element.title = tooltip;
    } else {
      element.removeAttribute('title');
    }
  }
};

/** @override */
net.bluemind.ui.LinkRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.LinkRenderer.CSS_CLASS;
};
