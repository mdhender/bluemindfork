<template>
    <div class="pref-filter-rule-criterion d-flex align-items-center">
        <span class="regular mr-3"> {{ criterion.getName(negates) }} </span>
        <slot />
    </div>
</template>

<script>
export default {
    name: "PrefFilterRuleCriterion",
    props: {
        criterion: {
            type: Object,
            required: true
        },
        negates: {
            type: Boolean,
            default: false
        }
    }
};
</script>
