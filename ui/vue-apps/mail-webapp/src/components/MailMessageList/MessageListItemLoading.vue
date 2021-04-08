<template>
    <bm-list-group-item
        class="message-list-item d-flex"
        :class="{
            ['message-list-item-' + settings.mail_message_list_style]: true,
            active: MESSAGE_IS_SELECTED(message.key) || IS_CURRENT_MESSAGE(message)
        }"
        aria-hidden="true"
    >
        <div class="message-list-item-left">
            <bm-skeleton-avatar />
        </div>
        <div class="message-list-item-middle d-flex flex-column flex-fill px-2">
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
import { mapGetters, mapState } from "vuex";
import { IS_CURRENT_MESSAGE, MESSAGE_IS_SELECTED } from "~getters";

export default {
    name: "MessageListItemLoading",
    components: {
        BmSkeletonAvatar,
        BmSkeleton,
        BmListGroupItem
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    computed: {
        ...mapGetters("mail", { IS_CURRENT_MESSAGE, MESSAGE_IS_SELECTED }),
        ...mapState("session", { settings: ({ settings }) => settings.remote })
    }
};
</script>
