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

/** @fileoverview Renderer for primary button style. */

goog.provide('bluemind.ui.style.PrimaryActionButtonRenderer');

goog.require('goog.ui.registry');
goog.require('goog.ui.style.app.ButtonRenderer');

/**
 * @constructor
 * @extends {goog.ui.style.app.ButtonRenderer}
 */
bluemind.ui.style.PrimaryActionButtonRenderer = function() {
  goog.base(this);
};
goog.inherits(bluemind.ui.style.PrimaryActionButtonRenderer,
    goog.ui.style.app.ButtonRenderer);
goog.addSingletonGetter(bluemind.ui.style.PrimaryActionButtonRenderer);

/** @override */
bluemind.ui.style.PrimaryActionButtonRenderer.CSS_CLASS =
    goog.getCssName('goog-primaryactionbutton');

/** @override */
bluemind.ui.style.PrimaryActionButtonRenderer.prototype.getCssClass =
    function() {
  return bluemind.ui.style.PrimaryActionButtonRenderer.CSS_CLASS;
};

/**
 * Register a decorator factory function for
 * bluemind.ui.style.PrimaryActionButtonRenderer.
 */
goog.ui.registry.setDecoratorByClassName(
bluemind.ui.style.PrimaryActionButtonRenderer.CSS_CLASS,
  function() {
  return new goog.ui.Button(null,
    bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  }
);
