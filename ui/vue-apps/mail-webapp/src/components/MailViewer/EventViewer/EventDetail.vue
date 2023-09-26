<script setup>
import { computed } from "vue";
import { BmIcon, BmLabelIcon } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import EventCalendarIllustration from "./EventCalendarIllustration.vue";
import { formatEventDates } from "./formatEventDates";

const { MessageHeader } = messageUtils;

const props = defineProps({
    event: { type: Object, required: true },
    message: { type: Object, required: true }
});

const eventValue = computed(() => props.event.serverEvent?.value?.main);
const eventTimeRange = computed(() => {
    const dtstart = eventValue.value?.dtstart;
    const dtend = eventValue.value?.dtend;
    const { startDate, endDate } = formatEventDates(dtstart, dtend, [
        props.event.counter?.dtstart,
        props.event.counter?.dtend
    ]);
    return endDate ? `${startDate} - ${endDate}` : startDate;
});
const counterTimeRange = computed(() => {
    const dtstart = props.event.counter?.dtstart;
    const dtend = props.event.counter?.dtend;
    const { startDate, endDate } = formatEventDates(dtstart, dtend, [
        eventValue.value?.dtstart,
        eventValue.value?.dtend
    ]);
    return endDate ? `${startDate} - ${endDate}` : startDate;
});

const hasHeader = header => props.message?.headers?.some(({ name }) => name.toUpperCase() === header.toUpperCase());
const calendarStatus = computed(() => {
    if (counterTimeRange.value) {
        return "countered";
    }
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
const withDetails = computed(() => !!(isRecurring.value || eventValue.value?.location || eventValue.value?.url));
</script>

<template>
    <div class="event-detail" :class="{ 'with-details': withDetails }">
        <event-calendar-illustration
            :status="calendarStatus"
            :date="event.counter?.dtstart.iso8601 ?? eventValue?.dtstart.iso8601"
            :is-recurring="isRecurring"
        />
        <div class="event-row-icon summary">
            <bm-icon v-if="event.private" icon="lock-fill" class="mr-2" />
            <h3>{{ event.summary }}</h3>
        </div>
        <div class="event-time title">
            <span :class="{ 'event-time-current': counterTimeRange }">
                {{ eventTimeRange }}
            </span>

            <bm-icon v-if="counterTimeRange" icon="chevron-right" size="xs" />
            <span v-if="counterTimeRange" class="event-time-counter">
                {{ counterTimeRange }}
            </span>
        </div>
        <div v-if="withDetails" class="details d-flex flex-column">
            <div v-if="isRecurring" class="event-row-icon">
                <bm-icon icon="repeat" class="mr-2" />
                <span>{{ event.date }}</span>
            </div>
            <div v-if="event.location" class="event-row-icon">
                <bm-icon icon="location" class="mr-2" />
                <span>{{ event.location }}</span>
            </div>
            <div v-if="event.url" class="event-row-icon">
                <bm-icon icon="link" class="mr-2" />
                <span>{{ event.url }}</span>
            </div>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";

.event-detail {
    display: grid;
    padding: 0 $sp-5 $sp-5 $sp-5;
    align-items: center;

    @include until-lg {
        $row-time-height: $sp-6 + $sp-3;
        $row-title-height: $sp-7 + $sp-5;
        padding: $sp-5;
        &.with-details {
            grid-template-areas:
                "illustration  summary"
                "illustration  time"
                "details  details";
            margin-left: 0;
            gap: $sp-5;
        }

        grid-template-columns: 75px 1fr;
        grid-template-rows: $row-title-height $row-time-height auto;
    }
    row-gap: $sp-4;
    column-gap: $sp-6;
    grid-template-areas:
        "illustration summary"
        "illustration time";

    @include from-lg {
        &.with-details {
            grid-template-rows: repeat(3, auto);
            grid-template-areas:
                "illustration summary"
                "illustration time"
                "illustration details";
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
        width: 100%;
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

        display: flex;
        gap: $sp-4;
        align-items: center;

        .event-time-current {
            font-size: 13px;
            font-weight: 400;
            line-height: 16px;
        }

        .event-time-counter {
            margin-left: $sp-1;
            color: $info-fg-hi1;
        }
    }
    .details {
        grid-area: details;
        padding-bottom: $sp-3;
        gap: $sp-4;
        @include until-lg {
            margin-top: $sp-2;
        }
    }
}
</style>
