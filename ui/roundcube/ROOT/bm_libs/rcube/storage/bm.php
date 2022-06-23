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

require_once('rcube/locator.php');
require_once('MailboxHierarchyClient.php');

/**
 */
class BMFolder { 
    public $mboxName;
    public $folder; 
}

class rcube_storage_bm extends rcube_imap {

  private $bm_headers;

  private $shared;

  private $other;

  private $fcache;

  private $client;

  private $searchMeta;

  private $hierarchy;

  private $iFolders;

  
  private $base_headers = array(
      'DATE',
      'FROM',
      'TO',
      'SUBJECT',
      'CONTENT-TYPE',
      'CC',
      'REPLY-TO',
      'LIST-POST',
      'DISPOSITION-NOTIFICATION-TO',
      'X-PRIORITY'
  );

  private $indexMap = array (
    "date" => "date",
    "size" => "size",
    "headers.from" => "from",
    "headers.to" => "to",
    "headers.cc" => "cc",
    "subject" => "subject",
    "content-type" => "content-type",
    "headers.reply-to" => "reply-to",
    "headers.disposition-notification-to" => "disposition-notification-to",
    "headers.list-post" => "list-post",
    "headers.x-priority" => "x-priority",
    "headers.x-bm-event" => "x-bm-event",
    "headers.x-bm-event-countered" => "x-bm-event-countered",
    "headers.x-bm-rsvp" => "x-bm-rsvp",
    "headers.x-bm-resourcebooking" => "x-bm-resourcebooking",
    "headers.x-bm-folderuid" => "x-bm-folderuid",
    "headers.x-bm-foldertype" => "x-bm-foldertype"
  );

  private $sortMap = array(
    "date" => "date",
    "arrival" => "internalDate",
    "size" => "size",
    "from" => "headers.from",
    "to" => "headers.to",
    "cc" => "header.cc",
    "subject" => "subject_kw"
  );

  public function __construct() {
    parent::__construct();
    $this->hierarchy = new rcube\MyHierarchy();
    $rcmail = rcmail::get_instance();
    $this->bm_headers = (array)$rcmail->config->get('show_additional_headers', array());
    foreach($this->bm_headers as $h) {
      $this->all_headers[] = strtoupper($h);
    }
    $config = rcmail::get_instance()->config;
    $this->shared = $config->get('mailshares_mbox');
    $this->other = $config->get('users_mbox');
  }

  private function _index($query = array()) {
    if (!$this->client) {
      $hosts = $_SESSION['bm']['hosts']['es'];
      if (!$hosts) {
        $ini_array = parse_ini_file("/etc/bm/bm.ini");
        $locator = new LocatorService($ini_array['locator'] ? $ini_array['locator'] : $ini_array['host']);
        $hosts  = $locator->getAll('bm/es', $_SESSION['bm_sso']['bmLogin']);
        $_SESSION['bm']['hosts']['es'] = $hosts;
      }
      $connections = array();
      foreach($hosts as $host) {
        $connections[] = array(
          'host' => $host,
          'port' => 9200
        );
        $this->client = new Elastica\Client(array('connections' => $connections));
      }
    }
    $index = new Elastica\Search($this->client);

    $index->addType('recordOrBody');

    foreach($query as $criterion) {
      $term = $criterion['term'];
      $val = $criterion['value'];
      switch($term) {
        case 'in':
          if ($val == '$current') {
            $mid = $this->getMboxUidByPath($this->get_folder());
            $index->addIndex('mailspool_alias_' . $mid);
          } elseif ($val == '$all') {
            $mailboxes = $this->getMboxesUid();
            foreach($mailboxes as $mid) {
              $index->addIndex('mailspool_alias_' . $mid);
            }                
          } else {
            $mid = $this->getMboxUidByPath($val);            
            $index->addIndex('mailspool_alias_' . $mid);
          }
          break;
	case 'mailbox':
          if ($val == '$current') {
            $index->addIndex('mailspool_alias_' . $_SESSION['bm_sso']['bmUserId']);            
          } elseif ($val == '$all') {
            $mailboxes = $this->getMboxesUid();
            foreach($mailboxes as $mid) {
              $index->addIndex('mailspool_alias_' . $mid);
            }            
          } else {
            $mailboxes = $this->getMboxesUid();
            if (in_array($val, $mailboxes)) {
              $index->addIndex('mailspool_alias_' . $val);              
            }
          }
          break;
      }
    }
    if (!$index->hasIndices()) {
      $mid = $this->getMboxUidByPath($this->get_folder());
      $index->addIndex('mailspool_alias_' . $mid);
    }
    return $index;
  }

    /**
     * Set a new name to an existing folder
     *
     * @param string $folder   Folder to rename
     * @param string $new_name New folder name
     *
     * @return boolean True on success
     */
    public function rename_folder($folder, $new_name)
    {
        if (!strlen($new_name)) {
            return false;
        }

        if (!$this->check_connection()) {
            return false;
        }

        $delm = $this->get_hierarchy_delimiter();

        // get list of subscribed folders
        if ((strpos($folder, '%') === false) && (strpos($folder, '*') === false)) {
            $a_subscribed = $this->list_folders_subscribed('', $folder . $delm . '*');
            $subscribed   = $this->folder_exists($folder, true);
        }
        else {
            $a_subscribed = $this->list_folders_subscribed();
            $subscribed   = in_array($folder, $a_subscribed);
        }

        $this->conn->select('INBOX');
        $result = $this->conn->renameFolder($folder, $new_name);

        if ($result) {
            // unsubscribe the old folder, subscribe the new one
            if ($subscribed) {
                $this->conn->unsubscribe($folder);
                $this->conn->subscribe($new_name);
            }

            // check if folder children are subscribed
            foreach ($a_subscribed as $c_subscribed) {
                if (strpos($c_subscribed, $folder.$delm) === 0) {
                    $this->conn->unsubscribe($c_subscribed);
                    $this->conn->subscribe(preg_replace('/^'.preg_quote($folder, '/').'/',
                        $new_name, $c_subscribed));

                    // clear cache
                    $this->clear_message_cache($c_subscribed);
                }
            }

            // clear cache
            $this->clear_message_cache($folder);
            $this->clear_cache('mailboxes', true);
        }

        return $result;
    }


  private function isIndexId($uid) {
    if (($decode = @base64_decode($uid, true))) {
      $uid = $decode;
    }
    return (preg_match("/^([0-9a-z-]+)[#:]([0-9]+)$/", $uid) == 1);
  }

  private function isAll($uid) {
    return ($uid === '*' || $uid === '1:*');
  }

  private function parseIndexId($uid) {
    if (($decode = @base64_decode($uid, true))) {
      $uid = $decode;
    }
    preg_match("/^([0-9a-z-]+)[#:]([0-9]+)$/", $uid, $match);
    return array('folder' => $this->getFolderByUid($match[1]), 'uid' => $match[2]);
  }

  private function parseUidList($uids, $folder = null) {
    if ($this->isAll($uids)) { 
      if(!empty($this->search_set)) {
        $uids =$this->search_set->get();
      } else {
        return array($folder => $uids);
      }
    }
    if (!is_array($uids)) {
      $uids = explode(',', $uids);
    }
    foreach($uids as $uid) {
      if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $f = $data['folder'];
        $uid = $data['uid'];
      } else {
        $f = $folder;
      }
      $folders[$f][] = trim($uid);
    }
    return $folders;
  }

  /** @override */
 public function set_search_set($set) {
   parent::set_search_set($set);
   if ($this->search_string) {
     $this->set_threading();
   }
 }

  /** @override */
  public function get_error_code() {
      $err = $this->conn->errornum;
      if ($err < 0) {
        $msg = $this->conn->error;
        if (stripos($msg, 'Over Quota') !== false) {
           $err = 1;
           $this->conn->errornum = 1;
           $this->conn->error = 'Over Quota';
        }
      }
      return $err;
  }

  /** @override */
  public function get_error_str()  {
      return $this->conn->error;
  }

  /** @override */
  public function fetch_headers($folder, $msgs, $sort = true, $force = false) {
    $folders = $this->parseUidList($msgs, $folder);
    $headers = array();
    $mailboxes = array();
    foreach($folders as $f => $uids) {
      $f = (string) $f;
      $results = parent::fetch_headers($f, $uids, false, $force);
      $mailbox = $this->getMboxUidByPath($f);
      foreach($results as $uid => $msg) {
        //FIXME: UID is the IMAP uid. This should be the index uid (entityid:folder:uid).
        //In a multi-folder request this WILL break.
        //Should be : $uid = $this->writeIndexId($folder, $uid);
        $headers[$uid] = $msg;
      }
    }

    if ($sort) {
      // use this class for message sorting
      $sorter = new rcube_header_sorter();
      $sorter->set_index($msgs);
      $sorter->sort_headers($headers);
    }

    return $headers;
  }

  /** @override */
  public function get_message_headers($uid, $folder = null, $force = false) {
    if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $folder = $data['folder'];
        $uid = $data['uid'];
    }
    return parent::get_message_headers($uid, $folder, $force);
  }

  /** @override */
  public function get_message($uid, $folder = null) {
    if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $folder = $data['folder'];
        $uid = $data['uid'];
    }
    if (!strlen($folder)) {
      $folder = $this->folder;
    }
    
    // Check internal cache
    if (!empty($this->icache['message'])) {
      if (($headers = $this->icache['message']) && ($headers->uid == $uid)) {
        return $headers;
      }
    }
    $headers = $this->get_message_headers($uid, $folder);
    // message doesn't exist?
    if (empty($headers)) {
      return null;
    }
    // structure might be cached
    if (!empty($headers->structure)) {
      return $headers;
    }
    $this->msg_uid = $uid;

    if (!$this->check_connection()) {
      return $headers;
    }


    $headers->ctype = strtolower($headers->ctype);
    if (empty($headers->bodystructure)) {
      $headers->bodystructure = $this->conn->getStructure($folder, $uid, true);
    }

    $structure = $headers->bodystructure;

    if (empty($structure)) {
      return $headers;
    }
    // set message charset from message headers
    if ($headers->charset) {
      $this->struct_charset = $headers->charset;
    }
    else {
      $this->struct_charset = $this->structure_charset($structure);
    }
    // Here we can recognize malformed BODYSTRUCTURE and
    // 1. [@TODO] parse the message in other way to create our own message structure
    // 2. or just show the raw message body.
    // Example of structure for malformed MIME message:
    // ("text" "plain" NIL NIL NIL "7bit" 2154 70 NIL NIL NIL)
    if ($headers->ctype && !is_array($structure[0]) && $headers->ctype != 'text/plain'
        && strtolower($structure[0].'/'.$structure[1]) == 'text/plain') {
      // we can handle single-part messages, by simple fix in structure (#1486898)
      if (preg_match('/^(text|application)\/(.*)/', $headers->ctype, $m)) {
        $structure[0] = $m[1];
        $structure[1] = $m[2];
      }
      else {
        // Try to parse the message using Mail_mimeDecode package
        // We need a better solution, Mail_mimeDecode parses message
        // in memory, which wouldn't work for very big messages,
        // (it uses up to 10x more memory than the message size)
        // it's also buggy and not actively developed
        if ($headers->size ) {
            $raw_msg = $this->get_raw_body($uid);
            $struct = rcube_mime::parse_message($raw_msg);
        } else {
            return $headers;
        }
      }
    }
    if (empty($struct)) {
      $struct = $this->structure_part($structure, 0, '', $headers);
    }
    // don't trust given content-type
    if (empty($struct->parts) && !empty($headers->ctype)) {
      $struct->mime_id = '1';
      $struct->mimetype = strtolower($headers->ctype);
      list($struct->ctype_primary, $struct->ctype_secondary) = explode('/', $struct->mimetype);
    }

    $headers->structure = $struct;

    return $this->icache['message'] = $headers;
  }


  /** @override */
  public function get_message_part($uid, $part=1, $o_part=NULL, $print=NULL, $fp=NULL, $skip_charset_conv=false) {
    if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $folder = $data['folder'];
        $uid = $data['uid'];
        if ($folder != $this->folder) {
          $this->set_folder($folder);
        }
    }
    return parent::get_message_part($uid, $part, $o_part, $print, $fp, $skip_charset_conv);
  }


  /** @override */
  public function get_raw_body($uid, $fp=null) {
    if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $folder = $data['folder'];
        $uid = $data['uid'];
        if ($folder != $this->folder) {
          $this->set_folder($folder);
        }
    }
    return parent::get_raw_body($uid, $fp);
  }
  
  /** @override */
  public function get_raw_headers($uid) {
    if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $folder = $data['folder'];
        $uid = $data['uid'];
        if ($folder != $this->folder) {
          $this->set_folder($folder);
        }
    }
    return parent::get_raw_headers($uid);
  }

  /** @override */
  public function print_raw_body($uid, $formatted = true) {
    if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $folder = $data['folder'];
        $uid = $data['uid'];
        if ($folder != $this->folder) {
          $this->set_folder($folder);
        }
    }
    parent::print_raw_body($uid, $formatted);
  }

  /** @override */
  public function set_flag($uids, $flag, $folder=null, $skip_cache=false) {
    $folders = $this->parseUidList($uids, $folder); 
    foreach($folders as $f => $uids) {
      $f = (string) $f;
      if (!parent::set_flag($uids, $flag, $f, $skip_cache)) {
        return false;
      }
    }
    return true;
  }

  /** @override */
  public function move_message($uids, $to_mbox, $from_mbox='') {
    $folders = $this->parseUidList($uids, $from_mbox); 
    foreach($folders as $f => $uids) {
      $f = (string) $f;
      if ($f !== $to_mbox) {
        if (!parent::move_message($uids, $to_mbox, $f)) {
          return false;
        }
      }
    }
    return true;
  }

  /** @override */
  public function copy_message($uids, $to_mbox, $from_mbox='') {
    $folders = $this->parseUidList($uids, $from_mbox); 
    foreach($folders as $f => $uids) {
      $f = (string) $f;
      if (!parent::copy_message($uids, $to_mbox, $f)) {
        return false;
      }
    }
    return true;
  }

  /** @override */
  public function delete_message($uids, $folder='') {
    $folders = $this->parseUidList($uids, $folder); 
    foreach($folders as $f => $uids) {
      $f = (string) $f;
      if (!parent::delete_message($uids, $f)) {
        return false;
      }
    }
    return true;
  }

  /** @override */
  public function expunge_message($uids, $folder = null, $clear_cache = true) {
    $folders = $this->parseUidList($uids, $folder); 
    foreach($folders as $f => $uids) {
      $f = (string) $f;
      if (!parent::expunge_message($uids, $f, $clear_cache)) {
        return false;
      }
    }
    return true;
  }
  /** @override */
  protected function clear_message_cache($folder = null, $uids = null) {
    $folders = $this->parseUidList($uids, $folder); 
    foreach($folders as $f => $uids) {
      $f = (string) $f;
      if (!parent::clear_message_cache($f, $uids)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Get message header names for rcube_imap_generic::fetchHeader(s)
   *
   * @return string Space-separated list of header names
   */
  protected function get_fetch_headers() {
      if (!empty($this->options['fetch_headers'])) {
          $headers = explode(' ', $this->options['fetch_headers']);
          $headers = array_map('strtoupper', $headers);
      }
      else {
          $headers = $this->bm_headers;
      }

      if ($this->messages_caching || $this->options['all_headers']) {
          $headers = array_merge($headers, $this->all_headers);
      }

      return implode(' ', array_unique($headers));
  }

  /** @override */
  public function save_message($folder, &$message, $headers='', $is_file=false) {
      if ($is_file) {
        $msg = $headers . "\r\n" . file_get_contents($message);
        $message = $msg;
      }
      return parent::save_message($folder, $message);
  }
 
  /** @override no prefix in BM */
  public function mod_folder($folder, $mode = 'out')
  {
    return $folder;
  }

  /** @override */
  public function list_folders_subscribed($root='', $name='*', $filter=null, $rights=null, $skip_sort=false) {
    $folders = parent::list_folders_subscribed($root, $name, $filter, $rights, $skip_sort);
    return $folders;
  }

    /**
     * Remove folder from server
     *
     * @param string $folder Folder name
     *
     * @return boolean True on success
     */
    function delete_folder($folder)
    {
        $delm = $this->get_hierarchy_delimiter();

        if (!$this->check_connection()) {
            return false;
        }

        // get list of folders
        if ((strpos($folder, '%') === false) && (strpos($folder, '*') === false)) {
            $sub_mboxes = $this->list_folders('', $folder . $delm . '*');
        }
        else {
            $sub_mboxes = $this->list_folders();
        }

        $sub_mboxes = array_reverse($sub_mboxes, true);

        foreach ($sub_mboxes as $c_mbox) {
          if (strpos($c_mbox, $folder.$delm) === 0) {
              $this->conn->select('INBOX');
              $this->conn->unsubscribe($c_mbox);
              $result = $this->conn->deleteFolder($c_mbox);
              if ($result) {
                  $this->clear_message_cache($c_mbox);
              } else {
                  return $result;
              }
            }
        }

        $this->conn->select('INBOX');
        // unsubscribe folder
        $this->conn->unsubscribe($folder);
        // send delete command to server
        $result = $this->conn->deleteFolder($folder);
        if ($result) {
            // clear folder-related cache
            $this->clear_message_cache($folder);
            $this->clear_cache('mailboxes', true);
        }

        return $result;
    }

 
  /** @override */
  public function list_folders($root='', $name='*', $filter=null, $rights=null, $skip_sort=false) {
    $folders = parent::list_folders($root, $name, $filter, $rights, $skip_sort);
    return $folders;
  }

  public function getImapUID($uid) {
    if ($this->isIndexId($uid)) {
        $data = $this->parseIndexId($uid);
        $uid = $data['uid'];
    }
    return $uid;
  }

  /** CACHING */
  /** @override */
  protected function get_cache_engine($fcache=false) {
    if ($fcache) { 
      return $this->get_fcache_engine();
    } else {
      return parent::get_cache_engine();
    }
  }


  protected function get_fcache_engine() {
    if ($this->caching && !$this->fcache) {
      $rcmail = rcmail::get_instance();
      $ttl = $rcmail->config->get('folder_cache_lifetime', '30m');
      $ttl = get_offset_time($ttl) - time();
      $this->fcache = $rcmail->get_cache('FOLDER', $this->caching, $ttl);
    }

    return $this->fcache;
  }

  /** @override */
  public function get_cache($key) {
    if ($cache = $this->get_cache_engine(strpos($key, 'mailboxes') === 0)) {
      return $cache->get($key);
    }
  }

  /** @override */
  public function update_cache($key, $data) {
    if ($cache = $this->get_cache_engine(strpos($key, 'mailboxes') === 0)) {
      $cache->set($key, $data);
    }
  }

  /** @override */
  public function clear_cache($key = null, $prefix_mode = false) {
    if ($cache = $this->get_cache_engine(strpos($key, 'mailboxes') === 0)) {
      $cache->remove($key, $prefix_mode);
    }
  }

  /** @override */
  public function expunge_cache() {
    parent::expunge_cache();
    if ($this->fcache)
      $this->fcache->expunge();
 
  }

  /** @override */
  protected function search_index($folder, $criteria='ALL', $charset=NULL, $sort_field=NULL) {
    //TODO: disable threading on search
    if ($criteria == 'ALL' || !$this->check_connection() || !$this->isIndexEnabled()) {
      return parent::search_index($folder, $criteria, $charset, $sort_field);
    }
    $criteria = $this->parseCriteria($criteria);
    $index = $this->_index($criteria);
    $criteria = $this->buildQuery($criteria, $this->options['skip_deleted']);

    $query = new Elastica\Query();
    $query->setQuery($criteria);

    $fields = array_keys($this->indexMap);
    $fields[] = "is";
    $fields[] = "uid";
    $query->setSource($fields);

    $page = $this->list_page ? $this->list_page : 1;
    $from  = ($page-1) * $this->page_size;
    $to    = $from + $this->page_size;
    $query->setFrom($from);
    $query->setSize($this->page_size);

    $sort = $sort_field ? $sort_field : rcmail_sort_column();
    $order = $this->sort_order ? $this->sort_order : rcmail_sort_order();
    if ($this->sortMap[$sort]) {
      $query->setSort(array($this->sortMap[$sort] => array('order' => strtolower($order))));
    }
    $this->search_sorted = true;

    try {
        $resultSet = $index->search($query);
    } catch (Exception $e) {
        error_log('Cannot execute search: '.$e->getMessage());
        return new rcube_result_esindex();
    }
    $count = min($resultSet->getTotalHits(), 9999);
    $results = $resultSet->getResults();
    $messages = array();
    foreach ($results as $result) {
      $messages[] = $this->headersFromIndex($result->getData(), $result->getId());
    }

    $this->searchMeta = array ('from' => $from, 'to' => $to, 'sort' => $sort, 'order' => $order, 'messages' => $messages);
    $query->setStoredFields(array());
    $query->setSource(array('uid'));
    $query->setFrom(0);
    $query->setSize($count);
    
    // Use internal methods not to duplicate result in memory
    $index->setOptionsAndQuery(null, $query);

    $response = $index->getClient()->request(
       $index->getPath(),
       Elastica\Request::GET,
       $index->getQuery()->toArray(),
       $index->getOptions() 
    );
    $result = $response->getData();

    if (isset($result['hits']['hits'])) {
      foreach ($result['hits']['hits'] as $hit) {
        list($folder) = explode(':', $hit['_id']);
        $uid = $hit['_source']['uid'];
        $r[] = base64_encode("$folder#$uid");
      }
    }
    $meta = array('ORDER' => $order, 'COUNT' => $count);
    return new rcube_result_esindex($r, $meta);
  }

   /** @override */
  protected function list_search_messages($folder, $page, $slice=0) {

    //TODO: disable threading on search
    if (!strlen($folder) || empty($this->search_set) || $this->search_set->is_empty() || !$this->isIndexEnabled()) {
      return parent::list_search_messages($folder, $page, $slice);
    }

    if ($this->search_set->is_empty()) {
        return array();
    }

    $from  = ($page-1) * $this->page_size;
    $to    = $from + $this->page_size;

    if ($this->searchMeta &&
        $this->searchMeta['from'] == $from &&
        $this->searchMeta['to'] == $to &&
        $this->searchMeta['sort'] == $this->sort_field &&
        $this->searchMeta['order'] == $this->sort_order ) {
      return $this->searchMeta['messages'];
    }


    $criteria = $this->parseCriteria($this->search_string);
    $index = $this->_index($criteria);
    $criteria = $this->buildQuery($criteria, $this->options['skip_deleted']);
    $query = new Elastica\Query();
    $query->setQuery($criteria);

    $fields = array_keys($this->indexMap);
    $fields[] = "is";
    $fields[] = "uid";
    $query->setSource($fields);

    $query->setFrom($from);
    $query->setSize($this->page_size);


    $sort = $this->sort_field ? $this->sort_field : rcmail_sort_column();
    $order = $this->sort_order ? $this->sort_order : rcmail_sort_order();
    if ($this->sortMap[$sort]) {
      $query->setSort(array($this->sortMap[$this->sort_field] => array('order' => strtolower($order))));
    }
    $resultSet = $index->search($query);
    $results = $resultSet->getResults();
    $messages = array();
    foreach ($results as $result) {
      $messages[] = $this->headersFromIndex($result->getData(), $result->getId());
    }

    $this->searchMeta = array ('from' => $from, 'to' => $to, 'sort' => $sort, 'order' => $order, 'messages' => $messages);
    return $messages;
   }

  public function isIndexEnabled() {
    if (!isset($_SESSION['bm']['search']['enabled']) || ($_SESSION['bm']['search']['enabled']['expire'] <= time())) {
      $imapQuota = $this->get_quota();
      $imapQuota = $imapQuota['used'];
      try {
        $esQuota = $this->indexQuota();
        if ($esQuota >= $imapQuota * 0.8) {
          $_SESSION['bm']['search']['enabled'] = array('expire' => time() + 300, 'value' => true);
        } else {
          error_log("[".$_SESSION['bm_sso']['bmLogin']."] esQuota < (imapQuota * 0.8). disable es search. esQuota: $esQuota, imapQuota: $imapQuota");
          $_SESSION['bm']['search']['enabled'] = array('expire' => time() + 600, 'value' => false);
        }
      } catch(Exception $e) {
        error_log('['.$_SESSION['bm_sso']['bmLogin'].'] error isIndexEnabled: '.$e->getMessage());
        $_SESSION['bm']['search']['enabled'] = array('expire' => time() + 600, 'value' => false);
      } 
    }
    return $_SESSION['bm']['search']['enabled']['value'];
  }

  private function indexQuota() {
    $criteria = $this->parseCriteria('mailbox:$current;');
    $index = $this->_index($criteria);
    $criteria = $this->buildQuery($criteria);    
    $query = new Elastica\Query();
    $query->setFrom(0);
    $query->setQuery($criteria);

    $facet = new Elastica\Aggregation\Stats("quota");
    $facet->setField("size");
    $query->addAggregation($facet->setField("size"));
    $results = $index->search($query, 0);
    $quota = $results->getAggregation('quota');
    return (int) ceil($quota['sum'] / 1024);
  }

  public function parseSearch($query, $filter, $headers = array()) {
    if ($this->isIndexEnabled()) {
      return $this->parseIndexSearch($query, $filter);
    } else {
      return $this->parseImapSearch($query, $filter, $headers);
    }
  }
 
  public function getEmailMboxUid($uid) {
    if ($this->isIndexId($uid)) {
      $data = $this->parseIndexId($uid);
      $folder = $data['folder'];
    } else {
      $folder = $this->get_folder();
    }
    return $this->getMboxUidByPath($folder);
  } 

  private function parseImapSearch($query, $filter, $headers) {
    $search_str = $filter && $filter != 'ALL' ? $filter : '';
    // Check the search string for type of search
    if (preg_match("/^from:.*/i", $query)) {
      list(,$srch) = explode(":", $query);
      $subject['from'] = "HEADER FROM";
    } else if (preg_match("/^to:.*/i", $query)) {
      list(,$srch) = explode(":", $query);
      $subject['to'] = "HEADER TO";
    } else if (preg_match("/^cc:.*/i", $query)) {
      list(,$srch) = explode(":", $query);
      $subject['cc'] = "HEADER CC";
    } else if (preg_match("/^bcc:.*/i", $query)) {
      list(,$srch) = explode(":", $query);
      $subject['bcc'] = "HEADER BCC";
    } else if (preg_match("/^subject:.*/i", $query)) {
      list(,$srch) = explode(":", $query);
      $subject['subject'] = "HEADER SUBJECT";
    } else if (preg_match("/^body:.*/i", $query)) {
      list(,$srch) = explode(":", $query);
      $subject['text'] = "TEXT";
    } else if (strlen(trim($query))) {
      if ($headers) {
        foreach (explode(',', $headers) as $header) {
          if ($header == 'text') {
            $subject = array('text' => 'TEXT');
            break;
          } else {
            $subject[$header] = 'HEADER '.strtoupper($header);
          }
        }
      } else {
        // search in subject by default
        $subject['subject'] = 'HEADER SUBJECT';
      }
    }
    
    $search = isset($srch) ? trim($srch) : trim($query);
    
    if (!empty($subject)) {
      $search_str .= str_repeat(' OR', count($subject)-1);
      foreach ($subject as $sub)
        $search_str .= sprintf(" %s {%d}\r\n%s", $sub, strlen($search), $search);
    }
    
    $search_str  = trim($search_str);
    return $search_str;
  }

  private function parseCriteria($query) {
    if (!$this->isParsedSearch($query)) {
      $query = $this->parseIndexSearch(null, $query);
    }
    $parts = explode(";", $query);
    $query = array();
    foreach($parts as $part) {
      $part = trim($part);
      if ($part == '') {
        continue; 
      }
      list($term, $val) = explode(":", $part, 2);
      if ($val === NULL) {
        $val = $term;
        $term = 'content';
      }
      $val = trim($val);
      $query[] = array('term' =>$term, 'value' => $val);
    }

    return $query;
  }

  private function parseIndexSearch($query, $filter) {
    $filter = $filter && $filter != 'ALL' ? 'filter:' . $filter: '';
    $query = trim($query);
    $query = ($query != 'in:$current') ? $query : '';
    return ($filter && $query) ? $filter .';' . $query : $filter . $query;
  }

  private function isParsedSearch($criteria) {
    return strpos($criteria, ':') !== false || strpos($criteria, ';') !== false;
  }

  private function getFolderByUid($uid) {
    $folder = $this->hierarchy->getFolderByUid($uid);
    if ($folder) {
      return $folder->path;
    } else {
      return rcube_charset::utf8_to_utf7imap($_SESSION['mbox']);
    }
  }

  private function getMboxUidByPath($path) {
    $mailbox = $this->hierarchy->getMailboxByPath($path);
    return $mailbox->uid;
  }

  private function getFolderUid($path) {
    $folder = $this->hierarchy->getFolderByPath($path);
    return $folder->uid;
  }

  private function getMboxesUid() {
    $mailboxes = $this->hierarchy->getMailboxes();
    $uids = array();
    foreach($mailboxes as $mbox) {
      $uids[] = $mbox->uid;
    }
    return $uids;
  }

  private function buildQuery($q, $skip_delete = false) {
    $scoped = false;
    $pattern = array();
    $parentPattern = array();

    $filter = new Elastica\Query\BoolQuery();
    if ($skip_delete) {
      $term = new Elastica\Query\Term(array('is' => 'deleted'));
      $filter->addMustNot($term);
    }

    foreach($q as $criterion) {
      $term = $criterion['term'];
      $val = $criterion['value'];      
      switch($term) {
        case 'filter':
          switch($val) {
            case '':
            case 'ALL':
              break;
            case 'UNANSWERED': 
              $term = new  Elastica\Query\Term(array('is' => array('value' => 'answered')));
              $filter->addMustNot($term);
              break;
            case 'HEADER X-PRIORITY 1': 
            case 'HEADER X-PRIORITY 2': 
            case 'HEADER X-PRIORITY 4': 
            case 'HEADER X-PRIORITY 5': 
              $term = new  Elastica\Query\Term();
              $val = str_replace('HEADER X-PRIORITY ','', $val);
              $term->setTerm('headers.x-priority', $val);
              $filter->addMust($term);
              break;
            case 'NOT HEADER X-PRIORITY 1 NOT HEADER X-PRIORITY 2 NOT HEADER X-PRIORITY 4 NOT HEADER X-PRIORITY 5' :
              $or = new Elastica\Query\BoolQuery();
              $term = new Elastica\Query\Exists('headers.x-priority');
              $or->addMustNot($term);
              $or->addShould(new Elastica\Query\Term(array('headers.x-priority' => array('value' => 3))));
              $filter->addMust($or);
              break;
            case 'UNREAD':
            case 'FLAGGED':
            default;
              $filter->addMust(new Elastica\Query\Term(array('is' => array('value' => strtolower($val)))));
              break;
          }
          break;
        case 'in':
          if ($val == '$current') { 
            $in = $this->getFolderUid($this->get_folder()); 
            $filter->addMust(new Elastica\Query\Term(array('in' => array('value' => $in))));
          }  elseif ($val != '$all'){
            $in = $this->getFolderUid($val);
            $filter->addMust(new Elastica\Filter\Term(array('in' => $in)));
          }
        case 'mailbox':
          $scoped = true;
          break;
        case 'raw': 
          $pattern[] = $val;
          break;
        case 'subject':
        case 'content':
        case 'from':
        case 'to':
        case 'cc':
        case 'filename':
          $parentPattern[] = $term.':'.$val;
          break;
        case '_all':
          $parentPattern[] = $val;
          break;
	default:
          $pattern[] = $term.':'.$val;
      }
    }
    if (!$scoped) {
      $filter->addMust(new Elastica\Query\Term(array('in' => array('value' => $this->getFolderUid($this->get_folder())))));
    }
    $queries = array();
    if (count($pattern) > 0) {
      $q  = new Elastica\Query\QueryString();
      $q->setDefaultOperator('AND'); 
      $q->setQuery(implode(' ', $pattern));
      $queries[] = $q;
    }
    if (count($parentPattern) > 0) {
      $q  = new Elastica\Query\QueryString();
      $q->setDefaultOperator('AND');
      $q->setFields(["subject", "content", "filename", "from", "to", "cc"]);
      $q->setQuery(implode(' ', $parentPattern));
      $queries[] = new Elastica\Query\HasParent($q, 'body');
    }

    switch(count($queries)) {
      case 0: 
        $q = new Elastica\Query\MatchAll();
        break;
      case 1:
        $q = array_pop($queries);
        break;
      default:
        $q = new Elastica\Query\Bool();
        foreach($queries as $query) {
          $q->addMust($query);
        }
    }
    if ($filter->hasParam('must') || $filter->hasParam('must_not')) {
      $ret = new Elastica\Query\BoolQuery();
      $ret->addMust($q);
      $ret->addMust($filter);
      return $ret;
    } else {
      return $q;
    }
  }


  private function headersFromIndex($index, $id) {
    $message = new rcube_mail_header();
    list($folder) = explode(':', $id);

	$uid = $index['uid'];
    $message->id = "$folder:$uid";
    $message->uid = base64_encode("$folder#$uid");
    $message->flags['skip_mbox_check'] = true;
    $message->list_flags['extra_flags']['uid'] = $uid;
    $message->list_flags['extra_flags']['mbox'] = $this->getFolderByUid($folder);

    foreach ($this->indexMap as $field => $header) {
      if (substr_count($field, ".") === 1) {
        list($doc, $doc_field) = explode(".", $field);
        if (isset($index[$doc])) {
            $index[$field] = $index[$doc][$doc_field];
        }
      }
      if (is_array($index[$field])) {
        $index[$field] = $index[$field][0];
      }
      if (isset($index[$field]) && $index[$field] != null && count($index[$field]) > 0) {
        $this->setMessageHeaderFromIndex($message, $header, $index[$field]);        
      }
    }
    $message->messageID = $message->uid;

    if (is_array($index['is'])) {
      foreach ($index['is'] as $flag) {
        $message->flags[$flag] = true;
      }
    }

    return $message;
  }

  private function setMessageHeaderFromIndex($message, $field, $string) {

    $field  = strtolower($field);
    $string = preg_replace('/\n[\t\s]*/', ' ', trim($string));

    switch ($field) {
    case 'date';
      $message->date = $string;
      $message->timestamp = $this->conn->strToTime($string);
      break;
    case 'from':
      $message->from = $string;
      break;
    case 'to':
      $message->to = preg_replace('/undisclosed-recipients:[;,]*/', '', $string);
      break;
    case 'subject':
      $message->subject = $string;
      break;
    case 'reply-to':
      $message->replyto = $string;
      break;
    case 'cc':
      $message->cc = $string;
      break;
    case 'bcc':
      $message->bcc = $string;
      break;
    case 'content-transfer-encoding':
      $message->encoding = $string;
    break;
    case 'content-type':
      $ctype_parts = preg_split('/[; ]/', $string);
      $message->ctype = strtolower(array_shift($ctype_parts));
      if (preg_match('/charset\s*=\s*"?([a-z0-9\-\.\_]+)"?/i', $string, $regs)) {
          $message->charset = $regs[1];
      }
      break;
    case 'in-reply-to':
      $message->in_reply_to = str_replace(array("\n", '<', '>'), '', $string);
      break;
    case 'references':
      $message->references = $string;
      break;
    case 'return-receipt-to':
    case 'disposition-notification-to':
    case 'x-confirm-reading-to':
      $message->mdn_to = $string;
      break;
    case 'message-id':
      $message->messageID = $string;
      break;
    case 'x-priority':
      if (preg_match('/^(\d+)/', $string, $matches)) {
          $message->priority = intval($matches[1]);
      }
      break;
    default:
      if (property_exists($message, $field)) {
        $message->$field = $string;
      } else {
        $message->set($field, $string);
      }
      break;
    }
    

  }

      /**
     * Build message part object
     *
     * @param array  $part
     * @param int    $count
     * @param string $parent
     */
    protected function structure_part($part, $count=0, $parent='', $mime_headers=null)
    {
        $struct = new rcube_message_part;
        $struct->mime_id = empty($parent) ? (string)$count : "$parent.$count";

        // multipart
        if (is_array($part[0])) {
            $struct->ctype_primary = 'multipart';

        /* RFC3501: BODYSTRUCTURE fields of multipart part
            part1 array
            part2 array
            part3 array
            ....
            1. subtype
            2. parameters (optional)
            3. description (optional)
            4. language (optional)
            5. location (optional)
        */

            // find first non-array entry
            for ($i=1; $i<count($part); $i++) {
                if (!is_array($part[$i])) {
                    $struct->ctype_secondary = strtolower($part[$i]);
                    break;
                }
            }

            $struct->mimetype = 'multipart/'.$struct->ctype_secondary;

            // build parts list for headers pre-fetching
            for ($i=0; $i<count($part); $i++) {
                if (!is_array($part[$i])) {
                    break;
                }

                // fetch message headers if message/rfc822
                // or named part (could contain Content-Location header)
                if (!is_array($part[$i][0])) {
                    $tmp_part_id = $struct->mime_id ? $struct->mime_id.'.'.($i+1) : $i+1;
                    if (strtolower($part[$i][0]) == 'message' && strtolower($part[$i][1]) == 'rfc822') {
                        $mime_part_headers[] = $tmp_part_id;
                    }
                    else if (in_array('name', (array)$part[$i][2]) && empty($part[$i][3])) {
                        $mime_part_headers[] = $tmp_part_id;
                    }
                    else if (!is_array($part[$i][8]) && !is_array($part[$i][9]) && !is_array($part[$i][11])) {
                        $mime_part_headers[] = $tmp_part_id;                      
                    }
                }
            }

            // pre-fetch headers of all parts (in one command for better performance)
            // @TODO: we could do this before _structure_part() call, to fetch
            // headers for parts on all levels
            if ($mime_part_headers) {
                $mime_part_headers = $this->conn->fetchMIMEHeaders($this->folder,
                    $this->msg_uid, $mime_part_headers);
            }

            $struct->parts = array();
            for ($i=0, $count=0; $i<count($part); $i++) {
                if (!is_array($part[$i])) {
                    break;
                }
                $tmp_part_id = $struct->mime_id ? $struct->mime_id.'.'.($i+1) : $i+1;
                $struct->parts[] = $this->structure_part($part[$i], ++$count, $struct->mime_id,
                    $mime_part_headers[$tmp_part_id]);
            }

            return $struct;
        }

        /* RFC3501: BODYSTRUCTURE fields of non-multipart part
            0. type
            1. subtype
            2. parameters
            3. id
            4. description
            5. encoding
            6. size
          -- text
            7. lines
          -- message/rfc822
            7. envelope structure
            8. body structure
            9. lines
          --
            x. md5 (optional)
            x. disposition (optional)
            x. language (optional)
            x. location (optional)
        */

        // regular part
        $struct->ctype_primary = strtolower($part[0]);
        $struct->ctype_secondary = strtolower($part[1]);
        $struct->mimetype = $struct->ctype_primary.'/'.$struct->ctype_secondary;

        // read content type parameters
        if (is_array($part[2])) {
            $struct->ctype_parameters = array();
            for ($i=0; $i<count($part[2]); $i+=2) {
                $struct->ctype_parameters[strtolower($part[2][$i])] = $part[2][$i+1];
            }

            if (isset($struct->ctype_parameters['charset'])) {
                $struct->charset = $struct->ctype_parameters['charset'];
            }
        }

        // #1487700: workaround for lack of charset in malformed structure
        if (empty($struct->charset) && !empty($mime_headers) && $mime_headers->charset) {
            $struct->charset = $mime_headers->charset;
        }

        // read content encoding
        if (!empty($part[5])) {
            $struct->encoding = strtolower($part[5]);
            $struct->headers['content-transfer-encoding'] = $struct->encoding;
        }

        // get part size
        if (!empty($part[6])) {
            $struct->size = intval($part[6]);
        }

        // read part disposition
        $di = 8;
        if ($struct->ctype_primary == 'text') {
            $di += 1;
        }
        else if ($struct->mimetype == 'message/rfc822') {
            $di += 3;
        }

        if (is_array($part[$di]) && count($part[$di]) == 2) {
            $struct->disposition = strtolower($part[$di][0]);

            if (is_array($part[$di][1])) {
                for ($n=0; $n<count($part[$di][1]); $n+=2) {
                    $struct->d_parameters[strtolower($part[$di][1][$n])] = $part[$di][1][$n+1];
                }
            }
        } else if (!empty($mime_headers)) {
          $args = array('mime' => $mime_headers, 'disposition' => $struct->disposition);
          $args = rcmail::get_instance()->plugins->exec_hook('part_disposition', $args); 
          $struct->disposition = $args['disposition'];
        }

        // get message/rfc822's child-parts
        if (is_array($part[8]) && $di != 8) {
            $struct->parts = array();
            for ($i=0, $count=0; $i<count($part[8]); $i++) {
                if (!is_array($part[8][$i])) {
                    break;
                }
                $struct->parts[] = $this->structure_part($part[8][$i], ++$count, $struct->mime_id);
            }
        }

        // get part ID
        if (!empty($part[3])) {
            $struct->content_id = $part[3];
            $struct->headers['content-id'] = $part[3];

            if (empty($struct->disposition)) {
                $struct->disposition = 'inline';
            }
        }

        // fetch message headers if message/rfc822 or named part (could contain Content-Location header)
        if ($struct->ctype_primary == 'message' || ($struct->ctype_parameters['name'] && !$struct->content_id) || ($struct->disposition == 'filehosting')) {
            if (empty($mime_headers)) {
                $mime_headers = $this->conn->fetchPartHeader(
                    $this->folder, $this->msg_uid, true, $struct->mime_id);
            }

            if (is_string($mime_headers)) {
                $struct->headers = rcube_mime::parse_headers($mime_headers) + $struct->headers;
            }
            else if (is_object($mime_headers)) {
                $struct->headers = get_object_vars($mime_headers) + $struct->headers;
            }

            // get real content-type of message/rfc822
            if ($struct->mimetype == 'message/rfc822') {
                // single-part
                if (!is_array($part[8][0])) {
                    $struct->real_mimetype = strtolower($part[8][0] . '/' . $part[8][1]);
                }
                // multi-part
                else {
                    for ($n=0; $n<count($part[8]); $n++) {
                        if (!is_array($part[8][$n])) {
                            break;
                        }
                    }
                    $struct->real_mimetype = 'multipart/' . strtolower($part[8][$n]);
                }
            }

            if ($struct->ctype_primary == 'message' && empty($struct->parts)) {
                if (is_array($part[8]) && $di != 8) {
                    $struct->parts[] = $this->structure_part($part[8], ++$count, $struct->mime_id);
                }
            }
        }

        // normalize filename property
        $this->set_part_filename($struct, $mime_headers);

        return $struct;
    }

    /**
     * Sort folders first by default folders and then in alphabethical order
     *
     * @param array $a_folders Folders list
     */
    protected function sort_folder_list($a_folders)  {
        $specials  = $this->default_folders;
        $folders   = array();
        // convert names to UTF-8
        foreach ($a_folders as $folder) {
            if (strpos($folder, '&') === false) {
              $folders[$folder] = $folder;
            } else {
              $folders[$folder] = rcube_charset_convert($folder, 'UTF7-IMAP');
            }
            // Replace delimiter with the first ascii char to use native sort on full path.
            // But control chars are ignored by UCA... the first spacing char is \t and is not
            // likely to be used in a folder name...
            $folders[$folder] =  str_replace($this->delimiter, "\t", $folders[$folder]);
        }
        // Use a collator (need php lib to be added to bm php build) instead of custom function
        $collator = new Collator($this->options['language'] ?: 'en_US');
        $collator->asort($folders, Collator::SORT_STRING);

        // sort folders
        $folders = array_keys($folders);

        // force the type of folder name variable (#1485527)
        $folders  = array_map('strval', $folders);
        $out      = array();

        // finally we must put special folders on top and rebuild the list
        // to move their subfolders where they belong...
        // but only for special folders
        $specials = array_unique(array_intersect($specials, $folders));
        $folders  = array_merge($specials, array_diff($folders, $specials));

        $this->sort_folder_specials(null, $folders, $specials, $out);

        return $out;
    }

    /**
     * Recursive function to put subfolders of special folders in place
     */
    protected function sort_folder_specials($folder, &$list, &$specials, &$out)
    {
        foreach ($list as $key => $name) {
            if ($folder === null || strpos($name, $folder.$this->delimiter) === 0) {
                $out[] = $name;
                unset($list[$key]);

                if (!empty($specials) && ($found = array_search($name, $specials)) !== false) {
                    unset($specials[$found]);
                    $this->sort_folder_specials($name, $list, $specials, $out);
                }
            }
        }

        reset($list);
    }
}
