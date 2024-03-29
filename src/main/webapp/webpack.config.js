const path = require('path')
const { VueLoaderPlugin } = require('vue-loader')
const isDev = process.env.NODE_ENV === 'dev'

module.exports = {
  entry: './app/main.js',
  output: {
    path: path.resolve(__dirname, '../../../target/generated-resources/vue-app'),
    publicPath: '/vue-components/dist/', filename: 'ui.js'
  },
  module: {
    rules: [
      { test: /\.css$/, use: [ 'vue-style-loader', 'css-loader' ] },
      { test: /\.vue$/, loader: 'vue-loader' },
      { test: /\.js$/, exclude: /node_modules/, loader: 'babel-loader' },
    ]
  },
  resolve: {
    alias: {
      'vue': 'vue/dist/vue.esm-bundler.js'
    },
    extensions: ['*', '.js', '.vue', '.json']
  },
  mode: isDev ? 'development' : 'production',
  plugins: [
    new VueLoaderPlugin()
  ]
};
