import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'avatar_cropper_screen.dart';
import 'register_provider.dart';

class _RegisterColors {
  static const primary = Color(0xFF4F46E5);
  static const primaryDark = Color(0xFF4338CA);
  static const primaryTint = Color(0xFFEEF2FF);
  static const primaryTintBorder = Color(0xFFE0E7FF);
  static const surface = Color(0xFFF9F9FC);
  static const card = Color(0xFFFFFFFF);
  static const border = Color(0xFFE2E8F0);
  static const slate900 = Color(0xFF0F172A);
  static const slate800 = Color(0xFF1E293B);
  static const slate500 = Color(0xFF64748B);
  static const slate400 = Color(0xFF94A3B8);
  static const slate50 = Color(0xFFF8FAFC);
  static const error = Color(0xFFDC2626);
}

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _emailController = TextEditingController();
  final _otpController = TextEditingController();

  Uint8List? _avatarBytes;

  Timer? _resendTimer;
  int _resendSecondsLeft = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(registerProvider.notifier).reset();
    });
  }

  @override
  void dispose() {
    _nameController.dispose();
    _emailController.dispose();
    _otpController.dispose();
    _resendTimer?.cancel();
    super.dispose();
  }

  Future<void> _pickAndCropAvatar() async {
    final source = await showModalBottomSheet<ImageSource>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (context) => const _ImageSourceSheet(),
    );
    if (source == null) return;

    final picker = ImagePicker();
    final picked = await picker.pickImage(source: source, imageQuality: 90);
    if (picked == null) return;

    final bytes = await picked.readAsBytes();
    if (!mounted) return;

    final cropped = await Navigator.of(context).push<Uint8List>(
      MaterialPageRoute(
        builder: (_) => AvatarCropperScreen(imageBytes: bytes),
      ),
    );

    if (cropped != null && mounted) {
      setState(() => _avatarBytes = cropped);
    }
  }

  Future<void> _sendOtp() async {
    if (!_formKey.currentState!.validate()) return;

    final success = await ref.read(registerProvider.notifier).sendOtp(
      email: _emailController.text.trim(),
      displayName: _nameController.text.trim(),
      avatarBytes: _avatarBytes,
    );

    if (success) _startResendTimer();
  }

  Future<void> _verifyOtp() async {
    if (!_formKey.currentState!.validate()) return;

    final success =
    await ref.read(registerProvider.notifier).verifyOtp(_otpController.text.trim());

    if (success && mounted) {
      context.go('/home');
    }
  }

  Future<void> _resendOtp() async {
    if (_resendSecondsLeft > 0) return;
    _otpController.clear();
    await ref.read(registerProvider.notifier).sendOtp(
      email: _emailController.text.trim(),
      displayName: _nameController.text.trim(),
      avatarBytes: _avatarBytes,
    );
    _startResendTimer();
  }

  void _startResendTimer() {
    _resendTimer?.cancel();
    setState(() => _resendSecondsLeft = 45);
    _resendTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_resendSecondsLeft <= 1) {
        timer.cancel();
        setState(() => _resendSecondsLeft = 0);
      } else {
        setState(() => _resendSecondsLeft -= 1);
      }
    });
  }

  void _backToDetails() {
    _resendTimer?.cancel();
    _resendSecondsLeft = 0;
    _otpController.clear();
    ref.read(registerProvider.notifier).reset();
  }

  void _goToLogin() => context.pop();

  InputDecoration _fieldDecoration({
    required String hint,
    required IconData icon,
  }) {
    return InputDecoration(
      hintText: hint,
      hintStyle: const TextStyle(color: _RegisterColors.slate400, fontSize: 13),
      prefixIcon: Icon(icon, size: 18, color: _RegisterColors.slate400),
      filled: true,
      fillColor: _RegisterColors.card,
      contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _RegisterColors.border),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _RegisterColors.border),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _RegisterColors.primary, width: 1.5),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: _RegisterColors.error),
      ),
    );
  }

  Widget _fieldLabel(String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: _RegisterColors.slate800,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final registerState = ref.watch(registerProvider);
    final otpSent = registerState.otpSent;
    final step = otpSent ? 2 : 1;

    return Scaffold(
      backgroundColor: _RegisterColors.surface,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            children: [
              _buildHeader(step),
              Expanded(
                child: Center(
                  child: SingleChildScrollView(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 400),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          _buildTitleBlock(),
                          const SizedBox(height: 20),
                          Container(
                            padding: const EdgeInsets.all(20),
                            decoration: BoxDecoration(
                              color: _RegisterColors.card,
                              borderRadius: BorderRadius.circular(18),
                              border: Border.all(color: _RegisterColors.border),
                              boxShadow: const [
                                BoxShadow(
                                  color: Color(0x08000000),
                                  blurRadius: 6,
                                  offset: Offset(0, 1),
                                ),
                              ],
                            ),
                            child: Form(
                              key: _formKey,
                              child: otpSent
                                  ? _buildOtpStep(registerState)
                                  : _buildDetailsStep(registerState),
                            ),
                          ),
                          const SizedBox(height: 20),
                          _buildSignInSwitcher(),
                        ],
                      ),
                    ),
                  ),
                ),
              ),
              const Text(
                'Protected by Jotly • Terms & Privacy',
                style: TextStyle(fontSize: 11, color: _RegisterColors.slate400),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader(int step) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        SizedBox(
          width: 40,
          height: 40,
          child: OutlinedButton(
            onPressed: () => context.pop(),
            style: OutlinedButton.styleFrom(
              padding: EdgeInsets.zero,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
              side: const BorderSide(color: _RegisterColors.border),
              backgroundColor: _RegisterColors.card,
            ),
            child: const Icon(Icons.arrow_back, size: 20, color: _RegisterColors.slate800),
          ),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: _RegisterColors.primaryTint,
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: _RegisterColors.primaryTintBorder),
          ),
          child: Text(
            'Step $step of 2',
            style: const TextStyle(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: _RegisterColors.primaryDark,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildTitleBlock() {
    return Column(
      children: [
        const SizedBox(height: 12),
        Container(
          width: 56,
          height: 56,
          decoration: BoxDecoration(
            color: _RegisterColors.primary,
            borderRadius: BorderRadius.circular(16),
            boxShadow: const [
              BoxShadow(
                color: Color(0x334F46E5),
                blurRadius: 10,
                offset: Offset(0, 4),
              ),
            ],
          ),
          child: const Icon(Icons.note_alt_outlined, color: Colors.white, size: 26),
        ),
        const SizedBox(height: 12),
        const Text(
          'Create your account',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 22,
            fontWeight: FontWeight.w700,
            color: _RegisterColors.slate900,
          ),
        ),
        const SizedBox(height: 4),
        const Text(
          'Start organizing your ideas with Jotly',
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 12, color: _RegisterColors.slate500),
        ),
      ],
    );
  }

  Widget _buildDetailsStep(RegisterState state) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Center(
          child: GestureDetector(
            onTap: _pickAndCropAvatar,
            child: Stack(
              clipBehavior: Clip.none,
              children: [
                Container(
                  width: 80,
                  height: 80,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    color: _RegisterColors.slate50,
                    border: Border.all(
                      color: _RegisterColors.primaryTintBorder,
                      width: 2,
                    ),
                  ),
                  child: ClipOval(
                    child: _avatarBytes != null
                        ? Image.memory(_avatarBytes!, fit: BoxFit.cover)
                        : const Icon(
                      Icons.person_outline,
                      size: 32,
                      color: _RegisterColors.slate400,
                    ),
                  ),
                ),
                Positioned(
                  right: -2,
                  bottom: -2,
                  child: Container(
                    width: 28,
                    height: 28,
                    decoration: const BoxDecoration(
                      color: _RegisterColors.primary,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.add_a_photo_outlined,
                      size: 14,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
        const Center(
          child: Text(
            'Upload profile picture',
            style: TextStyle(fontSize: 11, color: _RegisterColors.slate400),
          ),
        ),
        const SizedBox(height: 16),
        _fieldLabel('Full Name'),
        TextFormField(
          controller: _nameController,
          keyboardType: TextInputType.name,
          style: const TextStyle(fontSize: 13, color: _RegisterColors.slate800),
          decoration: _fieldDecoration(hint: 'Name', icon: Icons.person_outline),
          validator: (value) {
            if (value == null || value.trim().isEmpty) return 'Enter your name';
            return null;
          },
        ),
        const SizedBox(height: 12),
        _fieldLabel('Email Address'),
        TextFormField(
          controller: _emailController,
          keyboardType: TextInputType.emailAddress,
          style: const TextStyle(fontSize: 13, color: _RegisterColors.slate800),
          decoration: _fieldDecoration(hint: 'Email', icon: Icons.mail_outline),
          validator: (value) {
            if (value == null || value.trim().isEmpty) return 'Enter your email';
            return null;
          },
        ),
        if (state.errorMessage != null) ...[
          const SizedBox(height: 12),
          Text(
            state.errorMessage!,
            textAlign: TextAlign.center,
            style: const TextStyle(color: _RegisterColors.error, fontSize: 12),
          ),
        ],
        const SizedBox(height: 16),
        SizedBox(
          height: 46,
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: _RegisterColors.primary,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
            onPressed: state.isLoading ? null : _sendOtp,
            child: state.isLoading
                ? const SizedBox(
              height: 18,
              width: 18,
              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
            )
                : const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('Next', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                SizedBox(width: 6),
                Icon(Icons.arrow_forward, size: 16),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildOtpStep(RegisterState state) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: _RegisterColors.primaryTint,
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: _RegisterColors.primaryTintBorder),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 28,
                height: 28,
                margin: const EdgeInsets.only(top: 2),
                decoration: BoxDecoration(
                  color: _RegisterColors.primary,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.mark_email_read_outlined, size: 16, color: Colors.white),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Verify your email',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: _RegisterColors.primaryDark,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text.rich(
                      TextSpan(
                        text: 'We sent a 6-digit code to ',
                        style: const TextStyle(fontSize: 11, color: _RegisterColors.primaryDark),
                        children: [
                          TextSpan(
                            text: state.email ?? '',
                            style: const TextStyle(fontWeight: FontWeight.w700),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            _fieldLabel('OTP'),
            TextButton(
              onPressed: _resendSecondsLeft > 0 ? null : _resendOtp,
              style: TextButton.styleFrom(padding: EdgeInsets.zero, minimumSize: Size.zero),
              child: Text(
                _resendSecondsLeft > 0
                    ? 'Resend in 0:${_resendSecondsLeft.toString().padLeft(2, '0')}'
                    : 'Resend code',
                style: const TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.w600,
                  color: _RegisterColors.primary,
                ),
              ),
            ),
          ],
        ),
        TextFormField(
          controller: _otpController,
          keyboardType: TextInputType.number,
          autofocus: true,
          style: const TextStyle(fontSize: 13, color: _RegisterColors.slate800),
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
        if (state.errorMessage != null) ...[
          const SizedBox(height: 12),
          Text(
            state.errorMessage!,
            textAlign: TextAlign.center,
            style: const TextStyle(color: _RegisterColors.error, fontSize: 12),
          ),
        ],
        const SizedBox(height: 16),
        SizedBox(
          height: 46,
          child: ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: _RegisterColors.primary,
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
            ),
            onPressed: state.isLoading ? null : _verifyOtp,
            child: state.isLoading
                ? const SizedBox(
              height: 18,
              width: 18,
              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
            )
                : const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.verified_outlined, size: 16),
                SizedBox(width: 6),
                Text('Submit & Create Account',
                    style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 4),
        TextButton(
          onPressed: state.isLoading ? null : _backToDetails,
          child: const Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.arrow_back, size: 14, color: _RegisterColors.slate500),
              SizedBox(width: 4),
              Text(
                'Change email or name',
                style: TextStyle(fontSize: 11, color: _RegisterColors.slate500),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildSignInSwitcher() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        const Text(
          'Already have an account? ',
          style: TextStyle(fontSize: 12, color: _RegisterColors.slate500),
        ),
        GestureDetector(
          onTap: _goToLogin,
          child: const Text(
            'Sign in',
            style: TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: _RegisterColors.primary,
              decoration: TextDecoration.underline,
            ),
          ),
        ),
      ],
    );
  }
}

class _ImageSourceSheet extends StatelessWidget {
  const _ImageSourceSheet();

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 8),
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            ListTile(
              leading: const Icon(Icons.photo_library_outlined),
              title: const Text('Choose from gallery'),
              onTap: () => Navigator.of(context).pop(ImageSource.gallery),
            ),
            ListTile(
              leading: const Icon(Icons.photo_camera_outlined),
              title: const Text('Take a photo'),
              onTap: () => Navigator.of(context).pop(ImageSource.camera),
            ),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}