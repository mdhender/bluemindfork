<template>
    <div class="pref-filter-rules">
        <p>{{ $t("preferences.mail.filters.desc") }}</p>
        <pref-filter-rule-modal ref="filters-editing-modal" :filter="editingFilter" @updateFilter="updateUserFilter" />
        <hr />
        <pref-filter-rules-subset
            :filters="domainFilters"
            :title="$t('preferences.mail.filters.subset.domain', { count: domainFilters.length })"
        />
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
import { inject } from "@bluemind/inject";
import { read as readRule, write as writeRule } from "./filterRules.js";
import PrefFilterRuleModal from "./Modal/PrefFilterRuleModal";
import PrefFilterRulesSubset from "./PrefFilterRulesSubset";
import CentralizedSaving from "../../../mixins/CentralizedSaving";

export default {
    name: "PrefFilterRules",
    components: { PrefFilterRuleModal, PrefFilterRulesSubset },
    mixins: [CentralizedSaving],
    data() {
        return {
            domainFilters: [],
            editingFilter: {},
            expanded: []
        };
    },
    computed: {
        /**
         * BTable adds an annoying _showDetails to each expanded item, our saving mechanism will detect a change...
         * We have to remove/add the expanded info via an intermediate value.
         */
        userFilters: {
            get() {
                return this.value.map(v => ({ ...v, _showDetails: this.expanded[v.index] }));
            },
            set(userFilters) {
                this.value = userFilters.map(f => {
                    this.expanded[f.index] = f._showDetails;
                    const copy = { ...f };
                    delete copy._showDetails;
                    return copy;
                });
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

        this.domainFilters = await this.fetchDomainFilters();
    },
    methods: {
        async fetchDomainFilters() {
            const domainFilters = await inject("MailboxesPersistence")?.getDomainFilter();
            return domainFilters.rules.map((f, index) => ({ ...readRule(f), index }));
        },
        normalizeUserFilters(rawFilters) {
            return rawFilters.rules.map((rule, index) => ({
                ...readRule(rule),
                index,
                terminal: rule.stop,
                editable: true
            }));
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

.pref-filter-rules {
    hr {
        background-color: $alternate-light;
    }
}
</style>
