goog.provide("net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter");

goog.require("goog.Promise");
goog.require("goog.array");
goog.require("goog.dom");
goog.require("net.bluemind.calendar.vevent.EventType");
goog.require("net.bluemind.calendar.vtodo.TodolistsManager");
goog.require("net.bluemind.calendar.vtodo.VTodoAdaptor");
goog.require("net.bluemind.calendar.vtodo.consult.VTodoConsultView");
goog.require("net.bluemind.mvp.Presenter");
goog.require("net.bluemind.todolist.api.i18n.Priority.Caption");

/**
 * @param {net.bluemind.mvp.ApplicationContext} ctx Application context
 * @constructor
 * @extends {net.bluemind.mvp.Presenter}
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter = function(ctx) {
  net.bluemind.mvp.Presenter.call(this, ctx);
  this.view_ = new net.bluemind.calendar.vtodo.consult.VTodoConsultView(ctx);
  this.registerDisposable(this.view_);
  this.adaptor_ = new net.bluemind.calendar.vtodo.VTodoAdaptor(ctx);
  this.todolists_ = new net.bluemind.calendar.vtodo.TodolistsManager(ctx);
}
goog.inherits(net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter, net.bluemind.mvp.Presenter);

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.init = function() {
  return goog.Promise.resolve();
};

/**
 * @type {goog.ui.Component}
 * @private
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.view_;

/**
 * @type {net.bluemind.calendar.vtodo.ModelAdaptor}
 * @private
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.adaptor_;
/**
 * @type {net.bluemind.calendar.vtodo.TodolistsManager}
 * @private
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.todolists_;

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.init = function() {
  this.view_.render(goog.dom.getElement('main'));
  this.handler.listen(this.view_, net.bluemind.calendar.vevent.EventType.BACK, this.back_);
  return goog.Promise.resolve();
};

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.setup = function() {
  var container = this.ctx.params.get('container');
  var uid = this.ctx.params.get('uid');
  var data = {}, vtodo;
  return this.todolists_.getTodolistModelView(container).then(function(todolist) {
    data.todolist = todolist;
    return this.ctx.service('todolist').getItem(container, uid);
  }, null, this).then(function(item) {
    vtodo = item;
    return this.ctx.service('todolist').getLocalChangeSet(container);
  }, null, this).then(function(changes) {
    if (goog.isDefAndNotNull(vtodo)) {
      this.view_.setModel(this.toModelView_(vtodo, data.todolist, changes));
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
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.toModelView_ = function(vtodo, todolist, changes) {
  var mv = this.adaptor_.toModelView(vtodo, todolist);
  if (goog.isDefAndNotNull(mv.dtstart)) {
    mv.start = this.ctx.helper('dateformat').format(mv.dtstart);
  }
  if (goog.isDefAndNotNull(mv.due)) {
    mv.due = this.ctx.helper('dateformat').format(mv.due);
  }
  if (goog.isDefAndNotNull(mv.completed)) {
    mv.completed = this.ctx.helper('dateformat').format(mv.completed);
  }
  if (mv.priority < 5) {
    mv.priority = net.bluemind.todolist.api.i18n.Priority.MSG_HIGH
  } else if (mv.priority > 5) {
    mv.priority = net.bluemind.todolist.api.i18n.Priority.MSG_LOW;
  } else {
    mv.priority = net.bluemind.todolist.api.i18n.Priority.MSG_MEDIUM;
  }
  mv.container = todolist.name;
  var change = goog.array.find(changes, function(change) {
    return change['itemId'] == vtodo['uid'];
  });
  mv.states.synced = !goog.isDefAndNotNull(change);
  mv.states.error = !mv.states.synced && change['type'] == 'error';
  mv.error = mv.states.error && {
    code : change['errorCode'],
    message : change['errorMessage']
  };
  return mv;
};

/** @override */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.exit = function() {
  return goog.Promise.resolve();
};

/**
 * @param {goog.events.Event} e
 * @private
 */
net.bluemind.calendar.vtodo.consult.VTodoConsultPresenter.prototype.back_ = function(e) {
  this.ctx.helper('url').back(this.ctx.session.get('history'), '/');
};
