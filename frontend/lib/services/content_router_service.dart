import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';
import '../widgets/home_screen/repository_section_widget.dart';

import '../widgets/home_screen/instruction_section_widget.dart';
import '../widgets/home_screen/actions_section_widget.dart';

class ContentRouterService {
  static Widget getSectionContent(BuildContext context, int index) {
    final provider = context.read<TaskProvider>();
    
    switch (index) {
      case 0:
        return RepositorySectionWidget(provider: provider);
      case 1:
        return _buildProjectParams(context);
      case 2:
        return const InstructionSectionWidget();
      case 3:
        return const ActionsSectionWidget();
      case 4:
        return _buildUserId();
      case 5:
        return _buildOutputs();
      case 6:
        return _buildEdges();
      case 7:
        return _buildNumberOfEdges();
      case 8:
        return _buildTaskJson();
      case 9:
        return _buildValidate();
      default:
        return const Center(child: Text('Section not implemented yet'));
    }
  }

  static String getSectionTitle(int index) {
    switch (index) {
      case 0:
        return 'Select local repository folder';
      case 1:
        return 'Edit project parameters';
      case 2:
        return 'Instruction';
      case 3:
        return 'Actions';
      case 4:
        return 'User ID';
      case 5:
        return 'Outputs';
      case 6:
        return 'Edges';
      case 7:
        return 'Number of edges';
      case 8:
        return 'Task.json';
      case 9:
        return 'Validate Task.json';
      default:
        return 'Unknown Section';
    }
  }

  static Widget _buildProjectParams(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'Project Parameters',
          style: Theme.of(context).textTheme.headlineSmall,
        ),
        const SizedBox(height: 16),
        Consumer<TaskProvider>(
          builder: (context, provider, child) {
            return Column(
              children: [
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: TextEditingController(text: provider.task.env),
                        decoration: const InputDecoration(
                          labelText: 'Environment Name',
                          border: OutlineInputBorder(),
                        ),
                        onChanged: (value) {
                          provider.updateEnv(value);
                        },
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: TextField(
                        controller: TextEditingController(text: provider.task.interfaceNum.toString()),
                        decoration: const InputDecoration(
                          labelText: 'Interface Number',
                          border: OutlineInputBorder(),
                        ),
                        keyboardType: TextInputType.number,
                        onChanged: (value) {
                          final num = int.tryParse(value);
                          if (num != null) {
                            provider.updateInterfaceNum(num);
                          }
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                ElevatedButton.icon(
                  onPressed: provider.dirtyParams ? () async {
                    await provider.syncCache();
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Parameters saved')),
                    );
                  } : null,
                  icon: const Icon(Icons.save),
                  label: const Text('Save Parameters'),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  // Placeholder widgets for sections that haven't been implemented yet
  static Widget _buildUserId() {
    return const Center(
      child: Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.person, size: 48, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'User ID Section',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('This section will be implemented in the next phase.'),
            ],
          ),
        ),
      ),
    );
  }

  static Widget _buildOutputs() {
    return const Center(
      child: Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.output, size: 48, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'Outputs Section',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('This section will be implemented in the next phase.'),
            ],
          ),
        ),
      ),
    );
  }

  static Widget _buildEdges() {
    return const Center(
      child: Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.account_tree, size: 48, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'Edges Section',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('This section will be implemented in the next phase.'),
            ],
          ),
        ),
      ),
    );
  }

  static Widget _buildNumberOfEdges() {
    return const Center(
      child: Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.analytics, size: 48, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'Number of Edges Section',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('This section will be implemented in the next phase.'),
            ],
          ),
        ),
      ),
    );
  }

  static Widget _buildTaskJson() {
    return const Center(
      child: Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.code, size: 48, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'Task.json Section',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('This section will be implemented in the next phase.'),
            ],
          ),
        ),
      ),
    );
  }

  static Widget _buildValidate() {
    return const Center(
      child: Card(
        child: Padding(
          padding: EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.rule_folder, size: 48, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'Validate Task.json Section',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text('This section will be implemented in the next phase.'),
            ],
          ),
        ),
      ),
    );
  }
}