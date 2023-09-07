<script setup>
import store from "@bluemind/store";
import { BmToggleableButton } from "@bluemind/ui-components";
import { SET_EVENT_STATUS } from "~/actions";
import EventHeader from "./EventHeader";
import EventDetail from "./EventDetail";
import EventFooter from "./EventFooter";
import { REPLY_ACTIONS } from "./currentEvent";

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});

const replyActions = [
    { name: REPLY_ACTIONS.ACCEPTED, icon: "check", i18n: "accept" },
    { name: REPLY_ACTIONS.TENTATIVE, icon: "interrogation", i18n: "tentatively" },
    { name: REPLY_ACTIONS.DECLINED, icon: "cross", i18n: "decline" }
];

const setEventStatus = payload => store.dispatch(`mail/${SET_EVENT_STATUS}`, payload);
</script>

<template>
    <div class="event-request">
        <event-header v-if="message.eventInfo.needsReply">
            {{ $t("mail.viewer.invitation.request") }}

            <template #actions>
                <div v-if="event.status" class="reply-buttons">
                    <bm-toggleable-button
                        v-for="action in replyActions"
                        :key="action.name"
                        :icon="action.icon"
                        :pressed="event.status === action.name"
                        @click="setEventStatus({ message, status: action.name })"
                    >
                        {{ $t(`mail.viewer.invitation.${action.i18n}`) }}
                    </bm-toggleable-button>
                </div>
            </template>
        </event-header>
        <event-detail :event="event" />
        <event-footer :event="event" />
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-request {
    .reply-buttons {
        display: flex;
        gap: $sp-6;

        .b-skeleton-button {
            height: base-px-to-rem(30);
        }
    }
}
</style>
