goog.provide("net.bluemind.calendar.navigation.ac.CalendarRowRenderer");

goog.require("goog.ui.ac.Renderer.CustomRenderer");
goog.require("net.bluemind.calendar.navigation.templates");

/**
 * @constructor
 * @extends goog.ui.ac.Renderer.CustomRenderer
 */
net.bluemind.calendar.navigation.ac.CalendarRowRenderer = function() {
  goog.base(this);
};
goog.inherits(net.bluemind.calendar.navigation.ac.CalendarRowRenderer, goog.ui.ac.Renderer.CustomRenderer);

/** @override */
net.bluemind.calendar.navigation.ac.CalendarRowRenderer.prototype.render = null;

/** @override */
net.bluemind.calendar.navigation.ac.CalendarRowRenderer.prototype.renderRow = function(row, token, node) {
  node.innerHTML = net.bluemind.calendar.navigation.templates.calendarrow({
    entry : row.data
  });
};