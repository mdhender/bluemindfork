<template>
    <div class="bg-surface flex-fill h-100"></div>
</template>
<script>
import { mapGetters, mapState } from "vuex";
import { ACTIVE_MESSAGE } from "~/getters";

export default {
    name: "MailPopupCloseScreen",
    data() {
        return { closer: undefined };
    },
    computed: {
        ...mapState({ alerts: state => state.alert.filter(({ area }) => !area) }),
        ...mapGetters("mail", { ACTIVE_MESSAGE }),
        closable() {
            return !this.ACTIVE_MESSAGE && this.alerts.length === 0;
        }
    },
    watch: {
        closable(isClosable) {
            if (isClosable) {
                this.close();
            } else {
                this.cancelClose();
            }
        }
    },
    create() {
        if (this.closable) {
            this.close(2500);
        }
    },
    destroyed() {
        this.cancelClose();
    },
    methods: {
        close(timer = 1000) {
            if (!this.closer) {
                this.closer = setTimeout(() => window.close(), timer);
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
