<template>
    <div class="pref-my-filter-rules">
        <pref-filter-rule-modal ref="filters-editing-modal" :filter="editingFilter" @save="updateUserFilter" />
        <hr />
        <pref-filter-rules-subset
            :filters="userFilters"
            :title="$t('preferences.mail.filters.subset.user', { count: userFilters.length })"
            editable
            @remove="removeUserFilter"
            @edit="edit"
            @up="moveUp"
            @down="moveDown"
            @top="moveTop"
            @bottom="moveBottom"
            @create-before="createBefore"
            @create-after="createAfter"
            @toggle-active="toggleActive"
        />
    </div>
</template>

<script>
import { read as readRules, write as writeRule, NEW_FILTER } from "./filterRules";
import PrefFilterRuleModal from "./Modal/PrefFilterRuleModal";
import PrefFilterRulesSubset from "./PrefFilterRulesSubset";
import CentralizedSaving from "../../../mixins/CentralizedSaving";
import { ERROR, SUCCESS } from "@bluemind/alert.store";
import { SAVE_ALERT } from "../../../Alerts/defaultAlerts";

export default {
    name: "PrefMyFilterRules",
    components: { PrefFilterRuleModal, PrefFilterRulesSubset },
    data() {
        return { editingFilter: { addBefore: undefined, addAfter: undefined }, expanded: [] };
    },
    computed: {
        userFilters() {
            return this.$store.state.preferences.mailboxFilter.rules;
        }
    },
    methods: {
        alert(success = true) {
            this.$store.dispatch(`alert/${success ? SUCCESS : ERROR}`, SAVE_ALERT);
        },
        dispatchWithAlert(action, params) {
            this.$store
                .dispatch(`preferences/${action}`, params)
                .then(this.alert)
                .catch(() => this.alert(false));
        },
        addUserFilter(filter) {
            filter.addBefore
                ? this.dispatchWithAlert("ADD_RULE_BEFORE", { rule: filter, relative: filter.addBefore })
                : filter.addAfter
                ? this.dispatchWithAlert("ADD_RULE_AFTER", { rule: filter, relative: filter.addAfter })
                : this.dispatchWithAlert("ADD_RULE", filter);
        },
        updateUserFilter(filter) {
            filter.id === undefined ? this.addUserFilter(filter) : this.dispatchWithAlert("UPDATE_RULE", filter);
        },
        removeUserFilter(filter) {
            this.dispatchWithAlert("REMOVE_RULE", filter);
        },
        moveUp({ filter, relativeTo }) {
            this.dispatchWithAlert("MOVE_UP_RULE", { rule: filter, relativeTo });
        },
        moveDown({ filter, relativeTo }) {
            this.dispatchWithAlert("MOVE_DOWN_RULE", { rule: filter, relativeTo });
        },
        moveTop(filter) {
            this.dispatchWithAlert("MOVE_RULE_TOP", filter);
        },
        moveBottom(filter) {
            this.dispatchWithAlert("MOVE_RULE_BOTTOM", filter);
        },
        edit(filter) {
            this.editingFilter = { ...filter };
            this.$refs["filters-editing-modal"].show();
        },
        createBefore(filter) {
            this.editingFilter = { ...NEW_FILTER, addBefore: filter };
            this.$refs["filters-editing-modal"].show();
        },
        createAfter(filter) {
            this.editingFilter = { ...NEW_FILTER, addAfter: filter };
            this.$refs["filters-editing-modal"].show();
        },
        toggleActive(filter) {
            this.dispatchWithAlert("UPDATE_RULE", { ...filter, active: !filter.active });
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables";

.pref-my-filter-rules {
    hr {
        background-color: $neutral-bg-lo1;
    }
}
</style>
