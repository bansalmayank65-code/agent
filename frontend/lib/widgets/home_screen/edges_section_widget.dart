import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'dart:convert';
import '../../providers/task_provider.dart';
import '../../services/home_screen/ui_helper_service.dart';
import '../json_editor_viewer.dart';
import '../schema_display_widget.dart';

/// Widget for Edges section
class EdgesSectionWidget extends StatelessWidget {
  const EdgesSectionWidget({super.key});

  /// Convert edges from internal format back to proper JSON objects for display
  List<Map<String, dynamic>> _convertEdgesToDisplayFormat(List<Map<String, String>> edges) {
    return edges.map((edge) {
      final result = <String, dynamic>{};
      edge.forEach((key, value) {
        if (key == 'connection' && value.isNotEmpty && (value.startsWith('{') || value.startsWith('['))) {
          // Try to parse connection JSON strings back to objects
          try {
            result[key] = jsonDecode(value);
          } catch (_) {
            // If parsing fails, keep as string
            result[key] = value;
          }
        } else {
          // For other fields, keep as strings or try to parse if they look like JSON
          if (value.isNotEmpty && (value.startsWith('{') || value.startsWith('['))) {
            try {
              result[key] = jsonDecode(value);
            } catch (_) {
              result[key] = value;
            }
          } else {
            result[key] = value;
          }
        }
      });
      return result;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        // Convert edges to proper display format (parse connection JSON strings back to objects)
        final displayEdges = _convertEdgesToDisplayFormat(provider.task.edges);
        final edgesJson = displayEdges.isNotEmpty
            ? const JsonEncoder.withIndent('  ').convert(displayEdges)
            : const JsonEncoder.withIndent('  ').convert([]);

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.account_tree, color: Color(0xFF059669)),
                const SizedBox(width: 8),
                const Text(
                  'Edges JSON Editor',
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
                      'Define workflow edges with schema validation. Each edge needs from, to, and connection properties.',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            
            // Schema reference
            const SchemaDisplayWidget(
              schemaType: 'edges',
              title: 'Edges Schema Reference',
              color: Color(0xFF059669),
            ),
            const SizedBox(height: 16),
            
            // Action buttons
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                ElevatedButton.icon(
                  onPressed: provider.isLoading ? null : () async {
                    try {
                      final result = await provider.generateEdges();
                      if (context.mounted) {
                        if (result?['success'] == true) {
                          UIHelperService.showToast(context, 'Edges generated and updated successfully');
                        } else {
                          UIHelperService.showToast(
                            context, 
                            'Failed to generate edges: ${result?['error'] ?? 'Unknown error'}'
                          );
                        }
                      }
                    } catch (e) {
                      if (context.mounted) {
                        UIHelperService.showToast(context, 'Error generating edges: $e');
                      }
                    }
                  },
                  icon: provider.isLoading 
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                          ),
                        )
                      : const Icon(Icons.smart_toy, size: 16),
                  label: Text(provider.isLoading ? 'Generating...' : 'Add/Fix Edges'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF059669),
                    foregroundColor: Colors.white,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            
            Container(
              height: 500,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey[300]!),
                borderRadius: BorderRadius.circular(8),
              ),
              child: JsonEditorViewer(
                key: ValueKey(edgesJson.hashCode), // Force rebuild when edges change
                initialJson: edgesJson,
                onJsonChanged: (jsonString) {
                  try {
                    final parsed = jsonDecode(jsonString);
                    if (parsed is List) {
                      final edges = parsed.map((item) {
                        if (item is Map<String, dynamic>) {
                          // Convert Map<String, dynamic> to Map<String, String>
                          return item.map((key, value) {
                            if (key == 'connection' && value is Map) {
                              // For connection objects, encode as proper JSON
                              return MapEntry(key, jsonEncode(value));
                            } else if (value is Map || value is List) {
                              // For other complex objects, encode as JSON
                              return MapEntry(key, jsonEncode(value));
                            } else {
                              // For simple values, convert to string
                              return MapEntry(key, value.toString());
                            }
                          });
                        }
                        return <String, String>{};
                      }).toList();
                      
                      provider.updateEdges(edges);
                      UIHelperService.showToast(context, 'Edges updated');
                    }
                  } catch (e) {
                    // Ignore parse errors during editing
                  }
                },
                title: 'Edges Editor',
                showToolbar: true,
                splitView: MediaQuery.of(context).size.width > 800,
              ),
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                ElevatedButton.icon(
                  onPressed: provider.dirtyEdges ? () async { 
                    await provider.syncCache(); 
                    if (context.mounted) {
                      UIHelperService.showToast(context, 'Edges saved');
                    }
                  } : null,
                  icon: const Icon(Icons.save),
                  label: const Text('Save Edges'),
                ),
              ],
            ),
          ],
        );
      },
    );
  }
}