<script setup>
import { BmButton } from "@bluemind/ui-components";
</script>

<template>
    <div class="trash-result-header">
        <span>{{ $t("mail.list.recoverable.header") }}</span>
        <bm-button variant="text-accent" to="/">{{ $t("mail.list.recoverable.link", { count: 3 }) }}</bm-button>
    </div>
</template>
<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables.scss";

.trash-result-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    & > span {
        color: $neutral-fg;
    }
}
</style>
