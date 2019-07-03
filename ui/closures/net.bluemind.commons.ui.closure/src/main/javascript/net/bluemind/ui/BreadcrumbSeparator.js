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
goog.provide("net.bluemind.ui.BreadcrumbSeparator");

goog.require("goog.ui.Separator");
goog.require("net.bluemind.ui.BreadcrumbSeparatorRenderer");

/**
 * @constructor
 * 
 * @param {goog.ui.MenuSeparatorRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {goog.ui.Separator}
 */
net.bluemind.ui.BreadcrumbSeparator = function(opt_renderer, opt_domHelper) {
  var renderer = opt_renderer || net.bluemind.ui.BreadcrumbSeparatorRenderer.getInstance();
  goog.ui.Separator.call(this, renderer, opt_domHelper);
  // TODO Auto-generated constructor stub
}
goog.inherits(net.bluemind.ui.BreadcrumbSeparator, goog.ui.Separator);