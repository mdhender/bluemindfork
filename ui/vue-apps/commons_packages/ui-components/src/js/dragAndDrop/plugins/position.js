export const position = {
    id: "@bluemind/position",
    install(scope) {
        scope.Interactable.prototype.position = function ({ position }) {
            return this.draggable({}).on("dragstart", event => setPosition(event, position));
        };
    }
};

export function setPosition(event, position) {
    const gutter = 10;
    const target = event.currentTarget;
    target.style.position = "absolute";
    const { top, left, width, height } = target.getBoundingClientRect();

    switch (position) {
        case "left":
            target.style.left = event.pageX - gutter + "px";
            target.style.top = event.pageY - height / 2 + "px";
            break;
        case "right":
            target.style.left = event.pageX + height - gutter + "px";
            target.style.top = event.pageY - height / 2 + "px";
            break;
        case "top":
            target.style.left = event.pageX - width / 2 + "px";
            target.style.top = event.pageY + gutter + "px";
            break;
        case "bottom":
            target.style.left = event.pageX - width / 2 + "px";
            target.style.top = event.pageY - height - gutter + "px";
            break;
        case "center":
            target.style.left = event.pageX - width / 2 + "px";
            target.style.top = event.pageY - height / 2 + "px";
            break;
        default:
            var handle = target.querySelector(position);
            if (handle) {
                const {
                    top: handleTop,
                    left: handleLeft,
                    width: handlWidth,
                    height: handleHeight
                } = handle.getBoundingClientRect();
                target.style.left = event.pageX - (handleLeft + handlWidth / 2 - left) / 2 + "px";
                target.style.top = event.pageY - (handleTop + handleHeight / 2 - top) + "px";
            }
    }
}
