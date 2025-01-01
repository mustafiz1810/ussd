import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:ussd/ussd_advanced.dart';
import 'package:ussd/ussd_service.dart';

class UssdUi extends StatefulWidget {
  UssdUi({Key? key}) : super(key: key);

  @override
  State<UssdUi> createState() => _UssdUiState();
}

class _UssdUiState extends State<UssdUi> {

  late TextEditingController _controller;
  String? _response;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void fetchData() {
    try {
      String serviceName = "bkash";
      String personalNumber = "01854969657";
      String reference = "1";
      String amount = "20";
      String id = "9";

      print("Initiating request for $serviceName with ID $id");

      _sendMultiSessionRequestWithDetails(
        serviceName: serviceName,
        personalNumber: personalNumber,
        reference: reference,
        amount: amount,
        id: id,
      );

      print("USSD request completed for ID: $id");
    } catch (e) {
      print("Error during fetchData: $e");
    }
  }



  Future<void> _sendMultiSessionRequestWithDetails({
    required String serviceName,
    required String personalNumber,
    required String reference,
    required String amount,
    required String id,
  }) async {
    String? pin = "18103";

    try {
      print("Initiating personal send money for $serviceName...");
      await UssdService.sendMoneyPersonal(
        serviceName: serviceName,
        personalNumber: personalNumber,
        amount: amount,
        pin: pin!,
        reference: reference,
        id: id,
      );

      print("USSD transaction completed for $serviceName and ID $id");
    } catch (e) {
      print("Error during USSD transaction for $id: $e");
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Ussd Plugin example'),
        ),
        body: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          mainAxisSize: MainAxisSize.max,
          children: [
            // text input
            TextField(
              controller: _controller,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: 'Ussd code'),
            ),

            // dispaly responce if any
            if (_response != null)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 8),
                child: Text(_response!),
              ),

            // buttons
            Column(
              children: [
                ElevatedButton(
                  onPressed: () {
                    UssdAdvanced.sendUssd(
                        code: _controller.text, subscriptionId: 1);
                  },
                  child: const Text('normal request'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    String? _res = await UssdAdvanced.sendAdvancedUssd(
                        code: _controller.text, subscriptionId: 1);
                    setState(() {
                      _response = _res;
                    });
                  },
                  child: const Text('single session request'),
                ),
                ElevatedButton(
                  onPressed: fetchData,
                  child: const Text('multi session request'),
                ),
              ],
            )
          ],
        ),
      ),
    );
  }
}