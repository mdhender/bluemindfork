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
 * @fileoverview Renderer for {@link net.bluemind.ui.List}s.
 * 
 */

goog.provide("net.bluemind.ui.style.SubListRenderer");

goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.style");
goog.require("goog.a11y.aria.Role");
goog.require("goog.dom.classlist");
goog.require("goog.ui.registry");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.ListItem");
goog.require("net.bluemind.ui.style.ListItemRenderer");

/**
 * Default renderer for {@link net.bluemind.ui.SubList}s. Each item has the
 * following structure:
 * 
 * <pre>
 *   &lt;div class=&quot;sublist&quot;&gt;
 *     &lt;div class=&quot;sublist-content&quot;&gt;
 *     &lt;/div&gt;
 *     &lt;div class=&quot;listitem&quot;&gt;
 *       &lt;div class=&quot;listitem-content&quot;&gt;
 *         ...(list item contents)...
 *       &lt;/div&gt;
 *     &lt;/div&gt;
 *   &lt;/div&gt;
 * </pre>
 * 
 * @constructor
 * @extends {net.bluemind.ui.style.ListItemRenderer}
 */
net.bluemind.ui.style.SubListRenderer = function() {
  goog.base(this);
};
goog.inherits(net.bluemind.ui.style.SubListRenderer, net.bluemind.ui.style.ListItemRenderer);
goog.addSingletonGetter(net.bluemind.ui.style.SubListRenderer);

/**
 * CSS class name the renderer applies to list item elements.
 * 
 * @type {string}
 */
net.bluemind.ui.style.SubListRenderer.CSS_CLASS = goog.getCssName('bm-sublist');

net.bluemind.ui.style.SubListRenderer.CSS_CLASS_SUBMENU_ = goog.getCssName('goog-submenu-arrow');

/** @return {goog.a11y.aria.Role} The ARIA role. */
net.bluemind.ui.style.SubListRenderer.prototype.getAriaRole = function() {
  return goog.a11y.aria.Role.LISTITEM;
};

/** @override */
net.bluemind.ui.style.SubListRenderer.prototype.createDom = function(item) {
  var element = item.getDomHelper().createDom('div', this.getClassNames(item).join(' '), this.createCaption(item),
      this.createContent(item.getDomHelper()));
  this.setContent(element, item.getContent());
  goog.dom.classlist.add(element, net.bluemind.ui.style.SubListRenderer.CSS_CLASS);
  this.setAriaStates(item, element);
  return element;
};

/**
 * Create a structural element for sublist item's . caption
 * 
 * @param {goog.ui.Control} control Control object.
 * @return {Element} List item caption element.
 * @protected
 */
net.bluemind.ui.style.SubListRenderer.prototype.createCaption = function(control) {
  var dom = control.getDomHelper();

  var classes = [];
  classes.push(goog.getCssName(this.getCssClass(), 'caption'));
  var captionElem = dom.createDom('span', classes);

  var iconClass = [];
  if (control.hasState(goog.ui.Component.State.OPENED)) {
    iconClass.push(goog.getCssName('fa fa-chevron-down'));
  } else {
    iconClass.push(goog.getCssName('fa fa-chevron-right'));
  }
  var iconElement = dom.createDom('div', iconClass);

  var parentElem = dom.createDom('div');
  parentElem.appendChild(iconElement);
  parentElem.appendChild(captionElem);

  return parentElem;
};

/**
 * Takes the control's root element and returns the parent element of the
 * sublist's caption.
 * 
 * @param {Element} element Root element of the control whose content element is
 *          to be returned.
 * @return {Element} The control's content element.
 */
net.bluemind.ui.style.SubListRenderer.prototype.getCaptionElement = function(element) {
  return (/** @type {Element} */
  (element && goog.dom.getElementByClass(goog.getCssName(this.getCssClass(), 'caption'), element)));
};

net.bluemind.ui.style.SubListRenderer.prototype.getIconElement = function(element) {
  return (/** @type {Element} */
  (element && goog.dom.getElementByClass(goog.getCssName('fa'), element)));
};


/** @override */
net.bluemind.ui.style.SubListRenderer.prototype.decorate = function(control, element) {
  var subList = control;
  element = goog.base(this, 'decorate', subList, element);
  goog.dom.classlist.add(element, net.bluemind.ui.style.SubListRenderer.CSS_CLASS);
  // Search for a child menu and decorate it.
  var children = goog.dom.getElementsByTagNameAndClass('div', goog.getCssName('bm-listitem'), element);
  for (var i = 0; i < children.length; i++) {
    var child = children[i];
    // Hide the menu element before attaching it to the document body; see
    // bug 1089244.
    goog.style.setElementShown(child, false);
    control.getDomHelper().getDocument().body.appendChild(child);
    var item = new net.bluemind.ui.ListItem('');
    item.decorate(child);
    subList.addChild(item);
  }
  return element;
};

/**
 * Appends a child node with the class goog.getCssName('goog-submenu-arrow') or
 * 'goog-submenu-arrow-rtl' which can be styled to show an arrow.
 * 
 * @param {goog.ui.Control} subList SubList to render.
 * @param {Element} element Element to decorate.
 * @private
 */
net.bluemind.ui.style.SubListRenderer.prototype.addArrow_ = function(subList, element) {
  var arrow = subList.getDomHelper().createDom('span');
  arrow.className = net.bluemind.ui.style.SubListRenderer.CSS_CLASS_SUBMENU_;
  this.getContentElement(element).appendChild(arrow);
};

/** @override */
net.bluemind.ui.style.SubListRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.style.SubListRenderer.CSS_CLASS;
};

/** @override */
net.bluemind.ui.style.SubListRenderer.prototype.setContent = function(element, content) {
  var contentElem = this.getCaptionElement(element);
  if (contentElem) {
    goog.dom.removeChildren(contentElem);
    if (content) {
      if (goog.isString(content)) {
        goog.dom.setTextContent(contentElem, content);
      } else {
        var childHandler = function(child) {
          if (child) {
            var doc = goog.dom.getOwnerDocument(contentElem);
            contentElem.appendChild(goog.isString(child) ? doc.createTextNode(child) : child);
          }
        };
        if (goog.isArray(content)) {
          // Array of nodes.
          goog.array.forEach(content, childHandler);
        } else if (goog.isArrayLike(content) && !('nodeType' in content)) {
          // NodeList. The second condition filters out TextNode which also has
          // length attribute but is not array like. The nodes have to be cloned
          // because childHandler removes them from the list during iteration.
          goog.array.forEach(goog.array.clone(/** @type {NodeList} */
          (content)), childHandler);
        } else {
          // Node or string.
          childHandler(content);
        }
      }
    }
  }
};

/** @override */
net.bluemind.ui.style.SubListRenderer.prototype.setState = function(control, state, enable) {
  goog.base(this, 'setState', control, state, enable);
  if (state == goog.ui.Component.State.OPENED) {
    var el = this.getIconElement(control.getElement());
    if (el) {
      var isHidden = this.getContentElement(control.getElement()).parentElement.style.display == 'none';
      goog.dom.classlist.enable(el, goog.getCssName('fa-chevron-right'), isHidden);
      goog.dom.classlist.enable(el, goog.getCssName('fa-chevron-down'), !isHidden);
    }
  }
};

/**
 * Register a decorator factory function for
 * net.bluemind.ui.style.SubListRenderer.
 */
goog.ui.registry.setDecoratorByClassName(net.bluemind.ui.style.SubListRenderer.CSS_CLASS, function() {
  return new net.bluemind.ui.SubList(null, net.bluemind.ui.style.SubListRenderer.getInstance());
});
