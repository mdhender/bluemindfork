<template>
    <bm-form-group
        id="actions-group"
        class="pref-filter-rule-modal-actions"
        :label="$t('preferences.mail.filters.modal.actions')"
        label-class="circled-number three d-flex align-items-center"
    >
        <template v-for="(action, index) in actions">
            <div v-if="resolvedActions[index]" :key="index" class="rule-action">
                <div class="rule-action-name">
                    <bm-form-select
                        ref="actionCombo"
                        class="flex-fill"
                        variant="underline"
                        :value="actionComboValue(action)"
                        :options="actionChoices(action.isNew)"
                        :placeholder="$t('preferences.mail.filters.modal.actions.add.placeholder')"
                        :auto-min-width="false"
                        @input="modifyActionType(index, $event)"
                    />
                    <bm-button-close class="mobile-only" @click="removeAction(index)" />
                </div>
                <div v-if="resolvedActions[index].editor" class="rule-action-value">
                    <component
                        :is="resolvedActions[index].editor"
                        :action="action"
                        class="flex-fill"
                        @update:action="updateAction(index, $event)"
                    />
                </div>
                <bm-button-close class="desktop-only" @click="removeAction(index)" />
            </div>
        </template>
        <bm-button v-if="!actions || !actions.some(c => c.isNew)" variant="text-accent" @click="addNewAction">
            {{ $t("preferences.mail.filters.modal.actions.add") }}
        </bm-button>
    </bm-form-group>
</template>

<script>
import { all, resolve } from "../Actions/actionResolver.js";
import { ACTIONS } from "../filterRules";
import { BmButton, BmButtonClose, BmFormGroup, BmFormSelect } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleModalActions",
    components: { BmButton, BmButtonClose, BmFormGroup, BmFormSelect },
    props: {
        actions: {
            type: Array,
            required: true
        }
    },
    data() {
        return { allActions: all(this), showBoxOnNextUpdate: false };
    },
    computed: {
        hasForwardAction() {
            return this.actions.find(action => action.name === ACTIONS.FORWARD.name);
        },
        resolvedActions() {
            return this.actions?.map(a => (a.isNew ? a : resolve(a, this))) || [];
        }
    },
    watch: {
        actions() {
            if (this.showBoxOnNextUpdate) {
                this.showBoxOnNextUpdate = false;
                this.showActionCombo();
            }
        }
    },
    methods: {
        actionChoices(isNew) {
            let actions = this.allActions;
            if (isNew && this.hasForwardAction) {
                actions = actions.filter(action => action.name !== ACTIONS.FORWARD.name);
            }
            return actions.map(a => ({ value: a, text: a.text }));
        },
        actionComboValue(action) {
            return this.actionChoices(action.isNew).find(ac => ac.value.name === action.name)?.value;
        },
        modifyActionType(index, { name, parameters }) {
            const updated = { name, parameters };
            this.$emit(
                "update:actions",
                this.actions.map((action, i) => (index === i ? updated : action))
            );
        },
        updateAction(index, value) {
            this.$emit(
                "update:actions",
                this.actions.map((action, i) => (index === i ? value : action))
            );
        },
        addNewAction() {
            this.$emit("update:actions", [...this.actions, { isNew: true }]);
            this.showBoxOnNextUpdate = this.actions.length > 0;
        },
        removeAction(index) {
            this.$emit(
                "update:actions",
                this.actions.filter((criterion, i) => index !== i)
            );
        },
        async showActionCombo() {
            await this.$nextTick();
            const combos = this.$refs.actionCombo;
            const lastCombo = combos[combos.length - 1];
            lastCombo.$refs.dropdown.show();
        }
    }
};
</script>

<style lang="scss">
@import "@bluemind/ui-components/src/css/utils/responsiveness";
@import "@bluemind/ui-components/src/css/utils/variables";
@import "./variables";

.pref-filter-rule-modal-actions {
    .rule-action {
        display: flex;
        margin-bottom: $sp-4;
        gap: $name-value-close-gap;
        @include until-lg {
            flex-direction: column;
            margin-bottom: $sp-6;
        }

        .rule-action-name,
        .rule-action-value {
            display: flex;
            align-items: center;
            min-width: 0;
            gap: $name-value-close-gap;
        }
        @include from-lg {
            align-items: start;
            .rule-action-name {
                flex: 1;
            }
            .rule-action-value {
                flex: 1.5;
            }
        }

        > .bm-button-close {
            position: relative;
            top: $close-offset-top;
        }
    }
}
</style>
