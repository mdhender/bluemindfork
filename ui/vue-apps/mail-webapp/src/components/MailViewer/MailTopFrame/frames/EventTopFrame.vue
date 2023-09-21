<script setup>
import { computed, watch } from "vue";
import { messageUtils, loadingStatusUtils } from "@bluemind/mail";
import store from "@bluemind/store";
import ChainOfResponsibility from "../ChainOfResponsibility";
import { CURRENT_MAILBOX } from "~/getters";
import { FETCH_EVENT } from "~/actions";
import {
    EventRequest,
    EventReplied,
    EventCanceled,
    EventLoading,
    EventNotFound,
    EventCountered
} from "../../EventViewer";

const { isImip, MessageHeader } = messageUtils;
const { LoadingStatus } = loadingStatusUtils;

const props = defineProps({ message: { type: Object, required: true } });

const event = computed(() => store.state.mail.consultPanel.currentEvent);

const isMessageImip = computed(() => isImip(props.message));
watch(
    () => props.message.key,
    async function handler() {
        if (isMessageImip.value) {
            try {
                await store.dispatch(`mail/${FETCH_EVENT}`, {
                    message: props.message,
                    mailbox: store.getters[`mail/${CURRENT_MAILBOX}`]
                });
            } catch (e) {
                if (e !== "Event not found") {
                    throw e;
                }
            }
        }
    },
    { immediate: true }
);

const hasHeader = header => computed(() => props.message.headers?.some(({ name }) => name === header));
const isRequest = hasHeader(MessageHeader.X_BM_EVENT);
const isCountered = hasHeader(MessageHeader.X_BM_EVENT_COUNTERED);
const isReply = hasHeader(MessageHeader.X_BM_EVENT_REPLIED);
const isCanceled = hasHeader(MessageHeader.X_BM_EVENT_CANCELED);
</script>

<script>
export default {
    name: "EventTopFrame"
};
</script>

<template>
    <chain-of-responsibility :is-responsible="isMessageImip">
        <div class="event-wrapper">
            <event-loading v-if="event.loading === LoadingStatus.LOADING" />
            <event-canceled v-else-if="isCanceled" :message="message" />
            <event-not-found v-else-if="event.loading === LoadingStatus.ERROR" :event="event" />
            <event-request v-else-if="isRequest" :message="message" :event="event" />
            <event-countered v-else-if="isCountered" :message="message" :event="event" />
            <event-replied v-else-if="isReply" :message="message" :event="event" />
        </div>
    </chain-of-responsibility>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
.event-wrapper {
    display: grid;
    background-color: $neutral-bg-lo1;
    padding-bottom: $sp-4;

    > div {
        min-width: 0;
        min-height: 0;
    }
}
</style>
