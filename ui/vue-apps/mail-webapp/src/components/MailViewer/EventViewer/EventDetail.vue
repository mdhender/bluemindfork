<script setup>
import { computed } from "vue";
import { BmIcon, BmRow } from "@bluemind/ui-components";
import EventHelper from "~/store/helpers/EventHelper";
import EventCalendarIllustration from "./EventCalendarIllustration.vue";

const props = defineProps({
    event: { type: Object, required: true },
    illustration: { type: String, default: "calendar" }
});

const eventTimeRange = computed(() => {
    const dtstart = props.event.serverEvent?.value?.main?.dtstart;
    const dtend = props.event.serverEvent?.value?.main?.dtend;
    const { startDate, endDate } = EventHelper.adaptRangeDate(dtstart, dtend);
    return {
        start: startDate,
        end: endDate
    };
});
</script>

<template>
    <div class="event-detail">
        <event-calendar-illustration :illustration="illustration" />
        <bm-row class="event-row-icon summary">
            <bm-icon icon="lock-fill" class="mr-2" />
            <h3>{{ event.summary }}</h3>
        </bm-row>
        <bm-row class="event-time title"> {{ eventTimeRange.start }} - {{ eventTimeRange.end }} </bm-row>
        <bm-row class="event-row-icon occurence">
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
    @include until-lg {
        $row-time-height: $sp-6 + $sp-3;
        $row-title-height: $sp-7 + $sp-5;
        padding: $sp-5;
        grid-template-areas:
            "illustration  summary"
            "illustration  time"
            "occurrence  occurrence";
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
        "illustration time"
        "illustration occurrence";
    grid-template-columns: 120px 1fr;
    grid-template-rows: repeat(3, auto);
    .event-calendar-illustration {
        grid-area: illustration;
        justify-self: center;
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
