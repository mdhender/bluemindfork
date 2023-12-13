<template>
    <div v-if="isServiceWorkerAvailable" class="pref-smime d-flex align-items-center">
        <div v-if="swError">
            {{ $t("smime.preferences.service_worker.error") }}
        </div>
        <template v-else>
            <bm-illustration :value="SMIME_AVAILABLE ? 'lock-true' : 'lock-false'" size="xs" class="mr-5" />
            <div class="d-inline-block align-middle">
                <template v-if="SMIME_AVAILABLE">
                    <bm-label-icon icon="check-circle" :inline="false">
                        {{ $t("smime.preferences.import_field.cert_and_key_associated") }}
                    </bm-label-icon>
                    <bm-button variant="outline-danger" class="mt-5 ml-6" @click="dissociate">
                        {{ $t("common.dissociate") }}
                    </bm-button>
                </template>
                <template v-else>
                    <div>{{ $t("smime.preferences.import_field.cert_or_key_dissociated") }}</div>
                    <bm-button icon="key" variant="text-accent" class="mt-5 ml-4" @click="openUploadModal">
                        {{ $t("common.associate") }}
                    </bm-button>
                </template>
            </div>
        </template>
        <import-pkcs12-modal ref="import-modal" @ok="NEED_RELOAD()" />
    </div>
    <div v-else>
        {{ $t("smime.preferences.service_worker.undefined") }}
        <bm-read-more :href="incompatibleBrowserLink" />
    </div>
</template>

<script>
import { mapGetters } from "vuex";
import { BaseField } from "@bluemind/preferences";
import { BmButton, BmIllustration, BmLabelIcon, BmReadMore } from "@bluemind/ui-components";
import { DISSOCIATE_CRYPTO_FILES, SMIME_AVAILABLE } from "../../store/root-app/types";
import ImportPkcs12Modal from "./ImportPkcs12Modal";
import DocLinkMixin from "../../mixins/DocLinkMixin";

export default {
    name: "PrefSMime",
    components: { BmButton, BmIllustration, BmLabelIcon, BmReadMore, ImportPkcs12Modal },
    mixins: [BaseField, DocLinkMixin],
    computed: {
        ...mapGetters("smime", [SMIME_AVAILABLE]),
        isServiceWorkerAvailable() {
            return this.$store.state.smime.isServiceWorkerAvailable;
        },
        swError() {
            return this.$store.state.smime.swError;
        }
    },
    methods: {
        openUploadModal() {
            this.$refs["import-modal"].open();
        },
        async dissociate() {
            const confirm = await this.$bvModal.msgBoxConfirm(
                this.$t(`smime.preferences.import_field.dissociate_warning.content`),
                {
                    title: this.$t(`smime.preferences.import_field.dissociate_warning.title`),
                    okTitle: this.$t("common.dissociate"),
                    cancelTitle: this.$t("common.cancel"),
                    okVariant: "fill-danger"
                }
            );
            if (confirm) {
                await this.$store.dispatch("smime/" + DISSOCIATE_CRYPTO_FILES);
                this.NEED_RELOAD();
            }
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/utils/variables.scss";
.pref-smime {
    .icon-check-circle {
        color: $success-fg;
    }
}
</style>
