import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:convert';
import '../../providers/task_provider.dart';
import '../../services/home_screen/ui_helper_service.dart';
import '../json_editor_viewer.dart';
import '../schema_display_widget.dart';

/// Widget for Task JSON section
class TaskJsonSectionWidget extends StatelessWidget {
  const TaskJsonSectionWidget({super.key});

  /// Build the complete task.json structure from current provider data
  Future<String> _buildTaskJson(TaskProvider provider) {
    // Process edges to handle connection objects properly
    final processedEdges = provider.task.edges.map((edge) {
      final processedEdge = <String, dynamic>{};
      edge.forEach((key, value) {
        if (key == 'connection') {
          // Try to parse the connection string as JSON object
          try {
            processedEdge[key] = jsonDecode(value);
          } catch (e) {
            // If parsing fails, keep as string
            processedEdge[key] = value;
          }
        } else {
          processedEdge[key] = value;
        }
      });
      return processedEdge;
    }).toList();

    final taskJson = {
      'env': provider.task.env,
      'model_provider': 'fireworks', // Default value
      'model': 'qwen3-coder-480b-a35b-instruct', // Default value
      'num_trials': 3, // Default value
      'temperature': 1, // Default value
      'interface_num': provider.task.interfaceNum,
      'task': {
        'user_id': provider.task.userId,
        'instruction': provider.task.instruction,
        'actions': provider.task.actionObjects ?? provider.task.actions.map((name) => {'name': name}).toList(),
        'outputs': provider.task.outputs,
        'edges': processedEdges,
        'num_edges': processedEdges.length,
      }
    };
    
    return Future.value(const JsonEncoder.withIndent('  ').convert(taskJson));
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.code, color: Color(0xFF059669)),
                const SizedBox(width: 8),
                const Text(
                  'Task.json Editor',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: const Color(0xFF059669).withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.3)),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: const Color(0xFF059669), size: 16),
                  const SizedBox(width: 8),
                  const Expanded(
                    child: Text(
                      'Complete task configuration with environment, model settings, and task definition. Changes auto-sync.',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            
            // Schema reference
            const SchemaDisplayWidget(
              schemaType: 'task',
              title: 'Task.json Schema Reference',
              color: Color(0xFF059669),
            ),
            const SizedBox(height: 16),
            
            // JSON Editor
            Container(
              height: 500,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey[300]!),
                borderRadius: BorderRadius.circular(8),
              ),
              child: FutureBuilder<String>(
                future: _buildTaskJson(provider),
                builder: (context, snapshot) {
                  if (snapshot.connectionState == ConnectionState.waiting) {
                    return const Center(child: CircularProgressIndicator());
                  }
                  if (!snapshot.hasData) {
                    return const Center(child: Text('No data available'));
                  }
                  
                  return JsonEditorViewer(
                    initialJson: snapshot.data!,
                    onJsonChanged: (jsonString) {
                      try {
                        final parsed = jsonDecode(jsonString);
                        if (parsed is Map<String, dynamic>) {
                          // Update the provider with the parsed task JSON
                          // Note: This is a read-only view for now since task.json
                          // is automatically generated from other sections
                          UIHelperService.showToast(context, 'Task.json structure validated');
                        }
                      } catch (e) {
                        // Ignore parse errors during editing
                      }
                    },
                    title: 'Task.json Editor',
                    showToolbar: true,
                    splitView: MediaQuery.of(context).size.width > 800,
                  );
                },
              ),
            ),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: Colors.amber.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.amber.withValues(alpha: 0.3)),
              ),
              child: Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.amber[700], size: 16),
                  const SizedBox(width: 8),
                  const Expanded(
                    child: Text(
                      'Task.json is automatically generated from all sections. Use individual sections to make changes.',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
          ],
        );
      },
    );
  }
}