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
namespace rcube;

class MyHierarchy {
  private $mailboxes;
  private $folders;

  public function __construct() {
  }

  public function __call($name, $arguments) {
    $this->_initialize();
    return call_user_func_array(array($this, $name), $arguments);
  }

  private function _containers() {
    $rcmail = \rcmail::get_instance();
    return new \BM\ContainersClient($_SESSION['bm']['core'], 
      $rcmail->decrypt($_SESSION['password']), "mboxes_" . $_SESSION['bm_sso']['bmDomain']);
  }


  private function _mailboxfolders($mbox) {
    $rcmail = \rcmail::get_instance();
    if ($mbox == null) {
      $mbox = "user." . str_replace('.', '^', $_SESSION['bm_sso']['bmUserId']);
    }
    return new \BM\MailboxFoldersClient($_SESSION['bm']['core'], 
      $rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmPartition'], $mbox); 

  }

  private function _directory() {
    $rcmail = \rcmail::get_instance();
    return new \BM\DirectoryClient($_SESSION['bm']['core'], 
      $rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmDomain']);
  }

  private function _mailboxes() {
    $token = file_get_contents('/etc/bm/bm-core.tok');
    return new \BM\MailboxesClient($_SESSION['bm']['core'], 
      $token, $_SESSION['bm_sso']['bmDomain']);
  }

  private function _initialize() {
    if (!isset($_SESSION['bm']['hierarchy'])) {
      $mailboxes = $this->_loadMboxes();
      if (!empty($mailboxes)) {
        $_SESSION['bm']['hierarchy'] = array (
          'mailboxes' => $mailboxes,
          'folders' => $this->_loadFolders($mailboxes)
        );
      }
    }
    if (isset($_SESSION['bm']['hierarchy'])) {
      $this->mailboxes = $_SESSION['bm']['hierarchy']['mailboxes'];
      $this->folders = $_SESSION['bm']['hierarchy']['folders'];
    }
    
  }

  private function _loadMboxes() {
    // Using FolderHierarchy instead of container ?
    // FolderHierarchy would allow us to calculate folder without querying
    // the directory, but we need to query directory to get the displayname
    // so...
    $query = new \BM\ContainerQuery();
    $query->owner = null;
    $query->name = null;
    $query->type = 'mailboxacl';
    $query->verb = null; 
    $containers = $this->_containers()->allLight($query);
    $mailboxes = array();
    foreach($containers as $container) {
      $mailbox = new \stdClass;
      $mailbox->uid =  $container->owner;
      $mailboxes[$mailbox->uid] = $mailbox;
    }
    return $this->_loadMailboxData($mailboxes);
  }

  private function _loadMailboxData($mailboxes) {
    if (!empty($mailboxes)) {    
      $uids = array();
      foreach($mailboxes as $mbox) {
        $uids[] = $mbox->uid;
      }
      $items = $this->_mailboxes()->multipleGet(new \BMSerializableArray($uids));
      $rcmail = \rcmail::get_instance();      
      $usersNS = $rcmail->config->get('users_mbox');
      $mailshareNS = $rcmail->config->get('mailshares_mbox');
      foreach($items as $item) {
        if ($mailbox = $mailboxes[$item->uid]) {
          $mailbox->displayName = $item->displayName;
          $mailbox->kind = $item->value->type;
          $mailbox->name = $item->value->name;
          if ($item->uid == $_SESSION['bm_sso']['bmUserId']) {
            $mailbox->path = '';        
          } else {
            $mailbox->path = ($mailbox->kind == 'user' ? $usersNS : $mailshareNS) .  '/' . \rcube_charset::utf8_to_utf7imap($mailbox->name);
          }
        }
      }
    }
    return $mailboxes;    
  }

  private function _loadFolders($mailboxes) {
    $folders = array();
    foreach($mailboxes as $uid => $mailbox) {
      if ($mailbox->kind == "user") {
        $root = "user." . $uid;
      } else {
        $root =  $mailbox->name;
      }
      try {
        $all = $this->_mailboxfolders($root)->all();
      } catch (\Exception $e) {
        //write_log('errors', $e->getMessage());
        continue;
      }
      $own = $_SESSION['bm_sso']['bmUserId'] == $uid;
      $folders[$uid] = array();
      foreach($all as $folder) {
        $f = $folder->value;
        if ($f == null) continue;
        if ($own) {
          $f->path = \rcube_charset::utf8_to_utf7imap($f->fullName);
        } elseif ($f->fullName == "INBOX" || $f->fullName == $mailbox->name) {
          $f->path = $mailbox->path;
        } else {
          $f->path = $mailbox->path . "/" . \rcube_charset::utf8_to_utf7imap($f->fullName);
        }
        $f->uid = $folder->uid;
        $folders[$uid][$f->uid] = $f;     
      }
    }
    return $folders;
  }

  private function _loadFolder($uid) {
    // subtle..
    unset($_SESSION['bm']['hierarchy']);
    $this->_initialize();
  }

  private function _loadMailbox($uid) {
    // subtle..
    unset($_SESSION['bm']['hierarchy']);
    $this->_initialize();
  }


  protected function getMailboxByUid($uid) {
    if  (!$this->mailboxes[$uid]) {
      $this->_loadMailbox($uid);
    }
    return $this->mailboxes[$uid];
  }


  protected function getMailboxByPath($path) {
    $path = trim($path);
    $rcmail = \rcmail::get_instance();
    $users = $rcmail->config->get('users_mbox');
    $mailshares = $rcmail->config->get('mailshares_mbox');
    if (strpos($path, $users) === 0 || strpos($path, $mailshares) === 0) {
      foreach($this->mailboxes as $uid => $mailbox) {
        if (strpos($path, $mailbox->path) === 0) {
          return $mailbox;
        }
      }
    }
    return $this->mailboxes[$_SESSION['bm_sso']['bmUserId']];
  } 


  protected function getMailboxByName($name) {
    $name = trim(strtolower($name));
    foreach($this->mailboxes as $uid => $mailbox) {
      if (strtolower($mailbox->name) == $name) {
        return $mailbox;
      }
    }
  } 


  private function _getFolderByUid($uid) {
    if ($this->folders[$_SESSION['bm_sso']['bmUserId']][$uid]) {
      return $this->folders[$_SESSION['bm_sso']['bmUserId']][$uid];
    }
    foreach($this->mailboxes as $mbox => $data) {
      if ($this->folders[$mbox][$uid]) {
        return $this->folders[$mbox][$uid];
      }
    }
  }
  protected function getFolderByUid($uid) {
    $folder = $this->_getFolderByUid($uid);
    if (!$folder) {
      $this->_loadFolder($uid);
      $folder = $this->_getFolderByUid($uid);
    }
    return $folder;
  }

  protected function getFolderByPath($path) {
    $path = trim($path);
    $mailbox = $this->getMailboxByPath($path);
    foreach($this->folders[$mailbox->uid] as $uid => $folder) {
      if ($folder->path === $path) {
        return $folder;
      }
    }
  }

  protected function getMailboxes() {
    return array_values($this->mailboxes);
  }
 
  function startsWith($haystack, $needle) {
     $length = strlen($needle);
     return (substr($haystack, 0, $length) === $needle);
  }


}
