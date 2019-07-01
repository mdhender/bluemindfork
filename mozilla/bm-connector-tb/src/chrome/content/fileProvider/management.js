/**
 * BEGIN LICENSE
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

function onLoadProvider(provider) {
    let messenger = Components.classes["@mozilla.org/messenger;1"]
                            .createInstance(Components.interfaces.nsIMessenger);
    
    let fileSpaceUsed = document.getElementById("file-space-used");
    fileSpaceUsed.textContent = messenger.formatFileSize(provider.fileSpaceUsed);
    let fileSpaceUsedSwatch = document.getElementById("file-space-used-swatch");
    fileSpaceUsedSwatch.style.backgroundColor = pv.Colors.category20.values[0];
    
    let remainingFileSpace = document.getElementById("remaining-file-space");
    remainingFileSpace.textContent = messenger.formatFileSize(
    provider.remainingFileSpace);
    let remainingFileSpaceSwatch = document.getElementById("remaining-file-space-swatch");
    remainingFileSpaceSwatch.style.backgroundColor = pv.Colors.category20.values[1];
    
    let totalSpace = provider.fileSpaceUsed + provider.remainingFileSpace;
    let pieScale = 2 * Math.PI / totalSpace;
    
    let spaceDiv = document.getElementById("provider-space-visuals");
    let vis = new pv.Panel().canvas(spaceDiv)
                            .width(150)
                            .height(150);
    vis.add(pv.Wedge)
        .data([provider.fileSpaceUsed, provider.remainingFileSpace])
        .left(75)
        .top(75)
        .innerRadius(30)
        .outerRadius(65)
        .angle(function(d){ d * pieScale});
    
    vis.add(pv.Label)
        .left(75)
        .top(75)
        .font("14px Sans-Serif")
        .textAlign("center")
        .textBaseline("middle")
        .text(messenger.formatFileSize(totalSpace));
    
    vis.render();
}
