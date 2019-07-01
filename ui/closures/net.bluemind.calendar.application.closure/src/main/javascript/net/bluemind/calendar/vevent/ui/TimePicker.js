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
 * @fileoverview Timepicker componnent.
 */

goog.provide("net.bluemind.calendar.vevent.ui.TimePicker");
goog.provide("net.bluemind.calendar.vevent.ui.TimePicker.EventType");

goog.require("goog.array");
goog.require("goog.string");
goog.require("goog.style");
goog.require("goog.date.Interval");
goog.require("goog.date.UtcDateTime");
goog.require("goog.dom.classes");
goog.require("goog.events.EventType");
goog.require("goog.structs.Map");
goog.require("goog.ui.Component");
goog.require("goog.ui.Container");
goog.require("goog.ui.Control");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.date.DateTime");

/**
 * TimePicker widget.
 * 
 * @param {goog.i18n.DateTimeFormat} format Time formatter
 * @param {goog.i18n.DateTimeParse} parse Time parser
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.vevent.ui.TimePicker = function(format, parse, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.format_ = format;
  this.parse_ = parse;
  this.input_ = new goog.ui.LabelInput();
  this.addChild(this.input_);
  this.container_ = new goog.ui.Container();
  this.addChild(this.container_);
};
goog.inherits(net.bluemind.calendar.vevent.ui.TimePicker, goog.ui.Component);

/**
 * Handler
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.handler_;

/**
 * Input element
 * 
 * @type {goog.ui.LabelInput} input_
 * @private
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.input_;

/**
 * Time container
 * 
 * @type {goog.ui.Container} container_
 * @private
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.container_;

/**
 * times Controls
 * 
 * @type {goog.structs.Map} time value.
 * @private
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.times_;

/**
 * @private
 * @type {goog.i18n.DateTimeFormat}
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.format_;

/**
 * @private
 * @type {goog.i18n.DateTimeParse}
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.parse_;

/** @override */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.createDom = function() {
  goog.base(this, 'createDom');
  this.decorateInternal(this.getElement());
};

/** @override */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.decorateInternal = function(el) {
  goog.base(this, 'decorateInternal', el)

  this.input_.render(el);

  this.container_.render(el);

  this.times_ = new goog.structs.Map();

  var interval = new goog.date.Interval(goog.date.Interval.MINUTES, 30);
  var date = new goog.date.UtcDateTime(1970);
  for (var t = date.clone(); t.getDay() == date.getDay(); t.add(interval)) {
    var time = this.format_.format(t);
    var ctrl = new goog.ui.Control(time);
    this.times_.set(time, ctrl);
    this.container_.addChild(ctrl, true);
  }

  this.container_.setVisible(false);
  goog.dom.classes.add(el, goog.getCssName('timepicker'));
};

/** @override */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument')

  var eh = this.getHandler();

  var inputEl = this.input_.getElement();

  eh.listen(inputEl, goog.events.EventType.CLICK, function(e) {
    this.container_.setVisible(true);

    var v = this.input_.getValue();
    if (this.times_.containsKey(v)) {
      this.times_.get(v).setHighlighted(true);
      var idx = goog.array.indexOf(this.times_.getKeys(), v);
      this.container_.getElement().scrollTop = idx * 20;
    } else {
      var sp = v.split(':');
      var h = sp[0];
      var idx = 0;
      goog.array.forEach(this.times_.getValues(), function(e) {
        if (goog.string.startsWith(e.getContent(), h)) {
          e.setHighlighted(true);
          this.container_.getElement().scrollTop = idx * 20;
        }
        idx++;
      }, this);
    }
    e.stopPropagation();
  });

  eh.listen(inputEl, goog.events.EventType.KEYDOWN, function(e) {
    this.container_.setVisible(false);
    e.stopPropagation();
  });

  eh.listen(inputEl, goog.events.EventType.CHANGE, function(e) {
    this.checkTime_();
    this.dispatchEvent(net.bluemind.calendar.vevent.ui.TimePicker.EventType.UPDATE);
    e.stopPropagation();
  });

  eh.listen(this.container_, goog.ui.Component.EventType.ACTION, function(e) {
    var i = e.target;
    this.input_.setValue(i.getContent());
    this.container_.setVisible(false);
    this.checkTime_();
    this.dispatchEvent(net.bluemind.calendar.vevent.ui.TimePicker.EventType.UPDATE);
    e.stopPropagation();
  });

  eh.listen(this.dom_.getDocument(), goog.events.EventType.CLICK, function(e) {
    this.container_.setVisible(false);
    e.stopPropagation();
  });

};

net.bluemind.calendar.vevent.ui.TimePicker.prototype.getDefaultBegin = function(opt_max) {
  var dtstart = new net.bluemind.date.DateTime();
  if ((dtstart.getHours() + 2) > opt_max) {
    dtstart.setHours(opt_max);
  } else {
    dtstart.add(new goog.date.Interval(0, 0, 0, 2));
  }
  dtstart.setMinutes(0);
  dtstart.setSeconds(0);
  return dtstart;
};
/**
 * checkTime
 * 
 * @private
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.checkTime_ = function() {
  var v = this.getValue();
  if (v == null || v == '') {
    // FIXME
    var d = this.getDefaultBegin();
    this.setValue(this.format_.format(d));
  } else if (!this.parse_.strictParse(v, new net.bluemind.date.DateTime())) {
    var time = v.match(/(\d+)(:(\d\d))?\s*(p?)/i);
    var h = parseInt(time[1], 10);
    var m = parseInt(time[3], 10);
    var fix = false;
    if (isNaN(h)) {
      h = 0;
    }
    if (isNaN(m)) {
      m = 0;
    }
    var d = new net.bluemind.date.DateTime();
    d.setHours(h);
    d.setMinutes(m);
    this.setValue(this.format_.format(d));
  }
};

/** @override */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.disposeInternal = function() {
  goog.base(this, 'disposeInternal');
  this.input_ = null;
  this.container_ = null;
  this.times_ = null;
};

/**
 * get input element
 * 
 * @return {Object} input element.
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.getInputElement = function() {
  if (!this.input_.getElement()) {
    this.input_.createDom();
  }
  return this.input_.getElement();
};

/**
 * get text input value.
 * 
 * @return {text} input time value.
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.getValue = function() {
  if (!this.input_.getElement()) {
    this.input_.createDom();
  }
  return this.input_.getValue();
};

/**
 * Set text input value.
 * 
 * @param {text} v time value.
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.setValue = function(v) {
  if (!this.input_.getElement()) {
    this.input_.createDom();
  }
  this.input_.setValue(v);
};

/**
 * Set text input value.
 * 
 * @param {text} v time value.
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.setTime = function(date) {
  this.setValue(this.format_.format(date));
  this.dispatchEvent(net.bluemind.calendar.vevent.ui.TimePicker.EventType.UPDATE);
};

/**
 * Shows or hides the element.
 * 
 * @param {Element} element Element to update.
 * @param {boolean} visible Whether to show the element.
 */
net.bluemind.calendar.vevent.ui.TimePicker.prototype.setVisible = function(visible) {
  goog.style.setElementShown(this.getElement(), visible);
};

/**
 * Constants for event names.
 * 
 * @enum {string}
 */
net.bluemind.calendar.vevent.ui.TimePicker.EventType = {
  UPDATE : 'update'
};
