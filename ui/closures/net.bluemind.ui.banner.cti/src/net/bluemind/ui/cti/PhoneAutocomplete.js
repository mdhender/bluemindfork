goog.provide("net.bluemind.ui.cti.PhoneAutocomplete");

goog.require("goog.ui.ac.AutoComplete");
goog.require("goog.ui.ac.InputHandler");
goog.require("goog.ui.ac.Renderer");
goog.require("net.bluemind.ui.cti.PhoneMatcher");
goog.require("net.bluemind.ui.cti.PhoneRowRenderer");

/**
 * @constructor
 * 
 * @extends {goog.ui.ac.AutoComplete}
 */
net.bluemind.ui.cti.PhoneAutocomplete = function() {
  var matcher = new net.bluemind.ui.cti.PhoneMatcher();
  var renderer = new goog.ui.ac.Renderer(null, new net.bluemind.ui.cti.PhoneRowRenderer());
  renderer.className = goog.getCssName('dialer-renderer');
  var handler = new goog.ui.ac.InputHandler(null, null, false, 500);
  handler.setUpdateDuringTyping(false);
  goog.base(this, matcher, renderer, handler);
  handler.attachAutoComplete(this);
};
goog.inherits(net.bluemind.ui.cti.PhoneAutocomplete, goog.ui.ac.AutoComplete);

/** @override */
net.bluemind.ui.cti.PhoneAutocomplete.prototype.attachInputs = function(input) {
  this.renderer_.setWidthProvider(input);
  goog.base(this, 'attachInputs', input);
};

/** @override */
net.bluemind.ui.cti.PhoneAutocomplete.prototype.setToken = function(token, opt_fullString) {
  if (!token) {
    this.setTokenInternal('');
    this.dismiss();
  } else {
    goog.base(this, 'setToken', token, opt_fullString);
  }
};