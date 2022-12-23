/** Emit focusout only if the target is not inside the bound element. */
export default {
    bind(el, binding, vnode) {
        el.tabIndex = 0;
        el.addEventListener("focusout", function (event) {
            if (!el.contains(event?.relatedTarget)) {
                if (vnode?.componentInstance) {
                    vnode.componentInstance.$emit("focusout");
                } else {
                    el.dispatchEvent(new CustomEvent("focusout"));
                }
            }
        });
    }
};
