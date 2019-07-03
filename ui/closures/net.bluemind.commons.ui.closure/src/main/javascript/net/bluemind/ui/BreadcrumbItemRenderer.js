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
goog.provide("net.bluemind.ui.BreadcrumbItemRenderer");

goog.require("goog.ui.LinkButtonRenderer");

/**
 * @constructor
 * 
 * @extends {goog.ui.LinkButtonRenderer}
 */
net.bluemind.ui.BreadcrumbItemRenderer = function() {
  goog.ui.LinkButtonRenderer.call(this);
}
goog.inherits(net.bluemind.ui.BreadcrumbItemRenderer, goog.ui.LinkButtonRenderer);

goog.addSingletonGetter(net.bluemind.ui.BreadcrumbItemRenderer);

net.bluemind.ui.BreadcrumbItemRenderer.CSS_CLASS = goog.getCssName('breadcrumb-item');

/** @override */
net.bluemind.ui.BreadcrumbItemRenderer.prototype.getClassNames = function(control) {
  var classes = goog.base(this, 'getClassNames', control);
  classes.push(net.bluemind.ui.BreadcrumbItemRenderer.CSS_CLASS);
  return classes;
}