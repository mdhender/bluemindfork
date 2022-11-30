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
 * BlueMind webmail resources
 *
 *
 */

require_once('rcube/user.php');
require_once('rcube/webresources.php');
  
 
class bm_webmail extends rcube_plugin {

  private $rcmail;

  public function init() {
    $this->rcmail = rcmail::get_instance();
    // global hooks
    $this->add_hook('startup', array($this, 'startup'));
    $this->add_hook('render_page', array($this, 'render_page'));
  }

  public function startup($args) {
    // web resources
    $webResourcesService = new WebResourcesService($_SESSION['bm_sso']['bmTopoCore'], $_SESSION['bm_sso']['bmLang']);
    $r = $webResourcesService->get();
    $bundles = '';
    if ($r != null) {
      foreach ($r->css as $css) {
        $this->include_stylesheet($css);
      }
      foreach ($r->js as $js) {
        if($js->lifecycle ) {
          $bundles = $bundles.$js->bundle.",";
        }
      }
    }
    $this->rcmail->output->set_env('bundles',$bundles);
    return $args;
  }

  public function render_page($args) {
    if ($this->rcmail->task == 'settings' || ($this->rcmail->task == 'mail' && ($this->rcmail->action == '' || $this->rcmail->action == 'show'))) {
      $webResourcesService = new WebResourcesService($_SESSION['bm_sso']['bmTopoCore'], $_SESSION['bm_sso']['bmLang']);
      $r = $webResourcesService->get();
      if ($r != null) {
        foreach ($r->css as $css) {
          $this->include_stylesheet($css);
        }
        
	$content = '';
        foreach ($r->js as $js) {
          $l = "false";
          if( $js->lifecycle ) {
		$l = "true";
	  }
	  $content= $content.'bmLoadBundle("'.$js->bundle.'","plugins/bm_webmail/'.$js->path.'",'.$l.');';
        }
        $this->rcmail->output->add_footer(html::tag('script', array('type' => "text/javascript"),$content));
      }
    }
  }
}

