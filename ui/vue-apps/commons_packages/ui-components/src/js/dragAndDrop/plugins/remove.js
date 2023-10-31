export const remove = {
    id: "@bluemind/remove",
    install(scope) {
        scope.Interactable.prototype.remove = function () {
            return this.draggable({}).on("dragend", event => removeFn(event));
        };
    }
};

function removeFn(event) {
    if (event.target.parentNode !== null) {
        event.target.parentNode.removeChild(event.target);
    }
}
