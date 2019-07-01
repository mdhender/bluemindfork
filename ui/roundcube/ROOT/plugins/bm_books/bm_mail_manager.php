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
 * BlueMind roundcube mail manager
 * 
 */

class bm_mail_manager {
  private $contactsManager;
  
  function __construct($contactsManager) {
    $this->contactsManager = $contactsManager;
  }

  public function message_sent($args) {
    try {
      $dstContacts = array();
      $this->parseDstContacts($dstContacts, $args['headers']['To']);
      $this->parseDstContacts($dstContacts, $args['headers']['Cc']);
      $this->parseDstContacts($dstContacts, $args['headers']['Bcc']);

      $this->createNewBMContacts($dstContacts);
    } catch (Exception $e) {
      write_log('errors', $e->getMessage());
    }
  }

  public function message_compose($args) {
    $return = array();
    // BM-5373, c'est là que ça se passe
    if ($args['param']['gid']) {
      $source = $this->contactsManager->getPeopleSource(); 
      $source->set_group($args['param']['gid']);
      $contacts = $source->list_records();
      $members = array();
      while ($contacts && ($data = $contacts->iterate())) {
        foreach ((array)$data['email'] as $email) {
          $members[] = format_email_recipient($email, rcube_addressbook::compose_list_name($data));
          break;  // only expand one email per contact
        }
      } 
      $mailto = join(', ', $members);
      $mailtoId = substr(md5($mailto), 0, 16);
      $_SESSION['mailto'][$mailtoId] = urlencode($mailto);
      $return['param'] = array('mailto' => $mailtoId);
    }
    return $return;
  }

  private function parseDstContacts(&$dstContacts, $inlineContacts) {
    $contacts = explode(',', $inlineContacts);

    foreach($contacts as $contact) {
      $matches = preg_match('/^(.*)<(.+)>.*$/', $contact, $parts);
      if($matches == 0) {
        continue;
      }

      $name = trim($parts[1]);
      if($name != '') {
        $name = str_replace('"', '', $name);
        $name = str_replace('.', ' ', $name);
        $rcContact['name'] = $name;
      } else {
        $name = explode('@', $parts[2]);
        $rcContact['name'] = $name[0];
      }

      $rcContact['email'] = $parts[2];

      array_push($dstContacts, $rcContact);
    }
  }

  private function createNewBMContacts($dstContacts) {
    $rcmail = rcmail::get_instance();
    $defaultAddressbook = $this->contactsManager->getDefaultAddressbook();

    if ($defaultAddressbook == null) {
      return;
    }
    
    foreach($dstContacts as $contact) {
        $plugin = $rcmail->plugins->exec_hook('contact_create', array('record' => $contact, 'source' => $defaultAddressbook));
        if(!$plugin['abort']) {
          $addressbook = $rcmail->get_address_book($defaultAddressbook, true);
          $addressbook->collect($contact);
        }
    }
  }
  
  private function emailExist($email) {
    /* search in contacts */
    $rcmail = rcmail::get_instance();
    $sources = $rcmail->get_address_sources();
    
    foreach($sources as $s) {
      $source = $rcmail->get_address_book($s['id']);
      $result = $source->emailExists($s['id'], $email);
      if($result) {
        return true;
      }
    }
    
    /* search in groups */
    $bmGroupClient = $this->getBlueMindGroupClient();
    $groupQuery = new BlueMind_Model_GroupQuery();
    $groupQuery->setEmail($email);
    $groupsFound = $bmGroupClient->find($groupQuery);

    $count = $groupsFound->getNumFound();
    if ($count != 0) {
       return true;
    }

    return false;
  }

  private function getBlueMindGroupClient() {
    if(!isset($this->bmGroupClient)) {
      $rcmail = rcmail::get_instance();

      BlueMind_Service_ClientFactory::setStrategy(new LocateCoreStrategy());
      $this->bmGroupClient = BlueMind_Service_ClientFactory::getGroupClient($rcmail->decrypt($_SESSION['password']));
    }

    return $this->bmGroupClient;
  }


}
?>
