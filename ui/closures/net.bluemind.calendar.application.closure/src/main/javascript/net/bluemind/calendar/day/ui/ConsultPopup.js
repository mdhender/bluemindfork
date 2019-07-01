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
 * @fileoverview Event creation bubble graphic componnent.
 */

goog.provide("net.bluemind.calendar.day.ui.ConsultPopup");

goog.require("goog.dom");
goog.require("goog.soy");
goog.require("goog.events.EventType");
goog.require("net.bluemind.calendar.day.templates");
goog.require("net.bluemind.calendar.day.ui.Popup");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vevent.VEventEvent");

/**
 * @param {net.bluemind.i18n.DateTimeHelper.Formatter} format Formatter
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.day.ui.ConsultPopup = function(format, opt_domHelper) {
  goog.base(this, format, opt_domHelper);
};
goog.inherits(net.bluemind.calendar.day.ui.ConsultPopup, net.bluemind.calendar.day.ui.Popup);

/** @override */
net.bluemind.calendar.day.ui.ConsultPopup.prototype.setListeners = function() {
  goog.base(this, 'setListeners');

  var detail = goog.dom.getElement('eb-btn-event-consult-screen');
  this.getHandler().listen(detail, goog.events.EventType.CLICK, this.showDetails_, false, this);
};

net.bluemind.calendar.day.ui.ConsultPopup.prototype.showDetails_ = function(e) {
  e.stopPropagation();
  this.hide();
  var e = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.DETAILS, this.getModel());
  this.dispatchEvent(e);
}

/** @override */
net.bluemind.calendar.day.ui.ConsultPopup.prototype.buildContent = function() {
  var calendar = goog.array.find(this.calendars, function(calendar) {
    return calendar.uid == this.getModel().calendar;
  }, this);
  return goog.soy.renderAsElement(net.bluemind.calendar.day.templates.consult, {
    event : this.getModel(),
    calendar : calendar
  });
};
