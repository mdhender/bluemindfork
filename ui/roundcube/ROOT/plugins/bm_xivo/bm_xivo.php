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
 * Blue Mind Xivo
 */
class bm_xivo extends rcube_plugin {

  public $rcmail;

  private $caller;
  
  public function init() {
    $this->rcmail = rcmail::get_instance();
    $this->add_hook('startup', array($this, 'startup'));
    $this->register_action('plugin.bm_xivo.dial', array($this, 'dial'));
  }

  private function register() {
    $this->add_hook('template_object_searchfilter', array($this, 'searchfilter'));
    $this->add_hook('messages_list', array($this, 'message_list'));
    if ($this->rcmail->action == 'show' || $this->rcmail->action == 'preview') {
      $css = 'skins/bluemind/bm_xivo.css';
      $this->include_stylesheet($css);
      $this->add_hook('message_load', array($this, 'message_load'));
      $this->add_hook('template_object_messagebody', array($this, 'template_object_messagebody'));
      $this->include_script('bm_xivo.js');
    }
    $this->add_texts('localization', true);
  }

  public function startup() {
    if ($this->isEnabled()) {
      $this->rcmail->output->set_env('cti', 'true');
      $this->register();
    }
  }

  public function searchfilter($args) {
    if ($this->rcmail->get_storage()->isIndexEnabled()) { 
      $args['content'] = str_replace('</select>', '<option value="VOICEMAIL">'.$this->gettext('voicemail').'</option></select>', $args['content']);
    }
    return $args;
  }

  public function message_list($args) {
    $count = count($args['messages']);
    for ($i=0;$i<$count;$i++) {
      $header = $args['messages'][$i];
      $uid = $header->uid;
      if ($uid) {
        $voice = ($header->get('x-asterisk-callerid') != null);
        $voice = $voice || $header->flags['voicemail'];
        if ($voice) {
	  $header->list_flags['extra_flags']['voice'] = $voice;
          $span = html::span(array("class" => 'fa fa-phone', "title" => $this->gettext('isvoicemail')),"");
          $args['messages'][$i]->list_cols['attachment'] = $span;
        }
        
      }
    }
    return $args;
  }


  public function isEnabled() {
    if (!isset($_SESSION['bm']['cti']['enabled'])) {
      $ini_array = parse_ini_file("/etc/bm/bm.ini");
      $locator = new LocatorService($ini_array['locator'] ? $ini_array['locator'] : $ini_array['host']);

      // FIXME
      // $enabled =  $identity->isAllowed('/cti/dial');
      $enabled = true;
      try {
        $enabled =  $enabled && $locator->get('cti/frontend', $_SESSION['bm_sso']['bmLogin']);
      } catch (Exception $e) {
        $enabled = false;
      }
      $_SESSION['bm']['cti']['enabled'] = $enabled;
    }
    return $_SESSION['bm']['cti']['enabled'];
  }

  function message_load($p) {
    $this->caller = $p['object']->get_header('x-asterisk-callerid');
    return $p;
  }

  function template_object_messagebody($p ){
    if (!empty($this->caller) && $this->caller > 0) {
      $p['content'] = "<div class='cti-toolbar'>
        <table class='headers-table'>
          <tr>
            <td class='header'>
              <a onclick=\"javascript:dial($this->caller);return false;\" class='goog-inline-block goog-menu-button goog-button-base more callto'>
                <span class='inner'>Callback</span>
              </a>
            </td>
            <td class='header-title'>
              <strong>
                <a onclick=\"javascript:dial($this->caller);return false;\">" . $this->gettext(array('name' => 'callback', 'vars' => array('caller' => $this->caller))) ."</a>
              </strong>
            </td>
          </tr>
        </table>
      </div>
      $p[content]";

      return $p;
     }
   }

  public function dial() {
    try {
      $this->add_texts('localization');
      $number = get_input_value('_number', RCUBE_INPUT_POST);
      $rcmail = rcmail::get_instance();
      $cli = new BM\ComputerTelephonyIntegrationClient($_SESSION['bm']['core'],
        $rcmail->decrypt($_SESSION['password']), $_SESSION['bm_sso']['bmDomain'],
        $_SESSION['bm_sso']['bmUserId']);
      $cli->dial($number);
      $this->rcmail->output->command('display_message', '☎' , 'confirmation');
      $this->rcmail->output->send();
    } catch (Exception $e) {
    }

  }
}
