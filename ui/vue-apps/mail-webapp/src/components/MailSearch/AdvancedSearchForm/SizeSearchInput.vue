<template>
    <div class="size-search-input d-flex align-items-center">
        <bm-form-select
            v-model="selectedComparison"
            variant="underline"
            class="d-none d-lg-block"
            :options="comparisonOptions"
        />
        <bm-form-select v-model="selectedComparison" variant="outline" class="d-lg-none" :options="comparisonOptions" />
        <bm-form-input-number v-model="value" class="d-lg-none" min="0" variant="outline" type="number" />
        <bm-form-input-number v-model="value" class="d-none d-lg-block" min="0" variant="underline" type="number" />
        <bm-form-select v-model="selectedUnit" variant="outline" class="d-lg-none unit" :options="unitOptions" />
        <bm-form-select
            v-model="selectedUnit"
            variant="underline"
            class="d-none d-lg-block unit"
            :options="unitOptions"
        />
    </div>
</template>

<script>
import { BmFormSelect, BmFormInputNumber } from "@bluemind/ui-components";

const COMPARISON = {
    HIGHER: "higher",
    LOWER: "lower"
};
const UNIT = {
    MO: 1000000,
    KO: 1000,
    O: 1
};
export default {
    name: "SizeSearchInput",
    components: { BmFormSelect, BmFormInputNumber },
    props: {
        min: {
            type: Number,
            default: null
        },
        max: {
            type: Number,
            default: null
        }
    },
    data() {
        return {
            comparisonOptions: [
                { value: COMPARISON.HIGHER, text: this.$t("mail.search.label.higher") },
                { value: COMPARISON.LOWER, text: this.$t("mail.search.label.lower") }
            ],
            unitOptions: [
                { value: UNIT.MO, text: "Mo" },
                { value: UNIT.KO, text: "Ko" },
                { value: UNIT.O, text: "o" }
            ],
            selectedComparison: null,
            selectedUnit: null,
            value: null
        };
    },
    computed: {
        size() {
            return this.selectedUnit * this.value;
        },
        minMax() {
            return [this.min, this.max];
        }
    },
    watch: {
        selectedComparison(comparison) {
            if (comparison === COMPARISON.HIGHER) {
                this.$emit("update:max", null);
                this.$emit("update:min", this.size);
            } else {
                this.$emit("update:min", null);
                this.$emit("update:max", this.size);
            }
        },
        size: {
            handler(size) {
                if (size) {
                    const boundary = this.selectedComparison === COMPARISON.HIGHER ? "min" : "max";
                    this.$emit(`update:${boundary}`, size);
                } else {
                    this.$emit("update:min", 0);
                    this.$emit("update:max", 0);
                }
            },
            immediate: true
        },
        minMax: {
            handler() {
                this.selectedComparison = this.selectedComparison
                    ? this.selectedComparison
                    : !this.min && this.max
                    ? COMPARISON.LOWER
                    : COMPARISON.HIGHER;

                const value = this.min ? +this.min : +this.max;
                let unit = UNIT.MO;
                if (value && value < UNIT.KO) {
                    unit = UNIT.O;
                } else if (value && value < UNIT.MO) {
                    unit = UNIT.KO;
                }
                this.selectedUnit = unit;
                this.value = value ? `${+value / +this.selectedUnit}` : null;
            },
            immediate: true
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/variables";
@import "@bluemind/ui-components/src/css/utils/responsiveness";

.size-search-input {
    width: 100%;
    gap: $sp-4;
    @include from-lg {
        .bm-form-input-number {
            flex: 0 0 7rem;
        }
    }
    @include until-lg {
        .number {
            flex-grow: 1;
        }
    }
    .unit {
        flex: 0 0 4rem;
    }
}
</style>
