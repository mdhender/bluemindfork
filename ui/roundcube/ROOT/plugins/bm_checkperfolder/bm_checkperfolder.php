<?php

/*
 * Plugin to check individual folder 
 *
 */

class bm_checkperfolder extends rcube_plugin {

  private $rcmail;

  private $max = 15;

  private $modulo = 20;

  function init() {
    $this->rcmail = rcmail::get_instance();
    $this->add_hook('folder_form', array($this, 'folder_form'));
    $this->add_hook('folder_update', array($this, 'folder_update'));
    $this->add_hook('folder_create', array($this, 'folder_create'));
    $this->add_hook('check_recent', array($this, 'check_recent'));
    $this->add_hook('getunread', array($this, 'getunread'));
    $this->add_texts('localization', true);
  }

  function check_recent($args) {
    return $this->check($args, 'recent');
  }

  function getunread($args) {
    return $this->check($args, 'uread');
  }
 
  function check($args, $count) { 
    $folders = $this->rcmail->config->get('checked_folders', array());
    $checked = array_keys($folders, true);
    $checked[] = 'INBOX';
    $checked[] = $this->rcmail->storage->get_folder();
    $all = $args['all'];
    if ($all) {
      $i = $_SESSION['bm']['checkperfolder'][$count]?$_SESSION['bm']['checkperfolder'][$count]:0;
      if (count($args['folders']) > 0) {
        $max =  ceil(count($args['folders']) / $this->modulo);
        $i = $i % $max;
        foreach($args['folders'] as $folder) {
          $num = hexdec( substr(sha1($folder), 0, 10) );
          if ($num % $max == $i) {
            $checked[] = $folder; 
          }
        }
      }
      $_SESSION['bm']['checkperfolder'][$count] = ++$i;
    }
    /* BM-13156 check that folders are unique here*/
    $checked = array_unique($checked);
    $args['folders'] = array_filter($checked);
    return $args;
  }

  function folder_form($args) {
    $content = $args['form']['props']['fieldsets']['settings']['content'];
    $options = $args['options'];
    $folder = $options['name'];
    $folders = $this->rcmail->config->get('checked_folders');
     
    if (is_array($content) && !array_key_exists('check', $content) && ($folders[$folder] || count($folders) <= $this->max) && $folder != 'INBOX') {
      $box = new html_checkbox(array('name' => '_check', 'id' => '_check', 'value' => 1));
      $content['check'] = array(
        'label' => $this->gettext('checkfornewmail'),
        'value' => $box->show($folders[$folder])
      );
    } elseif (count($folders) > $this->max && !$folders[$folder]) {
      $content['check'] = array(
          'label' => $this->gettext('checkfornewmail'),
          'value' => $this->gettext('cannotcheckmorefolder')
      );
    }

    if (is_array($content) && !array_key_exists('check', $options)) {
        $options['check'] = $checked;
    }

    $args['form']['props']['fieldsets']['settings']['content'] = $content;

    $args['options'] = $options;
    return $args;
  }

  function folder_update($args) {
    $folders = (array) $this->rcmail->config->get('checked_folders');
    $folder = $args['record']['name'];
    $checked = get_input_value('_check', RCUBE_INPUT_POST);
    
    if ($checked) {
    	$folders[$folder] = true;
    } else {
      unset($folders[$folder]);
    }
    $this->rcmail->user->save_prefs(array('checked_folders' => array_filter($folders)));
    return $args;
  }

  function folder_create($args) {
    $folders = (array) $this->rcmail->config->get('checked_folders');
    $folder = $args['record']['name'];
    $checked = get_input_value('_check', RCUBE_INPUT_POST);
    
    if ($checked) {
    	$folders[$folder] = true;
    } else {
      unset($folders[$folder]);
    }
    $this->rcmail->user->save_prefs(array('checked_folders' => array_filter($folders)));
    return $args;
  }
}
