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

goog.provide("net.bluemind.task.todolists.TodoListsPresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("bm.ui.NewContainer");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.task.todolists.TodoListsView");

/**
 * @constructor
 * 
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.task.todolists.TodoListsPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.task.todolists.TodoListsView();
  this.registerDisposable(this.view_);
  this.handler.listen(ctx.service('folders'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.handleChanged_);
}
goog.inherits(net.bluemind.task.todolists.TodoListsPresenter, net.bluemind.mvp.Presenter);

/**
 * Constants
 */
net.bluemind.task.todolists.TodoListsPresenter.MY_LISTS = 0;
net.bluemind.task.todolists.TodoListsPresenter.SHARED_LISTS = 1;

/**
 * @type {net.bluemind.task.todolists.TodoListsView}
 * @private
 */
net.bluemind.task.todolists.TodoListsPresenter.prototype.view_;

/** @override */
net.bluemind.task.todolists.TodoListsPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('navigation'));

  // new container widget
  var newContainer = new bm.ui.NewContainer('todolist', this.ctx, this.ctx.service('todolists'));
  newContainer.render(goog.dom.getElement('navigation'));

  this.handler.listen(this.ctx.service('todolists'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      function() {
        this.load_();
      });
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.task.todolists.TodoListsPresenter.prototype.setup = function() {
  return this.load_();
};

/**
 * @private
 * @return {goog.Promise}
 */
net.bluemind.task.todolists.TodoListsPresenter.prototype.load_ = function() {
  var ctx = this.ctx;
  return ctx.service('todolists').list('todolist').then(function(todolists) {
    this.view_.setModel(this.toModelView_(todolists));
  }, null, this).then(function() {
    if (ctx.session.get('container')) {
      var browserHref = decodeURIComponent(window.location.href);
      if (browserHref.indexOf(ctx.session.get('container')) == -1){
    	  this.ctx.helper('url').goTo('/?container=' + ctx.session.get('container'));	
      }
      return this.view_.setSelected(ctx.params.get('container'));
    } 
  }, function(error) {
    this.ctx.notifyError(net.bluemind.task.Messages.errorLoadingLists(error), error);
  }, this)
}

/** @override */
net.bluemind.task.todolists.TodoListsPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Handle change on tasklists
 * 
 * @param {goog.events.Event} evt Change event.
 */
net.bluemind.task.todolists.TodoListsPresenter.prototype.handleChanged_ = function(evt) {
  this.load_();
};

/**
 * Get a model for Tasklists widget.
 * 
 * @return {Array.<Object>} TaskLists view mode.
 * @private
 */
net.bluemind.task.todolists.TodoListsPresenter.prototype.toModelView_ = function(todolists) {

  /** @meaning tasks.totolists.myLists */
  var MSG_MY_LISTS = goog.getMsg('My lists');
  /** @meaning tasks.totolists.sharedLists */
  var MSG_SHARED_LISTS = goog.getMsg('Shared lists');

  var mv = [ {
    label : MSG_MY_LISTS,
    entries : []
  }, {
    label : MSG_SHARED_LISTS,
    entries : []
  } ];

  goog.array.forEach(todolists, function(todolist) {
    var l = {};
    l.uid = todolist['uid'];
    l.label = todolist['name'];
    l.defaultContainer = todolist['defaultContainer'];
    if( todolist['dir'] && todolist['dir']['displayName']) {
      /** @meaning general.sharedBy */
      var MSG_SHARED_BY = goog.getMsg('Shared by');
      l.title = MSG_SHARED_BY + ' ' +todolist['dir']['displayName'];
    } else {
      l.title = l.label;
    }

    if (todolist['owner'] != this.ctx.user['uid']) {
      mv[net.bluemind.task.todolists.TodoListsPresenter.SHARED_LISTS].entries.push(l);
    } else {
      mv[net.bluemind.task.todolists.TodoListsPresenter.MY_LISTS].entries.push(l);
    }
  }, this);

  goog.array.sort(mv[net.bluemind.task.todolists.TodoListsPresenter.SHARED_LISTS].entries, function(a, b) {
    return goog.string.caseInsensitiveCompare(a.label, b.label);
  });

  goog.array.sort(mv[net.bluemind.task.todolists.TodoListsPresenter.MY_LISTS].entries, function(a, b) {
    if (a.defaultContainer && !b.defaultContainer) {
      return -1;
    }
    if (!a.defaultContainer && b.defaultContainer) {
      return 1;
    }
    return goog.string.caseInsensitiveCompare(a.label, b.label);
  });
  return mv;
};
