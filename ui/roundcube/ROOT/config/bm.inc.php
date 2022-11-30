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

// BlueMind initialization
require_once('Zend/Loader/Autoloader.php');
$autoloader = Zend_Loader_Autoloader::getInstance();
$autoloader->registerNamespace('BM\\');
$autoloader->registerNamespace('Elastica\\');
$autoloader->registerNamespace('rcube_');
$autoloader->registerNamespace('rcube\\');
$autoloader->registerNamespace('Psr\\');
$autoloader->setDefaultAutoloader(function($class) {
   $path = $class;
   $path = str_replace('_', '/', $path);
   $path = str_replace('\\', '/', $path);
   require_once($path . '.php');
});

class LocateCoreStrategy {
  public function execute() {
    return $_SESSION['bm_sso']['bmTopoCore'];
  }
}

function getBMDBConf() {
  $ini_array = parse_ini_file("/etc/bm/bm.ini");

  return strtolower($ini_array['dbtype']).'://'.$ini_array['user'].':'.$ini_array['password'].'@'.$ini_array['host'].'/'.$ini_array['db'];
}

// FIXME
class BMSerializableObject {
  public function __construct(array $arguments = array()) {
    if (!empty($arguments)) {
      foreach ($arguments as $property => $argument) {
        // if ($argument instanceof Closure) {
        //  What should we do for anonymous method ? 
        // }
        $this->{$property} = $argument;
      }
    }
  }

  public function serialize() {
    return json_encode($this);
  }
}
class BMSerializableArray {
  private $data;
  public function __construct(array $data = array()) {
    $this->data = $data;
  }

  public function serialize() {
    return json_encode($this->data);
  }
}
?>
