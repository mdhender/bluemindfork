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

goog.provide("net.bluemind.contact.vcards.VCardsView");
goog.provide("net.bluemind.contact.vcards.VCardsView.EventType");

goog.require("goog.array");
goog.require("goog.events");
goog.require("goog.soy");
goog.require("goog.string");
goog.require("goog.Uri.QueryData");
goog.require("goog.dom.ViewportSizeMonitor");
goog.require("goog.events.EventType");
goog.require("goog.net.EventType");
goog.require("goog.net.IframeIo");
goog.require("goog.ui.Component");
goog.require("goog.ui.ContainerRenderer");
goog.require("goog.ui.Control");
goog.require("goog.ui.Dialog");
goog.require("goog.ui.Menu");
goog.require("goog.ui.MenuItem");
goog.require("goog.ui.Toolbar");
goog.require("goog.ui.ToolbarMenuButton");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("goog.ui.Dialog.ButtonSet");
goog.require("goog.ui.Dialog.EventType");
goog.require("goog.ui.style.app.ButtonRenderer");
goog.require("net.bluemind.string");
goog.require("net.bluemind.ui.SingleSelectionModel");
goog.require("net.bluemind.commons.ui.TaskProgressDialog");
goog.require("net.bluemind.contact.vcards.templates");
goog.require("net.bluemind.ui.IScroll");

/**
 * @constructor
 * 
 * @extends {goog.ui.Component}
 */
net.bluemind.contact.vcards.VCardsView = function(ctx) {
  goog.ui.Component.call(this);
  this.ctx = ctx;
  var renderer = goog.ui.ContainerRenderer.getCustomRenderer(goog.ui.MenuRenderer, goog.getCssName('vcard-cutin'));

  var toolbar = new goog.ui.Toolbar();
  toolbar.setId('vcards-title');

  var child = new goog.ui.Control();
  child.setId('title');
  child.addClassName(goog.getCssName('goog-inline-block'));
  toolbar.addChild(child, true);

  var menu = new goog.ui.Menu();
  var dom = this.getDomHelper();
  /** @meaning contact.vcf.exportAs */
  var MSG_EXPORT_VCARD = goog.getMsg('Export as VCF');
  var content = dom.createDom('div', null, dom.createDom('span',
      [ goog.getCssName('fa'), goog.getCssName('fa-upload') ]), '\u00A0', MSG_EXPORT_VCARD);
  child = new goog.ui.MenuItem(content);
  child.setId('export');

  menu.addChild(child, true);

  /** @meaning contact.vcf.importFile */
  var MSG_IMPORT_VCARD = goog.getMsg('Import VCF file');
  content = dom.createDom('div', null,
      dom.createDom('span', [ goog.getCssName('fa'), goog.getCssName('fa-download') ]), '\u00A0', MSG_IMPORT_VCARD);
  var child = new goog.ui.MenuItem(content);
  child.setId('import');

  menu.addChild(child, true);

  var child = new goog.ui.ToolbarMenuButton(this.getDomHelper().createDom('div',
      goog.getCssName('goog-button-icon') + ' ' + goog.getCssName('fa') + ' ' + goog.getCssName('fa-cogs')), menu,
      goog.ui.style.app.ButtonRenderer.getInstance());
  child.setId('button');
  toolbar.addChild(child, true);

  this.addChild(toolbar, true);

  var index = new goog.ui.Menu(this.getDomHelper(), renderer);
  index.setId('vcards-index');
  // FIXME : create a object
  index.getOrCreateChild = function(letter) {
    var child = this.getChild(letter);
    if (!child) {
      child = new goog.ui.MenuItem(letter);
      child.setAutoStates(goog.ui.Component.State.HOVER, false);
      child.setId(letter);
      this.addChild(child, true);
    }
    return child;
  }
  this.addChild(index, true);

  var iscroll = new net.bluemind.ui.IScroll();
  iscroll.setId('vcards');
  iscroll.setSelectionModel(new net.bluemind.ui.SingleSelectionModel(function(o) {
    if (o.id)
      return o.id;
    if (o.getId)
      return o.getId();
  }));
  iscroll.fill = goog.bind(this.renderListItem_, this);
  this.addChild(iscroll, true);

  /** @meaning contact.vcf.import.chooseFile */
  var MSG_FILE_TO_IMPORT = goog.getMsg('Choose file to import');
  /** @meaning general.import */
  var MSG_IMPORT = goog.getMsg('Import');
  /** @meaning general.cancel */
  var MSG_CANCEL = goog.getMsg('Cancel');
  var dialog = new goog.ui.Dialog();
  dialog.setContent(net.bluemind.contact.vcards.templates.dialogImport());
  dialog.setTitle(MSG_FILE_TO_IMPORT);
  var dialogButtons = new goog.ui.Dialog.ButtonSet();
  dialogButtons.addButton({
    key : 'ok',
    caption : MSG_IMPORT
  }, true, false);
  dialogButtons.addButton({
    key : 'cancel',
    caption : MSG_CANCEL
  }, false, false);
  dialog.setButtonSet(dialogButtons);
  dialog.setId('import-dialog');
  this.addChild(dialog);
  dialog.render();

  /** @meaning contact.vcf.import.progress */
  var MSG_IMPORT_IN_PROGRESS = goog.getMsg('Import in progress...');
  dialog = new net.bluemind.commons.ui.TaskProgressDialog(ctx);
  dialog.setTitle(MSG_IMPORT_IN_PROGRESS);
  dialog.setId('progress-dialog');
  this.addChild(dialog);
  dialog.render();
}
goog.inherits(net.bluemind.contact.vcards.VCardsView, goog.ui.Component);

/** @override */
net.bluemind.contact.vcards.VCardsView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('vcards-title'), goog.ui.Component.EventType.ACTION, this.handleMenuAction_);
  this.getHandler().listen(this.getChild('import-dialog'), goog.ui.Dialog.EventType.SELECT, this.handleImport_);
  this.getHandler().listen(this.getChild('vcards-title'), goog.ui.Component.EventType.ACTION, this.handleMenuAction_);
  this.getHandler().listen(this.getChild('vcards'), goog.ui.Component.EventType.ACTION, this.onVCardAction);
  this.getHandler().listen(this.getChild('vcards-index'), goog.ui.Component.EventType.ACTION, this.onIndexAction_);
  var monitor = goog.dom.ViewportSizeMonitor.getInstanceForWindow(this.getDomHelper().getWindow());
  this.getHandler().listen(monitor, goog.events.EventType.RESIZE, this.resize_);

  this.resize_();
};

/**
 * Resize vcards view port
 */
net.bluemind.contact.vcards.VCardsView.prototype.resize_ = function() {
  var iscroll = this.getChild('vcards');
  var iindex = this.getChild('vcards-index');
  var size = this.getDomHelper().getViewportSize();
  var height = size.height;

  var top = iscroll.getElement().offsetTop;
  if (height - top > 100) {
    iscroll.getElement().style.height = (height - top - 90) + 'px';
  } else {
    iscroll.getElement().style.height = iscroll.getChildSize() + 'px';
  }
  iindex.getElement().style.height = iscroll.getElement().style.height;
  iscroll.refresh();
};

/**
 * Dispatch a view event on toolbar button pressed
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.handleMenuAction_ = function(evt) {
  var id = evt.target.getId();
  switch (id) {
  case 'import':
    this.getChild('import-dialog').setVisible(true);
    break;
  case 'export':
    evt.type = net.bluemind.contact.vcards.VCardsView.EventType.EXPORT;
    this.dispatchEvent(evt);
    break;
  }
};

/**
 * Dispatch a import event
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.handleImport_ = function(evt) {
  var id = evt.key;
  if (evt.key == 'ok') {
    // evt.type = net.bluemind.contact.vcards.VCardsView.EventType.IMPORT;
    // this.dispatchEvent(evt);

    var form = this.getChild('import-dialog').getElement().getElementsByTagName('form').item(0);

    var query = new goog.Uri.QueryData;
    query.set("containerUid", this.ctx.params.get('container'));
    var uri = "import-vcard?" + query;

    form.action = uri;
    var io = new goog.net.IframeIo();
    this.getHandler().listenOnce(io, goog.net.EventType.SUCCESS, function(e) {

      var io = e.target;
      var taskRefId = io.getResponseText();
      if (taskRefId) {
        if (goog.string.contains(taskRefId, '413 Request Entity Too Large')){
          var MSG_FAIL_TO_IMPORT_VCARD = goog.getMsg('Fail to import vCard');
          this.ctx.notifyError(MSG_FAIL_TO_IMPORT_VCARD);
        } else {
          this.getChild('progress-dialog').setVisible(true);
          this.getChild('progress-dialog').taskmon(taskRefId, goog.bind(this.handleImportFinished_, this));
        }
      }
    });
    io.sendFromForm(form);
  }
};

net.bluemind.contact.vcards.VCardsView.prototype.handleImportFinished_ = function(e) {
  /** @meaning addressbook.vcard.ko */
  var MSG_KO = goog.getMsg('Fail to import VCards.');

  if (e['state'] == 'InError') {
    this.setStatus(MSG_KO);
  } else {
    var res = goog.global['JSON'].parse(e['result']);
    var msg, all = res['total'], ok = res['uids'].length;
    if (ok > 1 && ok == all) {
      /** @meaning addressbook.vcard.success.all */
      var MSG_ICS_ALL = goog.getMsg('{$ok} cards succesfully imported.', {
        'ok' : ok
      });
      msg = MSG_ICS_ALL;
    } else if (ok == 1 && ok == all) {
      /** @meaning addressbook.vcard.success.one */
      var MSG_ICS_ONLY = goog.getMsg('The card have been succesfully imported.');
      msg = MSG_ICS_ONLY;
    } else if (ok > 1) {
      /** @meaning addressbook.vcard.success.some */
      var MSG_ICS_SOME = goog.getMsg('{$ok} / {$all} cards succesfully imported.', {
        'ok' : ok,
        'all' : all
      });
      msg = MSG_ICS_SOME;
    } else if (ok == 1) {
      /** @meaning addressbook.vcard.success.oneInMany */
      var MSG_ICS_ONE = goog.getMsg('1 / {$all} cards succesfully imported.', {
        'all' : all
      });
      msg = MSG_ICS_ONE;
    } else {
      msg = MSG_KO;
    }
    this.getChild('progress-dialog').setStatus(msg);
  }

}
/** @override */
net.bluemind.contact.vcards.VCardsView.prototype.setModel = function(model) {
  var old = this.getModel();
  goog.base(this, 'setModel', model);
  if (model) {
    var title = this.getChild('vcards-title').getChild('title');
    var text = goog.string.truncate(model.name, 30) + ' (' + model.count + ')';
    title.setContent(text);
    this.getChild('vcards-title').getChild('button').setVisible(true);
    var menu = this.getChild('vcards-title').getChild('button').getMenu();
    menu.getChild('import').setEnabled(model.states.writable);
  } else {
    this.getChild('vcards-title').getChild('button').setVisible(false);
    var title = this.getChild('vcards-title').getChild('title');
    title.setContent('');
  }
  if (model && old && model.uid == old.uid) {
    this.getChild('vcards').setRange(0, model.count);
    this.getChild('vcards').refresh();
  } else if (model) {
    this.getChild('vcards').setRange(0, model.count);
    this.getChild('vcards').clear();
  } else {
    this.getChild('vcards').setRange(0, 0);
    this.getChild('vcards').clear();
  }

};

/**
 * Called when a folder action is triggered (on select)
 * 
 * @param {goog.events.Event} e The event object.
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.onVCardAction = function(e) {
  e.type = net.bluemind.contact.vcards.VCardsView.EventType.GOTO;
  this.dispatchEvent(e);
};

/**
 * @param {goog.events.Event} e Event action triggered by index child.
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.onIndexAction_ = function(e) {
  var model = e.target.getModel();
  if (goog.isNumber(model)) {
    this.getChild('vcards').goTo(model);
  }
};

/**
 * @param {goog.ui.Control} child Child to render in
 * @param {*} data Data to render (entry info).
 * @param {number} previous index of the previous entry
 * @param {number} offset Index of the current entry
 * @return {boolean} Is current entry showned.
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.renderListItem_ = function(child, data, previous, offset) {
  var index = this.getChild('vcards-index').getModel();
  var current = data[offset];
  if (goog.isDefAndNotNull(current)) {
    var letter = this.getFirstLetter_(current.name);
    if (data[previous]) {
      var previousLetter = this.getFirstLetter_(data[previous].name);
      if (letter === previousLetter) {
        return this.renderVCardItem_(child, current);
      }
    }
    return this.renderLetterItem_(child, letter, !goog.isDef(previous))
  } else if (!goog.isDef(current) && index) {
    var i = 0;
    while (index[i] <= offset && i < index.length) {
      i++;
    }
    i--;
    if ((offset != previous && offset == index[i]) || !goog.isDef(previous)) {
      var alphabet = net.bluemind.i18n.AlphabetIndexSymbols;
      return this.renderLetterItem_(child, alphabet[i] || '', !goog.isDef(previous))
    }
    return this.renderTemporaryItem_(child)
  }
  child.setVisible(false);
  child.setEnabled(false);
  return true;
};

/**
 * @param {string} word
 * @return {string} letter
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.getFirstLetter_ = function(word) {
  var word = net.bluemind.string.normalizeAccent(word);
  if (word) {
    var alphabet = net.bluemind.i18n.AlphabetIndexSymbols;
    var l = (word[0] || '').toUpperCase();
    return (l < alphabet[0]) ? '#' : l;
  }
  return '#';
};

/**
 * @param {goog.ui.Control} child Child to render in
 * @param {*} data Data to render (entry info).
 * @return {boolean} Is current entry showned.
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.renderVCardItem_ = function(child, data) {
  var el = goog.soy.renderAsElement(net.bluemind.contact.vcards.templates.vcard, data);
  child.setEnabled(true);
  child.setContent(el);
  child.setVisible(true);
  child.enableClassName(goog.getCssName('not-synchronized'), !data.states.synced);
  return true;
};

/**
 * @param {goog.ui.Control} child Child to render in
 * @param {string} letter Section letter.
 * @param {boolean} isFirst Is first list item
 * @return {boolean} Is current entry showned.
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.renderLetterItem_ = function(child, letter, isFirst) {
  var alphabet = net.bluemind.i18n.AlphabetIndexSymbols;
  var el = goog.soy.renderAsElement(net.bluemind.contact.vcards.templates.section, {
    letter : letter
  });
  child.setEnabled(false);
  child.setContent(el);
  child.setVisible(true);
  if (isFirst) {
    var l = alphabet.length - 1;
    letter = (letter > alphabet[l]) ? alphabet[l] : letter;
    var widget = this.getChild('vcards-index').getChild(letter);
    if (widget)
      widget.setHighlighted(true);
  }
  return false;
};

/**
 * @param {goog.ui.Control} child Child to render in
 * @return {boolean} Is current entry showned.
 */
net.bluemind.contact.vcards.VCardsView.prototype.renderTemporaryItem_ = function(child) {
  var el = goog.soy.renderAsElement(net.bluemind.contact.vcards.templates.temporary);
  child.setEnabled(false);
  child.setContent(el);
  child.setVisible(true);
  return true;
};

/**
 * Build entries cutin index
 * 
 * @param {Array} index
 * @param {number} count
 */
net.bluemind.contact.vcards.VCardsView.prototype.renderIndex = function(index, count) {
  if (index.length == 0) {
    this.renderEmptyIndex_();
  } else {
    this.renderFullIndex_(index, count);
  }
};

net.bluemind.contact.vcards.VCardsView.prototype.renderFullIndex_ = function(index, count) {
  var cutin = this.getChild('vcards-index');
  var alphabet = net.bluemind.i18n.AlphabetIndexSymbols
  cutin.setModel(index);
  var anchor = cutin.getOrCreateChild('#');
  if (index[0] > 0) {
    anchor.setEnabled(true);
    anchor.setModel(0);
  } else {
    anchor.setModel(null);
    anchor.setEnabled(false);
  }
  for (var i = 0; i < alphabet.length; i++) {
    var letter = alphabet[i];
    anchor = cutin.getOrCreateChild(letter);
    var next = (i < (alphabet.length - 1) ? index[i + 1] : count);
    if (index[i] == next) {
      anchor.setModel(null);
      anchor.setEnabled(false);
    } else {
      anchor.setEnabled(true);
      anchor.setModel(index[i]);
    }
  }
};

/**
 * Build entries dummy index
 * 
 * @private
 */
net.bluemind.contact.vcards.VCardsView.prototype.renderEmptyIndex_ = function() {
  var widget = this.getChild('vcards-index');
  var alphabet = net.bluemind.i18n.AlphabetIndexSymbols
  var anchor = widget.getOrCreateChild('#');
  anchor.setModel(null);
  anchor.setEnabled(false);
  goog.array.forEach(alphabet, function(letter) {
    var anchor = widget.getOrCreateChild(letter);
    anchor.setModel(null);
    anchor.setEnabled(false);
  });
};

/**
 * @enum
 * 
 */
net.bluemind.contact.vcards.VCardsView.EventType = {
  GOTO : goog.events.getUniqueId('goto'),
  IMPORT : goog.events.getUniqueId('import'),
  EXPORT : goog.events.getUniqueId('export')
};
