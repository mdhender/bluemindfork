<template>
    <div class="external-share-management">
        <hr />
        <bm-label-icon icon="world" class="font-weight-bold mb-2" :inline="false">
            {{ $t("preferences.manage_shares.outside_my_organization") }}
        </bm-label-icon>
        <div v-if="externalShares.length === 0" class="ml-4 mt-3 font-italic">{{ noExternalShareSet }}</div>
        <div v-for="external in externalShares" v-else :key="external.token" class="share-entry">
            <div class="share-entry-title">
                <bm-contact
                    v-if="external.vcard"
                    :contact="VCardInfoAdaptor.toContact(external.vcard)"
                    transparent
                    bold-dn
                    show-address
                />
                <span v-else class="font-size-lg">{{ displayedLabel(external) }}</span>
            </div>
            <bm-row class="share-entry-body">
                <div class="share-entry-col url-and-copy-button">
                    <div class="share-url text-truncate">{{ external.url }}</div>
                    <bm-button-copy variant="text" size="lg" :content-provider="() => external.url" />
                </div>
                <div class="share-entry-col select-and-button">
                    <bm-form-select
                        :value="external.publishMode"
                        :options="publishModeOptions"
                        :auto-min-width="false"
                        @input="editPublishMode(external)"
                    />
                    <bm-icon-button
                        v-if="canRemoveLink(external)"
                        size="sm"
                        icon="trash"
                        @click="removeLink(external)"
                    />
                    <bm-icon-button v-else size="sm" icon="loop" @click="regenerateLink" />
                </div>
            </bm-row>
        </div>
    </div>
</template>

<script>
import { publishModeOptions } from "./ExternalShareHelper";
import { PublishMode } from "@bluemind/calendar.api";
import { VCardInfoAdaptor } from "@bluemind/contact";
import { inject } from "@bluemind/inject";
import { BmButtonCopy, BmIconButton, BmContact, BmFormSelect, BmLabelIcon, BmRow } from "@bluemind/styleguide";

export default {
    name: "ExternalShareManagement",
    components: { BmButtonCopy, BmIconButton, BmContact, BmFormSelect, BmLabelIcon, BmRow },
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
        return { publishModeOptions: publishModeOptions(inject("i18n")), VCardInfoAdaptor };
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
        displayedLabel({ publishMode }) {
            return publishMode === PublishMode.PUBLIC ? this.$t("common.public") : this.$t("common.private");
        },
        editPublishMode(external) {
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

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.external-share-management {
    .share-entry {
        margin-bottom: $sp-3;

        .share-entry-title {
            height: 1.5rem;
            display: flex;
            align-items: flex-end;

            .bm-contact {
                max-width: 100%;
            }
        }

        .share-entry-body {
            .url-and-copy-button {
                margin: $sp-1 0;
                display: flex;
                align-items: center;

                .share-url {
                    color: $neutral-fg;
                    flex: 1;
                    margin-right: $sp-1;
                }
                .btn {
                    flex: none;
                }
            }

            .select-and-button {
                display: flex;

                .bm-form-select {
                    min-width: 0;
                    flex: 1;
                    margin-right: $sp-1;
                }
                .btn {
                    flex: none;
                }
            }
        }
    }
}
</style>
