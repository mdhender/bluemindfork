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

class bm_search extends rcube_plugin {

  private $rcmail;

 
  public function init() {
    $this->rcmail = rcmail::get_instance();
    $this->add_hook('startup', array($this, 'startup'));
    $this->add_texts('localization', true);

  }

  public function startup($args) {
    if ($this->rcmail->task == 'mail' && $this->rcmail->action == '') {
      if ($this->rcmail->get_storage()->isIndexEnabled()) { 
        $this->include_script('search.js');
        $this->include_script('bm_search.js');
        $css = 'skins/bluemind/bm_search.css';
        $this->include_stylesheet($css);
        $css = 'skins/bluemind/search.css';
        $this->include_stylesheet($css);
        $this->add_texts('localization', true);
      }
    }
    return $args;
  }

}

?>
