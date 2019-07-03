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

goog.provide("net.bluemind.addressbook.service.AddressBookService");
goog.require("net.bluemind.i18n.AlphabetIndexSymbols");
goog.require("net.bluemind.string");
goog.require("net.bluemind.container.service.ContainerService");
goog.require("net.bluemind.mvp.helper.ServiceHelper");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 */
net.bluemind.addressbook.service.AddressBookService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.cs_ = new net.bluemind.container.service.ContainerService(ctx, 'contact');
  this.handler_ = new goog.events.EventHandler(this);

  this.handler_.listen(this.cs_, net.bluemind.container.service.ContainerService.EventType.CHANGE, function(e) {
    this.dispatchEvent(e);
  });
};
goog.inherits(net.bluemind.addressbook.service.AddressBookService, goog.events.EventTarget);

net.bluemind.addressbook.service.AddressBookService.prototype.isLocal = function() {
  return this.cs_.available();
};

net.bluemind.addressbook.service.AddressBookService.prototype.handleByState = function(states, containerId, params) {
  return this.ctx.service('folders').getFolder(containerId).then(function(folder) {
    var localState = [];
    if (this.cs_.available() && folder && folder['offlineSync']) {
      localState.push('local');
    }
    if (this.ctx.online) {
      localState.push('remote');
    }
    return net.bluemind.mvp.helper.ServiceHelper.handleByState(this.ctx, this, states, params, localState);
  }, null, this);
};

net.bluemind.addressbook.service.AddressBookService.prototype.getIndex = function(containerId) {
  return this.handleByState({
    'local' : this.getIndexLocal,
    'remote' : this.getIndexRemote
  }, containerId, [ containerId ]);
}

net.bluemind.addressbook.service.AddressBookService.prototype.getIndexRemote = function(containerId) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  var ret = client.allUids().then(function(uids) {
    return {
      'index' : [],
      'count' : uids.length
    };
  });
  return goog.async.Deferred.fromPromise(ret);
}

net.bluemind.addressbook.service.AddressBookService.prototype.getIndexLocal = function(containerId) {
  var p = this.cs_.getItemsIndex(containerId, net.bluemind.i18n.AlphabetIndexSymbols).then(function(itemIndex) {
    return this.cs_.countItems(containerId).then(function(count) {
      return {
        'index' : itemIndex,
        'count' : count
      };

    });
  }, null, this);
  return goog.async.Deferred.fromPromise(p);
}

net.bluemind.addressbook.service.AddressBookService.prototype.getItems = function(containerId, offset) {
  return this.handleByState({
    'local' : this.getItemsLocal,
    'remote' : this.getItemsRemote
  }, containerId, [ containerId, offset ]);
};
net.bluemind.addressbook.service.AddressBookService.prototype.getItemsLocal = function(containerId, offset) {
  return this.cs_.getItems(containerId, offset);
}
net.bluemind.addressbook.service.AddressBookService.prototype.getItemsRemote = function(containerId, offset) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  var ret = client.search({
    'from' : offset,
    'size' : 100,
    'query' : null
  }).then(function(res) {
    return goog.array.map(res['values'], function(info) {
      var infoValue = info['value'];
      var entry = info;
      entry['value'] = {
        'identification' : {
          'formatedName' : {
            'value' : infoValue['formatedName']
          },
          'photo': infoValue['photo']
        },
        'communications' : {},
        'deliveryAddressing' : {},
        'organizational' : {},
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

      entry['name'] = entry['value']['identification']['formatedName']['value'];
      entry['container'] = containerId;
      return entry;
    });
  });
  return goog.async.Deferred.fromPromise(ret);

};

net.bluemind.addressbook.service.AddressBookService.prototype.getItem = function(containerId, id) {
  return this.handleByState(//
  {
    'local' : this.getItemLocal, //
    'remote' : this.getItemRemote
  //
  }, containerId, [ containerId, id ]);

};

net.bluemind.addressbook.service.AddressBookService.prototype.getItemHistory = function(containerId, id) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.itemChangelog(id, 0);
}

net.bluemind.addressbook.service.AddressBookService.prototype.getItemLocal = function(containerId, id) {
  return this.cs_.getItem(containerId, id);
};

net.bluemind.addressbook.service.AddressBookService.prototype.copyItem = function(containerId, id, toContainerId) {
  return this.handleByState({
    'local,remote' : this.copyItemLocalRemote, //
    'local' : this.copyItemLocal, //
    'remote' : this.copyItemRemote
  }, containerId, [ containerId, id, toContainerId ]);
  // FIXME handle remote/local

};

net.bluemind.addressbook.service.AddressBookService.prototype.copyItemLocal = function(containerId, id, toContainerId) {
  return this.cs_.copyItem(containerId, id, toContainerId);
}

net.bluemind.addressbook.service.AddressBookService.prototype.copyItemRemote = function(containerId, id, toContainerId) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.copy([ id ], toContainerId);
}

net.bluemind.addressbook.service.AddressBookService.prototype.copyItemLocalRemote = function(containerId, id,
    toContainerId) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.copy([ id ], toContainerId).then(function() {
    client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', toContainerId);
    return client.getComplete(id);
  }, null, this).then(function(value) {
    var entry = {};
    entry['uid'] = value['uid'];
    entry['container'] = toContainerId;
    entry['value'] = value['value'];
    entry['displayName'] = value['displayName'];
    entry['name'] = value['displayName'];
    return this.cs_.storeItemWithoutChangeLog(entry);
  }, null, this);
}

net.bluemind.addressbook.service.AddressBookService.prototype.moveItem = function(containerId, id, toContainerId) {
  return this.handleByState({
    'local,remote' : this.moveItemLocalRemote, //
    'local' : this.moveItemLocal, //
    'remote' : this.moveItemRemote
  }, containerId, [ containerId, id, toContainerId ]);
};

net.bluemind.addressbook.service.AddressBookService.prototype.moveItemLocal = function(containerId, id, toContainerId) {
  return this.cs_.moveItem(containerId, id, toContainerId);
}

net.bluemind.addressbook.service.AddressBookService.prototype.moveItemRemote = function(containerId, id, toContainerId) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.move([ id ], toContainerId);
}

net.bluemind.addressbook.service.AddressBookService.prototype.moveItemLocalRemote = function(containerId, id,
    toContainerId) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.move([ id ], toContainerId).then(function() {
    return this.cs_.deleteItemWithoutChangeLog(containerId, id);
  }, null, this).then(function() {
    client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', toContainerId);
    return client.getComplete(id);
  }, null, this).then(function(value) {
    var entry = {};
    entry['uid'] = value['uid'];
    entry['container'] = toContainerId;
    entry['value'] = value['value'];
    entry['displayName'] = value['displayName'];
    entry['name'] = value['displayName'];
    return this.cs_.storeItemWithoutChangeLog(entry);
  }, null, this);
}

net.bluemind.addressbook.service.AddressBookService.prototype.getItemRemote = function(containerId, id) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.getComplete(id).then(function(item) {
    item['container'] = containerId;
    item['uid'] = id;
    return item;
  });
};

net.bluemind.addressbook.service.AddressBookService.prototype.getItemPosition = function(containerId, item) {
  item['order'] = net.bluemind.string.normalize(item['name']);
  return this.handleByState({
    'local' : function(containerId, item) {
      this.cs_.getItemPosition(containerId, item);
    }
  }, [ containerId, item ]);
};

net.bluemind.addressbook.service.AddressBookService.prototype.create = function(entry) {
  this.sanitize_(entry);
  return this.handleByState({
    'local,remote' : this.createLocalRemote, //
    'local' : this.createLocal, //
    'remote' : this.createRemote
  }, entry["container"], [ entry ]);

};

net.bluemind.addressbook.service.AddressBookService.prototype.createLocalRemote = function(entry) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', entry['container']);
  return client.create(entry['uid'], entry['value']).then(function() {
    return client.getComplete(entry['uid']);
  }, null, this).then(function(value) {
    entry['value'] = value['value'];
    entry['displayName'] = value['displayName'];
    entry['name'] = value['displayName'];
    return this.cs_.storeItemWithoutChangeLog(entry);
  }, null, this);
};

net.bluemind.addressbook.service.AddressBookService.prototype.createLocal = function(entry) {
  return this.cs_.storeItem(entry);
};
net.bluemind.addressbook.service.AddressBookService.prototype.createRemote = function(entry) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', entry['container']);
  return client.create(entry['uid'], entry['value']);
}

net.bluemind.addressbook.service.AddressBookService.prototype.update = function(entry) {
  this.sanitize_(entry);
  return this.handleByState({
    'local,remote' : this.updateLocalRemote, //
    'local' : this.updateLocal, //
    'remote' : this.updateRemote
  //
  }, entry["container"], [ entry ]);
};

net.bluemind.addressbook.service.AddressBookService.prototype.updateLocalRemote = function(entry) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', entry['container']);
  return client.update(entry['uid'], entry['value']).then(function() {
    return client.getComplete(entry['uid']);
  }, null, this).then(function(value) {
    entry['value'] = value['value'];
    entry['displayName'] = value['displayName'];
    entry['name'] = value['displayName'];
    return this.cs_.storeItemWithoutChangeLog(entry);
  }, null, this);
}

net.bluemind.addressbook.service.AddressBookService.prototype.updateLocal = function(entry) {
  return this.cs_.storeItem(entry);
}
net.bluemind.addressbook.service.AddressBookService.prototype.updateRemote = function(entry) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', entry['container']);
  return client.update(entry['uid'], entry['value']);
}

net.bluemind.addressbook.service.AddressBookService.prototype.setPhoto = function(container, cardUid, photoData) {
  return this.handleByState({
    'local,remote' : function(container, cardUid, photoData) {
      var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', container);
      return client.setPhoto(cardUid, photoData).then(function() {
        return this.cs_.getItem(container, cardUid);
      }, null, this).then(function(entry) {
        entry['value']['identification']['photo'] = true;
        return this.cs_.storeItemWithoutChangeLog(entry);
      }, null, this);
    },
    'remote' : function(container, cardUid, photoData) {
      var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', container);
      return client.setPhoto(cardUid, photoData);
    }

  }, container, [ container, cardUid, photoData ]);
};

net.bluemind.addressbook.service.AddressBookService.prototype.deletePhoto = function(container, cardUid) {
  return this.handleByState({
    'local,remote' : function(container, cardUid, photoData) {
      var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', container);
      return client.deletePhoto(cardUid).then(function() {
        return this.cs_.getItem(container, cardUid);
      }, null, this).then(function(entry) {
        entry['value']['identification']['photo'] = false;
        return this.cs_.storeItemWithoutChangeLog(entry);
      }, null, this);
    },
    'remote' : function(container, cardUid) {
      var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', container);
      return client.deletePhoto(cardUid);
    }
  }, container, [ container, cardUid ]);

};
net.bluemind.addressbook.service.AddressBookService.prototype.deleteItem = function(containerId, id) {
  return this.handleByState({
    'local,remote' : this.deleteItemLocalRemote,
    'local' : this.deleteItemLocal,
    'remote' : this.deleteItemRemote
  }, containerId, [ containerId, id ]);
};

net.bluemind.addressbook.service.AddressBookService.prototype.deleteItemLocalRemote = function(containerId, id) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.delete_(id).then(function() {
    return this.cs_.deleteItemWithoutChangeLog(containerId, id);
  }, null, this);
};

net.bluemind.addressbook.service.AddressBookService.prototype.deleteItemLocal = function(containerId, id) {
  return this.cs_.deleteItem(containerId, id);
};

net.bluemind.addressbook.service.AddressBookService.prototype.deleteItemRemote = function(containerId, id) {
  var client = new net.bluemind.addressbook.api.AddressBookClient(this.ctx.rpc, '', containerId);
  return client.delete_(id);
};

/**
 * Retrieve local changes
 * 
 * @param {string} containerId Container id
 * @return {goog.Promise.<Array.<Object>>} Changes object matching request
 */
net.bluemind.addressbook.service.AddressBookService.prototype.getLocalChangeSet = function(containerId) {
  return this.handleByState({
    'local,remote' : function(containerId) {
      return this.cs_.getLocalChangeSet(containerId);
    }, //
    'local' : function(containerId) {
      return this.cs_.getLocalChangeSet(containerId);
    }, //
    'remote' : function(containerId) {
      return goog.Promise.resolve([]);
    }
  }, containerId, [ containerId ]);
};

/**
 * @param {Object} entry
 * @private
 */
net.bluemind.addressbook.service.AddressBookService.prototype.sanitize_ = function(entry) {

  if (!entry['value']['identification']) {
    entry['value']['identification'] = {};
  }
  if (!entry['value']['communications']) {
    entry['value']['communications'] = {};
  }
  if (!entry['value']['explanatory']) {
    entry['value']['explanatory'] = {};
  }
  if (!entry['value']['related']) {
    entry['value']['related'] = {};
  }
  if (!entry['value']['deliveryAddressing']) {
    entry['value']['deliveryAddressing'] = [];
  }
  if (!entry['value']['organizational']) {
    entry['value']['organizational'] = {};
  }
  if (entry['value']['kind'] == 'individual') {
    delete entry['value']['organizational']['member']
  } else if (entry['value']['kind'] == 'group' && !entry['value']['organizational']['member']) {
    entry['value']['organizational']['member'] = []
  } else if (!entry['value']['kind']) {
    entry['value']['kind'] = goog.isArray(entry['value']['organizational']['member']) ? 'group' : 'individual';
  }

  if (entry['value']['explanatory']['categories']) {
    // FIXME should not be done here
    entry['tags'] = goog.array.map(entry['value']['explanatory']['categories'], function(t) {
      return t['itemUid'];
    });
  }
};
