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

/** @fileoverview Presenter for the application Logo */

goog.provide('net.bluemind.mvp.logo.LogoPresenter');

goog.require('goog.Promise');
goog.require('net.bluemind.mvp.Presenter');
goog.require('net.bluemind.mvp.logo.LogoView');
goog.require('bm.extensions.ExtensionsManager');

/**
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.mvp.logo.LogoPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.mvp.logo.LogoView();
  this.registerDisposable(this.view_);
};

goog.inherits(net.bluemind.mvp.logo.LogoPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.mvp.logo.LogoView}
 * @private
 */
net.bluemind.mvp.logo.LogoPresenter.prototype.view_;

/** @override */
net.bluemind.mvp.logo.LogoPresenter.prototype.init = function() {

  var x = bm.extensions.ExtensionsManager.getInstance();
  x = x.getExtensionPoint('net.bluemind.mvp.logo');
  var entries = goog.array.map(x.getExtensions(), function(ext) {
    return ext.data('logo');
  });

  goog.array.sort(entries, function(a, b) {
    var aIndex = 0;
    var bIndex = 0;
    if (a["priority"] != null) {
      aIndex = goog.string.parseInt(a["priority"]);
    }

    if (b["priority"] != null) {
      bIndex = goog.string.parseInt(b["priority"]);
    }

    return bIndex - aIndex;
  });

  var src = 'images/logo.png';
  if (entries.length > 0) {
    src = entries[0]['image'];
  }
  this.view_.setModel({
    image : src, // extension
    version : this.ctx.version
  });
  this.view_.render(goog.dom.getElement('header'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.mvp.logo.LogoPresenter.prototype.setup = function() {
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.mvp.logo.LogoPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};