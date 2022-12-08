<script>
import BmFormAutocompleteInput from "./BmFormAutocompleteInput";

export default {
    name: "BmComboBox",
    components: { BmFormAutocompleteInput },
    extends: BmFormAutocompleteInput,

    props: {
        icon: {
            type: String,
            required: false,
            default: "caret-down"
        },
        actionableIcon: {
            type: Boolean,
            required: false,
            default: true
        },
        selectInputOnFocus: {
            type: Boolean,
            default: true
        }
    },
    watch: {
        closeAutocomplete(value) {
            if (value) {
                this.$emit("close");
            }
        }
    },
    mounted() {
        this.$refs.input.$on("icon-click", () => {
            this.toggleResults();
            this.$emit("icon-click");
        });
    },
    methods: {
        toggleResults() {
            this.closeAutocomplete = !this.closeAutocomplete;
        }
    }
};
</script>

<style lang="scss">
@use "sass:map";
@import "../../css/_variables";

.bm-form-autocomplete-input {
    .bm-form-input {
        input[icon="caret-down"] + .icon-wrapper {
            right: 0;

            .bm-icon-button.actionable-icon {
                height: 100%;
                outline-offset: -2px;
                .bm-icon {
                    $size: map-get($icon-sizes, "xs");
                    width: $size;
                    height: $size;
                }
            }
        }
    }
}
</style>
