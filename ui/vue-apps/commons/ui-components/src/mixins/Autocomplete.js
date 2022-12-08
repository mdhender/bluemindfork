export default {
    props: {
        value: {
            validator: prop => typeof prop === "string" || prop === undefined,
            required: true
        },
        items: {
            type: Array,
            required: true
        },
        maxResults: {
            type: Number,
            required: false,
            default: 5
        },
        icon: {
            type: String,
            required: false,
            default: ""
        },
        enableAutoFocusOut: {
            type: Boolean,
            required: false,
            default: true
        },
        selectInputOnFocus: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            closeAutocomplete: true,
            newInputValue: ""
        };
    },
    computed: {
        items_() {
            return this.items.slice(0, this.maxResults);
        }
    },
    watch: {
        value: {
            handler: function () {
                this.newInputValue = this.value;
            },
            immediate: true
        }
    },
    methods: {
        onInput(newValue) {
            this.$emit("input", newValue);
            this.closeAutocomplete = false;
        },
        onFocusOut(event) {
            if (this.enableAutoFocusOut) {
                if (!this.$el.contains(document.activeElement) && !this.$el.contains(event.relatedTarget)) {
                    this.closeSuggestions();
                }
            }
            this.$emit("blur");
        },
        onFocusIn() {
            this.closeAutocomplete = false;
            if (this.selectInputOnFocus) {
                this.$refs.input.setSelectionRange();
            }
            this.$emit("focus");
        },
        closeSuggestions() {
            this.closeAutocomplete = true;
        },
        focus() {
            this.$refs["input"].focus();
        },
        async reset() {
            this.newInputValue = "";
            this.onInput("");
            await this.$nextTick();
            this.closeSuggestions();
        }
    }
};
