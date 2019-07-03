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
goog.provide("net.bluemind.contact.group.edit.GroupEditView");

goog.require("goog.iter");
goog.require("goog.ui.Component");
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.ToolbarButton");
goog.require("goog.ui.ToolbarMenuButton");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.style.app.MenuButtonRenderer");
goog.require("net.bluemind.contact.group.edit.ui.GroupForm");
goog.require("net.bluemind.ui.Link");
goog.require("bluemind.ui.style.DangerousActionButtonRenderer");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");
goog.require("net.bluemind.history.HistoryDialog");

/**
 * @constructor
 * 
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Component}
 */
net.bluemind.contact.group.edit.GroupEditView = function(ctx, opt_domHelper) {
  goog.ui.Component.call(this, opt_domHelper);
  this.addToolbar_();
  var child = new goog.ui.Control();
  child.setId('notice');
  child.addClassName(goog.getCssName('notification'));
  this.addChild(child, true);
  child = new net.bluemind.contact.group.edit.ui.GroupForm(opt_domHelper);
  child.setId('form');
  this.addChild(child, true);
  var history = new net.bluemind.history.HistoryDialog(ctx);
  history.setId('history-dialog');
  this.addChild(history);
}
goog.inherits(net.bluemind.contact.group.edit.GroupEditView, goog.ui.Component);

/**
 * Create the form toolbar.
 * 
 * @private
 */
net.bluemind.contact.group.edit.GroupEditView.prototype.addToolbar_ = function() {
  var button, menu, toolbar = new goog.ui.Toolbar();
  toolbar.setId('toolbar');
  this.addChild(toolbar, true);

  /** @meaning general.save */
  var MSG_SAVE = goog.getMsg('Save');
  button = new goog.ui.ToolbarButton(MSG_SAVE, bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
  button.setId('save');
  toolbar.addChild(button, true);

  /** @meaning contact.copyTo */
  var MSG_COPY_TO = goog.getMsg('Copy to...')
  menu = new goog.ui.Menu();
  button = new goog.ui.ToolbarMenuButton(MSG_COPY_TO, menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  button.setId('copy');
  button.setVisible(false);
  toolbar.addChild(button, true);

  /** @meaning contact.moveTo */
  var MSG_MOVE_TO = goog.getMsg('Move to...')
  menu = new goog.ui.Menu();
  button = new goog.ui.ToolbarMenuButton(MSG_MOVE_TO, menu, goog.ui.style.app.MenuButtonRenderer.getInstance());
  button.setId('move');
  button.setVisible(false);
  toolbar.addChild(button, true);

  button = new goog.ui.ToolbarButton(this.getDomHelper().createDom('div',
      goog.getCssName('goog-button-icon') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-paper-plane')),
      goog.ui.style.app.ButtonRenderer.getInstance());

  button.setId('mailto');
  button.setVisible(false);
  toolbar.addChild(button, true);

  /** @meaning general.history */
  var MSG_HISTORY = goog.getMsg('History');
  button = new goog.ui.ToolbarButton(MSG_HISTORY, goog.ui.style.app.ButtonRenderer.getInstance());
  button.setId('history');
  button.setVisible(false);
  toolbar.addChild(button, true);
  
  /** @meaning contact.group.validate */
  var MSG_VALIDATE = goog.getMsg('Validate');
  button = new goog.ui.ToolbarButton(MSG_VALIDATE, goog.ui.style.app.ButtonRenderer.getInstance());
  button.setId('validate');
  button.setVisible(false);
  toolbar.addChild(button, true);

  /** @meaning general.delete */
  var MSG_DELETE = goog.getMsg('Delete');
  button = new goog.ui.ToolbarButton(MSG_DELETE, bluemind.ui.style.DangerousActionButtonRenderer.getInstance());
  button.setId('delete');
  button.setVisible(false);
  toolbar.addChild(button, true);
};

/**
 * Set the folder list for toolbar menu
 * 
 * @param {bluemind.contact.model.folder.FolderSet} folders Set of folder.
 */
net.bluemind.contact.group.edit.GroupEditView.prototype.setFolders = function(folders) {
  if (this.getChild('toolbar').getChild('copy').getMenu().getChildCount() == 0) {
    folders = goog.array.filter(folders, function(f) {
      return f['writable']
    });
    goog.iter.forEach(folders, function(folder) {
      this.setFolder(folder, 'copy');
      this.setFolder(folder, 'move');
    }, this);
  }
};

/**
 * Set the folder item
 * 
 * @param {bluemind.contact.model.folder.FolderSet} folders Set of folder.
 */
net.bluemind.contact.group.edit.GroupEditView.prototype.setFolder = function(folder, elementName) {
  var item = new goog.ui.MenuItem(folder['name']);
  item.setId(folder['uid']);
  item.setModel(folder);
  this.getChild('toolbar').getChild(elementName).getMenu().addChild(item, true);
}

/**
 * Set tags
 * 
 * @param {Array.<bluemind.model.Tag>} tags tags
 */
net.bluemind.contact.group.edit.GroupEditView.prototype.setTags = function(tags) {
  this.getChild('form').setTags(tags);
}

/** @override */
net.bluemind.contact.group.edit.GroupEditView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('toolbar'), goog.ui.Component.EventType.ACTION, this.dispatchAction_);
  var monitor = goog.dom.ViewportSizeMonitor.getInstanceForWindow(this.getDomHelper().getWindow());
  this.getHandler().listen(monitor, goog.events.EventType.RESIZE, this.resize_);
  this.resize_();
};

/**
 * Resize list
 * 
 * @private
 */
net.bluemind.contact.group.edit.GroupEditView.prototype.resize_ = function() {
  var size = this.getDomHelper().getViewportSize();
  var height = size.height;

  var top = this.getElement().offsetTop;
  if (height - top > 400) {
    this.getElement().style.height = (height - top) + 'px';
  } else {
    this.getElement().style.height = '400px';
  }
};

/** @override */
net.bluemind.contact.group.edit.GroupEditView.prototype.setModel = function(model) {
  goog.base(this, 'setModel', model);
  this.showSynchronizationNotice_(model);
  var email = model.emails && (model.emails.length > 0) && model.emails[0];
  this.getChild('toolbar').getChild('mailto').setEnabled(!!email);
  this.getChild('toolbar').getChild('mailto').setVisible(!!email);

  this.getChild('toolbar').getChild('delete').setVisible(!!model.id);
  this.getChild('toolbar').getChild('history').setVisible(!!model.id);
  this.getChild('toolbar').getChild('validate').setVisible(!!model.id);
  this.getChild('toolbar').getChild('copy').setVisible(!!model.id);
  this.getChild('toolbar').getChild('move').setVisible(!!model.id);

  this.getChild('form').setModel(model);
};

/** @override */
net.bluemind.contact.group.edit.GroupEditView.prototype.getModel = function() {
  return this.getChild('form').getModel();
};

/**
 * Show synchronization state notice
 * 
 * @private
 */
net.bluemind.contact.group.edit.GroupEditView.prototype.showSynchronizationNotice_ = function(model) {
  var notice = this.getChild('notice');
  if (model.states.synced) {
    notice.setVisible(false);
    return;
  }
  if (model.states.error && model.error.message) {
    /** @meaning general.error.synchronization */
    var MSG_SYNC_ERROR = goog.getMsg("Synchronization failed : '{$message}'", {
      'message' : model.error.message
    });
    notice.setContent(MSG_SYNC_ERROR);
    notice.enableClassName(goog.getCssName('notice'), false);
    notice.enableClassName(goog.getCssName('error'), true);
  } else if (model.states.error) {
    /** @meaning general.error.synchronization.unkown */
    var MSG_UNKNOWN_SYNC_ERROR = goog
        .getMsg("Synchronization failed, a new attempt will be made later. Please contact support if this error persists.");
    notice.setContent(MSG_UNKNOWN_SYNC_ERROR);
    notice.enableClassName(goog.getCssName('notice'), false);
    notice.enableClassName(goog.getCssName('error'), true);
  } else {
    /** @meaning general.notice.notSynchronized */
    var MSG_NOT_SYNCHRONIZED = goog.getMsg("Not all modifications are synchronized with the server yet.");
    notice.setContent(MSG_NOT_SYNCHRONIZED);
    notice.enableClassName(goog.getCssName('notice'), true);
    notice.enableClassName(goog.getCssName('error'), false);
  }
  notice.setVisible(true);

};

/**
 * Dispatch an event to the controller
 * 
 * @param {goog.events.Event} evt Action event
 * @private
 */
net.bluemind.contact.group.edit.GroupEditView.prototype.dispatchAction_ = function(evt) {
  if (evt.target instanceof goog.ui.MenuItem) {
    evt.type = evt.target.getParent().getParent().getId();
  } else {
    evt.type = evt.target.getId();
  }
  this.dispatchEvent(evt);
};

net.bluemind.contact.group.edit.GroupEditView.prototype.showHistory = function(entries) {
  this.getChild('history-dialog').show(entries);
}
