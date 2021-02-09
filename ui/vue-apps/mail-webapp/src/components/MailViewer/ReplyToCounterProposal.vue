<template>
    <div class="reply-to-counter-proposal">
        <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="header px-3 pt-2 pb-3 bg-extra-light">
            <div class="font-weight-bold mb-2 d-block top">
                <bm-icon :stacked="agendaStackedIcons" class="mr-2" size="lg" />
                {{ counterEventInfo }}
            </div>
            <template v-if="currentEvent.counter">
                <h2 v-if="currentEvent.counter.occurrence" class="ml-4">
                    {{ $t("mail.ics.counter.schedule.proposal.exception") }}
                </h2>
                <h2 v-else class="ml-4">{{ $t("mail.ics.counter.schedule.proposal") }}</h2>
                <hr class="mt-0 mb-2 ml-4" />
                <div class="mb-2 d-block">
                    <bm-icon icon="clock" class="mr-2 text-primary" size="lg" />
                    <del>{{ currentEvent.counter.initialDate }}</del>
                    &nbsp;
                    <strong>{{ currentEvent.counter.proposedDate }}</strong>
                </div>
                <div v-if="currentEvent.counter.occurrence" class="mb-2 d-block">
                    <bm-icon icon="loop" class="mr-2 text-primary" size="lg" />
                    {{ currentEvent.date }}
                </div>
                <div class="mt-3 ml-4">
                    <bm-button variant="outline-primary" class="mr-2 px-1" @click="ACCEPT_COUNTER_EVENT">
                        <bm-label-icon icon="check" icon-size="lg">{{
                            $t("mail.ics.counter.schedule.proposal.accept")
                        }}</bm-label-icon>
                    </bm-button>
                    <bm-button variant="outline-primary" class="mr-2 px-1" @click="DECLINE_COUNTER_EVENT">
                        <bm-label-icon icon="cross" icon-size="lg">{{
                            $t("mail.ics.counter.schedule.proposal.decline")
                        }}</bm-label-icon>
                    </bm-button>
                </div>
            </template>
            <template v-else>
                <h2 class="text-primary ml-4">{{ $t("mail.ics.counter.schedule.proposal.handled") }}</h2>
            </template>
        </div>
        <div v-else-if="currentEvent.loading === LoadingStatus.LOADING" class="header px-3 pt-2 pb-3 bg-extra-light">
            <div class="font-weight-bold mb-1 d-block top">
                <bm-skeleton width="30%" />
            </div>
            <div v-if="message.eventInfo.needsReply" class="mt-3 d-flex">
                <bm-skeleton-button class="ml-4 d-inline-block" width="6rem" />
                <bm-skeleton-button class="ml-4 d-inline-block" width="6rem" />
            </div>
        </div>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { BmButton, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import { ACCEPT_COUNTER_EVENT, DECLINE_COUNTER_EVENT } from "~actions";
import { LoadingStatus } from "../../model/loading-status";

export default {
    name: "ReplyToCounterProposal",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon
    },
    data() {
        return { LoadingStatus };
    },
    computed: {
        ...mapState("mail-webapp/currentMessage", { currentMessageKey: "key" }),
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapState("mail", ["messages"]),
        message() {
            return this.messages[this.currentMessageKey];
        },
        counterEventInfo() {
            const attendee = this.message.from.dn
                ? this.message.from.dn + " <" + this.message.from.address + ">"
                : this.message.from.address;
            if (this.currentEvent.counter) {
                return this.$t("mail.ics.counter." + this.currentEvent.counter.status.toLowerCase(), { attendee });
            } else {
                return this.$t("mail.ics.counter", { attendee });
            }
        },
        agendaStackedIcons() {
            if (!this.currentEvent.counter) {
                return ["event"];
            }
            if (this.currentEvent.counter.status === "Declined") {
                return ["event", "cross"];
            }
            return ["event", "interrogation"];
        }
    },
    methods: {
        ...mapActions("mail", { ACCEPT_COUNTER_EVENT, DECLINE_COUNTER_EVENT })
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.reply-to-counter-proposal {
    .header .top {
        .fa-check {
            color: $green;
        }
        .fa-cross {
            color: $red;
        }
        .fa-interrogation {
            color: $purple;
        }
    }

    hr {
        border-color: $calendar-color;
        margin-bottom: $sp-1;
        max-width: 66%;
        margin-left: 0;
    }
}
</style>
