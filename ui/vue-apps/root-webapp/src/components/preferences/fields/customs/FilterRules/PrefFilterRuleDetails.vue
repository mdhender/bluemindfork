<template>
    <div class="row pref-filter-rule-details">
        <div class="col-6">
            <strong>{{ $tc("preferences.mail.filters.details.conditions", positiveCriteria.length || 1) }}</strong>
            <ol>
                <template v-if="positiveCriteria.length > 0">
                    <li v-for="(criterion, index) in positiveCriteria" :key="index">
                        <component :is="criterion.viewer" :criterion="criterion" />
                    </li>
                </template>
                <li v-else>
                    <em>{{ $t("preferences.mail.filters.details.conditions.all") }}</em>
                </li>
            </ol>
            <template v-if="negativeCriteria.length > 0">
                <strong>{{ $tc("preferences.mail.filters.details.exceptions", negativeCriteria.length) }}</strong>
                <ol>
                    <li v-for="(criterion, index) in negativeCriteria" :key="index">
                        <component :is="criterion.viewer" :criterion="criterion" :negates="true" />
                    </li>
                </ol>
            </template>
        </div>
        <div class="col-6">
            <strong>{{ $tc("preferences.mail.filters.details.actions", actions.length) }}</strong>
            <ol>
                <li v-for="(action, index) in actions" :key="index">
                    <component :is="action.viewer" :action="action" />
                </li>
            </ol>
        </div>
    </div>
</template>

<script>
import { resolve as resolveAction } from "./Actions/actionResolver.js";
import { resolve as resolveCriterion } from "./Criteria/criterionResolver.js";

export default {
    name: "PrefFilterRuleDetails",
    props: {
        filter: {
            type: Object,
            required: true
        }
    },
    computed: {
        positiveCriteria() {
            return this.filter.criteria.map(c => resolveCriterion(c, this));
        },
        negativeCriteria() {
            return this.filter.exceptions.map(c => resolveCriterion(c, this));
        },
        actions() {
            return this.filter.actions.map(a => resolveAction(a, this)).filter(Boolean);
        }
    },
    methods: {
        resolveCriterion
    }
};
</script>

<style lang="scss">
.pref-filter-rule-details {
    li {
        line-height: 2.25em;
    }
}
</style>
