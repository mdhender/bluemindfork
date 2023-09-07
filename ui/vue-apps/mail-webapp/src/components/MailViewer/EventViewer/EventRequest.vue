<script setup>
import { mapActions, mapState } from "vuex";
import store from "@bluemind/store";
import { SET_EVENT_STATUS } from "~/actions";
import EventReplyHeader from "./EventReplyHeader";
import EventSummary from "./EventSummary";
import EventFooter from "./EventFooter";
import { currentEvent, hasAttendees } from "./currentEvent";

const props = defineProps({ message: { type: Object, required: true } });

const setEventStatus = payload => store.dispatch(`mail/${SET_EVENT_STATUS}`, payload);
</script>

<template>
    <div class="event-request">
        <event-reply-header
            v-if="message.eventInfo.needsReply"
            :current-event="currentEvent"
            @event-replied="status => setEventStatus({ message, status })"
        />
        <event-summary :current-event="currentEvent" />
        <event-footer v-if="hasAttendees" :attendees="currentEvent.attendees" />
    </div>
</template>
