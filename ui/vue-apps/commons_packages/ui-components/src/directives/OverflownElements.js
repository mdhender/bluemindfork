import throttle from "lodash.throttle";

export default {
    bind(el, binding, vnode) {
        let last;
        const listener = throttle(() => {
            const detail = getDetail(vnode.elm);
            if (hasChanged(detail, last)) {
                emit(vnode, detail);
                last = detail;
                listener.cancel();
            }
        }, 250);
        const resizeObserver = new ResizeObserver(listener);
        resizeObserver.observe(el);
        const mutationObserver = new MutationObserver(listener);
        mutationObserver.observe(el, { childList: true });
    }
};

function getDetail(element) {
    const container = element.getBoundingClientRect();
    const detail = [];
    for (const child of element.children) {
        detail.push({ element: child, overflows: overflows(container, child.getBoundingClientRect()) });
    }
    return detail;
}

function hasChanged(detail, old) {
    return (
        detail.length !== old?.length ||
        detail.some((entry, i) => old[i].element !== entry.element || old[i].overflows !== entry.overflows)
    );
}
function emit(vnode, detail) {
    if (vnode.child) {
        vnode.child.$emit("overflown", { detail });
    } else {
        vnode.elm.dispatchEvent(new CustomEvent("overflown", { detail }));
    }
}

function overflows(parentRect, childRect) {
    return (
        childRect.left < parentRect.left ||
        childRect.top < parentRect.top ||
        childRect.right > parentRect.right ||
        childRect.bottom > parentRect.bottom
    );
}
