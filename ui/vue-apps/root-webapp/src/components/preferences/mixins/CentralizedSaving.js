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
            default: false
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
            return true; // can be overwritten by component
        }
    },
    methods: {
        ...mapActions("alert", { ERROR }),
        $_CentralizedSaving_defaultOptions() {
            return {
                saved: true,
                error: false,
                autosave: this.autosave,
                reload: this.needReload,
                logout: this.needLogout
            };
        },
        registerSaveAction(save) {
            const actions = {};
            const state = { current: null, saved: null };

            const wrappedSave = async (...args) => {
                if (this.current && !this.current.options.saved) {
                    const options = this.$_CentralizedSaving_defaultOptions();
                    try {
                        await save(...args);
                        this.PUSH_STATE({ value: cloneDeep(this.value), options });
                    } catch {
                        this.PUSH_STATE({ value: this.saved.value, options: { ...options, error: true } });
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
