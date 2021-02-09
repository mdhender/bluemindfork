<template>
    <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="event-viewer-invitation pl-5">
        <h1>
            {{ currentEvent.organizer.name }} {{ $t("mail.ics.got_invited") }}
            <span class="font-weight-bold">&laquo;{{ currentEvent.summary }}&raquo;</span>
        </h1>
        <hr />
        <div class="font-weight-bold">
            <bm-label-icon icon="event" class="d-block">{{ $t("common.title") }}</bm-label-icon>
            {{ currentEvent.summary }}
        </div>
        <hr />
        <div class="font-weight-bold">
            <bm-label-icon icon="clock" class="d-block">{{ $t("common.when") }}</bm-label-icon>
            {{ currentEvent.date }}
        </div>
        <hr />
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
import { BmLabelIcon } from "@bluemind/styleguide";
import { LoadingStatus } from "../../model/loading-status";
import MailViewerContentLoading from "./MailViewerContentLoading";
export default {
    name: "EventViewerInvitation",
    components: {
        BmLabelIcon,
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
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.event-viewer-invitation {
    .bm-label-icon {
        color: $calendar-color;
        margin-left: #{-1rem - $sp-3};

        svg {
            margin-right: $sp-3 !important;
        }
    }
}
</style>
