import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';

class ConnectionStatusWidget extends StatelessWidget {
  const ConnectionStatusWidget({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        return Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          color: provider.isServerConnected
              ? Colors.green.shade100
              : Colors.red.shade100,
          child: Row(
            children: [
              Icon(
                provider.isServerConnected
                    ? Icons.wifi
                    : Icons.wifi_off,
                color: provider.isServerConnected
                    ? Colors.green.shade700
                    : Colors.red.shade700,
                size: 20,
              ),
              const SizedBox(width: 8),
              Text(
                provider.isServerConnected
                    ? 'Connected to backend server'
                    : 'Backend server not available',
                style: TextStyle(
                  color: provider.isServerConnected
                      ? Colors.green.shade700
                      : Colors.red.shade700,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const Spacer(),
              if (!provider.isServerConnected)
                TextButton(
                  onPressed: () => provider.checkServerConnection(),
                  child: const Text('Retry'),
                ),
            ],
          ),
        );
      },
    );
  }
}