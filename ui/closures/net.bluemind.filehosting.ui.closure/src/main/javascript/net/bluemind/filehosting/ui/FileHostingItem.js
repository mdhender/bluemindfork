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
goog.provide("net.bluemind.filehosting.ui.FileHostingItem");

goog.require("goog.ui.Control");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.filehosting.ui.FileHostingItemRenderer");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent=} opt_content
 * @param {string=} opt_path
 * @param {goog.ui.ControlRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {goog.ui.Control}
 */
net.bluemind.filehosting.ui.FileHostingItem = function(opt_content, opt_path, opt_renderer, opt_domHelper) {
  var renderer = opt_renderer || net.bluemind.filehosting.ui.FileHostingItemRenderer.getInstance();
  goog.base(this, opt_content, renderer, opt_domHelper);
  this.path_ = opt_path || '';
}
goog.inherits(net.bluemind.filehosting.ui.FileHostingItem, goog.ui.Control);

/**
 * @type {string}
 * @private
 */
net.bluemind.filehosting.ui.FileHostingItem.prototype.path_;

/**
 * Sets the filehosting item to be selectable or not. Set to true for
 * filehosting items that represent selectable files.
 * 
 * @param {boolean} selectable Whether the filehosting item is selectable.
 */
net.bluemind.filehosting.ui.FileHostingItem.prototype.setSelectable = function(selectable) {
  this.setSupportedState(goog.ui.Component.State.SELECTED, selectable);
};

/** @override */
net.bluemind.filehosting.ui.FileHostingItem.prototype.getModel = function() {
  var model = goog.base(this, 'getModel');
  return model || this.path_;
};

/**
 * set file path
 * 
 * @param {string} path
 */
net.bluemind.filehosting.ui.FileHostingItem.prototype.setPath = function(path) {
  this.path_ = path;
};

/**
 * Return file path
 * 
 * @return {string}
 */
net.bluemind.filehosting.ui.FileHostingItem.prototype.getPath = function() {
  return this.path_;
};
