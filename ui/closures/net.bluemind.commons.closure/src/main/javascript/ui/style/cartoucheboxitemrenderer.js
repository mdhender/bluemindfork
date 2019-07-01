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
 * @fileoverview Widget composed of a text and a close button that has the form
 * of a cartouche.
 **/

/**
 * @fileoverview Renderer for {@link bluemind.ui.CartoucheItem}s.
 *
 */

goog.provide('bluemind.ui.style.CartoucheBoxItemRenderer');

goog.require('goog.dom');
goog.require('goog.a11y.aria');
goog.require('goog.a11y.aria.Role');
goog.require('goog.dom.classlist');
goog.require('goog.ui.ControlContent');
goog.require('goog.ui.ControlRenderer');
goog.require('goog.ui.registry');



/**
 * Default renderer for {@link goog.ui.CartoucheItem}s.  Each item has the following
 * structure:
 * <pre>
 *   <ul class="listitem">
 *     <li class="listitem-content">
 *       ...(list item contents)...
 *     </li>
 *   </ul>
 * </pre>
 * @constructor
 * @extends {goog.ui.ControlRenderer}
 */
bluemind.ui.style.CartoucheBoxItemRenderer = function() {
  goog.base(this);
};
goog.inherits(bluemind.ui.style.CartoucheBoxItemRenderer, goog.ui.ControlRenderer);
goog.addSingletonGetter(bluemind.ui.style.CartoucheBoxItemRenderer);


/**
 * CSS class name the renderer applies to list item elements.
 * @type {string}
 */
bluemind.ui.style.CartoucheBoxItemRenderer.CSS_CLASS = goog.getCssName('cartoucheboxitem');

/**
 * Map of component states to state-specific structural class names,
 * used when changing the DOM in response to a state change.  Precomputed
 * and cached on first use to minimize object allocations and string
 * concatenation.
 * @type {Object.<string, number>}
 * @private
 */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.stateFromClass_;

/**
 * Map of state-specific structural class names to component states,
 * used during element decoration.  Precomputed and cached on first use
 * to minimize object allocations and string concatenation.
 * @type {Object.<number, string>}
 * @private
 */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.classFromStates_;


/** @return {goog.a11y.aria.Role} The ARIA role. */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.getAriaRole = function() {
  return goog.a11y.aria.Role.LISTITEM;
};


/** @override */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.createDom = function(item) {
  var element = item.getDomHelper().createDom(
      'li', this.getClassNames(item).join(' '),
      this.createContent(item.getDomHelper()));
  this.setContent(element, item.getContent());
  element.title = item.getCaption();
  return element;
};


/** @override */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.getContentElement = function(element) {
  return /** @type {Element} */(element && goog.dom.getElementByClass(goog.getCssName(this.getStructuralCssClass(), 'content'), element));
};


/** @override */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.decorate = function(item, element) {
  if (!this.hasContentStructure(element)) {
    element.appendChild(
        this.createContent(item.getDomHelper()));
    this.setContent(element, element.childNodes);
    element.title = item.getCaption();
  }
  return goog.base(this, 'decorate', item, element);
};


/**
 * Returns true if the element appears to have a proper list item structure by
 * checking whether its first child has the appropriate structural class name.
 * @param {Element} element Element to check.
 * @return {boolean} Whether the element appears to have a proper list item DOM.
 * @protected
 */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.hasContentStructure = function(element) {
  var child = goog.dom.getFirstElementChild(element);
  var contentClassName = goog.getCssName(this.getStructuralCssClass(), 'content');
  return !!child && child.className.indexOf(contentClassName) != -1;
};


/**
 * Wraps the given text caption or existing DOM node(s) in a structural element
 * containing the list item's contents.
 * @param {goog.dom.DomHelper} dom DOM helper for document interaction.
 * @return {Element} Cartouche item content element.
 * @protected
 */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.createContent = function(dom) {
  var contentClassName = goog.getCssName(this.getStructuralCssClass(), 'content');
  var contentElem = dom.createDom('span', contentClassName);

  return contentElem;
};


/** @override */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.getCssClass = function() {
  return bluemind.ui.style.CartoucheBoxItemRenderer.CSS_CLASS;
};

/** @override */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.getClassForState = function(state) {
  if (!this.classFromStates_) {
    this.createClassStatesMap_();
  }
  if (this.classFromStates_[state]) {
    return this.classFromStates_[state];
  } 
  return goog.base(this, 'getClassForState', state);
};

/** @override */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.getStateFromClass = function(className) {
  if (!this.stateFromClass_) {
    this.createClassStatesMap_();
  }
  if (this.stateFromClass_[className]) {
    return /** @type {goog.ui.Component.State} */ (parseInt(this.stateFromClass_[className], 10));
  } 
  return goog.base(this, 'getStateFromClass', className);
};


/**
 * Creates the lookup table of states to classes, used during state changes.
 * @private
 */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.createClassStatesMap_ = function() {
  var baseClass = this.getStructuralCssClass();

  this.classFromStates_ = goog.object.create(
      goog.ui.Component.State.READONLY, goog.getCssName(baseClass, 'readonly'));
  this.stateFromClass_ = goog.object.transpose(this.classFromStates_);
 
};


/** @override */
bluemind.ui.style.CartoucheBoxItemRenderer.prototype.setContent = function(element, content) {
  goog.base(this,'setContent', element, content);
  var css = goog.getCssName(this.getStructuralCssClass(), 'empty');
  goog.dom.classlist.enable(element, css, !content);
};

/**
 * Register a decorator factory function for
 * bluemind.ui.style.CartoucheBoxItemRenderer.
 */
goog.ui.registry.setDecoratorByClassName(
  bluemind.ui.style.CartoucheBoxItemRenderer.CSS_CLASS, function() {
  return new bluemind.ui.CartoucheBoxItem(null, null, 
    bluemind.ui.style.CartoucheBoxItemRenderer.getInstance());
});

