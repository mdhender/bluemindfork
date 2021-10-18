import isEqual from "lodash.isequal";
import cloneDeep from "lodash.clonedeep";
import BaseField from "./BaseField";

export default {
    mixins: [BaseField],
    props: {
        autosave: {
            type: Boolean,
            required: false,
            default: false
        }
    },
    data() {
        return { value: undefined };
    },
    computed: {
        current() {
            return this.$store.state.preferences.fields[this.id]?.current;
        },
        saved() {
            return this.$store.state.preferences.fields[this.id]?.saved;
        },
        isValid() {
            return true;
        }
    },
    methods: {
        registerSaveAction(save) {
            const actions = {};
            const state = { current: null, saved: null };
            if (this.autosave) {
                actions.AUTOSAVE = save;
            } else {
                actions.SAVE = save;
            }

            this.$store.registerModule(["preferences", "fields", this.id], { state, actions });
        }
    },
    watch: {
        value: {
            handler() {
                if (!isEqual(this.value, this.current?.value)) {
                    const saved = !this.saved || isEqual(this.value, this.saved.value);
                    const error = !this.isValid;
                    const autosave = this.autosave;
                    this.PUSH_STATE({ options: { saved, error, autosave }, value: cloneDeep(this.value) });
                    if (!saved && autosave) {
                        this.$store.dispatch("preferences/AUTOSAVE");
                    }
                }
            },
            deep: true
        },
        current: {
            handler() {
                if (this.current && !isEqual(this.value, this.current.value)) {
                    this.value = cloneDeep(this.current.value);
                }
            },
            deep: true
        }
    }
};
