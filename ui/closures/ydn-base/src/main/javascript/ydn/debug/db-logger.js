/**
 * @fileoverview Record closure log into database.
 */


goog.provide('ydn.debug.DbLogger');
goog.require('goog.debug.TextFormatter');
goog.require('goog.log');
goog.require('ydn.db.core.Storage');



/**
 * Create and install closure log record into the database.
 * Typical use case:
 * <pre>
 *   ydn.debug.ILogger.log({error: 'something wrong'}};
 * </pre>
 * <pre>
 * var schema = {
 *   stores: [ydn.debug.DbLogger.getStoreSchema()]
 * };
 * ydn.debug.DbLogger.instance = new ydn.debug.DbLogger(schema);
 * ydn.debug.DbLogger.instance.setRecordLimit(10000); // default value
 * ydn.debug.DbLogger.instance.setLevelLimit(300); // default value
 * ydn.debug.DbLogger.instance.addFilter('ydn.db.tr.DbOperator'); // don't record it.
 * </pre>
 * @param {ydn.db.core.Storage} db
 * @constructor
 */
ydn.debug.DbLogger = function(db) {
  var schema = db.getSchema();
  var schema_ok = false;
  for (var i = 0; i < schema.stores.length; i++) {
    var store = schema.stores[i];
    if (store.name == ydn.debug.DbLogger.STORE_NAME) {
      schema_ok = true;
      goog.asserts.assert(store.autoIncrement, 'invalid db-logger store schema');
    }
  }
  goog.asserts.assert(schema_ok, 'Database schema does not have for db-logger');
  /**
   * Storage.
   * @type {ydn.db.crud.DbOperator}
   * @private
   */
  this.db_ = /** @type {ydn.db.crud.DbOperator} */ (db.branch(ydn.db.tr.Thread.Policy.SINGLE, false));
  this.db_ready_ = false;
  db.onReady(this.initDb, this);
  /**
   * @type {number}
   * @private
   */
  this.max_records_ = ydn.debug.DbLogger.LIMIT;
  /**
   * @type {number}
   * @private
   */
  this.level_limit_ = goog.debug.Logger.Level.FINEST.value;
  this.record_count_ = 0;

  this.publishHandler_ = goog.bind(this.addLogRecord, this);

  this.isCapturing_ = false;
  this.logBuffer_ = '';

  /**
   * Loggers that we shouldn't output.
   * @type {!Object.<boolean>}
   * @private
   */
  this.filteredLoggers_ = {};
};


/**
 * Purge record from the database if number of record in the store is
 * exceed to the limit.
 * @private
 */
ydn.debug.DbLogger.prototype.checkForPurge_ = function() {
  if (this.max_records_ && this.record_count_ >= this.max_records_) {
    // get the most earirest 100 records to delete
    var buf = this.max_records_ / 100;
    if (buf > 100) {
      buf = 100;
    }
    this.db_.keys(ydn.debug.DbLogger.STORE_NAME, null, buf).addCallback(function(ids) {
      var keys = ids.map(function(id) {
        return new ydn.db.Key(ydn.debug.DbLogger.STORE_NAME, id);
      });
      this.db_.remove(keys).addCallback(function() {
        this.db_.count(ydn.debug.DbLogger.STORE_NAME).addCallback(function(cnt) {
          this.record_count_ = cnt;
        }, this);
      }, this);
    }, this);
  }
};


/**
 * Set limit for number of records to store.
 * @param {number} n
 */
ydn.debug.DbLogger.prototype.setRecordLimit = function(n) {
  this.max_records_ = n;
  this.checkForPurge_();
};


/**
 * Get limit for number of records to store.
 * @return {number} limit.
 */
ydn.debug.DbLogger.prototype.getRecordLimit = function() {
  return this.max_records_;
};


/**
 * Set limit for logger level.
 * @param {number} n level lower than or equal to this level will not be recorded.
 */
ydn.debug.DbLogger.prototype.setLevelLimit = function(n) {
  this.level_limit_ = n;
};


/**
 * Get limit for logger level.
 * @return {number} limit.
 */
ydn.debug.DbLogger.prototype.getLevelLimit = function() {
  return this.level_limit_;
};


/**
 * @param {*} err
 * @protected
 */
ydn.debug.DbLogger.prototype.initDb = function(err) {
  var me = this;
  // FIXME: should not need settimeout
  // parallel thread has problem.
  // setTimeout(function() {
    if (me.max_records_) {
      me.db_ready_ = true;
      me.db_.count(ydn.debug.DbLogger.STORE_NAME).addCallback(function(cnt) {
        this.record_count_ = cnt;
        this.checkForPurge_();
      }, me);
    }
  // }, 500);

};


/**
 * @define {number} limit for number of recrods.
 */
ydn.debug.DbLogger.LIMIT = 10000;


/**
 * @define {string} Store name for this.
 */
ydn.debug.DbLogger.STORE_NAME = 'db-logger';


/**
 * @define {string} Primary key for this.
 */
ydn.debug.DbLogger.KEY_PATH = 'id';


/**
 * Get store schema for this.
 * @return {StoreSchema}
 */
ydn.debug.DbLogger.getStoreSchema = function() {
  var schema = {
    'name': ydn.debug.DbLogger.STORE_NAME,
    'autoIncrement': true,
    'indexes': [{
      'name': 'level'
    }, {
      'name': 'time'
    }, {
      'name': 'name'
    }, {
      'name': 'name, time',
      'keyPath': ['name', 'time']
    }]
  };
  return /** @type {StoreSchema} */ (/** @type {Object} */ (schema));
};


/**
 * Sets whether we are currently capturing logger output.
 * @param {boolean} capturing Whether to capture logger output.
 */
ydn.debug.DbLogger.prototype.setCapturing = function(capturing) {
  if (capturing == this.isCapturing_) {
    return;
  }

  // attach or detach handler from the root logger
  var rootLogger = goog.debug.LogManager.getRoot();
  if (capturing) {
    rootLogger.addHandler(this.publishHandler_);
  } else {
    rootLogger.removeHandler(this.publishHandler_);
    this.logBuffer = '';
  }
  this.isCapturing_ = capturing;
};


/**
 * Adds a log record.
 * @param {goog.debug.LogRecord} logRecord The log entry.
 */
ydn.debug.DbLogger.prototype.addLogRecord = function(logRecord) {

  if (!this.db_ready_) {
    return;
  }
  // to prevent circular logging when error originate from putting the logger
  var logger_name = logRecord.getLoggerName();
  if (goog.string.startsWith(logger_name, 'ydn.db.')) {
    return;
  }


  // Check to see if the log record is filtered or not.
  if (this.filteredLoggers_[logger_name]) {
    return;
  }

  var level = logRecord.getLevel();
  if (!level || level.value <= this.level_limit_) {
    return;
  }
  var record = {
    // 'exception': logRecord.getExceptionText(),
    'level': level.value,
    'logger': logRecord.getLoggerName(),
    'name': level.name,
    'time': logRecord.getMillis(),
    'message': logRecord.getMessage(),
    'seq': logRecord.getSequenceNumber()
  };
  this.db_.put(ydn.debug.DbLogger.STORE_NAME, record);
  this.record_count_++;
  this.checkForPurge_();
};


/**
 * Adds a logger name to be filtered.
 * @param {string} loggerName the logger name to add.
 */
ydn.debug.DbLogger.prototype.addFilter = function(loggerName) {
  this.filteredLoggers_[loggerName] = true;
};


/**
 * Removes a logger name to be filtered.
 * @param {string} loggerName the logger name to remove.
 */
ydn.debug.DbLogger.prototype.removeFilter = function(loggerName) {
  delete this.filteredLoggers_[loggerName];
};


/**
 * Get latest records.
 * @param {number=} opt_limit
 * @param {number=} opt_offset
 * @return {!goog.async.Deferred} return {!Array.<DbLoggerRecord>}
 */
ydn.debug.DbLogger.prototype.getLastRecords = function(opt_limit, opt_offset) {
  var n = opt_limit || 100;
  var o = opt_offset || 0;
  return this.db_.values(ydn.debug.DbLogger.STORE_NAME, null, n, o, true);
};


/**
 * Get latest records with info logging.
 * @param {number=} opt_limit
 * @param {number=} opt_offset
 * @return {!goog.async.Deferred} return {!Array.<DbLoggerRecord>}
 */
ydn.debug.DbLogger.prototype.getLastInfoRecords = function(opt_limit, opt_offset) {
  var n = opt_limit || 100;
  var o = opt_offset || 0;
  var kr = ydn.db.KeyRange.starts(['INFO']);
  return this.db_.values(ydn.debug.DbLogger.STORE_NAME, 'name, time', kr, n, o, true);
};


/**
 * Display latest record on console.
 * @param {number=} opt_limit
 */
ydn.debug.DbLogger.prototype.tail = function(opt_limit) {
  this.getLastRecords(opt_limit).addCallback(function(x) {
    var records = /** @type {!Array.<DbLoggerRecord>} */ (x);
    var formatter = new goog.debug.TextFormatter();
    formatter.showAbsoluteTime = false;
    formatter.showExceptionText = false;
    records.map(function(record) {
      var level = new goog.debug.Logger.Level(record.name, record.level);
      var s = formatter.formatRecord(new goog.debug.LogRecord(level, record.message,
          record.logger, record.time, record.seq));
      window.console.log(s);
    });

  });
};


/**
 * Global console logger instance
 * @type {ydn.debug.DbLogger}
 */
ydn.debug.DbLogger.instance = null;


