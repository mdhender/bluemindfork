<script setup>
import { computed } from "vue";
import { mapActions, mapState } from "vuex";
import EventRepliedHeader from "./EventRepliedHeader";
import EventSummary from "./EventSummary";
import EventFooter from "./EventFooter";
import { currentEvent, hasAttendees } from "./currentEvent";
import { loadingStatusUtils } from "@bluemind/mail";

const { LoadingStatus } = loadingStatusUtils;

const props = defineProps({ message: { type: Object, required: true } });
const fromAttendee = computed(() =>
    currentEvent.value.attendees?.find(attendee => attendee.mail === props.message.from.address)
);
</script>

<template>
    <div class="event-replied">
        <event-replied-header
            v-if="fromAttendee"
            :current-event="currentEvent"
            :from-attendee="fromAttendee"
            :is-occurrence="!!message.eventInfo.recuridIsoDate"
        />
        <event-summary :current-event="currentEvent" />
        <event-footer v-if="hasAttendees" :attendees="currentEvent.attendees" />
    </div>
</template>
