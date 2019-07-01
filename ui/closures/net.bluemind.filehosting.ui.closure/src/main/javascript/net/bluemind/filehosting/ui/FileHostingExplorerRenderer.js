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
goog.provide("net.bluemind.filehosting.ui.FileHostingExplorerRenderer");

goog.require("goog.ui.ContainerRenderer");
goog.require("goog.ui.media.MediaModel.Credit");

/**
 * @constructor
 * 
 * @param {string=} opt_ariaRole
 * @extends {goog.ui.ContainerRenderer}
 */
net.bluemind.filehosting.ui.FileHostingExplorerRenderer = function(opt_ariaRole) {
  goog.base(this, opt_ariaRole);
};
goog.inherits(net.bluemind.filehosting.ui.FileHostingExplorerRenderer, goog.ui.ContainerRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.FileHostingExplorerRenderer);

/**
 * Default CSS class to be applied to the root element of components rendered by
 * this renderer.
 * 
 * @type {string}
 */
net.bluemind.filehosting.ui.FileHostingExplorerRenderer.CSS_CLASS = goog.getCssName('filehosting-explorer');

/**
 * @type {string}
 */
net.bluemind.filehosting.ui.FileHostingExplorerRenderer.EMPTY_CLASS = goog.getCssName('filehosting-empty');

/** @override */
net.bluemind.filehosting.ui.FileHostingExplorerRenderer.prototype.getCssClass = function() {
  return net.bluemind.filehosting.ui.FileHostingExplorerRenderer.CSS_CLASS;
};

/**
 * Set empty state
 * 
 * @param {net.bluemind.filehosting.ui.FileHostingExplorer} container
 * @param {boolean} enabled
 */
net.bluemind.filehosting.ui.FileHostingExplorerRenderer.prototype.setEmpty = function(container, enabled) {
  var element = this.getContentElement(container.getElement());
  var current = goog.dom.classlist.contains(element,
      net.bluemind.filehosting.ui.FileHostingExplorerRenderer.EMPTY_CLASS)
  if (!current && enabled) {
    /** @meaning filehosting.empty */
    var MSG_EMPTY = goog.getMsg('This folder is empty');
    var message = container.getDomHelper().createDom(
        'div',
        goog.getCssName('filehosting-message'),
        container.getDomHelper().createDom('div',
            [ goog.getCssName('fa'), goog.getCssName('fa-5x'), goog.getCssName('fa-ban') ]),
        container.getDomHelper().createDom('div', null, MSG_EMPTY));
    goog.dom.classlist.add(element, net.bluemind.filehosting.ui.FileHostingExplorerRenderer.EMPTY_CLASS);
    container.getDomHelper().appendChild(element, message);
  } else if (current && !enabled) {
    var message = container.getDomHelper().getElementByClass(goog.getCssName('filehosting-message'), element);
    container.getDomHelper().removeNode(message);
    goog.dom.classlist.remove(element, net.bluemind.filehosting.ui.FileHostingExplorerRenderer.EMPTY_CLASS);
  }
}
