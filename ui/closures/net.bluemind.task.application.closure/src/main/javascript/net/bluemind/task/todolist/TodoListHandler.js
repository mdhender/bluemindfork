goog.provide("net.bluemind.task.todolist.TodoListHandler");

goog.require("net.bluemind.mvp.handler.PresenterHandler");
goog.require("net.bluemind.task.todolist.TodoListPresenter");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.handler.PresenterHandler}
 */
net.bluemind.task.todolist.TodoListHandler = function(ctx) {
  net.bluemind.mvp.handler.PresenterHandler.call(this, ctx);
}
goog.inherits(net.bluemind.task.todolist.TodoListHandler,
    net.bluemind.mvp.handler.PresenterHandler);


/** @override */
net.bluemind.task.todolist.TodoListHandler.prototype.createPresenter = function(ctx) {
  return new net.bluemind.task.todolist.TodoListPresenter(ctx);
};