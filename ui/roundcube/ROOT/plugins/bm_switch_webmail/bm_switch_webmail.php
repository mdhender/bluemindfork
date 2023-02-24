<?php
/* BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2020
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

class bm_switch_webmail extends rcube_plugin {

  public function init() {
    $roles = $_SESSION['bm_sso']['bmRoles'];
    if (in_array('hasMailWebapp', $roles)) {
        $this->rcmail = rcmail::get_instance();
        $this->add_hook('startup', array($this, 'startup'));
        $this->add_texts('localization', true);
        $this->include_script('bm_switch_webmail.js');
        $this->register_action('plugin.bm_switch_webmail.click', array($this, 'click'));
    }
  }

  public function startup($args) {
    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'print') {
      return $args;
    }
    if (!in_array('hasWebmail', $_SESSION['bm_sso']['bmRoles'])) {
        $this->redirectToMailApp();
    }
    $sc = new BM\UserSettingsClient($_SESSION['bm']['core'], $_SESSION['bm_sso']['bmSid'], $_SESSION['bm_sso']['bmDomain']);
    $defaultMailApplication = $sc->getOne($_SESSION['bm_sso']['bmUserId'], "mail-application");
    if ($defaultMailApplication == 'mail-webapp') {
        $this->redirectToMailApp();
    }
  }

  function redirectToMailApp() {
    if ($this->rcmail->task == 'mail' && $this->rcmail->action == 'compose') {
      $to = get_input_value('_to', RCUBE_INPUT_GET);
      $to = preg_replace('/^mailto:/i', 'mailto:', $to);
    } 
    header("Location: /webapp/mail/$to");
    exit(0);
    
  }

  function click() {
    $rcmail = rcmail::get_instance();
    $userSettingsClient = new BM\UserSettingsClient($_SESSION['bm']['core'], $rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmDomain']);
    $userSettingsClient->setOne($_SESSION['bm_sso']['bmUserId'], "mail-application", "mail-webapp");
    $rcmail->output->command('redirect', "/webapp/mail/");
  }
}

?>
