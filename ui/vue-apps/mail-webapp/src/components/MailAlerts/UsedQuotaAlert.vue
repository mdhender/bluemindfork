<template>
    <span class="used-quota-alert d-flex flex-wrap">
        {{ $t(alert.name) }}
    </span>
</template>

<script>
export default {
    name: "UsedQuotaAlert",
    props: {
        alert: {
            type: Object,
            required: true
        }
    }
};
</script>
