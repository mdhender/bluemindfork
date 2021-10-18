import CentralizedSaving from "./CentralizedSaving";

export default {
    props: {
        setting: {
            type: String,
            required: true
        },
        default: {
            type: String,
            required: false,
            default: undefined
        },
        needReload: {
            type: Boolean,
            required: false,
            default: false
        },
        needLogout: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    mixins: [CentralizedSaving],
    created() {
        const save = async ({ state: { current } }) => {
            if (current && !current.options.saved) {
                await this.$store.dispatch("session/SAVE_SETTING", { setting: this.setting, value: current.value });
                this.PUSH_STATE({
                    value: current.value,
                    options: { saved: true, reload: this.needReload, logout: this.needLogout }
                });
            }
        };
        this.registerSaveAction(save);
        const value = this.$store.state.session.settings.remote[this.setting];
        this.value = value !== undefined ? value : this.default;
    }
};
