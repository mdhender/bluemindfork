import { computed, inject } from "vue";

export function useToolbarContext() {
    const context = inject("$context");
    const isInToolbar = computed(() => context === "toolbar");
    return { isInToolbar };
}
