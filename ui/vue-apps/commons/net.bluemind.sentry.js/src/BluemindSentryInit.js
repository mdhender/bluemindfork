// import * as Sentry from "@sentry/vue";
// import { Integrations } from "@sentry/tracing";
import injector from "@bluemind/inject";

export default function initSentry() {
    const userSession = injector.getProvider("UserSession").get();
    if (userSession.sentryDsn) {
        // rewrite the url, in order to use webserver sentry filter do request
        // for us
        // let sentryDsnUrl = new URL(userSession.sentryDsn);
        // let url = new URL("/sentry" + sentryDsnUrl.pathname, window.location.origin);
        // url.username = sentryDsnUrl.username;
        // Sentry.init({
        //     Vue,
        //     dsn: url.toString(),
        //     release: userSession.sentryRelease,
        //     environment: userSession.sentryEnvironment,
        //     // Send all errors (100%) to sentry
        //     sampleRate: 1.0,
        //     integrations: [new Integrations.BrowserTracing()],
        //     // 10% sample
        //     tracesSampleRate: 0.1,
        //     tracingOptions: {
        //         trackComponents: true
        //     }
        // });
        // Sentry.setUser({ email: userSession.email });
    }
}
