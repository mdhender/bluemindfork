import debounce from "lodash.debounce";

export default {
    bind(el) {
        const debouncedEmitOverflownElements = debounce(() => emitOverflownElements(el), 25);
        const resizeObserver = new ResizeObserver(debouncedEmitOverflownElements);
        resizeObserver.observe(el);
        const mutationObserver = new MutationObserver(debouncedEmitOverflownElements);
        mutationObserver.observe(el, { childList: true });
    }
};

function emitOverflownElements(parentElement) {
    const detail = [];
    const parentRect = parentElement.getBoundingClientRect();
    for (const childElement of parentElement.children) {
        detail.push({ element: childElement, overflows: overflows(parentRect, childElement.getBoundingClientRect()) });
    }
    parentElement.dispatchEvent(new CustomEvent("overflown", { detail }));
}

function overflows(parentRect, childRect) {
    return (
        childRect.left < parentRect.left ||
        childRect.top < parentRect.top ||
        childRect.right > parentRect.right ||
        childRect.bottom > parentRect.bottom
    );
}
