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
 * Blue Mind mailbox tree
 *
 * Alter Blue Mind mailbox tree
 *
 */

class bm_mailboxTree extends rcube_plugin {
  public $task = '.*';

  private $ssoLogin;
  private $ssoToken;
  private $client;
  private $rcmail;
  private $hierarchy;

  public function init() {
    // global hooks
    $this->rcmail = rcmail::get_instance();
    $this->add_hook('render_mailboxlist', array($this, 'render_mailboxlist'));
    $this->include_script('bm_mailboxTree.js');
    $this->hierarchy = new rcube\MyHierarchy();    

  }

  // Render mailbox hook
  public function render_mailboxlist($args) {
    if($args['list']['Outbox'] !== NULL) {
      $args['list']['Outbox']['name'] = rcube_label("outbox");
    }

    $this->translateFolder($args['list']);
    $this->translateSharedFolder($args);

    $this->translateOtherUsers($args);

    return $args;
  }

  public function translateFolder(&$folders) {
    if (is_array($folders)) {
      foreach($folders as &$folder) {
	if (!empty($folder['folders'])) {
	   $this->translateDefaultDirs($folder);
           $this->translateFolder($folder['folders']);
        }
      }
    }
  }

  public function translateSharedFolder(&$args) {
    $nameSpaceName = "Dossiers partag&AOk-s";
    if($args['list'][$nameSpaceName] !== NULL) {
      $args['list'][$nameSpaceName]['name'] = rcube_label("sharedfolders");
    }else {
      $nameSpaceName = "Shared Folders";

      if($args['list'][$nameSpaceName] !== NULL) {
        $args['list'][$nameSpaceName]['name'] = rcube_label("sharedfolders");
      }else {
          return $args;
      }
    }
    return $args;
  }

  public function translateOtherUsers(&$args) {
    $nameSpaceName = "Autres utilisateurs";
    if($args['list'][$nameSpaceName] !== NULL) {
      $args['list'][$nameSpaceName]['name'] = rcube_label("otherusers");
    }else {
      $nameSpaceName = "Other Users";

      if($args['list'][$nameSpaceName] !== NULL) {
        $args['list'][$nameSpaceName]['name'] = rcube_label("otherusers");
      }else {
        return $args;
      }
    }

    if($args['list'][$nameSpaceName]['folders'] !== NULL) {
      foreach(array_keys($args['list'][$nameSpaceName]['folders']) as $userBoxName) {
        $args['list'][$nameSpaceName]['folders'][$userBoxName]['name'] = $this->getUserDisplayName($userBoxName);
      }
    }

    return $args;
  }

  public function getUserDisplayName($userBoxName) {
    $mbox = $this->hierarchy->getMailboxByName($userBoxName);
    return $mbox->displayName;
  }

  public function translateDefaultDirs(&$userBox) {
    if($userBox['folders'] === NULL) {
      return $userBox;
    }

    if($userBox['folders']['Drafts'] !== NULL) {
      $userBox['folders']['Drafts']['name'] = rcube_label("drafts");
    }

    if($userBox['folders']['Junk'] !== NULL) {
      $userBox['folders']['Junk']['name'] = rcube_label("junk");
    }

    if($userBox['folders']['Outbox'] !== NULL) {
      $userBox['folders']['Outbox']['name'] = rcube_label("outbox");
    }

    if($userBox['folders']['Sent'] !== NULL) {
      $userBox['folders']['Sent']['name'] = rcube_label("sent");
    }

    if($userBox['folders']['Trash'] !== NULL) {
      $userBox['folders']['Trash']['name'] = rcube_label("trash");
    }

    return $userBox;
  }

}
