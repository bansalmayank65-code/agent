import 'package:flutter/material.dart';
import '../../providers/task_provider.dart';
import '../../services/home_screen/ui_helper_service.dart';

/// Project parameters section widget
class ProjectParametersSectionWidget extends StatefulWidget {
  final TaskProvider provider;

  const ProjectParametersSectionWidget({
    super.key,
    required this.provider,
  });

  @override
  State<ProjectParametersSectionWidget> createState() => _ProjectParametersSectionWidgetState();
}

class _ProjectParametersSectionWidgetState extends State<ProjectParametersSectionWidget> {
  final TextEditingController _envController = TextEditingController(text: 'finance');
  final TextEditingController _interfaceController = TextEditingController(text: '4');

  @override
  void initState() {
    super.initState();
    _updateControllers();
  }

  @override
  void dispose() {
    _envController.dispose();
    _interfaceController.dispose();
    super.dispose();
  }

  void _updateControllers() {
    if (_envController.text != widget.provider.task.env) {
      _envController.text = widget.provider.task.env;
    }
    if (_interfaceController.text != widget.provider.task.interfaceNum.toString()) {
      _interfaceController.text = widget.provider.task.interfaceNum.toString();
    }
  }

  @override
  Widget build(BuildContext context) {
    _updateControllers();
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: TextField(
                controller: _envController,
                decoration: const InputDecoration(labelText: 'Environment Name'),
                onChanged: (value) {
                  widget.provider.updateEnv(value);
                },
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: TextField(
                controller: _interfaceController,
                decoration: const InputDecoration(labelText: 'Interface Number'),
                keyboardType: TextInputType.number,
                onChanged: (value) {
                  final number = int.tryParse(value);
                  if (number != null) {
                    widget.provider.updateInterfaceNum(number);
                  }
                },
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        ElevatedButton.icon(
          onPressed: widget.provider.dirtyParams ? _saveParameters : null,
          icon: const Icon(Icons.save),
          label: const Text('Save Parameters'),
        ),
      ],
    );
  }

  Future<void> _saveParameters() async {
    try {
      await widget.provider.syncCache();
      if (mounted) {
        UIHelperService.showToast(context, 'Parameters saved');
      }
    } catch (e) {
      if (mounted) {
        UIHelperService.showToast(context, 'Save failed: $e');
      }
    }
  }
}