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
goog.provide("net.bluemind.filehosting.ui.FileHostingItemRenderer");

goog.require("goog.ui.ControlRenderer");
goog.require('goog.ui.INLINE_BLOCK_CLASSNAME');

/**
 * @constructor
 * 
 * @extends {goog.ui.ControlRenderer}
 */
net.bluemind.filehosting.ui.FileHostingItemRenderer = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.filehosting.ui.FileHostingItemRenderer, goog.ui.ControlRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.FileHostingItemRenderer);

/**
 * Default CSS class to be applied to the root element of components rendered by
 * this renderer.
 * 
 * @type {string}
 */
net.bluemind.filehosting.ui.FileHostingItemRenderer.CSS_CLASS = goog.getCssName('filehosting-item');
/** @override */
net.bluemind.filehosting.ui.FileHostingItemRenderer.prototype.getCssClass = function() {
  return net.bluemind.filehosting.ui.FileHostingItemRenderer.CSS_CLASS;
};

/** @override */
net.bluemind.filehosting.ui.FileHostingItemRenderer.prototype.getStructuralCssClass = function() {
  return goog.getCssName('filehosting-base');
};

/**
 * CSS class name to be applied to the icon element.
 * 
 * @type {Array.<string>}
 * @protected
 */
net.bluemind.filehosting.ui.FileHostingItemRenderer.prototype.iconCSS = [ goog.getCssName('fa'),
    goog.getCssName('fa-5x'), goog.getCssName('fa-file') ];

/** @override */
net.bluemind.filehosting.ui.FileHostingItemRenderer.prototype.createDom = function(item) {
  var classNames = this.getClassNames(item);
  var attributes = {
    'class' : goog.ui.INLINE_BLOCK_CLASSNAME + ' ' + classNames.join(' ')
  };
  var cssIcon = this.iconCSS.join(' ') + ' ' + goog.getCssName(this.getStructuralCssClass(), 'icon');
  var cssLabel = goog.getCssName(this.getStructuralCssClass(), 'label');

  var element = item.getDomHelper().createDom('div', goog.ui.INLINE_BLOCK_CLASSNAME + ' ' + classNames.join(' '),
      item.getDomHelper().createDom('div', cssIcon, ''),
      item.getDomHelper().createDom('div', cssLabel, item.getContent()));
  element.title = item.getContent() + '';
  this.setAriaStates(item, element);
  return element;
};

/** @override */
net.bluemind.filehosting.ui.FileHostingItemRenderer.prototype.canDecorate = function(element) {
  return false;
};
