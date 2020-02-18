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
 * BlueMind address book
 * 
 */
class bm_book extends rcube_addressbook {
  public $primary_key = 'ID';
  public $readonly = true;
  public $groups = true;
  public $coltypes = array(
  'firstname',
  'surname',
  'email',
  'jobtitle',
  'organization',
  'phone',
  'spouse',
  'notes',
  'photo'
);

  private $filter;
  private $result;
  private $folderDisplayName;
  private $folder;
  private $manager;

  public function __construct($folderDisplayName, $folder, $manager) {
    $this->folderDisplayName = $folderDisplayName;
    $this->folder = $folder;
    $this->manager = $manager;
    $this->readonly = !$folder->writable;
    $this->ready = true;
    $this->setContactColTypes();
    $this->dlists = array();
  }
  
  private function setContactColTypes() {
    global $CONTACT_COLTYPES;
    $CONTACT_COLTYPES['email']['subtypes'] = array('work');
    $CONTACT_COLTYPES['phone']['subtypes'] = array('work');
    $CONTACT_COLTYPES['website']['subtypes'] = array('work');
  }

  private function getAddressbooksClient() {
    if(!isset($this->addressbooksClient)) {
      $rcmail = rcmail::get_instance();
      $this->addressbooksClient = new BM\AddressBooksClient($_SESSION['bm']['core'], 
        $rcmail->decrypt($_SESSION['password']));
    }
    return $this->addressbooksClient;
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


 private function convertBMMemberToRCContact($member, $cols = null) {
    $rcContact = array('ID' => $member->getId(),
      'name' => $member->getDisplayName(),
      'email' => $member->getEmail()
    );

    return $rcContact;
  }
  private function isDefault($email) {
          $parameters = $email->parameters;
          foreach($parameters as $parameter){
                  if($parameter->label == 'DEFAULT') {
                    return $parameter->value == "true";
                  }
          }
          return false;
  }

  private function convertBMContactToRCContact($contactItem, $cols = null) {
    $contact = $contactItem->value;
    $rcEmails = array();
    $bmEmails = $contact->communications->emails;
    if ($bmEmails) {
      foreach($bmEmails as $bmEmail) {
        if ($this->isDefault($bmEmail)) {
          array_unshift($rcEmails, $bmEmail->value);
	} else {
          array_push($rcEmails, $bmEmail->value);
        }
      }
    }

    $rcPhones = array();
    $bmPhones = $contact->communications->tels;
    if ($bmPhones) {
      foreach($bmPhones as $bmPhone) {
        array_push($rcPhones, $bmPhone->value);
      }
    }

    $rcContact = array('ID' => $contactItem->uid,
      'name' => $contact->identification->formatedName->value,
      'firstname' => $contact->identification->name->givenNames,
      'surname' => $contact->identification->name->familyNames,
      'email' => $rcEmails,
      'phone' => $rcPhones
    );

    $jobTitle = $contact->organizational->title;
    if(isset($jobTitle) && $jobTitle != '') {
      $rcContact['jobtitle'] = $jobTitle;
    }

    $spouse = $contact->related->spouse;
    if(isset($spouse) && $spouse != '') {
      $rcContact['spouse'] = $spouse;
    }

    $company = $contact->org->values[0];
    if(isset($company) && $company != '') {
      $rcContact['organization'] = $company;
    }
    
    $comment = $contact->explanatory->note;
    if(isset($comment) && $comment != '') {
      $rcContact['notes'] = $comment;
    }

    if ($cols != null) {
      $rcContactLite = array();
      $rcContactLite['ID'] = $rcContact['ID'];      
      foreach($cols as $col) {
        $rcContactLite[$col] = $rcContact[$col];
      }
      $rcContact = $rcContactLite;
    }
    return $rcContact;
  }

  public function get_name() {
    return $this->folderDisplayName;
  }

  public function set_search_set($filter) {
    $this->filter = $filter;
  }

  public function get_search_set() {
    return $this->filter;
  }

  public function reset() {

    $this->result = null;
    $this->filter = null;
  }

  public function list_groups($search = null) {
    $ret = array();
    $vcardQuery = new BM\VCardQuery();
    $vcardQuery->query = "value.kind: 'group'"; 
    $vcardQuery->size = $this->page_size;
    $vcardQuery->from = $this->page_size * (($this->list_page) ? $this->list_page - 1:0);
    $groupsFound = $this->getAddressbookClient($this->folder->uid)->search($vcardQuery);
    $result = new rcube_result_set($groupsFound->total, $page * $this->page_size);
    if ($groupsFound->values) {
      foreach($groupsFound->values as $group) {
      if ($group->value->defaultEmail) {
        $result->add(array('ID' => $group->uid, 'email' => array($group->value->defaultEmail), 'name' => $group->displayName, 'source' => 'bm_autocomp'));
        $this->dlists[$group->uid] = array('email' => $group->value->defaultEmail, 'name' => $group->displayName);
      } else {
        $result->add(array('ID' => $group->uid, 'name' => $group->displayName, 'source' => 'bm_autocomp'));
      }

      }
    }
    return $result;
  }

  public function set_group($gid) { 
    $this->gid = $gid;
  }

  private function expandMembers($container, $uid, $members) {
    $ab = $this->getAddressbookClient($container);
    $res = $ab->getComplete($uid);
    if ($res) {
        $item = $res;
        foreach ($item->value->organizational->member as $m) {
          if ($m->mailto) {
            $rc = array('ID' => $m->itemUid,
              'name' => $m->commonName,
              'email' => $m->mailto
            );
            $members->add($rc);
          } else {
            $this->expandMembers($m->containerUid, $m->itemUid, $members);
          }
        }
    }
  }

  public function list_records($cols=null, $subset=0) {
    if (!$this->result && $this->gid) {
      $this->result = new rcube_result_set();
      $q = new BM\VCardQuery();
      $q->query = "value.kind: 'group' AND uid: $this->gid";
      $q->size = 1;
      $res = $this->getAddressbooksClient()->search($q);
      $dlists = $res->values;
      if (sizeof($dlists) == 1) {
        $dlist = $dlists[0];
        $item = $this->getAddressbookClient($dlist->containerUid)->getComplete($this->gid);
        foreach ($item->value->organizational->member as $m) {
          if ($m->mailto) {
            $rc = array('ID' => $m->itemUid,
              'name' => $m->commonName,
              'email' => $m->mailto
            );
            $this->result->add($rc);
          } else {
            $this->expandMembers($m->containerUid, $m->itemUid, $this->result);
          }

        }
      }
    } elseif (!$this->result) {
      $vcardQuery = new BM\VCardQuery();
      $vcardQuery->query = "value.kind: 'individual'"; 
      $vcardQuery->size = $this->page_size;
      $vcardQuery->from = $vcardQuery->size * (($this->list_page) ? $this->list_page - 1:0);
      $vcardQuery->orderBy = null;
      $contactsFound = $this->getAddressbookClient($this->folder->uid)->search($vcardQuery);

      $count = $contactsFound->total;
      $this->result = new rcube_result_set($count, $page * $this->page_size);
      $items = $contactsFound->values;
      if ($items) {
        $uids = array();
        foreach ($items as $item) {
          $uids[] = $item->uid;
        }
        $contacts = $this->getAddressbookClient($this->folder->uid)->multipleGet($uids);
        foreach($contacts as $contact) {
          $rcContact = $this->convertBMContactToRCContact($contact, $cols);
          $this->result->add($rcContact);
        }

        usort($this->result->records, 'cmp_bm_book_contact');
      }
    }
    return $this->result;
  }

  public function emailExists($container, $email) {
    $query = "value.kind: 'individual' AND value.communications.emails.value:$email"; 

    $q = new BM\VCardQuery();
    $q->query = $query; 
    $q->size = 1;
    $res = $this->getAddressbookClient($container)->search($q);

    return $res->total > 0;
  }

  public function search($fields, $value, $strict=false, $select=true, $nocount=false, $required=array()) {
    $value = preg_replace("/[\"'!()\/=\\\\{}\\[\\]]/", "", $value);
    $this->result = new rcube_result_set();
    if (strlen($value)) {
      $query = "value.kind: 'individual' AND value.communications.emails.value:$value AND _exists_:value.communications.emails.value"; 
      $res = $this->doSearchQuery($query);
      $this->result->count = $res->total;
    }
    return $this->result;
  }

  private function doSearchQuery($query) {
    $q = new BM\VCardQuery();
    $q->query = $query; 
    $q->size = 10;
    return $this->getAddressbooksClient()->search($q);
  }

  public function count() {
    return new rcube_result_set(1);
  }

  public function get_result() {
    return $this->result;
  }

  public function get_record($id, $assoc=false) {
/* Useless
    $bmContactClient = $this->getAddressbookClient();
    $currentContact = $bmContactClient->getContactFromId($id);

    $contact = $this->convertBMContactToRCContact($currentContact);

    $this->result = new rcube_result_set(1);
    $this->result->add($contact);   

    return $contact;
 */
  }
  public function create_group($name) {
    return false;
  }

  public function delete_group($gid) {
    return false;
  }

  public function rename_group($gid, $newname) {
    return $newname;
  }

  public function add_to_group($group_id, $ids) {
    return false;
  }

  public function remove_from_group($group_id, $ids) {
     return false;
  }

  public function insert($data, $check=false) {
    if ($this->readonly) {
      $data['abort'] = true;
      return $data;
    }
    $data = $this->manager->contact_create($data, $this->folder->uid);
    return $data;
  }

  public function collect($data) {
    if ($this->readonly) {
      $data['abort'] = true;
      return $data;
    }

    $data = $this->manager->contact_collect($data, $this->folder->uid);
    return $data;
  }

  public function update($id, $data) {
    $args = $this->manager->contact_update($id, $data, $this->folder->uid);
    return $args;
  }

  public function delete($ids, $force=true) {
    return $this->contactsManager->contact_delete($ids, $this->folder->uid);
    
  }
  public function get_group($group_id) {
    return $this->dlists[$group_id];
  }
}

function cmp_bm_book_contact($a, $b) {
    return strcmp(unaccent(strtolower($a['name'])), unaccent(strtolower($b['name'])));
}

function unaccent($str, $charset='utf-8') {
    $str = htmlentities($str, ENT_NOQUOTES, $charset);
    
    $str = preg_replace('#&([A-za-z])(?:acute|cedil|caron|circ|grave|orn|ring|slash|th|tilde|uml);#', '\1', $str);
    $str = preg_replace('#&[^;]+;#', '', $str);
    
    return $str;
}
?>
