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
            await Promise.all(
                this.settings.map(setting =>
                    this.$store.dispatch("settings/SAVE_SETTING", { setting, value: current.value[setting] })
                )
            );
        };
        this.registerSaveAction(save);
        this.value = this.settings.reduce((values, prop) => {
            const value = this.$store.state.settings[prop];
            values[prop] = value || this.defaults?.[prop];
            return values;
        }, {});
    }
};
