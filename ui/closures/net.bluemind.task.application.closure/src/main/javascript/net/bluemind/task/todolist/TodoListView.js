/* BEGIN LICENSE
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

goog.provide("net.bluemind.task.todolist.TodoListView");
goog.provide("net.bluemind.task.todolist.TodoListView.EventType");

goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.dom.ViewportSizeMonitor");
goog.require("goog.events.EventType");
goog.require("goog.net.EventType");
goog.require("goog.net.IframeIo");
goog.require("goog.positioning.Corner");
goog.require("goog.ui.Button");
goog.require("goog.ui.Component");
goog.require("goog.ui.Control");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.PopupMenu");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("goog.ui.Dialog.ButtonSet");
goog.require("goog.ui.Dialog.EventType");
goog.require("goog.ui.Dialog.ButtonSet.DefaultButtons");
goog.require("goog.ui.style.app.ButtonRenderer");
goog.require("net.bluemind.commons.ui.TaskProgressDialog");
goog.require("net.bluemind.task.EventType");// FIXME - unresolved required
// symbol
goog.require("net.bluemind.task.todolist.templates");// FIXME - unresolved
// required symbol
goog.require("net.bluemind.task.todolist.ui.ListTask");
goog.require("net.bluemind.ui.List");
goog.require("net.bluemind.ui.SubList");
/**
 * @constructor
 * 
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.task.todolist.TodoListView = function(ctx) {
  goog.ui.Component.call(this);

  this.ctx_ = ctx;

  // Todo list
  var todos = new net.bluemind.ui.List(null, this.getDomHelper());
  todos.setId('todos');
  this.addChild(todos);

  // Action toolbar
  var toolbar = new goog.ui.Toolbar();
  toolbar.setId('toolbar');
  this.addChild(toolbar);

  var menuButton = new goog.ui.Button(this.getDomHelper().createDom('div',
      [ goog.getCssName('goog-button-icon'), goog.getCssName('fa'), goog.getCssName('fa-cogs') ]),
      goog.ui.style.app.ButtonRenderer.getInstance());
  menuButton.setId('menuButton');
  toolbar.addChild(menuButton, true);

  var dom = this.getDomHelper();

  /** @meaning tasks.ical.export */
  var MSG_EXPORT_ICS = goog.getMsg('Export to iCal');
  var content = dom.createDom('div', null, dom.createDom('span',
      [ goog.getCssName('fa'), goog.getCssName('fa-upload') ]), '\u00A0', MSG_EXPORT_ICS);
  var xport = new goog.ui.MenuItem(content);
  xport.setId('export');

  /** @meaning tasks.ical.import */
  var MSG_IMPORT_ICS = goog.getMsg('Import from iCal');
  content = dom.createDom('div', null,
      dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-download') ]), '\u00A0', MSG_IMPORT_ICS);
  var mport = new goog.ui.MenuItem(content);
  mport.setId('import');

  /** @meaning general.rename */
  var MSG_RENAME_TODOLIST = goog.getMsg('Rename');
  content = dom.createDom('div', null, dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-pencil') ]),
      '\u00A0', MSG_RENAME_TODOLIST);
  var rename = new goog.ui.MenuItem(content);
  rename.setId('rename');

  /** @meaning general.remove */
  var MSG_RM_TODOLIST = goog.getMsg('Remove');
  content = dom.createDom('div', null, dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-trash-o') ]),
      '\u00A0', MSG_RM_TODOLIST);
  var rm = new goog.ui.MenuItem(content);
  rm.setId('remove');

  var menu = new goog.ui.PopupMenu();
  menu.setId('menu');
  this.addChild(menu);
  menu.setToggleMode(true);
  menu.addChild(xport, true);
  menu.addChild(mport, true);
  menu.addChild(rename, true);
  menu.addChild(rm, true);
  menu.render(this.getDomHelper().getDocument().body);
  menu.attach(menuButton.getElement(), goog.positioning.Corner.BOTTOM_LEFT, goog.positioning.Corner.TOP_LEFT);

  /** @meaning task.tasks */
  var MSG_TASKS = goog.getMsg('Tasks');
  toolbar.addChild(new goog.ui.Control(MSG_TASKS), true);

  // Import dialog
  /** @meaning general.selectFile */
  var MSG_SELECT_FILE = goog.getMsg('Select file');
  /** @meaning general.import */
  var MSG_IMPORT = goog.getMsg('Import');
  var dialog = new goog.ui.Dialog();
  dialog.setId('import-dialog');
  dialog.setTitle(MSG_SELECT_FILE);
  dialog.setContent(net.bluemind.task.todolist.templates.dialogImport());

  var buttons = new goog.ui.Dialog.ButtonSet();
  buttons.addButton({
    key : 'ok',
    caption : MSG_IMPORT
  }, true, false);
  buttons.addButton(goog.ui.Dialog.ButtonSet.DefaultButtons.CANCEL, false, false);
  dialog.setButtonSet(buttons);
  this.addChild(dialog);

  // Rename todolist dialog
  /** @meaning tasks.todolist.renameDialog.title */
  var MSG_RENAME_DIALOG_TITLE = goog.getMsg('Rename Todolist');
  /** @meaning tasks.todolist.renameDialog.content */
  var MSG_RENAME_DIALOG_CONTENT = goog.getMsg('Enter new todolist name');
  /** @meaning tasks.todolist.renameDialog.button */
  var MSG_RENAME_DIALOG_OK_BUTTON = goog.getMsg('Rename');
  var renameDialog = new goog.ui.Dialog();
  renameDialog.setId('rename-dialog');
  renameDialog.setTitle(MSG_RENAME_DIALOG_TITLE);

  // FIXME: use goog.ui.LabelInput
  renameDialog.setContent("<input id='container-new-name' type='text' size='25em' placeholder='"
      + MSG_RENAME_DIALOG_CONTENT + "' autofocus/>");
  var renameButtons = new goog.ui.Dialog.ButtonSet();
  renameButtons.addButton({
    key : 'ok',
    caption : MSG_RENAME_DIALOG_OK_BUTTON
  }, true, false);
  renameButtons.addButton(goog.ui.Dialog.ButtonSet.DefaultButtons.CANCEL, false, false);
  renameDialog.setButtonSet(renameButtons);
  this.addChild(renameDialog);

  // Remove todolist dialog
  /** @meaning tasks.todolist.removeDialog.title */
  var MSG_RM_DIALOG_TITLE = goog.getMsg('Remove Todolist');
  /** @meaning tasks.todolist.removeDialog.content */
  var MSG_RM_DIALOG_CONTENT = goog.getMsg('Would you like to remove the selected todolist?'); // FIXME
  // todolist
  // name
  /** @meaning tasks.todolist.removeDialog.button */
  var MSG_RM_DIALOG_OK_BUTTON = goog.getMsg('Remove');
  var rmDialog = new goog.ui.Dialog();
  rmDialog.setId('rm-dialog');
  rmDialog.setTitle(MSG_RM_DIALOG_TITLE);
  rmDialog.setContent(MSG_RM_DIALOG_CONTENT);
  var rmButtons = new goog.ui.Dialog.ButtonSet();
  rmButtons.addButton({
    key : 'ok',
    caption : MSG_RM_DIALOG_OK_BUTTON
  }, true, false);
  rmButtons.addButton(goog.ui.Dialog.ButtonSet.DefaultButtons.CANCEL, false, false);
  rmDialog.setButtonSet(rmButtons);
  this.addChild(rmDialog);

  // TODO maybe a nice meaning general.import.progress ?
  /** @meaning contact.vcf.import.progress */
  var MSG_IMPORT_IN_PROGRESS = goog.getMsg('Import in progress...');
  dialog = new net.bluemind.commons.ui.TaskProgressDialog(ctx);
  dialog.setTitle(MSG_IMPORT_IN_PROGRESS);
  dialog.setId('progress-dialog');
  this.addChild(dialog);
  dialog.render();
}
goog.inherits(net.bluemind.task.todolist.TodoListView, goog.ui.Component);

/**
 * Context
 */
net.bluemind.task.todolist.TodoListView.prototype.ctx_;

/** @override */
net.bluemind.task.todolist.TodoListView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var el = this.getElement();
  this.getChild('toolbar').render(el);
  this.getChild('todos').render(el);

};

/** @override */
net.bluemind.task.todolist.TodoListView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);

  var children = this.getChild('todos').removeChildren(true);
  for (var i = 0; i < children.length; i++) {
    children[i].dispose();
  }
  if (model) {
    goog.array.forEach(model, function(section) {
      var s = new net.bluemind.ui.SubList(section.label);
      if (section.warning) {
        s.addClassName(goog.getCssName('warning'));
      }
      if (section.collapse) {
        s.collapse();
      }
      this.getChild('todos').addChild(s, true);
      this.getHandler().listen(s, net.bluemind.task.EventType.MARK_AS_DONE, this.markAsDone_, false, this);
      goog.array.forEach(section.entries, function(todo) {
        var t = new net.bluemind.task.todolist.ui.ListTask(todo);
        t.setId(todo.uid);
        s.addChild(t, true);
      }, this);
    }, this);
  }
};

/** @override */
net.bluemind.task.todolist.TodoListView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');

  var menu = this.getChild('menu');
  this.getHandler().listen(menu, goog.ui.Component.EventType.BEFORE_SHOW, this.popupShowAction_, false, this);

  this.getHandler().listen(menu, goog.ui.Component.EventType.ACTION, this.handleAction_, false, this);

  this.getHandler().listen(this.getChild('import-dialog'), goog.ui.Dialog.EventType.SELECT, this.handleImport_);

  var renameDialog = this.getChild('rename-dialog');
  this.getHandler().listen(renameDialog, goog.ui.Dialog.EventType.SELECT, this.renameDialogAction_, false, this);

  var rmDialog = this.getChild('rm-dialog');
  this.getHandler().listen(rmDialog, goog.ui.Dialog.EventType.SELECT, this.rmDialogAction_, false, this);

  var todos = this.getChild('todos');
  this.getHandler().listen(todos, goog.ui.Component.EventType.ACTION, this.onAction_);

  var monitor = goog.dom.ViewportSizeMonitor.getInstanceForWindow(this.getDomHelper().getWindow());
  this.getHandler().listen(monitor, goog.events.EventType.RESIZE, this.resize_);

  this.resize_();
};

/**
 * @private
 */
net.bluemind.task.todolist.TodoListView.prototype.resize_ = function() {
  var todos = this.getChild('todos');
  var size = this.getDomHelper().getViewportSize();
  var height = size.height;
  var elSize = goog.style.getClientPosition(todos.getElement());
  if (height - elSize.y > 40) {
    todos.getElement().style.height = (height - elSize.y - 20) + 'px';
  } else {
    todos.getElement().style.maxHeight = '10px';
  }
};

/**
 * FIXME ...
 */
net.bluemind.task.todolist.TodoListView.prototype.popupShowAction_ = function() {
  var uid = this.ctx_.params.get('container');
  var menu = this.getChild('menu');
  if (uid == null) {
    menu.getChild('import').setEnabled(false);
    menu.getChild('export').setEnabled(false);
    menu.getChild('rename').setEnabled(false);
    menu.getChild('remove').setEnabled(false);
  } else {
    var todolists = this.ctx_.session.get('todolists');
    var e = goog.array.find(todolists, function(todolist) {
      return uid == todolist['uid'];
    });

    menu.getChild('import').setEnabled(true);
    menu.getChild('export').setEnabled(true);
    if (e != null) {
      var name = e['name'];

      if (e['defaultContainer']) {
        menu.getChild('rename').setEnabled(false);
        menu.getChild('remove').setEnabled(false);
      } else {
        menu.getChild('rename').setEnabled(true);
        menu.getChild('remove').setEnabled(true);
      }
      // FIXME
      var dom = this.getDomHelper();
      /** @meaning general.rename */
      var MSG_RENAME = goog.getMsg('Rename');
      /** @meaning general.remove */
      var MSG_REMOVE = goog.getMsg('Remove');

      var content = dom.createDom('div', null, dom.createDom('span', [ goog.getCssName('fa'),
          goog.getCssName('fa-pencil') ]), '\u00A0', MSG_RENAME + " '" + name + "'");
      menu.getChild('rename').setContent(content);

      content = dom.createDom('div', null, dom.createDom('span',
          [ goog.getCssName('fa'), goog.getCssName('fa-trash-o') ]), '\u00A0', MSG_REMOVE + " '" + name + "'");
      menu.getChild('remove').setContent(content);
    }
  }

};

net.bluemind.task.todolist.TodoListView.prototype.markAsDone_ = function(e) {
  var that = this;
  var todoUid = e.target.getId();
  var container = this.ctx_.session.get('container');
  var service = this.ctx_.service('todolist');
  return service.getItem(container, todoUid).then(function(todo) {
    if (todo.value.status != "Completed") {
      todo.value.status = "Completed";
    } else {
      todo.value.status = "InProcess";
    }
    service.update(todo).then(function() {
      var loc = this.getDomHelper().getWindow().location;
      loc.hash = loc;
    }, null, this);
    ;
  }, null, this);
}

/**
 * Called when a list action is triggered (on select)
 * 
 * @param {goog.events.Event} e The event object.
 * @private
 */
net.bluemind.task.todolist.TodoListView.prototype.onAction_ = function(e) {
  var control = e.target;
  if (control.isSupportedState(goog.ui.Component.State.SELECTED)) {
    var loc = this.getDomHelper().getWindow().location;
    var model = control.getModel();
    loc.hash = '/vtodo/?container=' + model.container + '&uid=' + model.uid + '&ts=' + goog.now();
  }
};

/**
 * Dispatch a view event on toolbar button pressed
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.task.todolist.TodoListView.prototype.handleAction_ = function(evt) {
  var id = evt.target.getId();
  switch (id) {
  case 'import':
    this.getChild('import-dialog').setVisible(true);
    break;
  case 'export':
    evt.type = net.bluemind.task.todolist.TodoListView.EventType.EXPORT;
    this.dispatchEvent(evt);
    break;
  case 'rename':
    var uid = this.ctx_.params.get('container');
    if (uid != null) {
      var dialog = this.getChild('rename-dialog');
      dialog.setVisible(true);
    }
    break;
  case 'remove':
    var uid = this.ctx_.params.get('container');
    if (uid != null) {
      var dialog = this.getChild('rm-dialog');
      dialog.setVisible(true);
    }
    break;
  }
};

/**
 * Dispatch a view event remove todolist dialog
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.task.todolist.TodoListView.prototype.rmDialogAction_ = function(evt) {
  if (evt.key == 'ok') {
    var uid = this.ctx_.params.get('container');
    var service = this.ctx_.service("todolists");
    service.delete_(uid).then(function() {
      this.ctx_.notifyInfo('todolist deleted');
      this.ctx_.session.remove('container');
      this.ctx_.helper('url').goTo("/");
    }, null, this);
  }
};

/**
 * Dispatch a view event rename todolist dialog
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.task.todolist.TodoListView.prototype.renameDialogAction_ = function(evt) {
  if (evt.key == 'ok') {
    var label = goog.dom.getElement('container-new-name').value;
    if (label != null && label.trim() != '') {
      var uid = this.ctx_.params.get('container');
      var service = this.ctx_.service("todolists");
      service.update(uid, {
        'name' : label.trim(),
        'defaultContainer' : false
      }).then(function() {
        goog.dom.getElement('container-new-name').value = null;
        this.ctx_.notifyInfo('todolist renamed');
        this.ctx_.helper('url').reload();
      });
    }
  }
};

/**
 * Dispatch a import event
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.task.todolist.TodoListView.prototype.handleImport_ = function(evt) {
  var id = evt.key;
  if (evt.key == 'ok') {

    var form = this.getChild('import-dialog').getElement().getElementsByTagName('form').item(0);

    var todolist = this.ctx_.session.get('container');

    form.action = "import-vtodo?todolist=" + todolist;
    var io = new goog.net.IframeIo();
    this.getHandler().listenOnce(io, goog.net.EventType.SUCCESS, function(e) {

      var io = e.target;
      var taskRefId = io.getResponseText();
      if (taskRefId) {
        this.getChild('progress-dialog').setVisible(true);
        this.getChild('progress-dialog').taskmon(taskRefId, goog.bind(this.handleImportFinished_, this));
      }
    });
    io.sendFromForm(form);
  }
};

net.bluemind.task.todolist.TodoListView.prototype.handleImportFinished_ = function(e) {
  /** @meaning task.ics.ko */
  var MSG_VTODO_KO = goog.getMsg('Fail to import ICS. (VTODO)');

  if (e['state'] == 'InError') {
    this.setStatus(MSG_VTODO_KO);
  } else {
    var res = goog.global['JSON'].parse(e['result']);
    var msg, all = res['total'], ok = res['uids'].length;
    if (ok > 1 && ok == all) {
      /** @meaning task.ics.success.all */
      var MSG_ICS_ALL = goog.getMsg('{$ok} tasks succesfully imported.', {
        'ok' : ok
      });
      msg = MSG_ICS_ALL;
    } else if (ok == 1 && ok == all) {
      /** @meaning task.ics.success.one */
      var MSG_ICS_ONLY = goog.getMsg('The task have been succesfully imported.');
      msg = MSG_ICS_ONLY;
    } else if (ok > 1) {
      /** @meaning task.ics.success.some */
      var MSG_ICS_SOME = goog.getMsg('{$ok} / {$all} tasks succesfully imported.', {
        'ok' : ok,
        'all' : all
      });
      msg = MSG_ICS_SOME;
    } else if (ok == 1) {
      /** @meaning task.ics.success.oneInMany */
      var MSG_ICS_ONE = goog.getMsg('1 / {$all} tasks succesfully imported.', {
        'all' : all
      });
      msg = MSG_ICS_ONE;
    } else {
      msg = MSG_VTODO_KO;
    }
    this.getChild('progress-dialog').setStatus(msg);
  }

}
/**
 * Enum for the events dispatched by the View.
 * 
 * @enum {string}
 */
net.bluemind.task.todolist.TodoListView.EventType = {
  IMPORT : 'import',
  EXPORT : 'export',
  REMOVE : 'remove'
};
