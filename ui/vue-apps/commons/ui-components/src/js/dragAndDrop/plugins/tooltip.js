export const tooltip = {
    install(scope) {
        scope.Interactable.prototype.tooltip = setTooltip;
    }
};

function setTooltip(options) {
    if (!this.options.tooltip) {
        this.on("dragstart", append.bind(this));
        this.on("dragmove", move.bind(this));
        this.on("dragend", remove.bind(this));
    }
    setTooltipOptions(this, options);
    if (!this.options.tooltip.element) {
        setTooltipElement(this);
    }
    updateTooltipElement(this);
}

function remove() {
    if (this.options.tooltip.element && this.options.tooltip.element.parentNode) {
        this.options.tooltip.element.parentNode.removeChild(this.options.tooltip.element);
    }
}

function append(event) {
    if (this.options.tooltip.element && !this.options.tooltip.element.parentNode && this.options.tooltip.enabled) {
        event.target.parentElement.appendChild(this.options.tooltip.element);
        this.options.tooltip.element.style.left = event.target.offsetLeft + "px";
        this.options.tooltip.element.style.top = event.target.offsetTop + event.target.offsetHeight + "px";
    }
    if (this.options.tooltip.element && this.options.tooltip.element.parentNode && !this.options.tooltip.enabled) {
        remove.call(this, event);
    }
}

function move(event) {
    append.call(this, event);
    if (this.options.tooltip.element && this.options.tooltip.element.parentNode) {
        this.options.tooltip.element.style.transform = event.target.style.transform;
    }
}

function setTooltipElement(interactable) {
    const element = interactable.context().createElement("div");
    element.style.position = "absolute";
    element.classList.add("bm-drag-tooltip");
    interactable.options.tooltip.element = element;
}

function setTooltipOptions(interactable, options) {
    if (!interactable.options.tooltip) {
        interactable.options.tooltip = { enabled: false };
    }
    if (typeof options === "string") {
        interactable.options.tooltip.text = options;
        interactable.options.tooltip.enabled = !!options.trim();
    } else if (typeof options === "boolean") {
        interactable.options.tooltip.enabled = options;
    } else if (typeof options === "object") {
        interactable.options.tooltip = Object.assign(interactable.options.tooltip, options);
        interactable.options.tooltip.enabled =
            typeof options.enabled === "undefined" ? !!options.text.trim() || !!options.element : !!options.enabled;
    }
}

function updateTooltipElement(interactable) {
    if (interactable.options.tooltip.element && interactable.options.tooltip.text) {
        interactable.options.tooltip.element.innerHTML = interactable.options.tooltip.text;
    }
}
