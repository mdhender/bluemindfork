<template>
    <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="event-viewer">
        <reply-to-counter-proposal v-if="message.eventInfo.isCounterEvent" />
        <reply-to-invitation v-else />
        <bm-choice-group
            class="border-bottom my-3"
            :options="choices"
            :selected="choices[selectedChoice]"
            @select="index => (selectedChoice = index)"
        />
        <parts-viewer v-if="selectedChoice === 0" :message-key="message.key" />
        <event-viewer-invitation v-else :message="message" />
    </div>
    <div v-else-if="currentEvent.loading === LoadingStatus.ERROR">
        <parts-viewer :message-key="message.key" />
    </div>
</template>
<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { WARNING, REMOVE } from "@bluemind/alert.store";
import { BmChoiceGroup } from "@bluemind/styleguide";
import PartsViewer from "./PartsViewer/PartsViewer";
import ReplyToCounterProposal from "./ReplyToCounterProposal";
import ReplyToInvitation from "./ReplyToInvitation";
import { FETCH_EVENT } from "~actions";
import { CURRENT_MAILBOX } from "~getters";
import EventViewerInvitation from "./EventViewerInvitation";
import { LoadingStatus } from "../../model/loading-status";

export default {
    name: "EventViewer",
    components: {
        BmChoiceGroup,
        PartsViewer,
        ReplyToInvitation,
        ReplyToCounterProposal,
        EventViewerInvitation
    },
    props: {
        message: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            choices: [
                { text: this.$t("common.message"), value: "message" },
                { text: this.$t("common.invitation"), value: "invitation" }
            ],
            selectedChoice: 0,
            LoadingStatus
        };
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapGetters("mail", { CURRENT_MAILBOX }),
        eventNotFoundAlert() {
            return {
                alert: { name: "mail.EVENT_NOT_FOUND", uid: "EVENT_NOT_FOUND" },
                options: { area: "mail-thread", renderer: "DefaultAlert" }
            };
        }
    },
    watch: {
        "message.key": {
            immediate: true,
            async handler() {
                this.REMOVE(this.eventNotFoundAlert.alert);
                try {
                    await this.FETCH_EVENT({ message: this.message, mailbox: this.CURRENT_MAILBOX });
                } catch {
                    this.WARNING(this.eventNotFoundAlert);
                }
            }
        },
        currentEvent() {
            this.selectedChoice = 0;
        }
    },
    destroyed() {
        this.REMOVE(this.eventNotFoundAlert.alert);
    },
    methods: {
        ...mapActions("mail", { FETCH_EVENT }),
        ...mapActions("alert", { REMOVE, WARNING })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.event-viewer {
    .invitation {
        .bm-label-icon {
            color: $calendar-color;
            margin-left: #{-1rem - $sp-3};

            svg {
                margin-right: $sp-3 !important;
            }
        }
    }
}
</style>
