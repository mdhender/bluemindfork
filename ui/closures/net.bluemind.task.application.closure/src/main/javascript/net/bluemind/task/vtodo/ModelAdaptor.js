goog.provide("net.bluemind.task.vtodo.ModelAdaptor");

goog.require("goog.array");

/**
 * @constructor
 * 
 * @param {net.bluemind.mvp.ApplicationContext} ctx
 */
net.bluemind.task.vtodo.ModelAdaptor = function(ctx) {
  this.ctx_ = ctx;
};

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 * @private
 */
net.bluemind.task.vtodo.ModelAdaptor.prototype.ctx_;

/**
 * Build VTodo form model from VTodo Json
 * 
 * @param {*} vtodo
 * @return {Object}
 */
net.bluemind.task.vtodo.ModelAdaptor.prototype.vtodoToModelView = function(vtodo) {
  var model = {};
  model.id = vtodo['uid'];
  model.uid = vtodo['value']['uid'];
  model.container = vtodo['container'];
  model.summary = vtodo['value']['summary'];
  model.description = vtodo['value']['description'];
  model.location = vtodo['value']['location'];
  var helper = this.ctx_.helper('date');
  if (vtodo['value']['dtstart']) {
    model.start = this.ctx_.helper('date').create(vtodo['value']['dtstart']);
  }
  if (vtodo['value']['due']) {
    model.due = this.ctx_.helper('date').create(vtodo['value']['due']);
  }
  if (vtodo['value']['completed']) {
    model.completed = this.ctx_.helper('date').create(vtodo['value']['completed']);
  }
  model.percent = vtodo['value']['percent'];

  model.status = vtodo['value']['status'];

  model.priority = vtodo['value']['priority'];

  if (vtodo['value']['alarm']) {
    model.alarm = goog.array.map(vtodo['value']['alarm'], function(alarm) {
      if (alarm['trigger'] != null) {
        alarm['trigger'] = alarm['trigger'] * -1;
      }
      return {
        action : alarm['action'],
        trigger : alarm['trigger']
      }
    })
  }

  model.tags = goog.array.map(vtodo['value']['categories'] || [], function(tag) {
    return {
      id : tag['itemUid'],
      container : tag['containerUid'],
      label : tag['label'],
      color : tag['color']
    };
  });
  model.states = {};

  return model;
};

/**
 * Build Vtodo json model from form values
 * 
 * @param {*} model
 * @return {Object}
 */
net.bluemind.task.vtodo.ModelAdaptor.prototype.vtodoFromModelView = function(model) {
  var vtodo = {
    'container' : model.container,
    'uid' : model.id,
    'name' : model.summary,
    'value' : {}
  };

  vtodo['value']['summary'] = model.summary;
  vtodo['value']['description'] = model.description;
  if (model.start) {
    vtodo['value']['dtstart'] = this.ctx_.helper('date').toBMDateTime(model.start);
  }
  if (model.due) {
    vtodo['value']['due'] = this.ctx_.helper('date').toBMDateTime(model.due);
  }
  if (model.completed && model.status == 'Completed') {
    vtodo['value']['completed'] = this.ctx_.helper('date').toBMDateTime(model.completed);
  } else {
    vtodo['value']['completed'] = null;
  }
  vtodo['value']['uid'] = model.uid || net.bluemind.mvp.UID.generate();
  vtodo['value']['status'] = model.status;
  vtodo['value']['percent'] = model.percent;
  vtodo['value']['priority'] = model.priority;
  vtodo['value']['location'] = model.location;
  vtodo['value']['alarm'] = goog.array.map(model.alarm, function(alarm) {
    if (alarm.trigger != null) {
      alarm.trigger = alarm.trigger * -1;
    }
    return {
      'action' : alarm.action,
      'trigger' : alarm.trigger
    }
  });

  vtodo['value']['categories'] = goog.array.map(model.tags, function(tag) {
    return {
      'itemUid' : tag.id,
      'containerUid' : tag.container,
      'label' : tag.label,
      'color' : tag.color
    };

  });

  return vtodo;
};

/**
 * Build Todolist form value from VTodo JSON model
 * 
 * @param {*} todolist
 * @return {Object}
 */
net.bluemind.task.vtodo.ModelAdaptor.prototype.todoListToModelView = function(todolist) {

  return {
    uid : todolist['uid'],
    name : todolist['name'],
    states : {
      writable : todolist['writable']
    }
  };
};