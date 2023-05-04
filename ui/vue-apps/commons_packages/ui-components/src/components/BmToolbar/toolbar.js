import { computed, h, inject } from "vue";
import { BmExtension } from "@bluemind/extensions.vue";

export function useToolbarContext() {
    const $context = inject("$context");
    const isInToolbar = computed(() => !$context?.renderContext || $context?.renderContext === "toolbar");
    return { isInToolbar };
}

export function normalizeSlot(slot) {
    return (Array.isArray(slot) ? slot : slot ? [slot] : []).filter(vnode => Boolean(vnode.tag));
}

export function getExtensionsContent(extension) {
    return normalizeSlot(h(BmExtension, { props: { id: "webapp", path: extension } }));
}
