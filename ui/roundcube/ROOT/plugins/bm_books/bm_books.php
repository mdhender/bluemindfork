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
 * BlueMind address books support
 *
 * Make use of BlueMind address books from Roundcube
 *
 */

require_once('bm_contacts_manager.php');
require_once('bm_mail_manager.php');

class bm_books extends rcube_plugin {
  public $task = 'mail|addressbook|settings';

  private $contactsManager;
  private $mailManager;

  private $contactBMClient;
  private $translatedAddressbooksName;

  const USERS = 'users';
  const CONTACTS = 'contacts';
  const COLLECTED_CONTACTS = 'collected_contacts';

  function __construct($api) {
    parent::__construct($api);
    $this->add_texts('localization/');
    $this->setTranslatedAddressbooksName();
    $this->contactsManager = new bm_contacts_manager($this->translatedAddressbooksName);
    $this->mailManager = new bm_mail_manager($this->contactsManager);
  }


  private function setTranslatedAddressbooksName() {
    $this->translatedAddressbooksName[$this::USERS] = $this->gettext($this::USERS);
    $this->translatedAddressbooksName[$this::CONTACTS] = $this->gettext($this::CONTACTS);
    $this->translatedAddressbooksName[$this::COLLECTED_CONTACTS] = $this->gettext($this::COLLECTED_CONTACTS);
  }
  
  public function init() {
    // global hooks
    $this->add_hook('ready', array($this, 'ready_hook'));

    // addressbook hooks
    $this->add_hook('addressbooks_list', array($this, 'addressbooks_list'));
    $this->add_hook('addressbook_get', array($this, 'addressbook_get'));
    $this->add_hook('message_compose', array($this, 'message_compose'));
    // mail hooks
    $this->add_hook('message_sent', array($this, 'message_sent'));
  }
  
  // global hook
  public function ready_hook($args) {
    $this->contactsManager->addressbooks_list(null);
  }

  // addressbook
  public function addressbooks_list($addressBookList) {
    return $this->contactsManager->addressbooks_list($addressBookList);
  }

  public function addressbook_get($addressBook) {
    return $this->contactsManager->addressbook_get($addressBook);
  }

  // mail
  public function message_sent($args) {
    $this->mailManager->message_sent($args);
  }
  // mail
  public function message_compose($args) {
    return $this->mailManager->message_compose($args);
  }
}
?>
