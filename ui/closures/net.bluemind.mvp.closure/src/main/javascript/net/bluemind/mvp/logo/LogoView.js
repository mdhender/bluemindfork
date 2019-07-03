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
 * @fileoverview View class for application header (Logo + logo).
 */

goog.provide('net.bluemind.mvp.logo.LogoView');

goog.require('goog.ui.Component');
goog.require('goog.dom.classlist')
goog.require('net.bluemind.mvp.logo.template.logo');

/**
 * View class for application logo.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.mvp.logo.LogoView = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};

goog.inherits(net.bluemind.mvp.logo.LogoView, goog.ui.Component);

/** @override */
net.bluemind.mvp.logo.LogoView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var el = this.getElement();
  goog.dom.classlist.add(el, goog.getCssName('logo'))
  el.innerHTML = net.bluemind.mvp.logo.template.logo.main(this.getModel());
};

/**
 * @return {Object.<string, *>}
 * @suppress {checkTypes}
 * @override
 */
net.bluemind.mvp.logo.LogoView.prototype.getModel = function() {
  return goog.base(this, 'getModel');
};
