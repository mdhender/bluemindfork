<template>
    <bm-form-timepicker v-model="time" minutes-step="30" :locale="locale" hide-header @input="onInput" />
</template>

<script>
import { mapState } from "vuex";
import { BmFormTimepicker } from "@bluemind/styleguide";
import PrefFieldMixin from "../../mixins/PrefFieldMixin";

export default {
    name: "PrefWorksHours",
    components: { BmFormTimepicker },
    mixins: [PrefFieldMixin],
    data() {
        return { time: "" };
    },

    computed: {
        ...mapState("session", { locale: ({ settings }) => settings.remote.lang })
    },
    watch: {
        localUserSettings(value, oldValue) {
            if (value[this.setting] !== oldValue[this.setting]) {
                this.initTime();
            }
        }
    },
    mounted() {
        this.initTime();
    },
    methods: {
        onInput(newTime) {
            const newOption = worksHoursOptions.find(opt => opt.text === newTime);
            if (newOption) {
                this.localUserSettings[this.setting] = newOption.value;
            } else {
                this.initTime();
            }
        },
        initTime() {
            this.time = worksHoursOptions.find(opt => opt.value === this.localUserSettings[this.setting]).text;
        }
    }
};

const worksHoursOptions = [
    { text: "00:00:00", value: "0" },
    { text: "00:30:00", value: "0.5" },
    { text: "01:00:00", value: "1" },
    { text: "01:30:00", value: "1.5" },
    { text: "02:00:00", value: "2" },
    { text: "02:30:00", value: "2.5" },
    { text: "03:00:00", value: "3" },
    { text: "03:30:00", value: "3.5" },
    { text: "04:00:00", value: "4" },
    { text: "04:30:00", value: "4.5" },
    { text: "05:00:00", value: "5" },
    { text: "05:30:00", value: "5.5" },
    { text: "06:00:00", value: "6" },
    { text: "06:30:00", value: "6.5" },
    { text: "07:00:00", value: "7" },
    { text: "07:30:00", value: "7.5" },
    { text: "08:00:00", value: "8" },
    { text: "08:30:00", value: "8.5" },
    { text: "09:00:00", value: "9" },
    { text: "09:30:00", value: "9.5" },
    { text: "10:00:00", value: "10" },
    { text: "10:30:00", value: "10.5" },
    { text: "11:00:00", value: "11" },
    { text: "11:30:00", value: "11.5" },
    { text: "12:00:00", value: "12" },
    { text: "12:30:00", value: "12.5" },
    { text: "13:00:00", value: "13" },
    { text: "13:30:00", value: "13.5" },
    { text: "14:00:00", value: "14" },
    { text: "14:30:00", value: "14.5" },
    { text: "15:00:00", value: "15" },
    { text: "15:30:00", value: "15.5" },
    { text: "16:00:00", value: "16" },
    { text: "16:30:00", value: "16.5" },
    { text: "17:00:00", value: "17" },
    { text: "17:30:00", value: "17.5" },
    { text: "18:00:00", value: "18" },
    { text: "18:30:00", value: "18.5" },
    { text: "19:00:00", value: "19" },
    { text: "19:30:00", value: "19.5" },
    { text: "20:00:00", value: "20" },
    { text: "20:30:00", value: "20.5" },
    { text: "21:00:00", value: "21" },
    { text: "21:30:00", value: "21.5" },
    { text: "22:00:00", value: "22" },
    { text: "22:30:00", value: "22.5" },
    { text: "23:00:00", value: "23" },
    { text: "23:30:00", value: "23.5" }
];
</script>
