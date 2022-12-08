export default {
    data() {
        return {
            /** Enable to navigate between items using the Tab key. */
            tabNavigation: true
        };
    },
    methods: {
        focusFirst() {
            if (this.$_Container_focusables.length > 0) {
                return this.focusByIndex(0);
            }
        },
        focusLast() {
            if (this.$_Container_focusables.length > 0) {
                return this.focusByIndex(this.$_Container_focusables.length - 1);
            }
        },
        focusNext() {
            if (this.$el.contains(document.activeElement)) {
                const focusables = this.$_Container_focusables;
                if (focusables.length > 1) {
                    let i = indexOfFocusableElement(document.activeElement, this);
                    do {
                        i++;
                    } while (i < focusables.length && !isVisible(focusables[i]));
                    return this.focusByIndex(i);
                }
            }
        },
        focusPrevious() {
            if (this.$el.contains(document.activeElement)) {
                const focusables = this.$_Container_focusables;
                if (focusables.length > 1) {
                    let i = focusables.findIndex(focusable => focusable.contains(document.activeElement));
                    do {
                        i--;
                    } while (i >= 0 && !isVisible(focusables[i]));
                    return this.focusByIndex(i);
                }
            }
        },
        focusByIndex(i) {
            if (i >= 0 && i < this.$_Container_focusables.length) {
                this.$_Container_focusables[i].focus();
                return this.$_Container_focusables[i];
            }
        },
        focusByKey(key) {
            key = key.toString();
            const element = this.$_Container_focusables.find(element => element.dataset.browseKey === key);
            if (element) {
                element.focus();
                return element;
            }
        },
        focus() {
            this.$_Container_focused.focus();
            return this.$_Container_focused;
        }
    },
    mounted() {
        init(this);
        this.$el.addEventListener("keydown", e => handleNavigationKey(e, this));
        this.$el.addEventListener("focusin", e => handleFocus(e, this));
        this.$el.addEventListener("focusout", e => handleBlur(e, this));
    },
    updated() {
        init(this);
    }
};

let shift = false,
    ctrl = false;

document.addEventListener("keydown", e => handleMetaKey(e, this));
document.addEventListener("keyup", e => handleMetaKey(e, this));

function init(vm) {
    vm.$_Container_focusables = getFocusableChildren(vm.$el);
    if (vm.$_Container_focusables.length > 0) {
        vm.$_Container_focusables.sort((a, b) => (a.dataset.browseIndex || 99) - (b.dataset.browseIndex || 99));
        vm.$_Container_focused = vm.$_Container_focusables.find(el => el.dataset.browseDefault !== undefined);
        if (!vm.$_Container_focused) {
            vm.$_Container_focused = vm.$_Container_focusables.find(el => el.tabIndex === 0);
        }
        if (!vm.$_Container_focused) {
            vm.$_Container_focused = vm.$_Container_focusables[0];
        }
        vm.$_Container_focusables.forEach(element => (element.tabIndex = "-1"));
        vm.$_Container_focused.tabIndex = "0";
    }
}

function handleBlur(event, vm) {
    if (!vm.$el.contains(document.activeElement) && !vm.$el.contains(event.relatedTarget)) {
        vm.$emit("browse:blur");
    }
}

function handleFocus(event, vm) {
    if (vm.$el.contains(document.activeElement) && document.activeElement.dataset.browse !== undefined) {
        vm.$_Container_focusables.forEach(element => (element.tabIndex = "-1"));
        vm.$_Container_focused = document.activeElement;
        document.activeElement.tabIndex = "0";
        const e = {
            target: document.activeElement,
            key: document.activeElement.dataset.browseKey,
            shift,
            ctrl
        };
        vm.$emit("browse:focus", e);
    }
}

function handleNavigationKey(event, vm) {
    switch (event.key) {
        case "ArrowLeft":
        case "Left":
            vm.focusPrevious();
            break;
        case "ArrowRight":
        case "Right":
            vm.focusNext();
            break;
        case "Home":
            vm.focusFirst();
            event.preventDefault();
            break;
        case "End":
            vm.focusLast();
            event.preventDefault();
            break;
        case "Tab":
            if (vm.tabNavigation) {
                if (event.shiftKey) {
                    vm.focusPrevious() && event.preventDefault();
                } else {
                    vm.focusNext() && event.preventDefault();
                }
            }
            break;
    }
}

function handleMetaKey(event) {
    shift = event.shiftKey;
    ctrl = event.ctrlKey;
}

function getFocusableChildren(element) {
    let focusables = [];
    for (let i = 0; i < element.children.length; i++) {
        const el = element.children.item(i);
        //Fixme : isFocusable coule be suffisant. How to replace browableKey ?
        if (el.dataset.browse !== undefined) {
            let focusable;
            if (!isFocusable(el) && (focusable = getFocusableChild(el))) {
                focusable.dataset.browse = true;
                if (el.dataset.browseDefault !== undefined) {
                    focusable.dataset.browseDefault = true;
                }
                if (el.dataset.browseIndex) {
                    focusable.dataset.browseIndex = el.dataset.browseIndex;
                }
                if (el.dataset.browseKey) {
                    focusable.dataset.browseKey = el.dataset.browseKey;
                }
            }
            focusables.push(focusable || el);
        } else if (el.children) {
            focusables = focusables.concat(getFocusableChildren(el));
        }
    }
    return focusables;
}

function indexOfFocusableElement(element, vm) {
    return vm.$_Container_focusables.findIndex(focusable => focusable.contains(element));
}

//FIXME : Should use ally.js
function getFocusableChild(element) {
    for (let i = 0; i < element.children.length; i++) {
        const el = element.children.item(i);
        if (isFocusable(el)) {
            return el;
        } else if (el.children) {
            return getFocusableChild(el);
        }
    }
    return null;
}

function isFocusable(element) {
    const tagName = element.tagName.toLowerCase();
    return (
        (element.getAttribute("tabindex") || tagName === "input") &&
        isVisible(element) &&
        element.getAttribute("aria-hidden") !== "true"
    );
}

let ignoreVisibility = false;

export function setIgnoreVisibility(b) {
    ignoreVisibility = b;
}

function isVisible(element) {
    if (ignoreVisibility) {
        return true;
    }
    const visible = element.offsetWidth && element.offsetHeight && element.getClientRects().length;
    return visible && (!element.parentElement || isVisible(element.parentElement));
}
