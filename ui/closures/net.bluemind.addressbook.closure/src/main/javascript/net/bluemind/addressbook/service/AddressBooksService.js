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

goog.provide("net.bluemind.addressbook.service.AddressBooksService");

goog.require("net.bluemind.container.service.ContainersService");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("net.bluemind.addressbook.api.AddressBooksClient");
goog.require("net.bluemind.mvp.helper.ServiceHelper");
goog.require("net.bluemind.core.container.api.ContainersClient");
goog.require("goog.events.EventHandler");
goog.require("net.bluemind.sync.SyncEngine");
goog.require("goog.events.EventTarget");
goog.require("net.bluemind.container.service.ContainersObserver.EventType");
goog.require("net.bluemind.container.service.ContainerService");

/**
 * @constructor
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 */
net.bluemind.addressbook.service.AddressBooksService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.listenedContainers = [];
  this.handler = new goog.events.EventHandler(this);
  this.css_ = new net.bluemind.container.service.ContainersService(ctx, 'contact');
  this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'contact');
  if (this.isLocal()) {
    this.handler.listen(this.ctx.service('folders'), net.bluemind.container.service.ContainersService.EventType.CHANGE,
      this.synchronizeFolders_);
  }
  this.handler.listen(this.css_, net.bluemind.container.service.ContainersService.EventType.CHANGE, function() {
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  });
};
goog.inherits(net.bluemind.addressbook.service.AddressBooksService, goog.events.EventTarget);

net.bluemind.addressbook.service.AddressBooksService.prototype.isLocal = function() {
  return this.css_.available();
};

net.bluemind.addressbook.service.AddressBooksService.prototype.handleByState = function(states, params) {
  var localState = [];
  if (this.css_.available()) {
    localState.push('local');
  }
  if (this.ctx.online) {
    localState.push('remote');
  }

  return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
};

net.bluemind.addressbook.service.AddressBooksService.prototype.list = function() {
  return this.ctx.service('folders').getFolders('addressbook').then(function(addressbooks) {
    this.listenBooks(addressbooks);
    return addressbooks;
  }, null, this);
}

net.bluemind.addressbook.service.AddressBooksService.prototype.synchronizeFolders_ = function() {
  this.ctx.service('folders').getFolders('addressbook').then(function(addressbooks) {
    this.listenBooks(addressbooks);
    this.css_.sync('addressbook', addressbooks);
  }, null, this);
}

net.bluemind.addressbook.service.AddressBooksService.prototype.listenBooks = function(books) {

  this.ctx.service('containersObserver').observerContainers('addressbook', goog.array.map(books, function(book) {
    return book['uid'];
  }));

}
/**
 * If an event storage is raised to notify that the on calendar container has
 * changed (added, removed, renamed, ..) then this method rise a foldersChanged
 * (sick) event.
 * 
 * @param {goog.events.BrowserEvent} evt Storage event.
 */
net.bluemind.addressbook.service.AddressBooksService.prototype.handleStorageChange_ = function(evt) {
  var e = evt.getBrowserEvent();
  if (e.key == 'addressbooks-sync') {
    this.dispatchEvent(net.bluemind.container.service.ContainersService.EventType.CHANGE);
  }
};

net.bluemind.addressbook.service.AddressBooksService.prototype.search = function(token, opt_offset, opt_limit,
    opt_orderby, opt_custom_query) {
  return this.handleByState({
    'remote' : this.searchRemote,
    'local' : this.searchLocal
  }, [ token, opt_offset, opt_limit, opt_orderby, opt_custom_query ]);
}

net.bluemind.addressbook.service.AddressBooksService.prototype.searchLocal = function(token, opt_offset, opt_limit,
    opt_orderby, opt_custom_query) {
  var query = [];
  var words = net.bluemind.string.normalize(token).split(/[\s.,"'?!;:#$%&()+-/<>=@[\]\^_{}|~]/);
  var query = goog.array.map(words, function(word) {
    return [ 'fulltext', '^', word ];
  });

  return this.css_.searchItems(query, opt_offset, opt_limit).then(function(res) {
    // FIXME:
    var data = res;
    data.count = res.length;
    return data;
  });
}

net.bluemind.addressbook.service.AddressBooksService.prototype.searchRemote = function(token, opt_offset, opt_limit,
    opt_orderby, opt_custom_query) {
  token = this.ctx.helper('elasticsearch').escape(token);
  var q = 'value.identification.formatedName.value:' + token 
  + ' OR value.identification.nickname.value:' + token 
  + ' OR value.communications.emails.value:' + token
  + ' OR value.communications.tels.value:' + token
  + ' OR value.deliveryAddressing.address.streetAddress:' + token
  + ' OR value.deliveryAddressing.address.locality:' + token
  + ' OR value.deliveryAddressing.address.parameters.value:' + token
  + ' OR value.organizational.role:' + token
  + ' OR value.organizational.title:' + token
  + ' OR value.organizational.org.division:' + token
  + ' OR value.organizational.org.company:' + token
  + ' OR value.explanatory.categories.label:' + token
  + ' OR value.organizational.org.department:' + token;
  if (opt_custom_query != null) {
    q = opt_custom_query;
  }

  var client = new net.bluemind.addressbook.api.AddressBooksClient(this.ctx.rpc, '');
  var query = {
    'query' : q,
    'from' : opt_offset,
    'size' : opt_limit || 75
  }

  if (opt_orderby) {
    query['orderBy'] = opt_orderby;
  }

  var transform = null;

  return client.search(query).then(function(res) {
    return this.handleByState({
      'local,remote': this.retrieveRemote_,
      'local' : this.retrieveLocal_, // prefere retrieve items from local db
      'remote' : this.retrieveRemote_
    }, [ res ]);
  }, null, this);
};

net.bluemind.addressbook.service.AddressBooksService.prototype.retrieveLocal_ = function(res) {
  var data = goog.array.map(res['values'], function(info) {
    return this.ctx.service('addressbook').getItem(info['containerUid'], info['uid']);
  }, this);

  return goog.Promise.all(data).then(function(t) {
    t.count = res['total'];
    return t;
  });
}

net.bluemind.addressbook.service.AddressBooksService.prototype.retrieveRemote_ = function(res) {
  var data = goog.array.map(res['values'], function(info) {
    var infoValue = info['value'];
    var entry = info;
    entry['value'] = {
      'identification' : {
        'formatedName' : {
          'value' : infoValue['formatedName']
        },
        'photo': infoValue['photo']
      },
      'communications' : {
        'emails' : [ {
          'value' : infoValue['mail']
        } ]
      },
      'deliveryAddressing' : {},
      'organizational' : {
        'member' : []
      },
      'kind' : infoValue['kind'],
      'explanatory' : {
        'categories' : infoValue['categories']
      }
    };

    if (infoValue['tel']) {
      entry['value']['communications']['tels'] = [ {
        'value' : infoValue['tel']
      } ];
    }
    if (infoValue['mail']) {
      entry['value']['communications']['emails'] = [ {
        'value' : infoValue['mail']
      } ];
    }

    if (infoValue['memberCount'] >= 0) {
      entry['value']['organizational']['member'].length = infoValue['memberCount'];
    }
    entry['value']['source'] = infoValue['source'];
    entry['name'] = entry['value']['identification']['formatedName']['value'];
    entry['container'] = info['containerUid'];
    return entry;
  });
  // FIXME:
  data.count = res['total'];
  return data;
}

net.bluemind.addressbook.service.AddressBooksService.prototype.searchByTag = function(tagUid, opt_offset, opt_limit) {
  return this.handleByState({
    'local,remote': this.searchByTagRemote,
    'local' : this.searchByTagLocal,
    'remote' : this.searchByTagRemote
  }, [ tagUid, opt_offset, opt_limit ]);
}

net.bluemind.addressbook.service.AddressBooksService.prototype.searchByTagLocal = function(tagUid, opt_offset,
    opt_limit) {
  var query = [ [ 'tags', '=', tagUid ] ];

  return this.css_.searchItems(query, opt_offset, opt_limit).then(function(res) {
    // FIXME:
    var data = res;
    data.count = res.length;
    return data;
  });
}

net.bluemind.addressbook.service.AddressBooksService.prototype.searchByTagRemote = function(tagUid, opt_offset,
    opt_limit) {

  var q = 'value.explanatory.categories.itemUid:' + tagUid;

  var client = new net.bluemind.addressbook.api.AddressBooksClient(this.ctx.rpc, '');
  var query = {
    'query' : q,
    'from' : opt_offset,
    'size' : opt_limit || 75
  }

  return client.search(query).then(function(res) {
    return this.retrieveRemote_(res)
  }, null, this);
}
/**
 * Retrieve local changes
 * 
 * @param {string} containerId Container id
 * @return {goog.Promise.<Array.<Object>>} Changes object matching request
 */
net.bluemind.addressbook.service.AddressBooksService.prototype.getLocalChangeSet = function() {
  return this.handleByState({
    'local,remote' : function() {
      return this.cs_.getLocalChangeSet();
    }, //
    'local' : function() {
      return this.cs_.getLocalChangeSet();
    }, //
    'remote' : function() {
      return goog.Promise.resolve([]);
    }
  }, []);
};

net.bluemind.addressbook.service.AddressBooksService.prototype.expandGroup = function(c, uid, opt_expanded) {
  var expanded = opt_expanded || [];
  if (goog.array.contains(expanded, c + "-" + uid)) {
    return goog.Promise.resolve([]);
  }

  expanded.push(c + "-" + uid);
  return this.ctx.service('addressbook').getItem(c, uid).then(function(g) {
    var members = [];
    var promises = goog.array.map(g['value']['organizational']['member'], function(member) {
      if (!member['containerUid']){
        members.push({'mailto': member['mailto'], 'cn': member['commonName']});
        return goog.Promise.resolve([]);
      }
      return this.ctx.service('addressbook').getItem(member['containerUid'], member['itemUid']).then(function(card) {
        if (!card) {
          return;
        }
        if (card['value']['kind'] == 'group') {
          return this.expandGroup(member['containerUid'], member['itemUid'], expanded).then(function(m) {
            goog.array.extend(members, m);
          });
        } else {
          members.push(card);
        }
      }, function(e){
      	return null;
      }, this);
    }, this);
    return goog.Promise.all(promises).then(function() {
      return members;
    });
  }, null, this);

}
