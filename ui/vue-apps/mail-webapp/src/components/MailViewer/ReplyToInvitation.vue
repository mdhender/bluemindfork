<template>
    <div class="reply-to-invitation">
        <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="header">
            <div class="font-weight-bold mb-1 d-block top">
                <template v-if="!currentEvent.status || currentEvent.status === 'NeedsAction'">
                    <bm-icon icon="calendar" class="mr-2" />
                    <template v-if="message.eventInfo.isResourceBooking">
                        {{ $t("mail.ics.resourcebooking") }}
                    </template>
                    <template v-else>{{ $t("mail.ics") }}</template>
                </template>
                <template v-else>
                    <bm-icon :stacked="['calendar', computeEventIcon]" class="mr-2" />
                    <template v-if="currentEvent.status === 'Accepted'">
                        <template v-if="message.eventInfo.isResourceBooking">
                            {{ $t("mail.ics.resourcebooking.accepted") }}
                        </template>
                        <template v-else>
                            {{ $t("mail.ics.accepted") }}
                        </template>
                    </template>
                    <template v-else-if="currentEvent.status === 'Tentative'">
                        <template v-if="message.eventInfo.isResourceBooking">
                            {{ $t("mail.ics.resourcebooking.accepted.tentatively") }}
                        </template>
                        <template v-else>
                            {{ $t("mail.ics.accepted.tentatively") }}
                        </template>
                    </template>
                    <template v-else-if="currentEvent.status === 'Declined'">
                        <template v-if="message.eventInfo.isResourceBooking">
                            {{ $t("mail.ics.resourcebooking.declined") }}
                        </template>
                        <template v-else>
                            {{ $t("mail.ics.declined") }}
                        </template>
                    </template>
                </template>
            </div>
            <div v-if="message.eventInfo.needsReply && currentEvent.status" class="reply-buttons">
                <bm-toggleable-button
                    icon="check"
                    :pressed="currentEvent.status === 'Accepted'"
                    @click="answer('Accepted')"
                >
                    {{ $t("common.accept") }}
                </bm-toggleable-button>
                <bm-toggleable-button
                    icon="interrogation"
                    :pressed="currentEvent.status === 'Tentative'"
                    @click="answer('Tentative')"
                >
                    {{ $t("common.accept.tentatively") }}
                </bm-toggleable-button>
                <bm-toggleable-button
                    icon="cross"
                    :pressed="currentEvent.status === 'Declined'"
                    @click="answer('Declined')"
                >
                    {{ $t("common.refuse") }}
                </bm-toggleable-button>
            </div>
        </div>
        <div v-else-if="currentEvent.loading === LoadingStatus.LOADING" class="header">
            <div class="font-weight-bold mb-1 d-block top">
                <bm-skeleton width="30%" />
            </div>
            <div v-if="message.eventInfo.needsReply" class="reply-buttons">
                <bm-skeleton-button class="mr-2 d-inline-block" width="6rem" />
                <bm-skeleton-button class="mr-2 d-inline-block" width="12rem" />
                <bm-skeleton-button class="mr-2 d-inline-block" width="6rem" />
            </div>
        </div>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";

import { BmIcon, BmToggleableButton, BmSkeleton, BmSkeletonButton } from "@bluemind/ui-components";
import { loadingStatusUtils } from "@bluemind/mail";

import { SET_EVENT_STATUS } from "~/actions";

const { LoadingStatus } = loadingStatusUtils;

export default {
    name: "ReplyToInvitation",
    components: {
        BmIcon,
        BmToggleableButton,
        BmSkeleton,
        BmSkeletonButton
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return { LoadingStatus };
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        computeEventIcon() {
            let icon = "calendar";
            if (this.currentEvent.status) {
                switch (this.currentEvent.status) {
                    case "Accepted":
                        icon = "check";
                        break;
                    case "Tentative":
                        icon = "interrogation";
                        break;
                    case "Declined":
                        icon = "cross";
                        break;
                }
            }
            return icon;
        }
    },
    methods: {
        ...mapActions("mail", { SET_EVENT_STATUS }),
        answer(status) {
            this.SET_EVENT_STATUS({ message: this.message, status });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";
@import "./_variables.scss";

.reply-to-invitation {
    padding-top: $sp-4;
    padding-bottom: $sp-5;
    padding-left: $sp-5;
    padding-right: $inserts-padding-right;
    background-color: $neutral-bg-lo1;

    .header .reply-buttons {
        display: flex;
        gap: $sp-6;

        .b-skeleton-button {
            height: base-px-to-rem(30);
        }
    }

    .header .top {
        .fa-check {
            color: $success-fg;
        }
        .fa-cross {
            color: $danger-fg;
        }
        .fa-interrogation {
            color: $info-fg;
        }
    }
}
</style>
