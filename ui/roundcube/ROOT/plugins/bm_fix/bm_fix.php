<?php
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
?>
<?php

/**
 * This plugin is a bag to put all modification to fix unattended behaviour of 
 * rc that can be alter with a plugin. 
 *
 */

class bm_fix extends rcube_plugin {

  public function init() {
    $this->include_script('bm_fix.js');

    // Add some translated title
    $this->api->output->add_label('unread');
    $this->api->output->add_label('flagged');
    washtml::$block_elements[] = 'textarea';
    $this->add_hook('message_part_get', array($this, 'do_not_show_svg'));    

  }

  public function do_not_show_svg($args) {
    $mime = $args['mimetype'];
    $file = $args['filename'];
    $secondary = $args['ctype_secondary'];
    if (preg_match('/^image\/svg/i', $mime) || preg_match('/\.(svg)$/i', $file) || preg_match('/\.(svg)$/i', $secondary)) {
      $args['download'] = true;
    }
    return $args;
  }

  // global hooks
}
