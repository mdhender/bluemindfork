const { merge } = require("webpack-merge");
const common = require("./webpack.common.js");

module.exports = merge(common, {
    mode: "development",
    devServer: {
        allowedHosts: "all",
        client: false,
        hot: false,
        historyApiFallback: true,
        host: "0.0.0.0"
    }
});
