import { BCalendar } from "bootstrap-vue";
import CalendarMixin from "../mixins/CalendarMixin";

export default {
    extends: BCalendar,
    name: "BmCalendar",
    mixins: [CalendarMixin]
};
