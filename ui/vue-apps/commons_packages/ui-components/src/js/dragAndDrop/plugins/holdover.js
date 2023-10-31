let timer = null;
export const holdover = {
    id: "@bluemind/holdover",
    install(scope) {
        scope.actions.phaselessTypes.holdover = true;
    },
    listeners: {
        "interactable:new": ({ interactable }) => {
            interactable.on("dragenter", event => {
                timer = setTimeout(() => {
                    interactable.fire({ ...event, interaction: event.interaction, type: "holdover" });
                }, 300);
            });
            interactable.on("dragleave", () => clearTimeout(timer));
            interactable.on("dragend", () => clearTimeout(timer));
            interactable.on("autoscroll", () => clearTimeout(timer));
        }
    }
};
