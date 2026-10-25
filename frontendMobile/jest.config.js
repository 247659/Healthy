module.exports = {
  preset: '@react-native/jest-preset',

  transformIgnorePatterns: [
    'node_modules/(?!(@react-native-async-storage/async-storage|react-native-date-picker|@react-native|react-native|@react-navigation)/)',
  ],
};
