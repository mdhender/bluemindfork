<template>
    <div class="pref-delegates">
        <p>{{ $t("preferences.account.delegates.description") }}</p>
        <p v-if="!hasDelegates">{{ $t("preferences.account.delegates.none") }}</p>
        <bm-button icon="plus" variant="outline" @click="showEditForm = true">
            {{ $t("preferences.account.delegates.create") }}
        </bm-button>
        <pref-delegates-modal
            :visible.sync="showEditForm"
            :delegated-containers="delegatedContainers"
            :delegate="delegate"
        />
    </div>
</template>

<script>
import { Verb } from "@bluemind/core.container.api";
import { BmButton } from "@bluemind/ui-components";
import PrefDelegatesModal from "./PrefDelegatesModal";

const hasDelegationRightFn = container => container.verbs.some(v => v === Verb.SendAs || v === Verb.SendOnBehalf);

export default {
    name: "PrefDelegates",
    components: { BmButton, PrefDelegatesModal },
    data() {
        return { delegate: undefined, selected: undefined, showEditForm: false };
    },
    computed: {
        delegatedAddressBooks() {
            return this.$store.state.preferences.containers.otherAddressbooks.filter(hasDelegationRightFn);
        },
        delegatedCalendars() {
            return this.$store.state.preferences.containers.otherCalendars.filter(hasDelegationRightFn);
        },
        delegatedMailboxes() {
            return this.$store.state.preferences.containers.otherMailboxesContainers.filter(hasDelegationRightFn);
        },
        delegatedTodoLists() {
            return this.$store.state.preferences.containers.otherTodoLists.filter(hasDelegationRightFn);
        },
        hasDelegates() {
            return Boolean(
                this.delegatedAddressBooks.length ||
                    this.delegatedCalendars.length ||
                    this.delegatedMailboxes.length ||
                    this.delegatedTodoLists.length
            );
        },
        delegatedContainers() {
            if (this.delegate) {
                return {
                    addressBooks: this.delegatedAddressBooks?.filter(container => container.owner === this.delegate),
                    calendars: this.delegatedCalendars?.filter(container => container.owner === this.delegate),
                    mailboxes: this.delegatedMailboxes?.filter(container => container.owner === this.delegate),
                    todoLists: this.delegatedTodoList?.filter(container => container.owner === this.delegate)
                };
            }
            return undefined;
        }
    }
};
</script>
