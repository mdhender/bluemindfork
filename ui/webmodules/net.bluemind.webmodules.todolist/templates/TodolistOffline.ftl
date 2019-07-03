<!DOCTYPE html>
<html manifest="task.appcache">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"><!-- needed --></meta>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"><!-- needed --></meta>
    <meta name="gwt:property" content="locale=en"></meta>
    <link rel="shortcut icon"  type="image/x-icon" href="favicon.ico"><!-- needed --></link>
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
  <iframe style='display:none' frameborder="0" src="index-offline.html"><!-- needed --></iframe>
 </body>
</html>