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
 * @fileoverview Application bootstrap.
 */

goog.provide('net.bluemind.calendar.ColorPalette');

goog.require('goog.color');

/**
 * Get colors palette clone. If filter parameter is given, only colors not in
 * filter will be returned
 * 
 * @static
 * @param {Array=} opt_filter
 * @return {*}
 */
net.bluemind.calendar.ColorPalette.getColors = function(opt_filter) {
  var filter = opt_filter && goog.array.map(opt_filter, function(color) {
    return color.toUpperCase();
  }) || [];
  return goog.array.filter(net.bluemind.calendar.ColorPalette.colors_, function(color) {
    return !goog.array.contains(filter, color);
  });
}

/**
 * 
 */
net.bluemind.calendar.ColorPalette.colors_ = [ "#3D99FF", "#FF6638", "#62CD00", "#D07BE3", "#FFAD40", "#9E9E9E",
    "#00D5D5", "#F56A9E", "#E9D200", "#A77F65", "#B3CB00", "#B6A5E9", "#4C3CD9", "#B00021", "#6B9990", "#A8A171",
    "#860072", "#8C98BA", "#C98FA4", "#725299", "#5C5C5C" ];

/**
 * 
 * @static
 * @param {string} color
 * @param {float=} opt_factor Darker
 * @return {string}
 */
net.bluemind.calendar.ColorPalette.darker = function(color) {
  return goog.color.rgbArrayToHex(goog.color.darken(goog.color.hexToRgb(color), 0.3));
}

net.bluemind.calendar.ColorPalette.lighter = function(color) {
  return goog.color.rgbArrayToHex(goog.color.lighten(goog.color.hexToRgb(color), 0.7));
}

/**
 * Get text color with the highest contrast
 * 
 * @static
 * @param {string} color Background color
 * @return {string} text color
 */
net.bluemind.calendar.ColorPalette.textColor = function(color) {
  color = goog.color.darken(goog.color.hexToRgb(color), 0.3);
  return goog.color.rgbArrayToHex(goog.color.highContrast(color, [ [ 51, 51, 51 ], [ 255, 255, 255 ] ]));
}
