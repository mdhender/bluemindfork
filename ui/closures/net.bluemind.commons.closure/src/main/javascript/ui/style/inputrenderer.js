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

goog.provide('bluemind.ui.style.InputRenderer');

goog.require('goog.dom.classes');
goog.require('goog.ui.Component.State');
goog.require('goog.ui.ControlRenderer');
goog.require('goog.ui.LabelInput');



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
 * @extends {goog.ui.ControlRenderer}
 */
bluemind.ui.style.InputRenderer = function() {
  goog.base(this);
};
goog.inherits(bluemind.ui.style.InputRenderer, goog.ui.ControlRenderer);
goog.addSingletonGetter(bluemind.ui.style.InputRenderer);


/**
 * Default CSS class to be applied to the root element of components rendered
 * by this renderer.
 * @type {string}
 */
bluemind.ui.style.InputRenderer.CSS_CLASS = goog.getCssName('field-text');

/** @override */
bluemind.ui.style.InputRenderer.prototype.getStructuralCssClass = function() {
  return goog.getCssName('field-base');
};

/** @override */
bluemind.ui.style.InputRenderer.prototype.createDom = function(input) {
  var dom = input.getDomHelper();
  var classNames = this.getClassNames(input);
  var baseClass = this.getStructuralCssClass();
  var i = this.createDomField(input);
  var element = dom.createDom('label', classNames.join(' '), 
    dom.createDom(
      'span', goog.getCssName(baseClass, 'label'), input.getContent()
    ),
    dom.createDom('div', 
      goog.getCssName(baseClass, 'field'), i)
  );
  var tooltip = input.getTooltip();
  if (tooltip) {
    this.setTooltip(element, tooltip);
  }
  var value = input.getValue();
  if (value) {
    this.setValue(element, value);
  }
  var label = /** @type {string} */ (input.getContent());
  if (label) {
    this.setLabel(i, label);
  }


  
  return element;
};

/** @override */
bluemind.ui.style.InputRenderer.prototype.decorate = function(input, element) {
  element = goog.base(this, 'decorate', input, element);

  input.setValueInternal(this.getValue(element));
  input.setTooltipInternal(this.getTooltip(element));

  //FIXME
  this.decorateField(input, element.getElementsByTagName('input')[0]);
  return element;
};

/**
 * Create the field.
 * @param {goog.ui.Control} input Control whose DOM is to be initialized
 *   as it enters the document.
 * @param {Element} element The input's element.
 * @protected
 */
bluemind.ui.style.InputRenderer.prototype.decorateField = function(input, element) {
  var li = new goog.ui.LabelInput();
  input.addChild(li);
  li.setId('input');
  li.decorate(element);    
};

/**
 * Decorate the field.
 * @param {goog.ui.Control} input Control whose DOM is to be initialized
 *   as it enters the document.
 * @return {Element} element The input's element.
 * @protected
 */
bluemind.ui.style.InputRenderer.prototype.createDomField = function(input) {
  //FIXME
  var li = new goog.ui.LabelInput(/** @type {string} */ (input.getContent()));
  input.addChild(li);
  li.setId('input');
  li.createDom();
  return li.getElement();
};

/**
 * Takes a input's root element, and returns the value associated with it.
 * No-op in the base class.
 * @param {Element} element The input's root element.
 * @return {string|undefined} The input's value (undefined if none).
 */
bluemind.ui.style.InputRenderer.prototype.getValue = function(element) {
  var input = element.getElementsByTagName('input')[0];
  return input.value;
};


/** @override */
bluemind.ui.style.InputRenderer.prototype.getKeyEventTarget = function(control) {
  return control.getElement().getElementsByTagName('input')[0];
};

/**
 * Takes a input's root element and a value, and updates the element to reflect
 * the new value.  No-op in the base class.
 * @param {Element} element The input's root element.
 * @param {string} value New value.
 */
bluemind.ui.style.InputRenderer.prototype.setValue = function(element, value) {
  var input = element.getElementsByTagName('input')[0];
  input.value = value;
};

/**
 * Takes a input's root element, and returns its tooltip text.
 * @param {Element} element The input's root element.
 * @return {string|undefined} The tooltip text.
 */
bluemind.ui.style.InputRenderer.prototype.getTooltip = function(element) {
  return element.title;
};


/**
 * Takes a input's root element and a tooltip string, and updates the element
 * with the new tooltip.
 * @param {Element} element The input's root element.
 * @param {string} tooltip New tooltip text.
 * @protected
 */
bluemind.ui.style.InputRenderer.prototype.setTooltip = function(element, tooltip) {
  if (element) {
    element.title = tooltip || '';
  }
};

/**
 * Takes a input's root element, and returns its label text.
 * @param {Element} element The input's root element.
 * @return {string|undefined} The label text.
 */
bluemind.ui.style.InputRenderer.prototype.getLabel = function(element) {
  return element.getAttribute('label');
};


/**
 * Takes a input's root element and a tooltip string, and updates the element
 * with the new label.
 * @param {Element} element The input's root element.
 * @param {string} label New label text.
 * @protected
 */
bluemind.ui.style.InputRenderer.prototype.setLabel = function(element, label) {
  if (element) {
    element.setAttribute('label', (label || ''));
  }
};

/** @override */
bluemind.ui.style.InputRenderer.prototype.getCssClass = function() {
  return bluemind.ui.style.InputRenderer.CSS_CLASS;
};
