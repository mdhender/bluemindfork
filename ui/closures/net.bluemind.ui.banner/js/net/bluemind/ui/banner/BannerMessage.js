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
 */

goog.provide("net.bluemind.ui.BannerMessage");

goog.require("goog.dom.classes");
goog.require("goog.dom.classlist");
goog.require("goog.ui.Component");
goog.require("goog.ui.Control");
goog.require("net.bluemind.ui.BannerMessageTemplate");

// required symbol

/**
 * Simple widget to display a message in banner
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Control}
 */
net.bluemind.ui.BannerMessage = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
};
goog.inherits(net.bluemind.ui.BannerMessage, goog.ui.Control);

/** @override */
net.bluemind.ui.BannerMessage.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getDomHelper().getDocument(), 'ui-banner-message', this.handleBannerMessage_);
};

/**
 * @param {*} e
 */
net.bluemind.ui.BannerMessage.prototype.handleBannerMessage_ = function(e) {
  var event = e.getBrowserEvent();

  var msg = new goog.ui.Component();

  if(event['detail']['closeable']) {
    var close = new goog.ui.Component();
    var pos = this.getChildCount();
    this.addChild(close, true);
    goog.dom.classlist.add(close.getElement(), goog.getCssName('bannerClose'));
    goog.dom.classlist.add(close.getElement(), goog.getCssName('fa'));
    goog.dom.classlist.add(close.getElement(), goog.getCssName('fa-times'));
    this.getHandler().listen(close.getElement(), goog.events.EventType.CLICK, function() {
      this.removeChild(close, true);
      this.removeChild(msg, true);
    }); 
  }

  this.addChild(msg, true);
  goog.dom.classlist.add(msg.getElement(), goog.getCssName('bannerMsg'));
  msg.getElement().innerHTML = net.bluemind.ui.BannerMessageTemplate.message({
    type : event['detail']['type'],
    msg : event['detail']['message'],
    link : event['detail']['link']
  });

}

/** @override */
net.bluemind.ui.BannerMessage.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.decorateInternal(this.getElement());
};

/** @override */
net.bluemind.ui.BannerMessage.prototype.decorateInternal = function(el) {
  goog.base(this, 'decorateInternal', el);
  goog.dom.classlist.add(el, goog.getCssName('bannerMessageContainer'));
};
