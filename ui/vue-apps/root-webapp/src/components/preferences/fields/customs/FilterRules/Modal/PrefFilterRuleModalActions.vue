<template>
    <bm-form-group
        id="actions-group"
        class="pref-filter-rule-modal-actions"
        :label="$t('preferences.mail.filters.modal.actions')"
        label-class="circled-number three d-flex align-items-center"
    >
        <template v-for="(action, index) in actions">
            <div
                v-if="resolvedActions[index]"
                :key="index"
                class="d-flex align-items-start justify-content-between row mb-4"
            >
                <div class="d-flex col-11 align-items-start">
                    <div class="col-6">
                        <bm-form-select
                            ref="actionCombo"
                            class="w-100"
                            :value="actionComboValue(action)"
                            :options="actionChoices(action.isNew)"
                            :placeholder="$t('preferences.mail.filters.modal.actions.add.placeholder')"
                            :auto-min-width="false"
                            @input="modifyActionType(index, $event)"
                        />
                    </div>
                    <div v-if="resolvedActions[index].editor" class="col-6">
                        <component
                            :is="resolvedActions[index].editor"
                            :action="action"
                            class="w-100"
                            @update:action="updateAction(index, $event)"
                        />
                    </div>
                </div>
                <bm-icon-button variant="compact" icon="cross" @click="removeAction(index)" />
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
import { BmButton, BmIconButton, BmFormGroup, BmFormSelect } from "@bluemind/ui-components";

export default {
    name: "PrefFilterRuleModalActions",
    components: { BmButton, BmIconButton, BmFormGroup, BmFormSelect },
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
.pref-filter-rule-modal-actions {
    .col-11 {
        padding-left: 0 !important;
    }
}
</style>
