<script setup>
import { computed } from "vue";
import store from "@bluemind/store";
import { CONVERSATION_LIST_COUNT } from "~/getters";

const count = computed(() => store.getters[`mail/${CONVERSATION_LIST_COUNT}`]);
</script>

<template>
    <div class="active-folder-count regular">
        <span v-if="count">{{ $tc("mail.message.starter.elements_counts", count, { count }) }}</span>
    </div>
</template>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";

.active-folder-count {
    color: $neutral-fg;
    padding-top: $sp-6;
}
</style>
