const nav = {};

export default {
    bind(el, binding) {
        if (!nav[binding.arg]) {
            nav[binding.arg] = [];
        }
        nav[binding.arg].push(el);
        el.addEventListener("keydown", event => {
            if (["ArrowRight", "Right", "ArrowDown", "Down"].includes(event.key)) {
                event.shiftKey ? focusEnd(nav[binding.arg]) : focusNext(event.target, nav[binding.arg]);
            } else if (["ArrowLeft", "Left", "ArrowUp", "Up"].includes(event.key)) {
                event.shiftKey ? focusStart(nav[binding.arg]) : focusPrevious(event.target, nav[binding.arg]);
            }
            event.stopImmediatePropagation();
        });
        ensureTabAccess(el);
    },
    unbind(el, binding) {
        const index = nav[binding.arg].findIndex(element => element === el);
        if (index >= 0) {
            nav[binding.arg].splice(index, 1);
        }
        if (nav[binding.arg].length === 0) {
            delete nav[binding.arg];
        }
    },
    inserted(el, binding) {
        sortElementsByDOMOrder(nav[binding.arg]);
    }
};

function focusNext(currentElement, elements) {
    focus(currentElement, elements, false);
}

function focusPrevious(currentElement, elements) {
    focus(currentElement, elements, true);
}

function focus(currentElement, elements, reverse) {
    const currentIndex = elements.findIndex(element => element === currentElement);
    const nextIndex = reverse ? next(currentIndex, elements.length - 1) : previous(currentIndex, elements.length - 1);
    const nextElement = elements[nextIndex];

    if (nextElement && !isVisible(nextElement)) {
        focus(nextElement, elements, reverse);
        return;
    }
    nextElement?.focus();
}

function focusEnd(elements) {
    focusMax(elements, false);
}

function focusStart(elements) {
    focusMax(elements, true);
}

function focusMax(elements, reverse) {
    const index = reverse ? 0 : elements.length - 1;
    const element = elements[index];
    if (!isVisible(element)) {
        const sliced = reverse ? elements.slice(1, elements.length) : elements.slice(0, index);
        focusMax(sliced, reverse);
        return;
    }
    element?.focus();
}

function next(index, maxIndex) {
    return index === 0 ? maxIndex : index - 1;
}

function previous(index, maxIndex) {
    return index === maxIndex ? 0 : index + 1;
}

function isVisible(element) {
    return !!element.getClientRects().length;
}

function ensureTabAccess(element) {
    if (!element.hasAttribute("tabindex") && !["INPUT", "BUTTON"].includes(element.tagName)) {
        element.setAttribute("tabindex", "-1");
    }
}

function sortElementsByDOMOrder(elements) {
    elements.sort((a, b) => (a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1));
}
