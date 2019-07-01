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
 * @fileoverview toolbar.
 */

goog.provide('bluemind.ui.Toolbar');

goog.require('goog.ui.Toolbar');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.ui.Toolbar = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(bluemind.ui.Toolbar, goog.ui.Component);
goog.addSingletonGetter(bluemind.ui.Toolbar);

/**
 * @type {goog.ui.Container} west panel
 */
bluemind.ui.Toolbar.prototype.west_;

/**
 * @type {goog.ui.Container} east panel
 */
bluemind.ui.Toolbar.prototype.east_;

/**
 * @return {goog.ui.Container} west panel.
 */
bluemind.ui.Toolbar.prototype.getWest = function() {
  return this.west_;
};

/**
 * @return {goog.ui.Container} east panel.
 */
bluemind.ui.Toolbar.prototype.getEast = function() {
  return this.east_;
};

/**
 * @param {goog.ui.Container} west panel.
 */
bluemind.ui.Toolbar.prototype.setWest = function(west) {
  this.west_ = west;
};

/**
 * @param {goog.ui.Container} east panel.
 */
bluemind.ui.Toolbar.prototype.setEast = function(east) {
  this.east_ = east;
};

/** @inheritDoc */
bluemind.ui.Toolbar.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.east_ = new goog.ui.Toolbar();
  this.addChild(this.east_, true);
  goog.dom.classes.add(this.east_.getContentElement(),
    goog.getCssName('toolbar'));
  goog.dom.classes.add(this.east_.getContentElement(),
    goog.getCssName('FR'));

  this.west_ = new goog.ui.Toolbar();
  this.addChild(this.west_, true);
  goog.dom.classes.add(this.west_.getContentElement(),
    goog.getCssName('toolbar'));
};

/** @inheritDoc */
bluemind.ui.Toolbar.prototype.removeChildren = function() {
  this.east_.removeChildren(true);
  this.west_.removeChildren(true);
  goog.dom.setTextContent(this.east_.getContentElement(), ''); // Crap (david)
  goog.dom.setTextContent(this.west_.getContentElement(), ''); // Crap (david)

};
