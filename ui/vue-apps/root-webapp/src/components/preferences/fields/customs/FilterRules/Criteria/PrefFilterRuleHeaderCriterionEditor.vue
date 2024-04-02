<template>
    <div class="pref-filter-rule-header-criterion-editor">
        <div class="criterion-name">
            <div class="header-criterion-group">
                <bm-form-select
                    class="header-criterion-group-left"
                    variant="underline"
                    :options="[{ value: 0, text: $t('preferences.mail.filters.target.headers', { name: '' }) }]"
                    :value="0"
                    :auto-min-width="false"
                    @click="$emit('reset')"
                />
                <bm-form-input
                    v-model="target"
                    class="header-criterion-group-right"
                    variant="underline"
                    :placeholder="$t('preferences.mail.filters.modal.criteria.header.name.placeholder')"
                    required
                />
            </div>
            <bm-button-close class="mobile-only" @click="$emit('remove')" />
        </div>
        <div class="header-criterion-group criterion-value">
            <bm-form-select
                v-model="matcher"
                class="header-criterion-group-left"
                variant="underline"
                :auto-min-width="false"
                :options="options"
            />
            <bm-form-input
                v-if="matcher !== CRITERIA_MATCHERS.EXISTS"
                v-model="value"
                class="header-criterion-group-right"
                variant="underline"
                required
            />
        </div>
    </div>
</template>

<script>
import { CRITERIA_MATCHERS } from "../filterRules.js";
import { BmButtonClose, BmFormInput, BmFormSelect } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleHeaderCriterionEditor",
    components: { BmButtonClose, BmFormInput, BmFormSelect },
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

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";
@import "../Modal/variables";

.pref-filter-rule-header-criterion-editor {
    display: flex;
    align-items: center;
    gap: $name-value-close-gap;
    flex: 1 1 auto;
    min-width: 0;
    @include until-lg {
        flex-direction: column;
        align-items: stretch;
    }

    .header-criterion-group {
        display: flex;
        flex: 1;
        min-width: 0;
        align-items: center;
        gap: $name-value-close-gap;

        .header-criterion-group-left {
            flex: 0 1 base-px-to-rem(128);
        }
        .header-criterion-group-right {
            flex: 1;
        }
    }
}
</style>
