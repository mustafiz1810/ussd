
import 'package:ussd/ussd_advanced.dart';

class UssdService {
  static Future<void> sendMoneyPersonal({
    required String serviceName,
    required String personalNumber,
    required String amount,
    required String pin,
    required String id,
    String? reference,
  }) async {
    try {
      String baseCode = getBaseCode(serviceName);
      await UssdAdvanced.multisessionUssd(
          code: baseCode, subscriptionId: -1);

      await performSteps([
        getSendMoneyOption(serviceName),
        personalNumber,
        amount,
        if (needsReference(serviceName)) reference ?? '',
        pin,
      ], id);

      await UssdAdvanced.cancelSession();
    } catch (e) {
      print("failed");
    }
  }

  static Future<void> agentCashIn({
    required String serviceName,
    required String personalNumber,
    required String amount,
    required String pin,
    required String id,
  }) async {
    try {
      String baseCode = getBaseCode(serviceName);
      await UssdAdvanced.multisessionUssd(
          code: baseCode, subscriptionId: 1);

      await performSteps([
        getCashInOption(serviceName),
        personalNumber,
        amount,
        pin,
      ], id);

      await UssdAdvanced.cancelSession();
    } catch (e) {
      print("failed");
    }
  }

  
  static Future<void> performSteps(List<String> steps, String id) async {
    for (String step in steps) {
      if (step.isNotEmpty) {
        try {
          print('Sending step: $step');
          String? result = await UssdAdvanced.sendMessage(step);
          print('Response: $result');
          if (result == null) {
            print('No response for step: $step');
          }
        } catch (e) {
          print('Error during USSD step "$step": $e');
          break;
        }
      }
    }
  }

  // Helper method to get the base code for each service
  static String getBaseCode(String serviceName) {
    switch (serviceName.toLowerCase()) {
      case 'bkash':
        return '*247#';
      case 'rocket':
        return '*322#';
      case 'nagad':
        return '*167#';
      case 'upay':
        return '*268#';
      default:
        throw Exception('Unsupported service: $serviceName');
    }
  }

  // Helper method to get the send money option
  static String getSendMoneyOption(String serviceName) {
    switch (serviceName.toLowerCase()) {
      case 'bkash':
        return '1';
      case 'rocket':
        return '2';
      case 'nagad':
        return '2';
      case 'upay':
        return '1';
      default:
        throw Exception('Unsupported service: $serviceName');
    }
  }

  // Helper method to get the cash-in option
  static String getCashInOption(String serviceName) {
    switch (serviceName.toLowerCase()) {
      case 'bkash':
        return '1';
      case 'rocket':
        return '1';
      case 'nagad':
        return '1';
      case 'upay':
        return '1';
      default:
        throw Exception('Unsupported service: $serviceName');
    }
  }

  // Helper method to determine if reference is needed for a service
  static bool needsReference(String serviceName) {
    switch (serviceName.toLowerCase()) {
      case 'bkash':
      case 'nagad':
      case 'upay':
        return true;
      case 'rocket':
        return false;
      default:
        throw Exception('Unsupported service: $serviceName');
    }
  }
}
