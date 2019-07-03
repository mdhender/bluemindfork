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
goog.provide("net.bluemind.ui.BreadcrumbRenderer");

goog.require("goog.ui.ContainerRenderer");

/**
 * @constructor
 * 
 * @extends {goog.ui.ContainerRenderer}
 */
net.bluemind.ui.BreadcrumbRenderer = function() {
  goog.ui.ContainerRenderer.call(this);
}
goog.inherits(net.bluemind.ui.BreadcrumbRenderer, goog.ui.ContainerRenderer);

goog.addSingletonGetter(net.bluemind.ui.BreadcrumbRenderer);

net.bluemind.ui.BreadcrumbRenderer.CSS_CLASS = goog.getCssName('breadcrumb');

/** @override */
net.bluemind.ui.BreadcrumbRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.BreadcrumbRenderer.CSS_CLASS;
}