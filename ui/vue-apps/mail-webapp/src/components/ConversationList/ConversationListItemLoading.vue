<template>
    <bm-list-group-item
        class="conversation-list-item-loading d-flex"
        :class="{
            ['conversation-list-item-' + settings.mail_message_list_style]: true,
            active: isSelected
        }"
        aria-hidden="true"
    >
        <div class="conversation-list-item-left">
            <bm-skeleton-avatar />
        </div>
        <div class="conversation-list-item-middle d-flex flex-column flex-fill px-2">
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
import { mapState } from "vuex";

export default {
    name: "ConversationListItemLoading",
    components: {
        BmSkeletonAvatar,
        BmSkeleton,
        BmListGroupItem
    },
    props: {
        conversation: {
            type: Object,
            required: true
        },
        isSelected: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    computed: {
        ...mapState("session", { settings: ({ settings }) => settings.remote })
    }
};
</script>
