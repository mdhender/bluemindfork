export const clone = {
    id: "@bluemind/clone",
    install(scope) {
        scope.Interactable.prototype.clone = function () {
            this.options.clone = true;
            this.addState("cloned", { start: ["dragstart"], end: ["dragend"], target: "original" });
            return this;
        };
    },
    listeners: {
        "autoStart:before-start": ({ interaction }) => {
            const options = interaction.interactable.options;
            if (options.clone) {
                interaction.element = createClone(interaction.element);
            }
        }
    }
};

function createClone(source) {
    const clone = source.cloneNode(true);
    const position = source.getBoundingClientRect();
    clone.style.position = "absolute";
    clone.style.left = position.left + "px";
    clone.style.top = position.top + "px";
    clone.style.width = source.offsetWidth + "px";
    clone.style.height = source.offsetHeight + "px";
    clone.style.zIndex = 1100;
    clone.style.touchAction = "none";
    source.ownerDocument.body.appendChild(clone);
    return clone;
}
