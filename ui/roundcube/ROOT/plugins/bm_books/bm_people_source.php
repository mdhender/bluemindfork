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
 * BlueMind autocomplete source
 * 
 */
class bm_people_source extends rcube_addressbook {
  public $primary_key = 'ID';
  public $readonly = true;
  public $groups = true;
  public $coltypes = array(
  'name',
  'firstname',
  'surname',
  'email'
);

  private $filter;
  private $result;
  private $folderDisplayName;
  private $folder;
  private $gid;
  private $dlists;
  private $addressbookClient;
  private $addressbooksClient;

  public function __construct() {
    $this->dlists = array();
    $this->addressbookClient = array();
  }
  
  private function getAddressbookClient($containerUid) {
    if(!isset($this->addressbookClient[$containerUid])) {
      $rcmail = rcmail::get_instance();
      $this->addressbookClient[$containerUid] = new BM\AddressBookClient($_SESSION['bm']['core'], 
        $rcmail->decrypt($_SESSION['password']),
        $containerUid);
    }
    return $this->addressbookClient[$containerUid];
  }

  private function getAddressbooksClient() {
    if(!isset($this->addressbooksClient)) {
      $rcmail = rcmail::get_instance();
      $this->addressbooksClient = new BM\AddressBooksClient($_SESSION['bm']['core'], 
        $rcmail->decrypt($_SESSION['password']));
    }
    return $this->addressbooksClient;
  }
  
  private function expandAndFilterEmails($name, $emails, $terms, $pattern) {
    $unmatched = array();
    $smartFilter = $this->containsDomainEmail($emails);
    if (count($emails) == 1 || $this->match($name, $terms, $unmatched)) {
      $mails = $emails;
    } else {
      $mails = $this->filterEmails($emails, $unmatched, $smartFilter);
    }
    
    if (count($mails) == 1) {
      return $mails;
    } elseif (count($mails) >= count($emails)) {
       if ($smartFilter) {
        return $this->selectEmail($emails, $pattern);
      } else {
        return $emails;
      }     
    }
    return $mails;
  }

  private function containsDomainEmail($emails) {
    list($login, $domain) = explode('@', $_SESSION['bm_sso']['bmLogin']);
    if (!$emails) {
      return false;
    }
    foreach($emails as $email) {
      if (strpos($email->value, '@' . $domain) !== FALSE) {
        return true;
      }
    }
    return true;
  }

  private function match($string, $terms, &$unmatched = null) {
    $string = strtolower($string);
    $string = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $string);
    foreach($terms as $term) {
      if (strpos($string, $term) === FALSE) {
        if (is_array($unmatched)) {
          $unmatched[] = $term;
        } else {
          return false;
        }
      }
    }
    return (count($unmatched) == 0);
  }

  private function selectEmail($emails, $pattern) {
    if (preg_match("/[ .,\"'?!;:#$%&()+-\/<>=\\\\@^_{}|~\\[\\]]/", $pattern)) {
      $pattern = strtolower($pattern);
      $searchExactMatch = true;
    }

    foreach($emails as $email) {
      $all[] = $email;
      $default = null;
      foreach($email->parameters as $param) {
        if ($param->label == "DEFAULT" && $param->value == "true") {
          $default = $email;
        }
      }

      if ($default != null) {
        if (!$searchExactMatch) return array($default);
        $default = $email;
        $matches[] = $email;
      } elseif (strpos($email->value, $pattern) !== FALSE) {
        $matches[] = $email;
      }
    }
    if ($searchExactMatch && count($matches) <= count($emails) && count($matches) >= 0) {
      return $matches;
    }
    if ($default) {
      return array($default);
    }
    return $all;
  }

  private function filterEmails($emails, $terms, $smartFilter) {
    $mails = array();
    if (!$emails) {
      return $mails;
    }
    foreach($emails as $email) {
      $mail = $email->value;

      foreach($email->parameters as $param) {
        if ($param->label == "DEFAULT" && $param->value == "true") {
          $default = $email;
        }
      }

      if ($this->match($email->value, $terms)) {
        $mails[] = $email;
      }
    }
    if ($smartFilter && $mails > 1) {
      $domain = array_pop(explode('@', $default->value));
      $lefts = array();
      $filtered = array();
      foreach($mails as $mail) {
        list($l,$r) = explode('@',$mail->value);
        if ($r == $domain) {
          $lefts[] = $l;
          $filtered[] = $mail;
        }
      }
      foreach($mails as $mail) {
        list($l,$r) = explode('@',$mail->value);
        if ($r != $domain && !in_array($l, $lefts)) {
          $filtered[] = $mail;
        }
      }
      $mails = $filtered;
    }

    return $mails;
  }

  private function expand($uid, $contact, $mails) {
    // FIXME rsort($mails);
    $result = array();
    if ($mails != null) {
      foreach($mails as $mail) {
        $result[] = array('ID' => $uid,
          'name' => $contact->identification->formatedName->value,
          'firstname' => $contact->identification->name->givenNames,
          'surname' => $contact->identification->name->familyNames,
          'email' => $mail->value);
      }
    }
    return $result;
  }

  public function get_name() {
    return "autocomplete";
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
    $search = $this->escapeESQuery($search);
    $query = "value.kind: 'group' AND value.identification.formatedName.value:($search)"; 
    $res = $this->doSearchQuery($query);
    $groups = $res->values;
    if ($groups) {
      foreach ($groups as $group) {
        if ($group->value->mail) {
          array_push($ret, array('ID' => $group->uid, 'email' => array($group->value->mail), 'name' => $group->displayName, 'source' => 'bm_autocomp'));
          $this->dlists[$group->uid] = array('email' => $group->value->mail, 'name' => $group->displayName);
        } else {
          array_push($ret, array('ID' => $group->containerUid."/".$group->uid, 'name' => $group->displayName, 'source' => 'bm_autocomp'));
        }
      }
    }

    return $ret;
  }

  function get_group($group_id) {
    return $this->dlists[$group_id];
  }

  public function set_group($gid) { 
    $this->gid = $gid;
  }
  
  public function list_records($cols=null, $subset=0) {
    if ($this->gid) {
      $splitted = explode('/',$this->gid);
      $ab = $this->getAddressbookClient($splitted[0]);
      $members = new rcube_result_set();
      $this->expandMembers($splitted[0], $splitted[1], $members);
      return $members;
   }
    error_log("unused for autocomplete");
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
            $containerUid = $m->containerUid;
            if (is_null($containerUid)){
              $containerUid = $container;
            }
            $this->expandMembers($containerUid, $m->itemUid, $members);
          }
        } 
    }
  }
  private function escapeESQuery($string) {
    $regex = "/[\\+\\=\\&\\|\\!\\(\\)\\{\\}\\[\\]\\^\\\"\\~\\*\\<\\>\\?\\:\\\\\\/]/";
    return preg_replace($regex, ' ', $string);
  }

  public function search($fields, $value, $strict=false, $select=true, $nocount=false, $required=array()) {
    $value = $this->escapeESQuery($value);
    $this->result = new rcube_result_set();
    if (strlen($value)) {
      $query = "value.kind: 'individual' AND (value.identification.formatedName.value:($value) OR value.communications.emails.value:($value)) AND _exists_:value.communications.emails.value"; 
      $res = $this->doSearchQuery($query);
      $this->result->count = $res->total;
      $terms = $this->splitSearch($pattern);
      $contacts = $res->values;
      if ($contacts) {
        foreach ($contacts as $contact) {
          $contactItem = $this->getAddressbookClient($contact->containerUid)->getComplete($contact->uid);
          $c = $contactItem->value;
          $mails = $this->expandAndFilterEmails($c->identification->formatedName->value,
            $c->communications->emails, $terms, $vcardQuery->query);
          $rcs = $this->expand($contact->uid, $c, $mails);
          foreach($rcs as $rc) {
            $this->result->add($rc);
          }
        }
      }
    }
    return $this->result;
  }

  private function doSearchQuery($query) {
    $q = new BM\VCardQuery();
    $q->query = $query; 
    $q->orderBy = 'Pertinance';
    $q->size = 10;
    return $this->getAddressbooksClient()->search($q);
  }

  private function splitSearch($string) {
    $string = trim(strtolower($string));
    $string = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $string);
    return array_filter(preg_split("/[ .,\"'?!;:#$%&()+-\/<>=\\\\@^_{}|~\\[\\]]/", $string));
  }

  public function count() {
    return new rcube_result_set(1);
  }

  public function get_result() {
    return $this->result;
  }
  public function get_record($id, $assoc=false) {
/* Useless
    $addressbookClient = $this->getAddressbookClient();
    $currentContact = $addressbookClient->getContactFromId($id);

    $contact = $this->convertBMContactToRCContact($currentContact);

    $this->result = new rcube_result_set(1);
    $this->result->add($contact);   

    return $contact;
 */
  }
  public function create_group($name) {
    $result = false;

    return $result;
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
}
?>
