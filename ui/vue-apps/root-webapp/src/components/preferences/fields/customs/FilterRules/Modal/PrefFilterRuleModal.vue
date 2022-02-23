<template>
    <bm-modal
        :id="$attrs['id']"
        ref="pref-filter-rule-modal-bm-modal"
        class="pref-filter-rule-modal"
        dialog-class="pref-filter-rule-modal-dialog"
        centered
        :title="filter.index >= 0 ? $t('preferences.mail.filters.edit') : $t('preferences.mail.filters.create')"
        :cancel-title="$t('common.cancel')"
        :ok-title="filter.index >= 0 ? $t('common.edit') : $t('common.create')"
        :ok-disabled="okDisabled"
        @ok="save"
        @shown="init"
    >
        <bm-form class="mt-4">
            <pref-filter-rule-modal-name :filter="filterCopy" @submit="submit" />
            <pref-filter-rule-modal-criteria :filter="filterCopy" />
            <pref-filter-rule-modal-actions :filter="filterCopy" />
            <pref-filter-rule-modal-criteria :filter="filterCopy" negative />
            <pref-filter-rule-modal-terminal :filter="filterCopy" />
        </bm-form>
    </bm-modal>
</template>

<script>
import cloneDeep from "lodash.clonedeep";
import { BmForm, BmModal } from "@bluemind/styleguide";
import PrefFilterRuleModalActions from "./PrefFilterRuleModalActions";
import PrefFilterRuleModalCriteria from "./PrefFilterRuleModalCriteria";
import PrefFilterRuleModalName from "./PrefFilterRuleModalName";
import PrefFilterRuleModalTerminal from "./PrefFilterRuleModalTerminal";
import { createEmpty } from "../filterRules";

export default {
    name: "PrefFilterRuleModal",
    components: {
        BmForm,
        BmModal,
        PrefFilterRuleModalActions,
        PrefFilterRuleModalCriteria,
        PrefFilterRuleModalName,
        PrefFilterRuleModalTerminal
    },
    props: {
        filter: {
            type: Object,
            default: () => ({})
        }
    },
    data() {
        return {
            filterCopy: {}
        };
    },
    computed: {
        okDisabled() {
            return (
                !this.filterCopy.name ||
                this.filterCopy.name.trim() === "" ||
                this.filterCopy.actions.length === 0 ||
                areEqual(this.filter, this.filterCopy) ||
                this.filterCopy.criteria.some(c => c.isNew)
            );
        }
    },
    methods: {
        init() {
            const emptyFilter = createEmpty();
            const filter = cloneDeep(this.filter);
            this.filterCopy = { ...emptyFilter, ...filter };
            document.querySelector("#pref-filter-rule-modal-name-input")?.focus();
        },
        show() {
            this.$refs["pref-filter-rule-modal-bm-modal"].show();
        },
        hide() {
            this.$refs["pref-filter-rule-modal-bm-modal"].hide();
        },
        save() {
            this.$emit("updateFilter", this.filterCopy);
        },
        submit() {
            if (!this.okDisabled) {
                this.save();
                this.hide();
            }
        }
    }
};

function areEqual(filterA, filterB) {
    return (
        (!filterA && !filterB) ||
        (filterA &&
            filterB &&
            filterA.name === filterB.name &&
            filterA.terminal === filterB.terminal &&
            JSON.stringify(filterA.criteria) === JSON.stringify(filterB.criteria) &&
            JSON.stringify(filterA.actions) === JSON.stringify(filterB.actions))
    );
}
</script>

<style lang="scss">
@import "@bluemind/styleguide/css/_variables.scss";

$field-height: 2.25rem;
$circled-number-size: $field-height;
$circled-number-right-margin: 0.5rem;
$field-left-margin: calc(#{$circled-number-size} + #{$circled-number-right-margin});

.pref-filter-rule-modal-dialog {
    max-width: 66.5em !important;
    label,
    legend {
        &.circled-number {
            &::before {
                display: inline-flex;
                width: $circled-number-size;
                height: $circled-number-size;
                align-items: center;
                justify-content: center;
                margin-right: $circled-number-right-margin;

                border-radius: 50%;
                border: solid 1px $primary;

                font-size: $h1-font-size;
                font-weight: $font-weight-light;
                color: $primary;
                margin-bottom: 0;
                line-height: 1.375;
            }
            &.one::before {
                content: "1";
            }
            &.two::before {
                content: "2";
            }
            &.three::before {
                content: "3";
            }
            &.four::before {
                content: "4";
            }
        }
    }

    .pref-filter-rule-modal-name,
    .pref-filter-rule-modal-criteria,
    .pref-filter-rule-modal-actions,
    .pref-filter-rule-modal-exceptions,
    .pref-filter-rule-modal-terminal {
        & > :not(label):not(legend) {
            margin-left: $field-left-margin;
        }
    }

    .bm-form-input {
        width: auto !important;
        input {
            height: $field-height;
        }
    }

    footer {
        border-top: 1px solid $light;
    }

    .border,
    .bm-form-select .btn,
    .bm-form-input input {
        border-color: $alternate-light !important;
    }
}
</style>
