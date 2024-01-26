import Vue from "vue";
import { default as BmExtension } from "./BmExtension";
import { default as BmExtensionDecorator } from "./BmExtensionDecorator";
import { default as BmExtensionRenderless } from "./BmExtensionRenderless";
import useExtensions from "./composables/extensions";

Vue.component("BmExtensionDecorator", BmExtensionDecorator);
Vue.component("BmExtensionRenderless", BmExtensionRenderless);

export { BmExtension, useExtensions };
