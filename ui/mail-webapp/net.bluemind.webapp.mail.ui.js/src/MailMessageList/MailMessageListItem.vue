<template>
    <bm-list-group-item
        :class="message.states"
        :to="to"
        class="mail-message-list-item"
        active-class="active"
        @mouseenter.native="quickActionButtonsVisible = true"
        @mouseleave.native="quickActionButtonsVisible = false"
    >
        <bm-row class="align-items-center flex-nowrap">
            <bm-col cols="1" class="selector">
                <bm-avatar :alt="from" />
                <bm-check @click.native.stop />
            </bm-col>
            <bm-col cols="8" class="text-overflow">{{ from }}</bm-col>
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
            <bm-col class="mail-subject text-overflow">{{ message.subject }}</bm-col>
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

import {
    BmAvatar,
    BmCheck,
    BmCol,
    BmIcon,
    BmListGroupItem,
    BmRow
} from "@bluemind/styleguide";
import { DateTimeFormat } from "@bluemind/i18n";
import injector from "@bluemind/inject";
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
            locale: injector.getProvider("UserSession").get().lang
        };
    },
    computed: {
        displayedDate: function() {
            return DateTimeFormat.getRelativeFormat(this.message.date, this.locale);
        },
        smallerDisplayedDate: function() {
            const dateString = DateTimeFormat.getRelativeFormat(
                this.message.date,
                this.locale
            );
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
            return this.message.from.dn
                ? this.message.from.dn
                : this.message.from.address;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/variables";

.mail-message-list-item {
    cursor: pointer;
}

.mail-message-list-item:focus {
    outline: none;
}

.mail-message-list-item .selector .custom-check {
    display: none !important;
    margin-left: 1em;
    padding-left: 1rem;
}

.mail-message-list-item .selector:hover .bm-avatar {
    display: none !important;
}

.mail-message-list-item .selector:hover .custom-check {
    display: inline-block !important;
}

.list-group-item.mail-message-list-item.not-seen {
    border-left: theme-color("primary") 5px solid !important;
}

a.list-group-item.mail-message-list-item {
    border-left: transparent solid 5px !important;
    font-size: $font-size-lg;
}
.not-seen {
    font-weight: $font-weight-bold;
}

.mail-subject {
    color: $text-muted;
}

.text-overflow {
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
}

.mail-date, .mail-widgets {
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
