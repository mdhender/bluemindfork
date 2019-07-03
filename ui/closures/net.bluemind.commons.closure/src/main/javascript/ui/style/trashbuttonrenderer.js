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

/** @fileoverview Renderer for trash button style. */

goog.provide('bluemind.ui.style.TrashButtonRenderer');

goog.require('goog.ui.Button');
goog.require('goog.ui.FlatButtonRenderer');
goog.require('goog.ui.registry');

/**
 * Link renderer for {@link goog.ui.Button}s. Link buttons can contain almost
 * arbitrary HTML content, will flow like inline elements, but can be styled
 * like block-level elements.
 * 
 * @constructor
 * @extends {goog.ui.FlatButtonRenderer}
 */
bluemind.ui.style.TrashButtonRenderer = function() {
  goog.ui.FlatButtonRenderer.call(this);
};
goog.inherits(bluemind.ui.style.TrashButtonRenderer, goog.ui.FlatButtonRenderer);
goog.addSingletonGetter(bluemind.ui.style.TrashButtonRenderer);

/**
 * Default CSS class to be applied to the root element of components rendered by
 * this renderer.
 * 
 * @type {string}
 */
bluemind.ui.style.TrashButtonRenderer.CSS_CLASS = goog.getCssName('goog-trash-button');

/** @override */
bluemind.ui.style.TrashButtonRenderer.prototype.getCssClass = function() {
  return bluemind.ui.style.TrashButtonRenderer.CSS_CLASS;
};

/** @override */
bluemind.ui.style.TrashButtonRenderer.prototype.createDom = function(button) {
  var el = goog.base(this, 'createDom', button);
  var baseClass = this.getStructuralCssClass();
  var dom = button.getDomHelper();
  var lid = [ goog.getCssName(baseClass, 'lid'), goog.getCssName('fa'), goog.getCssName('fa-lg'),
      goog.getCssName('fa-trash-o') ];
  var can = [ goog.getCssName(baseClass, 'can'), goog.getCssName('fa'), goog.getCssName('fa-lg'),
      goog.getCssName('fa-trash-o') ];
  dom.append(el, '\u00A0');
  dom.append(el, dom.createDom('div', lid), dom.createDom('div', can));

  return el;
};

// Register a decorator factory function for Link Buttons.
goog.ui.registry.setDecoratorByClassName(bluemind.ui.style.TrashButtonRenderer.CSS_CLASS, function() {
  // Uses goog.ui.Button, but with LinkButtonRenderer.
  return new goog.ui.Button(null, bluemind.ui.style.TrashButtonRenderer.getInstance());
});
