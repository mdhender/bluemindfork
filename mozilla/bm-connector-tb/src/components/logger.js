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

/* File logger*/

Components.utils.import("resource://gre/modules/XPCOMUtils.jsm");
Components.utils.import("resource://bm/bmUtils.jsm");

const Cc = Components.classes;
const Ci = Components.interfaces;
const Cr = Components.results;

function OpenLogFile(name, index) {
    let file = Cc["@mozilla.org/file/directory_service;1"]
                .getService(Ci.nsIProperties)
                .get("TmpD", Ci.nsIFile);
    file.append("BlueMind");
    if( !file.exists() || !file.isDirectory() ) { 
        file.create(Ci.nsIFile.DIRECTORY_TYPE, 0o0755);
    }
    file.append(name + (index > 0 ? "." + index : ""));
    return file;
}

function Logger() {
    if (!this._stream) {
        try {
            let profDir = Cc["@mozilla.org/file/directory_service;1"]
                .getService(Ci.nsIProperties)
                .get("ProfD", Ci.nsIFile);
            let profDirName = profDir.leafName;
            let fileName = "bm-connector-tb-log-" + profDirName + ".txt";
            let file = OpenLogFile(fileName, 0);
            let rollOnSize = 1000 * 1000; //1 MB
            if (file.exists() && file.fileSize > rollOnSize) {
                let maxBackupIndex = 10;
                let renameSucceeded = true;
                
                let f = OpenLogFile(fileName, maxBackupIndex);
                if (f.exists()) {
                    f.remove(false);
                }
                for (let i = maxBackupIndex - 1; i >= 1 && renameSucceeded; i--) {
                    f = OpenLogFile(fileName, i);
                    if (f.exists()) {
                        try {
                            f.moveTo(null, fileName + "." + (i + 1));
                        } catch(e) {
                            renameSucceeded = false;
                        }
                    }
                }
                if (renameSucceeded) {
                    try {
                        file.moveTo(null, fileName + "." + 1);
                    } catch(e) {
                        renameSucceeded = false;
                    }
                    file = OpenLogFile(fileName, 0);
                }
            }
            this._stream = Cc["@mozilla.org/network/file-output-stream;1"].createInstance(Ci.nsIFileOutputStream);
            //PR_WRONLY		0x02
            //PR_CREATE_FILE	0x08
            //PR_APPEND         0x10
            this._stream.init(file, 26 , 0o0644, 0);
            
            let first = "\r\n" + new Date().toJSON() + ": NEW SESSION" + "\r\n";
            this._stream.write(first, first.length);
            this._stream.flush();
        } catch (e) {
            Components.utils.reportError(e);
        }
    }
    
    let level = bmUtils.getIntPref("extensions.bm.log.level", BMLogger.INFO);
    if (level < BMLogger.TRACE || level > BMLogger.ERROR) {
        level = BMLogger.DEBUG;
    }
    this.level = level;
    let onPrefChanged = function(branch, name) {
        Components.utils.reportError("onPrefChanged:" + name);
        switch (name) {
            case "debug":
                if (bmUtils.getBoolPref("extensions.bm.log.debug", false)) {
                    bmUtils.setIntPref("extensions.bm.log.level", BMLogger.TRACE);
                } else {
                    bmUtils.setIntPref("extensions.bm.log.level", BMLogger.INFO);
                }
                break;
            case "level":
                this.level = bmUtils.getIntPref("extensions.bm.log.level", BMLogger.INFO);
                Components.utils.reportError("new LEVEL:" + this.level);
                break;
        }
    }.bind(this);
    this._prefListener = new BmPrefListener("extensions.bm.log.", onPrefChanged);
    this._prefListener.register(false);
    this.wrappedJSObject = this;
}

BMLogger.TRACE = -1;
BMLogger.DEBUG = 0;
BMLogger.INFO = 1;
BMLogger.ERROR = 2;

function BMLogger(logger, header) {
    this._logger = logger;
    this._header = header;
}

BMLogger.prototype.error = function(message) {
    this._logMessage(BMLogger.ERROR, message, true);
}

BMLogger.prototype.debug = function(message, showStack) {
    this._logMessage(BMLogger.DEBUG, message, showStack);
}

BMLogger.prototype.trace = function(message, showStack) {
    this._logMessage(BMLogger.TRACE, message, showStack);
}

BMLogger.prototype.info = function(message, showStack) {
    this._logMessage(BMLogger.INFO, message, showStack);
}

BMLogger.prototype.isDebugEnabled = function() {
    return this._logger.level <= BMLogger.DEBUG;
}

BMLogger.prototype.isTraceEnabled = function() {
    return this._logger.level == BMLogger.TRACE;
}

BMLogger.prototype._logMessage = function(level, message, showStack) {
    if (level < this._logger.level) return;
    let msg = this._level2String(level)
                + "[" + new Date().toJSON() + "]"
                + this._header + message + "\r\n";
    if (showStack) {
        msg += this._formatStack(message);
    }
    this._logger._stream.write(msg, msg.length);
    this._logger._stream.flush();
}

BMLogger.prototype._level2String = function(level) {
    let res = "";
    switch(level) {
        case BMLogger.TRACE:
            res = "[TRACE]";
            break;
        case BMLogger.DEBUG:
            res = "[DEBUG]";
            break;
        case BMLogger.INFO:
            res = "[INFO ]";
            break;
        case BMLogger.ERROR:
            res = "[ERROR]";
            break;
    }
    return res;
}

BMLogger.prototype._formatStack = function(message) {
    let stack = message.stack ? message.stack : Components.stack;
    let res = "";
    let depth = 0;
    while (stack != null) {
        for (var i = 0; i < depth; i++) {
            res += ">";
        }
        res += " ";
        if (depth > 3) {
            res += stack.toString() + "\r\n";
        }
        depth++;
        stack = stack.caller;
    }
    return res;
}

Logger.prototype = {
    classDescription: "Logger XPCOM Component",
    classID:          Components.ID("{44257acb-7a1d-4097-ba67-fbe4ed98d1ed}"),
    contractID:       "@blue-mind.net/logger;1",
     /* Needed for XPCOMUtils NSGetModule */
    _xpcom_categories: [{category: "logger", entry: "m-logger"}],
    QueryInterface: ChromeUtils.generateQI ?
        ChromeUtils.generateQI([Components.interfaces.nsISupports])
        : XPCOMUtils.generateQI([Components.interfaces.nsISupports]),
    getLogger: function(header) {
        return new BMLogger(this, header);
    }
};

/**
 * XPCOMUtils.generateNSGetFactory was introduced in Mozilla 2 (Firefox 4).
 * XPCOMUtils.generateNSGetModule is for Mozilla 1.9.0 (Firefox 3.0).
 */
if (XPCOMUtils.generateNSGetFactory)
  var NSGetFactory = XPCOMUtils.generateNSGetFactory([Logger]);
else
  var NSGetModule = XPCOMUtils.generateNSGetModule([Logger]);