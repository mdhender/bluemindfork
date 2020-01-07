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

/**
 * @fileoverview Get contact data from DataBase. Database can either be WebSQL
 *               or IndexedDB.
 * @suppress {accessControls|checkTypes|checkVars|missingProperties|undefinedNames|undefinedVars|unknownDefines|uselessCode|visibility}
 */

goog.provide("net.bluemind.container.persistence.DBItemHome");

goog.require("goog.array");
goog.require("goog.functions");
goog.require("goog.iter");
goog.require("goog.object");
goog.require("goog.string");
goog.require("goog.async.Deferred");
goog.require("goog.async.DeferredList");
goog.require("net.bluemind.string");
goog.require("net.bluemind.container.persistence.ResultList");
goog.require("ydn.db");
goog.require("ydn.db.IndexIterator");
goog.require("ydn.db.IndexValueIterator");
goog.require("ydn.db.Key");
goog.require("ydn.db.KeyRange");
goog.require("ydn.db.Storage");
goog.require("ydn.db.algo.SortedMerge");
goog.require("bluemind.storage.StorageHelper");

/**
 * Use Web storage (aka local storage) to store and retrieve Item data
 * 
 * @param {ydn.db.Storage} database
 * @constructor
 */
net.bluemind.container.persistence.DBItemHome = function(database) {
  this.storage_ = database;
  this.fast_ = this.storage_.branch('multi', false);
};

/**
 * @type {number}
 */
net.bluemind.container.persistence.DBItemHome.MAX_LIMIT = 1048576;
/**
 * Connexion to the database.
 * 
 * @return {ydn.db.Storage} Storage accessor.
 * @private
 */
net.bluemind.container.persistence.DBItemHome.storage_;

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.clear = function() {
  return this.storage_.clear();
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.getVersion = function() {
  return this.storage_.get('configuration', 'version').then(function(row) {
    return row && row['value'];
  })
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.setVersion = function(version) {
  return this.storage_.put('configuration', {
    'property' : 'version',
    'value' : version
  });
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.getItem = function(containerId, id) {
  var range = ydn.db.KeyRange.only([ containerId, id ]);
  var query = new ydn.db.IndexValueIterator('item', 'container, uid', range);

  return this.storage_.get(query)
};

/** @private */
net.bluemind.container.persistence.DBItemHome.prototype.get_ = function(uids) {
  return this.storage_.values('item', uids).then(function(items) {
    return goog.array.filter(items, goog.isDef);
  });
};

net.bluemind.container.persistence.DBItemHome.prototype.all = function() {
  return this.storage_.values('item').then(function(items) {
    return goog.array.filter(items, goog.isDef);
  });
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.deleteItem = function(containerId, id, opt_sendNotification) {
  var sendNotification = goog.isDefAndNotNull(opt_sendNotification) ? opt_sendNotification : true;
  return this.getItem(containerId, id).addCallback(function(item) {
    if (item) {
      return this.remove_(item)
    }
    throw 'Entry (' + id + ') not found.';
  }, this).addCallback(function(item) {
    var change = {
      'uid' : item['id'],
      'container' : item['container'],
      'itemId' : item['uid'],
      'type' : 'deleted',
      'sendNotification': sendNotification 
    };
    return this.storage_.put('changes', [ change ]);
  }, this);
};

net.bluemind.container.persistence.DBItemHome.prototype.deleteItemWithoutChangeLog = function(containerId, id) {
  return this.getItem(containerId, id).addCallback(function(item) {
    if (item) {
      return this.remove_(item)
    }
    throw 'Entry (' + id + ') not found.';
  }, this);
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.storeItem = function(containerId, item) {
  item['container'] = containerId;
  item['id'] = containerId + '-' + item['uid'];

  return this.store_(item).addCallback(function(i) {
    this.addToChangeLog(item);
  }, this);
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.storeItemWithoutChangeLog = function(containerId, item) {
  item['container'] = containerId;
  item['id'] = containerId + '-' + item['uid'];

  return this.store_(item);
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.storeItemsWithoutChangeLog = function(containerId, items) {
  goog.array.forEach(items, function(item) {
    item['container'] = containerId;
    item['id'] = containerId + '-' + item['uid'];
  });

  return this.store_(items);
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.addToChangeLog = function(item) {
  var change = {
    'uid' : item['id'],
    'container' : item['container'],
    'itemId' : item['uid'],
    'type' : 'modified'
  };
  return this.storage_.put('changes', [ change ]);
};

/**
 * Create a search index from a container Item
 * 
 * @param {Object} item Item container Item to index
 * @return {Array.<string>} Return the words to store in the index
 * @private
 */
net.bluemind.container.persistence.DBItemHome.prototype.index_ = function(item) {
  var words = net.bluemind.string.normalize(item['name']).split(/[\s.,"'?!;:#$%&()*+-/<>=@[\]\^_{}|~]/);
  return words;
};

/**
 * Store the Item into the local storage.
 * 
 * @param {Array} entries Item to store.
 * @return {!goog.async.Deferred} return list of keys in deferred object.
 * @private
 */
net.bluemind.container.persistence.DBItemHome.prototype.store_ = function(entries) {

  if (!goog.isArray(entries)) {
    entries = [ entries ];
  }

  var values = [];
  var slices = [];

  goog.array.forEach(entries, function(item) {
    // FIXME : order, fulltext, index and other before insert tweek
    // should be performed by service level hook.
    item['id'] = item['container'] + '-' + item['uid'];
    if (null != item['name']) {
      item['order'] = item['order'] ? item['order'] : net.bluemind.string.normalize(item['name']);
      item['fulltext'] = this.index_(item);
      values.push(item);
    }

    if (values.length == 500) {
      goog.array.insert(slices, values);
      values = [];
    }

  }, this);

  goog.array.insert(slices, values);

  var next = goog.Promise.resolve();

  var storage = this.storage_;
  var storeValues = function(next, values) {
    return next.then(function() {
      return storage.put('item', values);
    });
  }

  goog.array.forEach(slices, function(values) {
    next = storeValues(next, values);
  });

  return goog.async.Deferred.fromPromise(next);
};

/**
 * Delete the contact from the local storage.
 * 
 * @param {Object} item Contact to delete.
 * @private
 */
net.bluemind.container.persistence.DBItemHome.prototype.remove_ = function(item) {
  var range = ydn.db.KeyRange.only([ item['container'], item['uid'] ]);
  return this.storage_.remove('item', 'container, uid', range).addCallback(function(deleted) {
    return item;
  });
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.getIndex = function(containerId, opt_alphabet) {
  var index = [];
  var alphabet = opt_alphabet;
  var requests = [];
  goog.array.forEach(alphabet, function(letter, i, arr) {
    var lower, upper;
    if (i == 0) {
      lower = [ containerId ];
    } else {
      lower = [ containerId, arr[i - 1].toLowerCase() ];
    }
    upper = [ containerId, letter.toLowerCase() ];
    var range = ydn.db.KeyRange.bound(lower, upper, false, true);
    requests.push(this.fast_.count('item', 'container, order, uid', range).addCallback(function(position) {
      index[i] = position;
    }, this));
  }, this);
  return new goog.async.DeferredList(requests).addCallback(function() {
    var iter = goog.iter.accumulate(index);
    return goog.iter.toArray(iter);
  });
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.count = function(containerId) {
  var range = ydn.db.KeyRange.starts([ containerId ]);
  return this.storage_.count('item', 'container, uid', range);
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.getItems = function(containerId, opt_offset) {
  var offset = opt_offset || 0;
  var range = ydn.db.KeyRange.starts([ containerId ]);
  return this.storage_.values('item', 'container, order, uid', range,
      net.bluemind.container.persistence.ResultList.PAGE, offset).addCallback(function(list) {
    var entries = list;
    entries = goog.array.filter(entries, goog.isDef);
    return entries;
    // this.storage_
    // .count('item', 'container, order, id', range)
    // .addCallback(
    // function(total) {
    // var list = new net.bluemind.container.persistence.ResultList( total,
    // entries);
    // result.callback(list);
    // }).addErrback(function(e) {
    // result.errback(e);
    // });
  }, this);
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.getPosition = function(containerId, item) {
  var order = item['order'], id = item['uid'];
  var range = ydn.db.KeyRange.bound([ containerId ], [ containerId, order, id ], false, true);
  return this.storage_.count('item', 'container, order, uid', range);
};

/**
 * @param {string=} containerId optional container id
 */
net.bluemind.container.persistence.DBItemHome.prototype.getLocalChangeSet = function(containerId) {
  if (containerId) {
    var range = ydn.db.KeyRange.starts(containerId);
    return this.storage_.values('changes', 'container', range, 1000, 0).then(function(items) {
      return goog.array.filter(items, goog.isDef);
    });
  } else {
    return this.storage_.values('changes').then(function(items) {
      return goog.array.filter(items, goog.isDef);
    });
  }
};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.syncItems = function(containerId, changed, deleted, version) {

  changed = goog.array.map(changed, function (change) {
    change['container'] = containerId;
    change['id'] = containerId + '-' + change['uid'];
    return change;
  });

  var deletedItems = goog.array.map(deleted, function(id) {
    return new ydn.db.Key('item', containerId + '-' + id);
  });

  var dChanges = goog.async.Deferred.succeed();

  var ret = dChanges.then(function() {
    if (deletedItems.length > 0) {
      return this.storage_.remove(deletedItems);
    } else {
      return;
    }
  }, null, this).then(function() {
    return this.store_(changed);
  }, null, this).then(function() {
    return this.clearChangelog(containerId, goog.array.map(changed, function(c) {return c['uid']}), deleted);
  }, null, this);

  return goog.async.Deferred.fromPromise(ret);
};

net.bluemind.container.persistence.DBItemHome.prototype.clearChangelog = function(containerId, changed, deleted, errors) {
  errors = errors || [];
  deleted = deleted || [];
  var cleared = goog.array.map(goog.array.concat(changed, deleted), function(uid) {
    return new ydn.db.Key('changes', containerId + '-' + uid);
  });
  var failed = goog.array.map(errors, function(error) {
    return containerId + "-" + error['uid'];
  });
  var succeed = goog.async.Deferred.succeed();
  
  if (cleared.length > 0) {
    succeed = succeed.then(function() {
      this.storage_.remove(cleared);
    }, null, this);
  }
  if (failed.length > 0) {
    succeed = succeed.then(function() {
      return this.storage_.values('changes', function (values) {
        var zip = goog.array.zip(errors, values);
        var changes = goog.array.map(zip, function (ev) {
          var error = ev[0], oldValue = ev[1];
          return {
            'uid': containerId + "-" + error['uid'],
            'container': containerId,
            'itemId': error['uid'],
            'type': 'error',
            'errorCount': (oldValue && oldValue['errorCount'] || 0) + 1,
            'errorCode': error['errorCode'],
            'errorMessage': error['message']
          };

        });
        return this.storage_.put('changes', changes);
      });
    });
  }
  return goog.async.Deferred.fromPromise(succeed);
} 

net.bluemind.container.persistence.DBItemHome.prototype.filterItems = function(query) {
  switch(query[1]) {
    case '=': 
      var upper = query[2];
      var upperIncluded = true;
    case '>=':
      var lowerIncluded = true;
    case '>':
      var lower = query[2];
      break;
    case '<=':
      var upperIncluded = true;
    case '<':
      var upper = query[2];
  }
  switch(query[3]) {
    case '>=':
      var lowerIncluded = true;
    case '>':
      var lower = query[4];
      break;
    case '<=':
      var upperIncluded = true;
    case '<':
      var upper = query[4];
  }
  var key = new ydn.db.KeyRange(lower, upper, !lowerIncluded, !upperIncluded); 
  return this.storage_.valuesByIndex("item", query[0], key).then(function(items) {
    return goog.array.filter(items, goog.isDef);
  });

};


/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.searchItems = function(query, opt_offset, opt_limit) {
  if (query.length == 1) {
    return this.filterItems(query[0]);
  }
  // FIXME: Query parser && refactoring
  var filters = [], iterators = [];
  goog.array.forEach(query, function(part) {
    if (part[1] == '=') {
      // TODO: Sorted Merge if all part are '='
      var it = ydn.db.IndexIterator.where('item', part[0], part[1], part[2], part[3], part[4]);
      iterators.push(it);
    } else if (part[1] == '^' && goog.isArray(part[2]) && goog.string.countOf(part[0], ',') == part[2].length) {
      // TODO: ZigZag merge if all part are ^ with compound index and the same
      // last part.
      var it = ydn.db.IndexIterator.where('item', part[0], part[1], part[2], part[3], part[4]);
      iterators.push(it);
      // FIXME : What with differents last part || different part[1] ?
    } else {
      filters.push(part);
    }
  });
  if (iterators.length == 0 && filters.length > 0) {
    var part = filters.shift();
    iterators.push(ydn.db.IndexIterator.where('item', part[0], part[1], part[2], part[3], part[4]));
  }

  var filterCallbacks = [];
  goog.array.forEach(filters, function(filter) {
    switch (filter[1]) {
    case '=':
      var compare = function(field, value) {
        return field == value;
      };
      break;
    case '<=':
      var compare = function(field, value) {
        return field <= value;
      }
      break;
    case '<':
      var compare = function(field, value) {
        return field < value;
      }
      break;
    case '>':
      var compare = function(field, value) {
        return field > value;
      }
      break;
    case '>=':
      var compare = function(field, value) {
        return field >= value;
      }
      break;
    case '^':
      var compare = function(field, value) {
        return field.startWith(value);
      }
      break;
    }
    if (goog.string.contains(filter[0], ',')) {
      filterCallbacks.push(function(row) {
        var values = goog.array.map(filter[0].split(','), function(field) {
          return goog.object.getValueByKeys(row, field.split('.'));
        });
        return compare(ydn.db.cmp(values, filter[2]), 0)
      });
    } else {
      filterCallbacks.push(function(row) {
        var field = goog.object.getValueByKeys(row, filter[0].split('.'));
        return compare(field, filter[2]);
      });
    }
  })

  if (iterators.length == 0) {
    return goog.async.Deferred.succeed([]);
  } else if (iterators.length == 1) {
    var it = iterators.pop();
    
    var result = this.storage_.valuesByIndex(it.getStoreName(), it.getIndexName(), it.getKeyRange(), opt_limit || net.bluemind.container.persistence.DBItemHome.MAX_LIMIT, opt_offset);
  } else {
    var count, out = [];
    // FIXME : NestedLoop is the worst alog choice.
    var join = new ydn.db.algo.SortedMerge(out);
    var result = this.storage_.scan(join, iterators).addCallback(function() {
      count = out.length;
      if (opt_offset) {
        out = goog.array.slice(out, opt_offset, opt_limit);
      }
      return out;
    }, this);

    result = result.then(function(ids) {
      if (opt_limit) {
        var offset = opt_offset;
        if (!offset) {
          offset = 0;
        }
        ids = goog.array.slice(ids, offset, offset + opt_limit);
      }
      return this.get_(ids);
    }, null, this)
  }
  if (filterCallbacks.length > 0) {
    result = result.then(function(entries) {
      var f = goog.functions.and.apply(this, filterCallbacks);
      return goog.array.filter(entries, f);
    })
  }
  return result;

};

/** @override */
net.bluemind.container.persistence.DBItemHome.prototype.oldsearchItems = function(containerId, query, opt_offset) {
  if (opt_wildcard && !goog.string.endsWith(query, '*')) {
    query = query + '*';
  }

  var offset = opt_offset || 0;
  var words = net.bluemind.string.normalize(query).split(/[\s.,"'?!;:#$%&()+-/<>=@[\]\^_{}|~]/);
  var iterators = [];
  for (var i = 0; i < words.length; i++) {
    var wildcard = opt_wildcard || goog.string.endsWith(words[i], '*');
    var word = words[i].replace('*', '');
    var range = (wildcard) ? ydn.db.KeyRange.starts(word) : ydn.db.KeyRange.only(word);
    iterators.push(new ydn.db.IndexIterator('item', 'fulltext', range));
  }
  var out = [];
  var join = new ydn.db.algo.NestedLoop(out);
  var count = out.length;

  return this.storage_.scan(join, iterators).addCallback(function() {
    out = goog.array.slice(out, offset, net.bluemind.container.persistence.ResultList.PAGE);
    return this.get_(out);
  }, this).addCallback(function(entries) {
    return new net.bluemind.container.persistence.ResultList(count, entries);
  }, this);
};
