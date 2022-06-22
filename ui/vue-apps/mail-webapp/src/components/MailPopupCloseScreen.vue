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
            }
        }
    },
    created() {
        this.close(2500);
    },
    destroyed() {
        this.cancelClose();
    },
    methods: {
        close(timer = 1000) {
            if (!this.closer) {
                setTimeout(() => window.close(), timer);
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
