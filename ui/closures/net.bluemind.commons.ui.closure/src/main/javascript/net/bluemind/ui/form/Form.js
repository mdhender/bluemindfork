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
goog.provide("net.bluemind.ui.form.Form");

goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.soy");
goog.require("goog.style");
goog.require("goog.ui.Component");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Select");
goog.require("goog.ui.Tab");
goog.require("goog.ui.TabBar");
goog.require("goog.ui.Textarea");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.date.Date");
goog.require("net.bluemind.ui.form.templates");
goog.require("bluemind.ui.RichText");
goog.require("bluemind.ui.TagBox");

/**
 * @constructor
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.ui.form.Form = function(opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);

  this.formatter = null; // formatter;
  this.parser = null; // parser;

  var child = new net.bluemind.ui.form.Form.Notification();
  child.setId('notifications')
  this.addChild(child, true);

}
goog.inherits(net.bluemind.ui.form.Form, goog.ui.Component);

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Formatter}
 */
net.bluemind.ui.form.Form.prototype.formatter;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Parser}
 * @private
 */
net.bluemind.ui.form.Form.prototype.parser;

/** @override */
net.bluemind.ui.form.Form.prototype.createDom = function() {
  this.setElementInternal(this.getDomHelper().createElement('form'));
};

/** @override */
net.bluemind.ui.form.Form.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  if (this.getChild('_tabs')) {
    this.getHandler().listen(this.getChild('_tabs'), goog.ui.Component.EventType.SELECT, this.onTabSelected_);
  }
};

/**
 * Default display method for adding a child
 * 
 * @param {goog.ui.Component} child Field component
 * @param {string} label Field label
 * @param {Array.<string>=} css Extra classes
 * @protected
 */
net.bluemind.ui.form.Form.prototype.addTab = function(id, label) {
  if (!this.getChild('_tabs')) {
    var bar = new goog.ui.TabBar();
    bar.addChild(tab, true);
    bar.setId('tabs');
    this.addChild(bar, true);
    bar.setVisible(false);
  }
  var tab = new goog.ui.Tab(label);
  tab.setId(id);
  this.getChild('_tabs').addChild(tab, true);
  tab.setSelected(this.getChild('_tabs').getChildCount() == 1);
};

/**
 * Show details form fields and hide rrule form field if .
 * 
 * @param {goog.events.Event} e Tab selected event
 */
net.bluemind.ui.form.Form.prototype.onTabSelected_ = function(e) {
  var tab, visible, tabs = this.getChild('_tabs');
  for (var i = 0; i < tabs.getChildCount(); i++) {
    visible = tabs.getSelectedTabIndex() == i;
    tab = tabs.getChildAt(i);
    goog.array.forEach(tab.getModel() || [], function(id) {
      var child = this.getChild(id).getElement();
      var row = this.getDomHelper().getAncestorByClass(child, goog.getCssName('field-base'));
      goog.style.setElementShown(row, visible);
    }, this)
  }
};

/**
 * Add an error to the form
 * 
 * @param {string} module Module that own this error.
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} opt_text Optional error text.
 * @private
 */
net.bluemind.ui.form.Form.prototype.addError_ = function(module, fields, opt_text) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }

  for (var i = 0; i < fields.length; i++) {
    var field = fields[i];
    if (goog.typeOf(field) == 'string') {
      field = goog.dom.getElement(field);
      fields[i] = field;
    }
    this.errors_.get(module).push({
      type : 'field',
      value : field
    });
    goog.dom.classlist.add(field, goog.getCssName('error'));
  }
  if (!opt_text) {
    opt_text = 'error';
  }

  this.getChild('notifications').addError(fields, opt_text);
};

/**
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */

net.bluemind.ui.form.Form.Notification = function(opt_domHelper) {
  goog.base(this, opt_domHelper);

  this.errors_ = new goog.structs.Map();
  this.warnings_ = new goog.structs.Map();
  this.notices_ = new goog.structs.Map();

};
goog.inherits(net.bluemind.ui.form.Form.Notification, goog.ui.Component);

/**
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.popup_;

/**
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.container_;

/**
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.btn_;

/**
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.notificationBandal_;

/**
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.errors_;

/**
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.warnings_;

/**
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.notices_;

/** @override */
net.bluemind.ui.form.Form.Notification.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this.btn_ = goog.dom.getElement('bm-ui-form-notification-popup-btn');
  this.container_ = goog.dom.getElement('bm-ui-form-notification-popup');
  this.popup_ = new goog.ui.Popup(this.container_);
  this.popup_.setHideOnEscape(true);
  this.popup_.setAutoHide(true);
  this.popup_
      .setPosition(new goog.positioning.AnchoredViewportPosition(this.btn_, goog.positioning.Corner.BOTTOM_LEFT));
  this.notificationBandal_ = goog.dom.getElement('bm-ui-form-notification');

  this.getHandler().listen(this.btn_, goog.events.EventType.CLICK, function(e) {
    this.show_();
  });
};

/**
 * add an error message
 * 
 * @param {string | Element | Array} fields Fields concerned by this error.
 * @param {string} msg message.
 */
net.bluemind.ui.form.Form.Notification.prototype.addError = function(fields, msg) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  var data = {
    css : goog.getCssName('error'),
    msg : msg
  };

  this.add_(fields, this.errors_, data);
};

/**
 * remove a error msg
 * 
 * @param {string | Element | Array} fields Fields concerned by this warning.
 */
net.bluemind.ui.form.Form.Notification.prototype.removeError = function(fields) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }

  goog.array.forEach(fields, function(f) {
    this.errors_.remove(f.id);
  }, this);

};

/**
 * add a warning message
 * 
 * @param {string | Element | Array} fields Fields concerned by this warning.
 * @param {string} msg message.
 */
net.bluemind.ui.form.Form.Notification.prototype.addWarn = function(fields, msg) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  var data = {
    css : goog.getCssName('warn'),
    msg : msg
  };

  this.add_(fields, this.warnings_, data);
};

/**
 * remove a warning msg
 * 
 * @param {string | Element | Array} fields Fields concerned by this warning.
 */
net.bluemind.ui.form.Form.Notification.prototype.removeWarn = function(fields) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  goog.array.forEach(fields, function(f) {
    this.warnings_.remove(f.id);
  }, this);

};

/**
 * add a notice message
 * 
 * @param {string | Element | Array} fields Fields concerned by this notice.
 * @param {string} msg message.
 */
net.bluemind.ui.form.Form.Notification.prototype.addNotice = function(fields, msg) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  var data = {
    css : goog.getCssName('notice'),
    msg : msg
  };

  this.add_(fields, this.notices_, data);
};

/**
 * add a message
 * 
 * @param {string | Element | Array} fields Fields concerned by this notice.
 * @param {Array} messages messages.
 * @param {string} data data.
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.add_ = function(fields, messages, data) {
  goog.array.forEach(fields, function(f) {
    messages.set(f.id, data);
  });
};

/**
 * remove a notice msg
 * 
 * @param {string | Element | Array} fields Fields concerned by this notice.
 */
net.bluemind.ui.form.Form.Notification.prototype.removeNotice = function(fields) {
  if (goog.typeOf(fields) != 'array') {
    fields = [ fields ];
  }
  goog.array.forEach(fields, function(f) {
    this.notices_.remove(f.id);
  }, this);
};

/**
 * show error popup
 * 
 * @private
 */
net.bluemind.ui.form.Form.Notification.prototype.show_ = function() {
  this.container_.innerHTML = '';

  var formNotification = net.bluemind.ui.form.templates.formNotification;

  goog.array.forEach(this.errors_.getValues(), function(e) {
    var msg = soy.renderAsFragment(formNotification, e);
    goog.dom.appendChild(this.container_, msg);
  }, this);

  goog.array.forEach(this.warnings_.getValues(), function(e) {
    var msg = soy.renderAsFragment(formNotification, e);
    goog.dom.appendChild(this.container_, msg);
  }, this);

  goog.array.forEach(this.notices_.getValues(), function(e) {
    var msg = soy.renderAsFragment(formNotification, e);
    goog.dom.appendChild(this.container_, msg);
  }, this);

  this.popup_.setVisible(true);
};

/** @override */
net.bluemind.ui.form.Form.Notification.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var el = this.getElement();
  goog.dom.appendChild(el, soy.renderAsFragment(net.bluemind.ui.form.templates.notification));
}