<template>
    <div v-if="!loading" class="event-viewer">
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
</template>
<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmChoiceGroup } from "@bluemind/styleguide";
import PartsViewer from "./PartsViewer/PartsViewer";
import ReplyToCounterProposal from "./ReplyToCounterProposal";
import ReplyToInvitation from "./ReplyToInvitation";
import { FETCH_EVENT } from "~actions";
import { CURRENT_MAILBOX } from "~getters";
import EventViewerInvitation from "./EventViewerInvitation";

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
            loading: true
        };
    },
    computed: {
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        ...mapGetters("mail", { CURRENT_MAILBOX })
    },
    watch: {
        message: {
            immediate: true,
            async handler() {
                this.loading = true;
                await this.FETCH_EVENT({ message: this.message, mailbox: this.CURRENT_MAILBOX });
                this.loading = false;
            }
        },
        currentEvent() {
            this.selectedChoice = 0;
        }
    },
    methods: {
        ...mapActions("mail", { FETCH_EVENT })
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
