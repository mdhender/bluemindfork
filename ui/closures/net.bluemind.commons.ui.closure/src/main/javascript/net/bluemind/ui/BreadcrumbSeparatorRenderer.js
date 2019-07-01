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
goog.provide("net.bluemind.ui.BreadcrumbSeparatorRenderer");

goog.require("goog.dom");
goog.require("goog.dom.classlist");
goog.require("goog.ui.MenuSeparatorRenderer");

/**
 * @constructor
 * 
 * @extends {goog.ui.MenuSeparatorRenderer}
 */
net.bluemind.ui.BreadcrumbSeparatorRenderer = function() {
  goog.ui.MenuSeparatorRenderer.call(this);
  // TODO Auto-generated constructor stub
}
goog.inherits(net.bluemind.ui.BreadcrumbSeparatorRenderer, goog.ui.MenuSeparatorRenderer);

goog.addSingletonGetter(net.bluemind.ui.BreadcrumbSeparatorRenderer);

/**
 * Default CSS class to be applied to the root element of components rendered by
 * this renderer.
 * 
 * @type {string}
 */
net.bluemind.ui.BreadcrumbSeparatorRenderer.CSS_CLASS = goog.getCssName('breadcrumb-separator');

/**
 * Returns an empty, styled breadcrumb separator DIV. Overrides {@link
 * goog.ui.ControlRenderer#createDom}.
 * 
 * @param {goog.ui.Control} separator goog.ui.Separator to render.
 * @return {!Element} Root element for the separator.
 * @override
 */
net.bluemind.ui.BreadcrumbSeparatorRenderer.prototype.createDom = function(separator) {
  var css = this.getClassNames(separator);
  css.push(goog.ui.INLINE_BLOCK_CLASSNAME);
  css.push(goog.getCssName('fa'));
  css.push(goog.getCssName('fa-chevron-right'));
  return separator.getDomHelper().createDom('div', css);
};

/**
 * Takes an existing element, and decorates it with the separator. Overrides
 * {@link goog.ui.ControlRenderer#decorate}.
 * 
 * @param {goog.ui.Control} separator goog.ui.MenuSeparator to decorate the
 *                element.
 * @param {Element} element Element to decorate.
 * @return {Element} Decorated element.
 * @override
 */
net.bluemind.ui.BreadcrumbSeparatorRenderer.prototype.decorate = function(separator, element) {
  // Normally handled in the superclass. But we don't call the superclass.
  if (element.id) {
    separator.setId(element.id);
  }

  if (element.tagName == 'HR') {
    // Replace HR with separator.
    var hr = element;
    element = this.createDom(separator);
    goog.dom.insertSiblingBefore(element, hr);
    goog.dom.removeNode(hr);
  } else {
    goog.dom.classlist.add(element, this.getCssClass());
  }
  return element;
};

/**
 * Overrides {@link goog.ui.ControlRenderer#setContent} to do nothing, since
 * separators are empty.
 * 
 * @param {Element} separator The separator's root element.
 * @param {goog.ui.ControlContent} content Text caption or DOM structure to be
 *                set as the separators's content (ignored).
 * @override
 */
net.bluemind.ui.BreadcrumbSeparatorRenderer.prototype.setContent = function(separator, content) {
};

/**
 * Returns the CSS class to be applied to the root element of components
 * rendered using this renderer.
 * 
 * @return {string} Renderer-specific CSS class.
 * @override
 */
net.bluemind.ui.BreadcrumbSeparatorRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.BreadcrumbSeparatorRenderer.CSS_CLASS;
};