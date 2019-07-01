<!DOCTYPE html>
<html manifest="contact.appcache">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8" />
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <link rel="shortcut icon"  type="image/x-icon" href="favicon.ico"/>
   ${cssRuntime}

    <title>${appName} - BlueMind</title>

  </head>
  <body>
  	<div id="header"><!-- needed --></div>
  	<div id="navigation"><!-- needed --></div>
  	<div id="sub-navigation"><!-- needed --></div>
  	<div id="main"><!-- needed --></div>
    <input type="hidden" id="history_input" />
    <iframe id="history_frame" width="0px" height="0px" frameborder="0" src="blank.html"><!-- needed --></iframe>
     ${jsRuntime}

    <noscript>
      <div>
        Your web browser must have JavaScript enabled
        in order for this application to display correctly.
      </div>
    </noscript>
    <!-- appcache" -->
    <iframe style='display:none' frameborder="0" src="index-offline.html"><!-- needed --></iframe>
  </body>
</html>
