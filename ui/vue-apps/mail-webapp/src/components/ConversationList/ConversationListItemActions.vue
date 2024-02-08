<template>
    <div class="conversation-list-item-actions">
        <slot name="actions" />
    </div>
</template>

<script>
export default {
    name: "ConversationListItemActions"
};
</script>
<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/variables";

.conversation-list-item {
    .conversation-list-item-actions {
        display: none;
        background-color: $surface;
        position: absolute;
        top: 1px;
        bottom: 1px;
        right: 1px;
        margin-right: $sp-5;
        align-items: center;
        &:empty {
            display: none !important;
        }

        .bm-toolbar,
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
