import 'package:flutter/cupertino.dart';
import 'package:ussd/services/ussd_advanced.dart';
import '../repositories/post_api_request.dart';

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
      print(baseCode);
      await UssdAdvanced.multisessionUssd(
          code: baseCode, subscriptionId: -1);

      bool stepsCompleted = await performSteps([
        getSendMoneyOption(serviceName),
        personalNumber,
        amount,
        if (needsReference(serviceName)) reference ?? '',
        pin,
      ], id);

      if (stepsCompleted) {
        await ApiHandler.sendUssdResponse(
          status: 'success',
          response: "Money sent successfully",
          id: id,
        );
      } else {
        throw Exception('Steps could not be completed.');
      }

      await UssdAdvanced.cancelSession();
    } catch (e) {
      await ApiHandler.sendUssdResponse(
        status: 'failed',
        response: "Session failed: $e",
        id: id,
      );
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

      bool stepsCompleted = await performSteps([
        getCashInOption(serviceName),
        personalNumber,
        amount,
        pin,
      ], id);

      if (stepsCompleted) {
        await ApiHandler.sendUssdResponse(
          status: 'success',
          response: "Cash-in successful",
          id: id,
        );
      } else {
        throw Exception('Steps could not be completed.');
      }

      await UssdAdvanced.cancelSession();
    } catch (e) {
      await ApiHandler.sendUssdResponse(
        status: 'failed',
        response: "Cash-in failed: $e",
        id: id,
      );
    }
  }

  static Future<bool> performSteps(List<String> steps, String id) async {
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
          await ApiHandler.sendUssdResponse(
            status: 'failed',
            response: 'Error at step "$step": $e',
            id: id,
          );
          return false;
        }
      }
    }
    return true; // Return true if all steps succeed
  }

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
