import isEqual from "lodash.isequal";
import cloneDeep from "lodash.clonedeep";
import BaseField from "./BaseField";
import { mapActions } from "vuex";
import { ERROR } from "@bluemind/alert.store";

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
        ...mapActions("alert", { ERROR }),
        registerSaveAction(save) {
            const actions = {};
            const state = { current: null, saved: null };

            const wrappedSave = async (...args) => {
                if (this.current && !this.current.options.saved) {
                    try {
                        await save(...args);
                        this.PUSH_STATE({
                            value: cloneDeep(this.value),
                            options: { saved: true, error: false, autosave: this.autosave }
                        });
                    } catch {
                        this.PUSH_STATE({
                            value: this.saved.value,
                            options: { saved: true, error: true, autosave: this.autosave }
                        });
                    }
                }
            };

            if (this.autosave) {
                actions.AUTOSAVE = wrappedSave;
            } else {
                actions.SAVE = wrappedSave;
            }

            this.$store.registerModule(["preferences", "fields", this.id], { state, actions });
        }
    },
    watch: {
        value: {
            async handler() {
                if (!isEqual(this.value, this.current?.value)) {
                    const saved = !this.saved || isEqual(this.value, this.saved.value);
                    const notValid = !this.isValid;
                    const autosave = this.autosave;
                    const value = cloneDeep(this.value);
                    this.PUSH_STATE({ value, options: { saved, notValid, autosave } });

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
