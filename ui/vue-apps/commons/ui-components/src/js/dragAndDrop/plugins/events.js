const EVENTS = {
    drag: [{ on: "dragmove" }],
    dragstart: [{ on: "dragstart" }],
    dragend: [{ on: "dragend" }],
    dragenter: [{ on: "dragenter" }, { on: "dragenter", target: "related" }],
    dragleave: [{ on: "dragleave" }, { on: "dragleave", target: "related" }],
    dropactivate: [{ on: "dropactivate" }],
    dropdeactivate: [{ on: "dropdeactivate" }],
    dragover: [{ on: "dragover" }],
    drop: [{ on: "drop" }, { on: "drop", target: "related" }],
    holdover: [{ on: "holdover" }, { on: "holdover", target: "related" }]
};

export const events = {
    id: "@bluemind/events",
    listeners: {
        "interactable:new": ({ interactable }) => {
            for (let eventType in EVENTS) {
                const targets = EVENTS[eventType];
                targets.forEach(clientEvent => {
                    interactable.on(clientEvent.on, event => fire(eventType, clientEvent.target, event));
                });
            }
        }
    }
};

function fire(type, target, event) {
    if (event.interaction) {
        const targets = getTargets(target, event);
        const clientEvent = createEvent(type, targets);
        targets.main.target.dispatchEvent(clientEvent);
    }
}

function createEvent(type, { main, related }) {
    const clientEvent = new Event(type);
    const data = main.data;
    clientEvent.data = data;
    clientEvent.interactable = main;
    clientEvent.relatedInteractable = related;
    clientEvent.relatedData = related && related.data;
    return clientEvent;
}

function getTargets(target, event) {
    switch (target) {
        case "related":
            return { main: event.draggable, related: event.dropzone };
        default:
            return event.interactable
                ? { main: event.interactable, related: event.dropzone }
                : { main: event.dropzone, related: event.draggable };
    }
}
