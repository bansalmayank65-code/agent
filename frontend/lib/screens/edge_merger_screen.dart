import 'package:flutter/material.dart';
import '../widgets/json_editor_viewer.dart';
import '../widgets/dialogs/changes_summary_dialog.dart';
import '../services/api_service.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;

/// Screen for merging duplicate edges in task.json
/// Uses side-by-side JSON viewer similar to Interface Changer
/// Follows the same coding guidelines and UI design pattern
class EdgeMergerScreen extends StatelessWidget {
  /// If true, wraps content in Scaffold with AppBar (for standalone navigation).
  /// If false, returns just the body content (for embedding in other layouts).
  final bool standalone;
  
  const EdgeMergerScreen({Key? key, this.standalone = true}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bodyContent = _EdgeMergerBody();
    
    if (standalone) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('Merge Edges'),
          backgroundColor: Colors.white,
          foregroundColor: const Color(0xFF2d3748),
        ),
        body: bodyContent,
      );
    }
    
    return bodyContent;
  }
}

/// The actual edge merger UI body (without Scaffold/AppBar)
class _EdgeMergerBody extends StatefulWidget {
  @override
  State<_EdgeMergerBody> createState() => _EdgeMergerBodyState();
}

class _EdgeMergerBodyState extends State<_EdgeMergerBody> {
  final beforeController = TextEditingController();
  final afterController = TextEditingController();
  bool isMerging = false;

  @override
  void dispose() {
    beforeController.dispose();
    afterController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Controls row
        Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Merge duplicate edges and remove redundant connections',
                      style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: Color(0xFF2d3748)),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Combines edges with same "from" and "to", prioritizes instruction edges, removes redundant action→action connections',
                      style: TextStyle(fontSize: 12, color: Color(0xFF718096)),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              ElevatedButton.icon(
                onPressed: isMerging ? null : _mergeEdges,
                icon: isMerging 
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                    )
                  : const Icon(Icons.merge),
                label: Text(isMerging ? 'Merging...' : 'Merge Edges'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF3b82f6),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        // Fixed height container for editors to avoid layout conflicts
        SizedBox(
          height: 600,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                Expanded(
                  child: JsonEditorViewer(
                    title: 'Before (Original task.json)',
                    showToolbar: true,
                    splitView: true,
                    controller: beforeController,
                  ),
                ),
                const VerticalDivider(width: 12),
                Expanded(
                  child: JsonEditorViewer(
                    title: 'After (Merged edges)',
                    showToolbar: true,
                    splitView: true,
                    controller: afterController,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
      ],
    );
  }

  /// Merge edges using the backend API
  Future<void> _mergeEdges() async {
    // Fail-fast: validate input JSON first
    final beforeText = beforeController.text;
    if (beforeText.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Paste a task.json into the left editor first'))
      );
      return;
    }

    dynamic decoded;
    try {
      decoded = jsonDecode(beforeText);
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Invalid JSON in left editor'))
      );
      return;
    }

    // Validate that it's a proper task.json with edges
    if (decoded is! Map<String, dynamic> || 
        decoded['task'] == null || 
        decoded['task']['edges'] == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Invalid task.json: missing task.edges'))
      );
      return;
    }

    setState(() {
      isMerging = true;
    });

    try {

      // Call backend API
      final response = await http.post(
        Uri.parse('${ApiService.baseUrl}/api/edges/merge-duplicate-edges'),
        headers: {'Content-Type': 'application/json'},
        body: beforeText,
      );

      if (response.statusCode == 200) {
        final responseData = jsonDecode(response.body);
        
        if (responseData['success'] == true) {
          // Extract the merged task
          final mergedTask = responseData['task'];
          
          // Format and display the result
          const encoder = JsonEncoder.withIndent('  ');
          afterController.text = encoder.convert(mergedTask);
          
          // Extract statistics for summary
          final stats = responseData['statistics'] as Map<String, dynamic>?;
          final changes = <ChangeItem>[];
          
          if (stats != null) {
            final originalCount = stats['original_edges_count'] ?? 0;
            final mergedCount = stats['merged_edges_count'] ?? 0;
            final exactDuplicatesRemoved = stats['exact_duplicates_removed'] ?? 0;
            final sameFromToMerged = stats['same_from_to_merged'] ?? 0;
            final redundantConnectionsRemoved = stats['redundant_connections_removed'] ?? 0;
            
            changes.add(ChangeItem(
              type: ChangeType.info,
              title: 'Original Edges',
              description: '$originalCount edges in the original task',
            ));
            
            // Check if any changes were made (any individual statistic > 0)
            final hasChanges = exactDuplicatesRemoved > 0 || sameFromToMerged > 0 || redundantConnectionsRemoved > 0;
            
            if (hasChanges) {
              final details = <String>[];
              
              if (exactDuplicatesRemoved > 0) {
                details.add('$exactDuplicatesRemoved exact duplicate edges removed');
              }
              
              if (sameFromToMerged > 0) {
                details.add('$sameFromToMerged merge operations performed (edges with same "from" and "to" combined)');
              }
              
              if (redundantConnectionsRemoved > 0) {
                details.add('$redundantConnectionsRemoved redundant action→action connections removed (instruction provides same input)');
              }
              
              if (details.isEmpty) {
                details.add('Edges optimized and merged');
              }
              
              changes.add(ChangeItem(
                type: ChangeType.merged,
                title: 'Edges Merged/Removed',
                description: '${exactDuplicatesRemoved + sameFromToMerged + redundantConnectionsRemoved} optimization(s) performed',
                details: details,
              ));
            } else {
              changes.add(ChangeItem(
                type: ChangeType.info,
                title: 'No Changes',
                description: 'No duplicate or redundant edges found',
              ));
            }
            
            changes.add(ChangeItem(
              type: ChangeType.modified,
              title: 'Final Edge Count',
              description: '$mergedCount edges after optimization',
            ));
          }
          
          // Show success snackbar
          final message = responseData['message'] ?? 'Edges merged successfully';
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(message),
              backgroundColor: Colors.green,
              duration: const Duration(seconds: 3),
            )
          );
          
          // Show changes summary dialog
          if (changes.isNotEmpty && mounted) {
            ChangesSummaryDialog.show(
              context,
              title: 'Merge Edges - Summary',
              changes: changes,
            );
          }
        } else {
          // API returned success=false
          final errorMsg = responseData['message'] ?? 'Unknown error';
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error: $errorMsg'), backgroundColor: Colors.red)
          );
        }
      } else {
        // HTTP error
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Server error: ${response.statusCode}'),
            backgroundColor: Colors.red,
          )
        );
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error during merge: $e'),
          backgroundColor: Colors.red,
        )
      );
    } finally {
      setState(() {
        isMerging = false;
      });
    }
  }
}
