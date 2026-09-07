class AppConfig {
  static const String appName = 'Jotly';
  static const String baseUrl = String.fromEnvironment(
    'BASE_URL',
    // defaultValue: 'http://10.0.2.2:8080',
    defaultValue: 'http://localhost:8080',
  );

  static const int jwtExpiryBufferSeconds = 60;
  static const int defaultPageSize = 20;
}