goog.provide("net.bluemind.ui.cti.PhoneRowRenderer");

goog.require("goog.ui.ac.Renderer.CustomRenderer");
goog.require("net.bluemind.ui.cti.template.rowrenderer");// FIXME -
// unresolved
// required symbol

/**
 * @constructor
 * 
 * @extends {goog.ui.ac.Renderer.CustomRenderer}
 */
net.bluemind.ui.cti.PhoneRowRenderer = function() {
  goog.base(this);
}
goog.inherits(net.bluemind.ui.cti.PhoneRowRenderer, goog.ui.ac.Renderer.CustomRenderer);

/** @override */
net.bluemind.ui.cti.PhoneRowRenderer.prototype.render = null;

/** @override */
net.bluemind.ui.cti.PhoneRowRenderer.prototype.renderRow = function(row, token, node) {
  node.innerHTML = net.bluemind.ui.cti.template.rowrenderer.main({
    entry : row.data
  });
};