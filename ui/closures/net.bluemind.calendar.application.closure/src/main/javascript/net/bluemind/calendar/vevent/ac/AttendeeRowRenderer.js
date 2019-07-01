goog.provide("net.bluemind.calendar.vevent.ac.AttendeeRowRenderer");

goog.require("goog.ui.ac.Renderer.CustomRenderer");
goog.require("net.bluemind.calendar.vevent.templates");// FIXME -
// unresolved
// required symbol

/**
 * @constructor
 * 
 * @extends {goog.ui.ac.Renderer.CustomRenderer}
 */
net.bluemind.calendar.vevent.ac.AttendeeRowRenderer = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.calendar.vevent.ac.AttendeeRowRenderer, goog.ui.ac.Renderer.CustomRenderer);

/** @override */
net.bluemind.calendar.vevent.ac.AttendeeRowRenderer.prototype.render = null;

/** @override */
net.bluemind.calendar.vevent.ac.AttendeeRowRenderer.prototype.renderRow = function(row, token, node) {
  node.innerHTML = net.bluemind.calendar.vevent.templates.acAttendeeRow({
    entry : row.data
  });
};