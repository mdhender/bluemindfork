<template>
    <div class="mail-component-alert d-flex flex-fill bg-surface">
        <bm-alert
            show
            variant="warning"
            class="d-flex flex-fill justify-content-between text-dark px-3 mb-0 align-items-center"
        >
            <bm-icon :icon="icon" class="mr-3 d-block fa-lg text-warning align-self-center" />
            <slot />
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
