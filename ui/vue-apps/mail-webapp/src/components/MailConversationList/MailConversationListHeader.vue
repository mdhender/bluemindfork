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
}
</style>
