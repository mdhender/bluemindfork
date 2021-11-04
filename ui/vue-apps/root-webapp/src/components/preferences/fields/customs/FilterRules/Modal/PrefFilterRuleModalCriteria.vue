<template>
    <bm-form-group
        id="criteria-group"
        class="pref-filter-rule-modal-criteria"
        :label="
            negative ? $t('preferences.mail.filters.modal.exceptions') : $t('preferences.mail.filters.modal.criteria')
        "
        :label-class="labelClass"
    >
        <template v-for="(criterion, index) in filter.criteria">
            <div
                v-if="resolvedCriteria[index] && negative !== resolvedCriteria[index].positive"
                :key="index"
                class="d-flex align-items-center justify-content-between row mb-1"
            >
                <div class="d-flex col">
                    <bm-form-select
                        v-show="!resolvedCriteria[index] || !resolvedCriteria[index].fullEditor"
                        ref="criterionCombo"
                        class="col-6 pr-4"
                        :value="criterionComboValue(criterion)"
                        :options="criterionChoices"
                        :placeholder="
                            negative
                                ? $t('preferences.mail.filters.modal.exceptions.add.placeholder')
                                : $t('preferences.mail.filters.modal.criteria.add.placeholder')
                        "
                        @input="modifyCriterionType(index, $event)"
                    />
                    <component
                        :is="resolvedCriteria[index].editor"
                        :class="{ 'col-6': !resolvedCriteria[index].fullEditor }"
                        :criterion="criterion"
                        :negative="negative"
                        @reset="resetCriterion(index)"
                    />
                </div>
                <bm-button-close class="col-1" @click="removeCriterion(index)" />
            </div>
        </template>
        <bm-button v-if="!criteria.some(c => c.isNew)" class="pl-0" variant="inline-primary" @click="addNewCriterion">
            {{
                negative
                    ? $t("preferences.mail.filters.modal.exceptions.add")
                    : $t("preferences.mail.filters.modal.criteria.add")
            }}
        </bm-button>
    </bm-form-group>
</template>

<script>
import { all as allCriteria, resolve as resolveCriterion } from "../Criteria/criterionResolver.js";
import { BmButton, BmButtonClose, BmFormGroup, BmFormSelect } from "@bluemind/styleguide";

export default {
    name: "PrefFilterRuleModalCriteria",
    components: { BmButton, BmButtonClose, BmFormGroup, BmFormSelect },
    props: {
        filter: {
            type: Object,
            required: true
        },
        negative: {
            type: Boolean,
            default: false
        }
    },
    data() {
        return {
            criterionChoices: allCriteria(this)
                .map(c => this.negative !== c.positive && { value: c, text: c.getName(this.negative) })
                .filter(Boolean)
        };
    },
    computed: {
        resolvedCriteria() {
            return this.filter.criteria?.map(c => (c.isNew ? c : resolveCriterion(c, this))) || [];
        },
        criteria() {
            return this.resolvedCriteria.filter(c => c && c.positive !== this.negative);
        },
        labelClass() {
            const labelClass = "d-flex align-items-center circled-number";
            return this.negative ? `${labelClass} four` : `${labelClass} two`;
        }
    },
    watch: {
        "filter.criteria"() {
            if (!this.negative && this.criteria.length === 0) {
                this.addNewCriterion();
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
            this.filter.criteria.splice(index, 1, {
                ...this.filter.criteria[index],
                matcher,
                target: { ...target },
                isNew: false
            });
        },
        addNewCriterion(forceOpenCombo) {
            this.filter.criteria.push({ isNew: true, positive: !this.negative });
            if (this.negative || this.criteria.length > 1 || forceOpenCombo) {
                this.$nextTick(this.showCriterionCombo);
            }
        },
        removeCriterion(index) {
            this.filter.criteria.splice(index, 1);
        },
        resetCriterion(index) {
            this.removeCriterion(index);
            this.addNewCriterion(true);
        },
        showCriterionCombo() {
            const combos = this.$refs.criterionCombo;
            const lastCombo = combos[combos.length - 1];
            lastCombo.$refs.dropdown.show();
        }
    }
};
</script>
