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
 * @fileoverview Logger that send log to bm server.
 */


goog.provide('net.bluemind.debug.RemoteLogger');

goog.require('goog.debug.LogManager');
goog.require('goog.debug.TextFormatter');
goog.require('goog.debug.Logger.Level');
goog.require('goog.net.XhrManager');
goog.require('net.bluemind.net.OnlineHandler');

/**
 * Send log to a remote command. By default it will only send SHOUT level log.
 * @constructor
 */
net.bluemind.debug.RemoteLogger = function() {
  this.publishHandler_ = goog.bind(this.sendLog_, this);
  this.formatter_ = new goog.debug.TextFormatter();
  this.filteredLoggers_ = {};
  this.xhr_ = new goog.net.XhrManager();
};
goog.addSingletonGetter(net.bluemind.debug.RemoteLogger);
  
/**
 * @type {goog.net.XhrManager}
 * @private
  */
net.bluemind.debug.RemoteLogger.prototype.xhr_;

/**
 * Default threshold below which a log shouldn't be report
 * @type {goog.debug.Logger.Level}
 */
net.bluemind.debug.RemoteLogger.prototype.threshold = goog.debug.Logger.Level.SHOUT;

/**
 * Formatter for formatted output.
 * @type {!goog.debug.TextFormatter}
 * @private
 */
net.bluemind.debug.RemoteLogger.prototype.formatter_;

/**
 * The bound handler function for handling log messages. This is kept in a
 * variable so that it can be deregistered when the logger client is disposed.
 * @type {Function}
 * @private
 */
net.bluemind.debug.RemoteLogger.prototype.publishHandler_;

/**
 * Loggers that we shouldn't output
 * @private {!Object}
 */
net.bluemind.debug.RemoteLogger.prototype.filteredLoggers_;

/**
 * Whether we are currently capturing logger output.
 *
 * @type {boolean}
 * @private
 */
net.bluemind.debug.RemoteLogger.prototype.isCapturing_ = false;

/**
 * Adds a logger name to be filtered.
 * @param {string} loggerName the logger name to add.
 */
net.bluemind.debug.RemoteLogger.prototype.addFilter = function(loggerName) {
  this.filteredLoggers_[loggerName] = true;
};


/**
 * Removes a logger name to be filtered.
 * @param {string} loggerName the logger name to remove.
 */
net.bluemind.debug.RemoteLogger.prototype.removeFilter = function(loggerName) {
  delete this.filteredLoggers_[loggerName];
};

/**
 * Sets whether we are currently capturing logger output.
 * @param {boolean} capturing Whether to capture logger output.
 */
net.bluemind.debug.RemoteLogger.prototype.setCapturing = function(capturing) {
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
 * Sends a log message through the channel.
 * @param {!goog.debug.LogRecord} logRecord The log message.
 * @private
 */
net.bluemind.debug.RemoteLogger.prototype.sendLog_ = function(logRecord) {

  var name = logRecord.getLoggerName();
  var level = logRecord.getLevel();
  var msg = logRecord.getMessage();
  var originalException = logRecord.getException();

  if (level >= this.threshold && !this.filteredLoggers_[logRecord.getLoggerName()]
      && net.bluemind.net.OnlineHandler.getInstance().isOnline()) {
    var exception;
    if (originalException) {
      exception = this.serializeException_(originalException);
    }

    restClient.sendMessage({
      "method" : "log",
      "path" : "/log",
      "params" : {},
      "body" : {
        name : name,
        level : level.value,
        message : msg,
        exception : JSON.stringify(exception)
      }
    }, function() {
    });
  }
};


/**
 * Prepare exception to be sent through wire.
 * @param {*} exception Log exception.
 * @return {Object} Serialized exception.
 * @private
 */
net.bluemind.debug.RemoteLogger.prototype.serializeException_ = function(original) {
  var normalizedException =
      goog.debug.normalizeErrorObject(original);
  var exception = {
    'name': normalizedException.name,
    'message': normalizedException.message,
    'line': normalizedException.lineNumber,
    'filename': normalizedException.fileName,
    'stack': original.stack ||
        goog.debug.getStacktrace(goog.debug.Logger.prototype.log)
  };

  if (goog.isObject(original)) {
    for (var i = 0; 'message' + i in original; i++) {
      exception['message' + i] = String(original['message' + i]);
    }
  }
  return exception;
};
