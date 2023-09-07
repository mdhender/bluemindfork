import { computed } from "vue";
import store from "@bluemind/store";

export const REPLY_ACTIONS = {
    ACCEPTED: "Accepted",
    TENTATIVE: "Tentative",
    DECLINED: "Declined"
};

export const currentEvent = computed(() => store.state.mail.consultPanel.currentEvent);
export const hasAttendees = computed(() => !!currentEvent.value?.attendees?.length);
