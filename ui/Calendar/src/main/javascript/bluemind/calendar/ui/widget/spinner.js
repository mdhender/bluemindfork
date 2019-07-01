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
 * @fileoverview Spinner componnent.
 */

goog.provide('bluemind.calendar.ui.widget.Spinner');

goog.require('bluemind.calendar.template');
goog.require('goog.dom.classlist');
goog.require('goog.style');
goog.require('goog.ui.Component');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.widget.Spinner = function(opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(bluemind.calendar.ui.widget.Spinner, goog.ui.Component);

/** @inheritDoc */
bluemind.calendar.ui.widget.Spinner.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.decorateInternal(this.getElement());
};

/** @inheritDoc */
bluemind.calendar.ui.widget.Spinner.prototype.decorateInternal = function(el) {
  goog.base(this, 'decorateInternal', el);
  goog.dom.classlist.add(el, goog.getCssName('bm-spinner'));
  //TODO: Howto do i18n without template??
  el.innerHTML = bluemind.calendar.template.spinner();
  goog.style.setElementShown(el, false);
};

/**
 * Show spinner
 */
bluemind.calendar.ui.widget.Spinner.prototype.show = function() {
  goog.style.setElementShown(this.getElement(), true);

};

/**
 * Hide spinner
 */
bluemind.calendar.ui.widget.Spinner.prototype.hide = function() {
  goog.style.setElementShown(this.getElement(), false);
};



