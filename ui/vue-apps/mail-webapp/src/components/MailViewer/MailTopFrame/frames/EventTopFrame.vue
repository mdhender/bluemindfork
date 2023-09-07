<template>
    <chain-of-responsibility :is-responsible="isImip">
        <div class="event-wrapper">
            <event-request v-if="isRequest" :message="message" />
        </div>
    </chain-of-responsibility>
</template>

<script>
import { mapGetters, mapState, mapActions } from "vuex";
import ChainOfResponsibility from "../ChainOfResponsibility";
import { messageUtils } from "@bluemind/mail";
import EventRequest from "../../EventViewer/EventRequest";
import { CURRENT_MAILBOX } from "~/getters";
import { FETCH_EVENT } from "~/actions";
const { isImip, MessageHeader } = messageUtils;
export default {
    name: "EventTopFrame",
    components: { ChainOfResponsibility, EventRequest },
    props: { message: { type: Object, required: true } },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapGetters("mail", { CURRENT_MAILBOX }),
        isImip() {
            return isImip(this.message);
        },
        isRequest() {
            return this.message.headers && this.message.headers.some(({ name }) => name === MessageHeader.X_BM_EVENT);
        }
    },
    watch: {
        "message.key": {
            immediate: true,
            async handler() {
                if (this.isImip) {
                    await this.FETCH_EVENT({ message: this.message, mailbox: this.CURRENT_MAILBOX });
                }
            }
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_EVENT })
    }
};
</script>

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
