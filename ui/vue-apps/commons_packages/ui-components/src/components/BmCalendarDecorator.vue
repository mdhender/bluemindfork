<template>
    <div>
        <header class="bm-calendar-caption d-flex" aria-hidden="true">
            <span class="mr-2">{{ currentMonth }}</span>
            <bm-form-select v-if="showSelector" v-model="year" :options="years" variant="inline" scrollbar />
            <div v-else>{{ year }}</div>
        </header>
        <div class="bm-calendar-weeknum">
            <div class="row no-gutters bm-calendar-weekdays-spacer"></div>
            <div v-for="week in weeks" :key="week" class="row no-gutters">
                <span>{{ week }}</span>
            </div>
        </div>
    </div>
</template>

<script>
import BmFormSelect from "./form/BmFormSelect";

export default {
    name: "BmCalendarDecorator",
    components: { BmFormSelect },
    props: {
        date: {
            type: Date,
            required: true
        },
        min: {
            type: Date,
            required: false,
            default: null
        },
        max: {
            type: Date,
            required: false,
            default: null
        },
        rows: {
            type: Number,
            required: true
        }
    },
    computed: {
        weeks() {
            let num = getWeekNumber(this.date);
            return Array(this.rows)
                .fill(0)
                .map((v, k) => num + k);
        },
        showSelector() {
            return !this.min || !this.max || this.min.getFullYear() !== this.max.getFullYear();
        },
        years() {
            const min = this.min?.getFullYear() || this.year - 100;
            const max = this.max?.getFullYear() + 1 || this.year + 100;
            return Array(max - min)
                .fill(0)
                .map((v, k) => ({ value: min + k, text: min + k }));
        },
        currentMonth() {
            return this.$d(this.date, "month");
        },
        year: {
            get() {
                return this.date.getFullYear();
            },
            set(value) {
                this.$emit("goTo", value);
            }
        }
    }
};
function getWeekNumber(date) {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    const dayNum = d.getUTCDay() || 7;
    d.setUTCDate(d.getUTCDate() + 4 - dayNum);
    const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
    return Math.ceil(((d - yearStart) / 86400000 + 1) / 7);
}
</script>
