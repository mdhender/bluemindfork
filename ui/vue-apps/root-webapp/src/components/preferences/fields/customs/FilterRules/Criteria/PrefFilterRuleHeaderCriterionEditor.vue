<template>
    <div class="pref-filter-rule-header-criterion-editor d-flex flex-fill">
        <div class="d-flex col-6">
            <bm-button variant="outline" class="font-weight-normal" @click="$emit('reset')">
                {{ $t("preferences.mail.filters.target.headers", { name: "" }) }}
                <bm-icon class="ml-3 text-neutral" icon="caret-down" size="xs" />
            </bm-button>
            <bm-form-input
                v-model="target"
                class="ml-3 flex-fill"
                :placeholder="$t('preferences.mail.filters.modal.criteria.header.name.placeholder')"
                required
            />
        </div>
        <div class="d-flex col-6">
            <bm-form-select v-model="matcher" :options="options" />
            <bm-form-input
                v-if="matcher !== CRITERIA_MATCHERS.EXISTS"
                v-model="value"
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
    computed: {
        matcher: {
            get() {
                return this.criterion.matcher;
            },
            set(matcher) {
                const { value, ...criterion } = this.criterion;
                if (matcher === CRITERIA_MATCHERS.EXISTS) {
                    this.$emit("update:criterion", { ...criterion, matcher });
                } else {
                    this.$emit("update:criterion", { ...criterion, value, matcher });
                }
            }
        },
        value: {
            get() {
                return this.criterion.value || "";
            },
            set(value) {
                this.$emit("update:criterion", { ...this.criterion, value });
            }
        },
        target: {
            get() {
                return this.criterion.target.name;
            },
            set(name) {
                this.$emit("update:criterion", { ...this.criterion, target: { ...this.criterion.target, name } });
            }
        }
    }
};
</script>
