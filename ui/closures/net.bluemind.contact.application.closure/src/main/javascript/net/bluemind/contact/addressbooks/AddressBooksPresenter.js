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

goog.provide("net.bluemind.contact.addressbooks.AddressBooksPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.contact.addressbooks.AddressBooksView");
goog.require("net.bluemind.container.service.ContainersService.EventType");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.contact.Messages");
/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.contact.addressbooks.AddressBooksPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.contact.addressbooks.AddressBooksView(ctx);
  this.registerDisposable(this.view_);
  this.handler.listen(this.view_, goog.ui.Component.EventType.ACTION, this.onAction_)

}
goog.inherits(net.bluemind.contact.addressbooks.AddressBooksPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {net.bluemind.contact.addressbooks.AddressBooksView}
 * @private
 */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.view_;

/** @override */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('navigation'));
  this.handler.listen(this.ctx.service('addressbooks'),
      net.bluemind.container.service.ContainersService.EventType.CHANGE, function() {
        this.ctx.session.set('addressbooks', []);
        this.ctx.session.set('tags', []);
        this.refreshAddressBooks_();
      }, false, this);
  this.handler.listen(this.ctx.service('tags'),
      net.bluemind.container.service.ContainerService.EventType.CHANGE, function() {
        this.ctx.session.set('addressbooks', []);
        this.ctx.session.set('tags', []);
        this.refreshAddressBooks_();
      }, false, this);
  
  this.refreshAddressBooks_();
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.setup = function() {
  this.refreshAddressBooks_();
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * @private
 */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.refreshAddressBooks_ = function() {
  var addressbooks = null;
  this.ctx.service('addressbooks').list().then(function(abs) {
    // FIXME tricks
    addressbooks = abs;
    return this.ctx.service('tags').getTags();
  }, null, this).then(function(tags) {
    this.ctx.session.set('addressbooks', addressbooks);
    this.ctx.session.set('tags', tags);
    this.displayAddressBooks_(goog.array.clone(addressbooks), goog.array.clone(tags))
  }, null, this).thenCatch(function(e) {
    this.ctx.notifyError(net.bluemind.contact.Messages.errorLoadingBooks(e));
  }, this);

}

/**
 * @private
 */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.displayAddressBooks_ = function(addressbooks, tags) {
  var selected = null;
  var pattern = this.ctx.params.get('vcontainer')
  var tag = this.ctx.params.get('tag');
  if (pattern) {
    /** @meaning general.search */
    var MSG_SEARCH = goog.getMsg('Search');
    var SEARCH_CONTAINER = MSG_SEARCH + ' ' + pattern;
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
    selected = vcontainer['id'];
    addressbooks.push(vcontainer);
  } else if (tag) {
    selected = tag;
  } else if (this.ctx.params.get('container')) {
    selected = this.ctx.params.get('container');
  }

  addressbooks = goog.array.concat(addressbooks, goog.array.map(tags, function(tag) {
    return {
      'name' : tag['label'],
      'id' : tag['containerUid'],
      'uid' : tag['itemUid'],
      'defaultContainer' : false,
      'type' : 'tag',
      'writable' : false,
      'settings' : {
        'color' : tag.color
      }
    };
  }));
  this.view_.setModel(this.toModelView_(addressbooks));
  if (null == selected) {
    selected = 'book:Contacts_' + this.ctx.user['uid'];
    this.ctx.helper('url').goTo('/?container=' + selected);
  }
  this.view_.setSelected(selected);
};
/**
 * Get a model for addressbooks widget.
 * 
 * @param {Array.<Object>} addressbooks Address books model
 * @return {Array.<Object>} Address books view mode.
 * @private
 */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.toModelView_ = function(addressbooks) {
  /** @meaning contact.addressbooks.myAddressbooks */
  var MSG_MY_ADDRESSBOOKS = goog.getMsg('My addressbooks');
  /** @meaning contact.addressbooks.sharedAddressbooks */
  var MSG_SHARED_ADDRESSBOOKS = goog.getMsg('Shared addressbooks');
  /** @meaning contact.addressbooks.others */
  var MSG_OTHERS = goog.getMsg('Others');
  /** @meaning contact.addressbooks.categories */
  var MSG_TAGS = goog.getMsg('Categories');

  var mv = [ {
    label : MSG_MY_ADDRESSBOOKS,
    entries : []
  }, {
    label : MSG_SHARED_ADDRESSBOOKS,
    entries : []
  }, {
    label : MSG_OTHERS,
    entries : []
  }, {
    label : MSG_TAGS,
    entries : []
  } ];
  for (var i = 0; i < addressbooks.length; i++) {
    var addressbook = addressbooks[i];
    var l = {};
    l.uid = addressbook['uid'];
    l.label = addressbook['name'];
    l.defaultContainer = addressbook['defaultContainer'];
    if (addressbook['dir'] && addressbook['dir']['displayName']) {
      /** @meaning contact.addressbooks.sharedBy */
      var MSG_SHARED_BY = goog.getMsg('Shared by');
      l.title = MSG_SHARED_BY + ' ' + addressbook['dir']['displayName'];
    } else {
      l.title = l.label;
    }
    l.settings = addressbook['settings'];
    l.type = addressbook['type'];
    if (addressbook['type'] == 'virtual') {
      mv[2].entries.push(l);
    } else if (addressbook['type'] == 'tag') {
      mv[3].entries.push(l);
    } else if (addressbook['owner'] == this.ctx.user['uid']) {
      mv[0].entries.push(l);
    } else {
      mv[1].entries.push(l);
    }
  }

  goog.array.sort(mv[0].entries, function(e1, e2) {
    if (e1.defaultContainer && !e2.defaultContainer) {
      return -1;
    }
    if (!e1.defaultContainer && e2.defaultContainer) {
      return 1;
    }
    return goog.string.caseInsensitiveCompare(e1.label, e2.label);
  });

  goog.array.sort(mv[1].entries, function(e1, e2) {
    return goog.string.caseInsensitiveCompare(e1.label, e2.label);
  });
  goog.array.sort(mv[3].entries, function(e1, e2) {
    return goog.string.caseInsensitiveCompare(e1.label, e2.label);
  });
  return mv;
};

/**
 * Called when a list item is selected
 * 
 * @param {goog.events.Event} e The event object.
 * @private
 */
net.bluemind.contact.addressbooks.AddressBooksPresenter.prototype.onAction_ = function(e) {
  var ab = e.target.getModel();
  if (ab.type == 'addressbook') {
    this.ctx.helper('url').goTo('/?container=' + ab.uid);
  } else if (ab.type == 'tag') {
    this.ctx.helper('url').goTo('/?tag=' + ab.uid);
  } else if (ab.type == 'virtual'){
    this.ctx.helper('url').goTo('/?vcontainer=' + this.ctx.params.get('vcontainer'));
  }
}
