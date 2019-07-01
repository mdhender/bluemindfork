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
 * @fileoverview Helper for date time format/parse.
 */

goog.provide("net.bluemind.i18n.DateTimeHelper");
goog.provide("net.bluemind.i18n.DateTimeHelper.Formatter");
goog.provide("net.bluemind.i18n.DateTimeHelper.Parser");
goog.provide("bluemind.i18n.DateTimeHelper");

goog.require("goog.date.Date");
goog.require("goog.i18n.DateTimeFormat");
goog.require("goog.i18n.DateTimeParse");
goog.require("goog.i18n.DateTimeFormat.Format");

/**
 * Helper for datetime formating/parsing
 * 
 * @param {goog.i18n.DateTimeFormat.Format=} opt_df Optional date format.
 * @param {goog.i18n.DateTimeFormat.Format=} opt_tf Optional time format.
 * @constructor
 */
net.bluemind.i18n.DateTimeHelper = function(opt_df, opt_tf) {
  var df = opt_df ? opt_df : goog.i18n.DateTimeSymbols.DATEFORMATS[3];
  var tf = opt_tf ? opt_tf : goog.i18n.DateTimeSymbols.TIMEFORMATS[3];
  var dtf = goog.i18n.DateTimeSymbols.DATETIMEFORMATS[3];
  dtf = dtf.replace('{1}', df).replace('{0}', tf);
  this.formatter = {
    datetime : new goog.i18n.DateTimeFormat(dtf),
    date : new goog.i18n.DateTimeFormat(df),
    time : new goog.i18n.DateTimeFormat(tf)
  };
  this.parser = {
    datetime : new goog.i18n.DateTimeParse(dtf),
    date : new goog.i18n.DateTimeParse(df),
    time : new goog.i18n.DateTimeParse(tf)
  };

};
// FIXME: Could/should be 100% static
goog.addSingletonGetter(net.bluemind.i18n.DateTimeHelper);

/**
 * @typedef {net.bluemind.i18n.DateTimeHelper};
 */
bluemind.i18n.DateTimeHelper = net.bluemind.i18n.DateTimeHelper

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Formatter}
 */
net.bluemind.i18n.DateTimeHelper.prototype.formatter;

/**
 * @type {net.bluemind.i18n.DateTimeHelper.Parser}
 * @private
 */
net.bluemind.i18n.DateTimeHelper.prototype.parser;

/**
 * Return the date formatter
 * 
 * @return {goog.i18n.DateTimeFormat} the date formatter.
 */
net.bluemind.i18n.DateTimeHelper.prototype.getDateFormatter = function() {
  return this.formatter.date;
};

/**
 * Return the time formatter
 * 
 * @return {goog.i18n.DateTimeFormat} the time formatter.
 */
net.bluemind.i18n.DateTimeHelper.prototype.getTimeFormatter = function() {
  return this.formatter.time;
};

/**
 * Return the time parser
 * 
 * @return {goog.i18n.DateTimeParse} the date parser.
 */
net.bluemind.i18n.DateTimeHelper.prototype.getDateParser = function() {
  return this.parser.date;
};

/**
 * Return the time parser
 * 
 * @return {goog.i18n.DateTimeParse} the time parser.
 */
net.bluemind.i18n.DateTimeHelper.prototype.getTimeParser = function() {
  return this.parser.time;
};

/**
 * Set the default date pattern
 * 
 * @param {string|number} pattern pattern specification or pattern type.
 */
net.bluemind.i18n.DateTimeHelper.prototype.setDateFormat = function(pattern) {
  this.formatter.date = new goog.i18n.DateTimeFormat(pattern);
  this.parser.date = new goog.i18n.DateTimeParse(pattern);
};

/**
 * Set the default time pattern
 * 
 * @param {string|number} pattern pattern specification or pattern type.
 */
net.bluemind.i18n.DateTimeHelper.prototype.setTimeFormat = function(pattern) {
  this.formatter.time = new goog.i18n.DateTimeFormat(pattern);
  this.parser.time = new goog.i18n.DateTimeParse(pattern);
};

/**
 * Format the date with the default date or datetime pattern.
 * 
 * @param {goog.date.Date} date Date to format.
 * @return {string} the formated date.
 */
net.bluemind.i18n.DateTimeHelper.prototype.format = function(date) {
  if (date instanceof goog.date.DateTime) {
    return this.formatDateTime(date);
  } else {
    return this.formatDate(date);
  }
};

/**
 * Format the date with the default date pattern.
 * 
 * @param {goog.date.Date} date Date to format.
 * @return {string} the formated date.
 */
net.bluemind.i18n.DateTimeHelper.prototype.formatDate = function(date) {
  return this.formatter.date.format(date);
};

/**
 * Format the date with the default time pattern.
 * 
 * @param {goog.date.DateTime} datetime DateTime to format.
 * @return {string} the formated time.
 */
net.bluemind.i18n.DateTimeHelper.prototype.formatTime = function(datetime) {
  return this.formatter.time.format(datetime);
};

/**
 * Format the date with the default datetime pattern.
 * 
 * @param {goog.date.DateTime} datetime DateTime to format.
 * @return {string} the formated datetime.
 */
net.bluemind.i18n.DateTimeHelper.prototype.formatDateTime = function(datetime) {
  // FIXME: use goog.i18n.DateTimeSymbols.DATETIMEFORMATS
  return this.formatter.date.format(datetime) + ' ' + this.formatter.time.format(datetime);
};

/**
 * Parse the given date string and fill info into date object.
 * 
 * @param {string} text The string being parsed.
 * @param {goog.date.Date} opt_date The Date object to hold the parsed date.
 * @return {number} How many characters parser advanced.
 */
net.bluemind.i18n.DateTimeHelper.prototype.parseDate = function(text, opt_date) {
  if (!opt_date) {
    opt_date = new net.bluemind.date.Date();
  }
  return this.parser.date.strictParse(text, opt_date);
};

/**
 * Parse the given time string and fill info into datetime object.
 * 
 * @param {string} text The string being parsed.
 * @param {goog.date.DateTime} opt_datetime The DateTime object to hold the
 *          parsed date.
 * @return {number} How many characters parser advanced.
 */
net.bluemind.i18n.DateTimeHelper.prototype.parseTime = function(text, opt_datetime) {
  if (!opt_datetime) {
    opt_datetime = new net.bluemind.date.DateTime();
  }
  return this.parser.time.strictParse(text, opt_datetime);
};

/**
 * @typedef {{time: goog.i18n.DateTimeFormat, date:goog.i18n.DateTimeFormat}};
 */
net.bluemind.i18n.DateTimeHelper.Formatter;

/**
 * @typedef {{time: goog.i18n.DateTimeParse, date:goog.i18n.DateTimeParse,
 *          datetime:goog.i18n.DateTimeParse}};
 */
net.bluemind.i18n.DateTimeHelper.Parser;
