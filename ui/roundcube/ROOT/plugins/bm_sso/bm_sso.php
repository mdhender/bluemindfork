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
 * BlueMind SSO authentication
 *
 * Make use of BlueMind SSO authentication to perform login
 *
 */

require_once('rcube/user.php');
require_once('rcube/locator.php');
require_once('rcube/uiextension.php');

    function BMgetallheaders()
    { 
           $headers = ''; 
       foreach ($_SERVER as $name => $value) 
       { 
           if (substr($name, 0, 5) == 'HTTP_') 
           { 
	     if (substr($name, 0, 7) == 'HTTP_BM') {
               $headers[str_replace(' ', '-', str_replace('_', ' ', substr($name, 5)))] = $value;
	     } else {
               $headers[str_replace(' ', '-', ucwords(strtolower(str_replace('_', ' ', substr($name, 5)))))] = $value; 
	       }
           } 
       } 
       return $headers; 
    }
 
/*
 * 1. If token exist in session, use it to authenticate
 * 2. If no token exist or token not valid: redirect to SSO to get a valid ticket, then ckeck ticket against SSO:
 *   a. if valid ticket, get valid token
 *   b. if invalid ticket, redirect to SSO again
*/
class bm_sso extends rcube_plugin {
  public $task = '.*';

  private $ssoLogin;
  private $ssoDomain;
  private $ssoToken;
  private $lang;
  private $rcmail;

  private function getHost($service) {
    $ini_array = parse_ini_file("/etc/bm/bm.ini");
    $locator = new LocatorService($ini_array['locator'] ? $ini_array['locator'] : $ini_array['host']);
    $host = $locator->get($service, $this->ssoLogin);
    return $host;

  }

  public function init() {
    $this->initFromHeaders();
    $this->rcmail = rcmail::get_instance();
    // global hooks
    $this->add_hook('startup', array($this, 'startup'));
    $this->add_hook('ready', array($this, 'ready_hook'));

    // login hooks
    $this->add_hook('authenticate', array($this, 'authenticate'));
    $this->add_hook('login_after', array($this, 'login_after'));
    $this->add_hook('logout_after', array($this, 'logout_after'));
    $this->add_hook('login_failed', array($this, 'login_failed'));
    
    // Template
    $this->add_hook('template_object_pagetitle', array($this, 'template_object_pagetitle'));
    $this->add_hook('template_object_pagetitle', array($this, 'template_object_pagetitle'));
    $this->add_hook('render_page', array($this, 'render_page'));
  }

  // global hooks
  public function ready_hook($args) {
    $this->rcmail->config->set('language', $_SESSION['bm']['settings']['lang']);
    $this->rcmail->config->set('timezone', $_SESSION['bm']['settings']['timezone']);
    $this->rcmail->config->set('_timezone_value', $_SESSION['bm']['settings']['timezone']);
    $this->rcmail->config->set('date_format', $this->convertIsoToPhpFormat($_SESSION['bm']['settings']['date']));
    $this->rcmail->config->set('time_format', $this->convertIsoToPhpFormat($_SESSION['bm']['settings']['timeformat']));
    $this->rcmail->config->set('date_long', $this->rcmail->config->get('date_format') . ' ' . $this->rcmail->config->get('time_format'));
    $this->rcmail->config->set('date_short', 'D d M ' . $this->rcmail->config->get('time_format'));
    return $args;
  }

  private function initFromHeaders() {
     $hds = BMgetallheaders();
     list($login, $domain) = explode('@', $hds['BMUSERLATD']);
     $this->ssoDomain = $domain;
     $this->ssoLogin = $hds['BMUSERLATD']; 
     $this->ssoToken = $hds['BMSESSIONID'];
     $this->ssoUserUid = $hds['BMUSERID'];
     $lastname = base64_decode($hds['BMUSERLASTNAME']);
     $firstname = base64_decode($hds['BMUSERFIRSTNAME']);
     $_SESSION['bm_sso']['bmUserDisplayName'] = $firstname . ' ' . $lastname;

     $_SESSION['bm_sso']['bmUserId'] = $hds['BMUSERID'];
     $_SESSION['bm_sso']['bmDomain'] = $hds['BMUSERDOMAINID'];
     $_SESSION['bm_sso']['bmUserPhotoId'] = $hds['BMPHOTOID'];
     $_SESSION['bm_sso']['bmUserPerms'] = $hds['BMUSERPERMS'];
     $_SESSION['bm_sso']['bmVersion'] = $hds['BMVERSION'];
     $_SESSION['bm_sso']['bmBrandVersion'] = $hds['BMBRANDVERSION'];
     $_SESSION['bm_sso']['bmFullStringVersion'] = $hds['BMRELEASE'];
     $_SESSION['bm_sso']['bmLang'] = $hds['BMLANG'];
     $_SESSION['bm_sso']['bmMailPerms'] = $hds['BMUSERMAILPERMS'];
     $_SESSION['bm_sso']['bmIM'] = $hds['BMINSTANTMESSAGING'];
     $_SESSION['bm_sso']['bmLang'] = $hds['BMLANG'];
     $_SESSION['bm_sso']['bmSid'] = $hds['BMSESSIONID'];
     $_SESSION['bm_sso']['bmLogin'] = $hds['BMUSERLATD'];
     $_SESSION['bm_sso']['bmJID'] = $hds['BMUSERDEFAULTEMAIL'];
     $_SESSION['bm_sso']['bmHasIM'] = $hds['BMINSTANTMESSAGING'];
     $_SESSION['bm_sso']['bmRoles'] = explode(',', $hds['BMROLES']);
     $_SESSION['bm_sso']['bmDefaultEmail'] = $hds['BMUSERDEFAULTEMAIL'];
     $_SESSION['bm_sso']['bmPartition'] = $hds['BMPARTITION'];
     $_SESSION['bm_sso']['bmDataLocation'] = $hds['BMDATALOCATION'];
     $this->domain = $hds['BMUSERDOMAINID'];
  }

  public function startup($args) {
    if ($_SESSION['bm_sso']['bmMailPerms'] !== 'true') {
      header('Location: /');
    }
    $this->lang = $_SESSION['bm_sso']['bmLang'];
    $this->rcmail->set_user(new bm_user($this->rcmail->user));
    $this->rcmail->output->set_env('displayname', $_SESSION['bm_sso']['bmUserDisplayName']);
    $this->rcmail->output->set_env('photoId', $_SESSION['bm_sso']['bmUserPhotoId']);
    $this->rcmail->output->set_env('bmVersion', $_SESSION['bm_sso']['bmVersion']);
    $this->rcmail->output->set_env('bmBrandVersion', $_SESSION['bm_sso']['bmBrandVersion']);
    $this->rcmail->output->set_env('bmFullStringVersion', $_SESSION['bm_sso']['bmFullStringVersion']);
    $this->rcmail->output->set_env('im', $_SESSION['bm_sso']['bmIM']);
    $this->rcmail->output->set_env('bmLang', $_SESSION['bm_sso']['bmLang']);
    $this->rcmail->output->set_env('bmSid', $_SESSION['bm_sso']['bmSid']);
    $this->rcmail->output->set_env('bmLogin', $_SESSION['bm_sso']['bmLogin']);
    $this->rcmail->output->set_env('bmDefaultEmail', $_SESSION['bm_sso']['bmDefaultEmail']);
    $this->rcmail->output->set_env('bmDomain', $_SESSION['bm_sso']['bmDomain']);
    $this->rcmail->output->set_env('bmJID', $_SESSION['bm_sso']['bmJID']);
    $this->rcmail->output->set_env('bmHasIM', $_SESSION['bm_sso']['bmHasIM']);
    $this->rcmail->output->set_env('bmRoles', $_SESSION['bm_sso']['bmRoles']);
    $this->rcmail->output->set_env('bmUserId', $_SESSION['bm_sso']['bmUserId']);
    $this->rcmail->output->set_env('bmPartition', $_SESSION['bm_sso']['bmPartition']);
    $this->rcmail->output->set_env('bmDataLocation', $_SESSION['bm_sso']['bmDataLocation']);
    $this->rcmail->output->set_env('sessionId', $this->ssoToken);
    if($this->rcmail->decrypt($_SESSION['password']) != $this->ssoToken || $args['task'] == 'login' ||
       !$this->rcmail->session->check_auth() || empty($this->rcmail->user->ID)) {
      $args['action'] = 'login';
      $args['task'] = 'login';
      if (!$GLOBALS['OUTPUT']->ajax_call) {
        $_POST['_url'] = $_SERVER['QUERY_STRING'];
      }
    }

    // ui extension
    $uiExtensionService = new UiExtentionService($this->getHost("bm/core"), $_SESSION['bm_sso']['bmLang']);
    $this->rcmail->output->set_env('bmExtensions', $uiExtensionService->get(), false);

    return $args;
  }

  private function _loadDomainAliases() {
    $rcmail = $this->rcmail;
    $token = file_get_contents('/etc/bm/bm-core.tok');
    $client = new \BM\DomainsClient($_SESSION['bm']['core'], 
      $token);
    $domain = $client->get($this->domain);
    $_SESSION['bm_sso']['domainAliases'] = array($this->domain);
    foreach($domain->value->aliases as $alias) {
      $_SESSION['bm_sso']['domainAliases'][] = $alias;
    }
  }

  // login hooks
  public function authenticate($args) {
    $args['user'] = $this->ssoLogin;
    $args['pass'] = $this->ssoToken;
    $args['token'] = $this->ssoToken;
    $args['valid'] = true;
    $args['host'] = $this->getHost('mail/imap');
    $args['core'] = "http://" . $this->getHost('bm/core') . ":8090";
    $_SESSION['bm']['core'] = $args['core'];
    $_SESSION['bm_sso']['bmUserUid'] = $this->ssoUserUid;
    $args['cookiecheck'] = false;

    $sc = new BM\UserSettingsClient($args['core'], $this->ssoToken, $this->ssoDomain);
    $settings = $sc->get($this->ssoUserUid);

    $_SESSION['bm']['settings'] = array();
    foreach($settings as $key => $value) {
      $_SESSION['bm']['settings'][$key] = $value;
    }
    $_SESSION['language'] = $this->lang;
    $this->_loadDomainAliases();
    
    return $args;
  }
  
  public function login_after($args) {

    $this->rcmail->set_user(new bm_user($this->rcmail->user));
    if ($_GET['_task'] && $_GET['_task'] != 'login') {
      $args['_task'] = $_GET['_task'];
    }
    return $args;
  }

  public function logout_after($args) {
    header("BMUserLogin", $this->ssoLogin);
    header("BMSessionId", $this->ssoToken);
    header("Location: /webmail");
  }
  
  public function login_failed($args) {
    $this->logout_after($args);
  }
  
  public function template_object_pagetitle($args) {
    if ($this->rcmail->task == 'mail') {
      $args['content'] = rcube_label("webmail").' - BlueMind';
    } else {
      $app = preg_replace('/^.+:: /', '', $args['content']);
      $args['content'] = $app.' - BlueMind';
    }

    return $args;
  }

  public function render_page($args) {
    $this->rcmail->output->add_script("
    var i18n = {};
    i18n.nounreadmail = '" . rcube_label("nounreadmail") ."';
    i18n.oneunreadmail = '" . rcube_label("oneunreadmail") ."';
    i18n.unreadmails = '" . rcube_label("unreadmails") ."';
    ", 'head_top');
  }

  private function convertIsoToPhpFormat($format) {
    if ($format === null) {
        return null;
    }
    $convert = array( 'zzzz' => 'e', 'ZZZZ' => 'P', 'YYYY' => 'o', 'yyyy' => 'Y', 'MMMM' => 'F', 'EEEE' => 'l', 'MMM' => 'M', 'eee' => 'N',
        'ddd' => 't', 'yy' => 'y', 'ww' => 'W', 'ss' => 's', 'SS' => 'S', 'mm' => 'i', 'MM' => 'm', 'HH' => 'H', 'hh' => 'h',
        'EE' => 'D', 'dd' => 'd', 'z' => 'T', 'Z' => 'O', 'X' => 'Z', 'U' => 'U', 'r' => 'r', 'M' => 'n', 'l' => 'L', 'I' => 'I',
        'h' => 'g', 'H' => 'G', 'e' => 'w', 'd' => 'j', 'D' => 'z', 'B' => 'B', 'a' => 'A');
    $result = $format;
    foreach ($convert as $key => $value) {
        if (strpos($format, $key) !== false) {
          $result = str_replace($key, $value, $result);
          $format = str_replace($key, '', $format);
        }
    }

    return $result;
  }
}
