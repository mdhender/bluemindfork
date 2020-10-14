# JavaScript Style Guide for BlueMind Vue Apps

## Files Management

-   Components are written in [single-file components](https://vuejs.org/v2/guide/single-file-components.html) in `components/` folder.
-   Filenames of [single-file components](https://vuejs.org/v2/guide/single-file-components.html) must always be PascalCase: `ConversationList.vue`.
-   Child components that are tightly coupled with their parent should include the parent component name as a prefix: `ConversationList.vue`, `ConversationListItem.vue`, `ConversationListSeparator.vue`

### examples

```
.
└── MailConversationList
    ├── ConversationListItemQuickActionButtons.vue
    ├── ConversationListItem.vue
    ├── ConversationListSeparator.vue
    └── ConversationList.vue
```

## Component

-   Component names should always be kebab-case in components and DOM templates: `<message-list></message-list>`.
-   Directive shorthands (`:` for `v-bind:`, `@` for `v-on:` and `#` for `v-slot`) must always be used.
-   Component names are mandatory and must always be multi-word, except for root `App` components, and built-in components provided by Vue.
-   The first child element inside a template must have a css class named with kebab-case component name: `<template><div class="message-list"></div></template>`.
-   Unit testing is a fundamental part of components development: https://vuejs.org/v2/cookbook/unit-testing-vue-components.html
-   Get the awesome [Vetur extension](https://github.com/vuejs/vetur) for [Visual Studio Code](https://code.visualstudio.com/), which provides many great features.

## State Management

-   [Vuex](https://vuex.vuejs.org/) should be preferred for global state management, instead of `this.$root` or a global event bus.

## JavaScript

-   Formatting rules are the responsibility of [prettier](https://prettier.io/docs/en/comparison.html).
-   Code quality rules are the responsibility of [eslint](https://prettier.io/docs/en/integrating-with-linters.html#eslint).

### Code convention

#### avoid `bind` and `apply`

❌ **BAD**

```js
mailboxes.forEach(mutations.addMailbox.bind(null, state));
```

✔️ **GOOD**

```js
mailboxes.forEach(mailbox => mutations.addMailbox(state, mailbox));
```

#### simple object creation

-   No function , simple object must be serializable: `expect(JSON.parse(JSON.stringify(obj)).toEqual(obj)`.
-   Avoid OOP paradigm and class syntax.
-   Use _object literal_ syntax and spread operator to modify objects.

##### examples:

❌ **BAD**

```js
function fromMailboxContainer(item) {
    const mailbox = {};
    mailbox.owner = item.owner;
    if (mailbox.type === MailboxType.USER) {
        mailbox.uid = "user." + item.name;
    } else {
        mailbox.uid = item.owner;
    }
    return mailbox;
}
```

✔️ **GOOD**

```js
function fromMailboxContainer(item) {
    const mailbox = {
        owner: item.owner
    };
    if (mailbox.type === MailboxType.USER) {
        return {
            ...mailbox,
            uid: "user." + item.name
        };
    } else {
        return {
            ...mailbox,
            uid: item.owner
        };
    }
}
```

✔️ **GOOD**

```js
function Animal(name) {
    return {
        name,
        type: "animal"
    };
}

function Dog(name) {
    return {
        ...new Animal(name),
        type: "dog"
    };
}

const alf = new Animal("alf");
const pif = new Dog("pif");
```

#### Object cloning and modification

-   Prefer pure functions, side-effects are hard to maintain.
-   Return a new object instead of modifying the properties of the object passed in parameter.
-   Prefer the [spread operator](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Spread_syntax) to `Object.assign` to clone objects.
-   Create a new object from scratch with `Object.assign` instead of modifying the original object: `Object.assign({}, original, { newprop: value })`

##### examples

❌ **BAD**

```js
function sanitize(mailbox) {
    mailbox.folders = mailbox.folders || [];
}

const mailbox = getMailbox();
sanitize(mailbox);
```

✔️ **GOOD**

```js
function sanitize(mailbox) {
    return {
        ...mailbox,
        folders: mailbox.folders || []
    };
}

const mailbox = sanitize(getMailbox());
// or
const mailbox = getMailbox();
const sanitizedMailbox = sanitize(mailbox);
```

##### within Vuex

❌ **BAD**

```js
const state = {};

function addItemById(state, item) {
    state = { ...state, [item.id]: item };
}

addItemById(state, { id: 0 }); // state n'est pas modifié
```

❌ **BAD**

```js
const state = {};

function addItemById(state, item) {
    state[item.id] = item;
}

addItemById(state, { id: 0 }); // state n'est pas réactif
```

✔️ **GOOD**

```js
const state = {};

function addItemById(state, item) {
    Object.assign(state, { [item.id]: item });
}

addItemById(state, { id: 0 }); // sans Vue.set
```

✔️ **GOOD**

```js
import Vue from "vue";
const state = {};

function addItemById(state, item) {
    Vue.set(state, item.id, item);
}

addItemById(state, { id: 0 }); // avec Vue.set
```

#### let vs const

Eslint recommands [`prefer-const`](https://eslint.org/docs/rules/prefer-const).

## Resources

-   [**BlueMind JavaScript Style Guide**: https://forge.bluemind.net/confluence/display/TRBM/Javascript+Dev+Guidelines](https://forge.bluemind.net/confluence/display/TRBM/Javascript+Dev+Guidelines)
-   [**Vue.js Style Guide**: https://vuejs.org/v2/style-guide/](https://vuejs.org/v2/style-guide/)
-   [**Google JavaScript Style Guide**: https://google.github.io/styleguide/jsguide.html](https://google.github.io/styleguide/jsguide.html)
-   [**Airbnb JavaScript Style Guide**: https://github.com/airbnb/javascript](https://github.com/airbnb/javascript)
