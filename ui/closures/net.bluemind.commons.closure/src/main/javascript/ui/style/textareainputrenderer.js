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
 * @fileoverview Default renderer for {@link bluemind.ui.Input}s.
 */

goog.provide('bluemind.ui.style.TextareaInputRenderer');

goog.require('bluemind.ui.style.InputRenderer');
goog.require('goog.ui.Textarea');


/**
 * Default renderer for {@link bluemind.ui.Input}s.  Extends the superclass with
 * the following input-specific API methods:
 * <ul>
 *   <li>{@code getValue} - returns the input element's value
 *   <li>{@code setValue} - updates the input element to reflect its new value
 *   <li>{@code getTooltip} - returns the input element's tooltip text
 *   <li>{@code setTooltip} - updates the input element's tooltip text
 * </ul>
 * @constructor
 * @extends {bluemind.ui.style.InputRenderer}
 */
bluemind.ui.style.TextareaInputRenderer= function() {
  goog.base(this);
};

goog.inherits(bluemind.ui.style.TextareaInputRenderer, bluemind.ui.style.InputRenderer);
goog.addSingletonGetter(bluemind.ui.style.TextareaInputRenderer);

/**
 * Default CSS class to be applied to the root element of components rendered
 * by this renderer.
 * @type {string}
 */
bluemind.ui.style.TextareaInputRenderer.CSS_CLASS = goog.getCssName('field-textarea');


/** @override */
bluemind.ui.style.TextareaInputRenderer.prototype.getCssClass = function() {
  return bluemind.ui.style.TextareaInputRenderer.CSS_CLASS;
};

/** */
bluemind.ui.style.TextareaInputRenderer.prototype.getValue = function(element) {
  var input = element.getElementsByTagName('textarea')[0];
  return input.value;
};


/** @override */
bluemind.ui.style.TextareaInputRenderer.prototype.getKeyEventTarget = function(control) {
  return control.getElement().getElementsByTagName('textarea')[0];
};

/** @override */
bluemind.ui.style.TextareaInputRenderer.prototype.setValue = function(element, value) {
  var input = element.getElementsByTagName('textarea')[0];
  input.value = value;
};


/** @override */
bluemind.ui.style.TextareaInputRenderer.prototype.decorateField = function(input, element) {
  var ta = new goog.ui.Textarea("");
  input.addChild(ta);
  ta.decorate(element);    
};

/** @override */
bluemind.ui.style.TextareaInputRenderer.prototype.createDomField = function(input) {
  //FIXME
  var ta = new goog.ui.Textarea("");
  input.addChild(ta);
  ta.createDom();
  return ta.getElement()
};
