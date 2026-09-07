import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:crop_your_image/crop_your_image.dart';

class AvatarCropperScreen extends StatefulWidget {
  final Uint8List imageBytes;

  const AvatarCropperScreen({super.key, required this.imageBytes});

  @override
  State<AvatarCropperScreen> createState() => _AvatarCropperScreenState();
}

class _AvatarCropperScreenState extends State<AvatarCropperScreen> {
  final _controller = CropController();
  bool _isCropping = false;

  void _confirmCrop() {
    setState(() => _isCropping = true);
    _controller.crop();
  }

  void _handleCropped(CropResult result) {
    if (!mounted) return;

    if (result is CropSuccess) {
      Navigator.of(context).pop(result.croppedImage);
      return;
    }

    // CropFailure or any other outcome — surface it and let the user retry.
    setState(() => _isCropping = false);
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Could not crop that image. Please try again.')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        elevation: 0,
        foregroundColor: Colors.white,
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text('Adjust photo'),
        actions: [
          TextButton(
            onPressed: _isCropping ? null : _confirmCrop,
            child: Text(
              'Done',
              style: TextStyle(
                color: _isCropping ? Colors.white38 : Colors.white,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
      body: Stack(
        children: [
          Crop(
            controller: _controller,
            image: widget.imageBytes,
            aspectRatio: 1,
            withCircleUi: true,
            baseColor: Colors.black,
            maskColor: Colors.black.withOpacity(0.65),
            onCropped: _handleCropped,
            progressIndicator: const Center(
              child: CircularProgressIndicator(color: Colors.white),
            ),
          ),
          if (_isCropping)
            const Positioned.fill(
              child: ColoredBox(
                color: Color(0x66000000),
                child: Center(child: CircularProgressIndicator(color: Colors.white)),
              ),
            ),
        ],
      ),
    );
  }
}