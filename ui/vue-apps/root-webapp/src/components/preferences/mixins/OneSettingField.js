import CentralizedSaving from "./CentralizedSaving";

export default {
    props: {
        setting: {
            type: String,
            required: true
        },
        default: {
            type: String,
            default: undefined
        }
    },
    mixins: [CentralizedSaving],
    created() {
        const save = async ({ state: { current } }) =>
            this.$store.dispatch("settings/SAVE_SETTING", { setting: this.setting, value: current.value });
        this.registerSaveAction(save);
        const value = this.$store.state.settings[this.setting];
        this.value = value !== undefined ? value : this.default;
    }
};
