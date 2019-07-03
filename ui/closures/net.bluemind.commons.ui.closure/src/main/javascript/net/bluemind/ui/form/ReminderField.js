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

/**
 * @fileoverview Reminder field.
 */

goog.provide("net.bluemind.ui.form.ReminderField");

goog.require("goog.array");
goog.require("goog.string");
goog.require("goog.ui.Button");
goog.require("goog.ui.Control");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.LinkButtonRenderer");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Select");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.form.FormField");
goog.require("bluemind.ui.style.TrashButtonRenderer");// FIXME - unresolved
// required symbol

/**
 * Reminder field.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM hepler, used for
 *          document interaction.
 * @constructor
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.ReminderField = function(label, options, opt_renderer, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('field-reminder'));
  /** @meaning commons.addReminder */
  var MSG_ADD_REMINDER = goog.getMsg('Add a reminder');

};
goog.inherits(net.bluemind.ui.form.ReminderField, net.bluemind.ui.form.FormField);
/**
 * Duration constants
 */
net.bluemind.ui.form.ReminderField.DURATION_DAY = 86400;
net.bluemind.ui.form.ReminderField.DURATION_HOUR = 3600;
net.bluemind.ui.form.ReminderField.DURATION_MINUTE = 60;

/** @override */
net.bluemind.ui.form.ReminderField.prototype.createField = function() {
  /** @meaning commons.addReminder */
  var MSG_ADD_REMINDER = goog.getMsg('Add reminder');
  var control = new goog.ui.Button(MSG_ADD_REMINDER, goog.ui.LinkButtonRenderer.getInstance());
  control.setId('add');
  this.addChild(control, true);
};

/** @override */
net.bluemind.ui.form.ReminderField.prototype.getContentElement = function() {
  return this.getElementByClass(goog.getCssName('field-base-field'))
};

/** @override */
net.bluemind.ui.form.ReminderField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('add'), goog.ui.Component.EventType.ACTION, this.addReminderInternal_);
};

/**
 * On reminder button clicked
 * 
 * @param {goog.events.Event} e Dispatched event
 */
net.bluemind.ui.form.ReminderField.prototype.addReminderInternal_ = function(e) {
  this.addReminder({
    trigger : 600,
    action : 'Email'
  });
};

/**
 * 
 * @private
 * @param {integer} duration Dispatched event
 */
net.bluemind.ui.form.ReminderField.prototype.durationToValue_ = function(duration) {
  if (duration <= 0)
    return;
  var index = 1;
  var value = 60;
  if (duration % net.bluemind.ui.form.ReminderField.DURATION_DAY == 0) {
    value = duration / net.bluemind.ui.form.ReminderField.DURATION_DAY;
    index = 3;
  } else if (duration % net.bluemind.ui.form.ReminderField.DURATION_HOUR == 0) {
    value = duration / net.bluemind.ui.form.ReminderField.DURATION_HOUR;
    index = 2;
  } else if (duration % net.bluemind.ui.form.ReminderField.DURATION_MINUTE == 0) {
    value = duration / net.bluemind.ui.form.ReminderField.DURATION_MINUTE;
  } else {
    value = duration;
    index = 0;
  }
  return {
    select : index,
    text : value
  };
};

/**
 * Add a reminder
 * 
 * @param {{duration:number, action:string}} value Reminder in seconds
 */
net.bluemind.ui.form.ReminderField.prototype.addReminder = function(value) {

  var duration = this.durationToValue_(value.trigger);
  if (!duration) {
  	// BUGFIX BM-9502
    return;
  }

  var container = new goog.ui.Control();
  container.setHandleMouseEvents(false);
  container.setSupportedState(goog.ui.Component.State.FOCUSED, false);
  container.addClassName(goog.getCssName('field-base'));
  this.addChildAt(container, this.getChildCount() - 1, true);

  var control = new goog.ui.LabelInput();
  control.setId('field');
  container.addChild(control, true);
  control.setValue("" + duration.text);

  control = new goog.ui.Select();
  control.addClassName(goog.getCssName('goog-button-base'));
  control.addClassName(goog.getCssName('goog-select'));
  /** @meaning commons.time.seconds */
  var MSG_SECONDS = goog.getMsg('seconds');
  control.addItem(new goog.ui.MenuItem(MSG_SECONDS, 1));
  /** @meaning commons.time.minutes */
  var MSG_MINUTES = goog.getMsg('minutes');
  control.addItem(new goog.ui.MenuItem(MSG_MINUTES, 60));
  /** @meaning commons.time.hours */
  var MSG_HOURS = goog.getMsg('hours');
  control.addItem(new goog.ui.MenuItem(MSG_HOURS, 3600));
  /** @meaning commons.time.days */
  var MSG_DAYS = goog.getMsg('days');
  control.addItem(new goog.ui.MenuItem(MSG_DAYS, 86400));
  control.setId('unit');
  container.addChild(control, true);
  control.setSelectedIndex(duration.select);

  var trash = new goog.ui.Button(" ", bluemind.ui.style.TrashButtonRenderer.getInstance());
  container.addChild(trash, true);
  this.getHandler().listen(trash, goog.ui.Component.EventType.ACTION, function() {
    this.removeChild(container, true).dispose();
  });

};

/**
 * Reset value
 * 
 * @protected
 */
net.bluemind.ui.form.ReminderField.prototype.resetValue = function() {
  var children = this.removeChildren(true);
  goog.array.forEach(children, function(child) {
    if (child.getId() == 'add') {
      this.addChild(child, true)
    } else {
      child.dispose();
    }
  }, this);
};

/** @override */
net.bluemind.ui.form.ReminderField.prototype.setValue = function(values) {
  this.resetValue();
  values = values || [];
  goog.array.forEach(values, function(value) {
    this.addReminder(value);
  }, this);
};

/** @override */
net.bluemind.ui.form.ReminderField.prototype.getValue = function() {
  var value = [];
  this.forEachChild(function(child) {
    if (child.getChild('field') && child.getChild('unit')) {
      var trigger = goog.string.toNumber(child.getChild('field').getValue()) * child.getChild('unit').getValue();
      value.push({
        trigger : trigger,
        action : 'Email'
      });
    }
  }, this);
  return value;
};
