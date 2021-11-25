function VideoWidgetCreator() {
    const roles = ["hasFullVideoconferencing", "hasSimpleVideoconferencing"];
    var allowed = roles.some(role => RegExp(`\\b${role}\\b`).test(window.bmcSessionInfos.roles));

    if (allowed) {
        var shortid = function () {
            return "xxxxxxxx"
                .replace(/[xy]/g, c => {
                    const r = (Math.random() * 16) | 0,
                        v = c === "x" ? r : (r & 0x3) | 0x8;
                    return v.toString(16);
                })
                .toUpperCase();
        };
        var widget = document.createElement("a");
        widget.classList.add("fa", "fa-lg", "fa-video-camera");
        widget.onclick = function () {
            var url = "/visio/" + shortid();
            window.open(url, "_blank");
        };
        return widget;
    }
}
