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
 * @fileoverview Interface bluemind configurable plugins.
 *
 * @see ../demos/editor/editor.html
 */

goog.provide('bluemind.ui.editor.IConfigurablePlugin');

goog.require('goog.editor.Plugin');

/**
 * Interface bluemind configurable plugins.
 * @interface
 */
bluemind.ui.editor.IConfigurablePlugin = function(opt_options) {};


/**
 * Set plugin options
 * @param {*} options Optional plugin's parameters .
 *
 */
bluemind.ui.editor.IConfigurablePlugin.prototype.setOptions = function(options) {};

/**
 * Get plugin options.
 * @return {?*} plugin's parameters.
 * 
 */
bluemind.ui.editor.IConfigurablePlugin.prototype.getOptions = function(options) {};

/**
 * @return {string} The ID unique to this plugin class. Note that different
 *     instances off the plugin share the same classId.
 */
bluemind.ui.editor.IConfigurablePlugin.prototype.getTrogClassId = function () {};
