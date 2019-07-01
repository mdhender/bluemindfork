goog.provide("net.bluemind.task.create.CreateView");

goog.require("goog.ui.Button");
goog.require("goog.ui.Component.EventType");
goog.require("bluemind.ui.style.PrimaryActionButtonRenderer");

/**
 * @constructor
 *
 * @extends {goog.ui.Button}
 */
net.bluemind.task.create.CreateView = function() {
  /** @meaning tasks.create.newTask */
  var MSG_NEW_TASK = goog.getMsg('New task');
  goog.ui.Button.call(this, MSG_NEW_TASK,
      bluemind.ui.style.PrimaryActionButtonRenderer.getInstance());
}
goog.inherits(net.bluemind.task.create.CreateView, goog.ui.Button);