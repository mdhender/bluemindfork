<template>
    <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="event-viewer-invitation pl-5">
        <h1>
            {{ currentEvent.organizer.name }}
            <template v-if="message.eventInfo.isResourceBooking">
                {{ $t("mail.ics.resource_got_invited") }}
            </template>
            <template v-else>
                {{ $t("mail.ics.got_invited") }}
            </template>
            <span class="font-weight-bold">&laquo;{{ currentEvent.summary }}&raquo;</span>
        </h1>
        <hr />
        <div class="font-weight-bold">
            <bm-label-icon icon="event">{{ $t("common.title") }}</bm-label-icon>
            {{ currentEvent.summary }}
        </div>
        <hr />
        <div class="font-weight-bold">
            <bm-label-icon icon="clock">{{ $t("common.when") }}</bm-label-icon>
            {{ currentEvent.date }}
        </div>
        <hr />
        <template v-if="currentEvent.conference">
            <div class="font-weight-bold">
                <bm-label-icon icon="video">{{ $t("common.videoconference") }}</bm-label-icon>
                <a ref="conference" :href="currentEvent.conference" target="_blank">{{ currentEvent.conference }}</a>
                <bm-button variant="inline" @click="copy">
                    <bm-icon icon="copy" />
                </bm-button>
            </div>
            <hr />
        </template>
        <div>
            <bm-label-icon icon="user" class="font-weight-bold d-block">{{ $t("common.organizer") }}</bm-label-icon>
            <span class="font-weight-bold">{{ currentEvent.organizer.name }}</span>
            &lt;{{ currentEvent.organizer.mail }}&gt;
        </div>
        <hr />
        <bm-label-icon icon="group" class="font-weight-bold d-block">{{
            $tc("common.attendees", currentEvent.attendees.length)
        }}</bm-label-icon>
        <div v-for="attendee in currentEvent.attendees" :key="attendee.mail">
            <span class="font-weight-bold">{{ attendee.name }}</span> &lt;{{ attendee.mail }}&gt;
        </div>
        <template v-if="currentEvent.sanitizedDescription">
            <hr />
            <div>
                <bm-label-icon icon="pencil" class="font-weight-bold d-block">
                    {{ $t("common.description") }}
                </bm-label-icon>
                <!-- eslint-disable-next-line vue/no-v-html -->
                <span v-html="currentEvent.sanitizedDescription" />
            </div>
        </template>
    </div>
    <div v-else-if="currentEvent.loading === LoadingStatus.LOADING">
        <mail-viewer-content-loading />
    </div>
</template>
<script>
import { mapState } from "vuex";
import { BmButton, BmLabelIcon, BmIcon } from "@bluemind/styleguide";
import { LoadingStatus } from "~/model/loading-status";
import MailViewerContentLoading from "./MailViewerContentLoading";
export default {
    name: "EventViewerInvitation",
    components: {
        BmButton,
        BmLabelIcon,
        BmIcon,
        MailViewerContentLoading
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
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent })
    },
    methods: {
        copy() {
            try {
                navigator.clipboard.writeText(this.currentEvent.conference);
            } catch {
                const range = document.createRange();
                range.selectNode(this.$refs["conference"]);
                window.getSelection().removeAllRanges();
                window.getSelection().addRange(range);
                document.execCommand("copy");
                window.getSelection().removeAllRanges();
            }
        }
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.event-viewer-invitation {
    .bm-label-icon {
        color: $calendar-color;
        margin-left: #{-1rem - $sp-3};
        display: flex !important;

        svg {
            margin-right: $sp-3 !important;
        }
    }
}
</style>
