<template>
    <div v-if="currentEvent.loading === LoadingStatus.LOADED" class="event-viewer">
        <reply-to-counter-proposal v-if="message.eventInfo.isCounterEvent" />
        <reply-to-invitation v-else :message="message" />
        <bm-choice-group
            class="border-bottom my-3"
            :options="choices"
            :selected="choices[selectedChoice]"
            @select="index => (selectedChoice = index)"
        />
        <mail-inlines-block v-if="selectedChoice === 0" :parts="parts" :message="message">
            <template v-for="(_, slot) of $scopedSlots" v-slot:[slot]="scope">
                <slot :name="slot" v-bind="scope" />
            </template>
        </mail-inlines-block>
        <event-viewer-invitation v-else :message="message" />
    </div>
    <mail-inlines-block v-else-if="currentEvent.loading === LoadingStatus.ERROR" :parts="parts" :message="message">
        <template v-for="(_, slot) of $scopedSlots" v-slot:[slot]="scope">
            <slot :name="slot" v-bind="scope" />
        </template>
    </mail-inlines-block>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { INFO, WARNING, REMOVE } from "@bluemind/alert.store";
import { BmChoiceGroup } from "@bluemind/styleguide";
import { loadingStatusUtils } from "@bluemind/mail";
import MailInlinesBlock from "./MailInlinesBlock";
import ReplyToCounterProposal from "./ReplyToCounterProposal";
import ReplyToInvitation from "./ReplyToInvitation";
import { FETCH_EVENT } from "~/actions";
import { CURRENT_MAILBOX } from "~/getters";
import EventViewerInvitation from "./EventViewerInvitation";

const { LoadingStatus } = loadingStatusUtils;

export default {
    name: "EventViewer",
    components: {
        BmChoiceGroup,
        MailInlinesBlock,
        ReplyToInvitation,
        ReplyToCounterProposal,
        EventViewerInvitation
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        parts: {
            type: Array,
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
                options: { area: "right-panel", renderer: "DefaultAlert" }
            };
        },
        videoConferenceAlert() {
            return {
                alert: {
                    name: "mail.VIDEO_CONFERENCE",
                    uid: "VIDEO_CONFERENCE",
                    payload: this.currentEvent.conference
                },
                options: {
                    area: "right-panel",
                    renderer: "VideoConferencing",
                    icon: "video-circle",
                    dismissible: false
                }
            };
        }
    },
    watch: {
        "message.key": {
            immediate: true,
            async handler() {
                try {
                    await this.FETCH_EVENT({ message: this.message, mailbox: this.CURRENT_MAILBOX });
                    this.REMOVE(this.eventNotFoundAlert.alert);
                    if (this.currentEvent.conference) {
                        this.INFO(this.videoConferenceAlert);
                    } else {
                        this.REMOVE(this.videoConferenceAlert.alert);
                    }
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
        this.REMOVE(this.videoConferenceAlert.alert);
    },
    methods: {
        ...mapActions("mail", { FETCH_EVENT }),
        ...mapActions("alert", { INFO, REMOVE, WARNING })
    }
};
</script>
<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.event-viewer {
    .invitation {
        .bm-label-icon {
            color: $neutral-fg;
            margin-left: #{-1rem - $sp-3};

            svg {
                margin-right: $sp-3 !important;
            }
        }
    }
}
</style>
