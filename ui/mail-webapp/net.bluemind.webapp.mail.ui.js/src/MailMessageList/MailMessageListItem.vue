<template>
    <bm-list-group-item
        :class="message.states"
        :to="to"
        class="mail-message-list-item"
        active-class="active"
        @mouseenter.native="quickActionButtonsVisible = true"
        @mouseleave.native="quickActionButtonsVisible = false"
    >
        <bm-row class="align-items-center flex-nowrap no-gutters">
            <bm-col cols="1" class="selector">
                <bm-avatar :alt="from" />
                <bm-check @click.native.stop />
            </bm-col>
            <bm-col cols="8" class="text-overflow">
                <div v-bm-tooltip.ds500.viewport :title="from" class="text-overflow mw-100 sender">
                    {{ from }}
                </div>
            </bm-col>
            <bm-col cols="3">
                <transition name="fade" mode="out-in">
                    <div v-if="!quickActionButtonsVisible" class="float-right">
                        <component
                            :is="widget"
                            v-for="widget in widgets"
                            :key="widget.template"
                        />
                    </div>
                    <mail-message-list-item-quick-action-buttons
                        v-if="quickActionButtonsVisible" 
                        :message="message"
                    />
                </transition>
            </bm-col>
        </bm-row>
        <bm-row class="no-gutters">
            <bm-col cols="1" class="mail-attachment">
                <component :is="state" v-if="!!state" class="ml-1" />
            </bm-col>
            <bm-col class="text-secondary text-overflow subject">
                <div 
                    v-bm-tooltip.ds500.bottom.viewport
                    :title="message.subject"
                    class="text-overflow mw-100"
                >
                    {{ message.subject }}
                </div>
            </bm-col>
            <bm-col cols="3" class="text-right">
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
    BmRow,
    BmTooltip
} from "@bluemind/styleguide";
import { DateComparator } from "@bluemind/date";
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
    directives: { BmTooltip },
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
            quickActionButtonsVisible: false
        };
    },
    computed: {
        displayedDate: function() {
            const today = new Date();
            const messageDate = this.message.date;
            if (DateComparator.isSameDay(messageDate, today)) {
                return this.$d(messageDate, 'short_time');
            } else if (DateComparator.isSameYear(messageDate, today)) {
                return this.$d(messageDate, 'relative_date');
            }
            return this.$d(messageDate, 'short_date');
        },
        smallerDisplayedDate: function() {
            return this.displayedDate.substring(this.displayedDate.indexOf(" ") + 1);
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

.mail-message-list-item .bm-avatar {
    width: 1.3rem !important;
    height: 1.3rem !important;
}

.mail-message-list-item .selector .custom-check {
    display: none !important;
    margin-left: 0.825rem;
    padding-left: 0.825rem;
    min-height: 1.4rem;
}

.mail-message-list-item .selector:hover .bm-avatar {
    display: none !important;
}

.mail-message-list-item .selector:hover .custom-check {
    display: block !important;
}

.custom-control-label::after, .custom-control-label::before{
    top: 0.2rem !important;
}

.list-group-item.mail-message-list-item.not-seen {
    border-left: theme-color("primary") 4px solid !important;
}

a.list-group-item.mail-message-list-item {
    border-left: transparent solid 4px !important;
    font-size: $font-size-lg;
}
.list-group-item.mail-message-list-item:focus {
   outline: $outline;
   &:hover {
    background-color: darken($component-active-bg, 10%);
   }
}

.not-seen .sender, .not-seen .subject {
    font-weight: $font-weight-bold;
}

.text-overflow {
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
}

.fade-enter-active {
    transition: opacity 0.2s;
}

.fade-leave-active {
    transition: opacity 0.3s;
}

.fade-enter, .fade-leave-to /* .fade-leave-active below version 2.1.8 */ {
    opacity: 0;
}
</style>
