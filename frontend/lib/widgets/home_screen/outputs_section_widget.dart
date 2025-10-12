import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../services/home_screen/ui_helper_service.dart';

/// Widget for Outputs section
class OutputsSectionWidget extends StatefulWidget {
  const OutputsSectionWidget({super.key});

  @override
  State<OutputsSectionWidget> createState() => _OutputsSectionWidgetState();
}

class _OutputsSectionWidgetState extends State<OutputsSectionWidget> {
  final TextEditingController _outputsController = TextEditingController();

  @override
  void dispose() {
    _outputsController.dispose();
    super.dispose();
  }

  /// Build pill-style display for outputs
  Widget _buildPillList(List<String> items) {
    return Wrap(
      spacing: 8,
      runSpacing: 8,
      children: items.map((item) {
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: const Color(0xFF3b82f6).withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: const Color(0xFF3b82f6).withValues(alpha: 0.3)),
          ),
          child: Text(
            item,
            style: const TextStyle(
              fontSize: 12,
              color: Color(0xFF1e40af),
              fontWeight: FontWeight.w500,
            ),
          ),
        );
      }).toList(),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        // Initialize controller if empty and provider has data
        if (_outputsController.text.isEmpty && provider.task.outputs.isNotEmpty) {
          _outputsController.text = provider.task.outputs.join(', ');
        }

        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _outputsController,
              decoration: const InputDecoration(
                labelText: 'Outputs (comma separated)',
                helperText: 'Example: 125, 755, 988',
              ),
              maxLines: 2,
              onChanged: (value) {
                provider.updateOutputs(provider.parseOutputs(value));
              },
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: provider.dirtyOutputs ? () async {
                await provider.syncCache();
                if (mounted) {
                  UIHelperService.showToast(context, 'Outputs saved');
                }
              } : null,
              icon: const Icon(Icons.save),
              label: const Text('Save'),
            ),
            if (provider.task.outputs.isNotEmpty) ...[
              const SizedBox(height: 12),
              const Text(
                'Current Outputs:',
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                  color: Color(0xFF374151),
                ),
              ),
              const SizedBox(height: 8),
              _buildPillList(provider.task.outputs),
            ],
          ],
        );
      },
    );
  }
}