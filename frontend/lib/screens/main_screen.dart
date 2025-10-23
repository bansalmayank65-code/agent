import 'dart:io';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:path_provider/path_provider.dart';
import '../providers/task_provider.dart';
import '../widgets/step_card.dart';
import '../widgets/connection_status_widget.dart';
import '../widgets/progress_indicator_widget.dart';
import 'instruction_validation_screen.dart';
import 'task_interface_converter_screen.dart';
import 'edge_merger_screen.dart';
import 'task_refiner_screen.dart';
import 'policy_actions_builder_screen.dart';

class MainScreen extends StatefulWidget {
  const MainScreen({Key? key}) : super(key: key);

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen>
    with TickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;

  // Text controllers for form inputs
  final TextEditingController _instructionController = TextEditingController();
  final TextEditingController _actionsController = TextEditingController();
  final TextEditingController _userIdController = TextEditingController();
  final TextEditingController _outputsController = TextEditingController();
  final TextEditingController _edgesController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 1000),
      vsync: this,
    );
    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeInOut,
    ));
    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    _instructionController.dispose();
    _actionsController.dispose();
    _userIdController.dispose();
    _outputsController.dispose();
    _edgesController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final isDesktop = screenWidth > 1200;
    
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [Color(0xFF667eea), Color(0xFF764ba2)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(6),
              ),
              child: const Center(
                child: Text(
                  'T',
                  style: TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.bold,
                    fontSize: 18,
                  ),
                ),
              ),
            ),
            const SizedBox(width: 12),
            const Text('Amazon Agentic Workstation'),
          ],
        ),
        backgroundColor: Colors.white,
        foregroundColor: const Color(0xFF2d3748),
        elevation: 2,
        actions: [
          Consumer<TaskProvider>(
            builder: (context, provider, child) {
              return IconButton(
                icon: const Icon(Icons.refresh),
                onPressed: provider.isLoading ? null : () => _refreshAll(provider),
                tooltip: 'Refresh',
              );
            },
          ),
          // Always-visible Interface Changer button
          IconButton(
            icon: const Icon(Icons.swap_horiz),
            onPressed: () => _navigateToOtherTools(),
            tooltip: 'Interface Changer',
          ),
          // Web-style navigation for desktop
          if (isDesktop) ...[
            TextButton.icon(
              onPressed: () => _navigateToInstructionValidation(),
              icon: const Icon(Icons.verified),
              label: const Text('Instruction Validation'),
              style: TextButton.styleFrom(
                foregroundColor: const Color(0xFF667eea),
              ),
            ),
            const SizedBox(width: 8),
            TextButton.icon(
              onPressed: () => _navigateToOtherTools(),
              icon: const Icon(Icons.swap_horiz),
              label: const Text('Interface Changer'),
              style: TextButton.styleFrom(
                foregroundColor: const Color(0xFF667eea),
              ),
            ),
            const SizedBox(width: 8),
          ],
          PopupMenuButton<String>(
            icon: const Icon(Icons.more_vert),
            onSelected: (value) => _handleMenuSelection(value),
            itemBuilder: (context) => [
              if (!isDesktop) ...[
                const PopupMenuItem(
                  value: 'instruction_validation',
                  child: Text('Instruction Validation'),
                ),
                const PopupMenuItem(
                  value: 'policy_actions_builder',
                  child: Text('Policy Actions Builder'),
                ),
                const PopupMenuItem(
                  value: 'hr_interface_changer',
                  child: Text('Interface Changer'),
                ),
                const PopupMenuItem(
                  value: 'merge_edges',
                  child: Text('Merge Edges'),
                ),
                const PopupMenuItem(
                  value: 'task_refiner',
                  child: Text('Refine task.json'),
                ),
              ],
              const PopupMenuItem(
                value: 'expand_all',
                child: Text('Expand All Steps'),
              ),
              const PopupMenuItem(
                value: 'collapse_all',
                child: Text('Collapse All Steps'),
              ),
              const PopupMenuItem(
                value: 'reset',
                child: Text('Reset All Data'),
              ),
            ],
          ),
        ],
      ),
      body: FadeTransition(
        opacity: _fadeAnimation,
        child: Column(
          children: [
            // Connection status and progress indicator
            const ConnectionStatusWidget(),
            const ProgressIndicatorWidget(),
            
            // Main content with responsive layout
            Expanded(
              child: Consumer<TaskProvider>(
                builder: (context, provider, child) {
                  return _buildWebLayout(provider, isDesktop);
                },
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: Consumer<TaskProvider>(
        builder: (context, provider, child) {
          return FloatingActionButton.extended(
            onPressed: provider.isLoading ? null : () => _generateAndDownload(provider),
            icon: provider.isLoading 
                ? const SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.download),
            label: const Text('Generate & Download'),
            backgroundColor: const Color(0xFF667eea),
            foregroundColor: Colors.white,
          );
        },
      ),
    );
  }

  Widget _buildWebLayout(TaskProvider provider, bool isDesktop) {
    return SingleChildScrollView(
      padding: EdgeInsets.symmetric(
        horizontal: isDesktop ? 32 : 16,
        vertical: 16,
      ),
      child: Center(
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxWidth: isDesktop ? 1400 : double.infinity,
          ),
          child: Column(
            children: [
              // Error display
              if (provider.error != null) _buildErrorCard(provider.error!),
              
              // Step cards in responsive grid for desktop
              if (isDesktop)
                _buildDesktopStepGrid(provider)
              else
                _buildMobileStepColumn(provider),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildDesktopStepGrid(TaskProvider provider) {
    final steps = _getStepData(provider);
    
    return Wrap(
      spacing: 24,
      runSpacing: 24,
      children: steps.asMap().entries.map((entry) {
        final index = entry.key;
        final step = entry.value;
        
        return SizedBox(
          width: 400, // Fixed width for desktop cards
          child: StepCard(
            stepNumber: index + 1,
            title: step['title'] as String,
            description: step['description'] as String,
            icon: step['icon'] as IconData,
            isExpanded: provider.expandedSteps[index],
            onToggle: () => provider.toggleStepExpansion(index),
            child: step['content'] as Widget,
          ),
        );
      }).toList(),
    );
  }

  Widget _buildMobileStepColumn(TaskProvider provider) {
    final steps = _getStepData(provider);
    
    return Column(
      children: steps.asMap().entries.map((entry) {
        final index = entry.key;
        final step = entry.value;
        
        return StepCard(
          stepNumber: index + 1,
          title: step['title'] as String,
          description: step['description'] as String,
          icon: step['icon'] as IconData,
          isExpanded: provider.expandedSteps[index],
          onToggle: () => provider.toggleStepExpansion(index),
          child: step['content'] as Widget,
        );
      }).toList(),
    );
  }

  void _navigateToInstructionValidation() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => const InstructionValidationScreen(),
      ),
    );
  }

  void _navigateToOtherTools() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => const TaskInterfaceConverterScreen(),
      ),
    );
  }

  void _navigateToHRInterfaceChanger() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => const TaskInterfaceConverterScreen(),
      ),
    );
  }

  void _navigateToPolicyActionsBuilder() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => const PolicyActionsBuilderScreen(),
      ),
    );
  }

  void _navigateToTaskRefiner() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => const TaskRefinerScreen(),
      ),
    );
  }

  void _navigateToEdgeMerger() {
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (context) => const EdgeMergerScreen(),
      ),
    );
  }

  List<Map<String, dynamic>> _getStepData(TaskProvider provider) {
    return [
      {
        'title': 'Step 1: Instruction',
        'description': 'Enter the main instruction for your task',
        'icon': Icons.description,
        'content': _buildInstructionStep(provider),
      },
      {
        'title': 'Step 2: Validate Instruction',
        'description': 'Validate the entered instruction',
        'icon': Icons.check_circle,
        'content': _buildValidateInstructionStep(provider),
      },
      {
        'title': 'Step 3: Actions',
        'description': 'Define actions for your task (comma-separated)',
        'icon': Icons.list,
        'content': _buildActionsStep(provider),
      },
      {
        'title': 'Step 4: User ID',
        'description': 'Specify the user identifier',
        'icon': Icons.person,
        'content': _buildUserIdStep(provider),
      },
      {
        'title': 'Step 5: Outputs',
        'description': 'Define expected outputs (comma-separated)',
        'icon': Icons.output,
        'content': _buildOutputsStep(provider),
      },
      {
        'title': 'Step 6: Edges',
        'description': 'Define task relationships (JSON format)',
        'icon': Icons.account_tree,
        'content': _buildEdgesStep(provider),
      },
      {
        'title': 'Step 7: Generate task.json',
        'description': 'Generate the task configuration file',
        'icon': Icons.code,
        'content': _buildGenerateTaskStep(provider),
      },
      {
        'title': 'Step 8: Validate task.json',
        'description': 'Validate the generated task configuration',
        'icon': Icons.verified,
        'content': _buildValidateTaskStep(provider),
      },
      {
        'title': 'Step 9: Download All',
        'description': 'Download all files as a zip archive',
        'icon': Icons.download,
        'content': _buildDownloadStep(provider),
      },
    ];
  }

  Widget _buildInstructionStep(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _instructionController,
          decoration: const InputDecoration(
            labelText: 'Enter instruction',
            hintText: 'Describe what you want the task to accomplish',
            prefixIcon: Icon(Icons.edit_note),
          ),
          maxLines: 3,
          onChanged: (value) => provider.updateInstruction(value),
        ),
        if (provider.task.instruction.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            color: Colors.green.shade50,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Row(
                children: [
                  Icon(Icons.check, color: Colors.green.shade700),
                  const SizedBox(width: 8),
                  const Text('Instruction entered successfully'),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildValidateInstructionStep(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton.icon(
          onPressed: provider.task.instruction.isEmpty || provider.isLoading
              ? null
              : provider.validateInstruction,
          icon: const Icon(Icons.check_circle),
          label: const Text('Validate Instruction'),
        ),
        if (provider.validationResult.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Validation Result:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(provider.validationResult),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildActionsStep(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _actionsController,
          decoration: const InputDecoration(
            labelText: 'Actions (comma-separated)',
            hintText: 'action1, action2, action3',
            prefixIcon: Icon(Icons.list),
            helperText: 'Enter actions separated by commas',
          ),
          maxLines: 2,
          onChanged: (value) {
            final actions = provider.parseActions(value);
            provider.updateActions(actions);
          },
        ),
        if (provider.task.actions.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Actions:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  ...provider.task.actions.map((action) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(
                      children: [
                        const Icon(Icons.arrow_right, size: 16),
                        const SizedBox(width: 8),
                        Expanded(child: Text(action)),
                      ],
                    ),
                  )),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildUserIdStep(TaskProvider provider) {
    return TextField(
      controller: _userIdController,
      decoration: const InputDecoration(
        labelText: 'User ID',
        hintText: 'Enter user identifier',
        prefixIcon: Icon(Icons.person),
      ),
      onChanged: (value) => provider.updateUserId(value),
    );
  }

  Widget _buildOutputsStep(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _outputsController,
          decoration: const InputDecoration(
            labelText: 'Outputs (comma-separated)',
            hintText: 'output1, output2, output3',
            prefixIcon: Icon(Icons.output),
            helperText: 'Enter expected outputs separated by commas',
          ),
          maxLines: 2,
          onChanged: (value) {
            final outputs = provider.parseOutputs(value);
            provider.updateOutputs(outputs);
          },
        ),
        if (provider.task.outputs.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Outputs:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  ...provider.task.outputs.map((output) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 2),
                    child: Row(
                      children: [
                        const Icon(Icons.arrow_right, size: 16),
                        const SizedBox(width: 8),
                        Expanded(child: Text(output)),
                      ],
                    ),
                  )),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildEdgesStep(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _edgesController,
          decoration: const InputDecoration(
            labelText: 'Edges (JSON format)',
            hintText: '[{"from": "node1", "to": "node2"}]',
            prefixIcon: Icon(Icons.account_tree),
            helperText: 'Enter edges in JSON array format',
          ),
          maxLines: 3,
          onChanged: (value) {
            try {
              final edges = provider.parseEdges(value);
              provider.updateEdges(edges);
            } catch (e) {
              // Handle JSON parsing error
            }
          },
        ),
        const SizedBox(height: 8),
        Card(
          color: Colors.blue.shade50,
          child: const Padding(
            padding: EdgeInsets.all(12),
            child: Text(
              'Example: [{"from": "input", "to": "process"}, {"from": "process", "to": "output"}]',
              style: TextStyle(fontSize: 12, fontStyle: FontStyle.italic),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildGenerateTaskStep(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton.icon(
          onPressed: provider.task.instruction.isEmpty || provider.isLoading
              ? null
              : provider.generateTaskJson,
          icon: const Icon(Icons.code),
          label: const Text('Generate task.json'),
        ),
        if (provider.generatedTaskJson.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Generated task.json:',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      IconButton(
                        icon: const Icon(Icons.copy),
                        onPressed: () => _copyToClipboard(provider.generatedTaskJson),
                        tooltip: 'Copy to clipboard',
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.grey.shade100,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      provider.generatedTaskJson,
                      style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildValidateTaskStep(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        ElevatedButton.icon(
          onPressed: provider.generatedTaskJson.isEmpty || provider.isLoading
              ? null
              : provider.validateTaskJson,
          icon: const Icon(Icons.verified),
          label: const Text('Validate task.json'),
        ),
        if (provider.validationResult.isNotEmpty) ...[
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Validation Result:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  Text(provider.validationResult),
                ],
              ),
            ),
          ),
        ],
      ],
    );
  }

  Widget _buildDownloadStep(TaskProvider provider) {
    return ElevatedButton.icon(
      onPressed: provider.isLoading ? null : () => _downloadFiles(provider),
      icon: const Icon(Icons.download),
      label: const Text('Download All Files'),
    );
  }

  Widget _buildErrorCard(String error) {
    return Card(
      color: Colors.red.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(Icons.error, color: Colors.red.shade700),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                error,
                style: TextStyle(color: Colors.red.shade700),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.close),
              onPressed: () => context.read<TaskProvider>().clearError(),
            ),
          ],
        ),
      ),
    );
  }

  void _handleMenuSelection(String value) {
    final provider = context.read<TaskProvider>();
    switch (value) {
      case 'instruction_validation':
        _navigateToInstructionValidation();
        break;
      case 'policy_actions_builder':
        _navigateToPolicyActionsBuilder();
        break;
      case 'hr_interface_changer':
        _navigateToHRInterfaceChanger();
        break;
      case 'merge_edges':
        _navigateToEdgeMerger();
        break;
      case 'task_refiner':
        _navigateToTaskRefiner();
        break;
      case 'expand_all':
        provider.expandAllSteps();
        break;
      case 'collapse_all':
        provider.collapseAllSteps();
        break;
      case 'reset':
        _showResetDialog();
        break;
    }
  }

  void _showResetDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reset All Data'),
        content: const Text('Are you sure you want to reset all entered data? This action cannot be undone.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () {
              context.read<TaskProvider>().resetTask();
              _clearAllControllers();
              Navigator.of(context).pop();
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('All data has been reset')),
              );
            },
            child: const Text('Reset'),
          ),
        ],
      ),
    );
  }

  void _clearAllControllers() {
    _instructionController.clear();
    _actionsController.clear();
    _userIdController.clear();
    _outputsController.clear();
    _edgesController.clear();
  }

  Future<void> _refreshAll(TaskProvider provider) async {
    // Refresh server connection status
    await provider.checkServerConnection();
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Refreshed')),
    );
  }

  Future<void> _generateAndDownload(TaskProvider provider) async {
    if (provider.task.instruction.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter an instruction first')),
      );
      return;
    }

    // Generate task JSON first
    await provider.generateTaskJson();
    
    if (provider.error == null && provider.generatedTaskJson.isNotEmpty) {
      // Then download files
      await _downloadFiles(provider);
    }
  }

  Future<void> _downloadFiles(TaskProvider provider) async {
    final data = await provider.downloadAllFiles();
    if (data != null) {
      // Save file to downloads directory
      try {
        final directory = await getDownloadsDirectory();
        if (directory != null) {
          final file = File('${directory.path}/agentic_workstation_files.zip');
          await file.writeAsBytes(data);
          
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Files downloaded to ${file.path}'),
                action: SnackBarAction(
                  label: 'Open Folder',
                  onPressed: () {
                    // Open folder (platform-specific implementation needed)
                  },
                ),
              ),
            );
          }
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Error saving file: $e')),
          );
        }
      }
    }
  }

  void _copyToClipboard(String text) {
    // Implement clipboard functionality
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Copied to clipboard')),
    );
  }
}