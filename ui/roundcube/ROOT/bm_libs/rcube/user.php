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

class bm_user extends rcube_user {

  private $client;

  private $cache;

  /**
   * Object constructor
   *
   * @param int   $id      User id
   * @param array $sql_arr SQL result set
   */
  function __construct($user) {
    parent::__construct($user->ID, $user->data);
    $rcmail = rcmail::get_instance();
    $this->client = new BM\UserMailIdentitiesClient($_SESSION['bm']['core'], 
        $rcmail->decrypt($_SESSION['password']),
        $_SESSION['bm_sso']['bmDomain'], $_SESSION['bm_sso']['bmUserId']);
  }
  
  function get_identity($id = null) {
    $result = $this->list_identities($id);
    return $result[0];
  }

  function get_matching_identity($to, $cc = array(), $mbox = NULL) {
    $identities = $this->list_identities();
    $default = $identities[0];
    $user = $_SESSION['bm_sso']['bmUserId'];
    if ($mbox == null) $mbox = $user;
    $priority = array($mbox => 8, $default['owner'] => 16, $user => 32);
    $found = array();
    foreach($identities as $id => $identity) {
      $email = format_email($identity['email']);
      $email = format_email(rcube_idn_to_utf8($email));
      $score = ($identity['standard']) | ($priority[$identity['owner']]);
      if (($t = array_search($email, $to)) !== false || ($c = array_search($email, $cc)) !== false) {
        $score = $score | ($t ? 4 : 0) | ($c ? 2 : 0);
        if ($score > $found['score']) 
          $found = array('score' => $score, 'identity' => $identity);
      }
    }
    return $found['identity'] ? $found['identity'] : $default;
  }

  
  function list_identities($id = '')  {
    $result = array();
    if (!$this->cache) {
      $identities = $this->client->getIdentities();
      foreach($identities as $identityDescriptor) {
      $identity = $this->client->get($identityDescriptor->id);
      if(!$id || $id == $identityDescriptor->id) {
          $entry = array();
          $entry['identity_id'] = $identityDescriptor->id;
          $entry['user_id'] = $identity->mailboxUid;
          $entry['owner'] = $identity->mailboxUid;
          $entry['changed'] = '';
          $entry['del'] = '0';
          $entry['standard'] = ($identity->isDefault) ? 1 : 0;
          $entry['name'] = $identity->displayname;
          $entry['organisation'] = '';
          $entry['email'] = $identity->email;
          $entry['reply-to'] = '';
          $entry['sent'] = $identity->sentFolder;
          $entry['bcc'] = '';
          $entry['signature'] = $identity->signature ? $identity->signature : ' ';
          $entry['html_signature'] = ($identity->format == "HTML");
          $entry['text'] = format_email_recipient($entry['email'], $entry['name']);
          if ( $identity->name ) {
             $entry['text'] .= " - " . $identity->name;
          }
          if ($entry['standard']) {
            array_unshift($result, $entry);
          } else {
            array_push($result, $entry);
          }
        }
      }
      $this->cache = $result;
    }
    return $this->cache;
  }
}
