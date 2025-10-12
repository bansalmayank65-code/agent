import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../providers/task_provider.dart';

class InstructionSectionWidget extends StatefulWidget {
  const InstructionSectionWidget({super.key});

  @override
  State<InstructionSectionWidget> createState() => _InstructionSectionWidgetState();
}

class _InstructionSectionWidgetState extends State<InstructionSectionWidget> {
  final TextEditingController _instructionController = TextEditingController();

  @override
  void dispose() {
    _instructionController.dispose();
    super.dispose();
  }

  void _showToast(String message) {
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
    
    // Sync controller with provider data
    if (_instructionController.text.isEmpty && provider.task.instruction.isNotEmpty) {
      _instructionController.text = provider.task.instruction;
    }
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: _instructionController,
          maxLines: 5,
          decoration: const InputDecoration(
            labelText: 'Instruction',
            alignLabelWithHint: true,
            prefixIcon: Icon(Icons.edit),
            border: OutlineInputBorder(),
          ),
          onChanged: (value) {
            provider.updateInstruction(value);
          },
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 12,
          children: [
            ElevatedButton.icon(
              onPressed: provider.dirtyInstruction ? () async {
                await provider.syncCache();
                _showToast('Instruction cached');
              } : null,
              icon: const Icon(Icons.save),
              label: const Text('Save'),
            ),
            OutlinedButton.icon(
              onPressed: provider.task.instruction.isEmpty ? null : () async {
                final uri = Uri.parse('https://turing-amazon-tooling.vercel.app/instruction_validation');
                if (!await launchUrl(uri, mode: LaunchMode.externalApplication)) {
                  _showToast('Could not open validation tool');
                }
              },
              icon: const Icon(Icons.open_in_new),
              label: const Text('Validate (new tab)'),
            ),
          ],
        ),
      ],
    );
  }
}