<template>
    <div class="ics-viewer">
        <div class="header px-3 pt-2 pb-3">
            <bm-label-icon icon="event" class="font-weight-bold mb-1 d-block">
                <template v-if="currentEvent.status === 'NeedsAction'">{{ $t("mail.ics") }}</template>
                <template v-else>{{ $t("mail.ics.already_answered") }}</template>
            </bm-label-icon>
            <div v-if="currentEvent && message.ics.needsReply">
                <div v-if="currentEvent.status === 'NeedsAction' || wantToEdit" class="needs-action">
                    <bm-button variant="simple" class="mr-2 px-1" @click="answer('Accepted')">
                        <bm-label-icon icon="check" icon-size="lg">{{ $t("common.accept") }}</bm-label-icon>
                    </bm-button>
                    <bm-button variant="simple" class="mr-2 px-1" @click="answer('Tentative')">
                        <bm-label-icon icon="interrogation" icon-size="lg">{{
                            $t("common.accept.temporarily")
                        }}</bm-label-icon>
                    </bm-button>
                    <bm-button variant="simple" class="px-1" @click="answer('Declined')">
                        <bm-label-icon icon="cross" icon-size="lg">{{ $t("common.refuse") }}</bm-label-icon>
                    </bm-button>
                </div>
                <div v-else class="answered d-flex pl-4">
                    <bm-label-icon v-if="currentEvent.status === 'Accepted'" icon="check" icon-size="lg">
                        {{ $t("mail.ics.accepted") }}
                    </bm-label-icon>
                    <bm-label-icon v-else-if="currentEvent.status === 'Tentative'" icon="interrogation" icon-size="lg">
                        {{ $t("mail.ics.accepted.temporarily") }}
                    </bm-label-icon>
                    <bm-label-icon v-else-if="currentEvent.status === 'Declined'" icon="cross" icon-size="lg">
                        {{ $t("mail.ics.refused") }}
                    </bm-label-icon>
                    <bm-button variant="outline-secondary" @click="wantToEdit = true">
                        {{ $t("common.edit.answer") }}
                    </bm-button>
                </div>
            </div>
        </div>
        <bm-choice-group
            class="border-bottom my-3"
            :options="choices"
            :selected="choices[selectedChoice].value"
            @select="index => (selectedChoice = index)"
        />
        <parts-viewer v-if="selectedChoice === 0" class="message" />
        <div v-else class="invitation pl-5 d-inline-block">
            <h1>{{ currentEvent.organizer.name }} {{ $t("mail.ics.got_invited") }}</h1>
            <h1 class="font-weight-bold">&laquo;{{ currentEvent.summary }}&raquo;</h1>
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
            <bm-label-icon :icon="attendeesLength > 1 ? 'group' : 'user'" class="font-weight-bold d-block">{{
                $tc("common.attendees", attendeesLength)
            }}</bm-label-icon>
            <div v-for="attendee in currentEvent.attendees" :key="attendee.mail">
                <span class="font-weight-bold">{{ attendee.name }}</span> &lt;{{ attendee.mail }}&gt;
            </div>
        </div>
    </div>
</template>

<script>
import { mapActions, mapGetters, mapState } from "vuex";
import { BmButton, BmChoiceGroup, BmLabelIcon } from "@bluemind/styleguide";
import PartsViewer from "./PartsViewer/PartsViewer";

export default {
    name: "IcsViewer",
    components: {
        BmButton,
        BmChoiceGroup,
        BmLabelIcon,
        PartsViewer
    },
    data() {
        return {
            choices: [
                { text: this.$t("common.message"), value: "message" },
                { text: this.$t("common.invitation"), value: "invitation" }
            ],
            selectedChoice: 0,
            wantToEdit: false
        };
    },
    computed: {
        ...mapGetters("mail-webapp/currentMessage", ["message"]),
        ...mapState("mail-webapp", ["currentEvent"]),
        attendeesLength() {
            return this.currentEvent.attendees.length;
        }
    },
    methods: {
        ...mapActions("mail-webapp", ["setEventStatus"]),
        answer(status) {
            this.setEventStatus(status);
            this.wantToEdit = false;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.ics-viewer {
    hr {
        background-color: $calendar-color;
        margin-bottom: $sp-1;
    }

    .header {
        background-color: $calendar-light-color;
        .needs-action {
            .btn,
            .btn:hover {
                background-color: $calendar-color;
                color: $white;
            }
        }
        .answered {
            .fa-check {
                color: $green;
            }

            .fa-cross {
                color: $red;
            }

            .bm-label-icon {
                margin-right: auto;
                align-self: center;
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
