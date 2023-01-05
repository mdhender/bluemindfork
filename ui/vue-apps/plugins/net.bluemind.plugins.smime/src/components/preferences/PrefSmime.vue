<template>
    <div v-if="IS_SW_AVAILABLE" class="pref-smime">
        <div v-if="swError">
            {{ $t("smime.preferences.service_worker.error") }}
        </div>
        <template v-else>
            <img :src="SMIME_AVAILABLE ? setKeyIllustration : unsetKeyIllustration" class="mr-5" />
            <div class="d-inline-block align-middle">
                <template v-if="SMIME_AVAILABLE">
                    <bm-label-icon icon="check-circle" :inline="false">
                        {{ $t("smime.preferences.import_field.cert_and_key_associated") }}
                    </bm-label-icon>
                    <bm-button variant="text" class="mt-5 ml-6" @click="dissociate">
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
        <import-pkcs12-modal ref="import-modal" @ok="needReload" />
    </div>
    <div v-else>{{ $t("smime.preferences.service_worker.undefined") }}</div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BaseField } from "@bluemind/preferences";
import { BmButton, BmLabelIcon } from "@bluemind/ui-components";
import { DISSOCIATE_CRYPTO_FILES } from "../../store/actionTypes";
import { SMIME_AVAILABLE } from "../../store/getterTypes";
import { IS_SW_AVAILABLE } from "../../lib/constants";
import ImportPkcs12Modal from "./ImportPkcs12Modal";
import unsetKeyIllustration from "../../../assets/setting-encryption-key-unset.png";
import setKeyIllustration from "../../../assets/setting-encryption-key-set.png";

export default {
    name: "PrefSmime",
    components: { BmButton, BmLabelIcon, ImportPkcs12Modal },
    mixins: [BaseField],
    data() {
        return { IS_SW_AVAILABLE, setKeyIllustration, unsetKeyIllustration };
    },
    computed: {
        ...mapState("mail", ["hasPrivateKey", "hasPublicCert", "swError"]),
        ...mapGetters("mail", [SMIME_AVAILABLE])
    },
    methods: {
        openUploadModal() {
            this.$refs["import-modal"].open();
        },
        async dissociate() {
            await this.$store.dispatch("mail/" + DISSOCIATE_CRYPTO_FILES);
            this.NEED_RELOAD();
        },
        needReload() {
            this.NEED_RELOAD();
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables.scss";
.pref-smime {
    .fa-check-circle {
        color: $success-fg;
    }
}
</style>
