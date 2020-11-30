<template>
    <div class="event-viewer">
        <bm-choice-group
            class="border-bottom my-3"
            :options="choices"
            :selected="choices[selectedChoice]"
            @select="index => (selectedChoice = index)"
        />
        <parts-viewer v-if="selectedChoice === 0" :message-key="message.key" />
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
            <template v-if="currentEvent.sanitizedDescription">
                <hr />
                <div>
                    <bm-label-icon icon="pencil" class="font-weight-bold d-block">
                        {{ $t("common.description") }}
                    </bm-label-icon>
                    <!-- eslint-disable-next-line vue/no-v-html -->
                    <span v-html="currentEvent.sanitizedDescription" />
                </div>
            </template>
        </div>
    </div>
</template>
<script>
import { BmChoiceGroup, BmLabelIcon } from "@bluemind/styleguide";
import PartsViewer from "./PartsViewer/PartsViewer";

export default {
    name: "EventViewer",
    components: {
        BmChoiceGroup,
        BmLabelIcon,
        PartsViewer
    },
    props: {
        message: {
            type: Object,
            required: true
        },
        currentEvent: {
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
            selectedChoice: 0
        };
    },
    watch: {
        currentEvent() {
            this.selectedChoice = 0;
        }
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
