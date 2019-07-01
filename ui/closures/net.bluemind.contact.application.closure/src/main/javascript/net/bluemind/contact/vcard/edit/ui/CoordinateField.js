/*
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
goog.provide("net.bluemind.contact.vcard.edit.ui.CoordinateField");

goog.require("goog.events.FocusHandler");
goog.require("goog.events.InputHandler");
goog.require("goog.events.FocusHandler.EventType");
goog.require("goog.events.InputHandler.EventType");
goog.require("goog.ui.Button");
goog.require("goog.ui.Control");
goog.require("goog.ui.FlatMenuButtonRenderer");
goog.require("goog.ui.LabelInput");
goog.require("goog.ui.Menu");
goog.require("goog.ui.Select");
goog.require("goog.ui.Component.EventType");
goog.require("goog.ui.Component.State");
goog.require("net.bluemind.ui.form.FormField");
goog.require("net.bluemind.ui.form.templates");
goog.require("bluemind.ui.style.TrashButtonRenderer");

/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {*} options
 * @param {goog.ui.ControlRenderer=} opt_renderer
 * @param {goog.dom.DomHelper=} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField = function(label, options, labelType, opt_renderer, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_renderer, opt_domHelper);
  this.options = options;
  this.labelType = labelType;
  this.addClassName(goog.getCssName('field-coordinate'));

}
goog.inherits(net.bluemind.contact.vcard.edit.ui.CoordinateField, net.bluemind.ui.form.FormField);

/**
 * @type {*}
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.options;

/** @override */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.createFormField = function() {
  // Add container
  var container = new goog.ui.Control();
  container.setHandleMouseEvents(false);
  container.setSupportedState(goog.ui.Component.State.FOCUSED, false);
  container.addClassName(goog.getCssName('field-base'));
  this.addChild(container, true);

  // Add trash to previous child
  var previous = this.getChildAt(this.indexOfChild(container) - 1);
  if (previous && previous.getChild('field') && !previous.getChild('trash')) {
	if (null != this.labelType){
		if (this.labelType == 'EMAIL'){
		  this.addCommunicationIcon(previous, 'fa-paper-plane', 'mailto:');
		} else if (this.labelType == 'WEBSITE'){
		  this.addCommunicationIcon(previous, 'fa-globe', 'http://');
		} else {
		  this.addCommunicationIcon(previous, 'fa-phone', 'tel:');
		}
	}
    var trash = new goog.ui.Button(" ", bluemind.ui.style.TrashButtonRenderer.getInstance());
    previous.addChild(trash);
    trash.render(previous.getElementByClass(goog.getCssName('field-base-field')));
    this.getHandler().listen(trash, goog.ui.Component.EventType.ACTION, function() {
      this.removeFormField(previous);
    });
  }

  container.getElement().innerHTML = net.bluemind.ui.form.templates.field();

  var label = this.createLabel();
  container.addChild(label);
  label.render(container.getElementByClass(goog.getCssName('field-base-label')));

  var field = this.createField();
  container.addChild(field);
  field.render(container.getElementByClass(goog.getCssName('field-base-field')));

  var input = new goog.events.InputHandler(field.getElement());
  container.registerDisposable(input);
  var focus = new goog.events.FocusHandler(field.getElement())
  container.registerDisposable(focus);


  this.getHandler().listen(label, goog.ui.Component.EventType.ACTION, function(e) {
    this.handleLabelChanged(container);
  });

  this.getHandler().listen(input, goog.events.InputHandler.EventType.INPUT, function(e) {
    if (this.indexOfChild(container) == (this.getChildCount() - 1) && field.getValue() != '') {
      this.createFormField();
    }
    this.handleInputChanged(container);
  });

  this.getHandler().listen(focus, goog.events.FocusHandler.EventType.FOCUSOUT, function() {
    if (this.indexOfChild(container) != (this.getChildCount() - 1) && field.getValue() == '') {
      this.removeFormField(container);
    }
  });

  return container;
};

/**
 * Add mailto/tel icon
 * @param {goog.ui.Control} container
 * @param {string} css Icon css
 * @param {string} proto Default link protocol 
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.addCommunicationIcon = function(container, css, proto) {
	var elem = goog.dom.createDom('i', 
			goog.getCssName('coordinate-icon') + ' ' + goog.getCssName('goog-button-icon') + ' ' + goog.getCssName('fa') + ' ' + css);
	var href = goog.dom.createDom('a');
	if( goog.Uri.parse(container.getChildAt(1).getValue()).hasScheme()) {
	   href.href = container.getChildAt(1).getValue();
	} else {
    href.href = proto + container.getChildAt(1).getValue();

	}
  href.target = "_blank";
	goog.dom.appendChild(href, elem);
	goog.dom.appendChild(container.getElementByClass(goog.getCssName('field-base-field')), href);
}

/**
 * Remove a row
 * 
 * @param {goog.ui.Control} container
 * @protected
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.removeFormField = function(container) {
  this.removeChild(container, true);
  container.dispose();
};

/** @override */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.createLabel = function() {
  var menu = new goog.ui.Menu();
  var select = new goog.ui.Select(this.label, menu, goog.ui.FlatMenuButtonRenderer.getInstance());
  select.addClassName(goog.getCssName('goog-select'));
  
  var choiceArray = [];
  goog.object.forEach(this.options.STANDARD, function(msg, value) {
	if (!goog.array.contains(choiceArray, msg)){  
		select.addItem(new goog.ui.MenuItem(msg, value), true);
		goog.array.insert(choiceArray, msg);
	}
  });
  select.setId('label');
  return select;

};

/** @override */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.createField = function() {
  var field = this.createInput();
  field.setId('field');
  return field;
};

/**
 * Create input field
 * 
 * @return {goog.ui.LabelInput}
 * @protected
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.createInput = function() {
  return new goog.ui.LabelInput(this.label);
};

/**
 * Return field css
 * 
 * @return {Array}
 * @protected
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.getFieldClassNames = function() {
  return [ goog.getCssName('field-text') ];
};

/**
 * Reset value
 * 
 * @protected
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.resetValue = function() {
  goog.array.forEach(this.removeChildren(true), function(child) {
    child.dispose();
  })
  this.createFormField();
};

/** @override */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.setValue = function(values) {
  this.resetValue();
  values = values || [];
  goog.array.forEach(values, function(value) {
    var child = this.getChildAt(this.getChildCount() - 1);
    this.setFieldValue(child, value);
    this.setLabelValue(child, value);
    this.createFormField();
  }, this);
};

/**
 * @param {goog.ui.Component} component
 * @param {*} value
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.setFieldValue = function(component, value) {
  component.getChild('field').setValue(value.value);
};

/**
 * @param {goog.ui.Component} component
 * @param {*} value
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.setLabelValue = function(component, value) {
  var msg;
  component.getChild('label').setValue(value.label);
  if (component.getChild('label').getSelectedIndex() == -1) {
    if ((msg = this.options.ALL[value.label])) {
      component.getChild('label').addItem(new goog.ui.MenuItem(msg, value.label), true);
    } else {
      msg = this.options.FALLBACK;
      component.getChild('label').addItem(new goog.ui.MenuItem(msg, value.label), true);
    }
    for (var i=0; i<component.getChild('label').getItemCount();i++){
    	if (component.getChild('label').getItemAt(i).getContent() == msg){
    	  component.getChild('label').setValue(component.getChild('label').getItemAt(i).getValue());
    	}
    }
    
  }
};

/** @override */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.getValue = function() {
  var value = [];
  this.forEachChild(function(child) {
    if (child.getChild('field') && child.getChild('label')) {
      var result = {};
      result.value = this.getFieldValue(child);
      if (!!result.value && result.value != 'undefined') { // != 'undefined' because of old bug
        result.label = this.getLabelValue(child);
        value.push(result);
      }
    }
  }, this);
  return value;
};

/**
 * @param {goog.ui.Component} component
 * @param {*} value
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.getFieldValue = function(component) {
  return component.getChild('field').getValue();
};

/**
 * @param {goog.ui.Component} component
 * @param {*} value
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.getLabelValue = function(component) {
  return component.getChild('label').getValue();
};

/**
 * @protected
 * @param {goog.ui.Control} container
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.handleInputChanged = function(container) {
};

/**
 * @protected
 * @param {goog.ui.Control} container
 */
net.bluemind.contact.vcard.edit.ui.CoordinateField.prototype.handleLabelChanged = function(container) {
  container.getChild('field').setLabel(container.getChild('label').getCaption());
};
