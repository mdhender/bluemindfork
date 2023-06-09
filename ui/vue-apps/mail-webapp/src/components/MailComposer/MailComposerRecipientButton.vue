<template>
    <div>
        <bm-button variant="text-accent" @click="show = true">{{ recipientType }}</bm-button>
        <bm-modal id="recipient-picker" v-model="show" centered hide-footer />
    </div>
</template>

<script>
import { BmButton, BmModal } from "@bluemind/ui-components";

export default {
    components: {
        BmButton,
        BmModal
    },
    props: {
        recipientType: {
            type: String,
            required: true
        }
    },
    data() {
        return {
            show: false
        };
    }
};
</script>
