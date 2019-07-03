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
goog.provide("net.bluemind.ui.BreadcrumbItem");

goog.require("goog.ui.Button");
goog.require("goog.ui.Control");
goog.require("net.bluemind.ui.BreadcrumbItemRenderer");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent=} opt_content
 * @param {goog.ui.ButtonRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {goog.ui.Button}
 */
net.bluemind.ui.BreadcrumbItem = function(opt_content, opt_renderer, opt_domHelper) {
  var renderer = opt_renderer || net.bluemind.ui.BreadcrumbItemRenderer.getInstance();
  goog.ui.Button.call(this, opt_content, renderer, opt_domHelper);
}
goog.inherits(net.bluemind.ui.BreadcrumbItem, goog.ui.Button);