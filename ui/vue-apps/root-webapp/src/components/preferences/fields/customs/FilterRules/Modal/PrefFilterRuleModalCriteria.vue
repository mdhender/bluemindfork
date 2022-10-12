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
            <div
                v-if="resolvedCriteria[index]"
                :key="index"
                class="d-flex align-items-center justify-content-between row mb-4"
            >
                <div class="d-flex col-11">
                    <div v-show="!resolvedCriteria[index] || !resolvedCriteria[index].fullEditor" class="col-6">
	                <bm-form-select
	                    ref="criterionCombo"
                            class="w-100"
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
                    </div>
                    <div :class="resolvedCriteria[index].fullEditor ? 'w-100' : 'col-6'">
	                <component
	                    :is="resolvedCriteria[index].editor"
	                    :criterion="criterion"
                            class="w-100"
	                    @reset="resetCriterion(index)"
	                />
                    </div>
                </div>
                <bm-icon-button variant="compact" icon="cross" @click="removeCriterion(index)" />
            </div>
        </template>
        <bm-button
            v-if="!resolvedCriteria.some(c => c.isNew)"
            variant="text-accent"
            @click="addNewCriterion"
        >
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
import { BmButton, BmIconButton, BmFormGroup, BmFormSelect } from "@bluemind/styleguide";

export default {
    name: "PrefFilterRuleModalCriteria",
    components: { BmButton, BmIconButton, BmFormGroup, BmFormSelect },
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
        "criteria"() {
            if (!this.negative && this.resolvedCriteria.length === 0) {
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
            this.criteria.splice(index, 1, {
                ...this.criteria[index],
                matcher,
                target: { ...target },
                isNew: false
            });
        },
        addNewCriterion(forceOpenCombo) {
            this.criteria.push({ isNew: true, exception: this.negative });
            if (this.negative || this.resolvedCriteria.length > 1 || forceOpenCombo) {
                this.$nextTick(this.showCriterionCombo);
            }
        },
        removeCriterion(index) {
            this.criteria.splice(index, 1);
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

<style lang="scss">
.pref-filter-rule-modal-criteria {
    .col-11 {
        padding-left: 0 !important;
    }
}
</style>
