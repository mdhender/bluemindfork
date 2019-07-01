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

goog.provide("net.bluemind.calendar.tasks.TasksView");

goog.require("goog.array");
goog.require("goog.soy");
goog.require("goog.style");
goog.require("goog.dom.classlist");
goog.require("goog.ui.AnimatedZippy");
goog.require("goog.ui.Component");
goog.require("goog.ui.Container");
goog.require("goog.ui.Control");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.Zippy.Events");
goog.require("goog.ui.MenuSeparator");
goog.require("goog.ui.ac.AutoComplete.EventType");
goog.require("net.bluemind.calendar.navigation.ac.CalendarAutocomplete");
goog.require("net.bluemind.calendar.tasks.templates");
goog.require("goog.events.Event");
goog.require("goog.events.EventHandler");
goog.require("net.bluemind.calendar.navigation.ui.ViewSelector");
goog.require("goog.ui.ColorPalette");
goog.require('net.bluemind.calendar.ColorPalette');
goog.require('goog.ui.Dialog');
goog.require('goog.ui.HsvPalette');
goog.require('net.bluemind.calendar.tasks.TaskView');

/**
 * View class for navigation bar.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.tasks.TasksView = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
};
goog.inherits(net.bluemind.calendar.tasks.TasksView, goog.ui.Component);

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.tasks.TasksView.prototype.ctx;

/**
 * @type {goog.dom.ViewportSizeMonitor}
 */
net.bluemind.calendar.tasks.TasksView.prototype.sizeMonitor_

/** @meaning tasks.section.late */
net.bluemind.calendar.tasks.TasksView.MSG_SECTION_LATE = goog.getMsg('Late');

/** @meaning tasks.section.today */
net.bluemind.calendar.tasks.TasksView.MSG_SECTION_TODAY = goog.getMsg('Today');

/** @meaning tasks.section.tomorrow */
net.bluemind.calendar.tasks.TasksView.MSG_SECTION_TOMORROW = goog.getMsg('Tomorrow');

/** @meaning tasks.section.week */
net.bluemind.calendar.tasks.TasksView.MSG_SECTION_WEEK = goog.getMsg('This week');

/** @meaning tasks.section.month */
net.bluemind.calendar.tasks.TasksView.MSG_SECTION_MONTH = goog.getMsg('This month');

/** @meaning tasks.section.noDueDate */
net.bluemind.calendar.tasks.TasksView.MSG_SECTION_NODUEDATE = goog.getMsg('No due date');

/** @override */
net.bluemind.calendar.tasks.TasksView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var dom = this.getDomHelper();
};

/** @override */
net.bluemind.calendar.tasks.TasksView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var dom = this.getDomHelper();

  var id = this.makeId('tasks');
  var content = dom.getElement(id);

  goog.dom.classlist.add(this.getElement(), goog.getCssName('tasks-list'));

  var c = new goog.ui.Button(this.getDomHelper().createDom(
      'div',
      [ goog.getCssName('goog-button-icon'), goog.getCssName('fa'), goog.getCssName('fa-lg'),
          goog.getCssName('fa-bars') ]), goog.ui.FlatButtonRenderer.getInstance());
  c.addClassName(goog.getCssName('toggle-button'));
  /** @meaning calendar.todos.toggle */
  var MSG_TASKS = goog.getMsg('Show/Hide todos list');
  c.setTooltip(MSG_TASKS)
  c.setId('toggle-view');
  this.addChild(c, true);
  this.getHandler().listen(c, goog.ui.Component.EventType.ACTION, function() {
    this.dispatchEvent(new goog.events.Event(net.bluemind.calendar.tasks.events.EventType.TOGGLE_VIEW));
  });

  var root = new goog.ui.Component();
  root.setId('tasks');
  this.addChild(root, true);
  this.sizeMonitor_ = new goog.dom.ViewportSizeMonitor();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.resize_);
  this.draw();
  this.resize_();
};

net.bluemind.calendar.tasks.TasksView.prototype.draw = function() {
  this.getChild('tasks').removeChildren(true);

  if (!this.getModel()) {
    return;
  }

  this.getChild('toggle-view').setChecked(this.getModel().show);
  var main = this.getDomHelper().getElement('content-body');
  if (this.getModel().show == true) {
    goog.style.setStyle(main, "margin-right", "216px");
    goog.style.setStyle(this.getDomHelper().getElement('content-right'), "width", "216px");
    var tasksBySection = this.getModel() || {};
    var empty = tasksBySection['late'].length == 0 && tasksBySection['today'].length == 0
        && tasksBySection['tomorrow'].length == 0 && tasksBySection['this-week'].length == 0
        && tasksBySection['this-month'].length == 0 && tasksBySection['no-due'].length == 0;

    if (empty) {
      goog.dom.classlist.add(this.getElement(), 'empty');
    } else {
      goog.dom.classlist.remove(this.getElement(), 'empty');
    }

    this.drawSection_(net.bluemind.calendar.tasks.TasksView.MSG_SECTION_LATE, tasksBySection['late']);
    this.drawSection_(net.bluemind.calendar.tasks.TasksView.MSG_SECTION_TODAY, tasksBySection['today']);
    this.drawSection_(net.bluemind.calendar.tasks.TasksView.MSG_SECTION_TOMORROW, tasksBySection['tomorrow']);
    this.drawSection_(net.bluemind.calendar.tasks.TasksView.MSG_SECTION_WEEK, tasksBySection['this-week']);
    this.drawSection_(net.bluemind.calendar.tasks.TasksView.MSG_SECTION_MONTH, tasksBySection['this-month']);
    this.drawSection_(net.bluemind.calendar.tasks.TasksView.MSG_SECTION_NODUEDATE, tasksBySection['no-due']);
  } else {
    goog.style.setStyle(main, "margin-right", "0px");
    goog.style.setStyle(this.getDomHelper().getElement('content-right'), "width", "0px");
  }
};

net.bluemind.calendar.tasks.TasksView.prototype.refresh = function() {
  this.draw();
}

net.bluemind.calendar.tasks.TasksView.prototype.drawSection_ = function(title, tasks) {
  if (!tasks || tasks.length == 0) {
    return;
  }

  var section = new goog.ui.Component();
  this.getChild('tasks').addChild(section, true);
  section.getElement().innerHTML = net.bluemind.calendar.tasks.templates.sectionView({
    title : title
  });

  goog.array.forEach(tasks, function(t) {
    var taskView = new net.bluemind.calendar.tasks.TaskView();
    taskView.setModel(t);
    section.addChild(taskView, true);
  }, this);

}

net.bluemind.calendar.tasks.TasksView.prototype.resize_ = function() {
  var dom = this.getDomHelper();
  var grid = this.getElement();
  var elSize = goog.style.getClientPosition(grid);
  var size = this.sizeMonitor_.getSize();
  var height = size.height - elSize.y - 3;
  grid.style.height = height + 'px';
};
