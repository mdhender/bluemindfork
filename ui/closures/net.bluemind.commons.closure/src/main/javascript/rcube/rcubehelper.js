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
 * @fileoverview Load of static methods and constants to help
 * with roundcube.
 */

goog.provide('bluemind.rcube.RCubeHelper');

goog.require('goog.window');
/**
 * @constructor
 */
bluemind.rcube.RCubeHelper = function() {};

bluemind.rcube.RCubeHelper.URL = '/webmail/';


/**
 * Open the compose window with the 'To' field
 * fill with the email
 * @param {string | number} recipient Email of the contact or id of the list
 * @return {Window} Child window
 */
bluemind.rcube.RCubeHelper.mailTo = function(recipient) {
  if (recipient) {
    var url;
    if (goog.math.isInt(recipient)) {
      url = bluemind.rcube.RCubeHelper.URL + '?_task=mail&_action=compose&_gid=' + recipient;
    } else {
      url = bluemind.rcube.RCubeHelper.URL + '?_task=mail&_action=compose&_to=' + recipient;
    }
    var options = {width: 1100 , height: 600, target: 'rc_compose_child'};
    var win = goog.window.open(url, options);
    return win;
  } else {
    return null;
  }
};
