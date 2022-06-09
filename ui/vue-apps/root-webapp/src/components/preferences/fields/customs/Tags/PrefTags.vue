<template>
    <div class="pref-tags">
        <p>{{ $t("preferences.general.tags.desc") }}</p>
        <pref-tag-modal ref="tag-editing-modal" :tag="editingTag" @updateTag="updateUserTag" />
        <template v-if="!collapsed">
            <div class="pref-item-width"><hr /></div>
            <pref-tags-subset
                :tags="domainTags"
                :title="$t('preferences.general.tags.subset.domain', { count: domainTags.length })"
            />
            <div class="pref-item-width"><hr /></div>
            <pref-tags-subset
                v-if="value"
                :tags="value"
                :title="$t('preferences.general.tags.subset.user', { count: value.length })"
                editable
                @remove="removeUserTag"
                @edit="
                    editingTag = { ...$event };
                    $refs['tag-editing-modal'].show();
                "
            />
        </template>
    </div>
</template>

<script>
import { inject } from "@bluemind/inject";
import UUIDGenerator from "@bluemind/uuid";

import PrefTagModal from "./PrefTagModal";
import PrefTagsSubset from "./PrefTagsSubset";
import CentralizedSaving from "../../../mixins/CentralizedSaving";

export default {
    name: "PrefTags",
    components: { PrefTagModal, PrefTagsSubset },
    mixins: [CentralizedSaving],
    data() {
        return { domainTags: [], editingTag: {} };
    },
    async created() {
        const save = async ({ state: { current, saved } }) => {
            await this.saveUserTags(current.value, saved?.value);
        };
        this.registerSaveAction(save);

        this.value = await this.fetchUserTags();
        this.domainTags = await this.fetchDomainTags();
    },
    methods: {
        async fetchDomainTags() {
            const containerId = "tags_" + inject("UserSession").domain;
            const domainTags = await inject("TagsPersistence", containerId)?.all();
            return domainTags.map(t => ({
                label: t.value.label,
                color: normalizeColor(t.value.color),
                id: t.uid
            }));
        },
        async fetchUserTags() {
            const containerId = "tags_" + inject("UserSession").userId;
            const userTags = await inject("TagsPersistence", containerId)?.all();
            return userTags.map(t => ({
                label: t.value.label,
                color: normalizeColor(t.value.color),
                id: t.uid,
                editable: true
            }));
        },
        async saveUserTags(current, previous = []) {
            const previousById = previous.reduce((result, value) => ({ ...result, [value.id]: value }), {});
            const previousIds = Object.keys(previousById);
            const currentIds = current.map(c => c.id);

            const added = current.filter(c => !previousIds.includes(c.id));
            const removed = previous.filter(p => !currentIds.includes(p.id));
            const modified = current.filter(c => previousIds.includes(c.id) && !areEqual(c, previousById[c.id]));

            const operations = {
                add: toStoredTags(added),
                modify: toStoredTags(modified),
                delete: toStoredTags(removed)
            };

            const containerId = "tags_" + inject("UserSession").userId;
            await inject("TagsPersistence", containerId)?.updates(operations);
        },
        async addUserTag(tag) {
            const label = tag.label.trim();
            const color = toStoredColor(tag.color);
            const uid = UUIDGenerator.generate();

            this.value.push({ label, color, id: uid, editable: true });
        },
        async updateUserTag(tag) {
            if (!tag.id) {
                this.addUserTag(tag);
            } else {
                const tagIndex = this.value.findIndex(t => t.id === tag.id);
                const previous = { ...this.value[tagIndex] };
                this.value.splice(tagIndex, 1, { ...previous, ...tag });
            }
        },
        async removeUserTag(tag) {
            const tagIndex = this.value.findIndex(t => t.id === tag.id);
            this.value.splice(tagIndex, 1);
        }
    }
};

function normalizeColor(color) {
    return color.startsWith("#") ? color : "#" + color;
}

function toStoredColor(color) {
    return color.startsWith("#") ? color.substring(1) : color;
}
function areEqual(tagA, tagB) {
    return tagA.id === tagB.id && tagA.color === tagB.color && tagA.label === tagB.label;
}

function toStoredTags(tags) {
    return tags.map(t => ({ uid: t.id, value: { label: t.label.trim(), color: toStoredColor(t.color) } }));
}
</script>

<style lang="scss">
@import "~@bluemind/styleguide/css/_variables";

.pref-tags {
    hr {
        background-color: $neutral-bg-lo1;
        position: relative;
    }
}
</style>
