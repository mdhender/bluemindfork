<template>
    <bm-list-group-item
        :class="message.states"
        :to="to"
        class="mail-message-list-item"
        active-class="active"
        @mouseenter.native="quickActionButtonsVisible = true"
        @mouseleave.native="quickActionButtonsVisible = false"
    >
        <bm-row class="align-items-center">
            <bm-col cols="1" class="selector">
                <bm-avatar :alt="from" />
                <bm-check @click.native.stop />
            </bm-col>
            <bm-col cols="8">
                <span>{{ from }}</span>
            </bm-col>
            <bm-col v-show="!quickActionButtonsVisible" cols="3" class="mail-widgets">
                <component :is="widget" v-for="widget in widgets" :key="widget.template" class="ml-2" />
            </bm-col>
            <bm-col v-show="quickActionButtonsVisible" cols="3">
                <transition name="fade">
                    <mail-message-list-item-quick-action-buttons :message="message" />
                </transition>
            </bm-col>
        </bm-row>
        <bm-row>
            <bm-col cols="1" class="mail-attachment">
                <component :is="state" v-if="!!state" class="ml-2" />
            </bm-col>
            <bm-col class="mail-subject">{{ message.subject }}</bm-col>
            <bm-col cols="3" class="mail-date">
                <span class="text-nowrap d-none d-sm-block d-md-none d-xl-block">{{ displayedDate }}</span>
                <span class="text-nowrap d-sm-none d-md-block d-xl-none">{{ smallerDisplayedDate }}</span>
            </bm-col>
        </bm-row>
    </bm-list-group-item>
</template>

<script>
const STATE_COMPONENT = {
    ["has-attachment"]: {
        components: { BmIcon },
        template: '<bm-icon icon="paper-clip"/>',
        priority: 99
    }
};
const FLAG_COMPONENT = {
    flagged: {
        components: { BmIcon },
        template: '<bm-icon icon="flag-fill"/>'
    },
    $forwarded: {
        components: { BmIcon },
        template: '<bm-icon icon="forward"/>'
    },
    answered: {
        components: { BmIcon },
        template: '<bm-icon icon="reply"/>'
    }
};

import BmAvatar from "@bluemind/styleguide/components/BmAvatar";
import BmCheck from "@bluemind/styleguide/components/BmCheck";
import BmCol from "@bluemind/styleguide/components/layout/BmCol";
import BmIcon from "@bluemind/styleguide/components/BmIcon";
import BmListGroupItem from "@bluemind/styleguide/components/lists/BmListGroupItem";
import BmRow from "@bluemind/styleguide/components/layout/BmRow";
import { DateTimeFormat } from "@bluemind/i18n";
import MailMessageListItemQuickActionButtons from "./MailMessageListItemQuickActionButtons";

export default {
    name: "MailMessageListItem",
    components: {
        BmAvatar,
        BmCheck,
        BmCol,
        BmIcon,
        BmListGroupItem,
        BmRow,
        MailMessageListItemQuickActionButtons
    },
    props: {
        to: {
            required: false,
            type: [String, Object],
            default: null
        },
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            quickActionButtonsVisible: false,

        };
    },
    computed: {
        displayedDate: function() {
            return DateTimeFormat.getRelativeFormat(this.message.date);
        },
        smallerDisplayedDate: function() {
            const dateString = DateTimeFormat.getRelativeFormat(this.message.date);
            return dateString.substring(dateString.indexOf(" ") + 1);
        },
        widgets() {
            return this.message.flags.map(flag => FLAG_COMPONENT[flag]).filter(widget => !!widget);
        },
        state() {
            return this.message.states
                .map(state => STATE_COMPONENT[state])
                .filter(state => !!state)
                .sort((a, b) => a.order < b.order)
                .shift();
        },
        from() {
            return this.message.from.formattedName;
        }
    }
};
</script>

<style lang="scss" >
@import "~@bluemind/styleguide/css/variables";

.mail-message-list-item .selector .custom-check {
    display: none !important;
    margin-left: 1em;
    padding-left: 1rem;
}

.mail-message-list-item .selector:hover .bm-avatar {
    display: none!important;
}

.mail-message-list-item .selector:hover .custom-check {
    display: inline-block!important;
}

.list-group-item.mail-message-list-item.not-seen {
    border-left: theme-color("primary") 5px solid !important;
}

.list-group-item.mail-message-list-item {
    border-left: transparent solid 5px !important;
    font-size: $font-size-lg;
}
.not-seen {
    font-weight: $font-weight-bold;
}

.mail-subject {
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    color: $text-muted;
}

.mail-date,
.mail-widgets {
    text-align: right;
}

.fade-enter-active {
    transition: opacity 0.5s;
}

.fade-leave-active {
    transition: opacity 0s;
}

.fade-enter, .fade-leave-to /* .fade-leave-active below version 2.1.8 */ {
    opacity: 0;
}
</style>
