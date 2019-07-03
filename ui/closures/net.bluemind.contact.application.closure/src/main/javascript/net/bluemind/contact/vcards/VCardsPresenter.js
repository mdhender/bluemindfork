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
goog.provide("net.bluemind.contact.vcards.VCardsPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("goog.string");
goog.require("goog.Uri.QueryData");
goog.require("goog.async.Deferred");
goog.require("goog.net.EventType");
goog.require("goog.net.IframeIo");
goog.require("net.bluemind.async.Throttle");
goog.require("net.bluemind.contact.vcards.VCardsView");
goog.require("net.bluemind.contact.vcards.VCardsView.EventType");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.contact.Messages");

/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.contact.vcards.VCardsPresenter = function(ctx) {
  goog.base(this, ctx);
  this.view_ = new net.bluemind.contact.vcards.VCardsView(ctx);
  this.registerDisposable(this.view_);
  var iscroll = this.view_.getChild('vcards');
  var throttle = new net.bluemind.async.Throttle(this.getVCards_, 200, this);
  iscroll.dataRequest = goog.bind(throttle.fire, throttle);
  iscroll.positionRequest = goog.bind(this.getVCardPosition_, this);
  this.registerDisposable(throttle);
  this.handler.listen(this.view_, net.bluemind.contact.vcards.VCardsView.EventType.GOTO, this.handleConsultVCard_)
  this.handler.listen(this.view_, net.bluemind.contact.vcards.VCardsView.EventType.EXPORT, this.handleExport_)
  this.handler.listen(this.ctx.service('addressbook'),
      net.bluemind.container.service.ContainerService.EventType.CHANGE, this.handleChange_);

};
goog.inherits(net.bluemind.contact.vcards.VCardsPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {goog.ui.Component}
 * @private
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.view_;

/** @override */
net.bluemind.contact.vcards.VCardsPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('sub-navigation'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.contact.vcards.VCardsPresenter.prototype.setup = function() {
  return this.buildVCardsList_();
};

/**
 * @private
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.buildVCardsList_ = function() {

  var container = this.ctx.params.get('container');
  var vcontainer = this.ctx.params.get('vcontainer');
  var tagContainer = this.ctx.params.get('tag');
  if (vcontainer) {
    this.buildVirtualList_(vcontainer);
  } else if (tagContainer) {
    this.buildTagList_(tagContainer);
  } else if (container) {
    this.buildAddressbookList_(container);
  } else {
    this.buildEmptyList_();
  }
}

/**
 * @private
 * @return {goog.Promise}
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.buildAddressbookList_ = function(container) {
  var vcards = this.view_.getChild('vcards');
  var title = this.view_.getChild('vcards-title');
  this.ctx.service('addressbook').getIndex(container).then(function(result) {
    var index = result['index'];
    var count = result['count'];

    // find addressbook in session
    var abs = this.ctx.session.get('addressbooks');
    var def = goog.array.find(abs, function(adb) {
      return adb['uid'] == container;
    });
    var addressbook = def;

    this.view_.renderIndex(index, count);
    if (addressbook) {
      this.view_.setModel(this.addressbookToModelView_(addressbook, count));
    } else {
      this.view_.setModel(null);
    }
  }, null, this).thenCatch(function(error) {
    this.ctx.notifyError(net.bluemind.contact.Messages.errorLoadingBooks(error), error);
  }, this);
};

/**
 * @private
 * @return {goog.Promise}
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.buildVirtualList_ = function(pattern) {
  var vcards = this.view_.getChild('vcards');
  var title = this.view_.getChild('vcards-title');
  var index = [];
  var count = Infinity; // results[0]['count'];

  /** @meaning general.search */
  var MSG_SEARCH_CONTAINER = goog.getMsg('Search');
  var SEARCH_CONTAINER = MSG_SEARCH_CONTAINER + ' ' + pattern;

  var vcontainer = {
    'name' : SEARCH_CONTAINER,
    'uid' : 'search:' + pattern,
    'defaultContainer' : false,
    'type' : 'virtual',
    'writable' : false,
    'settings' : {
      'pattern' : pattern
    }
  };
  // find addressbook in session
  this.view_.renderIndex(index, count);
  if (vcontainer) {
    this.view_.setModel(this.addressbookToModelView_(vcontainer, count));
  } else {
    this.view_.setModel(null);
  }
};

/**
 * @private
 * @return {goog.Promise}
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.buildTagList_ = function(tagUid) {
  var tag = goog.array.find(this.ctx.session.get('tags'), function(tag) {
    return tagUid == tag['itemUid'];
  });
  var vcards = this.view_.getChild('vcards');
  var title = this.view_.getChild('vcards-title');
  var index = [];
  var count = Infinity; // results[0]['count'];

  var vcontainer = {
    'name' : tag['label'],
    'uid' : tag['itemUid'],
    'defaultContainer' : false,
    'type' : 'tag',
    'writable' : false,
    'settings' : {
      'color' : tag['color']
    }
  };
  // find addressbook in session
  this.view_.renderIndex(index, count);
  if (vcontainer) {
    this.view_.setModel(this.addressbookToModelView_(vcontainer, count));
  } else {
    this.view_.setModel(null);
  }
};

/**
 * @private
 * @return {goog.Promise}
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.buildEmptyList_ = function() {
  this.view_.renderIndex([], 0);
  this.view_.setModel(null);
};

/**
 * Update entry list
 * 
 * @param {number} offset
 * @param {number} limit
 * @private
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.getVCards_ = function(offset, limit) {
  var container = this.ctx.params.get('container');
  var vcontainer = this.ctx.params.get('vcontainer');
  var tag = this.ctx.params.get('tag');
  // FIXME
  var vcards;
  if (vcontainer) {
    return this.ctx.service('addressbooks').search(vcontainer, offset).then(function(entries) {
      var model = this.view_.getModel();
      if (model.count != entries.count) {
        model.count = entries.count;
        this.view_.setModel(model);
      }
      vcards = entries;
      return this.ctx.service('addressbooks').getLocalChangeSet();
    }, null, this).then(function(changes) {
      return goog.array.map(vcards, goog.partial(this.vcardToModelView_, changes));
    }, null, this).thenCatch(function(error) {
      this.ctx.notifyError(net.bluemind.contact.Messages.errorLoadingBooks(error), error);
      return [];
    }, this);
  } else if (tag) {
    return this.ctx.service('addressbooks').searchByTag(tag, offset).then(function(entries) {
      var model = this.view_.getModel();
      if (model.count != entries.count) {
        model.count = entries.count;
        this.view_.setModel(model);
      }
      vcards = entries;
      return this.ctx.service('addressbooks').getLocalChangeSet();
    }, null, this).then(function(changes) {
      return goog.array.map(vcards, goog.partial(this.vcardToModelView_, changes));
    }, null, this).thenCatch(function(error) {
      this.ctx.notifyError(net.bluemind.contact.Messages.errorLoadingBooks(error), error);
      return [];
    }, this);
  } else if (container) {
    return this.ctx.service('addressbook').getItems(container, offset).then(function(entries) {
      vcards = entries;
      return this.ctx.service('addressbook').getLocalChangeSet(container);
    }, null, this).then(function(changes) {
      return goog.array.map(vcards, goog.partial(this.vcardToModelView_, changes));
    }, null, this).thenCatch(function(error) {
      this.ctx.notifyError(net.bluemind.contact.Messages.errorLoadingBooks(error), error);
      return [];
    }, this);
  } else {
    return goog.async.Deferred.succeed([]);
  }
};

/**
 * @param {Object} entry Item<VCard>
 * @returns {Object} Vcard converted for display purpose
 * @private
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.vcardToModelView_ = function(changes, entry) {
  var value = entry['value'];
  var card = {};
  
  card.name = entry['name'];
  card.kind = value['kind'];
  card.phone = value['communications']['tels'] && value['communications']['tels'][0]
      && value['communications']['tels'][0]['value'];
  if (card.phone) {
    card.callto = goog.string.removeAll(card.phone, ' ');
  }

  goog.array.forEach([] || value['communications']['emails'], function(email) {
    goog.array.forEach([] || email['parameters'], function(parameter) {
      if (parameter['label'] == 'DEFAULT' && parameter['value'] == 'true') {
        card.email = email['value'];
      }
    });
  });
  card.email = value['communications']['emails'] && value['communications']['emails'][0]
      && value['communications']['emails'][0]['value'];

  if (card.email) {
    card.mailto = card.email;
  } else if (value['kind'] == 'group') {
    card.mailto = "dlist:" + entry['container'] + "/" + entry['uid'];
  }
  card.tags = [];
  goog.array.forEach(value['explanatory']['categories'] || [], function(tag) {
    card.tags.push({
      color : tag['color'],
      label : tag['label']
    });
  });

  card.kind = value['kind'];
  card.id = entry['uid'];
  card.container = entry['container'];
  if (value['identification']['photo']) {
    card.photo = '/api/addressbooks/' + entry['container'] + '/' + entry['uid'] + '/icon';
  }
  var change = goog.array.find(changes, function(change) {
    return (change['itemId'] == entry['uid'] && change['container'] == entry['container']);
  })
  card.states = {};
  card.states.synced = !goog.isDefAndNotNull(change);
  return card;
};

/**
 * @private
 * @param {*} addressbook
 * @param {*} count
 * @return {Object}
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.addressbookToModelView_ = function(addressbook, count) {
  return {
    name : addressbook['name'],
    uid : addressbook['uid'],
    count : count,
    states : {
      writable : addressbook['writable'] && !addressbook['readOnly']
    }
  };
};
/**
 * Get entry position
 * 
 * @param {number} offset
 * @param {number} limit
 * @private
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.getVCardPosition_ = function(entry) {
  var container = this.ctx.params.get('container');
  if (container) {
    var item = {
      'uid' : entry.id,
      'name' : entry.name
    };
    return this.ctx.service('addressbook').getItemPosition(container, item);
  } else {
    return goog.async.Deferred.succeed(null);
  }
};

/**
 * @param {goog.event.Event} e
 * @private
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.handleConsultVCard_ = function(e) {
  var model = e.target.getModel();
  this.ctx.helper('url').goTo('/vcard/?uid=' + model.id + '&container=' + model.container, 'vcontainer');
};

/** @override */
net.bluemind.contact.vcards.VCardsPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * Export action
 * 
 * @param {goog.events.Event} evt Dispatched event
 * @private
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.handleExport_ = function(evt) {
  var container = this.ctx.params.get('container');
  var query = new goog.Uri.QueryData();
  if (container) {
    query.set('containerUid', container);
    goog.global.window.open('export-vcard?' + query.toString());
  } else {
    var tag = this.ctx.params.get('tag');
    if (tag){
      query.set('tagUid', tag);
      goog.global.window.open('export-vcard?' + query.toString());
    }
  }
};

/**
 * Handle change on vcard lists
 * 
 * @param {goog.events.Event} evt Change event.
 */
net.bluemind.contact.vcards.VCardsPresenter.prototype.handleChange_ = function(e) {
  if (this.ctx.params.get('container') == e.container) {
    this.buildAddressbookList_(this.ctx.params.get('container'));
  }
}