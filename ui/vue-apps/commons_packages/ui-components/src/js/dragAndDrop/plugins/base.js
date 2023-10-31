import { events } from "./events";
import { holdover } from "./holdover";
import { refresh } from "./refresh";
import { states } from "./states";
import { tooltip } from "./tooltip";
import { iframe } from "./iframe";
import { clone } from "./clone";
import { disableTouch } from "./disableTouch";
import { shadow } from "./shadow";
import { position } from "./position";
import { move } from "./move";
import { remove } from "./remove";

export const base = {
    id: "@bluemind/base",
    install(scope) {
        scope.usePlugin(tooltip);
        scope.usePlugin(holdover);
        scope.usePlugin(refresh);
        scope.usePlugin(events);
        scope.usePlugin(states);
        scope.usePlugin(iframe);
        //
        scope.usePlugin(disableTouch);
        scope.usePlugin(clone);
        scope.usePlugin(move);
        scope.usePlugin(position);
        scope.usePlugin(remove);
        scope.usePlugin(shadow);

        scope.resetDropzone = [];
    },
    listeners: {
        "interactable:new": ({ interactable, options }) => {
            interactable.name = options.name || "";
            interactable.data = options.data;
        },
        "interactions:action-start": ({ iEvent: { interactable } }, scope) => {
            return activateDropzones(interactable, scope);
        },
        "interactions:action-move": ({ iEvent: { interactable } }, scope) => {
            if (scope.dynamicDrop) {
                return activateDropzones(interactable, scope);
            }
        }
    }
};

function activateDropzones(interactable, { interactables }) {
    if (interactable) {
        interactables.list.forEach(dropzone => {
            if (Array.isArray(dropzone.options.drop.accept)) {
                dropzone.options.drop.enabled = dropzone.options.drop.accept.includes(interactable.name);
            }
        });
    }
}
