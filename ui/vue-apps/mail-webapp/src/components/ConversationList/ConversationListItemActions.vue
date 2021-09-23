<template>
    <bm-button-toolbar class="conversation-list-item-actions">
        <slot name="actions" />
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar } from "@bluemind/styleguide";

export default {
    name: "ConversationListItemActions",
    components: {
        BmButtonToolbar
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/variables";
$custom-warning-color: theme-color-level("warning", $alert-bg-level);

.conversation-list-item {
    .conversation-list-item-actions {
        display: none;
        background-color: inherit !important;
        position: absolute;
        height: calc(100% - #{$sp-2});
        top: $sp-1;
        right: $sp-2;
        &:empty {
            display: none !important;
        }
    }

    @media only screen and (min-width: map-get($grid-breakpoints, "lg")) {
        &:hover {
            .conversation-list-item-actions {
                display: flex;
            }
        }
    }
}
</style>
