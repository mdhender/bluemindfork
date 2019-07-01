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

goog.provide('net.bluemind.calendar.navigation.ui.SaveViewDialog');

goog.require('goog.dom.classlist');
goog.require('goog.ui.Dialog');
goog.require('goog.ui.ComboBox');
goog.require('net.bluemind.calendar.navigation.events.EventType');
goog.require('net.bluemind.calendar.navigation.events.SaveViewEvent');
goog.require('net.bluemind.calendar.navigation.templates');

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper; see {@link
 *          goog.ui.Component} for semantics.
 * @constructor
 * @extends {goog.ui.Dialog}
 */
net.bluemind.calendar.navigation.ui.SaveViewDialog = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
  this.setDraggable(false);
};
goog.inherits(net.bluemind.calendar.navigation.ui.SaveViewDialog, goog.ui.Dialog);

/** @override */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.createDom = function() {
  var elem = goog.soy.renderAsElement(net.bluemind.calendar.navigation.templates.saveviewdialog);
  this.decorateInternal(elem);
};

/**
 * User's saved views
 * 
 * @type {goog.ui.ComboBox}
 * @private
 */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.combo_;

/**
 * User's views
 * 
 * @type {goog.structs.Map}
 * @private
 */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.views_;

/** @override */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, function(e) {
    e.stopPropagation();
    if (e.key == 'save') {
      this.save_();
    }
  });

  var btn = goog.dom.getElement('avd-btn-save');
  this.getHandler().listen(btn, goog.events.EventType.CLICK, function(e) {
    e.stopPropagation();
    this.save_();
  });
};

/** @override */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.setVisible = function(visible) {
  goog.base(this, 'setVisible', visible);
  if (visible) {
    if (this.combo_)
      this.combo_.dispose();

    this.combo_ = new goog.ui.ComboBox();
    this.combo_.setUseDropdownArrow(false);
    this.combo_.setFieldName('save-dialog-view-title');
    this.combo_.render(goog.dom.getElement('save-dialog-view-title'));
    this.views_ = new goog.structs.Map();

    this.getHandler().listen(this.combo_, goog.ui.Component.EventType.CHANGE, this.handleCombo_);

    var btn = goog.dom.getElement('avd-btn-save');
    /** @meaning calendar.view.create */
    var MSG_CREATE_VIEW = goog.getMsg('Create view');
    btn.innerHTML = MSG_CREATE_VIEW;

    var views = this.getModel().views;
    goog.array.forEach(views, function(v) {
      if (!v.isDefault) {
        var item = new goog.ui.ComboBoxItem(v.label, v);
        this.combo_.addItem(item);
        this.views_.set(v.label, item);
      }
    }, this);

    var selected = goog.array.find(views, function(v) {
      return v.uid == this.getModel().selected;
    }, this);

    if (selected) {
      if (!selected.isDefault) {
        this.combo_.setValue(selected.label);
      } else {
        this.combo_.setValue(null);
      }
    }
  }
};

/**
 * 
 */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.handleCombo_ = function() {
  var btn = goog.dom.getElement('avd-btn-save');
  if (this.combo_.getValue() != '' && this.views_.containsKey(this.combo_.getValue())) {
    /** @meaning calendar.view.update */
    var MSG_UPDATE_VIEW = goog.getMsg('Update view');
    btn.innerHTML = MSG_UPDATE_VIEW;
  } else {
    /** @meaning calendar.view.create */
    var MSG_CREATE_VIEW = goog.getMsg('Create view');
    btn.innerHTML = MSG_CREATE_VIEW;
  }
};

/**
 * Set combo label
 * 
 * @param {string} label label to set.
 */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.setLabel = function(label) {
  this.combo_.setValue(label);
};

/**
 * save current view
 * 
 * @private
 */
net.bluemind.calendar.navigation.ui.SaveViewDialog.prototype.save_ = function() {
  var label = this.combo_.getValue();
  if (goog.string.trim(label) == '') {
    goog.dom.classlist.add(goog.dom.getElement('save-dialog-view-title'), goog.getCssName('error'));
  } else {
    var item = this.views_.get(label);
    if (item) {
      this.dispatchEvent(new net.bluemind.calendar.navigation.events.SaveViewEvent(item.getModel().uid, label));
      this.setVisible(false);
    } else {
      this.dispatchEvent(new net.bluemind.calendar.navigation.events.SaveViewEvent(null, label));
      this.setVisible(false);
    }
  }
};
