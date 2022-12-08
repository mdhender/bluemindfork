<template>
    <div class="pref-filter-rule-header-criterion-editor d-flex flex-fill">
        <div class="d-flex col-6">
            <bm-button variant="outline" class="font-weight-normal" @click="$emit('reset')">
                {{ $t("preferences.mail.filters.target.headers", { name: "" }) }}
                <bm-icon class="ml-3 text-neutral" icon="caret-down" size="xs" />
            </bm-button>
            <bm-form-input
                v-model="criterion.target.name"
                class="ml-3 flex-fill"
                :placeholder="$t('preferences.mail.filters.modal.criteria.header.name.placeholder')"
                required
            />
        </div>
        <div class="d-flex col-6">
            <bm-form-select v-model="criterion.matcher" :options="options" @input="deleteValueIfNeeded" />
            <bm-form-input
                v-if="criterion.matcher !== CRITERIA_MATCHERS.EXISTS"
                v-model="criterion.value"
                class="ml-3 flex-fill"
                required
            />
        </div>
    </div>
</template>

<script>
import { CRITERIA_MATCHERS } from "../filterRules.js";
import { BmButton, BmIcon, BmFormInput, BmFormSelect } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleHeaderCriterionEditor",
    components: { BmButton, BmFormInput, BmFormSelect, BmIcon },
    props: {
        criterion: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            CRITERIA_MATCHERS,
            options: [CRITERIA_MATCHERS.EQUALS, CRITERIA_MATCHERS.CONTAINS, CRITERIA_MATCHERS.EXISTS].map(matcher => ({
                text: this.$t(`preferences.mail.filters.matcher.${matcher}`),
                value: matcher
            }))
        };
    },
    methods: {
        deleteValueIfNeeded(matcher) {
            if (matcher === CRITERIA_MATCHERS.EXISTS) {
                delete this.criterion.value;
            }
        }
    }
};
</script>
