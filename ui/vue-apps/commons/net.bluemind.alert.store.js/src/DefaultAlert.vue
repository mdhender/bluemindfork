<template functional>
    <span>{{ parent.$tc($options.i18n(props.alert), $options.count(props), props.options) }}</span>
</template>

<script>
export default {
    name: "DefaultAlert",
    props: {
        alert: {
            type: Object,
            required: true
        },
        options: {
            type: Object,
            default: () => ({})
        },
        count: {
            type: Number,
            default: 0
        }
    },
    i18n({ name, type }) {
        return ["alert", name.toLowerCase(), type.toLowerCase()].join(".");
    },
    count({ alert, count }) {
        if (count > 0) {
            return count;
        } else if (Array.isArray(alert.payload)) {
            return alert.payload.length;
        } else {
            return 1;
        }
    }
};
</script>
