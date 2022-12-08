import BmCalendarDecorator from "../components/BmCalendarDecorator";
import BmIcon from "../components/BmIcon";

export default {
    props: {
        hideHeader: {
            type: Boolean,
            required: false,
            default: true
        },
        labelHelp: {
            type: String,
            required: false,
            default: ""
        },
        todayVariant: {
            type: String,
            required: false,
            default: "primary"
        },
        dateInfoFn: {
            type: [Object, Function],
            required: false,
            default: function () {
                // Tricky : dateInfoFn is evaluated in the BCalendar component. We need "this" to match the component
                // whith extended by CalendarMixin. By setting dateInfoFn type to Object, the default value is a function
                // returning the real default value and executed by the component declaring the props.
                return decorator.bind(this);
            }
        },
        showRange: {
            type: Boolean,
            required: false,
            default: false
        },
        placeholder: {
            type: String,
            default() {
                return this.$t("common.date");
            }
        }
    },
    data() {
        return {
            $_BmCalendarMixin_calendar: null,
            $_BmCalendarMixin_lock: false
        };
    },
    created() {
        this.$slots["nav-prev-month"] = this.$createElement(BmIcon, { props: { icon: "chevron-left" } });
        this.$slots["nav-next-month"] = this.$createElement(BmIcon, { props: { icon: "chevron-right" } });
        this.$slots["button-content"] = this.$createElement(BmIcon, { props: { icon: "calendar" } });
        this.$on("context", this.decorateCalendar);
    },
    mounted() {
        this.$_BmCalendarMixin_calendar = this.$refs["calendar"] || this;
        if (this.$refs["control"]) {
            this.$refs["control"].$on("shown", () => {
                this.$refs["control"].$root.$off("bv::dropdown::shown", this.$refs["control"].rootCloseListener);
            });
        }
    },
    updated() {
        if (!this.$_BmCalendarMixin_lock) {
            this.$_BmCalendarMixin_lock = true;
            this.decorateCalendar();
            this.$slots["button-content"] = this.$createElement(BmIcon, { props: { icon: "calendar" } });
            this.$nextTick(() => (this.$_BmCalendarMixin_lock = false));
        }
    },
    methods: {
        decorateCalendar() {
            this.$_BmCalendarMixin_calendar.$slots.default = this.$createElement(BmCalendarDecorator, {
                props: {
                    date: this.$_BmCalendarMixin_calendar.calendarFirstDay,
                    rows: this.$_BmCalendarMixin_calendar.calendar.length,
                    min: this.min,
                    max: this.max
                },
                on: {
                    goTo: year => {
                        const date = new Date(this.$_BmCalendarMixin_calendar.activeYMD);
                        date.setFullYear(year);
                        this.$_BmCalendarMixin_calendar.activeYMD = formatYMD(date);
                    }
                }
            });
            this.$_BmCalendarMixin_calendar.$forceUpdate();
        }
    }
};

function decorator(d) {
    const calendar = this.$_BmCalendarMixin_calendar;
    if (!calendar) {
        return;
    }
    const date = new Date(d);
    const classes = [];
    if (isWeekEnd(date)) {
        classes.push("bm-calendar-date-weekend");
    }
    const initialDate = calendar.initialDate && formatYMD(calendar.initialDate);
    if (initialDate === d) {
        classes.push("bm-calendar-date-initial");
    } else if (d === calendar.selectedYMD) {
        classes.push("bm-calendar-date-selected");
    } else {
        classes.push("bm-calendar-date-standard");
    }
    if (this.showRange && initialDate) {
        const side = inRange(d, initialDate, calendar.selectedYMD);
        if (side !== 0) {
            classes.push("bm-calendar-date-range");
            classes.push(side > 0 ? "right" : "left");
        }
    }
    return classes;
}
function isWeekEnd(date) {
    return date.getDay() % 6 === 0;
}
function formatYMD(date) {
    const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
    return d.toISOString().split("T").shift();
}
function inRange(date, bound, floating) {
    if (bound && floating) {
        if (date === bound && date === floating) {
            return 0;
        }
        if (date >= bound && date <= floating) {
            return 1;
        }
        if (date <= bound && date >= floating) {
            return -1;
        }
    }
    return 0;
}
