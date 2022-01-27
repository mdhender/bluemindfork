<template>
    <i18n path="alert.preferences.field.save.error" tag="span" class="save-error-alert">
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
    name: "SaveErrorAlert",
    mixins: [Navigation],
    props: {
        alert: {
            type: Object,
            default: () => ({})
        }
    }
};
</script>
