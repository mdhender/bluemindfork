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

goog.provide("net.bluemind.filehosting.ui.FileHostingDirectory");

goog.require("net.bluemind.filehosting.ui.FileHostingDirectoryRenderer");
goog.require("net.bluemind.filehosting.ui.FileHostingItem");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent=} opt_content
 * @param {string=} opt_path
 * @param {goog.ui.ControlRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {net.bluemind.filehosting.ui.FileHostingItem}
 */
net.bluemind.filehosting.ui.FileHostingDirectory = function(opt_content, opt_path, opt_renderer, opt_domHelper) {
  var renderer = opt_renderer || net.bluemind.filehosting.ui.FileHostingDirectoryRenderer.getInstance();
  goog.base(this, opt_content, opt_path, renderer, opt_domHelper);
}
goog.inherits(net.bluemind.filehosting.ui.FileHostingDirectory, net.bluemind.filehosting.ui.FileHostingItem);
