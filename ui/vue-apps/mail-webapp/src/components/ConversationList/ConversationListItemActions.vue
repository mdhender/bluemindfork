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

.conversation-list-item {
    .conversation-list-item-actions {
        display: none;
        background-color: $surface;
        position: absolute;
        height: calc(100% - #{$sp-2});
        top: $sp-1;
        right: $sp-2;
        &:empty {
            display: none !important;
        }
    }

    &.warning-custom:not(.active) .conversation-list-item-actions > * {
        background-color: $warning-bg-lo1;
    }
    &:hover .conversation-list-item-actions > * {
        background-color: $neutral-bg-lo1;
    }
    &:hover.active .conversation-list-item-actions > * {
        background-color: $secondary-bg;
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
