import CentralizedSaving from "./CentralizedSaving";

export default {
    props: {
        settings: {
            type: Array,
            required: true
        },
        defaults: {
            type: Object,
            required: false,
            default: undefined
        }
    },
    mixins: [CentralizedSaving],
    created() {
        const save = async ({ state: { current } }) => {
            if (current && !current.options.saved) {
                await Promise.all(
                    this.settings.map(setting =>
                        this.$store.dispatch("session/SAVE_SETTING", { setting, value: current.value[setting] })
                    )
                );
                this.PUSH_STATE({ value: current.value, options: { saved: true } });
            }
        };
        this.registerSaveAction(save);
        this.value = this.settings.reduce((values, prop) => {
            const value = this.$store.state.session.settings.remote[prop];
            values[prop] = value || this.defaults?.[prop];
            return values;
        }, {});
    }
};
