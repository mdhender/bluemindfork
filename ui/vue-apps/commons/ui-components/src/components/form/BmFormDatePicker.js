import { BFormDatepicker } from "bootstrap-vue";
import CalendarMixin from "../../mixins/CalendarMixin";

export default {
    name: "BmFormDatePicker",
    extends: BFormDatepicker,
    mixins: [CalendarMixin]
};
