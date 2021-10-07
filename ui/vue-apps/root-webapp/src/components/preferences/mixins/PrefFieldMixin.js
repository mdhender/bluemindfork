export default {
    props: {
        localUserSettings: {
            type: Object,
            required: false
        },
        setting: {
            type: String,
            required: false
        },
        options: {
            type: Object,
            required: true
        },
        disabled: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    created() {
        if (this.options.autosave) {
            this.$watch(
                () => this.localUserSettings[this.setting],
                () => this.$emit("requestSave")
            );
        }
    }
};
