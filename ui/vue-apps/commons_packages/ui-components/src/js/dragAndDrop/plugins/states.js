const STATES = {
    active: {
        start: ["dragstart", "dropactivate"],
        end: ["dragend", "dropdeactivate"]
    },
    hover: {
        start: ["dragenter"],
        end: ["dragleave", "dropdeactivate"]
    }
};

export const states = {
    id: "@bluemind/states",
    install(scope) {
        scope.Interactable.prototype.disableState = function (state) {
            this.options.supportedStates = this.options.supportedStates.filter(supported => supported !== state);
        };
        scope.Interactable.prototype.addState = function (name, { start, end, target }) {
            this.options.supportedStates.push(name);
            start.forEach(eventType => this.on(eventType, setState.bind(this, name, target)));
            end.forEach(eventType => this.on(eventType, unsetState.bind(this, name, target)));
        };
    },
    listeners: {
        "interactable:new": ({ interactable }) => {
            interactable.options.supportedStates = Object.keys(STATES);
            for (let name in STATES) {
                const { end, start, target } = STATES[name];
                start.forEach(eventType => interactable.on(eventType, setState.bind(interactable, name, target)));
                end.forEach(eventType => interactable.on(eventType, unsetState.bind(interactable, name, target)));
            }
        }
    }
};

function setState(state, target, event) {
    if (this.options.supportedStates.includes(state)) {
        const prefix = event.interactable ? "bm-drag" : "bm-dropzone";
        getTarget(target, event).classList.add(prefix + "-" + state);
    }
}

function unsetState(state, target, event) {
    if (this.options.supportedStates.includes(state)) {
        const prefix = event.interactable ? "bm-drag" : "bm-dropzone";
        getTarget(target, event).classList.remove(prefix + "-" + state);
    }
}

function getTarget(target, event) {
    switch (target) {
        case "original":
            return event.interactable ? event.interactable.target : event.dropzone.target;
        case "related":
            return event.interactable ? event.interactable.target : event.draggable.target;
        default:
            return event.target;
    }
}
