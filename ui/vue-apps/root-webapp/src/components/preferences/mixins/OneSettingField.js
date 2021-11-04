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
        },
        needReload: {
            type: Boolean,
            default: false
        },
        needLogout: {
            type: Boolean,
            default: false
        }
    },
    mixins: [CentralizedSaving],
    created() {
        const save = async ({ state: { current } }) =>
            this.$store.dispatch("session/SAVE_SETTING", { setting: this.setting, value: current.value });
        this.registerSaveAction(save);
        const value = this.$store.state.session.settings.remote[this.setting];
        this.value = value !== undefined ? value : this.default;
    }
};
