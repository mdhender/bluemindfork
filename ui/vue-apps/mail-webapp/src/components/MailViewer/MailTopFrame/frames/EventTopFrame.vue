<script setup>
import ChainOfResponsibility from "../ChainOfResponsibility";
import { messageUtils } from "@bluemind/mail";
import store from "@bluemind/store";
import EventRequest from "../../EventViewer/EventRequest";
import EventReplied from "../../EventViewer/EventReplied";
import { computed, watch } from "vue";
import { CURRENT_MAILBOX } from "~/getters";
import { FETCH_EVENT } from "~/actions";
const { isImip, MessageHeader } = messageUtils;

const props = defineProps({ message: { type: Object, required: true } });

const isMessageImip = computed(() => isImip(props.message));
const isRequest = computed(() => props.message.headers?.some(({ name }) => name === MessageHeader.X_BM_EVENT));
const isReply = computed(() => props.message.headers?.some(({ name }) => name === MessageHeader.X_BM_EVENT_REPLIED));
watch(
    () => props.message.key,
    async function handler() {
        if (isMessageImip.value) {
            await store.dispatch(`mail/${FETCH_EVENT}`, {
                message: this.message,
                mailbox: store.state.mail.CURRENT_MAILBOX
            });
        }
    },
    { immediate: true }
);
</script>

<script>
export default {
    name: "EventTopFrame"
};
</script>

<template>
    <chain-of-responsibility :is-responsible="isMessageImip">
        <div class="event-wrapper">
            <event-request v-if="isRequest" :message="message" />
            <event-replied v-else-if="isReply" :message="message" />
        </div>
    </chain-of-responsibility>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
.event-wrapper {
    display: grid;
    background-color: $neutral-bg-lo1;
    padding-bottom: $sp-4;
    & > div {
        display: flex;
        flex-direction: column;
        gap: $sp-4;
    }
}
</style>
