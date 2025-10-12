import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_provider.dart';
import '../providers/auth_provider.dart';
import '../widgets/home_screen/left_navigation_widget.dart';
import '../services/home_screen/content_router_service.dart';

/// Task workflow screen for working with a specific task
class TaskWorkflowScreen extends StatefulWidget {
  final String taskId;
  final int? initialStepIndex;

  const TaskWorkflowScreen({
    Key? key,
    required this.taskId,
    this.initialStepIndex,
  }) : super(key: key);

  @override
  State<TaskWorkflowScreen> createState() => _TaskWorkflowScreenState();
}

class _TaskWorkflowScreenState extends State<TaskWorkflowScreen> {
  late int _selectedIndex;
  final ScrollController _rightScroll = ScrollController();
  bool _isInitialized = false;
  bool _taskLoaded = false;

  @override
  void initState() {
    super.initState();
    _selectedIndex = widget.initialStepIndex ?? 0;
    
    // Load the task when the screen initializes
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _initializeTask();
    });
  }

  @override
  void dispose() {
    _rightScroll.dispose();
    super.dispose();
  }

  Future<void> _initializeTask() async {
    if (!mounted) return;

    final taskProvider = context.read<TaskProvider>();
    final authProvider = context.read<AuthProvider>();

    try {
      // Check authentication
      if (authProvider.userId != null) {
        // Load the task by ID
        await taskProvider.loadTaskById(widget.taskId, authProvider.userId!);
        setState(() {
          _taskLoaded = true;
          _isInitialized = true;
        });
      } else {
        // Not authenticated, redirect to login
        if (mounted) {
          Navigator.of(context).pushReplacementNamed('/');
        }
      }
    } catch (e) {
      // Task not found or error loading
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error loading task: $e'),
            backgroundColor: Colors.red,
          ),
        );
        // Redirect to task history
        Navigator.of(context).pushReplacementNamed('/tasks');
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (!_isInitialized) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('Loading Task...'),
          backgroundColor: Colors.orange.shade600,
          foregroundColor: Colors.white,
        ),
        body: const Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              CircularProgressIndicator(
                valueColor: AlwaysStoppedAnimation<Color>(Colors.orange),
              ),
              SizedBox(height: 16),
              Text('Loading task workflow...'),
            ],
          ),
        ),
      );
    }

    if (!_taskLoaded) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('Task Not Found'),
          backgroundColor: Colors.red.shade600,
          foregroundColor: Colors.white,
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(
                Icons.error_outline,
                size: 64,
                color: Colors.red,
              ),
              const SizedBox(height: 16),
              const Text(
                'Task not found or failed to load',
                style: TextStyle(fontSize: 18),
              ),
              const SizedBox(height: 24),
              ElevatedButton(
                onPressed: () => Navigator.of(context).pushReplacementNamed('/tasks'),
                child: const Text('Go to Task History'),
              ),
            ],
          ),
        ),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text('Task Workflow - ${widget.taskId.substring(0, 8)}...'),
        backgroundColor: Colors.orange.shade600,
        foregroundColor: Colors.white,
        actions: [
          IconButton(
            icon: const Icon(Icons.history),
            tooltip: 'Task History',
            onPressed: () => Navigator.of(context).pushNamed('/tasks'),
          ),
          Consumer<AuthProvider>(
            builder: (context, auth, child) {
              return PopupMenuButton<String>(
                icon: const Icon(Icons.account_circle),
                onSelected: (value) {
                  if (value == 'logout') {
                    _showLogoutDialog(context, auth);
                  }
                },
                itemBuilder: (context) => [
                  PopupMenuItem<String>(
                    enabled: false,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Logged in as:',
                          style: TextStyle(
                            fontSize: 12,
                            color: Colors.grey.shade600,
                          ),
                        ),
                        Text(
                          auth.userId ?? 'Unknown',
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const PopupMenuDivider(),
                  const PopupMenuItem<String>(
                    value: 'logout',
                    child: Row(
                      children: [
                        Icon(Icons.logout),
                        SizedBox(width: 8),
                        Text('Logout'),
                      ],
                    ),
                  ),
                ],
              );
            },
          ),
        ],
      ),
      body: Consumer<TaskProvider>(
        builder: (context, provider, child) {
          return LayoutBuilder(
            builder: (context, constraints) {
              // Ensure we have valid constraints before building
              if (constraints.maxWidth <= 0 || constraints.maxHeight <= 0) {
                return const Center(child: CircularProgressIndicator());
              }

              return Row(
                children: [
                  // Left Navigation using our extracted widget
                  LeftNavigationWidget(
                    selectedIndex: _selectedIndex,
                    onItemSelected: (index) {
                      if (mounted) {
                        setState(() {
                          _selectedIndex = index;
                        });
                        
                        // Note: For full URL updates, you might want to use go_router or similar package
                        // This would update the URL to '/task/${widget.taskId}/step/$index'
                      }
                    },
                  ),
                  
                  // Main Content using our content router service  
                  Expanded(
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 250),
                      child: ContentRouterService.buildRightContent(
                        context,
                        provider,
                        _selectedIndex,
                        key: ValueKey(_selectedIndex),
                        scrollController: _rightScroll,
                        onNavigate: (index) {
                          if (mounted) {
                            setState(() {
                              _selectedIndex = index;
                            });
                          }
                        },
                      ),
                    ),
                  ),
                ],
              );
            },
          );
        },
      ),
    );
  }

  void _showLogoutDialog(BuildContext context, AuthProvider auth) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Confirm Logout'),
        content: const Text('Are you sure you want to log out?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.of(context).pop();
              auth.logout();
              // Navigate back to login
              Navigator.of(context).pushReplacementNamed('/');
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red.shade600,
              foregroundColor: Colors.white,
            ),
            child: const Text('Logout'),
          ),
        ],
      ),
    );
  }
}