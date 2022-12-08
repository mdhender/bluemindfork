<template>
    <div
        class="bm-contact-input d-flex"
        :class="{ disabled, underline: variant === 'underline', inline: variant === 'inline' }"
    >
        <label :for="makeUniq('new')" class="bm-contact-input-label"><slot /></label>
        <div class="flex-fill d-flex">
            <div
                v-overflown-elements
                class="flex-fill d-flex"
                :class="collapsed ? 'flex-nowrap overflow-hidden' : 'flex-wrap'"
                @overflown="hideContactsIfApplicable"
            >
                <div
                    v-for="(contact, index) in contacts_"
                    :key="contact.key"
                    class="align-items-center d-inline-flex contact"
                    :class="{ 'flex-fill': contact.edit }"
                >
                    <div
                        :class="{ active: contact.selected, 'flex-fill': contact.edit }"
                        :data-browse-key="contact.key"
                        data-browse
                        class="d-inline-flex contact-wrapper align-items-center mw-100"
                        tabindex="-1"
                        @keydown.enter.prevent="edit(contact)"
                        @keydown.delete="onRemove(contact)"
                        @keydown.down="$emit('expand', index)"
                        @dblclick="edit(contact)"
                    >
                        <bm-contact
                            v-if="!contact.edit"
                            :contact="contact"
                            :invalid="!valid(contact)"
                            :selected="valid(contact) && contact.selected"
                            :show-address="valid(contact) && (contact.hasBeenEdited || anyContactHasSameDn(contact))"
                            :closeable="!disabled"
                            class="mw-100"
                            @remove="onRemove(contact)"
                            @expand="$emit('expand', index)"
                        />
                        <bm-form-autocomplete-input
                            v-else
                            v-model="contact.input"
                            size="sm"
                            :items="canDisplayAutocomplete ? autocompleteResults : []"
                            :class="{ beingEdited: 'flex-fill' }"
                            @click.native="showAutocomplete(true)"
                            @selected="selectedContact => editFromAutocomplete(contact, selectedContact)"
                            @submit="submit(contact)"
                            @keydown.native.delete.stop
                            @keydown.native.left="preventPrevious"
                            @keydown.native.right="preventNext"
                            @keydown.native.backspace="forcePrevious"
                            @keydown.native.esc="cancel(contact)"
                            @keydown.native.,.prevent="focusNext"
                            @keydown.native.;.prevent="focusNext"
                            @autocompleteShown="$emit('autocompleteShown')"
                            @autocompleteHidden="$emit('autocompleteHidden')"
                        >
                            <template #default="{ item }">
                                <bm-contact-input-autocomplete-item
                                    :contact="item"
                                    :input-value="contact.input"
                                    @delete="$emit('delete', item)"
                                />
                            </template>
                            <template #extra>
                                <bm-contact-input-autocomplete-extra
                                    v-if="showExpand"
                                    @click.native="$emit('expandSearch')"
                                />
                            </template>
                        </bm-form-autocomplete-input>
                    </div>
                </div>
                <bm-form-autocomplete-input
                    v-show="!hiddenContactCount && contacts_.length < maxContacts"
                    ref="new"
                    v-model="value"
                    size="sm"
                    :items="canDisplayAutocomplete ? autocompleteResults : []"
                    :class="{ 'flex-fill': !beingEdited }"
                    :disabled="disabled"
                    data-browse
                    data-browse-default
                    data-browse-key="new"
                    @click.native="showAutocomplete(true)"
                    @focus="showAutocomplete(true)"
                    @selected="createFromAutocomplete"
                    @submit="create"
                    @keydown.native.left="preventPrevious"
                    @keydown.native.right="preventNext"
                    @keydown.native.backspace="forcePrevious"
                    @keydown.native.esc="collapsed = autoCollapsible"
                    @keydown.native.,.prevent="create"
                    @keydown.native.;.prevent="create"
                    @keydown.native.space="value.includes('@') && forceCreate($event)"
                    @autocompleteShown="$emit('autocompleteShown')"
                    @autocompleteHidden="$emit('autocompleteHidden')"
                >
                    <template #default="{ item }">
                        <bm-contact-input-autocomplete-item
                            :contact="item"
                            :input-value="value"
                            @delete="$emit('delete', item)"
                        />
                    </template>
                    <template #extra>
                        <bm-contact-input-autocomplete-extra v-if="showExpand" @click.native="$emit('expandSearch')" />
                    </template>
                </bm-form-autocomplete-input>
            </div>
            <bm-more-items-badge
                ref="more-items-badge"
                :active="collapsed"
                :count="hiddenContactCount"
                @click="
                    requestFocusOnInput = true;
                    collapsed = false;
                "
            />
        </div>
    </div>
</template>

<script>
import BmContact from "../BmContact";
import BmContactInputAutocompleteExtra from "./BmContactInputAutocompleteExtra";
import BmContactInputAutocompleteItem from "./BmContactInputAutocompleteItem";
import BmFormAutocompleteInput from "./BmFormAutocompleteInput";
import BmMoreItemsBadge from "../BmMoreItemsBadge";
import BrowsableContainer from "../../mixins/BrowsableContainer";
import MakeUniq from "../../mixins/MakeUniq";
import OverflownElements from "../../directives/OverflownElements";

let key = 0;

function toItem({ address, dn, kind, members, uid, urn }) {
    return {
        address,
        dn,
        edit: false,
        hasBeenEdited: false,
        input: kind === "group" ? dn : address,
        key: key++,
        kind,
        members,
        selected: false,
        uid,
        urn
    };
}

export default {
    name: "BmContactInput",
    components: {
        BmContact,
        BmContactInputAutocompleteExtra,
        BmContactInputAutocompleteItem,
        BmFormAutocompleteInput,
        BmMoreItemsBadge
    },
    directives: { OverflownElements },
    mixins: [BrowsableContainer, MakeUniq],
    props: {
        autoCollapsible: { type: Boolean, default: true },
        autocompleteResults: { type: Array, default: () => [] },
        contacts: { type: Array, default: () => [] },
        disabled: { type: Boolean, default: false },
        maxContacts: { type: Number, default: Number.MAX_VALUE },
        showExpand: { type: Boolean, default: false },
        validateAddressFn: { type: Function, required: true },
        variant: {
            type: String,
            default: "outline",
            validator: function (value) {
                return ["outline", "underline", "inline"].includes(value);
            }
        }
    },
    data() {
        return {
            canDisplayAutocomplete: false,
            collapsed: this.autoCollapsible,
            contacts_: this.contacts.map(toItem),
            hiddenContactCount: 0,
            requestFocusOnInput: false,
            value: ""
        };
    },
    computed: {
        current() {
            return this.contacts_.find(contact => contact.selected);
        },
        beingEdited() {
            return this.contacts_.find(contact => contact.edit);
        }
    },
    watch: {
        contacts_: {
            handler: function () {
                const model = this.contacts_,
                    input = this.contacts;
                if (input.length !== model.length || input.some((contact, i) => !sameContact(model[i], contact))) {
                    this.$emit(
                        "update:contacts",
                        model.map(c => {
                            const contact = { ...c };
                            delete contact.edit;
                            delete contact.hasBeenEdited;
                            delete contact.input;
                            delete contact.selected;
                            return contact;
                        })
                    );
                }
                const editedContact = model.find(contact => contact.edit);
                if (editedContact !== undefined) {
                    this.showAutocomplete(true);
                    this.$emit("search", editedContact.input);
                }
            },
            deep: true
        },
        contacts() {
            const model = this.contacts_,
                input = this.contacts;
            if (input.length !== model.length || input.some((contact, i) => !sameContact(model[i], contact))) {
                this.contacts_ = input.map(toItem);
            }
        },
        "current.edit": function (value, old) {
            if (this.current && value !== old) {
                if (value) {
                    this.$nextTick(() => {
                        this.focusByKey(
                            this.current.key
                        ).firstElementChild.firstElementChild.firstElementChild.select();
                    });
                } else {
                    this.focusByKey(this.current.key);
                }
            }
        },
        value() {
            if (this.value !== "") {
                this.showAutocomplete(true);
            }
            this.$emit("search", this.value);
        }
    },
    created() {
        this.$on("browse:focus", e => {
            const key = parseInt(e.key);
            if (key >= 0) {
                this.select(this.contacts_.find(contact => contact.key === key));
            } else if (this.current) {
                this.unselect(this.current);
            }
        });
        this.$on("browse:blur", () => {
            if (this.current) {
                this.unselect(this.current);
            }
            this.create();
            if (this.autoCollapsible) {
                this.collapsed = true;
            }
        });
    },
    updated() {
        if (!this.collapsed && this.requestFocusOnInput && isVisible(this.$refs.new?.$el)) {
            this.$refs.new.focus();
            this.requestFocusOnInput = false;
        }
    },
    methods: {
        // 1st level methods (Model level)
        valid(contact) {
            if (!this.validateAddressFn) {
                return true;
            }
            return this.validateAddressFn(contact.input, contact);
        },
        add(contact) {
            if ((contact.kind === "group" && contact.dn) || contact.address.trim()) {
                this.contacts_.push(toItem(contact));
            }
        },
        update(contact) {
            if (contact.kind === "group") {
                contact.dn = contact.input.trim();
                if (contact.dn === "") {
                    this.remove(contact);
                }
            } else {
                contact.address = contact.input.trim();
                if (contact.address === "") {
                    this.remove(contact);
                }
            }
        },
        remove(contact) {
            if (contact.selected) {
                contact.select = false;
            }
            this.contacts_.splice(this.contacts_.indexOf(contact), 1);
        },
        // 2nd level methods (UI level)
        select(contact) {
            if (!contact.selected) {
                if (this.current) {
                    this.unselect(this.current);
                }
                contact.selected = true;
            }
        },
        unselect(contact) {
            if (contact.selected) {
                if (contact.edit) {
                    this.submit(contact);
                } else {
                    contact.selected = false;
                }
            }
        },
        edit(contact) {
            if (this.disabled) {
                return;
            }
            contact.input = contact.kind === "group" ? contact.dn : contact.address;
            contact.edit = true;
        },
        cancel(contact) {
            contact.edit = false;
        },
        submit(contact) {
            contact.edit = false;
            contact.hasBeenEdited =
                (contact.kind === "group" && contact.input !== contact.dn) || contact.input !== contact.address;
            this.update(contact);
            this.$refs.new?.focus();
            this.showAutocomplete(false);
            this.$emit("search", "");
        },
        create() {
            if (this.value.trim() !== "") {
                const result = this.getDnAndAddress(this.value);
                this.add({ address: result.address, dn: result.dn }); // FIXME: find dn on server when user enter email address directly ?
                this.afterCreate();
            }
        },
        getDnAndAddress(value) {
            const DN_ADDRESS_REGEX = /(?:"?([^"]*)"?\s*<)?(.+@[^>]+)>?/;
            const matches = value.match(DN_ADDRESS_REGEX);
            if (matches) {
                const [dnMatch, addressMatch] = matches.slice(1);
                const dn = dnMatch ? dnMatch.trim() : "";
                const address = addressMatch.trim();
                return { dn, address };
            }
            return { dn: "", address: value };
        },
        async afterCreate() {
            await this.$nextTick();
            this.value = "";
            this.showAutocomplete(false);
            this.$refs.new?.focus();
        },
        anyContactHasSameDn(contact) {
            const filteredByDn = this.contacts.filter(c => c.dn === contact.dn);
            return filteredByDn.length >= 2;
        },

        // 3th level methods (Event level)
        preventNext(event) {
            if (
                event.target.selectionStart !== event.target.selectionEnd ||
                event.target.selectionStart < event.target.value.length
            ) {
                event.stopPropagation();
            }
        },
        preventPrevious(event) {
            if (event.target.selectionStart !== event.target.selectionEnd || event.target.selectionStart > 0) {
                event.stopPropagation();
            }
        },
        forcePrevious(event) {
            if (event.target.value.length === 0) {
                event.preventDefault();
                event.stopPropagation();
                this.focusPrevious();
                if (this.current && !this.current.edit) {
                    this.$nextTick(() => this.edit(this.current));
                }
            }
        },
        forceCreate(event) {
            if (this.value.trim() !== "") {
                event.preventDefault();
                event.stopPropagation();
                this.create();
            }
        },
        onRemove(contact) {
            if (this.disabled) {
                return;
            }
            if (contact.selected) {
                this.focusNext();
            }
            this.remove(contact);
            this.$refs.new.focus();
        },

        // Autocomplete methods
        createFromAutocomplete(contact) {
            this.add(contact);
            this.afterCreate();
            this.$emit("search", "");
        },
        editFromAutocomplete(contact, selectedContact) {
            contact.address = selectedContact.address;
            contact.dn = selectedContact.dn;
            contact.input = selectedContact.kind === "group" ? selectedContact.dn : selectedContact.address;
            contact.kind = selectedContact.kind;
            contact.members = selectedContact.members;
            this.submit(contact);
        },
        showAutocomplete(show) {
            this.canDisplayAutocomplete = show;
            this.collapsed = show ? false : this.collapsed;
        },
        hideContactsIfApplicable(overflownEvent) {
            if (this.autoCollapsible) {
                this.hideContacts(overflownEvent);
            }
        },
        hideContacts(overflownEvent) {
            const badge = this.$refs["more-items-badge"];
            if (badge) {
                this.hiddenContactCount = badge.hideOverflownElements({ overflownEvent, elementClass: "contact" });
            }
        }
    }
};

function sameContact(c1, c2) {
    return (
        c1.kind === c2.kind &&
        ((!c1.members && !c2.members) || c1.members?.every(m1 => c2.members?.some(m2 => m2.address === m1.address))) &&
        c1.address === c2.address
    );
}

function isVisible(element) {
    return element.offsetWidth && element.offsetHeight && element.getClientRects().length;
}
</script>

<style lang="scss">
@use "sass:math";
@import "../../css/_variables.scss";
@import "../../css/_type.scss";

.bm-contact-input {
    border: $input-border-width solid $neutral-fg;

    &.underline {
        border-color: $neutral-fg-lo2;
        border-radius: 0;
        border-top-color: transparent !important; // keep it to simplify vertical padding management
        border-left: none;
        border-right: none;
    }
    &.inline {
        border-color: transparent !important; // keep it to simplify padding management
    }

    padding: $input-border-width;
    &.underline {
        padding-left: 2 * $input-border-width;
        padding-right: 2 * $input-border-width;
    }

    label {
        color: $neutral-fg;
    }

    &:hover {
        border-color: $neutral-fg-hi1;
        &.underline {
            border-color: $neutral-fg;
        }
        label {
            color: $neutral-fg-hi1;
        }
    }
    &:focus-within {
        border-width: 2 * $input-border-width;
        border-color: $secondary-fg;
        &.underline {
            border-color: $secondary-fg;
        }
        padding-top: 0;
        padding-bottom: 0;
        &:not(.underline) {
            padding-left: 0;
            padding-right: 0;
        }
        label {
            color: $neutral-fg-hi1;
        }
    }
    &.disabled {
        border-color: $neutral-fg-disabled;
        &.underline {
            border-color: transparent;
        }
    }

    .contact,
    .bm-form-input,
    .bm-form-input .form-control {
        height: calc(#{$input-height} - #{4 * $input-border-width});
        padding-top: 0 !important;
        padding-bottom: 0 !important;
    }

    .bm-contact {
        cursor: pointer;
    }

    &.disabled {
        border-color: $neutral-fg-disabled !important;
    }

    .bm-contact-input-label:not(:empty) {
        @extend %bold;
        margin-top: base-px-to-rem(8);
        margin-bottom: 0;
        margin-right: $sp-3;
        padding-left: 0.75rem;
        padding-right: $sp-1;
    }
    input,
    input:focus {
        border: unset;
        box-shadow: none;
    }
    & > div,
    & > div > span:not(.bm-badge) {
        // need this to enable the text-truncate feature in children
        // @see https://css-tricks.com/flexbox-truncated-text/
        min-width: 0;
    }

    .list-group {
        width: auto;
    }
    input {
        width: 100% !important;
    }

    $inputMinWidth: 10vw;

    .bm-form-autocomplete-input {
        min-width: $inputMinWidth;

        .suggestions {
            border: none;
            padding: $sp-2;

            .list-group-item {
                height: $input-height + $sp-2 + $line-height-small;
                .bm-contact-input-autocomplete-item {
                    flex: 1;
                }
                padding: 0;
                margin: 0;
            }
        }

        .list-group-separator {
            display: none;
        }
    }

    .bm-contact {
        display: inline-flex;
        align-items: center;
        margin: $sp-2 + $sp-1 $sp-3;
    }

    .contact {
        max-width: calc(100% - $inputMinWidth);
    }

    .contact-wrapper:focus {
        outline: $outline;
    }
}
</style>
