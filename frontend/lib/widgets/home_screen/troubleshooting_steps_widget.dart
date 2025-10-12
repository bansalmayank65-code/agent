import 'package:flutter/material.dart';

class TroubleshootingStepsWidget extends StatelessWidget {
  const TroubleshootingStepsWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 1,
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(Icons.build, color: Colors.blue[700], size: 16),
                const SizedBox(width: 8),
                Text(
                  'Troubleshooting Steps',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Colors.blue[700],
                    fontSize: 14,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            const Text(
              '1. Ensure repository is selected and contains the required environment structure',
              style: TextStyle(fontSize: 12),
            ),
            const SizedBox(height: 4),
            const Text(
              '2. Verify the environment name matches a folder in the envs directory',
              style: TextStyle(fontSize: 12),
            ),
            const SizedBox(height: 4),
            const Text(
              '3. Check that the interface number corresponds to an existing tools folder',
              style: TextStyle(fontSize: 12),
            ),
            const SizedBox(height: 4),
            const Text(
              '4. Confirm policy.md file exists in the interface directory',
              style: TextStyle(fontSize: 12),
            ),
          ],
        ),
      ),
    );
  }
}