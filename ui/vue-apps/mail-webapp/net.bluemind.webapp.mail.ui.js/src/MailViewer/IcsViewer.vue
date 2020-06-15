<template>
    <div class="ics-viewer">
        <div class="header pl-3 pt-2 pb-3">
            <bm-label-icon icon="event" class="font-weight-bold mb-3 d-block">{{ $t("mail.ics") }}</bm-label-icon>
            <div v-if="currentEvent && message.ics.needsReply">
                <bm-button variant="simple" class="mr-2 px-1">
                    <bm-label-icon icon="check" icon-size="lg">{{ $t("common.accept") }}</bm-label-icon>
                </bm-button>
                <bm-button variant="simple" class="mr-2 px-1">
                    <bm-label-icon icon="interrogation" icon-size="lg">{{
                        $t("common.accept.temporarily")
                    }}</bm-label-icon>
                </bm-button>
                <bm-button variant="simple" class="px-1">
                    <bm-label-icon icon="cross" icon-size="lg">{{ $t("common.refuse") }}</bm-label-icon>
                </bm-button>
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
import { mapGetters, mapState } from "vuex";
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
            selectedChoice: 0
        };
    },
    computed: {
        ...mapGetters("mail-webapp/currentMessage", ["message"]),
        ...mapState("mail-webapp", ["currentEvent"]),
        attendeesLength() {
            return this.currentEvent.attendees.length;
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

        .btn,
        .btn:hover {
            background-color: $calendar-color;
            color: $white;
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
