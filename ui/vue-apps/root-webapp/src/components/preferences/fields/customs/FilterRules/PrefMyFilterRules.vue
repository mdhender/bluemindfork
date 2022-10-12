<template>
    <div class="pref-my-filter-rules">
        <pref-filter-rule-modal ref="filters-editing-modal" :filter="editingFilter" @updateFilter="updateUserFilter" />
        <hr />
        <pref-filter-rules-subset
            :filters="userFilters"
            :title="$t('preferences.mail.filters.subset.user', { count: userFilters.length })"
            editable
            @remove="removeUserFilter"
            @edit="
                editingFilter = { ...$event };
                $refs['filters-editing-modal'].show();
            "
            @up="moveUp"
            @down="moveDown"
            @toggle-active="updateUserFilter"
        />
    </div>
</template>

<script>
import { read as readRules, write as writeRule } from "./filterRules.js";
import PrefFilterRuleModal from "./Modal/PrefFilterRuleModal";
import PrefFilterRulesSubset from "./PrefFilterRulesSubset";
import CentralizedSaving from "../../../mixins/CentralizedSaving";

export default {
    name: "PrefFilterRules",
    components: { PrefFilterRuleModal, PrefFilterRulesSubset },
    mixins: [CentralizedSaving],
    data() {
        return { editingFilter: {}, expanded: [] };
    },
    computed: {
        /**
         * BTable adds an annoying _showDetails to each expanded item, our saving mechanism will detect a change...
         * We have to remove/add the expanded info via an intermediate value.
         * Also, `this.value` contains all rules even those that can't be managed here:
         *  - we filter them out when we get the `userFilters` from the store
         *  - we add them back when we set `this.value` from `userFilters`
         */
        userFilters: {
            get() {
                return this.value.filter(v => v.manageable).map(v => ({ ...v, _showDetails: this.expanded[v.index] }));
            },
            set: function (userFilters) {
                this.value = [
                    ...userFilters.map(f => {
                        this.expanded[f.index] = f._showDetails;
                        const copy = { ...f };
                        delete copy._showDetails;
                        return copy;
                    }),
                    ...this.value.filter(v => !v.manageable)
                ].sort((a, b) => a.index - b.index);
            }
        }
    },
    async created() {
        const save = async ({ state: { current }, dispatch }) => {
            await dispatch(
                "preferences/SAVE_RULES",
                current.value.map(v => writeRule(v)),
                { root: true }
            );
        };
        this.registerSaveAction(save);

        this.value = this.normalizeUserFilters(this.$store.state.preferences.mailboxFilter);
    },
    methods: {
        normalizeUserFilters(rawFilters) {
            return readRules(rawFilters.rules);
        },
        addUserFilter(filter) {
            this.pushUserFilters(filter);
            this.updateIndexes(this.userFilters);
        },
        updateUserFilter(filter) {
            if (filter.index === undefined) {
                this.addUserFilter(filter);
            } else {
                this.spliceUserFilters(filter.index, 1, filter);
            }
        },
        removeUserFilter(filter) {
            this.spliceUserFilters(filter.index, 1);
            this.updateIndexes(this.userFilters);
        },
        moveUp(filter) {
            if (filter.index !== 0) {
                this.move(filter, -1);
            }
        },
        moveDown(filter) {
            if (filter.index + 1 !== this.userFilters.length) {
                this.move(filter, 1);
            }
        },
        move(filter, delta) {
            const userFilters = [...this.userFilters];
            const expanded = [...this.expanded];

            //remove
            const removedFilter = { ...userFilters.splice(filter.index, 1)[0] };
            const removedExpanded = expanded.splice(filter.index, 1)[0];

            //insert
            const newIndex = filter.index + delta;
            userFilters.splice(newIndex, 0, removedFilter);
            expanded.splice(newIndex, 0, removedExpanded);

            this.updateIndexes(userFilters);

            this.expanded = expanded;
            this.userFilters = userFilters;
        },
        updateIndexes(filters) {
            filters.forEach((f, index) => (f.index = index));
        },
        spliceUserFilters(index, removeCount, ...items) {
            return this.modifyUserFilters("splice", index, removeCount, ...items);
        },
        pushUserFilters(item) {
            return this.modifyUserFilters("push", item);
        },
        /**
         * /!\ this.userFilters is a computed with custom getter/setter, methods like splice or push do not call the
         * setter.
         */
        modifyUserFilters(method, ...args) {
            const userFilters = this.userFilters;
            userFilters[method](...args);
            this.userFilters = userFilters;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-my-filter-rules {
    hr {
        background-color: $neutral-bg-lo1;
    }
}
</style>
