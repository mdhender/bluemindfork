<template>
    <div v-if="IS_SW_AVAILABLE" class="pref-smime">
        <bm-spinner v-if="loading" />
        <div v-else-if="swError">
            {{ $t("smime.preferences.service_worker.error") }}
        </div>
        <template v-else>
            <img :src="SMIME_AVAILABLE ? setKeyIllustration : unsetKeyIllustration" class="mr-5" />
            <div class="d-inline-block align-middle">
                <template v-if="SMIME_AVAILABLE">
                    <bm-label-icon icon="check-circle" icon-size="lg">
                        {{ $t("smime.preferences.import_field.cert_and_key_associated") }}
                    </bm-label-icon>
                    <bm-button variant="text" class="d-block mt-5 ml-6" @click="dissociate">
                        {{ $t("common.dissociate") }}
                    </bm-button>
                </template>
                <template v-else>
                    {{ $t("smime.preferences.import_field.cert_or_key_dissociated") }}
                    <bm-button variant="text-accent" class="d-block mt-5 ml-4" @click="openUploadModal">
                        <bm-icon icon="key" class="mr-3" />
                        {{ $t("common.associate") }}
                    </bm-button>
                </template>
            </div>
        </template>
        <import-smime-key-modal ref="import-modal" />
    </div>
    <div v-else>{{ $t("smime.preferences.service_worker.undefined") }}</div>
</template>

<script>
import { mapGetters, mapState } from "vuex";
import { BmButton, BmIcon, BmLabelIcon, BmSpinner } from "@bluemind/ui-components";
import { CHECK_IF_ASSOCIATED, DISSOCIATE_CRYPTO_FILES } from "../../store/actionTypes";
import { SMIME_AVAILABLE } from "../../store/getterTypes";
import { IS_SW_AVAILABLE } from "../../lib/constants";
import ImportSmimeKeyModal from "./ImportSmimeKeyModal";
import unsetKeyIllustration from "../../../assets/setting-encryption-key-unset.png";
import setKeyIllustration from "../../../assets/setting-encryption-key-set.png";

export default {
    name: "PrefSmime",
    components: { BmButton, BmIcon, BmLabelIcon, BmSpinner, ImportSmimeKeyModal },
    data() {
        return { IS_SW_AVAILABLE, setKeyIllustration, unsetKeyIllustration };
    },
    computed: {
        ...mapState("smime", ["hasPrivateKey", "hasPublicCert", "loading", "swError"]),
        ...mapGetters("smime", [SMIME_AVAILABLE])
    },
    mounted() {
        this.$store.dispatch("smime/" + CHECK_IF_ASSOCIATED);
    },
    methods: {
        openUploadModal() {
            this.$refs["import-modal"].open();
        },
        dissociate() {
            this.$store.dispatch("smime/" + DISSOCIATE_CRYPTO_FILES);
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/ui-components/src/css/variables";

.pref-smime {
    .fa-check-circle {
        color: $success-fg;
    }
}
</style>
