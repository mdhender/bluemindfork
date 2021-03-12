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
*/

/**
 * @fileoverview Renderer for {@link goog.ui.Button}s in bluemind style. This
 * type of button is typically used for an application's dangerous action, eg
 * the delete action.
 *
 */

goog.provide('bluemind.ui.style.ImportantActionButtonRenderer');

goog.require('goog.ui.Button');
goog.require('goog.ui.registry');
goog.require('goog.ui.style.app.ButtonRenderer');

/**
 * Custom renderer for {@link goog.ui.Button}s. This renderer supports the
 * "primary action" style for buttons.
 *
 * @constructor
 * @extends {goog.ui.style.app.ButtonRenderer}
 */
bluemind.ui.style.ImportantActionButtonRenderer = function() {
  goog.ui.style.app.ButtonRenderer.call(this);
};
goog.inherits(bluemind.ui.style.ImportantActionButtonRenderer,
    goog.ui.style.app.ButtonRenderer);
goog.addSingletonGetter(bluemind.ui.style.ImportantActionButtonRenderer);


/**
 * Default CSS class to be applied to the root element of components rendered
 * by this renderer.
 * @type {string}
 */
bluemind.ui.style.ImportantActionButtonRenderer.CSS_CLASS =
  goog.getCssName('importantactionbutton');


/**
 * Array of arrays of CSS classes that we want composite classes added and
 * removed for in IE6 and lower as a workaround for lack of multi-class CSS
 * selector support.
 * @type {Array.<Array.<string>>}
 */
bluemind.ui.style.ImportantActionButtonRenderer.IE6_CLASS_COMBINATIONS = [
  [goog.getCssName('goog-button-base-disabled'),
    goog.getCssName('importantactionbutton')],
  [goog.getCssName('goog-button-base-focused'),
    goog.getCssName('importantactionbutton')],
  [goog.getCssName('goog-button-base-hover'),
    goog.getCssName('importantactionbutton')]
];


/** @inheritDoc */
bluemind.ui.style.ImportantActionButtonRenderer.prototype.getCssClass =
  function() {
  return bluemind.ui.style.ImportantActionButtonRenderer.CSS_CLASS;
};


/** @inheritDoc */
bluemind.ui.style.ImportantActionButtonRenderer.prototype.
  getIe6ClassCombinations = function() {
  return bluemind.ui.style.ImportantActionButtonRenderer.
    IE6_CLASS_COMBINATIONS;
};

/**
 * Register a decorator factory function for
 * bluemind.ui.style.ImportantActionButtonRenderer.
 */
goog.ui.registry.setDecoratorByClassName(
bluemind.ui.style.ImportantActionButtonRenderer.CSS_CLASS,
  function() {
  return new goog.ui.Button(null,
    bluemind.ui.style.ImportantActionButtonRenderer.getInstance());
  }
);
