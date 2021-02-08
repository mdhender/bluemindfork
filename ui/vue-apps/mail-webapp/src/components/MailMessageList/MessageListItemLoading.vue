<template>
    <bm-list-group-item
        class="message-list-item d-flex"
        :class="{
            ['message-list-item-' + userSettings.mail_message_list_style]: true,
            active: MESSAGE_IS_SELECTED(message.key) || currentMessageKey === message.key
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
import { MESSAGE_IS_SELECTED } from "~getters";

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
        ...mapGetters("mail", { MESSAGE_IS_SELECTED }),
        ...mapState("session", ["userSettings"]),
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" })
    }
};
</script>
