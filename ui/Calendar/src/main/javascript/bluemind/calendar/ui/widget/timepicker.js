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


goog.provide('bluemind.calendar.ui.widget.TimePicker');

goog.require('goog.events.EventType');
goog.require('goog.ui.Container');
goog.require('goog.ui.LabelInput');


/**
 * TimePicker widget.
 *
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
bluemind.calendar.ui.widget.TimePicker = function(opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.input_ = new goog.ui.LabelInput();
  this.addChild(this.input_);
  this.input_.createDom();
  this.container_ = new goog.ui.Container();
  this.addChild(this.container_);
};
goog.inherits(bluemind.calendar.ui.widget.TimePicker, goog.ui.Component);

/**
 * Handler
 * @private
 */
bluemind.calendar.ui.widget.TimePicker.prototype.handler_;

/**
 * Input element
 * @type {goog.ui.LabelInput} input_
 * @private
 */
bluemind.calendar.ui.widget.TimePicker.prototype.input_;

/**
 * Time container
 * @type {goog.ui.Container} container_
 * @private
 */
bluemind.calendar.ui.widget.TimePicker.prototype.container_;

/**
 * times Controls
 * @type {goog.structs.Map} time value.
 * @private
 */
bluemind.calendar.ui.widget.TimePicker.prototype.times_;


/** @override */
bluemind.calendar.ui.widget.TimePicker.prototype.createDom = function() {
  bluemind.calendar.ui.widget.TimePicker.superClass_.createDom.call(this);
  this.decorateInternal(this.getElement());
};

/** @override */
bluemind.calendar.ui.widget.TimePicker.prototype.decorateInternal =
  function(el) {
  bluemind.calendar.ui.widget.TimePicker.superClass_.decorateInternal.call(
    this, el);


  this.input_.render(el);

  
  this.container_.render(el);

  this.times_ = new goog.structs.Map();

  var interval = new goog.date.Interval(goog.date.Interval.MINUTES, 30);
  var date = new goog.date.UtcDateTime(1970);
  for (var t = date.clone(); t.getDay() == date.getDay(); t.add(interval)) {
    var time = bluemind.i18n.DateTimeHelper.getInstance().formatTime(t);
    var ctrl = new goog.ui.Control(time);
    this.times_.set(time, ctrl);
    this.container_.addChild(ctrl, true);
  }

  this.container_.setVisible(false);
  goog.dom.classes.add(el, goog.getCssName('timepicker'));
};


/** @override */
bluemind.calendar.ui.widget.TimePicker.prototype.enterDocument = function() {
  bluemind.calendar.ui.widget.TimePicker.superClass_.enterDocument.call(this);

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
    this.dispatchEvent(bluemind.calendar.ui.widget.TimePickerEventType.UPDATE);
    e.stopPropagation();
  });

  eh.listen(this.container_, goog.ui.Component.EventType.ACTION, function(e) {
    var i = e.target;
    this.input_.setValue(i.getContent());
    this.container_.setVisible(false);
    this.checkTime_();
    this.dispatchEvent(bluemind.calendar.ui.widget.TimePickerEventType.UPDATE);
    e.stopPropagation();
  });

  eh.listen(this.dom_.getDocument(), goog.events.EventType.CLICK, function(e) {
    this.container_.setVisible(false);
    e.stopPropagation();
  });

};

/**
 * checkTime
 * @private
 */
bluemind.calendar.ui.widget.TimePicker.prototype.checkTime_ = function() {
  var v = this.input_.getValue();
  if (v == null || v == '') {
    var d = bluemind.calendar.view.forms.Event.getDefaultBegin();
    this.input_.setValue(
      bluemind.i18n.DateTimeHelper.getInstance().formatTime(d));
  } else if (!bluemind.i18n.DateTimeHelper.getInstance().parseTime(v)) {
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
    var d = new goog.date.DateTime();
    d.setHours(h);
    d.setMinutes(m);
    this.input_.setValue(
      bluemind.i18n.DateTimeHelper.getInstance().formatTime(d));
  }
};

/** @override */
bluemind.calendar.ui.widget.TimePicker.prototype.disposeInternal = function() {
  bluemind.calendar.ui.widget.TimePicker.superClass_.disposeInternal.call(this);
  this.input_ = null;
  this.container_ = null;
  this.times_ = null;
};

/**
 * get input element
 * @return {Object} input element.
 */
bluemind.calendar.ui.widget.TimePicker.prototype.getInputElement = function() {
  return this.input_.getElement();
};

/**
 * get text input value.
 * @return {text} input time value.
 */
bluemind.calendar.ui.widget.TimePicker.prototype.getValue = function() {
  return this.input_.getValue();
};

/**
 * Set text input value.
 * @param {text} v time value.
 */
bluemind.calendar.ui.widget.TimePicker.prototype.setValue = function(v) {
  this.input_.setValue(v);
};

/**
 * Constants for event names.
 * @enum {string}
 */
bluemind.calendar.ui.widget.TimePickerEventType = {
  UPDATE: 'update'
};
