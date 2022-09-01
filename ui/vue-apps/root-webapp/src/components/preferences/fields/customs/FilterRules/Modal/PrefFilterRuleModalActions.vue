<template>
    <bm-form-group
        id="actions-group"
        class="pref-filter-rule-modal-actions"
        :label="$t('preferences.mail.filters.modal.actions')"
        label-class="circled-number three d-flex align-items-center"
    >
        <template v-for="(action, index) in filter.actions">
            <div
                v-if="resolvedActions[index]"
                :key="index"
                class="d-flex align-items-start justify-content-between row mb-1"
            >
                <div class="d-flex col-11 align-items-start">
                    <div class="col-6">
                        <bm-form-select
                            ref="actionCombo"
                            class="w-100"
                            :value="actionComboValue(action)"
                            :options="actionChoices"
                            :placeholder="$t('preferences.mail.filters.modal.actions.add.placeholder')"
                            :auto-min-width="false"
                            @input="modifyActionType(index, $event)"
                        />
                    </div>
                    <div v-if="resolvedActions[index].editor" class="col-6">
                        <component :is="resolvedActions[index].editor" :action="action" class="w-100" />
                    </div>
                </div>
                <bm-icon-button variant="compact" icon="cross" @click="removeAction(index)" />
            </div>
        </template>
        <bm-button
            v-if="!filter.actions || !filter.actions.some(c => c.isNew)"
            variant="text-accent"
            @click="addNewAction"
        >
            {{ $t("preferences.mail.filters.modal.actions.add") }}
        </bm-button>
    </bm-form-group>
</template>

<script>
import { all as allActions, resolve as resolveAction } from "../Actions/actionResolver.js";
import { BmButton, BmIconButton, BmFormGroup, BmFormSelect } from "@bluemind/styleguide";

export default {
    name: "PrefFilterRuleModalActions",
    components: { BmButton, BmIconButton, BmFormGroup, BmFormSelect },
    props: {
        filter: {
            type: Object,
            required: true
        }
    },
    data() {
        return {
            actionChoices: allActions(this).map(a => ({ value: a, text: a.name }))
        };
    },
    computed: {
        resolvedActions() {
            return this.filter.actions?.map(a => (a.isNew ? a : resolveAction(a, this))) || [];
        }
    },
    watch: {
        "filter.actions"() {
            if (this.filter.actions?.length === 0) {
                this.addNewAction();
            }
        }
    },
    methods: {
        actionComboValue(action) {
            return this.actionChoices.find(ac => ac.value.type === action.type)?.value;
        },
        modifyActionType(index, { type, value }) {
            this.filter.actions.splice(index, 1, { type, value });
        },
        addNewAction() {
            this.filter.actions.push({ isNew: true });
            if (this.filter.actions.length > 1) {
                this.$nextTick(this.showActionCombo);
            }
        },
        removeAction(index) {
            this.filter.actions.splice(index, 1);
        },
        showActionCombo() {
            const combos = this.$refs.actionCombo;
            const lastCombo = combos[combos.length - 1];
            lastCombo.$refs.dropdown.show();
        }
    }
};
</script>

<style lang="scss">
.pref-filter-rule-modal-actions {
    .col-11 {
        padding-left: 0 !important;
    }
}
</style>
