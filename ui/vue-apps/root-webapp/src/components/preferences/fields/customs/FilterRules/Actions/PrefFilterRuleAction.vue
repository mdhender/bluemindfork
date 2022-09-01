<template>
    <div class="pref-filter-rule-action d-flex align-items-center text-nowrap">
        <span class="regular mr-3">{{ action.name }}</span>
        <slot />
    </div>
</template>

<script>
export default {
    name: "PrefFilterRuleAction",
    props: {
        action: {
            type: Object,
            required: true
        }
    }
};
</script>
