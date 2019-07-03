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

/** @fileoverview Presenter for the application search bar */

goog.provide("net.bluemind.contact.search.SearchPresenter");

goog.require("goog.Promise");
goog.require("goog.dom");
goog.require("goog.structs.Set");
goog.require("goog.ui.Component.EventType");
goog.require("net.bluemind.contact.search.SearchView");
goog.require("net.bluemind.mvp.Presenter");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.contact.search.SearchPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.contact.search.SearchView();
  this.registerDisposable(this.view_);
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.handleAction_);
};
goog.inherits(net.bluemind.contact.search.SearchPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.contact.search.SearchView}
 * @private
 */
net.bluemind.contact.search.SearchPresenter.prototype.view_;

/** @override */
net.bluemind.contact.search.SearchPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('header'));
  return goog.Promise.resolve();

};

/** @override */
net.bluemind.contact.search.SearchPresenter.prototype.setup = function() {
  var vcontainer = this.ctx.params.get('vcontainer');
  // if (vcontainer && vcontainer['settings'] &&
  // vcontainer['settings']['query']) {
  // this.view_.setModel(vcontainer['settings']['query']);
  this.view_.setModel(vcontainer || '');

  return goog.Promise.resolve();
};

/** @override */
net.bluemind.contact.search.SearchPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * @param {goog.events.Event} e
 * @private
 */
net.bluemind.contact.search.SearchPresenter.prototype.handleAction_ = function(e) {
  // FIXME
  var pattern = this.view_.getChild('search').getValue();
  if (pattern.trim() != '') {
	  /** @meaning general.search */
    var MSG_SEARCH_CONTAINER = goog.getMsg('Search');
    var SEARCH_CONTAINER = MSG_SEARCH_CONTAINER + ' ' + pattern;
    var vcontainer = {
      'name' : SEARCH_CONTAINER,
      'id' : 'search:' + pattern,
      'uid' : 'search:' + pattern,
      'defaultContainer' : false,
      'type' : 'virtual',
      'writable' : false,
      'settings' : {
        'pattern' : pattern
      }
    };

    var uri = new goog.Uri('/');
    uri.getQueryData().set('vcontainer', pattern);
    this.ctx.helper('url').goTo(uri);
  }
};
