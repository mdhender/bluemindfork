<script setup>
import MailConversationListFilters from "./MailConversationListFilters";
</script>
<template>
    <div class="mail-conversation-list-header">
        <slot name="toolbar">
            <mail-conversation-list-filters />
        </slot>
        <slot />
    </div>
</template>

<style lang="scss">
@use "sass:math";
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "../ConversationList/variables.scss";

.mail-conversation-list-header {
    display: flex;
    background-color: $surface;
    .mail-conversation-list-filters {
        border-bottom: 1px solid $neutral-fg-lo3;
    }
    & > :last-child {
        border-bottom: 1px solid $neutral-fg-lo2 !important;
    }
}
</style>
