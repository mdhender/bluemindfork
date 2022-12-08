export const shadow = {
    id: "@bluemind/shadow",
    install(scope) {
        scope.Interactable.prototype.shadow = function ({ element }) {
            this.options.shadow = element;
            this.addState("cloned", { start: ["dragstart"], end: ["dragend"], target: "original" });
            return this;
        };
    },
    listeners: {
        "autoStart:before-start": ({ interaction }) => {
            const options = interaction.interactable.options;
            if (options.shadow) {
                interaction.element = createShadow(interaction.element, options.shadow);
            }
        }
    }
};

function createShadow(source, shadow) {
    const clone = shadow.cloneNode(true);
    const position = source.getBoundingClientRect();
    clone.style.position = "absolute";
    clone.style.display = "block";
    clone.style.left = position.left + "px";
    clone.style.top = position.top + "px";
    clone.style.zIndex = 1100;
    clone.style.touchAction = "none";
    clone.style.userSelect = "none";
    source.ownerDocument.body.appendChild(clone);
    return clone;
}
