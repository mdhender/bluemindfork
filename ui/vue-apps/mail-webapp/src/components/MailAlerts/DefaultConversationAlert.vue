<template functional>
    <span>{{ parent.$tc($options.i18n(props.alert), $options.count(props), props.options) }}</span>
</template>
<script>
export default {
    name: "DefaultConversationAlert",
    props: {
        alert: {
            type: Object,
            default: () => ({})
        },
        options: {
            type: Object,
            required: false,
            default: () => ({})
        }
    },
    i18n({ name, type }) {
        return ["alert", name.toLowerCase(), type.toLowerCase()].join(".");
    },
    count({ alert }) {
        return alert.payload.conversations.length;
    }
};
</script>
