import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'login_provider.dart';

class _LoginColors {
  static const primaryContainer = Color(0xFF2A14B4);
  static const onPrimary = Color(0xFFFFFFFF);
  static const surface = Color(0xFFF9F9FC);
  static const surfaceContainerLowest = Color(0xFFFFFFFF);
  static const surfaceContainerLow = Color(0xFFF3F3F6);
  static const onSurface = Color(0xFF1A1C1E);
  static const onSurfaceVariant = Color(0xFF464554);
  static const outline = Color(0xFF777586);
  static const error = Color(0xFFBA1A1A);
}

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _otpController = TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _otpController.dispose();
    super.dispose();
  }

  Future<void> _sendOtp() async {
    if (!_formKey.currentState!.validate()) return;
    await ref.read(loginProvider.notifier).sendOtp(_emailController.text.trim());
  }

  Future<void> _verifyOtp() async {
    if (!_formKey.currentState!.validate()) return;

    final success =
    await ref.read(loginProvider.notifier).verifyOtp(_otpController.text.trim());

    if (success && mounted) {
      context.go('/home');
    }
  }

  void _changeEmail() {
    _otpController.clear();
    ref.read(loginProvider.notifier).reset();
  }

  void _goToRegister() {
    context.push('/register');
  }

  InputDecoration _fieldDecoration({
    required String hint,
    required IconData icon,
  }) {
    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(
        color: _LoginColors.outline,
        fontSize: 14,
      ),
      prefixIcon: Icon(icon, size: 18, color: _LoginColors.onSurfaceVariant),
      filled: true,
      fillColor: _LoginColors.surfaceContainerLow,
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide.none,
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide.none,
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide.none,
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _LoginColors.error, width: 1),
      ),
    );
  }

  Widget _fieldLabel(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w500,
          color: _LoginColors.onSurface,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final loginState = ref.watch(loginProvider);
    final otpSent = loginState.otpSent;

    return Scaffold(
      backgroundColor: _LoginColors.surface,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 24),

                  Column(
                    children: [
                      Container(
                        width: 64,
                        height: 64,
                        padding: const EdgeInsets.all(4),
                        decoration: BoxDecoration(
                          color: _LoginColors.surfaceContainerLowest,
                          borderRadius: BorderRadius.circular(16),
                          boxShadow: const [
                            BoxShadow(
                              color: Color(0x14000000),
                              blurRadius: 8,
                              offset: Offset(0, 2),
                            ),
                          ],
                        ),
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(12),
                          child: Image.asset(
                            'assets/images/logo.png',
                            fit: BoxFit.contain,
                            errorBuilder: (_, __, ___) => const Icon(
                              Icons.description_outlined,
                              color: _LoginColors.primaryContainer,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      const Text(
                        'Welcome back',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.w600,
                          letterSpacing: -0.2,
                          color: _LoginColors.onSurface,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Padding(
                        padding: EdgeInsets.symmetric(horizontal: 8),
                        child: Text(
                          'Sign in to sync your notes across devices.',
                          textAlign: TextAlign.center,
                          style: TextStyle(
                            fontSize: 14,
                            color: _LoginColors.onSurfaceVariant,
                          ),
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 20),

                  Container(
                    padding: const EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: _LoginColors.surfaceContainerLowest,
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: const [
                        BoxShadow(
                          color: Color(0x0F000000),
                          blurRadius: 6,
                          offset: Offset(0, 1),
                        ),
                      ],
                    ),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          _fieldLabel('Email address'),
                          TextFormField(
                            controller: _emailController,
                            enabled: !otpSent,
                            keyboardType: TextInputType.emailAddress,
                            style: const TextStyle(
                              fontSize: 14,
                              color: _LoginColors.onSurface,
                            ),
                            decoration: _fieldDecoration(
                              hint: 'Email',
                              icon: Icons.mail_outline,
                            ),
                            validator: (value) {
                              if (value == null || value.trim().isEmpty) {
                                return 'Enter your email';
                              }
                              return null;
                            },
                          ),
                          if (otpSent) ...[
                            const SizedBox(height: 12),
                            _fieldLabel('OTP'),
                            TextFormField(
                              controller: _otpController,
                              keyboardType: TextInputType.number,
                              autofocus: true,
                              style: const TextStyle(
                                fontSize: 14,
                                color: _LoginColors.onSurface,
                              ),
                              decoration: _fieldDecoration(
                                hint: 'Enter the 6-digit code',
                                icon: Icons.lock_outline,
                              ),
                              validator: (value) {
                                if (value == null || value.trim().isEmpty) {
                                  return 'Enter the OTP';
                                }
                                return null;
                              },
                            ),
                          ],
                          if (loginState.errorMessage != null) ...[
                            const SizedBox(height: 12),
                            Text(
                              loginState.errorMessage!,
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                color: _LoginColors.error,
                                fontSize: 13,
                              ),
                            ),
                          ],
                          const SizedBox(height: 16),
                          SizedBox(
                            height: 48,
                            child: ElevatedButton(
                              style: ElevatedButton.styleFrom(
                                backgroundColor: _LoginColors.primaryContainer,
                                foregroundColor: _LoginColors.onPrimary,
                                elevation: 2,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(12),
                                ),
                              ),
                              onPressed: loginState.isLoading
                                  ? null
                                  : (otpSent ? _verifyOtp : _sendOtp),
                              child: loginState.isLoading
                                  ? const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: _LoginColors.onPrimary,
                                ),
                              )
                                  : Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Text(
                                    otpSent ? 'Submit' : 'Next',
                                    style: const TextStyle(
                                      fontSize: 13,
                                      fontWeight: FontWeight.w500,
                                    ),
                                  ),
                                  const SizedBox(width: 6),
                                  const Icon(Icons.arrow_forward, size: 16),
                                ],
                              ),
                            ),
                          ),
                          if (otpSent) ...[
                            const SizedBox(height: 8),
                            TextButton(
                              onPressed: loginState.isLoading ? null : _changeEmail,
                              child: const Text(
                                'Use a different email',
                                style: TextStyle(
                                  fontSize: 12,
                                  color: _LoginColors.primaryContainer,
                                ),
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),

                  const SizedBox(height: 20),

                  // Sign-Up
                  Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const Text(
                            "Don't have an account? ",
                            style: TextStyle(
                              fontSize: 12,
                              color: _LoginColors.onSurfaceVariant,
                            ),
                          ),
                          GestureDetector(
                            onTap: _goToRegister,
                            child: const Text(
                              'Sign up',
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w600,
                                color: _LoginColors.primaryContainer,
                                decoration: TextDecoration.underline,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Wrap(
                        alignment: WrapAlignment.center,
                        spacing: 8,
                        children: const [
                          Text(
                            'Privacy Policy',
                            style: TextStyle(
                              fontSize: 11,
                              color: _LoginColors.onSurfaceVariant,
                            ),
                          ),
                          Text('•', style: TextStyle(color: _LoginColors.outline)),
                          Text(
                            'Terms of Service',
                            style: TextStyle(
                              fontSize: 11,
                              color: _LoginColors.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}