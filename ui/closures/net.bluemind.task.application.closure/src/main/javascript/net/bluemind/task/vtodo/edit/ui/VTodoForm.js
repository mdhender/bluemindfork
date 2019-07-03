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
goog.provide("net.bluemind.task.vtodo.edit.ui.VTodoForm");

goog.require("net.bluemind.todolist.api.i18n.Priority.Caption");
goog.require("net.bluemind.todolist.api.i18n.Status.Caption");
goog.require("net.bluemind.ui.form.DateField");
goog.require("net.bluemind.ui.form.Form");
goog.require("net.bluemind.ui.form.ReminderField");
goog.require("net.bluemind.ui.form.RichTextField");
goog.require("net.bluemind.ui.form.SelectField");
goog.require("net.bluemind.ui.form.TagField");
goog.require("net.bluemind.ui.form.TextField");
goog.require('goog.style');
goog.require('goog.dom');

/**
 * Task Form ui.
 * 
 * @param {Object} model Default model
 * @param {net.bluemind.i18n.DateHelper.Formatter} formatter Date formatter.
 * @param {net.bluemind.i18n.DateHelper.Parser} parser Date parser.
 * 
 * @constructor
 * @extends {net.bluemind.ui.form.Form}
 */
net.bluemind.task.vtodo.edit.ui.VTodoForm = function(ctx) {
  goog.base(this, formatter, parser);

  this.ctx = ctx;
  var formatter = ctx.helper('dateformat').formatter;
  var parser = ctx.helper('dateformat').parser;

  var headerDiv = new goog.ui.Component();
  headerDiv.setId('headerDiv');
  
  /** @meaning tasks.form.summary */
  var MSG_SUMMARY = goog.getMsg('Summary');
  var child = new net.bluemind.ui.form.TextField(MSG_SUMMARY);
  child.setId('summary');
  headerDiv.addChild(child, true);

  /** @meaning tasks.form.dtstart */
  var MSG_DTSTART = goog.getMsg('Date start');
  child = new net.bluemind.ui.form.DateField(MSG_DTSTART, formatter.date, parser.date);
  child.setId('dtstart');
  child.addClassName(goog.getCssName('inline'));
  headerDiv.addChild(child, true);

  /** @meaning tasks.form.dueDate */
  var MSG_DUE = goog.getMsg('Due date');
  child = new net.bluemind.ui.form.DateField(MSG_DUE, formatter.date, parser.date);
  child.setId('due');
  child.addClassName(goog.getCssName('inline'));
  headerDiv.addChild(child, true);

  /** @meaning tasks.form.priority */
  var MSG_PRIORITY = goog.getMsg('Priority');
  child = new net.bluemind.ui.form.SelectField(MSG_PRIORITY, net.bluemind.todolist.api.i18n.Priority.Caption);
  child.setId('priority');
  headerDiv.addChild(child, true);

  /** @meaning tasks.form.state */
  var MSG_STATE = goog.getMsg('State');
  child = new net.bluemind.ui.form.SelectField(MSG_STATE, net.bluemind.todolist.api.i18n.Status.Caption);
  child.setId('status');
  headerDiv.addChild(child, true);

  this.addChild(headerDiv, true);
  goog.dom.classlist.add(headerDiv.getElement(), goog.getCssName('task-headerbg'));

  /** @meaning tasks.form.percentCompleted */
  var MSG_PERCENT_COMPLETE = goog.getMsg('% Complete')
  child = new net.bluemind.ui.form.TextField(MSG_PERCENT_COMPLETE)
  child.setId('percent');
  child.addClassName(goog.getCssName('inline'));
  this.addChild(child, true);

  /** @meaning tasks.form.completionDate */
  var MSG_COMPLETED = goog.getMsg('Completion date');
  child = new net.bluemind.ui.form.DateField(MSG_COMPLETED, formatter.date, parser.date)
  child.setId('completed');
  child.addClassName(goog.getCssName('inline'));
  this.addChild(child, true);

  /** @meaning tasks.form.location */
  var MSG_LOCATION = goog.getMsg('Location');
  child = new net.bluemind.ui.form.TextField(MSG_LOCATION);
  child.setId('location');
  this.addChild(child, true);

  /** @meaning tasks.form.tags */
  var MSG_TAG = goog.getMsg('Tags');
  child = new net.bluemind.ui.form.TagField(MSG_TAG);
  child.setId('tags');
  this.addChild(child, true);

  /** @meaning tasks.form.description */
  var MSG_DESCRIPTION = goog.getMsg('Description');
  child = new net.bluemind.ui.form.RichTextField(MSG_DESCRIPTION);
  child.setId('description');
  this.addChild(child, true);

  /** @meaning tasks.form.reminder */
  var MSG_REMINDER = goog.getMsg('Reminder');
  child = new net.bluemind.ui.form.ReminderField(MSG_REMINDER);
  child.setId('reminder');
  this.addChild(child, true);

};
goog.inherits(net.bluemind.task.vtodo.edit.ui.VTodoForm, net.bluemind.ui.form.Form);

/** @override */
net.bluemind.task.vtodo.edit.ui.VTodoForm.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  this.getChild('headerDiv').getChild('summary').setValue(model.summary);
  this.getChild('headerDiv').getChild('dtstart').setValue(model.start);
  this.getChild('headerDiv').getChild('due').setValue(model.due);
  this.getChild('headerDiv').getChild('priority').setValue(model.priority);
  this.getChild('headerDiv').getChild('status').setValue(model.status);
  this.getChild('percent').setValue(model.percent);
  this.getChild('completed').setValue(model.completed);
  this.getChild('location').setValue(model.location);
  this.getChild('tags').setValue(model.tags);
  this.getChild('description').setValue(model.description);
  this.getChild('reminder').setValue(model.alarm);

  if (model.errors) {
    var notif = this.getChild('notifications');
    goog.array.forEach(model.errors, function(e) {
      notif.addError(e.property, e.msg);
    }, this);
    this.getChild('notifications').show_();
  }

  this.getChild('headerDiv').getChild('summary').focus();
};

/** @override */
net.bluemind.task.vtodo.edit.ui.VTodoForm.prototype.createDom = function() {
  goog.base(this, 'createDom');
};

/**
 * Refresh model with data from the form
 * 
 * @private
 */
net.bluemind.task.vtodo.edit.ui.VTodoForm.prototype.getModel = function() {
  var model = goog.base(this, 'getModel') || {};
  model.summary = this.getChild('headerDiv').getChild('summary').getValue();
  model.start = this.getChild('headerDiv').getChild('dtstart').getValue();
  model.due = this.getChild('headerDiv').getChild('due').getValue();
  model.priority = this.getChild('headerDiv').getChild('priority').getValue();
  model.status = this.getChild('headerDiv').getChild('status').getValue();
  model.percent = this.getChild('percent').getValue();
  model.completed = this.getChild('completed').getValue();
  model.location = this.getChild('location').getValue();
  model.tags = this.getChild('tags').getValue();
  model.description = this.getChild('description').getValue();
  model.alarm = this.getChild('reminder').getValue();
  return model;
};

net.bluemind.task.vtodo.edit.ui.VTodoForm.prototype.setTags = function(tags) {
  this.getChild('tags').setTags(tags);
}
