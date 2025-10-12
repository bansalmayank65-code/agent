import 'package:flutter/material.dart';
import '../../providers/task_provider.dart';

/// UI helper service for common UI operations
class UIHelperService {
  /// Show a toast message
  static void showToast(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
      ),
    );
  }

  /// Show confirmation dialog for discarding changes
  static Future<bool> showDiscardChangesDialog(BuildContext context) async {
    return await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Unsaved Changes'),
        content: const Text('You have unsaved changes. Switching sections will discard them. Continue?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.orange),
            child: const Text('Discard Changes'),
          ),
        ],
      ),
    ) ?? false;
  }

  /// Save current section with optional advancement
  static Future<void> saveCurrentSection(
    BuildContext context,
    TaskProvider provider, {
    required bool advance,
  }) async {
    try {
      await provider.syncCache();
      if (context.mounted) {
        showToast(context, 'Saved successfully');
        if (advance) {
          // Handle advancement logic if needed
        }
      }
    } catch (e) {
      if (context.mounted) {
        showToast(context, 'Save failed: $e');
      }
    }
  }
}