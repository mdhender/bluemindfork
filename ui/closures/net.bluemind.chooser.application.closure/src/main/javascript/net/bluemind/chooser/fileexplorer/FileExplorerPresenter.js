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
goog.provide('net.bluemind.chooser.fileexplorer.FileExplorerPresenter');

goog.require('goog.Promise');
goog.require('goog.array');
goog.require('goog.dom');
goog.require('goog.ui.Component.EventType');
goog.require('net.bluemind.chooser.fileexplorer.FileExplorerView');
goog.require('net.bluemind.filehosting.api.FileHostingClient');
goog.require('net.bluemind.filehosting.ui.FileHostingDirectory');
goog.require('net.bluemind.filehosting.ui.FileHostingFile');
goog.require('net.bluemind.mvp.Presenter');
goog.require('net.bluemind.string');



/**
 * @constructor
 *
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.chooser.fileexplorer.FileExplorerView();
  this.registerDisposable(this.view_);
  this.view_.setMultipleSelectionAllowed(!!ctx.session.get('multiSelect'));

};
goog.inherits(net.bluemind.chooser.fileexplorer.FileExplorerPresenter, net.bluemind.mvp.Presenter);


/**
 * @type {net.bluemind.chooser.fileexplorer.FileExplorerView}
 * @private
 */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter.prototype.view_;


/** @override */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('full'));
  this.handler.listen(this.view_, goog.ui.Component.EventType.SELECT, this.handleSelect_);
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.handleAction_);
  return goog.Promise.resolve();

};


/** @override */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter.prototype.setup = function() {
  this.view_.removeChildren(true);

  this.browse_().then(function(files) {
    goog.array.sort(files, function(file1, file2) {
      if (file1['type'] != file2['type']) {
        return (file1['type'] == 'DIRECTORY') ? -1 : 1;
      }
      return net.bluemind.string.normalizedCompare(file1['name'], file2['name']);

    });
    goog.array.forEach(files, function(file) {
      var child;
      if (file['type'] == 'DIRECTORY') {
        child = new net.bluemind.filehosting.ui.FileHostingDirectory(file['name'], file['path']);
        child.setModel(file);
      } else {
        child = new net.bluemind.filehosting.ui.FileHostingFile(file['name'], file['path']);
        child.setModel(file);
      }

      this.view_.addChild(child, true);
    }, this);
  }, null, this);
  return goog.Promise.resolve();
};


/**
 * @private
 * @return {goog.Thenable}
 */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter.prototype.browse_ = function() {

  var client = new net.bluemind.filehosting.api.FileHostingClient(this.ctx.rpc, '', this.ctx.user['domainUid']);
  if (this.ctx.params.containsKey('search')) {
    var search = (this.ctx.params.get('search') + '').trim();
    if (search) {
      return (/** @type {goog.Thenable} */
          (client.find(search)));
    }
  }
  var path = this.ctx.params.containsKey('path') ? this.ctx.params.get('path') + '' : '/';
  return (/** @type {goog.Thenable} */
      (client.list(path || '/')));
};


/** @override */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};


/**
 * @param {goog.events.ActionEvent} e
 * @private
 */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter.prototype.handleAction_ = function(e) {
  var file = e.target;
  if (file instanceof net.bluemind.filehosting.ui.FileHostingDirectory) {
    this.ctx.helper('url').goTo('?path=' + file.getPath());
  }
};


/**
 * @param {goog.events.Event} e
 * @private
 */
net.bluemind.chooser.fileexplorer.FileExplorerPresenter.prototype.handleSelect_ = function(e) {
  var selection = goog.array.map(this.view_.getSelectedChildren(), function(file) {
    return file.getModel();
  });
  this.ctx.session.set('selection', selection);
  this.ctx.handler('selection').dispatchEvent(goog.events.EventType.CHANGE);
};
