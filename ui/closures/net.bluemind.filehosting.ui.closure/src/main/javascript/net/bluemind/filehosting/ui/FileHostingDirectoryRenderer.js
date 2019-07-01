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
goog.provide("net.bluemind.filehosting.ui.FileHostingDirectoryRenderer");

goog.require("net.bluemind.filehosting.ui.FileHostingItemRenderer");

/**
 * @constructor
 * 
 * @extends {net.bluemind.filehosting.ui.FileHostingItemRenderer}
 */
net.bluemind.filehosting.ui.FileHostingDirectoryRenderer = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.filehosting.ui.FileHostingDirectoryRenderer,
    net.bluemind.filehosting.ui.FileHostingItemRenderer);

goog.addSingletonGetter(net.bluemind.filehosting.ui.FileHostingDirectoryRenderer);

/** @override */
net.bluemind.filehosting.ui.FileHostingDirectoryRenderer.prototype.iconCSS = [ goog.getCssName('fa'),
    goog.getCssName('fa-5x'), goog.getCssName('fa-folder-o') ];

/**
 * Default CSS class to be applied to the root element of components rendered by
 * this renderer.
 * 
 * @type {string}
 */
net.bluemind.filehosting.ui.FileHostingDirectoryRenderer.CSS_CLASS = goog.getCssName('filehosting-directory');

/** @override */
net.bluemind.filehosting.ui.FileHostingDirectoryRenderer.prototype.getCssClass = function() {
  return net.bluemind.filehosting.ui.FileHostingDirectoryRenderer.CSS_CLASS;
};
