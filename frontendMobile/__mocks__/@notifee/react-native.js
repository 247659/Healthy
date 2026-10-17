const notifee = {
  requestPermission: jest.fn(async () => ({})),
  createChannel: jest.fn(async () => 'mock-channel'),
  displayNotification: jest.fn(async () => undefined),
  cancelNotification: jest.fn(async () => undefined),
  cancelAllNotifications: jest.fn(async () => undefined),
  getInitialNotification: jest.fn(async () => null),
  onForegroundEvent: jest.fn(() => jest.fn()),
  onBackgroundEvent: jest.fn(),
};

module.exports = {
  __esModule: true,
  default: notifee,
  AndroidImportance: {
    NONE: 0,
    MIN: 1,
    LOW: 2,
    DEFAULT: 3,
    HIGH: 4,
  },
  AndroidVisibility: {
    PRIVATE: 0,
    PUBLIC: 1,
    SECRET: -1,
  },
  EventType: {
    DISMISSED: 0,
    PRESS: 1,
    ACTION_PRESS: 2,
    DELIVERED: 3,
    APP_BLOCKED: 4,
    CHANNEL_BLOCKED: 5,
    CHANNEL_GROUP_BLOCKED: 6,
    TRIGGER_NOTIFICATION_CREATED: 7,
  },
};
