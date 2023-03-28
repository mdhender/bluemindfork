import { extensions } from "@bluemind/extensions";
import camelCase from "lodash.camelcase";
import BaseUri from "./BaseUriRegExp";
import EmbeddedAppWrapper from "./EmbeddedAppWrapper";

const applications = extensions.get("net.bluemind.webapp", "application") || [];

export default applications.filter(isEmbedded).map(toRoute);

function toRoute({ name, href: route, embed: { src } }) {
    const path = route.replace(BaseUri, "");
    const sanitized = camelCase(name);
    return {
        name: `embedded:${sanitized}:root`,
        path,
        component: EmbeddedAppWrapper(`Embedded${pascalCase(sanitized)}App`, src)
    };
}

function isEmbedded(application) {
    return BaseUri.test(application.href) && application.embed;
}

function pascalCase(str) {
    return str.charAt(0).toUpperCase() + str.slice(1);
}
