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
 * BlueMind preference list customization
 */

class bm_preferenceList extends rcube_plugin {
  public $task = 'settings';

  public function init() {
    // settings hooks
    $this->add_hook('preferences_sections_list', array($this, 'preferences_sections_list'));
  }

  // settings hooks
  public function preferences_sections_list($args) {
    unset($args['list']['addressbook']);
    unset($args['list']['folders']);

    return $args;
  }
}
