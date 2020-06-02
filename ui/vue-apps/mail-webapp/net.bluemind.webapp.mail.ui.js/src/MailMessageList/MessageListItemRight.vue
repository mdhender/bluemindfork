<template>
    <div class="message-list-item-right">
        <transition :name="quickActionsTransitionClass" mode="out-in">
            <message-list-item-quick-action-buttons
                v-if="quickActionButtonsVisible"
                key="quick-action-buttons"
                class="h-100 fade-slow"
                :message="message"
            />
            <div v-else class="h-100">
                <div class="d-flex flex-column justify-content-between h-100">
                    <div class="d-flex justify-content-end">
                        <component :is="widget" v-for="widget in widgets" :key="widget.template" />
                    </div>
                    <div class="mail-message-list-item-date text-right">
                        <span class="text-nowrap d-none d-sm-block d-md-none d-xl-block">
                            {{ displayedDate }}
                        </span>
                        <span class="text-nowrap d-sm-none d-md-block d-xl-none">
                            {{ smallerDisplayedDate }}
                        </span>
                    </div>
                </div>
            </div>
        </transition>
    </div>
</template>

<script>
const FLAG_COMPONENT = {
    [Flag.FLAGGED]: {
        components: { BmIcon },
        template: '<bm-icon class="text-warning" icon="flag-fill"/>',
        order: 3
    },
    [Flag.FORWARDED]: {
        components: { BmIcon },
        template: '<bm-icon icon="forward"/>',
        order: 1
    },
    [Flag.ANSWERED]: {
        components: { BmIcon },
        template: '<bm-icon icon="reply"/>',
        order: 2
    }
};

import { BmIcon } from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
import { Flag } from "@bluemind/email";
import { mapState } from "vuex";
import MessageListItemQuickActionButtons from "./MessageListItemQuickActionButtons";
import MailMessageListItemShadow from "./MailMessageListItemShadow";

export default {
    name: "MessageListItemRight",
    components: {
        MessageListItemQuickActionButtons,
        MailMessageListItemShadow
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        mouseIn: {
            type: Boolean,
            required: true
        }
    },
    computed: {
        ...mapState("mail-webapp", ["selectedMessageKeys"]),
        displayedDate: function() {
            const today = new Date();
            const messageDate = this.message.date;
            if (DateComparator.isSameDay(messageDate, today)) {
                return this.$d(messageDate, "short_time");
            } else if (DateComparator.isSameYear(messageDate, today)) {
                return this.$d(messageDate, "relative_date");
            }
            return this.$d(messageDate, "short_date");
        },
        smallerDisplayedDate: function() {
            return this.displayedDate.substring(this.displayedDate.indexOf(" ") + 1);
        },
        widgets() {
            return this.message.flags
                .map(flag => FLAG_COMPONENT[flag])
                .filter(widget => !!widget)
                .sort((a, b) => a.order - b.order);
        },
        quickActionButtonsVisible() {
            return this.mouseIn && this.selectedMessageKeys.length <= 1;
        },
        quickActionsTransitionClass() {
            return this.quickActionButtonsVisible ? "fade-in" : "fade-out";
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.message-list-item-right {
    min-height: 1.5rem;

    .fade-in-enter-active {
        transition: opacity 0.6s linear;
    }

    .fade-out-leave-active {
        transition: opacity 0.15s linear;
    }

    .fade-in-leave-active,
    .fade-out-enter-active {
        transition: opacity 0s linear;
    }

    .fade-in-enter,
    .fade-in-leave-to,
    .fade-out-enter,
    .fade-out-leave-to {
        opacity: 0;
    }
}
</style>
