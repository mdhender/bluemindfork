<template>
    <bm-list-group-item
        v-touch:touchhold="onTouch"
        class="d-flex message-list-item"
        :class="{
            ['message-list-item-' + userSettings.mail_message_list_style]: true,
            'not-seen': !message.flags.includes(Flag.SEEN),
            'warning-custom': message.flags.includes(Flag.FLAGGED),
            active: MESSAGE_IS_SELECTED(message.key) || IS_ACTIVE_MESSAGE(message)
        }"
        role="link"
        @click.exact="navigateTo"
        @keyup.enter.exact="navigateTo"
        @mouseenter="mouseIn = true"
        @mouseleave="mouseIn = false"
    >
        <screen-reader-only-message-information :message="message" />
        <message-list-item-left :message="message" @toggle-select="$emit('toggle-select', message.key, true)" />
        <message-list-item-middle
            class="flex-fill px-2"
            :message="message"
            :is-important="message.flags.includes(Flag.FLAGGED)"
            :mouse-in="mouseIn"
        />
        <message-list-item-quick-action-buttons v-show="mouseIn" :message="message" />
    </bm-list-group-item>
</template>

<script>
import { BmListGroupItem } from "@bluemind/styleguide";
import { Flag } from "@bluemind/email";
import { mapGetters, mapState } from "vuex";
import MessageListItemLeft from "./MessageListItemLeft";
import MessageListItemMiddle from "./MessageListItemMiddle";
import MessageListItemQuickActionButtons from "./MessageListItemQuickActionButtons";
import ScreenReaderOnlyMessageInformation from "./ScreenReaderOnlyMessageInformation";
import { IS_ACTIVE_MESSAGE, MESSAGE_IS_SELECTED } from "~getters";

export default {
    name: "MessageListItem",
    components: {
        BmListGroupItem,
        MessageListItemLeft,
        MessageListItemMiddle,
        MessageListItemQuickActionButtons,
        ScreenReaderOnlyMessageInformation
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        isMuted: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return {
            mouseIn: false,
            Flag
        };
    },
    computed: {
        ...mapState("mail", ["folders", "activeFolder", "selection"]),
        ...mapGetters("mail", { MESSAGE_IS_SELECTED, IS_ACTIVE_MESSAGE }),
        ...mapState("session", { userSettings: ({ settings }) => settings.remote })
    },
    methods: {
        onTouch() {
            this.$emit("toggleSelect", this.message.key);
        },
        navigateTo() {
            this.$router.navigate({ name: "v:mail:message", params: { message: this.message } });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item {
    border-left: transparent solid 4px !important;
    .states .fa-event {
        color: $calendar-color;
    }
    &.not-seen {
        border-left: theme-color("primary") 4px solid !important;
    }

    &.not-seen .mail-message-list-item-sender,
    &.not-seen .mail-message-list-item-subject {
        font-weight: $font-weight-bold;
    }

    &.message-list-item-full {
        padding-top: $sp-1 !important;
        padding-bottom: $sp-1 !important;
        .mail-message-list-item-subject,
        .mail-message-list-item-date {
            line-height: $line-height-sm;
        }
        .mail-message-list-item-preview {
            display: block;
        }
    }

    &.message-list-item-compact {
        padding-top: $sp-1 !important;
        padding-bottom: $sp-1 !important;
        .mail-message-list-item-subject,
        .mail-message-list-item-date {
            line-height: $line-height-sm;
        }
        .mail-message-list-item-preview {
            display: none;
        }
    }

    &.message-list-item-normal .mail-message-list-item-preview,
    &.message-list-item-null .mail-message-list-item-preview {
        display: none;
    }

    // obtain the same enlightment that BAlert applies on $warning TODO move to variables.scss in SG
    $custom-warning-color: theme-color-level("warning", $alert-bg-level);

    &.warning-custom:not(.active) {
        background-color: $custom-warning-color;
    }
}
</style>
