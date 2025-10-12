import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../services/home_screen/ui_helper_service.dart';

/// Widget for User ID section
class UserIdSectionWidget extends StatefulWidget {
  const UserIdSectionWidget({super.key});

  @override
  State<UserIdSectionWidget> createState() => _UserIdSectionWidgetState();
}

class _UserIdSectionWidgetState extends State<UserIdSectionWidget> {
  final TextEditingController _userIdController = TextEditingController();

  @override
  void dispose() {
    _userIdController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        // Initialize controller if empty and provider has data
        if (_userIdController.text.isEmpty && provider.task.userId.isNotEmpty) {
          _userIdController.text = provider.task.userId;
        }

        return Column(
          children: [
            TextField(
              controller: _userIdController,
              decoration: const InputDecoration(
                labelText: 'User ID',
                helperText: 'Enter a unique identifier for the user',
              ),
              onChanged: provider.updateUserId,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: provider.dirtyUserId ? () async {
                await provider.syncCache();
                if (mounted) {
                  UIHelperService.showToast(context, 'User ID saved');
                }
              } : null,
              icon: const Icon(Icons.save),
              label: const Text('Save'),
            ),
          ],
        );
      },
    );
  }
}