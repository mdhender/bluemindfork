import registerRequireContextHook from 'babel-plugin-require-context-hook/register';

/** Enable to use async/await in tests. */
import "babel-polyfill";

registerRequireContextHook();

