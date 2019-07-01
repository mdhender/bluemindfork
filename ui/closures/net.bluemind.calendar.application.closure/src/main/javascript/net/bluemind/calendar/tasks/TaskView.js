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
 * @fileoverview View class for application header (Logo + logo).
 */

goog.provide("net.bluemind.calendar.tasks.TaskView");

goog.provide("net.bluemind.calendar.tasks.events.ToggleStatusEvent");
goog.provide("net.bluemind.calendar.tasks.events.DeleteEvent");
goog.provide("net.bluemind.calendar.tasks.events.EventType");
goog.require("goog.array");
goog.require("goog.soy");
goog.require("goog.ui.Component");
goog.require("net.bluemind.calendar.tasks.templates");
goog.require('goog.ui.LabelInput');
goog.require('goog.ui.Checkbox');
goog.require('goog.events.FocusHandler');
goog.require('bluemind.ui.style.TrashButtonRenderer');

/**
 * View class for navigation bar.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.tasks.TaskView = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(net.bluemind.calendar.tasks.TaskView, goog.ui.Component);

/** @override */
net.bluemind.calendar.tasks.TaskView.prototype.createDom = function() {
  goog.base(this, 'createDom');

  var markAsDone = new goog.ui.Checkbox();
  markAsDone.setId('markAsDone');
  markAsDone.addClassName('markAsDone');
  this.addChild(markAsDone, true);

  var todolistFlag = new goog.ui.Component();
  todolistFlag.setId('color');
  this.addChild(todolistFlag, true);
  goog.dom.classlist.add(todolistFlag.getElement(), 'todolist-color');
  var label = new goog.ui.LabelInput();
  label.setId('summary');
  this.addChild(label, true);
  goog.dom.classlist.add(label.getElement(), 'td-summary');

  var trash = new goog.ui.Button(" ", bluemind.ui.style.TrashButtonRenderer
    .getInstance());
  trash.setId('trash');
  this.addChild(trash, true);

  goog.dom.classlist.add(this.getElement(), goog.getCssName('todolist-entry'));
};

/** @override */
net.bluemind.calendar.tasks.TaskView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  this
    .getHandler()
    .listen(
      this.getChild('markAsDone'),
      goog.ui.Component.EventType.ACTION,
      function() {
        this
          .dispatchEvent(new net.bluemind.calendar.tasks.events.ToggleStatusEvent(
            this.getModel().container, this.getModel().id));
      });

  this.fHandler_ = new goog.events.FocusHandler(this.getChild('summary')
    .getElement());


  this.getHandler().listen(
    this.fHandler_,
    goog.events.FocusHandler.EventType.FOCUSOUT,
    function() {
      this.updateSummary_();
    });

  this.getHandler().listen(
      this.getChild('summary').getElement(),
      goog.events.EventType.KEYPRESS,
    function(e) {
        console.log("keystroke",e);
      if (e.keyCode != 13) {
        return;
      }
      this.updateSummary_();
    });

  this.getHandler().listen(
    this.getChild('trash'),
    goog.ui.Component.EventType.ACTION,
    function() {
      this.dispatchEvent(new net.bluemind.calendar.tasks.events.DeleteEvent(
        this.getModel().container, this.getModel().id));
    });

  this.refresh();
};

net.bluemind.calendar.tasks.TaskView.prototype.updateSummary_ = function() {
  var value = this.getChild('summary').getValue();
  if (value != this.getModel().summary) {
    this.dispatchEvent(new net.bluemind.calendar.tasks.events.SummaryEvent(
      this.getModel().container, this.getModel().id, value));
  }
}
net.bluemind.calendar.tasks.TaskView.prototype.exitDocument = function() {
  goog.base(this, 'exitDocument');

  this.fHandler_.dispose();
  this.fHandler_ = null;
};

/** @override */
net.bluemind.calendar.tasks.TaskView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  this.refresh();
}

/**
 * 
 */
net.bluemind.calendar.tasks.TaskView.prototype.refresh = function() {
  if (this.getModel() && this.isInDocument()) {
    this.getChild('summary').setValue(this.getModel().summary);
    goog.style.setStyle(this.getChild('color').getElement(),
      'background-color', this.getModel().color);
    this.getChild('markAsDone').setVisible(this.getModel()['writable']);
    this.getChild('trash').setVisible(this.getModel()['writable']);
    this.getChild('summary').setEnabled(this.getModel()['writable']);
    goog.style.setStyle(this.getChild('summary').getElement(),
      'background-color', "white");

  }
}

/**
 * Object representing a toggle task status event.
 * 
 * @param {string} containerUid
 * @param {string} uid
 * @extends {goog.events.Event}
 * @constructor
 */
net.bluemind.calendar.tasks.events.ToggleStatusEvent = function(containerUid,
  uid) {
  goog.base(this, net.bluemind.calendar.tasks.events.EventType.TOGGLE_STATUS);
  this.containerUid = containerUid;
  this.uid = uid;
};
goog.inherits(net.bluemind.calendar.tasks.events.ToggleStatusEvent,
  goog.events.Event);

/**
 * @type {string}
 */
net.bluemind.calendar.tasks.events.ToggleStatusEvent.prototype.containerUid;

/**
 * @type {string}
 */
net.bluemind.calendar.tasks.events.ToggleStatusEvent.prototype.uid;

/**
 * Object representing a toggle task status event.
 * 
 * @param {string} containerUid
 * @param {string} uid
 * @extends {goog.events.Event}
 * @constructor
 */
net.bluemind.calendar.tasks.events.DeleteEvent = function(containerUid, uid) {
  goog.base(this, net.bluemind.calendar.tasks.events.EventType.DELETE);
  this.containerUid = containerUid;
  this.uid = uid;
};
goog
  .inherits(net.bluemind.calendar.tasks.events.DeleteEvent, goog.events.Event);

/**
 * @type {string}
 */
net.bluemind.calendar.tasks.events.DeleteEvent.prototype.containerUid;

/**
 * @type {string}
 */
net.bluemind.calendar.tasks.events.DeleteEvent.prototype.uid;

/**
 * Object representing a toggle task status event.
 * 
 * @param {string} containerUid
 * @param {string} uid
 * @extends {goog.events.Event}
 * @constructor
 */
net.bluemind.calendar.tasks.events.SummaryEvent = function(containerUid, uid,
  text) {
  goog.base(this, net.bluemind.calendar.tasks.events.EventType.SUMMARY_CHANGE);
  this.containerUid = containerUid;
  this.uid = uid;
  this.text = text;
};
goog.inherits(net.bluemind.calendar.tasks.events.SummaryEvent,
  goog.events.Event);

/**
 * @type {string}
 */
net.bluemind.calendar.tasks.events.SummaryEvent.prototype.containerUid;

/**
 * @type {string}
 */
net.bluemind.calendar.tasks.events.SummaryEvent.prototype.uid;
/**
 * @type {string}
 */
net.bluemind.calendar.tasks.events.SummaryEvent.prototype.text;

/** @enum {string} */
net.bluemind.calendar.tasks.events.EventType = {
  TOGGLE_STATUS : goog.events.getUniqueId('toggle-status'),
  DELETE : goog.events.getUniqueId('delete'),
  SUMMARY_CHANGE : goog.events.getUniqueId('summary-change'),
  TOGGLE_VIEW : goog.events.getUniqueId('toggle-view')
};
