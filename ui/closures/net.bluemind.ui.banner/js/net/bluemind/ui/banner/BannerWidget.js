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

/**
 * @fileoverview
 * 
 * Bluemind application banner.
 */

goog.provide("net.bluemind.ui.banner.BannerWidget");

goog.require("goog.dom");
goog.require("goog.log");
goog.require("goog.log.Logger");
goog.require("goog.dom.classlist");
goog.require("goog.ui.Control");

/**
 * Bluemind application banner
 * 
 * @param {Object} ext
 * @constructor
 * @extends {goog.ui.Control}
 */
net.bluemind.ui.banner.BannerWidget = function(ext) {
  goog.base(this);
  this.extension_ = ext;
  this.logger_ = goog.log.getLogger('net.bluemind.ui.banner.BannerWidget');
};
goog.inherits(net.bluemind.ui.banner.BannerWidget, goog.ui.Control);

/**
 * @type {Object}
 * @private
 */
net.bluemind.ui.banner.BannerWidget.prototype.extension_;

/**
 * @private
 * @type {goog.log.Logger}
 */
net.bluemind.ui.banner.BannerWidget.prototype.logger_;

/** @override */
net.bluemind.ui.banner.BannerWidget.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  goog.dom.classlist.set(this.getElement(), 'bm-widget');
  var dom = this.getDomHelper();
  if (this.extension_ == null) {
    return;
  }

  this.setVisible(false);

  try {
    var creator = this.extension_['creator'];
    this.extension_.extension.resolve().then(
      function() {
        var w = window[creator];
        if (w) {
          goog.dom.append(this.getElement(), w.apply());
          goog.log.info(this.logger_, "widget "
            + this.extension_.extension + " resolved ");
          this.setVisible(true);
        } else {
          goog.log.warning(this.logger_, 'Fail to load widget ' + creator
            + '. window["' + creator + '"] does not exist');
        }
      }, null, this);

  } catch (e) {
    goog.log.error(this.logger_, 'Fail to load widget ' + creator
      + '. window["' + creator + '"] throw exception ', e);
  }
}
