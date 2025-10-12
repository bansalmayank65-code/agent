import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_json_viewer/flutter_json_viewer.dart';
import '../services/schema_service.dart';

class SchemaDisplayWidget extends StatelessWidget {
  final String schemaType; // 'actions', 'edges', or 'task'
  final String title;
  final Color color;

  const SchemaDisplayWidget({
    super.key,
    required this.schemaType,
    required this.title,
    required this.color,
  });

  Future<String> _getSchemaString() async {
    switch (schemaType) {
      case 'actions':
        return await SchemaService.getActionsSchema();
      case 'edges':
        return await SchemaService.getEdgesSchema();
      case 'task':
        return await SchemaService.getTaskSchema();
      default:
        return '[]';
    }
  }

  Future<Map<String, dynamic>> _getSchemaJson() async {
    final schemaString = await _getSchemaString();
    return {'schema': jsonDecode(schemaString)};
  }

  void _copyToClipboard(BuildContext context, String text) {
    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('$title schema copied to clipboard'),
        duration: const Duration(seconds: 2),
        backgroundColor: color,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        border: Border.all(color: color.withValues(alpha: 0.3)),
        borderRadius: BorderRadius.circular(8),
        color: color.withValues(alpha: 0.05),
      ),
      child: ExpansionTile(
        tilePadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
        childrenPadding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
        leading: Icon(
          _getIconForType(),
          color: color,
          size: 20,
        ),
        title: Text(
          title,
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: color,
            fontSize: 14,
          ),
        ),
        subtitle: Text(
          'Click to view $schemaType schema structure',
          style: const TextStyle(fontSize: 12),
        ),
        children: [
          FutureBuilder<String>(
            future: _getSchemaString(),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Padding(
                  padding: EdgeInsets.all(16),
                  child: Center(child: CircularProgressIndicator()),
                );
              }
              
              if (snapshot.hasError) {
                return Padding(
                  padding: const EdgeInsets.all(16),
                  child: Text(
                    'Error loading schema: ${snapshot.error}',
                    style: const TextStyle(color: Colors.red),
                  ),
                );
              }

              final schemaString = snapshot.data ?? '{}';
              
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Toolbar
                  Row(
                    children: [
                      const Text(
                        'Schema Structure:',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 12,
                        ),
                      ),
                      const Spacer(),
                      IconButton(
                        onPressed: () => _copyToClipboard(context, schemaString),
                        icon: const Icon(Icons.copy, size: 16),
                        tooltip: 'Copy schema',
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(
                          minWidth: 32,
                          minHeight: 32,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  // JSON Code display
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.grey[50],
                      border: Border.all(color: Colors.grey[300]!),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: SelectableText(
                      schemaString,
                      style: const TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 11,
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                  // JSON Tree view
                  FutureBuilder<Map<String, dynamic>>(
                    future: _getSchemaJson(),
                    builder: (context, jsonSnapshot) {
                      if (jsonSnapshot.hasData) {
                        return Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            border: Border.all(color: Colors.grey[300]!),
                            borderRadius: BorderRadius.circular(6),
                          ),
                          constraints: const BoxConstraints(maxHeight: 200),
                          child: SingleChildScrollView(
                            child: JsonViewer(jsonSnapshot.data!),
                          ),
                        );
                      }
                      return const SizedBox.shrink();
                    },
                  ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  IconData _getIconForType() {
    switch (schemaType) {
      case 'actions':
        return Icons.play_arrow;
      case 'edges':
        return Icons.account_tree;
      case 'task':
        return Icons.task_alt;
      default:
        return Icons.schema;
    }
  }
}