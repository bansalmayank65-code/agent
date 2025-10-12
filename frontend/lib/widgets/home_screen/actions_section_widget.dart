import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../widgets/json_editor_viewer.dart';
import '../../widgets/schema_display_widget.dart';

class ActionsSectionWidget extends StatelessWidget {
  const ActionsSectionWidget({super.key});

  void _showToast(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        duration: const Duration(seconds: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TaskProvider>();
    
    // Get current actions as JSON
    final actionsJson = provider.task.actionObjects != null && provider.task.actionObjects!.isNotEmpty
        ? const JsonEncoder.withIndent('  ').convert(provider.task.actionObjects)
        : const JsonEncoder.withIndent('  ').convert(provider.task.actions.map((name) => {'name': name}).toList());

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.list_alt, color: Color(0xFF2563EB)),
            const SizedBox(width: 8),
            const Text(
              'Actions JSON Editor',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: const Color(0xFF2563EB).withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: const Color(0xFF2563EB).withValues(alpha: 0.3)),
          ),
          child: Row(
            children: [
              Icon(Icons.info_outline, color: const Color(0xFF2563EB), size: 16),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Edit actions as JSON with full argument definitions. Changes auto-sync to task data.',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        
        // Schema reference
        const SchemaDisplayWidget(
          schemaType: 'actions',
          title: 'Actions Schema Reference',
          color: Color(0xFF2563EB),
        ),
        const SizedBox(height: 16),
        
        Container(
          height: 500,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(8),
          ),
          child: JsonEditorViewer(
            initialJson: actionsJson,
            onJsonChanged: (jsonString) {
              try {
                final parsed = jsonDecode(jsonString);
                if (parsed is List) {
                  final actionObjects = parsed.map((item) {
                    if (item is Map<String, dynamic>) {
                      return item;
                    } else if (item is String) {
                      return {'name': item};
                    }
                    return {'name': item.toString()};
                  }).toList();
                  
                  provider.updateActionObjects(actionObjects);
                  _showToast(context, 'Actions updated');
                }
              } catch (e) {
                // Ignore parse errors during editing
              }
            },
            title: 'Actions Editor',
            showToolbar: true,
            splitView: MediaQuery.of(context).size.width > 800,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            ElevatedButton.icon(
              onPressed: provider.dirtyActions ? () async { 
                await provider.syncCache(); 
                _showToast(context, 'Actions saved'); 
              } : null,
              icon: const Icon(Icons.save),
              label: const Text('Save Actions'),
            ),
          ],
        ),
      ],
    );
  }
}