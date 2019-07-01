goog.provide("net.bluemind.calendar.navigation.ac.CalendarAutocomplete");

goog.require("goog.ui.ac.AutoComplete");
goog.require("goog.ui.ac.InputHandler");
goog.require("goog.ui.ac.Renderer");
goog.require("net.bluemind.calendar.navigation.ac.CalendarMatcher");
goog.require("net.bluemind.calendar.navigation.ac.CalendarRowRenderer");

/**
 * @constructor
 * 
 * @param {Object} matcher
 * @param {Element} input
 * @param {Object} selectionHandler
 * @extends {goog.ui.ac.AutoComplete}
 */
net.bluemind.calendar.navigation.ac.CalendarAutocomplete = function(ctx) {
  var matcher = new net.bluemind.calendar.navigation.ac.CalendarMatcher(ctx);
  var renderer = new goog.ui.ac.Renderer(null, new net.bluemind.calendar.navigation.ac.CalendarRowRenderer());
  var handler = new goog.ui.ac.InputHandler(null, null, false, 500);
  handler.setUpdateDuringTyping(false);
  goog.base(this, matcher, renderer, handler);
  handler.attachAutoComplete(this);
};
goog.inherits(net.bluemind.calendar.navigation.ac.CalendarAutocomplete, goog.ui.ac.AutoComplete);

/** @override */
net.bluemind.calendar.navigation.ac.CalendarAutocomplete.prototype.attachInputs = function(input) {
  this.renderer_.setWidthProvider(input);
  goog.base(this, 'attachInputs', input);
};

/**
 * Set calendar already present in calendar selection.
 * 
 * @param {Array.<Object>} calendars;
 */
net.bluemind.calendar.navigation.ac.CalendarAutocomplete.prototype.setCalendars = function(calendars) {
  this.matcher_.calendars = calendars;
};

/** @override */
net.bluemind.calendar.navigation.ac.CalendarAutocomplete.prototype.setToken = function(token, opt_fullString) {
  if (!token) {
    this.setTokenInternal('');
    this.dismiss();
  } else {
    goog.base(this, 'setToken', token, opt_fullString);
  }
};