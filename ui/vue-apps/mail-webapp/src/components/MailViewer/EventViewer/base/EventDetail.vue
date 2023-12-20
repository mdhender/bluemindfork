<script setup>
import { computed } from "vue";
import { BmIcon } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import EventHelper from "~/store/helpers/EventHelper";
import EventCalendarIllustration from "./EventCalendarIllustration.vue";
import { formatEventDates } from "../formatEventDates";
import { REPLY_ACTIONS } from "../replyActions";
const { MessageHeader } = messageUtils;

const props = defineProps({
    event: { type: Object, required: true },
    message: { type: Object, required: true }
});

const eventValue = computed(
    () => props.event.serverEvent && EventHelper.eventInfos(props.event.serverEvent, props.event.recuridIsoDate)
);

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

    if (props.event?.status && hasHeader(MessageHeader.X_BM_EVENT)) {
        return props.event.status !== REPLY_ACTIONS.NEEDS_ACTION ? props.event.status : null;
    }

    if (props.event?.attendees?.length && hasHeader(MessageHeader.X_BM_EVENT_REPLIED)) {
        return props.event.attendees.find(attendee => attendee.mail === props.message.from.address)?.status ?? null;
    }

    if (props.event?.private) {
        return "private";
    }

    return null;
});
const isReply = computed(() => hasHeader(MessageHeader.X_BM_EVENT_REPLIED));
const isRecurring = computed(() => Boolean(props.event.serverEvent?.value?.main?.rrule));
const withDetails = computed(() => {
    if (isReply.value) {
        return false;
    }
    if (isRecurring.value) {
        if (props.event.recuridIsoDate) {
            return Boolean(eventValue.value?.location) || Boolean(eventValue.value?.url);
        }
        return true;
    }
    return Boolean(eventValue.value?.location) || Boolean(eventValue.value?.url);
});
</script>

<template>
    <div class="event-detail" :class="{ 'with-details': withDetails, 'no-summary': !event.attendee }">
        <event-calendar-illustration
            :status="calendarStatus"
            :date="eventValue?.counter?.dtstart.iso8601 ?? eventValue?.dtstart.iso8601"
            :is-recurring="isRecurring"
            :only-occurrence="Boolean(event.recuridIsoDate)"
        />
        <div v-if="event.attendee" class="event-row-icon summary">
            <bm-icon v-if="event.private" icon="lock-fill" />
            <h3>{{ event.summary }}</h3>
        </div>
        <div class="event-time title" :class="{ 'no-summary': !event.attendee }">
            <span :class="{ 'event-time-current regular': counterTimeRange }">
                {{ eventTimeRange }}
            </span>

            <div v-if="counterTimeRange" class="event-time-counter">
                <bm-icon icon="forward" size="xs" />
                <span>
                    {{ counterTimeRange }}
                </span>
            </div>
        </div>
        <div v-if="withDetails" class="details d-flex flex-column">
            <div v-if="isRecurring && !event.recuridIsoDate" class="event-row-icon occurence">
                <bm-icon icon="repeat" />
                <span>{{ event.date }}</span>
            </div>
            <div v-if="!isReply && event.location" class="event-row-icon">
                <bm-icon icon="location" />
                <span>{{ eventValue.location }}</span>
            </div>
            <div v-if="!isReply && event.url" class="event-row-icon">
                <bm-icon icon="link" />
                <a class="event-link" :href="eventValue.url" target="_blank">{{ eventValue.url }}</a>
            </div>
        </div>
    </div>
</template>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/text";

.event-detail {
    padding: 0 $sp-5;

    padding-top: $sp-5;
    @include from-lg {
        padding-top: $sp-4;
    }

    display: grid;
    align-items: center;
    row-gap: $sp-5;
    @include from-lg {
        row-gap: $sp-4;
    }
    column-gap: $sp-6;
    grid-template-areas:
        "illustration summary"
        "illustration time";

    &.no-summary {
        grid-template-areas: "illustration time";
    }

    @include until-lg {
        $row-time-height: $sp-6 + $sp-3;
        $row-title-height: $sp-7 + $sp-5;
        &.with-details {
            grid-template-areas:
                "illustration summary"
                "illustration time"
                "details details";
            &.no-summary {
                grid-template-areas:
                    "illustration time"
                    "details details";
            }
            margin-left: 0;
        }

        grid-template-columns: map-get($illustration-widths, "xxs") minmax(0, 1fr);
    }

    @include from-lg {
        &.with-details {
            grid-template-areas:
                "illustration summary"
                "illustration time"
                "illustration details";
            &.no-summary {
                grid-template-areas:
                    "illustration time"
                    "details details";
            }
        }
    }

    grid-template-columns: calc(#{map-get($illustration-widths, "xs")} + #{2 * $sp-5}) minmax(0, 1fr);
    .event-calendar-illustration {
        grid-area: illustration;
        justify-self: center;
        align-self: center;
    }
    .event-row-icon {
        display: flex;
        align-items: start;
        flex-wrap: nowrap;
        gap: $sp-4;
        line-height: $line-height;
        width: 100%;
    }
    a.event-link {
        color: $neutral-fg;
        &:hover {
            color: $neutral-fg-hi1;
        }
        @include text-overflow;
    }
    .summary {
        grid-area: summary;
        align-self: end;
        color: $primary-fg;
        padding-top: $sp-3;
        & > h3 {
            margin-bottom: 0;
            overflow: hidden;
            display: -webkit-box;
            -webkit-line-clamp: 4;
            -webkit-box-orient: vertical;
        }
    }
    .event-time.title {
        grid-area: time;
        align-self: start;
        &.no-summary {
            align-self: center;
        }
        margin-bottom: 0;

        @include until-lg {
            margin-top: $sp-4 - $sp-5; // negative margin intentional
        }

        display: flex;
        gap: $sp-2 $sp-3;
        @include from-lg {
            gap: $sp-3 $sp-4;
        }
        align-items: baseline;
        flex-wrap: wrap;

        .event-time-current {
            color: $neutral-fg-lo1;
        }

        .event-time-counter {
            display: flex;
            gap: $sp-1 + $sp-3;
            @include from-lg {
                gap: $sp-2 + $sp-4;
            }
            align-items: baseline;
            > .bm-icon {
                color: $neutral-fg-lo1;
                position: relative;
                bottom: base-px-to-rem(-1);
            }
            > span {
                margin-left: $sp-1;
                color: $info-fg-hi1;
            }
        }
    }
    .details {
        grid-area: details;
        @include from-lg {
            padding-bottom: $sp-3;
        }
        gap: $sp-4;
        @include until-lg {
            gap: $sp-5;
        }
    }
}
</style>
