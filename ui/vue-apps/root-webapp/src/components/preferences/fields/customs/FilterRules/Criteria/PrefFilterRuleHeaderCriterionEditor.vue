<template>
    <div class="pref-filter-rule-header-criterion-editor d-flex flex-fill">
        <div class="d-flex col-6">
            <bm-button variant="outline-secondary" class="px-2 border font-weight-normal" @click="$emit('reset')">
                {{ $t("preferences.mail.filters.target.HEADER", { name: "" }) }}
                <bm-icon class="ml-1 text-dark" icon="caret-down" size="sm" />
            </bm-button>
            <bm-form-input
                v-model="criterion.target.name"
                class="ml-1 pr-4 flex-fill"
                :placeholder="$t('preferences.mail.filters.modal.criteria.header.name.placeholder')"
                required
            />
        </div>
        <div class="d-flex col-6">
            <bm-form-select v-model="criterion.matcher" :options="options" @input="deleteValueIfNeeded" />
            <bm-form-input
                v-if="![CRITERIA_MATCHERS.EXISTS, CRITERIA_MATCHERS.DOESNOTEXIST].includes(criterion.matcher)"
                v-model="criterion.value"
                class="ml-1 flex-fill"
                required
            />
        </div>
    </div>
</template>

<script>
import { CRITERIA_MATCHERS, reverseMatcher } from "../filterRules.js";
import { BmButton, BmIcon, BmFormInput, BmFormSelect } from "@bluemind/styleguide";

export default {
    name: "PrefFilterRuleHeaderCriterionEditor",
    components: { BmButton, BmFormInput, BmFormSelect, BmIcon },
    props: {
        criterion: {
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
            CRITERIA_MATCHERS,
            options: this.negative
                ? [CRITERIA_MATCHERS.ISNOT, CRITERIA_MATCHERS.DOESNOTCONTAIN, CRITERIA_MATCHERS.DOESNOTEXIST].map(
                      matcher => ({
                          text: this.$t(`preferences.mail.filters.matcher.${reverseMatcher(matcher)}`),
                          value: matcher
                      })
                  )
                : [CRITERIA_MATCHERS.IS, CRITERIA_MATCHERS.CONTAINS, CRITERIA_MATCHERS.EXISTS].map(matcher => ({
                      text: this.$t(`preferences.mail.filters.matcher.${matcher}`),
                      value: matcher
                  }))
        };
    },
    methods: {
        deleteValueIfNeeded(matcher) {
            if ([CRITERIA_MATCHERS.EXISTS, CRITERIA_MATCHERS.DOESNOTEXIST].includes(matcher)) {
                delete this.criterion.value;
            }
        }
    }
};
</script>
