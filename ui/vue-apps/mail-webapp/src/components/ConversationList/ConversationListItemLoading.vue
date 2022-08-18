<template>
    <bm-list-group-item
        class="conversation-list-item-loading d-flex"
        :class="{
            ['conversation-list-item-' + messageListStyle]: true,
            active: isSelected
        }"
        aria-hidden="true"
    >
        <div class="conversation-list-item-left">
            <bm-skeleton-avatar />
        </div>
        <div class="conversation-list-item-middle d-flex flex-column flex-fill px-4">
            <bm-skeleton width="50%" />
            <div class="d-flex justify-content-between">
                <bm-skeleton width="65%" />
                <bm-skeleton width="20%" />
            </div>
        </div>
    </bm-list-group-item>
</template>

<script>
import { BmListGroupItem, BmSkeleton, BmSkeletonAvatar } from "@bluemind/styleguide";

export default {
    name: "ConversationListItemLoading",
    components: {
        BmSkeletonAvatar,
        BmSkeleton,
        BmListGroupItem
    },
    props: {
        isSelected: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        messageListStyle() {
            return this.$store.state.settings.mail_message_list_style;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.list-group-item.conversation-list-item-loading {
    &.conversation-list-item-compact {
        height: base-px-to-rem(44);
    }
    &.conversation-list-item-normal {
        height: base-px-to-rem(54);
    }
    &.conversation-list-item-full {
        height: base-px-to-rem(60);
    }
}
</style>
