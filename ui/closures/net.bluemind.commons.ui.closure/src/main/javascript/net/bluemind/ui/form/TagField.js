/* BEGIN LICENSE
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
goog.provide("net.bluemind.ui.form.TagField");

goog.require("net.bluemind.ui.form.FormField");
goog.require("bluemind.ui.TagBox");// FIXME - unresolved required symbol
goog.require("bluemind.model.Tag");
goog.require("goog.ui.ComboBox");
goog.require("goog.ui.ComboBoxItem");
goog.require("net.bluemind.ui.form.TagFieldTemplate");
goog.require("goog.math");
goog.require('goog.string');
goog.provide("net.bluemind.ui.form.TagField.ComboBoxItemRenderer");
goog.provide("net.bluemind.ui.form.TagField.ComboBoxItem");
/**
 * @constructor
 * 
 * @param {goog.ui.ControlContent} label
 * @param {goog.ui.ControlRenderer} opt_renderer
 * @param {goog.dom.DomHelper} opt_domHelper
 * @extends {net.bluemind.ui.form.FormField}
 */
net.bluemind.ui.form.TagField = function(label, opt_renderer, opt_domHelper) {
  net.bluemind.ui.form.FormField.call(this, label, opt_renderer, opt_domHelper);
  this.addClassName(goog.getCssName('field-tag'));

  this.tags = [];
  this.value = [];

}
goog.inherits(net.bluemind.ui.form.TagField, net.bluemind.ui.form.FormField);


/**
 * Tags default colors
 * @const
 */
net.bluemind.ui.form.TagField.COLORS = [ "3D99FF", "FF6638", "62CD00", "D07BE3", "FFAD40", "9E9E9E", "00D5D5",
    "F56A9E", "E9D200", "A77F65", "B3CB00", "B6A5E9", "4C3CD9", "B00021", "6B9990", "A8A171", "860072", "8C98BA",
    "C98FA4", "725299", "5C5C5C" ];


/** 
 * @private {Array.<*>}
 */
net.bluemind.ui.form.TagField.prototype.tags;
/** 
 * @private {Array.<*>}
 */
net.bluemind.ui.form.TagField.prototype.value;

/** @override */
net.bluemind.ui.form.TagField.prototype.createField = function() {
  var renderer = new goog.ui.ContainerRenderer();
  // because it's java script :D
  renderer.createDom = function(container) {
    return container.getDomHelper().createDom('ul');
  };

  var bullets = new goog.ui.Container(null, renderer);
  bullets.setFocusable(true);
  bullets.setId('bullets');
  this.addChild(bullets);
  bullets.render(this.getElementByClass(goog.getCssName('field-base-field')));
  var cb = new goog.ui.ComboBox();
  cb.setUseDropdownArrow(true);

  /** @meaning tags.selectOne */
  var MSG_TAG_SELECT = goog.getMsg('Select a tag...');

  cb.setId('tagsbox');
  cb.setDefaultText(MSG_TAG_SELECT);
  cb.setMatchFunction(function(str1, str2) {
    /** @meaning tags.create */
    var MSG_TAG_CREATE = goog.getMsg('Create tag');
    if (goog.string.startsWith(str1, MSG_TAG_CREATE.toLowerCase())) {
      return str2.length > 0;
    }
    return goog.string.startsWith(str1, str2);
  })
  this.addChild(cb);
  cb.render(this.getElementByClass(goog.getCssName('field-base-field')));
  cb.setEnabled(this.isEnabled());

};

/** @override */
net.bluemind.ui.form.TagField.prototype.enterDocument = function() {
  goog.base(this, 'enterDocument');
  this.getHandler().listen(this.getChild('tagsbox'), goog.ui.Component.EventType.ACTION, function(evt) {
    var value = evt.item.getValue();
    if (value != 'create') {
      this.addTag(value);
    } else {
      this.createTag(this.getChild('tagsbox').getValue());
    }
    evt.stopPropagation();
  });
  
  this.getHandler().listen(this.getChild('tagsbox'), 'change', function(evt) {
    var tagsbox = this.getChild('tagsbox');
    var item = tagsbox.getItemAt(tagsbox.getItemCount() - 1);
    /** @meaning tags.create */
    var MSG_TAG_CREATE = goog.getMsg('Create tag');
    item.setContent(MSG_TAG_CREATE + ' "' + evt.target.getValue() + '" ...');
    tagsbox.lastToken_ = tagsbox.lastToken ? tagsbox.lastToken : null;
  });
  this.getHandler().listen(this.getChild('bullets').getKeyEventTarget(), goog.events.EventType.FOCUS, function(evt) {
    this.getChild('tagsbox').getLabelInput().focusAndSelect();
  });
  
  this.refresh_();
}

/** @override */
net.bluemind.ui.form.TagField.prototype.getValue = function() {
  return this.value;
};

/** @override */
net.bluemind.ui.form.TagField.prototype.setValue = function(value) {
  this.value = value || [];
  this.refresh_();
};

/**
 * Set combobox tags
 * @param tags
 */
net.bluemind.ui.form.TagField.prototype.setTags = function(tags) {
  this.tags = tags || [];
  this.refresh_();
};

/** @override */
net.bluemind.ui.form.TagField.prototype.setEnabled = function(enabled) {
  goog.base(this, 'setEnabled', enabled);
  if (this.getChild('tagsbox')) {
    this.getChild('tagsbox').setEnabled(enabled);
   this.refresh_();
  }
};

/**
 * Refresh tag field content
 * @private
 */
net.bluemind.ui.form.TagField.prototype.refresh_ = function() {
  if (!this.isInDocument()) {
    return;
  }
  this.getChild('bullets').removeChildren(true);

  if (this.value) {
    goog.array.forEach(this.value, function(tagref) {

      var tagcomp = new goog.ui.Control();
      tagcomp.setModel(tagref);
      // because it's java script :D
      tagcomp.createDom = function() {
        this.element_ = goog.soy.renderAsElement(net.bluemind.ui.form.TagFieldTemplate.bullet, tagref, null, tagcomp
            .getDomHelper());
      };
      this.getChild('bullets').addChild(tagcomp, true);
      tagcomp.setEnabled(this.isEnabled());
      this.getHandler().listen(tagcomp, goog.ui.Component.EventType.ACTION, function(evt) {
        this.removeTag(evt.currentTarget.getModel());
        evt.stopPropagation();
      });
    }, this);

  }

  this.getChild('tagsbox').removeAllItems();

  goog.array.forEach(goog.array.filter(this.tags, function(tag) {
    return goog.array.findIndex(this.value, function(v) {
      return v.id == tag.id;
    }) == -1;
  }, this), function(tag) {
    this.getChild('tagsbox').addItem(new net.bluemind.ui.form.TagField.ComboBoxItem(tag.label, tag));
  }, this);

  this.getChild('tagsbox').addItem(new goog.ui.MenuSeparator());

  /** @meaning tags.create */
  var MSG_TAG_CREATE = goog.getMsg('Create tag');
  var item = new goog.ui.ComboBoxItem(MSG_TAG_CREATE, 'create');
  item.setId('create-tag');
  this.getChild('tagsbox').addItem(item);

  this.resizeInput_();
}

/**
 * Resize input and set cursor position.
 * 
 * @private
 */
net.bluemind.ui.form.TagField.prototype.resizeInput_ = function() {
  var liSize, coords;
  var li = this.getDomHelper().getLastElementChild(this.getChild('bullets').getElement());
  if (li != null) {
    liSize = goog.style.getSize(li);
    coords = goog.style.getPosition(li);
  } else {
    liSize = new goog.math.Size(0, 0);
    coords = new goog.math.Coordinate(0, 0);
  }
  coords.x += liSize.width;
  var input = this.getChild('tagsbox').getLabelInput().getElement();
  var textSize = goog.style.getSize(input);
  if (textSize.width > coords.x + 90) {
    input.style.paddingLeft = (coords.x + 4) + 'px';
    input.style.paddingTop = (coords.y + 2) + 'px';
  } else {
    input.style.paddingLeft = 4 + 'px';
    input.style.paddingTop = (coords.y + liSize.height + 4) + 'px';
  }
};

/**
 * Add a tag
 * @param {Object.<*>} tag
 */
net.bluemind.ui.form.TagField.prototype.addTag = function(tag) {
  this.value.push(tag);
  this.refresh_();
}

/**
 * Remove a tag
 * @param Object.<*> tag
 */
net.bluemind.ui.form.TagField.prototype.removeTag = function(tag) {
  goog.array.remove(this.value, tag);
  this.refresh_();
}

/**
 * Create a new tag in tag list
 * @param {string}
 */
net.bluemind.ui.form.TagField.prototype.createTag = function(tag) {
  this.value.push({
    'id' : null,
    container : null,
    label : tag,
    color : net.bluemind.ui.form.TagField.COLORS[goog.math.randomInt(net.bluemind.ui.form.TagField.COLORS.length)]
  });
  this.refresh_();
}

/**
 * Class for tag box items.
 * @param {goog.ui.ControlContent} content Text caption or DOM structure to
 *     display as the content of the item (use to add icons or styling to
 *     menus).
 * @param {Object=} opt_data Identifying data for the menu item.
 * @param {goog.dom.DomHelper=} opt_domHelper Optional dom helper used for dom
 *     interactions.
 * @param {goog.ui.MenuItemRenderer=} opt_renderer Optional renderer.
 * @constructor
 * @extends {goog.ui.ComboBoxItem}
 */
net.bluemind.ui.form.TagField.ComboBoxItem = function(content, opt_data, opt_domHelper, opt_renderer) {
  goog.ui.ComboBoxItem.call(this, content, opt_data, opt_domHelper, net.bluemind.ui.form.TagField.ComboBoxItemRenderer
      .getInstance());
};
goog.inherits(net.bluemind.ui.form.TagField.ComboBoxItem, goog.ui.ComboBoxItem);

/** @override */
net.bluemind.ui.form.TagField.ComboBoxItem.prototype.createDom = function() {
  goog.base(this, 'createDom');
  goog.dom.insertChildAt(this.getElement(), this.getDomHelper().createDom("div", {
    "style" : "color:#" + this.getModel().color + "; float: left",
    "class" : "fa fa-lg fa-tag"
  }), 0);
}


/**
 * Default renderer for TagField items
 * @constructor
 * @extends {goog.ui.ControlRenderer}
 */
net.bluemind.ui.form.TagField.ComboBoxItemRenderer = function() {
  goog.ui.MenuItemRenderer.call(this);
}
goog.inherits(net.bluemind.ui.form.TagField.ComboBoxItemRenderer, goog.ui.MenuItemRenderer);
goog.addSingletonGetter(net.bluemind.ui.form.TagField.ComboBoxItemRenderer);

/** @override */
net.bluemind.ui.form.TagField.ComboBoxItemRenderer.prototype.getContentElement = function(element) {
  return goog.dom.getElementByClass(goog.getCssName("goog-menuitem-content"), element);
};
