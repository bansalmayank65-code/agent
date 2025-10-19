import 'package:flutter/material.dart';
import '../widgets/json_editor_viewer.dart';
import '../services/api_service.dart';
import 'dart:convert';

/// Screen for refining task.json files with the following operations:
/// 1. Merge duplicate actions
/// 2. Move all *_audit_logs actions to the end
/// 3. Add/fix edges using backend EdgeGenerator
/// 4. Fix num_of_edges based on calculated edges
///
/// Uses side-by-side JSON viewer similar to HR Expert Interface Changer

class TaskRefinerScreen extends StatelessWidget {
  /// If true, wraps content in Scaffold with AppBar (for standalone navigation).
  /// If false, returns just the body content (for embedding in other layouts).
  final bool standalone;
  
  const TaskRefinerScreen({Key? key, this.standalone = true}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bodyContent = _TaskRefinerBody();
    
    if (standalone) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('Refine task.json'),
          backgroundColor: Colors.white,
          foregroundColor: const Color(0xFF2d3748),
        ),
        body: bodyContent,
      );
    }
    
    return bodyContent;
  }
}

/// The actual refiner UI body (without Scaffold/AppBar)
class _TaskRefinerBody extends StatefulWidget {
  @override
  State<_TaskRefinerBody> createState() => _TaskRefinerBodyState();
}

class _TaskRefinerBodyState extends State<_TaskRefinerBody> {
  final beforeController = TextEditingController();
  final afterController = TextEditingController();
  final apiService = ApiService();
  bool isRefining = false;
  String currentStep = '';
  List<String> completedSteps = [];
  int _editorKey = 0; // Key to force re-render of editors
  
  // Refinement operation flags - all enabled by default
  bool mergeDuplicates = true;
  bool moveAuditLogs = true;
  bool generateEdges = true;
  bool updateNumEdges = true;

  @override
  void dispose() {
    beforeController.dispose();
    afterController.dispose();
    super.dispose();
  }

  void _updateStep(String step) {
    setState(() {
      if (currentStep.isNotEmpty) {
        completedSteps.add(currentStep);
      }
      currentStep = step;
    });
  }

  void _resetSteps() {
    setState(() {
      currentStep = '';
      completedSteps = [];
    });
  }

  Widget _buildCheckbox(String label, bool value, ValueChanged<bool?> onChanged) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: 20,
          height: 20,
          child: Checkbox(
            value: value,
            onChanged: isRefining ? null : onChanged,
            materialTapTargetSize: MaterialTapTargetSize.shrinkWrap,
            visualDensity: VisualDensity.compact,
          ),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: const TextStyle(fontSize: 12, color: Color(0xFF4a5568)),
        ),
      ],
    );
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
                child: Text(
                  'Paste your task.json on the left, then click Refine to process it',
                  style: TextStyle(fontSize: 14, color: Color(0xFF4a5568)),
                ),
              ),
              const SizedBox(width: 12),
              ElevatedButton.icon(
                onPressed: isRefining ? null : () => _refineTask(context),
                icon: isRefining 
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Icon(Icons.auto_fix_high),
                label: Text(isRefining ? 'Refining...' : 'Refine'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF4299e1),
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton.icon(
                onPressed: isRefining ? null : () {
                  setState(() {
                    beforeController.clear();
                    afterController.clear();
                    _resetSteps();
                    _editorKey++; // Force re-render of editors
                  });
                },
                icon: const Icon(Icons.clear),
                label: const Text('Clear'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF4a5568),
                  padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                ),
              ),
            ],
          ),
        ),
        // Refinement operations checkboxes
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 12),
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: const Color(0xFFf7fafc),
              border: Border.all(color: const Color(0xFFe2e8f0)),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Select Refinement Operations:',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF2d3748),
                    fontSize: 13,
                  ),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 16,
                  runSpacing: 4,
                  children: [
                    _buildCheckbox(
                      'Merge duplicate actions',
                      mergeDuplicates,
                      (value) => setState(() => mergeDuplicates = value!),
                    ),
                    _buildCheckbox(
                      'Move audit logs to end',
                      moveAuditLogs,
                      (value) => setState(() => moveAuditLogs = value!),
                    ),
                    _buildCheckbox(
                      'Generate edges',
                      generateEdges,
                      (value) => setState(() => generateEdges = value!),
                    ),
                    _buildCheckbox(
                      'Update num_of_edges',
                      updateNumEdges,
                      (value) => setState(() => updateNumEdges = value!),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        // Progress indicator showing current step
        if (isRefining || completedSteps.isNotEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFe6f7ff),
                border: Border.all(color: const Color(0xFF91d5ff)),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        isRefining ? Icons.hourglass_empty : Icons.check_circle,
                        color: isRefining ? const Color(0xFF1890ff) : Colors.green,
                        size: 20,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        isRefining ? 'Refining...' : 'Refinement Complete',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: isRefining ? const Color(0xFF1890ff) : Colors.green,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  ...completedSteps.map((step) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(
                      children: [
                        const Icon(Icons.check, color: Colors.green, size: 16),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            step,
                            style: const TextStyle(fontSize: 13, color: Color(0xFF262626)),
                          ),
                        ),
                      ],
                    ),
                  )),
                  if (currentStep.isNotEmpty)
                    Padding(
                      padding: const EdgeInsets.symmetric(vertical: 2),
                      child: Row(
                        children: [
                          const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              currentStep,
                              style: const TextStyle(
                                fontSize: 13,
                                color: Color(0xFF1890ff),
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                ],
              ),
            ),
          ),
        if (isRefining || completedSteps.isNotEmpty)
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
                    key: ValueKey('before_$_editorKey'),
                    title: 'Original Task',
                    showToolbar: true,
                    splitView: true,
                    controller: beforeController,
                  ),
                ),
                const VerticalDivider(width: 12),
                Expanded(
                  child: JsonEditorViewer(
                    key: ValueKey('after_$_editorKey'),
                    title: 'Refined Task',
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
        // Info footer
        Padding(
          padding: const EdgeInsets.all(12),
          child: Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: const Color(0xFFedf2f7),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: const [
                Text(
                  'Available Refinement Operations (all enabled by default):',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF2d3748),
                  ),
                ),
                SizedBox(height: 4),
                Text(
                  '• Merge duplicate actions - Keeps first occurrence, removes duplicates',
                  style: TextStyle(fontSize: 12, color: Color(0xFF4a5568)),
                ),
                Text(
                  '• Move audit logs to end - Relocates all *_audit_logs actions to the end',
                  style: TextStyle(fontSize: 12, color: Color(0xFF4a5568)),
                ),
                Text(
                  '• Generate edges - Uses EdgeGenerator to calculate action dependencies',
                  style: TextStyle(fontSize: 12, color: Color(0xFF4a5568)),
                ),
                Text(
                  '• Update num_of_edges - Sets num_of_edges based on calculated edges',
                  style: TextStyle(fontSize: 12, color: Color(0xFF4a5568)),
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  /// Call backend API to refine the task
  Future<void> _refineTask(BuildContext context) async {
    // Reset previous steps
    _resetSteps();
    
    // Validate input JSON first
    final beforeText = beforeController.text;
    if (beforeText.trim().isEmpty) {
      _showSnackBar(context, 'Please paste a task.json into the left editor first', isError: true);
      return;
    }

    _updateStep('Validating JSON structure...');
    await Future.delayed(const Duration(milliseconds: 300)); // Brief delay for UX
    
    dynamic taskData;
    try {
      taskData = jsonDecode(beforeText);
    } catch (e) {
      _resetSteps();
      _showSnackBar(context, 'Invalid JSON in left editor', isError: true);
      return;
    }

    // Validate that it has actions
    bool hasActions = false;
    if (taskData is Map) {
      if (taskData.containsKey('actions')) {
        hasActions = true;
      } else if (taskData.containsKey('task') && taskData['task'] is Map) {
        hasActions = (taskData['task'] as Map).containsKey('actions');
      }
    }

    if (!hasActions) {
      _resetSteps();
      _showSnackBar(context, 'Task must contain "actions" field', isError: true);
      return;
    }

    // Start refinement process
    setState(() {
      isRefining = true;
    });

    try {
      // Show steps only for selected operations
      if (mergeDuplicates) {
        _updateStep('Merging duplicate actions...');
        await Future.delayed(const Duration(milliseconds: 300));
      }
      
      if (moveAuditLogs) {
        _updateStep('Moving audit log actions to end...');
        await Future.delayed(const Duration(milliseconds: 300));
      }
      
      if (generateEdges) {
        _updateStep('Generating edges from action dependencies...');
        await Future.delayed(const Duration(milliseconds: 300));
      }
      
      if (updateNumEdges) {
        _updateStep('Calculating num_of_edges...');
      }
      
      // Create options map to pass to backend
      final options = {
        'mergeDuplicates': mergeDuplicates,
        'moveAuditLogs': moveAuditLogs,
        'generateEdges': generateEdges,
        'updateNumEdges': updateNumEdges,
      };
      
      final response = await apiService.refineTask(taskData, options: options);
      
      if (response['success'] == true && response.containsKey('refined_task')) {
        _updateStep('Formatting refined task...');
        await Future.delayed(const Duration(milliseconds: 200));
        
        // Format and display the refined task
        const encoder = JsonEncoder.withIndent('  ');
        afterController.text = encoder.convert(response['refined_task']);
        
        // Mark final step as complete
        setState(() {
          if (currentStep.isNotEmpty) {
            completedSteps.add(currentStep);
          }
          currentStep = '';
        });
        
        _showSnackBar(
          context, 
          response['message'] ?? 'Task refined successfully',
          isError: false,
        );
      } else {
        _resetSteps();
        _showSnackBar(
          context, 
          response['error'] ?? 'Refinement failed',
          isError: true,
        );
      }
    } catch (e) {
      _resetSteps();
      _showSnackBar(context, 'Error: $e', isError: true);
    } finally {
      setState(() {
        isRefining = false;
      });
    }
  }

  void _showSnackBar(BuildContext context, String message, {required bool isError}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? Colors.red : Colors.green,
        duration: Duration(seconds: isError ? 4 : 2),
      ),
    );
  }
}
