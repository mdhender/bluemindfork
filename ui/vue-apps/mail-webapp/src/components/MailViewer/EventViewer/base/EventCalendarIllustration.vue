<script setup>
import { computed } from "vue";
import { BmIcon, BmResponsiveIllustration } from "@bluemind/ui-components";
import { messageUtils } from "@bluemind/mail";
import { REPLY_ACTIONS } from "../replyActions";

const { MessageHeader } = messageUtils;

const props = defineProps({
    date: { type: String, default: null },
    isRecurring: { type: Boolean, default: false },
    onlyOccurrence: { type: Boolean, default: false },
    status: {
        type: String,
        default: null,
        validator: value =>
            !value ||
            [
                "edited",
                "countered",
                REPLY_ACTIONS.ACCEPTED,
                REPLY_ACTIONS.TENTATIVE,
                REPLY_ACTIONS.DECLINED,
                REPLY_ACTIONS.NEEDS_ACTION
            ].includes(value)
    }
});

const iconStatus = {
    edited: "pencil",
    countered: "interrogation",
    [REPLY_ACTIONS.ACCEPTED]: "check",
    [REPLY_ACTIONS.TENTATIVE]: "interrogation",
    [REPLY_ACTIONS.DECLINED]: "cross",
    [REPLY_ACTIONS.NEEDS_ACTION]: "interrogation"
};

const dateStart = computed(() => (props.date ? new Date(props.date) : null));
const repeatIcon = computed(() => {
    let exceptionIconSuffix = "";
    if (props.onlyOccurrence) {
        exceptionIconSuffix = "-exclamation";
    }
    return "repeat" + exceptionIconSuffix;
});
</script>

<template>
    <div class="event-calendar-illustration">
        <bm-responsive-illustration over-background value="calendar" />
        <div v-if="dateStart" class="event-calendar-illustration-text">
            <div class="event-calendar-illustration-year">{{ $d(dateStart, "year") }}</div>
            <div>
                <div class="event-calendar-illustration-weekday">{{ $d(dateStart, "short_weekday") }}</div>
                <div class="event-calendar-illustration-day">{{ dateStart.getDate() }}</div>
                <div class="event-calendar-illustration-month">{{ $d(dateStart, "short_month") }}</div>
            </div>
            <bm-icon
                v-if="status"
                :icon="iconStatus[status]"
                class="event-calendar-illustration-icon-status"
                :class="{
                    'event-calendar-illustration-countered': ['countered', REPLY_ACTIONS.NEEDS_ACTION].includes(status)
                }"
            />
            <bm-icon
                v-if="isRecurring || onlyOccurrence"
                :icon="repeatIcon"
                class="event-calendar-illustration-icon-repeat"
            />
        </div>
    </div>
</template>

<style lang="scss">
@use "sass:map";
@import "~@bluemind/ui-components/src/css/utils/variables";
@import "~@bluemind/ui-components/src/css/utils/responsiveness";
@import "~@bluemind/ui-components/src/css/utils/typography";

.event-calendar-illustration {
    display: flex;
    align-items: center;
    position: relative;

    @include from-lg {
        margin: 0 $sp-5;
    }

    .event-calendar-illustration-text {
        position: absolute;
        top: 0;
        text-align: center;
        display: flex;
        flex-direction: column;
        color: $neutral-fg;

        @include until-lg {
            gap: 3px;
            left: 0;
            right: 0;
            padding-top: 9px;
        }
        @include from-lg {
            gap: 7px;
            left: 10px;
            right: 10px;
            padding-top: 11px;
        }

        .event-calendar-illustration-year,
        .event-calendar-illustration-weekday,
        .event-calendar-illustration-month {
            @include until-lg {
                @include caption-bold;
            }
            @include from-lg {
                @include bold;
            }
        }

        .event-calendar-illustration-year {
            color: $lightest;
        }

        .event-calendar-illustration-day {
            @include until-lg {
                font-size: 28px;
                line-height: 24px;
            }
            @include from-lg {
                font-size: 32px;
                line-height: 28px;
            }
            font-weight: 400;
        }

        .event-calendar-illustration-icon-status,
        .event-calendar-illustration-icon-repeat {
            position: absolute;
            bottom: 0;

            @include until-lg {
                $size: map-get($icon-sizes, "sm");
                width: $size;
                height: $size;

                &.event-calendar-illustration-icon-status {
                    left: 4px;
                    bottom: -1px;
                }
                &.event-calendar-illustration-icon-repeat {
                    right: 3px;
                    bottom: -2px;
                }
            }
            @include from-lg {
                $size: map-get($icon-sizes, "md");
                width: $size;
                height: $size;
                &.event-calendar-illustration-icon-status {
                    left: 7px;
                    bottom: -1px;
                }
                &.event-calendar-illustration-icon-repeat {
                    right: 6px;
                    bottom: -1px;
                }
            }
        }

        .event-calendar-illustration-icon-repeat {
            color: $neutral-fg;
        }

        .event-calendar-illustration-icon-status.bm-icon {
            &.icon-pencil {
                color: $neutral-fg-lo1;
            }
            &.icon-check {
                color: $success-fg;
            }
            &.icon-interrogation {
                color: $warning-fg;

                &.event-calendar-illustration-countered {
                    color: $info-fg;
                }
            }
            &.icon-cross {
                color: $danger-fg;
            }
        }
    }
}
</style>
