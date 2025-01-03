import 'package:flutter/services.dart';

class USSDService {
  static const MethodChannel _channel = MethodChannel('com.example.ussd.USSDService');

  // Method to check if the accessibility service is enabled
  static Future<bool> isAccessibilityServiceEnabled() async {
    try {
      final bool isEnabled = await _channel.invokeMethod('isAccessibilityServiceEnabled');
      return isEnabled;
    } on PlatformException catch (e) {
      print("Failed to check accessibility service: ${e.message}");
      return false;
    }
  }
}
