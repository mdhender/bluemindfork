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
goog.provide("net.bluemind.ui.Breadcrumb");

goog.require("goog.array");
goog.require("goog.ui.Container");
goog.require("goog.ui.Container.Orientation");
goog.require("net.bluemind.ui.BreadcrumbItem");
goog.require("net.bluemind.ui.BreadcrumbRenderer");
goog.require("net.bluemind.ui.BreadcrumbSeparator");

/**
 * @constructor
 * 
 * @param {net.bluemind.ui.BreadcrumbRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {goog.ui.Container}
 */
net.bluemind.ui.Breadcrumb = function(opt_renderer, opt_domHelper) {
  var renderer = opt_renderer || net.bluemind.ui.BreadcrumbRenderer.getInstance();
  goog.ui.Container.call(this, goog.ui.Container.Orientation.HORIZONTAL, renderer, opt_domHelper);
  this.setFocusable(false);
  this.setFocusableChildrenAllowed(true);
}
goog.inherits(net.bluemind.ui.Breadcrumb, goog.ui.Container);

/**
 * Path separator
 * 
 * @type {string}
 * @private
 */
net.bluemind.ui.Breadcrumb.prototype.separator_ = '/';

/**
 * Set breadcrumb full path. The path will be exploded with #getSeparator.
 * 
 * @param {*} path
 */
net.bluemind.ui.Breadcrumb.prototype.setPath = function(path) {
  this.reset();
  var elements = path.split(this.separator_);
  var tmp = this.separator_;
  /** @meaning general.userhome */
  var MSG_HOME = goog.getMsg('Home');
  var child = new net.bluemind.ui.BreadcrumbItem(MSG_HOME);
  child.setModel(tmp);
  child.setId(tmp);
  this.addChild(child, true);
  goog.array.forEach(elements, function(element) {
    if (element != '') {
      this.addChild(new net.bluemind.ui.BreadcrumbSeparator(), true);
      tmp += element + this.separator_;
      var child = new net.bluemind.ui.BreadcrumbItem(element);
      child.setModel(tmp);
      child.setId(tmp);
      this.addChild(child, true);
    }
  }, this);
};

/** @override */
net.bluemind.ui.Breadcrumb.prototype.addChildAt = function(control, index, opt_render) {
  goog.base(this, 'addChildAt', control, index, opt_render);
  this.getChildAt(this.getChildCount() - 1).setEnabled(false);
  if (this.getChildCount() > 1) {
    this.getChildAt(this.getChildCount() - 2).setEnabled(true);
  }
}

/** @override */
net.bluemind.ui.Breadcrumb.prototype.removeChild = function(control, opt_render) {
  var child = goog.base(this, 'removeChild', control, opt_render);
  if (this.getChildCount() > 0) {
    this.getChildAt(this.getChildCount() - 1).setEnabled(false);
  }
  return child;
}

/**
 * Reset breadcrumb.
 */
net.bluemind.ui.Breadcrumb.prototype.reset = function() {
  this.forEachChild(function(child) {
    child.dispose();
  });
  this.removeChildren();
};