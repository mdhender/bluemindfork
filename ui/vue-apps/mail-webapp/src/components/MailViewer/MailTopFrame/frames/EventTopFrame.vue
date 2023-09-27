<script setup>
import { computed, watch } from "vue";
import { messageUtils, loadingStatusUtils } from "@bluemind/mail";
import store from "@bluemind/store";
import ChainOfResponsibility from "../ChainOfResponsibility";
import { CURRENT_MAILBOX } from "~/getters";
import { FETCH_EVENT } from "~/actions";
import useEventComponent from "../../EventViewer/useEventComponent";

const { isImip, MessageHeader } = messageUtils;
const { LoadingStatus } = loadingStatusUtils;

const props = defineProps({ message: { type: Object, required: true } });

const event = computed(() => store.state.mail.consultPanel.currentEvent);

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

const typeOfEvent = computed(() => {
    const typeIs = checkXmbEventType(props.message.headers);

    if (event.value.loading === LoadingStatus.ERROR) {
        return typeIs(MessageHeader.X_BM_EVENT_CANCELED) ? "CANCELED" : "NOT_FOUND";
    }
    if (typeIs(MessageHeader.X_BM_EVENT_DECLINECOUNTER)) {
        return "DECLINE_COUNTER";
    }
    if (typeIs(MessageHeader.X_BM_EVENT)) {
        return "REQUEST";
    }
    if (typeIs(MessageHeader.X_BM_EVENT_COUNTERED)) {
        return "COUNTER";
    }
    if (typeIs(MessageHeader.X_BM_EVENT_REPLIED)) {
        return "REPLY";
    }
    return "LOADING";
});

function checkXmbEventType(headersList = []) {
    return anHeaderName => headersList.findIndex(({ name }) => name === anHeaderName) !== -1;
}
const eventInsert = useEventComponent(typeOfEvent);
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
