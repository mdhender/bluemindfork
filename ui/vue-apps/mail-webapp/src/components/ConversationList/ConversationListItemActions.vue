<template>
    <bm-button-toolbar class="conversation-list-item-actions">
        <slot name="actions" />
    </bm-button-toolbar>
</template>

<script>
import { BmButtonToolbar } from "@bluemind/ui-components";

export default {
    name: "ConversationListItemActions",
    components: {
        BmButtonToolbar
    }
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/mixins/responsiveness";
@import "~@bluemind/ui-components/src/css/variables";

.conversation-list-item {
    .conversation-list-item-actions {
        display: none;
        background-color: $surface;
        position: absolute;
        top: 1px;
        bottom: 1px;
        right: 1px;
        align-items: center;
        &:empty {
            display: none !important;
        }

        .btn-group,
        .btn {
            height: 100%;
        }
    }

    &.warning-custom:not(.active) .conversation-list-item-actions > * {
        background-color: $warning-bg;
    }
    &:hover .conversation-list-item-actions > * {
        background-color: $neutral-bg-lo1;
    }
    &:hover.active .conversation-list-item-actions > * {
        background-color: $secondary-bg;
    }

    @media only screen {
        @include from-lg {
            &:hover {
                .conversation-list-item-actions {
                    display: flex;
                }
            }
        }
    }
}
</style>
