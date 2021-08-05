<template>
    <div class="reply-to-invitation">
        <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="header px-3 pt-2 pb-3 bg-extra-light">
            <div class="font-weight-bold mb-1 d-block top">
                <template v-if="!currentEvent.status || currentEvent.status === 'NeedsAction'">
                    <bm-icon icon="event" class="mr-2" size="lg" />
                    <template v-if="message.eventInfo.isResourceBooking">
                        {{ $t("mail.ics.resourcebooking") }}
                    </template>
                    <template v-else>{{ $t("mail.ics") }}</template>
                </template>
                <template v-else>
                    <bm-icon :stacked="['event', computeEventIcon]" class="mr-2" size="lg" />
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
            <div v-if="message.eventInfo.needsReply && currentEvent.status" class="mt-3">
                <bm-button
                    variant="outline-primary"
                    class="mr-2 px-1"
                    :class="currentEvent.status === 'Accepted' ? 'active' : ''"
                    @click="answer('Accepted')"
                >
                    <bm-label-icon icon="check" icon-size="lg">{{ $t("common.accept") }}</bm-label-icon>
                </bm-button>
                <bm-button
                    variant="outline-primary"
                    class="mr-2 px-1"
                    :class="currentEvent.status === 'Tentative' ? 'active' : ''"
                    @click="answer('Tentative')"
                >
                    <bm-label-icon icon="interrogation" icon-size="lg">
                        {{ $t("common.accept.tentatively") }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    variant="outline-primary"
                    class="px-1"
                    :class="currentEvent.status === 'Declined' ? 'active' : ''"
                    @click="answer('Declined')"
                >
                    <bm-label-icon icon="cross" icon-size="lg">{{ $t("common.refuse") }}</bm-label-icon>
                </bm-button>
            </div>
        </div>
        <div v-else-if="currentEvent.loading === LoadingStatus.LOADING" class="header px-3 pt-2 pb-3 bg-extra-light">
            <div class="font-weight-bold mb-1 d-block top">
                <bm-skeleton width="30%" />
            </div>
            <div v-if="message.eventInfo.needsReply" class="mt-3 d-flex">
                <bm-skeleton-button class="mr-2 d-inline-block" width="6rem" />
                <bm-skeleton-button class="mr-2 d-inline-block" width="12rem" />
                <bm-skeleton-button class="mr-2 d-inline-block" width="6rem" />
            </div>
        </div>
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";

import { BmButton, BmIcon, BmLabelIcon, BmSkeleton, BmSkeletonButton } from "@bluemind/styleguide";

import { SET_EVENT_STATUS } from "~/actions";
import { LoadingStatus } from "~/model/loading-status";

export default {
    name: "ReplyToInvitation",
    components: {
        BmButton,
        BmIcon,
        BmLabelIcon,
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
            let icon = "event";
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
@import "~@bluemind/styleguide/css/_variables";

.reply-to-invitation {
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
}
</style>
