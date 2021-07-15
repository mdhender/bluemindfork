function VideoWidgetCreator() {
    const roles = ["hasFullVideoconferencing", "hasSimpleVideoconferencing"];
    var allowed = window.bmcSessionInfos.roles.split(",").find(role => roles.includes(role));
    if (allowed) {
        var uuid = function () {
            return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx"
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
            var url = "/visio/" + uuid();
            window.open(url, "_blank");
        };
        return widget;
    }
}
