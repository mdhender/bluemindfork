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
 * @fileoverview Manage databases.
 */

goog.provide("net.bluemind.persistance.DatabaseService");
goog.provide("net.bluemind.persistance.DatabaseService.EventType");

goog.require("goog.events");
goog.require("goog.events.EventTarget");
goog.require("goog.Promise");
goog.require("ydn.db.Storage");
goog.require("goog.structs.Map");
goog.require('goog.userAgent');
goog.require('goog.userAgent.product');
goog.require('goog.userAgent.product.isVersion');
goog.require('goog.Timer');

/**
 * @author mehdi
 *
 */
/**
 * Manage databases
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context.
 * @extends {goog.events.EventTarget}
 * @constructor
 */
net.bluemind.persistance.DatabaseService = function(ctx) {
  goog.base(this);
  this.ctx = ctx;
  this.databases_ = new goog.structs.Map();
  this.databasesResetTags_ = new goog.structs.Map();
};
goog.inherits(net.bluemind.persistance.DatabaseService, goog.events.EventTarget);

/**
 * @type {goog.structs.Map.<string,ydn.db.Storage>}
 * @private
 */
net.bluemind.persistance.DatabaseService.prototype.databases_;

/**
 * @type {goog.structs.Map.<string,Object>}
 * @private
 */
net.bluemind.persistance.DatabaseService.prototype.databasesResetTags_;

/**
 * Storage mechanisms availables
 * @type {Array.<string>}
 * @private
 */
net.bluemind.persistance.DatabaseService.prototype.mechanisms_;

/**
 * @protected
 * @type {goog.debug.Logger}
 */
net.bluemind.persistance.DatabaseService.prototype.logger = goog.log
    .getLogger('net.bluemind.persistance.DatabaseService');

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.persistance.DatabaseService.prototype.ctx;

/**
 * @return {ydn.db.Storage}
 */
net.bluemind.persistance.DatabaseService.prototype.getDatabase = function(databaseName) {
  return this.databases_.get(databaseName);
}


/**
 * Register a set of schema
 * @param {Array.<Object>} schemas
 * @return {goog.Promise}
 */
net.bluemind.persistance.DatabaseService.prototype.regsiterSchemas = function(schemas) {
  var promises = goog.array.map(schemas, function(schema) {
    return this.register(schema.name, schema.schema, schema.options);
  }, this);
  return goog.Promise.all(promises);
}

/**
 * Register a database
 * @param {string} databaseName
 * @param {DatabaseSchema} schema Database schema
 * @param {StorageOptions=} opt_options Storage options.
 * @return {goog.Promose} 
 */
net.bluemind.persistance.DatabaseService.prototype.register = function(databaseName, schema, opt_options) {
  if (this.databases_.containsKey(databaseName)) {
    goog.log.info(this.logger, 'database ' + databaseName + ' is already registered');
    return goog.Promise.resolve(this.databases_.get(databaseName));
  }
  var options = this.getOptions_(opt_options)

  if (schema.resetTags) {
    this.databasesResetTags_.set(databaseName, schema.resetTags);
    delete schema.resetTags;
  } else {
    this.databasesResetTags_.set(databaseName, []);
  }

  if (goog.array.isEmpty(options.mechanisms)) {
    goog.log.info(this.logger, 'No suitable storage for database ' + databaseName + '.');
    this.databases_.set(databaseName, null);
    return goog.Promise.resolve();
  }


  return this.createDatabase_(databaseName, schema, options).then(function(db) {
    this.databases_.set(databaseName, db);
  }, function() {
    this.databases_.set(databaseName, null);
  }, this);
};


/**
 * Create a database
 * @param {string} databaseName
 * @param {DatabaseSchema} schema Database schema
 * @param {StorageOptions=} opt_options Storage options.
 * @return {goog.Promise<ydn.db.Storage>}
 */
net.bluemind.persistance.DatabaseService.prototype.createDatabase_ = function(databaseName, schema, options) {

  goog.log.info(this.logger, 'Initialize database ' + databaseName + ' with mechanism ' + options.mechanisms[0]);
  var resolver = goog.Promise.withResolver();
  
  var db = new ydn.db.Storage(databaseName, schema, options);
  
  db.addEventListener('ready', function(e) {
    goog.log.info(this.logger, 'Database ' + databaseName + ' ready');
    resolver.resolve(db);
  }, false, this);
  
  db.addEventListener('fail', function(event) {
    var msg = 'Database ' + databaseName + ' initialization failed';
    goog.log.warning(this.logger, msg);
    resolver.reject(msg);
  }, false, this);

  return resolver.promise;
}

/**
 * Initialize service
 * 
 * @return {goog.Promise} promise
 */
net.bluemind.persistance.DatabaseService.prototype.initialize = function() {
  return this.capabilities_().then(function(capabilities) {
    this.mechanisms_ = capabilities;
  }, null, this)
}


/**
 * Detect browser capabilities.
 * @return {goog.Promise.<Array.<string>>}
 * @private
 */
net.bluemind.persistance.DatabaseService.prototype.capabilities_ = function() {
  var capabilities = ['localstorage', 'sessionstorage'];
  if (this.ctx.privacy == false) {
    capabilities = ['sessionstorage'];
  } else if (goog.userAgent.product.CHROME) {
    capabilities = ['indexeddb', 'localstorage', 'sessionstorage'];
  } else if (goog.userAgent.product.FIREFOX) {
    capabilities = ['indexeddb', 'localstorage', 'sessionstorage'];
  } else if (goog.userAgent.product.OPERA) {
    capabilities = ['indexeddb', 'localstorage', 'sessionstorage'];
  } else if (goog.userAgent.product.SAFARI) {
    if (goog.userAgent.product.isVersion('10')) {
      capabilities = ['websql', 'localstorage', 'sessionstorage'];
    } else {
      capabilities = ['websql', 'localstorage', 'sessionstorage'];
    }
  }
  
  if (goog.array.contains(capabilities, 'indexeddb') || goog.array.contains(capabilities, 'websql')) {
    var options = {
      mechanisms: goog.array.filter(capabilities, function(capability) {
        return capability == 'indexeddb' || capability == 'websql';
      })
    };
    var schema = {
      stores : [ {
        name : 'capability'
      }, {
        name : 'features'
      } ]
    };
    return this.createDatabase_("capabilities", schema, options).thenCatch(function() {
      goog.array.remove(capabilities, 'indexeddb');
      goog.array.remove(capabilities, 'websql');
    }).then(function() {
      return capabilities; 
    });
  } 
  return goog.Promise.resolve(capabilities);
};


/**
 * Validate and sanitize database options
 * @private
 * @param {StorageOptions=} opt_options Storage options.
 * @return {StorageOptions}
 */
net.bluemind.persistance.DatabaseService.prototype.getOptions_ = function(opt_options) {
  var index, options = opt_options || {};
  options.mechanisms = options.mechanisms ||  ['database'];
  index = goog.array.indexOf(options.mechanisms, 'database');
  if (index >= 0) {
    goog.array.splice(options.mechanisms, index, 1, 'indexeddb', 'websql');
  }
  index = goog.array.indexOf(options.mechanisms, 'webstorage');
  if (index >= 0) {
    goog.array.splice(options.mechanisms, index, 1, 'localstorage', 'sessionstorage');
  }
  options.mechanisms = goog.array.filter(options.mechanisms, function(mechanism) {
    return goog.array.contains(this.mechanisms_, mechanism);
  }, this);
  if (!options.size) {
    options.size = 200 * 1024 * 1024;
  }
  return options;
};

net.bluemind.persistance.DatabaseService.prototype.clearAll = function() {
  net.bluemind.mvp.Application.lock(true);
  var promises = goog.array.map(this.databases_.getValues(), function(v) {
    if (v == null) return goog.Promise.resolve();
    goog.log.info(this.logger, 'Clear database ' + v);
    try {
      return v.clear().then(function() {
      }).thenCatch(function() {
      });
    } catch (e) {
      return goog.Promise.resolve();
    }
  }, this);
  return goog.Promise.all(promises).then(function(res) {
    net.bluemind.mvp.Application.lock(false);
    return res;
  });
}

net.bluemind.persistance.DatabaseService.prototype.checkVersionAndUser = function() {
  var userUid = goog.global['bmcSessionInfos']['userId'];
  var version = this.ctx.version;

  var promises = goog.array.map(this.databases_.getKeys(), function(dbName) {
    var db = this.databases_.get(dbName);
    if (db == null)
      return goog.Promise.resolve();
    var dbVersion = null;
    var dbUser = null;

    return db.get('configuration', 'release').then(function(v) {
      if (v != null) {
        dbVersion = v['value'];
      }
      return db.get('configuration', 'user');
    }, null, this).then(function(u) {
      if (u != null) {
        dbUser = u['value']
      }

      if (userUid == null) {
        userUid = dbUser;
      }

      if (version == null) {
        version = dbVersion;
        this.ctx.version = dbVersion;
      }

      if (dbUser != userUid) {
        goog.log.warning(this.logger, "User is different, reset everything! (dbservion:" + dbVersion //
            + ") (user:" + dbUser + ")");
        try {
          return db.clear();
        } catch (e) {

        }
      } else {

        var resetTags = this.databasesResetTags_.get(dbName);
        var resetTag = goog.array.find(resetTags, function(v) {
          if (goog.string.compareVersions(v, dbVersion) == 1) {
            return true;
          } else {
            return false;
          }
        });

        goog.log.warning(this.logger, "    ** dbName:" + dbName + ", dbVersion: " + dbVersion 
          + ", resetTags: " + resetTags + ", resetTag: " + resetTag);

        if (resetTag) {
          goog.log.warning(this.logger, "dabase version is different, reset everything! (dbservion:" + dbVersion //
              + ") (user:" + dbUser + ")");
          try {
            return db.clear();
          } catch (e) {
            goog.log.error(this.logger, e);
          }
        } else {
          goog.log.info(this.logger, "user and dbversion are ok, continue");
        }
      }
    }, null, this).then(function() {
      try {
        return db.put("configuration", [ {
          'property' : 'user',
          'value' : userUid
        }, {
          'property' : 'release',
          'value' : version
        } ]);
      } catch (e) {

      }
    });
  }, this);
  return goog.Promise.all(promises);
};

/**
 * Get a database using memory storage. Since memory storage is synchronous, no
 * need to use a asynchronous mechanism.
 * 
 * @suppress {checkTypes}
 * @param {string} name Database name
 * @param {!ydn.db.schema.Database=} schema Database schema
 * @return {ydn.db.Storage}
 */
net.bluemind.persistance.DatabaseService.prototype.getMemoryDatabase = function(name, schema) {
  var db = new ydn.db.Storage(name, schema, {
    mechanisms : [ 'memory' ]
  });
  return db;
};


/**
 * Get a database using session storage. Since web storage is synchronous, no
 * need to use a asynchronous mechanism.
 * 
 * @suppress {checkTypes}
 * @param {string} name Database name
 * @param {!ydn.db.schema.Database=} schema Database schema
 * @return {ydn.db.Storage}
 */
net.bluemind.persistance.DatabaseService.prototype.getSessionDatabase = function(name, schema) {
  var db = new ydn.db.Storage(name, schema, {
    mechanisms : [ 'sessionstorage' ]
  });
  return db;
};

/**
 * Get a database using local storage. Since web storage is synchronous, no
 * need to use a asynchronous mechanism.
 * 
 * @suppress {checkTypes}
 * @param {string} name Database name
 * @param {!ydn.db.schema.Database=} schema Database schema
 * @return {ydn.db.Storage}
 */
net.bluemind.persistance.DatabaseService.prototype.getLocalDatabase = function(name, schema) {
  var db = new ydn.db.Storage(name, schema, {
    mechanisms : [ 'localstorage' ]
  });
  return db;
};
/**
 * @enum {string}
 */
net.bluemind.persistance.DatabaseService.EventType = {
  CHANGE : goog.events.getUniqueId('changed')
};
