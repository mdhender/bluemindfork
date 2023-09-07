<template>
    <div class="event-request">
        <event-reply-header
            v-if="message.eventInfo.needsReply"
            :current-event="currentEvent"
            @event-replied="status => SET_EVENT_STATUS({ message, status })"
        />
        <event-summary :current-event="currentEvent" />
        <event-footer v-if="hasAttendees" :attendees="currentEvent.attendees" />
    </div>
</template>

<script>
import { mapActions, mapState } from "vuex";
import { SET_EVENT_STATUS } from "~/actions";
import EventReplyHeader from "./EventReplyHeader";
import EventSummary from "./EventSummary";
import EventFooter from "./EventFooter";
export default {
    nam: "EventRequest",
    components: { EventReplyHeader, EventSummary, EventFooter },
    props: { message: { type: Object, required: true } },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        hasAttendees() {
            return this.currentEvent?.attendees?.length;
        }
    },
    methods: {
        ...mapActions("mail", { SET_EVENT_STATUS })
    }
};
</script>
