/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2016
 *
 * This file is part of BlueMind. BlueMind is a messaging and collaborative
 * solution.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of either the GNU Affero General Public License as
 * published by the Free Software Foundation (version 3 of the License).
 *
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * See LICENSE.txt
 * END LICENSE
 */

/**
 * @fileoverview View class for application header (Logo + logo).
 */

goog.provide("net.bluemind.calendar.navigation.NavigationView");

goog.require("goog.array");
goog.require("goog.soy");
goog.require("goog.string");
goog.require("goog.style");
goog.require("goog.dom.classlist");
goog.require("goog.ui.AnimatedZippy");
goog.require("goog.ui.Component");
goog.require("goog.ui.Container");
goog.require("goog.ui.Control");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.Zippy.Events");
goog.require("goog.ui.MenuSeparator");
goog.require("goog.ui.ac.AutoComplete.EventType");
goog.require("net.bluemind.calendar.navigation.ac.CalendarAutocomplete");
goog.require("net.bluemind.calendar.navigation.templates");
goog.require("goog.events.Event");
goog.require("goog.events.EventHandler");
goog.require("net.bluemind.calendar.navigation.ui.ViewSelector");
goog.require("goog.ui.ColorPalette");
goog.require('net.bluemind.calendar.ColorPalette');
goog.require('goog.ui.Dialog');
goog.require('goog.ui.HsvPalette');
/**
 * View class for navigation bar.
 * 
 * @param {goog.dom.DomHelper=} opt_domHelper Optional DOM helper.
 * @constructor
 * @extends {goog.ui.Component}
 */
net.bluemind.calendar.navigation.NavigationView = function(ctx, opt_domHelper) {
  goog.base(this, opt_domHelper);
  var child = new net.bluemind.calendar.navigation.ui.ViewSelector();
  child.setId('view-selector');
  this.addChild(child);
  child = new goog.ui.LabelInput(net.bluemind.calendar.navigation.NavigationView.MSG_ADD_CALENDAR);
  child.setId('calendar-autocomplete');
  this.addChild(child);

  child = new goog.ui.Component();
  child.setId('calendars-list');
  this.addChild(child);

  child = new goog.ui.Component();
  child.setId('todolists-list');
  this.addChild(child);

  child = new goog.ui.Component();
  child.setId('tags-list');
  this.addChild(child);
  this.ctx = ctx;
  this.ac_ = new net.bluemind.calendar.navigation.ac.CalendarAutocomplete(ctx);

  var dialog = new goog.ui.Dialog();
  /** @meaning calendar.color.dialog */
  var MSG_COLOR_TITLE = goog.getMsg('Choose a color');
  dialog.setTitle(MSG_COLOR_TITLE);
  dialog.setId('hsv-palette-dialog');
  this.addChild(dialog, true);
  goog.dom.classlist.add(dialog.getElement(), goog.getCssName('goog-hsv-palette-dialog'));
  child = new goog.ui.HsvPalette(null, null, goog.getCssName('goog-hsv-palette-sm'));
  child.setId('palette');
  dialog.setVisible(false);
  dialog.addChild(child, true);


};

goog.inherits(net.bluemind.calendar.navigation.NavigationView, goog.ui.Component);
/** @meaning calendar.addCalendar */
net.bluemind.calendar.navigation.NavigationView.MSG_ADD_CALENDAR = goog.getMsg('Add a calendar...');
/** @meaning general.calendars */
net.bluemind.calendar.navigation.NavigationView.MSG_CALENDARS = goog.getMsg('Calendars');
/** @meaning general.tags */
net.bluemind.calendar.navigation.NavigationView.MSG_TAGS = goog.getMsg('Tags');

/**
 * @type {net.bluemind.mvp.ApplicationContext}
 */
net.bluemind.calendar.navigation.NavigationView.prototype.ctx;

/**
 * @private
 * @type {net.bluemind.calendar.navigation.ac.CalendarAutocomplete}
 */
net.bluemind.calendar.navigation.NavigationView.prototype.ac_;

/**
 * @type {goog.dom.ViewportSizeMonitor}
 */
net.bluemind.calendar.navigation.NavigationView.prototype.sizeMonitor_

/** @override */
net.bluemind.calendar.navigation.NavigationView.prototype.createDom = function() {
  goog.base(this, 'createDom');
  var dom = this.getDomHelper();

  this.getChild('view-selector').render(this.getElement());
  var id, label, element, content;

  element = goog.soy.renderAsElement(net.bluemind.calendar.navigation.templates.calendarsAndTags, null, null, dom);
  dom.appendChild(this.getElement(), element);

  var calendarsView = dom.getElementByClass(goog.getCssName('view-calendars'), element);
  content = dom.getElementByClass(goog.getCssName('goog-zippy-content'), calendarsView);
  this.getChild('calendar-autocomplete').render(content);
  this.getChild('calendars-list').render(content);
  goog.dom.classlist.add(this.getChild('calendars-list').getElement(), goog.getCssName('bm-calendars-items'));

  var todolistsView = dom.getElementByClass(goog.getCssName('view-todolists'), element);
  content = dom.getElementByClass(goog.getCssName('goog-zippy-content'), todolistsView);
  this.getChild('todolists-list').render(content);
  goog.dom.classlist.add(this.getChild('todolists-list').getElement(), goog.getCssName('bm-todolists-items'));

  var tagsView = dom.getElementByClass(goog.getCssName('view-tags'), element);
  content = dom.getElementByClass(goog.getCssName('goog-zippy-content'), tagsView);
  this.getChild('tags-list').render(content);
  goog.dom.classlist.add(this.getChild('tags-list').getElement(), goog.getCssName('bm-tags-items'));

  this.ac_.attachInputs(this.getChild('calendar-autocomplete').getElement());

};

/** @override */
net.bluemind.calendar.navigation.NavigationView.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  var dom = this.getDomHelper();

  var id = this.makeId('calendars');
  var content = dom.getElement(id);

  goog.array.forEach(this.getElementsByClass(goog.getCssName('goog-zippy-header')), function(head) {
    var dom = this.getDomHelper();
    var content = dom.getNextElementSibling(head);
    var zippy = new goog.ui.AnimatedZippy(head, content, true);
    goog.dom.classlist.enable(dom.getFirstElementChild(head), goog.getCssName('fa-chevron-down'), true);
    this.registerDisposable(zippy);
    this.getHandler().listen(zippy, goog.ui.Zippy.Events.TOGGLE, this.toggle_);
    this.initZippy_(zippy);
  }, this);

  // this.getHandler().listen(this.getChild('calendars-list'),
  // goog.ui.Component.EventType.ACTION, this.toggleCalendar_);
  // this.getHandler().listen(this.getChild('tags-list'),
  // goog.ui.Component.EventType.ACTION, this.toggle_);

  this.getHandler().listen(this.ac_, goog.ui.ac.AutoComplete.EventType.UPDATE, this.handleAddCalendar_);

  this.sizeMonitor_ = new goog.dom.ViewportSizeMonitor();
  this.getHandler().listen(this.sizeMonitor_, goog.events.EventType.RESIZE, this.resize_);
  this.resize_();

};

net.bluemind.calendar.navigation.NavigationView.prototype.resize_ = function() {
  var dom = this.getDomHelper();
  var grid = dom.getElementByClass(goog.getCssName('calendars-and-tags'), this.getElement());
  var elSize = goog.style.getClientPosition(grid);
  var size = this.sizeMonitor_.getSize();
  var height = size.height - elSize.y - 3;
  grid.style.height = height + 'px';
}
/**
 * @param {goog.event.Event} e
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.handleAddCalendar_ = function(e) {
  this.getChild('calendar-autocomplete').clear();
  if (e.row['type'] == 'group') {
    var add = new goog.events.Event(net.bluemind.calendar.navigation.events.EventType.ADD_CALENDAR);

    this.ctx.client('calendar-autocomplete').calendarGroupLookup(e.row['uid']).then(function(cals) {
      add.calendars = goog.array.map(cals, function(cal) {
        return cal['uid'];
      })
      this.dispatchEvent(add);
    }, null, this);
  } else {
    var add = new goog.events.Event(net.bluemind.calendar.navigation.events.EventType.ADD_CALENDAR);
    add.calendars = [ e.row['uid'] ];
    this.dispatchEvent(add);
  }

};

/**
 * @param {goog.event.Event} e
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.handleRemoveCalendar_ = function(calendar) {
  var remove = new goog.events.Event(net.bluemind.calendar.navigation.events.EventType.REMOVE_CALENDAR);
  remove.calendar = calendar;
  this.dispatchEvent(remove);

};

/**
 * @param {goog.event.Event} e
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.handleToggleCalendar_ = function(calendar) {
  var type = net.bluemind.calendar.navigation.events.EventType.SHOW_CALENDAR
  if (calendar.states.visible) {
    type = net.bluemind.calendar.navigation.events.EventType.HIDE_CALENDAR;
  }
  var e = new goog.events.Event(type);
  e.calendar = calendar
  this.dispatchEvent(e);

};

net.bluemind.calendar.navigation.NavigationView.prototype.handleToggleTag_ = function(tag) {
  var type = net.bluemind.calendar.navigation.events.EventType.TOGGLE_TAG;
  var e = new goog.events.Event(type);
  e.tag = tag;
  this.dispatchEvent(e);
}
/**
 * init zippy collapse state
 * 
 * @param {goog.ui.Zippy} zippy zippy widget
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.initZippy_ = function(zippy) {

  var id = zippy.getVisibleHeaderElement().id;
  if (id == null) {
    return;
  }
  var cookies = new goog.net.Cookies(window.document);
  var hide = cookies.get('show-zippy-' + id) == "false";
  if (hide) {
    var el = this.getDomHelper().getFirstElementChild(zippy.getVisibleHeaderElement());
    goog.dom.classlist.enable(el, goog.getCssName('fa-chevron-right'), true)
    goog.dom.classlist.enable(el, goog.getCssName('fa-chevron-down'), false);
    zippy.collapse();
  }
};

/**
 * Sync sublist visible state with zippy.
 * 
 * @param {goog.ui.ZippyEvent} e Toggle event
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.toggle_ = function(e) {
  var zippy = e.target;
  var el = this.getDomHelper().getFirstElementChild(zippy.getVisibleHeaderElement());

  goog.dom.classlist.enable(el, goog.getCssName('fa-chevron-right'), !e.expanded);
  goog.dom.classlist.enable(el, goog.getCssName('fa-chevron-down'), e.expanded);

  var id = zippy.getVisibleHeaderElement().id;
  if (id == null) {
    return;
  }

  var cookies = new goog.net.Cookies(window.document);
  var current = cookies.get('show-zippy-' + id) == "true";
  cookies.set('show-zippy-' + id, e.expanded ? "true" : "false", 60 * 60 * 24 * 5, '/cal', null, goog.string.startsWith(window.location.protocol, 'https'));

};

/**
 * Set calendar list
 * 
 * @param {Array.<{name: string, uid: string}>} calendars Calendars list.
 */
net.bluemind.calendar.navigation.NavigationView.prototype.setCalendars = function(calendars) {
  var list = this.getChild('calendars-list');
  this.setContainers(calendars, list, true, net.bluemind.calendar.navigation.templates.calendar);
  // FIXME
  this.ac_.setCalendars(list.getChildIds());
};

/**
 * @param {Array.<{label: string, color: string}>} todolists Todolists list.
 */
net.bluemind.calendar.navigation.NavigationView.prototype.setTodolists = function(todolists) {
  var list = this.getChild('todolists-list');

  // BM-6665 show Default calendar on top of the list
  // BM-7569 calendars are not sorted by name
  var user = this.ctx.user['uid'];
  goog.array.sort(todolists, function(o1, o2) {
    if (goog.string.contains(o1.uid, user)) {
      return -1;
    }
    if (goog.string.contains(o2.uid, user)) {
      return 1;
    }
    return goog.string.caseInsensitiveCompare(o1.name, o2.name);
  }, this);

  this.setContainers(todolists, list, false, net.bluemind.calendar.navigation.templates.todolist);
};

/**
 * Set calendar list
 * 
 * @param {Array.<{name: string, uid: string}>} containers Containers list.
 * @param {goog.ui.Component} list Containers list
 * @param {boolean} removable Containers can be removed
 * @param {function} tpl Container item template
 */
net.bluemind.calendar.navigation.NavigationView.prototype.setContainers = function(containers, list, removable, tpl) {

  var toRemove = list.getChildIds();
  goog.array.forEach(containers, function(container, position) {
    goog.array.remove(toRemove, container.uid);
    var control = list.getChild(container.uid);
    if (!control) {
      this.addContainerInternal_(container, position, list, removable, tpl);
    } else {
      this.updateContainerInternal_(container, list);
    }
  }, this);
  goog.array.forEach(toRemove, function(id) {
    this.removeContainerInternal_(id, list);
  }, this);

};

/**
 * Add a calendar to calendar list
 * 
 * @param {{name: string, uid: string}} container Container data.
 * @param {integer} position Element position
 * @param {goog.ui.Component} list Containers list
 * @param {boolean} removable Containers can be removed
 * @param {function} tpl Container item template
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.addContainerInternal_ = function(container, position, list,
    removable, tpl) {
  var dom = this.getDomHelper();
  var element = goog.soy.renderAsFragment(tpl, container, null, dom);
  var control = new goog.ui.Control(element);
  control.setId(container.uid);
  control.setModel(container);
  control.setSupportedState(goog.ui.Component.State.FOCUSED, false);

  list.addChildAt(control, position, true);
  control.getRenderer().setState(control, goog.ui.Component.State.DISABLED, !container.states.visible);
  control.addClassName(goog.getCssName('calendar-item'));
  var menu = new goog.ui.Menu();

  if (removable) {
    /** @meaning general.remove */
    var MSG_REMOVE = goog.getMsg('Remove');
    var remove = new goog.ui.MenuItem(MSG_REMOVE);
    remove.setId('remove');
    menu.addChild(remove, true);
    menu.addChild(new goog.ui.MenuSeparator(), true);

    this.getHandler().listen(remove, goog.ui.Component.EventType.ACTION, function() {
      this.handleRemoveCalendar_(control.getModel());
    });
  }

  var palette = new goog.ui.ColorPalette(net.bluemind.calendar.ColorPalette.getColors());
  palette.setId('palette');
  menu.addChild(palette, true);

  /** @meaning calendar.chooseColor */
  var MSG_CHOSE_COLOR = goog.getMsg('Choose a personalized color...');

  var customColor = new goog.ui.MenuItem(MSG_CHOSE_COLOR);
  customColor.setId('custom');
  menu.addChild(customColor, true);

  var button = new goog.ui.MenuButton(null, menu);
  button.setId('button');
  control.addChild(button, true);
  button.addClassName(goog.getCssName('calendar'));
  button.getElement().style.backgroundColor = container.color.background;
  button.getElement().style.color = container.color.foreground;

  this.getHandler().listen(control, goog.ui.Component.EventType.ACTION, function(e) {
    this.handleToggleCalendar_(control.getModel());
  });

  this.getHandler().listen(button, goog.ui.Component.EventType.ACTION, goog.events.Event.stopPropagation);

  this.getHandler().listen(button.getElement(), goog.events.EventType.MOUSEDOWN, goog.events.Event.stopPropagation);
  this.getHandler().listen(customColor, goog.ui.Component.EventType.ACTION, function(e) {
    var dialog = this.getChild('hsv-palette-dialog');
    dialog.setVisible(true);
    dialog.getChild('palette').setColor(container.color.background);
    this.getHandler().listenOnce(dialog, goog.ui.Dialog.EventType.SELECT, function(e) {
      if (e.key == 'ok') {
        var change = new goog.events.Event(net.bluemind.calendar.navigation.events.EventType.CHANGE_CALENDAR_COLOR);
        change.calendar = control.getModel();
        change.color = dialog.getChildAt(0).getColor();
        this.dispatchEvent(change);
      }
    });
  });

  this.getHandler().listen(palette, goog.ui.Component.EventType.ACTION, function(e) {
    var change = new goog.events.Event(net.bluemind.calendar.navigation.events.EventType.CHANGE_CALENDAR_COLOR);
    change.calendar = control.getModel();
    change.color = palette.getSelectedColor();
    this.dispatchEvent(change);
  }, this);

};

/**
 * Update a calendar from calendar list
 * 
 * @param {{label: string, color: string}} container Container to update .
 * @param {goog.ui.Component} list Containers list
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.updateContainerInternal_ = function(container, list) {
  var control = list.getChild(container.uid);
  var old = control.getModel();
  control.setModel(container);
  if (old.name != container.name) {
    var el = control.getElementByClass(goog.getCssName('title')).lastChild;
    el.innerHTML = container.name;
  }
  if (old.states.visible != container.states.visible) {
    control.getRenderer().setState(control, goog.ui.Component.State.DISABLED, !container.states.visible);
  }
  if (container.color.background != old.color.background || container.color.foreground != old.color.foreground) {
    var button = control.getChild('button')
    button.getElement().style.backgroundColor = container.color.background;
    button.getElement().style.color = container.color.foreground;
  }
};

/**
 * Remove a calendar from calendar list
 * 
 * @param {string} uid Calendar uid.
 * @param {goog.ui.Component} list Containers list
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.removeContainerInternal_ = function(uid, list) {
  var child = list.removeChild(list.getChild(uid), true);
  var button = child.getChild('button');
  this.getHandler().unlisten(child, goog.ui.Component.EventType.ACTION);
  this.getHandler().unlisten(button, goog.ui.Component.EventType.ACTION);
  this.getHandler().unlisten(button.getElement(), goog.events.EventType.MOUSEDOWN);
  var custom = button.getMenu().getChild('custom');
  this.getHandler().unlisten(custom, goog.ui.Component.EventType.ACTION);
  var palette = button.getMenu().getChild('palette');
  this.getHandler().unlisten(palette, goog.ui.Component.EventType.ACTION);
  var remove = button.getMenu().getChild('remove');
  if (remove) {
    this.getHandler().unlisten(remove, goog.ui.Component.EventType.ACTION);
  }
  child.dispose();
};

/**
 * @param {Array.<{label: string, color: string}>} tags Tags list.
 */
net.bluemind.calendar.navigation.NavigationView.prototype.setTags = function(tags) {
  var list = this.getChild('tags-list');
  var toRemove = list.getChildIds();
  goog.array.forEach(tags, function(tag, position) {
    goog.array.remove(toRemove, tag.uid);
    var control = list.getChild(tag.uid);
    if (!control) {
      this.addTagInternal_(tag, position);
    } else {
      this.updateTagInternal_(tag);
    }
  }, this);
  goog.array.forEach(toRemove, function(id) {
    this.removeTagInternal_(id);
  }, this);
  goog.style.setElementShown(this.getChild('tags-list').getElement(), !!tags.length);

};

/**
 * Add a tag to tag list
 * 
 * @param {{name: string, uid: string}} tag Tag data.
 * @param {integer} position Element position
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.addTagInternal_ = function(tag, position) {
  var selectedTag = this.ctx.session.get('selected-tag') || [];
  var list = this.getChild('tags-list');
  var tpl = net.bluemind.calendar.navigation.templates.tag;
  var element = goog.soy.renderAsElement(tpl, tag, null, this.getDomHelper());
  var control = new goog.ui.Control(element);
  control.setModel(tag);
  control.setId(tag.uid);
  list.addChild(control, true);
  control.setSupportedState(goog.ui.Component.State.FOCUSED, false);
  control.getRenderer().setState(control, goog.ui.Component.State.DISABLED,
      !goog.array.isEmpty(selectedTag) && !goog.array.contains(selectedTag, tag.uid));
  this.getHandler().listen(control, goog.ui.Component.EventType.ACTION, function(e) {
    this.handleToggleTag_(control.getModel());
  });
};

/**
 * Update a tag from Tag list
 * 
 * @param {Array.<{label: string, color: string}>} tag Tag .
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.updateTagInternal_ = function(tag) {
  var selectedTag = this.ctx.session.get('selected-tag') || [];
  var list = this.getChild('tags-list');
  var control = list.getChild(tag.uid);
  var old = control.getModel();
  control.setModel(tag);
  if (old.color != tag.color) {
    var el = control.getElementByClass(goog.getCssName('tag-item')).firstChild;
    el.style.color = '#' + tag.color;
  }
  if (old.name != tag.name) {
    el = control.getElementByClass(goog.getCssName('title')).lastChild;
    el.innerHTML = tag.name;
  }
  control.getRenderer().setState(control, goog.ui.Component.State.DISABLED,
      !goog.array.isEmpty(selectedTag) && !goog.array.contains(selectedTag, tag.uid));
};

/**
 * Remove a tag from tag list
 * 
 * @param {string} uid Tag uid.
 * @private
 */
net.bluemind.calendar.navigation.NavigationView.prototype.removeTagInternal_ = function(uid) {
  var list = this.getChild('tags-list');
  var child = list.removeChild(list.getChild(uid), true);
  this.getHandler().unlisten(child, goog.ui.Component.EventType.ACTION);
  child.dispose();
};

/**
 * @param {Array.<{uid: string, label: string}>} views views list.
 */
net.bluemind.calendar.navigation.NavigationView.prototype.setViews = function(views) {
  this.getChild('view-selector').setModel(views);
};

net.bluemind.calendar.navigation.NavigationView.prototype.setSelected = function(view) {
  this.getChild('view-selector').setSelected(view);
};
