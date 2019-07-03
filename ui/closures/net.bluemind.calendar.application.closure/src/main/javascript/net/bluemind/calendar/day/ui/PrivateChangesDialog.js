goog.provide("net.bluemind.calendar.day.ui.PrivateChangesDialog");

goog.require("goog.ui.Dialog");
goog.require("goog.ui.Dialog.ButtonSet");
goog.require("goog.ui.Dialog.DefaultButtonKeys");
goog.require("goog.ui.Dialog.EventType");

/**
 * @constructor
 *
 * @param {String} opt_class
 * @param {Boolean} opt_useIframeMask
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {goog.ui.Dialog}
 */
net.bluemind.calendar.day.ui.PrivateChangesDialog = function(opt_domHelper) {
  goog.base(this, undefined, undefined, opt_domHelper);
  /** @meaning calendar.privateChanges.title */
  var MSG_TITLE = goog.getMsg('Those changes will remain private');
  /** @meaning calendar.privateChanges.content */
  var MSG_CONTENT = goog.getMsg('You are about to make changes that will only '
      + 'be reflected on this calendar. Do you still want to continue ?');
  this.setTitle(MSG_TITLE);
  this.setContent(MSG_CONTENT);
  this.setButtonSet(goog.ui.Dialog.ButtonSet.createYesNo());
}
goog.inherits(net.bluemind.calendar.day.ui.PrivateChangesDialog, goog.ui.Dialog);

/** @override */
net.bluemind.calendar.day.ui.PrivateChangesDialog.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this, goog.ui.Dialog.EventType.SELECT, this.handle_);
};

/**
 * @private
 * @param {goog.ui.Dialog.Event} e
 */
net.bluemind.calendar.day.ui.PrivateChangesDialog.prototype.handle_ = function(e) {
  e.stopPropagation();
  if (e.key == goog.ui.Dialog.DefaultButtonKeys.YES) {
    this.getModel().target = this;
    this.getModel().currentTarget = this;
    this.getModel().force = true;
    this.dispatchEvent(this.getModel());
  } else {
    var event = new net.bluemind.calendar.vevent.VEventEvent(net.bluemind.calendar.vevent.EventType.REFRESH, this
        .getModel().vevent);
    this.dispatchEvent(event);
  }
};