<template>
    <div class="mail-component-alert z-index-200 bg-surface">
        <bm-alert
            show
            variant="warning"
            class="d-flex flex-fill justify-content-between text-dark px-3 mb-0 align-items-center"
        >
            <bm-icon :icon="icon" class="mr-3 d-block fa-lg text-warning align-self-center" />
            <div><slot /></div>
            <bm-button-close class="ml-auto" @click="$emit('close')" />
        </bm-alert>
    </div>
</template>

<script>
import { BmButtonClose, BmAlert, BmIcon } from "@bluemind/styleguide";

export default {
    name: "MailComponentAlert",
    components: {
        BmAlert,
        BmButtonClose,
        BmIcon
    },
    props: {
        icon: {
            type: String,
            required: true
        }
    }
};
</script>
