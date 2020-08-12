<template>
    <div class="ics-viewer">
        <div class="header px-3 pt-2 pb-3 bg-extra-light">
            <div class="font-weight-bold mb-1 d-block top">
                <template v-if="!currentEvent.status || currentEvent.status === 'NeedsAction'">
                    <bm-icon icon="event" class="mr-2" size="lg" /> {{ $t("mail.ics") }}
                </template>
                <template v-else>
                    <bm-icon :stacked="['event', computeEventIcon]" class="mr-2" size="lg" />
                    <template v-if="currentEvent.status === 'Accepted'">{{ $t("mail.ics.accepted") }}</template>
                    <template v-else-if="currentEvent.status === 'Tentative'">
                        {{ $t("mail.ics.accepted.tentatively") }}
                    </template>
                    <template v-else-if="currentEvent.status === 'Declined'">{{ $t("mail.ics.declined") }}</template>
                </template>
            </div>
            <div v-if="message.ics.needsReply && currentEvent.status" class="mt-3">
                <bm-button
                    variant="outline-primary"
                    class="mr-2 px-1"
                    :class="currentEvent.status === 'Accepted' ? 'active' : ''"
                    @click="answer('Accepted')"
                >
                    <bm-label-icon icon="check" icon-size="lg">{{ $t("common.accept") }}</bm-label-icon>
                </bm-button>
                <bm-button
                    variant="outline-primary"
                    class="mr-2 px-1"
                    :class="currentEvent.status === 'Tentative' ? 'active' : ''"
                    @click="answer('Tentative')"
                >
                    <bm-label-icon icon="interrogation" icon-size="lg">
                        {{ $t("common.accept.tentatively") }}
                    </bm-label-icon>
                </bm-button>
                <bm-button
                    variant="outline-primary"
                    class="px-1"
                    :class="currentEvent.status === 'Declined' ? 'active' : ''"
                    @click="answer('Declined')"
                >
                    <bm-label-icon icon="cross" icon-size="lg">{{ $t("common.refuse") }}</bm-label-icon>
                </bm-button>
            </div>
        </div>
        <bm-choice-group
            class="border-bottom my-3"
            :options="choices"
            :selected="choices[selectedChoice]"
            @select="index => (selectedChoice = index)"
        />
        <parts-viewer v-if="selectedChoice === 0" class="message" />
        <div v-else class="invitation pl-5">
            <h1>
                {{ currentEvent.organizer.name }} {{ $t("mail.ics.got_invited") }}
                <span class="font-weight-bold">&laquo;{{ currentEvent.summary }}&raquo;</span>
            </h1>
            <hr />
            <div class="font-weight-bold">
                <bm-label-icon icon="event" class="d-block">{{ $t("common.title") }}</bm-label-icon>
                {{ currentEvent.summary }}
            </div>
            <hr />
            <div class="font-weight-bold">
                <bm-label-icon icon="clock" class="d-block">{{ $t("common.when") }}</bm-label-icon>
                {{ currentEvent.date }}
            </div>
            <hr />
            <div>
                <bm-label-icon icon="user" class="font-weight-bold d-block">{{ $t("common.organizer") }}</bm-label-icon>
                <span class="font-weight-bold">{{ currentEvent.organizer.name }}</span>
                &lt;{{ currentEvent.organizer.mail }}&gt;
            </div>
            <hr />
            <bm-label-icon icon="group" class="font-weight-bold d-block">{{
                $tc("common.attendees", currentEvent.attendees.length)
            }}</bm-label-icon>
            <div v-for="attendee in currentEvent.attendees" :key="attendee.mail">
                <span class="font-weight-bold">{{ attendee.name }}</span> &lt;{{ attendee.mail }}&gt;
            </div>
        </div>
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmButton, BmChoiceGroup, BmIcon, BmLabelIcon } from "@bluemind/styleguide";
import PartsViewer from "./PartsViewer/PartsViewer";

export default {
    name: "IcsViewer",
    components: {
        BmButton,
        BmChoiceGroup,
        BmIcon,
        BmLabelIcon,
        PartsViewer
    },
    data() {
        return {
            choices: [
                { text: this.$t("common.message"), value: "message" },
                { text: this.$t("common.invitation"), value: "invitation" }
            ],
            selectedChoice: 0
        };
    },
    computed: {
        ...mapGetters("mail-webapp/currentMessage", ["message"]),
        ...mapState("mail", { currentEvent: state => state.consultPanel.currentEvent }),
        computeEventIcon() {
            let icon = "event";
            if (this.currentEvent.status) {
                switch (this.currentEvent.status) {
                    case "Accepted":
                        icon = "check";
                        break;
                    case "Tentative":
                        icon = "interrogation";
                        break;
                    case "Declined":
                        icon = "cross";
                        break;
                }
            }
            return icon;
        }
    },
    watch: {
        currentEvent() {
            this.selectedChoice = 0;
        }
    },
    methods: {
        ...mapActions("mail", ["SET_EVENT_STATUS"]),
        answer(status) {
            this.SET_EVENT_STATUS(status);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.ics-viewer {
    hr {
        border-color: $calendar-color;
        margin-bottom: $sp-1;
        max-width: 66%;
        margin-left: 0;
    }

    .header {
        .top {
            .fa-check {
                color: $green;
            }

            .fa-cross {
                color: $red;
            }

            .fa-interrogation {
                color: $purple;
            }
        }
    }

    .invitation {
        .bm-label-icon {
            color: $calendar-color;
            margin-left: #{-1rem - $sp-3};

            svg {
                margin-right: $sp-3;
            }
        }
    }
}
</style>
