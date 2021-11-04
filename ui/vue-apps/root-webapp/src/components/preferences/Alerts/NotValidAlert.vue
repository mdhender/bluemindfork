<template>
    <i18n path="alert.preferences.validity.warning" tag="span" class="not-valid-alert">
        <template #name>
            <router-link :to="anchor(alert.payload.group, true)" @click.native="scrollTo(alert.payload.group)">
                {{ alert.payload.group.name }}
            </router-link>
        </template>
    </i18n>
</template>

<script>
import Navigation from "../mixins/Navigation";

export default {
    name: "NotValidAlert",
    mixins: [Navigation],
    props: {
        alert: {
            type: Object,
            default: () => ({})
        }
    }
};
</script>
