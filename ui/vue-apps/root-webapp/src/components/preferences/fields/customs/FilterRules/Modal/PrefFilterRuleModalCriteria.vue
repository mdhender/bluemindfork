<template>
    <bm-form-group
        id="criteria-group"
        class="pref-filter-rule-modal-criteria"
        :label="
            negative ? $t('preferences.mail.filters.modal.exceptions') : $t('preferences.mail.filters.modal.criteria')
        "
        :label-class="labelClass"
    >
        <template v-for="(criterion, index) in criteria">
            <div v-if="resolvedCriteria[index]" :key="index" class="criterion">
                <div v-if="!resolvedCriteria[index] || !resolvedCriteria[index].fullEditor" class="criterion-name">
                    <bm-form-select
                        ref="criterionCombo"
                        class="flex-fill"
                        variant="underline"
                        :value="criterionComboValue(criterion)"
                        :options="criterionChoices"
                        :placeholder="
                            negative
                                ? $t('preferences.mail.filters.modal.exceptions.add.placeholder')
                                : $t('preferences.mail.filters.modal.criteria.add.placeholder')
                        "
                        :auto-min-width="false"
                        @input="modifyCriterionType(index, $event)"
                    />
                    <bm-button-close class="mobile-only" @click="removeCriterion(index)" />
                </div>
                <div class="criterion-value" :class="{ 'flex-fill': !!resolvedCriteria[index].fullEditor }">
                    <component
                        :is="resolvedCriteria[index].editor"
                        :criterion="criterion"
                        class="flex-fill"
                        @update:criterion="updateCriterion(index, $event)"
                        @reset="resetCriterion(index)"
                        @remove="removeCriterion(index)"
                    />
                </div>
                <bm-button-close class="desktop-only" @click="removeCriterion(index)" />
            </div>
        </template>
        <bm-button v-if="!resolvedCriteria.some(c => c.isNew)" variant="text-accent" @click="addNewCriterion">
            {{
                negative
                    ? $t("preferences.mail.filters.modal.exceptions.add")
                    : $t("preferences.mail.filters.modal.criteria.add")
            }}
        </bm-button>
    </bm-form-group>
</template>

<script>
import cloneDeep from "lodash.clonedeep";
import { all as allCriteria, resolve as resolveCriterion } from "../Criteria/criterionResolver.js";
import { BmButton, BmButtonClose, BmFormGroup, BmFormSelect } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleModalCriteria",
    components: { BmButton, BmButtonClose, BmFormGroup, BmFormSelect },
    props: {
        criteria: {
            type: Array,
            default: () => []
        },
        negative: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            showBoxOnNextUpdate: false,
            criterionChoices: allCriteria(this)
                .map(c => ({ value: c, text: c.text }))
                .filter(Boolean)
        };
    },
    computed: {
        resolvedCriteria() {
            return this.criteria?.map(c => (c.isNew ? c : resolveCriterion(c, this))) || [];
        },
        labelClass() {
            const labelClass = "d-flex align-items-center circled-number";
            return this.negative ? `${labelClass} four` : `${labelClass} two`;
        }
    },
    watch: {
        criteria() {
            if (this.showBoxOnNextUpdate) {
                this.showBoxOnNextUpdate = false;
                this.showCriterionCombo();
            }
        }
    },
    methods: {
        criterionComboValue(criterion) {
            return this.criterionChoices.find(
                cc => cc.value.target.type === criterion.target?.type && cc.value.matcher === criterion.matcher
            )?.value;
        },
        modifyCriterionType(index, { matcher, target }) {
            const updated = { ...this.criteria[index], matcher, target: { ...target }, isNew: false };
            this.$emit(
                "update:criteria",
                this.criteria.map((criterion, i) => (i === index ? updated : criterion))
            );
        },
        updateCriterion(index, value) {
            this.$emit(
                "update:criteria",
                this.criteria.map((criterion, i) => (i === index ? value : criterion))
            );
        },
        addNewCriterion() {
            this.$emit("update:criteria", [...this.criteria, this.createCriterion()]);
            this.showBoxOnNextUpdate = this.negative || this.resolvedCriteria.length > 0;
        },
        removeCriterion(index) {
            this.$emit(
                "update:criteria",
                this.criteria.filter((criterion, i) => index !== i)
            );
        },
        resetCriterion(index) {
            const newCriteria = [...this.criteria];
            newCriteria.splice(index, 1, this.createCriterion(this.criteria[index].value));
            this.$emit("update:criteria", newCriteria);
            this.showBoxOnNextUpdate = true;
        },
        async showCriterionCombo() {
            await this.$nextTick();
            const combos = this.$refs.criterionCombo;
            const lastCombo = combos[combos.length - 1];
            lastCombo.$refs.dropdown.show();
        },
        createCriterion(value) {
            return { isNew: true, exception: this.negative, value };
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";
@import "./variables";

.pref-filter-rule-modal-criteria {
    .criterion {
        display: flex;
        align-items: center;
        margin-bottom: $sp-4;
        gap: $name-value-close-gap;
        @include until-lg {
            flex-direction: column;
            align-items: stretch;
            margin-bottom: $sp-6;
        }

        .criterion-name,
        .criterion-value {
            display: flex;
            align-items: center;
            min-width: 0;
            gap: $name-value-close-gap;
        }
        @include from-lg {
            .criterion-name {
                flex: 1;
            }
            .criterion-value {
                flex: 1.5;
            }
        }
    }
}
</style>
