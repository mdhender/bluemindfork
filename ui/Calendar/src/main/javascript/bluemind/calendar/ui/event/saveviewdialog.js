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
 * @fileoverview save view dialog component.
 */

goog.provide('bluemind.calendar.ui.event.SaveViewDialog');

goog.require('bluemind.calendar.event.template');
goog.require('goog.ui.Dialog');

/**
 * @param {string} opt_class CSS class name for the dialog element, also used
 *    as a class name prefix for related elements; defaults to modal-dialog.
 * @param {boolean} opt_useIframeMask Work around windowed controls z-index
 *     issue by using an iframe instead of a div for bg element.
 * @param {goog.dom.DomHelper} opt_domHelper Optional DOM helper; see {@link
 *    goog.ui.Component} for semantics.

 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.event.SaveViewDialog =
  function(opt_class, opt_useIframeMask, opt_domHelper) {
  goog.ui.Dialog.call(this, opt_class, opt_useIframeMask, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(bluemind.calendar.ui.event.SaveViewDialog, goog.ui.Dialog);

/** @inheritDoc */
bluemind.calendar.ui.event.SaveViewDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(
    bluemind.calendar.event.template.saveViewDialog);
  this.decorateInternal(elem);
};

/**
 * User's saved views
 * @type {goog.ui.ComboBox}
 * @private
 */
bluemind.calendar.ui.event.SaveViewDialog.prototype.combo_;

/**
 * User's views
 * @type {goog.structs.Map}
 * @private
 */
bluemind.calendar.ui.event.SaveViewDialog.prototype.views_;

/** @inheritDoc */
bluemind.calendar.ui.event.SaveViewDialog.prototype.enterDocument =
  function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT,
    function(e) {
      e.stopPropagation();
      if (e.key == 'save') {
        this.save_();
      }
    }, false, this);

  var btn = goog.dom.getElement('avd-btn-save');
  this.getHandler().listen(btn,
    goog.events.EventType.CLICK, function(e) {
      e.stopPropagation();
      this.save_();
    }, false, this);
};

/** @inheritDoc */
bluemind.calendar.ui.event.SaveViewDialog.prototype.setVisible =
  function(visible) {
  bluemind.calendar.ui.event.SaveViewDialog.superClass_.setVisible.call(
    this, visible);
  if (visible) {
    if (this.combo_) this.combo_.dispose();

    this.combo_ = new goog.ui.ComboBox();
    this.combo_.setUseDropdownArrow(true);
    this.combo_.setFieldName('save-dialog-view-title');
    this.combo_.render(goog.dom.getElement('save-dialog-view-title'));
    this.views_ = new goog.structs.Map();

    this.getHandler().listen(this.combo_, goog.ui.Component.EventType.CHANGE,
      function(e) {
        if (this.combo_.getValue() != '' &&
          this.views_.containsKey(this.combo_.getValue())) {
          btn.innerHTML = bluemind.calendar.template.i18n.updateViewBtn();
        } else {
          btn.innerHTML = bluemind.calendar.template.i18n.newViewBtn();
        }
      }, this);

    var btn = goog.dom.getElement('avd-btn-save');
    btn.innerHTML = bluemind.calendar.template.i18n.newViewBtn();

    goog.array.forEach(bluemind.savedViews.getValues(), function(v) {
      var item = new goog.ui.ComboBoxItem(v.getLabel(), v);
      this.combo_.addItem(item);
      this.views_.set(v.getLabel(), item);
    }, this);
  }
};

/**
 * Set combo label
 * @param {string} label label to set.
 */
bluemind.calendar.ui.event.SaveViewDialog.prototype.setLabel = function(label) {
  this.combo_.setValue(label);
};

/**
 * save current view
 * @private
 */
bluemind.calendar.ui.event.SaveViewDialog.prototype.save_ = function() {
  var label = this.combo_.getValue();
  if (goog.string.trim(label) == '') {
    goog.dom.classes.add(goog.dom.getElement('save-dialog-view-title'),
      goog.getCssName('error'));
  } else {
    var v = this.views_.get(label);
    if (v != null) {
      bluemind.calendar.Controller.getInstance().overrideViewDialog(v);
    } else {
      bluemind.calendar.Controller.getInstance().saveView(label);
      this.setVisible(false);
    }
  }
};

