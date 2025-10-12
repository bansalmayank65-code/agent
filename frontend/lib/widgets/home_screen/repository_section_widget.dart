import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../providers/auth_provider.dart';
import '../../services/home_screen/file_operations_service.dart';
import '../../services/home_screen/ui_helper_service.dart';

/// Repository section widget
class RepositorySectionWidget extends StatefulWidget {
  final TaskProvider provider;

  const RepositorySectionWidget({
    super.key,
    required this.provider,
  });

  @override
  State<RepositorySectionWidget> createState() => _RepositorySectionWidgetState();
}

class _RepositorySectionWidgetState extends State<RepositorySectionWidget> {

  @override
  void initState() {
    super.initState();
  }

  @override
  void dispose() {
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Task Import Card
        Card(
          elevation: 2,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.upload_file, color: Color(0xFF059669)),
                    const SizedBox(width: 8),
                    const Text(
                      'Task Management',
                      style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                if (widget.provider.hasImportedTaskJson) ...[
                  _buildImportedJsonDisplay(),
                  const SizedBox(height: 12),
                ],
                _buildImportActions(),
              ],
            ),
          ),
        ),
      ],
    );
  }



  Widget _buildImportedJsonDisplay() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF059669).withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.check_circle, color: Color(0xFF059669), size: 18),
              const SizedBox(width: 8),
              const Text(
                'Imported task.json',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF059669),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Row(
            children: [
              const Icon(Icons.description_outlined, color: Color(0xFF059669), size: 16),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  widget.provider.importedJsonPath ?? 'Unknown path',
                  style: const TextStyle(
                    fontSize: 12,
                    color: Color(0xFF065F46),
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildImportActions() {
    return Wrap(
      spacing: 12,
      runSpacing: 8,
      children: [
        ElevatedButton.icon(
          onPressed: _importTaskJson,
          icon: const Icon(Icons.upload_file),
          label: const Text('Import task.json'),
          style: ElevatedButton.styleFrom(
            backgroundColor: const Color(0xFF059669),
            foregroundColor: Colors.white,
          ),
        ),
      ],
    );
  }



  Future<void> _importTaskJson() async {
    try {
      Map<String, dynamic>? result;
      String? importedPath;
      
      if (kIsWeb) {
        result = await FileOperationsService.importTaskJsonWeb();
        if (result != null) {
          importedPath = result['fileName'];
        }
      } else {
        // For native platforms, use file picker to select task.json from any location
        final selectedPath = await FileOperationsService.pickTaskJsonFile("");
        if (selectedPath != null) {
          // Read the file content
          try {
            // This would need actual file reading implementation
            // For now, we'll show a message that it's selected
            importedPath = selectedPath;
            if (mounted) {
              UIHelperService.showToast(context, 'Selected: $selectedPath');
              UIHelperService.showToast(context, 'File reading not yet implemented for native platforms');
            }
            return;
          } catch (e) {
            throw Exception('Failed to read file: $e');
          }
        }
      }
      
      if (result != null) {
        final authProvider = context.read<AuthProvider>();
        // Import without overriding the user ID from JSON - let users edit it in step 4
        // But pass the logged-in user ID separately for database foreign key constraint
        final importResult = await widget.provider.importTaskJson(result['data'], dbUserId: authProvider.userId);
        if (importResult['success'] == true) {
          widget.provider.setImportedJsonPath(importedPath);
          if (mounted) {
            UIHelperService.showToast(context, 'task.json imported successfully from: $importedPath');
            
            // Navigate to task workflow after successful import
            final taskId = importResult['taskId'];
            if (taskId != null) {
              Navigator.pushNamed(context, '/task/$taskId');
            }
          }
        } else {
          if (mounted) {
            final errorMsg = importResult['message'] ?? 'Import failed';
            UIHelperService.showToast(context, 'Import failed: $errorMsg');
          }
        }
      }
    } catch (e) {
      if (mounted) {
        UIHelperService.showToast(context, 'Import failed: $e');
      }
    }
  }


}