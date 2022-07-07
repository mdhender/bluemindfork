<template>
    <div class="pref-automatic-reply-optional-date">
        <label :label-for="'date-picker' + _uid">{{ labels.main }}</label>
        <bm-form-radio-group v-model="hasDate" class="radio-options">
            <bm-form-radio :value="false" class="null-date" :disabled="disabled">{{ labels.nullDate }}</bm-form-radio>
            <div class="non-null-date">
                <bm-form-radio :value="true" :disabled="disabled">
                    <span class="sr-only">{{ labels.day }}</span>
                </bm-form-radio>
                <bm-form-date-picker
                    :id="'date-picker' + _uid"
                    v-model="dayObj"
                    :disabled="disabled"
                    :locale="userLang"
                    value-as-date
                    show-range
                    :min="minDayObj"
                    :initial-date="minDayObj"
                />
                <span>{{ $t("common.at_time") }}</span>
                <bm-form-time-picker id="time-picker" v-model="timeHM" :disabled="disabled || !dayObj">
                    <span class="sr-only">{{ labels.time }}</span>
                </bm-form-time-picker>
            </div>
        </bm-form-radio-group>
    </div>
</template>

<script>
import { BmFormDatePicker, BmFormRadio, BmFormRadioGroup, BmFormTimePicker } from "@bluemind/styleguide";

// The code in this file uses the following terminology:
//
// - Day = a day in the calendar, e.g. January 1st, 1970
// - Time = a time of the day with a minute precision, e.g. 10:30
// - Date = a day in the calendar + a time of this day
//
// - Str = a string representation of a date, e.g. "Thu Jan 01 1970 01:00:00 GMT+0100"
// - HM = a string representation of a time of the day, e.g. "10:30"
// - Obj = an instance of the JavaScript Date class
// - Num = a numeric representation of a date, as returned by Date.now() or by method Date.getTime

function setTimeFromHM(dateObj, timeHM) {
    const [h, m] = timeHM.split(":"); // i18n problem ?
    dateObj.setHours(h);
    dateObj.setMinutes(m);
}

function setTimeFromObj(dateObj, timeObj) {
    dateObj.setHours(timeObj.getHours());
    dateObj.setMinutes(timeObj.getMinutes());
}

export default {
    name: "PrefAutomaticReplyOptionalDate",
    components: {
        BmFormDatePicker,
        BmFormRadio,
        BmFormRadioGroup,
        BmFormTimePicker
    },
    props: {
        disabled: {
            type: Boolean,
            default: false
        },
        value: {
            type: Number,
            default: null
        },
        min: {
            type: Number,
            default: null
        },
        labels: {
            type: Object,
            required: true,
            validator: function (value) {
                return (
                    Object.keys(value).includes("main") &&
                    Object.keys(value).includes("nullDate") &&
                    Object.keys(value).includes("day") &&
                    Object.keys(value).includes("time")
                );
            }
        }
    },
    data: () => {
        return {
            bufferedDateNum: null,
            hasDate: false,
            defaultTimeHM: "08:00"
        };
    },
    computed: {
        storedDateNum() {
            return this.hasDate ? this.bufferedDateNum : null;
        },
        minDayObj() {
            return this.min ? new Date(this.min) : null;
        },
        dayObj: {
            get() {
                return this.bufferedDateNum ? new Date(this.bufferedDateNum) : null;
            },
            set(obj) {
                if (!obj) {
                    return;
                }
                const obj2 = new Date(obj);
                if (this.bufferedDateNum) {
                    setTimeFromObj(obj2, new Date(this.bufferedDateNum));
                } else {
                    setTimeFromHM(obj2, this.defaultTimeHM);
                }
                this.bufferedDateNum = obj2.getTime();
                this.hasDate = true;
            }
        },
        timeHM: {
            get() {
                return this.bufferedDateNum ? this.$d(new Date(this.bufferedDateNum), "short_time") : "";
            },
            set(HM) {
                if (!this.bufferedDateNum) {
                    return;
                }
                const dateObj = new Date(this.bufferedDateNum);
                setTimeFromHM(dateObj, HM);
                this.bufferedDateNum = dateObj.getTime();
                this.hasDate = true;
            }
        },
        userLang() {
            return this.$store.state.settings.lang;
        }
    },
    watch: {
        hasDate(value) {
            if (value && this.bufferedDateNum === null) {
                const dateObj = new Date(Date.now());
                setTimeFromHM(dateObj, this.defaultTimeHM);
                this.bufferedDateNum = dateObj.getTime();
            }
        },
        storedDateNum(num) {
            this.$emit("input", num);
        },
        min(num) {
            if (this.bufferedDateNum !== null && num !== null && this.bufferedDateNum < num) {
                this.bufferedDateNum = null;
                this.hasDate = false;
            }
        },
        value: {
            immediate: true,
            handler() {
                this.bufferedDateNum = this.value;
                this.hasDate = this.value !== null;
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-automatic-reply-optional-date {
    display: flex;
    flex-direction: column;

    .radio-options {
        display: flex;
        flex-direction: row;
        flex-wrap: wrap;
        align-items: center;

        .null-date {
            flex: none;
            width: 8rem;
            margin-right: $sp-4;
        }

        .non-null-date {
            display: flex;
            align-items: center;

            .bm-form-radio .custom-control-inline {
                margin-right: 0;
            }

            .b-form-datepicker {
                height: 1.75rem;
                width: 14rem;
                min-width: unset;
                flex: none;
                margin-right: $sp-2;
            }

            span {
                margin-right: $sp-2;
            }

            .bm-form-time-picker {
                height: 1.75rem;
                width: 4rem !important;
                min-width: unset;
                flex: none;
            }
        }

        .bm-form-input {
            width: unset !important;
        }
        .form-group {
            margin-bottom: 0 !important;
        }
    }
}
</style>
