/**
 * BEGIN LICENSE
 * Copyright © Blue Mind SAS, 2012-2022
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

window.addEventListener("message", async (event) => {
    console.log("received message:", event);
    if (event.data && event.data.type == "success") {
        await browser.runtime.sendMessage({
            type: "remotechooser-success",
            files: event.data.data,
        });
        window.close();
    }
});

window.eval(`
  let options = {\
    success: function (links) {\
      var files = [];\
      for (var i = 0; i < links.length; i++) {\
        var size = 0;\
        var sizeKeys = ['Content-Length', 'size'];\
        for (var j = 0; j < links[i].metadata.length; j++) {\
          if (sizeKeys.indexOf(links[i].metadata[j]['key']) >= 0) {\
            size = links[i].metadata[j]['value'];\
            break;\
          }\
        }\
        files.push({\
          size: size,\
          path: links[i].path,\
          name: links[i].name,\
        });\
      }\
      window.postMessage({type: 'success', data: files}, '*');
    },\
    cancel: function () {\
      window.close();
    },\
    multi: false,\
    close: false\
  };\
  window.application.setOptions(options);`
);
