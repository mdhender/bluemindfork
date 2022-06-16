<template>
    <div class="bg-surface flex-fill h-100"></div>
</template>
<script>
import { mapState } from "vuex";
export default {
    name: "MailPopupCloseScreen",
    data() {
        return { closer: undefined };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => !area) })
    },
    watch: {
        alerts: {
            handler(alerts) {
                if (alerts.length === 0) {
                    this.close();
                } else {
                    this.cancelClose();
                }
            },
            immediate: true
        }
    },
    destroyed() {
        this.cancelClose();
    },
    methods: {
        close() {
            if (!this.closer) {
                setTimeout(() => window.close(), 1000);
            }
        },
        cancelClose() {
            if (this.closer) {
                clearTimeout(this.closer);
                this.closer = undefined;
            }
        }
    }
};
</script>
<style>
.mail-popup-app .mail-composer {
    margin: 0 !important;
}
</style>
