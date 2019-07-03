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
 * BlueMind roundcube contacts manager
 * 
 */
 
require_once('bm_book.php');
require_once('bm_people_source.php');

class bm_contacts_manager {
  private $addressbookClient;
  private $addressbooksClient;
  private $containerClient;
  private $bmAddressbookIds;
  private $defaultAddressbook;
  private $userSubscriptionClient;

  function __construct() {
    unset($this->bmAddressbookIds);
  }

  public function setAddressBookCompletionIds() {
    $rcmail = rcmail::get_instance();
    $rcmail->config->set('autocomplete_addressbooks', array(0 => 'bm_autocomp'));
  }

  private function getAddressbookClient($container) {
    if(!isset($this->addressbookClient)) {
      $rcmail = rcmail::get_instance();
      $this->addressbookClient = new BM\AddressBookClient($_SESSION['bm']['core'], 
        $rcmail->decrypt($_SESSION['password']),
        $container);
    }
    return $this->addressbookClient;
  }

    private function getAddressbooksClient() {
    if(!isset($this->addressbooksClient)) {
      $rcmail = rcmail::get_instance();
      $this->addressbooksClient = new BM\AddressBooksClient($_SESSION['bm']['core'], 
        $rcmail->decrypt($_SESSION['password']));
    }
    return $this->addressbooksClient;
  }

  private function getContainerClient() {
    if(!isset($this->containerClient)) {
      $rcmail = rcmail::get_instance();
      $this->containerClient = new BM\ContainersClient($_SESSION['bm']['core'],
        $rcmail->decrypt($_SESSION['password']),
        $_SESSION['bm_sso']['bmDomain']);
    }
    return $this->containerClient;
  }


  private function getUserSubscriptionClient() {
    if(!isset($this->userSubscriptionClient)) {
      $rcmail = rcmail::get_instance();
      $this->userSubscriptionClient = new BM\UserSubscriptionClient($_SESSION['bm']['core'],
        $rcmail->decrypt($_SESSION['password']),
        $_SESSION['bm_sso']['bmDomain']);
    }
    return $this->userSubscriptionClient;
  }


  private function getFolders() {
   if (!isset($_SESSION['bmContactFolders'])) {
      $_SESSION['bmContactFolders'] = $this->getUserSubscriptionClient()->listSubscriptions($_SESSION['bm_sso']['bmUserId'], 'addressbook');
    }
    return $_SESSION['bmContactFolders'];
  }

  private function getFolderFromId($id) {
    $folder = $this->getContainerClient()->get($id);
    return $folder;
  }

  public function convertBMFolderToRCFolder($bmFolder) {
    return array(
          'id' => (string)$bmFolder->uid,
          'name' => $bmFolder->name,
          'readonly' => $bmFolder->writable ? false : true,
          'groups' => false
        );
  }

  public function addressbooks_list($addressBookList) {
    // Remove default RC addressbook
    unset($addressBookList['sources'][0]);

    $folders = $this->getFolders();

    foreach($folders as $folder) {
      $addressBookList['sources'][(string)$folder->containerUid] = $this->convertBMFolderToRCFolder($folder);
      $this->bmAddressbookIds[(string)$folder->containerUid] = true;
      $this->setDefaultAddressbook($folder);
    }

    // Update address book completions Ids
    $this->setAddressBookCompletionIds();
    return $addressBookList;
  }
  
  private function setDefaultAddressbook($folder) {
    $rcmail = rcmail::get_instance();
    if(!isset($_SESSION['bm']['book']['default'])) {
      if(isset($folder)
          && $folder->containerUid == 'book:CollectedContacts_' . $_SESSION['bm_sso']['bmUserId']
        ) {
        $_SESSION['bm']['book']['default'] = $folder->containerUid;
        $rcmail->config->set('default_addressbook', $folder->containerUid);
      }
    } elseif ($rcmail->config->get('default_addressbook') != $_SESSION['bm']['book']['default']) {
      $rcmail->config->set('default_addressbook', $_SESSION['bm']['book']['default']);
    }
  }
  
  public function getDefaultAddressbook() {
    if(!isset($_SESSION['bm']['book']['default'])) {
      $this->addressbooks_list(null);
    }

    return $_SESSION['bm']['book']['default'];
  }
   
  public function getPeopleSource() {
    return new bm_people_source();
  }
 
  public function addressbook_get($addressBook) {
    if (isset($addressBook['id']) && $addressBook['id'] == 'bm_autocomp') {
      return array('instance' => new bm_people_source());
    }

    if(!isset($addressBook['id']) || !array_key_exists($addressBook['id'], $this->bmAddressbookIds)) {
      return null;
    }

    $folder = $this->getFolderFromId($addressBook['id']);

    if(isset($folder)) {
      $addressbook['instance'] = new bm_book($folder->name, $folder, $this);
    }
    
    return $addressbook;
  }

  public function contact_create($data, $book) {
    if(!isset($this->bmAddressbookIds)) {
      $this->addressbooks_list(null);
    }

    if (!$book) {
      $book = $this->getDefaultAddressbook();
    }

    if(array_key_exists($book, $this->bmAddressbookIds)) {
      $contactClient = $this->getAddressbookClient($book);
      $vcard = $this->toVCard($data);
      $uid = uniqid();
      $ret = $contactClient->create($uid, $vcard);
      return $uid;
    }
    return null;
  }

  public function contact_collect($data, $book) {
    if(!isset($this->bmAddressbookIds)) {
      $this->addressbooks_list(null);
    }

    if (!$book) {
      $book = $this->getDefaultAddressbook();
    }

    if(array_key_exists($book, $this->bmAddressbookIds)) {
      $vcardQuery = new BM\VCardQuery();
      $vcardQuery->query = "value.communications.emails.value:\"" . $data['email'] . "\""; 
      $res = $this->getAddressbooksClient()->search($vcardQuery);
      if ($res->total == 0) {
        $vcard = $this->toVCard($data);
        $uid = uniqid();
        $this->getAddressbookClient($book)->create($uid, $vcard);
        return $uid;
      }
    }
    return null;
  }

  private function toVCard($rcContact) {

    $vcard = new BM\VCard();
    $vcard->kind = 'individual';

    // Identification
    if(isset($rcContact['firstname']) || isset($rcContact['surname'])) {
      if(isset($rcContact['firstname'])) {
        $vcard->identification->name->givenNames = $rcContact['firstname'];
      }

      if(isset($rcContact['surname'])) {
        $vcard->identification->name->familyNames = $rcContact['surname'];

      }
    } elseif(isset($rcContact['name'])) {
      $name = NULL; 
      if (strpos($rcContact['name'], '?') !== FALSE) {
		$name = iconv_mime_decode($rcContact['name'], 1, "UTF-8");
      } else {
		$name = $rcContact['name'];
	  }
	  $arr_name = explode( ' ', $name );
	  $vcard->identification->name->givenNames = $arr_name[0];
	  if (count($arr_name) > 1){
	    $famNames = "";
	    for($i=1; $i<count($arr_name); $i++) {
    		$famNames .= " ";
    		$famNames .= $arr_name[$i];
    	}
		$vcard->identification->name->familyNames = $famNames;
	  } 
    }
    if(isset($rcContact['email']) && $rcContact['email'] != '') {
      $email = new BM\VCardCommunicationsEmail();
      $email->value =$rcContact['email']; 
      $email->parameters[] = array('label'=> 'TYPE', 'value' => 'work');
      $vcard->communications->emails[] = $email;
    } else {
      if(array_key_exists('email:work', $rcContact)) {
        for($i=0; $i<count($rcContact['email:work']); $i++) {
          if($rcContact['email:work'][$i] != '') {
            $email = new BM\VCardCommunicationsEmail();
            $email->value = $rcContact['email:work'][$i]; 
            $email->parameters[] = array('label'=> 'TYPE', 'value' => 'work');
            $vcard->communications->emails[] = $email;
          }
        }
      }
      if(array_key_exists('email:other', $rcContact)) {
        for($i=0; $i<count($rcContact['email:other']); $i++) {
          if($rcContact['email:other'][$i] != '') {
            $email = new BM\VCardCommunicationsEmail();
            $email->value = $rcContact['email:other'][$i]; 
            $vcard->communications->emails[] = $email;
          }
        }
      }
    }

    if(array_key_exists('phone:work', $rcContact)) {
      for($i=0; $i<count($rcContact['phone:work']); $i++) {
        if($rcContact['phone:work'][$i] != '') {
          $phone = new BM\VCardCommunicationsTel();
          $phone->value = $rcContact['phone:work'][$i]; 
          $phone->parameters[] = array('label'=> 'TYPE', 'value' => 'work');
          $phone->parameters[] = array('label'=> 'TYPE', 'value' => 'voice');
          $vcard->communications->tels[] = $phone;
        }
      }
    }

    if(array_key_exists('jobtitle', $rcContact) && $rcContact['jobtitle'] != '') {
      $vcard->organizational->title = $rcContact['jobtitle'][0]; 
    }

    if(array_key_exists('spouse', $rcContact) && $rcContact['spouse'] != '') {
      $vcard->related->spouse = $rcContact['spouse'][0]; 
    }

    if(array_key_exists('organization', $rcContact) && $rcContact['organization'] != '') {
      $vcard->organizational->org->company = $rcContact['organization'];
    }

    if(array_key_exists('notes', $rcContact) && $rcContact['notes'] != '') {
      $vcard->explanatory->note .= $rcContact['notes'][0]; 
    }

    return $vcard;
  }
  
}
?>
