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

/**
 * Blue Mind Folder sharing
 */
class bm_foldersharing extends rcube_plugin {

  public $task = 'mail';
  private $rcmail;
  private $containerClient;
  private $folder_id;
  private $folder_type;
  private $other;
  private $shared;
  private $userSubscriptionClient;

  function init() {
    $this->other = $_SESSION['imap_namespace']['other'][0][0];
    $this->shared = $_SESSION['imap_namespace']['shared'][0][0];

    $this->register_action('plugin.bm_foldersharing.subscribe', array($this, 'subscribe'));
    $this->register_action('plugin.bm_foldersharing.unsubscribe', array($this, 'unsubscribe'));

    $this->rcmail = rcmail::get_instance();
    if ($this->rcmail->action == 'show' || $this->rcmail->action == 'preview') {
      $this->add_texts('localization/', true);
      $this->include_stylesheet('skins/default/bm_foldersharing.css');
      $this->add_hook('message_load', array($this, 'message_load'));
      $this->add_hook('storage_init', array($this, 'storage_init'));
      $this->add_hook('template_object_messagebody', array($this, 'template_object_messagebody'));
      $this->include_script('bm_foldersharing.js');
    }
  }

  function storage_init($p) {
    if ($add_headers = (array)$this->rcmail->config->get('show_additional_headers', array()))
      $p['fetch_headers'] = trim($p['fetch_headers'].' ' . strtoupper(join(' ', $add_headers)));
    return $p;
  }

  function message_load($p) {
    $this->folder_id = $p['object']->get_header('x-bm-folderuid');
    $this->folder_type = $p['object']->get_header('x-bm-foldertype');
    return $p;
  }

  function template_object_messagebody($p){
    if (!empty($this->folder_id) && !(strpos($_SESSION['mbox'], $this->shared) === 0) && !(strpos($_SESSION['mbox'], $this->other) === 0)) {
      $user = $_SESSION['bm_sso']['bmUserId'];

      $f = $this->getFolder();
      if ($f == null) {
        return;
      }

      $yesCss = "";
      $noCss = "";

      $this->isSubscribed();

      if ($this->isSubscribed()) {
        $yesCss = "class='highlight'";
      } else {
        $noCss = "class='highlight'";
      }

      $yes = "<a id='fs-toolbar-yes' $yesCss href='javascript:subscribe(\"$this->folder_id\");'>".$this->gettext('yes')."</a>";
      $no = "<a id='fs-toolbar-no' $noCss href='javascript:unsubscribe(\"$this->folder_id\");' >".$this->gettext('no')."</a>";

      $p['content'] = "<div class='fs-toolbar'>
        <table class='headers-table'>
          <tr>
            <td class='header-title'>".$this->gettext($this->folder_type)."</td>
            <td class='header'>".$f->name."</td>
          </tr>
          <tr>
            <td class='header-title'>".$this->gettext('subscribe')."</td>
            <td class='header'>$yes - $no</td>
          </tr>
        </table>
      </div>
      $p[content]";

      return $p;
    }
  }

  private function getContainersClient() {
    if(!isset($this->containerClient)) {
      $rcmail = rcmail::get_instance();
      $this->containerClient = new BM\ContainersClient($_SESSION['bm']['core'],
        $rcmail->decrypt($_SESSION['password']),
        $_SESSION['bm_sso']['bmDomain']);
    }
    return $this->containerClient;
  }

  private function getContainerClient($uid) {
    $rcmail = rcmail::get_instance();
    $client = new BM\ContainerManagementClient($_SESSION['bm']['core'],
        $rcmail->decrypt($_SESSION['password']),
        $uid);
    return $client;

  }

  private function getFolder() {
    try {
      return $this->getContainerClient($this->folder_id)->getDescriptor();
    } catch (Exception $e) {
      return null;
    }
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

  private function isSubscribed() {
    $subscriptions = $this->getUserSubscriptionClient()->listSubscriptions($_SESSION['bm_sso']['bmUserId'], $this->folder_type);
    foreach($subscriptions as $sub) {
      if ($sub->uid == $this->folder_id) {
        return true;
      }
    }
    return false;
  }

  public function subscribe() {
    try {
      $this->add_texts('localization');
      $folderId = get_input_value('_folderId', RCUBE_INPUT_POST);
      $sub = new BM\ContainerSubscription();
      $sub->containerUid = $folderId;
      $this->getUserSubscriptionClient()->subscribe($_SESSION['bm_sso']['bmUserId'], array($sub));
      $this->rcmail->output->command('display_message', $this->gettext('confirmation'), 'confirmation');
      $this->rcmail->output->send();
    } catch (Exception $e) {
      write_log('errors', $e);
    }
  }

  public function unsubscribe() {
    try {
      $this->add_texts('localization');
      $folderId = get_input_value('_folderId', RCUBE_INPUT_POST);
      $this->getUserSubscriptionClient()->unsubscribe($_SESSION['bm_sso']['bmUserId'], array($folderId));
      $this->rcmail->output->command('display_message', $this->gettext('confirmation'), 'confirmation');
      $this->rcmail->output->send();
    } catch (Exception $e) {
      write_log('errors', $e);
    }
  }

}

