import debounce from "lodash.debounce";

export const refresh = {
    id: "@bluemind/refresh",
    install(scope) {
        scope.Interactable.prototype.refresh = function () {
            if (!scope.interactStatic.dynamicDrop()) {
                const listener = function () {
                    scope.interactStatic.dynamicDrop(false);
                    this.off("dragmove", listener);
                    this.off("dragend", listener);
                }.bind(this);
                scope.interactStatic.dynamicDrop(true);
                this.on("dragmove", listener);
                this.on("dragend", listener);
            }
        };
    },
    listeners: {
        "interactable:new": ({ interactable }) => {
            interactable.on(
                "autoscroll",
                debounce(() => {
                    interactable.refresh();
                }, 50)
            );
        }
    }
};
