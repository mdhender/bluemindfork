<script setup>
import { computed } from "vue";
import store from "@bluemind/store";
import { CONVERSATION_LIST_COUNT } from "~/getters";

const count = computed(() => store.getters[`mail/${CONVERSATION_LIST_COUNT}`]);
</script>
<template>
    <div class="search-result-content-header">
        {{ $t("common.search") }} :
        {{ $tc("common.messages", count, { count }) }}
    </div>
</template>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";

.search-result-content-header {
    text-align: center;
    padding: $sp-3;
}
</style>
