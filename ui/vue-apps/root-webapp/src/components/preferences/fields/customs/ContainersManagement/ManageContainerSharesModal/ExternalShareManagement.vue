<template>
    <div>
        <hr />
        <bm-label-icon icon="user" icon-size="lg" class="font-weight-bold mb-2" :inline="false">
            {{ $t("preferences.manage_shares.outside_my_organization") }}
        </bm-label-icon>
        <template v-if="externalShares.length === 0">
            <div class="ml-4 mt-3 font-italic">{{ noExternalShareSet }}</div>
        </template>
        <template v-for="(external, index) in externalShares" v-else>
            <bm-row :key="external.token" class="align-items-center mt-2">
                <div class="col-6">
                    <bm-contact
                        v-if="external.vcard"
                        :contact="VCardInfoAdaptor.toContact(external.vcard)"
                        variant="transparent"
                    />
                    <span v-else class="font-size-lg">{{ displayedLabel(external) }}</span>
                    <div class="row mr-3 align-items-center">
                        <div class="text-neutral text-truncate col-8">{{ external.url }}</div>
                        <div class="col-4 pl-2">
                            <bm-button v-if="activeCopyBtn === index" variant="success">
                                <bm-label-icon icon="check">{{ $t("common.copied") }}</bm-label-icon>
                            </bm-button>
                            <bm-button
                                v-else
                                variant="outline-neutral"
                                @click="copyLinkInClipboard(external.url, index)"
                            >
                                <bm-label-icon icon="copy">{{ $t("common.copy") }}</bm-label-icon>
                            </bm-button>
                        </div>
                    </div>
                </div>
                <div class="col-6 d-flex">
                    <bm-form-select
                        :value="external.publishMode"
                        :options="publishModeOptions"
                        @input="editPublishMode(external)"
                    />
                    <bm-button v-if="canRemoveLink(external)" variant="inline-neutral" @click="removeLink(external)">
                        <bm-icon icon="trash" size="lg" />
                    </bm-button>
                    <bm-button v-else variant="inline-neutral" @click="regenerateLink">
                        <bm-icon icon="loop" size="lg" />
                    </bm-button>
                </div>
            </bm-row>
        </template>
    </div>
</template>

<script>
import { publishModeOptions } from "./ExternalShareHelper";
import { PublishMode } from "@bluemind/calendar.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { BmButton, BmContact, BmFormSelect, BmIcon, BmLabelIcon, BmRow } from "@bluemind/styleguide";

export default {
    name: "ExternalShareManagement",
    components: { BmButton, BmContact, BmFormSelect, BmIcon, BmLabelIcon, BmRow },
    props: {
        container: {
            type: Object,
            required: true
        },
        externalShares: {
            type: Array,
            required: true
        }
    },
    data() {
        return { activeCopyBtn: -1, publishModeOptions: publishModeOptions(inject("i18n")), VCardInfoAdaptor };
    },
    computed: {
        noExternalShareSet() {
            return this.$t("preferences.manage_shares.no_external_share_set", {
                name: this.container.name,
                type: this.$t("common.container_type_with_definite_article." + this.container.type)
            });
        }
    },
    methods: {
        copyLinkInClipboard(url, index) {
            navigator.clipboard.writeText(url);
            this.activeCopyBtn = index;
        },
        displayedLabel({ publishMode }) {
            return publishMode === PublishMode.PUBLIC ? this.$t("common.public") : this.$t("common.private");
        },
        editPublishMode(external) {
            this.activeCopyBtn = -1;
            this.$emit("publish-mode-change", external);
        },
        regenerateLink() {
            // not implemented yet, code will be close to editPublishMode method
        },
        removeLink(external) {
            this.$emit("remove", external.token);
        },
        canRemoveLink() {
            // not implemented yet
            // later, every user will have a default 'Public' link that you cant remove but only regenerate it
            return true;
        }
    }
};
</script>
