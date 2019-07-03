/*
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
goog.provide("net.bluemind.ui.ButtonLinkRenderer");

goog.require("net.bluemind.ui.LinkRenderer");

/**
 * @constructor
 * 
 * @extends {net.bluemind.ui.LinkRenderer}
 */
net.bluemind.ui.ButtonLinkRenderer = function() {
  net.bluemind.ui.LinkRenderer.call(this);
  // TODO Auto-generated constructor stub
}
goog.inherits(net.bluemind.ui.ButtonLinkRenderer, net.bluemind.ui.LinkRenderer);
goog.addSingletonGetter(net.bluemind.ui.ButtonLinkRenderer);

net.bluemind.ui.ButtonLinkRenderer.CSS_CLASS = goog.getCssName('bluemind-link');

/** @override */
net.bluemind.ui.ButtonLinkRenderer.prototype.getStructuralCssClass = function() {
  return goog.getCssName('goog-button-base');
};

/** @override */
net.bluemind.ui.ButtonLinkRenderer.prototype.getCssClass = function() {
  return net.bluemind.ui.ButtonLinkRenderer.CSS_CLASS;
};