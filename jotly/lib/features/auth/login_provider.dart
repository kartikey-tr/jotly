import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/api_service.dart';

class LoginState {
  final bool isLoading;
  final bool otpSent;
  final String? email;
  final String? errorMessage;

  const LoginState({
    this.isLoading = false,
    this.otpSent = false,
    this.email,
    this.errorMessage,
  });

  LoginState copyWith({
    bool? isLoading,
    bool? otpSent,
    String? email,
    String? errorMessage,
  }) {
    return LoginState(
      isLoading: isLoading ?? this.isLoading,
      otpSent: otpSent ?? this.otpSent,
      email: email ?? this.email,
      errorMessage: errorMessage,
    );
  }
}

class LoginNotifier extends StateNotifier<LoginState> {
  final Ref _ref;
  LoginNotifier(this._ref) : super(const LoginState());

  Future<bool> sendOtp(String email) async {
    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final api = _ref.read(apiServiceProvider);

      // Backend expects email as a query param, not a JSON body.
      await api.post(
        '/api/auth/send-login-otp?email=${Uri.encodeQueryComponent(email)}',
      );

      state = state.copyWith(isLoading: false, otpSent: true, email: email);
      return true;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Could not send OTP. Check the email and try again.',
      );
      return false;
    }
  }

  Future<bool> verifyOtp(String otp) async {
    final email = state.email;
    if (email == null) return false;

    state = state.copyWith(isLoading: true, errorMessage: null);

    try {
      final api = _ref.read(apiServiceProvider);

      final response = await api.post('/api/auth/verify-login-otp', data: {
        'email': email,
        'otp': otp,
      });

      final data = response.data as Map<String, dynamic>;
      final accessToken = data['accessToken'] as String;
      final refreshToken = data['refreshToken'] as String;
      final responseEmail = data['email'] as String? ?? email;

      await _ref.read(authStateProvider.notifier).login(
        accessToken: accessToken,
        refreshToken: refreshToken,
        email: responseEmail,
      );

      state = state.copyWith(isLoading: false);
      return true;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Invalid or expired OTP. Please try again.',
      );
      return false;
    }
  }

  void reset() {
    state = const LoginState();
  }
}

final loginProvider = StateNotifierProvider<LoginNotifier, LoginState>(
      (ref) => LoginNotifier(ref),
);