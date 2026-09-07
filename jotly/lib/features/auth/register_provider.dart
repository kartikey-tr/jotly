import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../core/providers/auth_provider.dart';
import '../../core/services/api_service.dart';


class RegisterState {
  final bool isLoading;
  final bool otpSent;
  final String? email;
  final String? displayName;
  final String? errorMessage;

  const RegisterState({
    this.isLoading = false,
    this.otpSent = false,
    this.email,
    this.displayName,
    this.errorMessage,
  });

  RegisterState copyWith({
    bool? isLoading,
    bool? otpSent,
    String? email,
    String? displayName,
    String? errorMessage,
  }) {
    return RegisterState(
      isLoading: isLoading ?? this.isLoading,
      otpSent: otpSent ?? this.otpSent,
      email: email ?? this.email,
      displayName: displayName ?? this.displayName,
      errorMessage: errorMessage,
    );
  }
}

class RegisterNotifier extends StateNotifier<RegisterState> {
  final Ref _ref;

  RegisterNotifier(this._ref) : super(const RegisterState());

  Future<bool> sendOtp({
    required String email,
    required String displayName,
    Uint8List? avatarBytes,
  }) async {
    state = state.copyWith(isLoading: true, errorMessage: null);
    try {
      final api = _ref.read(apiServiceProvider);

      final formData = FormData.fromMap({
        'name': displayName,
        'email': email,
        if (avatarBytes != null)
          'profilePhoto': MultipartFile.fromBytes(
            avatarBytes,
            filename: 'avatar.jpg',
          ),
      });

      await api.post('/api/auth/register', data: formData);

      state = state.copyWith(
        isLoading: false,
        otpSent: true,
        email: email,
        displayName: displayName,
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        isLoading: false,
        errorMessage: 'Could not create your account. Please try again.',
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
      final response =
      await api.post('/api/auth/verify-registration-otp', data: {
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
        displayName: state.displayName,
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
    state = const RegisterState();
  }
}

final registerProvider =
StateNotifierProvider<RegisterNotifier, RegisterState>(
      (ref) => RegisterNotifier(ref),
);