import 'package:flutter/material.dart';

/// A simple UI for the TaskInterfaceConverter tool.
/// This widget accepts JSON input or plain text and shows a converted result.
class TaskInterfaceConverter extends StatefulWidget {
  const TaskInterfaceConverter({Key? key}) : super(key: key);

  @override
  State<TaskInterfaceConverter> createState() => _TaskInterfaceConverterState();
}

class _TaskInterfaceConverterState extends State<TaskInterfaceConverter> {
  final TextEditingController _inputController = TextEditingController();
  String _output = '';
  bool _isProcessing = false;

  @override
  void dispose() {
    _inputController.dispose();
    super.dispose();
  }

  void _convert() async {
    setState(() {
      _isProcessing = true;
      _output = '';
    });

    // Placeholder conversion: reverse the input and wrap in a JSON-like block.
    await Future.delayed(const Duration(milliseconds: 200));
    final input = _inputController.text.trim();
    final converted = '{"converted": "${input.split('').reversed.join()}"}';

    setState(() {
      _output = converted;
      _isProcessing = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(12.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            'Task Interface Converter',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          const Text(
            'Paste a task interface or JSON below and press Convert. This tool will transform the input into the target interface format.',
            style: TextStyle(fontSize: 12),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: _inputController,
            decoration: const InputDecoration(
              border: OutlineInputBorder(),
              labelText: 'Input',
              hintText: '{ "name": "example" }',
            ),
            maxLines: 6,
          ),
          const SizedBox(height: 12),
          ElevatedButton.icon(
            onPressed: _isProcessing ? null : _convert,
            icon: _isProcessing ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2)) : const Icon(Icons.build),
            label: const Text('Convert'),
          ),
          const SizedBox(height: 12),
          const Text('Output', style: TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey.shade100,
              borderRadius: BorderRadius.circular(6),
            ),
            child: SelectableText(
              _output.isEmpty ? 'No output yet' : _output,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}
