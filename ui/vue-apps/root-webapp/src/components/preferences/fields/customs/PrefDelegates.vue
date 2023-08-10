<template>
    <div>
        <p>{{ $t("preferences.account.delegates.description") }}</p>
        <p v-if="!hasDelegates">{{ $t("preferences.account.delegates.none") }}</p>
        <bm-button icon="plus" variant="outline">{{ $t("preferences.account.delegates.create") }}</bm-button>
    </div>
</template>

<script>
import { Verb } from "@bluemind/core.container.api";
import { BmButton } from "@bluemind/ui-components";

const hasDelegationRightFn = container => container.verbs.some(v => v === Verb.SendAs || v === Verb.SendOnBehalf);

export default {
    name: "PrefDelegates",
    components: { BmButton },
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
        }
    }
};
</script>
