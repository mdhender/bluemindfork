<template>
    <div class="bm-contact-input-autocomplete-extra d-flex flex-fill justify-content-center h-100 align-items-center">
        <span class="text-secondary font-weight-bold">
            {{
                $te("styleguide.contact-input.autocomplete.expand")
                    ? $t("styleguide.contact-input.autocomplete.expand")
                    : "Expand search"
            }}
        </span>
    </div>
</template>

<script>
export default {
    name: "BmContactInputAutocompleteExtra"
};
</script>
