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
goog.provide("net.bluemind.contact.individual.consult.IndividualConsultView");

goog.require("goog.ui.Component");
goog.require("net.bluemind.contact.individual.consult.templates");// FIXME -
// unresolved
// required
// symbol
// required symbol

/**
 * @constructor
 * 
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.contact.individual.consult.IndividualConsultView = function(opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
}
goog.inherits(net.bluemind.contact.individual.consult.IndividualConsultView, goog.ui.Component);

/** @override */
net.bluemind.contact.individual.consult.IndividualConsultView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.classlist.add(this.getElement(), goog.getCssName('vcard-consult'));
  if (this.getModel()) {
    this.getElement().innerHTML = net.bluemind.contact.individual.consult.templates.card({
      contact : this.getModel()
    });
  }
};

/** @override */
net.bluemind.contact.individual.consult.IndividualConsultView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var monitor = goog.dom.ViewportSizeMonitor.getInstanceForWindow(this.getDomHelper().getWindow());
  this.getHandler().listen(monitor, goog.events.EventType.RESIZE, this.resize_);
  this.resize_();
};

/**
 * Resize list
 * 
 * @private
 */
net.bluemind.contact.individual.consult.IndividualConsultView.prototype.resize_ = function() {
  var size = this.getDomHelper().getViewportSize();
  var height = size.height;

  var top = this.getElement().offsetTop;
  if (height - top > 400) {
    this.getElement().style.height = (height - top) + 'px';
  } else {
    this.getElement().style.height = '400px';
  }
};
/** @override */
net.bluemind.contact.individual.consult.IndividualConsultView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  if (this.getElement()) {
    this.getElement().innerHTML = net.bluemind.contact.individual.consult.templates.card({
      contact : this.getModel()
    });
  }
};
