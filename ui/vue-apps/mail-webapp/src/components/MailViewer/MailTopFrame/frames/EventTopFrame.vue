<script setup>
import { computed, watch } from "vue";
import { messageUtils, loadingStatusUtils } from "@bluemind/mail";
import store from "@bluemind/store";
import ChainOfResponsibility from "../../../ChainOfResponsibility.vue";
import * as EVENT_COMPONENT from "../../EventViewer";
import { CURRENT_MAILBOX } from "~/getters";
import { FETCH_EVENT } from "~/actions";
const { isImip, MessageHeader } = messageUtils;
const { LoadingStatus } = loadingStatusUtils;

const props = defineProps({ message: { type: Object, required: true } });

const isMessageImip = computed(() => isImip(props.message));
watch(
    () => props.message.key,
    async function handler() {
        if (isMessageImip.value) {
            await store
                .dispatch(`mail/${FETCH_EVENT}`, {
                    message: props.message,
                    mailbox: store.getters[`mail/${CURRENT_MAILBOX}`]
                })
                .catch(e => {
                    if (e !== "Event not found") {
                        throw e;
                    }
                });
        }
    },
    { immediate: true }
);

const event = computed(() => store.state.mail.consultPanel.currentEvent);
const eventInsert = computed(() => {
    const typeIs = checkXmbEventType(props.message.headers);

    if (event.value.loading === LoadingStatus.LOADING) {
        return EVENT_COMPONENT.EventLoading;
    }
    if (typeIs(MessageHeader.X_BM_EVENT_CANCELED)) {
        return EVENT_COMPONENT.EventCanceled;
    }
    if (event.value.loading === LoadingStatus.ERROR) {
        return EVENT_COMPONENT.EventNotFound;
    }
    if (typeIs(MessageHeader.X_BM_EVENT_DECLINECOUNTER)) {
        return EVENT_COMPONENT.EventDeclineCounter;
    }
    if (typeIs(MessageHeader.X_BM_EVENT)) {
        return EVENT_COMPONENT.EventRequest;
    }
    if (typeIs(MessageHeader.X_BM_COUNTER_ATTENDEE)) {
        return EVENT_COMPONENT.EventNotificationForward;
    }
    if (typeIs(MessageHeader.X_BM_EVENT_COUNTERED)) {
        return EVENT_COMPONENT.EventCountered;
    }
    if (typeIs(MessageHeader.X_BM_EVENT_REPLIED)) {
        return EVENT_COMPONENT.EventReplied;
    }
    return null;
});

const checkXmbEventType = (headersList = []) => {
    return anHeaderName =>
        headersList.findIndex(({ name }) => name.toUpperCase() === anHeaderName.toUpperCase()) !== -1;
};
</script>

<script>
export default {
    name: "EventTopFrame"
};
</script>

<template>
    <chain-of-responsibility :is-responsible="isMessageImip">
        <div class="event-wrapper">
            <component :is="eventInsert" :message="message" :event="event" />
        </div>
    </chain-of-responsibility>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-wrapper {
    display: grid;
    background-color: $neutral-bg-lo1;
    padding-bottom: $sp-5;

    > div {
        min-width: 0;
        min-height: 0;
    }
}
</style>
