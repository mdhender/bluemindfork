goog.provide("net.bluemind.task.vtodo.consult.VTodoConsultPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.task.vtodo.ModelAdaptor");
goog.require("net.bluemind.task.vtodo.VTodoManager");
goog.require("net.bluemind.task.vtodo.consult.VTodoConsultView");
goog.require("net.bluemind.todolist.api.i18n.Priority.Caption");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.task.vtodo.consult.VTodoConsultView(ctx);
  this.registerDisposable(this.view_);
  this.adaptor_ = new net.bluemind.task.vtodo.ModelAdaptor(ctx);
  this.manager_ = new net.bluemind.task.vtodo.VTodoManager(ctx, this.adaptor_);
  //
  this.handler.listenWithScope(this.view_, 'copy', this.manager_.handleCopy, false, this.manager_);
}
goog.inherits(net.bluemind.task.vtodo.consult.VTodoConsultPresenter, net.bluemind.mvp.Presenter);

/**
 * @type {goog.ui.Component}
 * @private
 */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.view_;

/** @override */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.init = function() {
  return goog.Promise.resolve();
};

/**
 * @type {goog.ui.Component}
 * @private
 */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.view_;

/**
 * @type {net.bluemind.task.vtodo.ModelAdaptor}
 * @private
 */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.adaptor_;

/**
 * @type {net.bluemind.task.vtodo.VTodoManager}
 * @private
 */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.manager_;

/** @override */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('main'));
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.setup = function() {
  var container = this.ctx.params.get('container');
  var uid = this.ctx.params.get('uid');
  var todolists = this.ctx.session.get('todolists');
  var todolist = goog.array.find(todolists, function(tdl) {
    return (tdl['uid'] == container);
  });
  return this.ctx.service('todolist').getItem(container, uid).then(function(vtodo) {
    if (goog.isDefAndNotNull(vtodo)) {
      this.view_.setModel(this.toModelView_(vtodo, todolist));
      var lists = goog.array.map(todolists, this.adaptor_.todoListToModelView, this.adaptor_);
      this.view_.setTodoLists(lists);
    } else {
      throw 'VTodo ' + uid + ' not found';
    }
  }, null, this);
};

/**
 * @private
 * @param {Object} vtodo VTodo json
 * @param {Object} todolist Todolist json
 * @return {Object} Model view
 */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.toModelView_ = function(vtodo, todolist) {
  var mv = this.adaptor_.vtodoToModelView(vtodo, todolist);
  mv.start = mv.start != null ? this.ctx.helper('dateformat').format(mv.start) : null;
  mv.due = mv.due != null ? this.ctx.helper('dateformat').format(mv.due) : null;
  mv.completed = mv.completed != null ? this.ctx.helper('dateformat').format(mv.completed) : null;
  if (mv.priority < 5) {
    mv.priority = net.bluemind.todolist.api.i18n.Priority.MSG_HIGH
  } else if (mv.priority > 5) {
    mv.priority = net.bluemind.todolist.api.i18n.Priority.MSG_LOW;
  } else {
    mv.priority = net.bluemind.todolist.api.i18n.Priority.MSG_MEDIUM;
  }
  return mv;
};

/** @override */
net.bluemind.task.vtodo.consult.VTodoConsultPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};
