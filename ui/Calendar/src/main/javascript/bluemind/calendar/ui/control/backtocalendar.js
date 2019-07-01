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
 * @fileoverview Back to calendar.
 */

goog.provide('bluemind.calendar.ui.control.BackToCalendar');

goog.require('bluemind.calendar.template.i18n');
goog.require('goog.soy');
goog.require('goog.ui.Button');
goog.require('goog.ui.style.app.ButtonRenderer');

/**
 * @constructor
 * @extends {goog.ui.Button}
 */
bluemind.calendar.ui.control.BackToCalendar = function() {
  goog.base(this, goog.dom.createDom('div',
    goog.getCssName('back-calendar-icon') +
    ' ' + goog.getCssName('goog-inline-block')),
    new goog.ui.style.app.ButtonRenderer.getInstance());
  this.setTooltip(bluemind.calendar.template.i18n.backToCalendar());
};
goog.inherits(bluemind.calendar.ui.control.BackToCalendar, goog.ui.Button);

/** @inheritDoc */
bluemind.calendar.ui.control.BackToCalendar.prototype.performActionInternal =
  function() {
  bluemind.view.lastView();
};
