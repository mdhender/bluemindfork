export const disableTouch = {
    id: "@bluemind/disableTouch",
    install(scope) {
        scope.Interactable.prototype.disableTouch = function () {
            this.on("down", disableTouchFn);
            return this;
        };
    }
};

function disableTouchFn(event) {
    event.interactable.options.drag.enabled = event.pointerType !== "touch";
}
