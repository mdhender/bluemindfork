<template>
    <div class="pref-tags">
        <p>{{ $t("preferences.general.tags.desc") }}</p>
        <pref-tag-modal ref="tag-editing-modal" :tag="editingTag" @updateTag="updateUserTag" />
        <div class="pref-item-width"><hr /></div>
        <pref-tags-subset
            :tags="domainTags"
            :title="$t('preferences.general.tags.subset.domain', { count: domainTags.length })"
        />
        <div class="pref-item-width"><hr /></div>
        <pref-tags-subset
            :tags="userTags"
            :title="$t('preferences.general.tags.subset.user', { count: userTags.length })"
            editable
            @remove="removeUserTag"
            @edit="
                editingTag = { ...$event };
                $refs['tag-editing-modal'].show();
            "
        />
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import { mapActions } from "vuex";
import { ERROR } from "@bluemind/alert.store";
import UUIDGenerator from "@bluemind/uuid";
import PrefTagModal from "./PrefTagModal";
import PrefTagsSubset from "./PrefTagsSubset";

export default {
    name: "PrefTags",
    components: { PrefTagModal, PrefTagsSubset },
    data() {
        return { domainTags: [], userTags: [], editingTag: {} };
    },
    async created() {
        this.domainTags = await this.fetchDomainTags();
        this.userTags = await this.fetchUserTags();
    },
    methods: {
        ...mapActions("alert", { ERROR }),
        async fetchDomainTags() {
            const containerId = "tags_" + inject("UserSession").domain;
            const domainTags = await inject("TagsPersistence", containerId)?.all();
            return domainTags.map(t => ({
                label: t.value.label,
                color: this.normalizeColor(t.value.color),
                id: t.uid
            }));
        },
        async fetchUserTags() {
            const containerId = "tags_" + inject("UserSession").userId;
            const userTags = await inject("TagsPersistence", containerId)?.all();
            return userTags.map(t => ({
                label: t.value.label,
                color: this.normalizeColor(t.value.color),
                id: t.uid,
                editable: true
            }));
        },
        async addUserTag(tag) {
            const label = tag.label.trim();
            const color = this.toStoredColor(tag.color);
            const uid = UUIDGenerator.generate();

            this.userTags.push({ label, color, id: uid, editable: true });

            try {
                const containerId = "tags_" + inject("UserSession").userId;
                await inject("TagsPersistence", containerId)?.updates({ add: [{ uid, value: { label, color } }] });
            } catch (e) {
                this.userTags.pop();
                this.error();
                throw e;
            }
        },
        async updateUserTag(tag) {
            if (!tag.id) {
                this.addUserTag(tag);
            } else {
                const tagIndex = this.userTags.findIndex(t => t.id === tag.id);
                const previous = { ...this.userTags[tagIndex] };

                this.userTags.splice(tagIndex, 1, { ...previous, ...tag });

                try {
                    const containerId = "tags_" + inject("UserSession").userId;
                    await inject("TagsPersistence", containerId)?.updates({
                        modify: [
                            { uid: tag.id, value: { label: tag.label.trim(), color: this.toStoredColor(tag.color) } }
                        ]
                    });
                } catch (e) {
                    this.userTags[tagIndex] = previous;
                    this.error();
                    throw e;
                }
            }
        },
        async removeUserTag(tag) {
            const tagIndex = this.userTags.findIndex(t => t.id === tag.id);

            this.userTags.splice(tagIndex, 1);

            try {
                const containerId = "tags_" + inject("UserSession").userId;
                await inject("TagsPersistence", containerId)?.updates({ delete: [{ uid: tag.id }] });
            } catch (e) {
                this.userTags.splice(tagIndex, 0, tag);
                this.error();
                throw e;
            }
        },
        error() {
            this.ERROR({
                alert: { name: "preferences.update", uid: "TAG_UPDATE_ERROR" },
                options: { area: "pref-right-panel", renderer: "DefaultAlert" }
            });
        },
        normalizeColor(color) {
            return color.startsWith("#") ? color : "#" + color;
        },
        toStoredColor(color) {
            return color.startsWith("#") ? color.substring(1) : color;
        }
    }
};
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-tags {
    hr {
        background-color: $alternate-light;
        position: relative;
    }
}
</style>
