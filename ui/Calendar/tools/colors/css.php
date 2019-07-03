<?php


function fill($i, $light, $dark, $border, $bizTop, $bizBot, $dim="") {
$txtShadow = "  text-shadow: 0 -1px 0 rgba(0, 0, 0, 0.25);"; 
if ($dim == "-dim") {
  $txtShadow = "";
}
  $pattern = "
.calendar$i$dim dt {
  border-bottom: 1px solid #$bizTop;
}
.calendar$i$dim dd {
  border-top: 1px solid #$bizBot;
}
.calendar$i$dim {
$txtShadow
  border: 1px solid #$border;
  background: #$light;
  background-image: linear-gradient(to bottom, #$light, #$dark);
  /* @alternate */ background-image: -khtml-gradient(linear, left top, left bottom, from(#$light), to(#$dark));
  /* @alternate */ background-image: -moz-linear-gradient(top, #$light, #$dark);
  /* @alternate */ background-image: -ms-linear-gradient(top, #$light, #$dark);
  /* @alternate */ background-image: -webkit-gradient(linear, left top, left bottom, color-stop(0%, #$light), color-stop(100%, #$dark));
  /* @alternate */ background-image: -webkit-linear-gradient(top, #$light, #$dark);
  /* @alternate */ background-image: -o-linear-gradient(top, #$light, #$dark);
  filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#$light', endColorstr='#$dark', GradientType=0);
}
.monthCalendar$i$dim {
  color: #$light !important;
  font-weight: bold;
}
";
  
  echo $pattern;

}

$c = array();
// $c[] = array(LIGHT, DARK, BORDER, BIZTOP, "BIZBOTTOM)
$c[] = array("3D99FF", "3385E3", "2967B0", "2C79D1", "66AFFF"); // bleu clair
$c[] = array("FF6638", "E4491D", "A13514", "B53A16", "FF835E"); // orange foncé
$c[] = array("62CD00", "4FAF00", "519100", "5DA600", "7CDE00"); // vert pomme
$c[] = array("D07BE3", "B552CC", "912AA8", "B838D4", "E587FA"); // violet clair
$c[] = array("FFAD40", "FF9100", "AD6300", "D17900", "FFBD63"); // orange clair
$c[] = array("9E9E9E", "888888", "5E5E5E", "787878", "B0B0B0"); // gris clair

$c[] = array("00D5D5", "00BCBC", "009696", "00ADAD", "59EBEB"); // turquoise
$c[] = array("F56A9E", "FF478D", "912951", "B33264", "FF78AC"); // rose poudré
$c[] = array("E9D200", "DAB600", "AB9100", "C9AB00", "FFE036"); // jaune \o/
$c[] = array("A77F65", "84664D", "785348", "8F6256", "B38779"); // marron
$c[] = array("B3CB00", "8BAC00", "7E9100", "96AD00", "D2F200"); // vert
$c[] = array("B6A5E9", "8979ED", "756DC9", "867CE6", "ADA6FF"); // parme (TFC-délavé style)

$c[] = array("4C3CD9", "3B2EAA", "201A5C", "2F2787", "5A4ED9"); // bleu foncé / violet
$c[] = array("B00021", "8D0019", "590010", "780015", "D10026"); // pourpre
$c[] = array("6B9990", "538177", "3A5952", "4A7369", "68A194"); // un vert chelou 
$c[] = array("A8A171", "9A925D", "736D45", "857E50", "BAB26F"); // une couleur chelou
$c[] = array("860072", "6E005D", "4A003F", "660057", "AB0092"); // un violet très foncé
$c[] = array("8C98BA", "6D79A0", "4F5673", "656E94", "97A6DE"); // un gris qui tire vers le bleu

$c[] = array("C98FA4", "B87B92", "875A6C", "9C687C", "ED9FBE"); // un rose dégueulasse
$c[] = array("725299", "5B417B", "46335E", "573F75", "8A64BA"); // violet
$c[] = array("5C5C5C", "434343", "303030", "424242", "6B6B6B"); // gris foncé

for($i = 0;$i<sizeof($c);$i++) {
  fill($i, $c[$i][0], $c[$i][1],$c[$i][2], $c[$i][3], $c[$i][4]);
  fill($i, colourBrightness($c[$i][0]),
  colourBrightness($c[$i][1]),
  colourBrightness($c[$i][2]),
  colourBrightness($c[$i][3]),
  colourBrightness($c[$i][4]), '-dim');
}

function colourBrightness($hex, $percent=0.4) {
 // Work out if hash given
 $hash = '';
 if (stristr($hex,'#')) {
  $hex = str_replace('#','',$hex);
  $hash = '#';
 }
 /// HEX TO RGB
 $rgb = array(hexdec(substr($hex,0,2)), hexdec(substr($hex,2,2)), hexdec(substr($hex,4,2)));
 //// CALCULATE
 for ($i=0; $i<3; $i++) {
  // See if brighter or darker
  if ($percent > 0) {
   // Lighter
   $rgb[$i] = round($rgb[$i] * $percent) + round(255 * (1-$percent));
  } else {
   // Darker
   $positivePercent = $percent - ($percent*2);
   $rgb[$i] = round($rgb[$i] * $positivePercent) + round(0 * (1-$positivePercent));
  }
  // In case rounding up causes us to go to 256
  if ($rgb[$i] > 255) {
   $rgb[$i] = 255;
  }
 }
 //// RBG to Hex
 $hex = '';
 for($i=0; $i < 3; $i++) {
  // Convert the decimal digit to hex
  $hexDigit = dechex($rgb[$i]);
  // Add a leading zero if necessary
  if(strlen($hexDigit) == 1) {
  $hexDigit = "0" . $hexDigit;
  }
  // Append to the hex string
  $hex .= $hexDigit;
 }
 return $hash.$hex;
}

?>
