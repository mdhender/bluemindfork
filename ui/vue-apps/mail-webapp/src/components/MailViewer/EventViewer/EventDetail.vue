<script setup>
import { computed } from "vue";
import { BmIcon, BmRow } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import EventCalendarIllustration from "./EventCalendarIllustration.vue";
import { formatEventDates } from "./formatEventDates";

const { MessageHeader } = messageUtils;

const props = defineProps({
    event: { type: Object, required: true },
    message: { type: Object, required: true },
    illustration: { type: String, default: "calendar" }
});

const eventValue = computed(() => props.event.serverEvent?.value?.main);
const eventTimeRange = computed(() => {
    const dtstart = eventValue.value?.dtstart;
    const dtend = eventValue.value?.dtend;
    const { startDate, endDate } = formatEventDates(dtstart, dtend);
    return endDate ? `${startDate} - ${endDate}` : startDate;
});

const hasHeader = header => props.message?.headers?.some(({ name }) => name.toUpperCase() === header.toUpperCase());
const calendarStatus = computed(() => {
    // TODO need a specific header to detect that an event has been edited
    // if (hasHeader(MessageHeader.X_BM_EVENT_XXX)) {
    //     return "edited";
    // }
    if (hasHeader(MessageHeader.X_BM_EVENT_REPLIED)) {
        return props.event.attendees?.find(attendee => attendee.mail === props.message.from.address)?.status ?? null;
    }
    return null;
});

const isRecurring = computed(() => Boolean(eventValue.value?.rrule));
</script>

<template>
    <div class="event-detail" :class="{ 'event-detail-recurring': isRecurring }">
        <event-calendar-illustration
            :status="calendarStatus"
            :date="eventValue?.dtstart.iso8601"
            :is-recurring="isRecurring"
        />
        <bm-row class="event-row-icon summary">
            <bm-icon icon="lock-fill" class="mr-2" />
            <h3>{{ event.summary }}</h3>
        </bm-row>
        <bm-row class="event-time title"> {{ eventTimeRange }} </bm-row>
        <bm-row v-if="isRecurring" class="event-row-icon occurence">
            <bm-icon icon="repeat" class="mr-2" />
            <span>{{ event.date }}</span>
        </bm-row>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-detail {
    display: grid;
    padding: 0 $sp-5 0px $sp-5;
    align-items: center;

    @include until-lg {
        $row-time-height: $sp-6 + $sp-3;
        $row-title-height: $sp-7 + $sp-5;
        padding: $sp-5;
        &.event-detail-recurring {
            grid-template-areas:
                "illustration  summary"
                "illustration  time"
                "occurrence  occurrence";
            .occurence {
                margin-left: 0;
            }
        }

        grid-template-columns: 75px 1fr;
        grid-template-rows: $row-title-height $row-time-height auto;
        .event-occurence {
            padding: 0 $sp-5 0 $sp-5;
        }
    }
    row-gap: $sp-4;
    column-gap: $sp-6;
    grid-template-areas:
        "illustration summary"
        "illustration time";

    @include from-lg {
        &.event-detail-recurring {
            grid-template-rows: repeat(3, auto);
            grid-template-areas:
                "illustration summary"
                "illustration time"
                "illustration occurrence";
        }
    }

    grid-template-columns: 120px 1fr;
    grid-template-rows: repeat(2, auto);
    .event-calendar-illustration {
        grid-area: illustration;
        justify-self: center;
        align-self: center;
    }
    .event-row-icon {
        display: flex;
        align-items: baseline;
        flex-wrap: nowrap;
        gap: $sp-4;
        line-height: $line-height;
        & > svg {
            translate: 0 $sp-2;
        }
    }
    .summary {
        grid-area: summary;
        color: $primary-fg-hi1;
        padding-top: $sp-3;
        & > h3 {
            margin-bottom: 0;
            overflow: hidden;
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
        }
    }
    .event-time.title {
        margin-bottom: 0;
    }
    .occurence {
        grid-area: occurrence;
        padding-bottom: $sp-3;
    }
}
</style>
