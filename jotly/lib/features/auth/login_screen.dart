import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../auth/login_provider.dart';

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

  @override
  Widget build(BuildContext context) {
    final loginState = ref.watch(loginProvider);
    final otpSent = loginState.otpSent;

    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Text(
                  'Sign in',
                  style: Theme.of(context).textTheme.headlineMedium,
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 32),
                TextFormField(
                  controller: _emailController,
                  enabled: !otpSent,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    labelText: 'Email',
                    border: OutlineInputBorder(),
                  ),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Enter your email';
                    }
                    return null;
                  },
                ),
                if (otpSent) ...[
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _otpController,
                    keyboardType: TextInputType.number,
                    autofocus: true,
                    decoration: const InputDecoration(
                      labelText: 'OTP',
                      border: OutlineInputBorder(),
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
                  const SizedBox(height: 16),
                  Text(
                    loginState.errorMessage!,
                    style: TextStyle(color: Theme.of(context).colorScheme.error),
                    textAlign: TextAlign.center,
                  ),
                ],
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: loginState.isLoading
                      ? null
                      : (otpSent ? _verifyOtp : _sendOtp),
                  child: loginState.isLoading
                      ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                      : Text(otpSent ? 'Submit' : 'Next'),
                ),
                if (otpSent) ...[
                  const SizedBox(height: 12),
                  TextButton(
                    onPressed: loginState.isLoading ? null : _changeEmail,
                    child: const Text('Use a different email'),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}