import 'dart:convert';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class AuthState {
  final bool isInitializing;
  final bool isAuthenticated;
  final String? email;
  final String? accessToken;
  final String? displayName;

  const AuthState({
    this.isInitializing = false,
    this.isAuthenticated = false,
    this.email,
    this.accessToken,
    this.displayName,
  });

  AuthState copyWith({
    bool? isInitializing,
    bool? isAuthenticated,
    String? email,
    String? accessToken,
    String? displayName,
  }) {
    return AuthState(
      isInitializing: isInitializing ?? this.isInitializing,
      isAuthenticated: isAuthenticated ?? this.isAuthenticated,
      email: email ?? this.email,
      accessToken: accessToken ?? this.accessToken,
      displayName: displayName ?? this.displayName,
    );
  }
}

class AuthNotifier extends StateNotifier<AuthState> {
  static const _storage = FlutterSecureStorage();
  static const _accessTokenKey = 'access_token';
  static const _refreshTokenKey = 'refresh_token';
  static const _userKey = 'user_data';

  int _generation = 0;

  AuthNotifier() : super(const AuthState(isInitializing: true)) {
    _restoreSession();
  }

  Future<void> _restoreSession() async {
    final myGeneration = _generation;
    try {
      final accessToken = await _storage.read(key: _accessTokenKey);
      final refreshToken = await _storage.read(key: _refreshTokenKey);
      final userJson = await _storage.read(key: _userKey);

      if (refreshToken == null || userJson == null) return;

      final user = jsonDecode(userJson) as Map<String, dynamic>;

      if (myGeneration != _generation) return;

      state = AuthState(
        isInitializing: false,
        isAuthenticated: true,
        email: user['email'] as String?,
        accessToken: accessToken,
        displayName: user['displayName'] as String?,
      );
    } catch (_) {
      await logout();
    } finally {
      if (myGeneration == _generation && state.isInitializing) {
        state = state.copyWith(isInitializing: false);
      }
    }
  }

  Future<void> login({
    required String accessToken,
    required String refreshToken,
    required String email,
    String? displayName,
  }) async {
    _generation++;

    await _storage.write(key: _accessTokenKey, value: accessToken);
    await _storage.write(key: _refreshTokenKey, value: refreshToken);
    await _storage.write(
      key: _userKey,
      value: jsonEncode({
        'email': email,
        if (displayName != null) 'displayName': displayName,
      }),
    );

    state = AuthState(
      isInitializing: false,
      isAuthenticated: true,
      email: email,
      accessToken: accessToken,
      displayName: displayName,
    );
  }

  Future<void> updateTokens({
    required String accessToken,
    required String refreshToken,
  }) async {
    await _storage.write(key: _accessTokenKey, value: accessToken);
    await _storage.write(key: _refreshTokenKey, value: refreshToken);
    state = state.copyWith(accessToken: accessToken);
  }

  Future<void> logout() async {
    _generation++;
    await _storage.deleteAll();
    state = const AuthState();
  }

  Future<String?> getRefreshToken() => _storage.read(key: _refreshTokenKey);
}

final authStateProvider = StateNotifierProvider<AuthNotifier, AuthState>(
      (ref) => AuthNotifier(),
);

final authProvider = authStateProvider;