<script setup>
import { inject } from "@bluemind/inject";
import store from "@bluemind/store";
import { BmToggleableButton } from "@bluemind/ui-components";
import { SET_EVENT_STATUS } from "~/actions";
import EventHeader from "./base/EventHeader";
import EventDetail from "./base/EventDetail";
import { onUnmounted } from "vue";
import { REMOVE, WARNING } from "@bluemind/alert.store";
import EventFooter from "./base/EventFooter";
import { REPLY_ACTIONS } from "./replyActions";

const props = defineProps({
    message: { type: Object, required: true },
    event: { type: Object, required: true }
});

const replyActions = [
    { name: REPLY_ACTIONS.ACCEPTED, icon: "check", i18n: "accept" },
    { name: REPLY_ACTIONS.TENTATIVE, icon: "interrogation", i18n: "tentatively" },
    { name: REPLY_ACTIONS.DECLINED, icon: "cross", i18n: "decline" }
];

const setEventStatus = status => store.dispatch(`mail/${SET_EVENT_STATUS}`, { status });

const user = inject("UserSession").userId;

const privateEventNotSentToDelegatesAlert = {
    alert: {
        name: "mail.private_event_not_sent_to_delegates",
        uid: "PRIVATE_EVENT_NOT_SENT_TO_DELEGATES",
        payload: props.message
    },
    options: { area: "right-panel", renderer: "PrivateEventNotSentToDelegatesAlert" }
};

const fetchHasImipDelegates = async () => {
    const mailboxDelegationRule = await inject("MailboxesPersistence").getMailboxDelegationRule(user);
    hasImipDelegates = mailboxDelegationRule?.delegateUids.length;
};

const showPrivateEventNotSentToDelegatesAlert = () => {
    if (hasImipDelegates && props.event.private) {
        store.dispatch(`alert/${WARNING}`, privateEventNotSentToDelegatesAlert);
    }
};

hasImipDelegates === undefined
    ? fetchHasImipDelegates().then(showPrivateEventNotSentToDelegatesAlert)
    : showPrivateEventNotSentToDelegatesAlert();

onUnmounted(() => store.dispatch(`alert/${REMOVE}`, privateEventNotSentToDelegatesAlert.alert));
</script>

<template>
    <div class="event-request">
        <event-header v-if="event.needsResponse && event.attendee">
            <template v-if="user !== event.calendarOwner">
                <i18n path="mail.viewer.invitation.request.delegate">
                    <template #for>
                        <i18n path="mail.viewer.invitation.request.delegate_for" class="font-weight-bold">
                            <template #name>
                                {{ event.attendee.commonName }}
                            </template>
                        </i18n>
                    </template>
                </i18n>
            </template>
            <template v-else>
                {{ $t("mail.viewer.invitation.request") }}
            </template>
            <template #actions>
                <div v-if="event.status" class="reply-buttons">
                    <bm-toggleable-button
                        v-for="action in replyActions"
                        :key="action.name"
                        :icon="action.icon"
                        :pressed="event.status === action.name"
                        @click="setEventStatus(action.name)"
                    >
                        {{ $t(`mail.viewer.invitation.${action.i18n}`) }}
                    </bm-toggleable-button>
                </div>
            </template>
        </event-header>
        <div>
            <event-detail :event="event" :message="message" />
        </div>
    </div>
</template>

<script>
let hasImipDelegates;
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/typography";
@import "~@bluemind/ui-components/src/css/utils/variables";

.event-request {
    display: flex;
    flex-direction: column;

    .event-header {
        gap: $sp-4;
        .label {
            @include regular;
        }
    }

    .reply-buttons {
        display: flex;
        gap: $sp-5 + $sp-3 + $sp-2;
        min-width: 0;

        .bm-toggleable-button {
            min-width: 0;
            flex: 0 1 auto;
        }

        .b-skeleton-button {
            height: base-px-to-rem(30);
        }
    }
}
</style>
