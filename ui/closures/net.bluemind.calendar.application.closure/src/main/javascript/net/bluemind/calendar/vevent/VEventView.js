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
 * @fileoverview Event details componnents.
 */

goog.provide('net.bluemind.calendar.vevent.VEventView');

goog.require('goog.ui.Component');
goog.require('net.bluemind.calendar.vevent.ui.Form');

/**
 * View class for Calendar months view.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.VEventView = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
  var child = new net.bluemind.calendar.vevent.ui.Form();
  this.addChild(child, true)
};

goog.inherits(net.bluemind.calendar.vevent.VEventView, goog.ui.Component);