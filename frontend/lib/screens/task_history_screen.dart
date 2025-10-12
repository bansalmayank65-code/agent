import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../providers/task_history_provider.dart';
import '../providers/auth_provider.dart';
import '../models/task_history_model.dart';

class TaskHistoryScreen extends StatefulWidget {
  final Function(int)? onNavigate;

  const TaskHistoryScreen({super.key, this.onNavigate});

  @override
  State<TaskHistoryScreen> createState() => _TaskHistoryScreenState();
}

class _TaskHistoryScreenState extends State<TaskHistoryScreen> {
  String _selectedFilter = 'ALL';
  String _searchQuery = '';
  final TextEditingController _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadTasks();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadTasks() async {
    final authProvider = context.read<AuthProvider>();
    final taskHistoryProvider = context.read<TaskHistoryProvider>();

    if (authProvider.userId != null) {
      if (_selectedFilter == 'ALL') {
        await taskHistoryProvider.loadTasksForUser(
            authProvider.userId!, authProvider.sessionId);
      } else {
        await taskHistoryProvider.loadTasksByStatus(
            authProvider.userId!, _selectedFilter, authProvider.sessionId);
      }
      
      // Also load statistics
      await taskHistoryProvider.loadTaskStatistics(
          authProvider.userId!, authProvider.sessionId);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer2<TaskHistoryProvider, AuthProvider>(
      builder: (context, taskProvider, authProvider, child) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            // Header
            const Text(
              'Tasks History',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Color(0xFF111827),
              ),
            ),
            const SizedBox(height: 16),

            // Statistics Cards
            if (taskProvider.statusCounts.isNotEmpty)
              _buildStatisticsCards(taskProvider.statusCounts),

            const SizedBox(height: 24),

            // Search and Filter Controls
            _buildSearchAndFilter(taskProvider),

            const SizedBox(height: 16),

            // Tasks List
            _buildTasksList(taskProvider, authProvider),
          ],
        );
      },
    );
  }

  Widget _buildStatisticsCards(Map<String, int> statusCounts) {
    final totalTasks = statusCounts.values.fold(0, (sum, count) => sum + count);
    
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withOpacity(0.1),
            spreadRadius: 1,
            blurRadius: 3,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'Task Overview',
            style: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Color(0xFF374151),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _buildStatCard('Total Tasks', totalTasks, Colors.blue),
              const SizedBox(width: 16),
              _buildStatCard('Completed', 
                  (statusCounts['APPROVED'] ?? 0) + (statusCounts['MERGED'] ?? 0), 
                  Colors.green),
              const SizedBox(width: 16),
              _buildStatCard('Needs Attention', 
                  (statusCounts['REJECTED'] ?? 0) + (statusCounts['NEEDS_CHANGES'] ?? 0), 
                  Colors.orange),
            ],
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: statusCounts.entries.map((entry) {
              return Chip(
                label: Text('${entry.key}: ${entry.value}'),
                backgroundColor: _getStatusColor(entry.key).withOpacity(0.1),
                labelStyle: TextStyle(
                  color: _getStatusColor(entry.key),
                  fontSize: 12,
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(String label, int value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          color: color.withOpacity(0.1),
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: color.withOpacity(0.3)),
        ),
        child: Column(
          children: [
            Text(
              value.toString(),
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.bold,
                color: color,
              ),
            ),
            Text(
              label,
              style: TextStyle(
                fontSize: 12,
                color: color,
              ),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSearchAndFilter(TaskHistoryProvider taskProvider) {
    return Row(
      children: [
        // Search
        Expanded(
          flex: 2,
          child: TextField(
            controller: _searchController,
            decoration: const InputDecoration(
              hintText: 'Search tasks...',
              prefixIcon: Icon(Icons.search),
              border: OutlineInputBorder(),
              contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            ),
            onChanged: (value) {
              setState(() {
                _searchQuery = value;
              });
            },
          ),
        ),
        const SizedBox(width: 16),
        
        // Status Filter
        Expanded(
          flex: 1,
          child: DropdownButtonFormField<String>(
            value: _selectedFilter,
            decoration: const InputDecoration(
              labelText: 'Filter by Status',
              border: OutlineInputBorder(),
              contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            ),
            items: [
              const DropdownMenuItem(value: 'ALL', child: Text('All Tasks')),
              const DropdownMenuItem(value: 'DRAFT', child: Text('Draft')),
              const DropdownMenuItem(value: 'SUBMITTED', child: Text('Submitted')),
              const DropdownMenuItem(value: 'APPROVED', child: Text('Approved')),
              const DropdownMenuItem(value: 'REJECTED', child: Text('Rejected')),
              const DropdownMenuItem(value: 'NEEDS_CHANGES', child: Text('Needs Changes')),
              const DropdownMenuItem(value: 'MERGED', child: Text('Merged')),
              const DropdownMenuItem(value: 'DISCARDED', child: Text('Discarded')),
            ],
            onChanged: (value) {
              setState(() {
                _selectedFilter = value!;
              });
              _loadTasks();
            },
          ),
        ),
        const SizedBox(width: 16),
        
        // Refresh button
        IconButton(
          icon: const Icon(Icons.refresh),
          onPressed: taskProvider.isLoading ? null : _loadTasks,
          tooltip: 'Refresh',
        ),
      ],
    );
  }

  Widget _buildTasksList(TaskHistoryProvider taskProvider, AuthProvider authProvider) {
    if (taskProvider.isLoading) {
      return const SizedBox(
        height: 200,
        child: Center(
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (taskProvider.errorMessage != null) {
      return SizedBox(
        height: 200,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
            Icon(Icons.error_outline, size: 48, color: Colors.red[300]),
            const SizedBox(height: 16),
            Text(
              'Error: ${taskProvider.errorMessage}',
              style: const TextStyle(color: Colors.red),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _loadTasks,
              child: const Text('Retry'),
            ),
          ],
        ),
      ),
    );
    }

    List<TaskHistoryModel> filteredTasks = taskProvider.tasks;
    if (_searchQuery.isNotEmpty) {
      filteredTasks = taskProvider.searchTasks(_searchQuery);
    }

    if (filteredTasks.isEmpty) {
      return SizedBox(
        height: 200,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.assignment_outlined, size: 48, color: Colors.grey[400]),
              const SizedBox(height: 16),
              Text(
                _searchQuery.isNotEmpty 
                    ? 'No tasks found matching "$_searchQuery"'
                    : 'No tasks found',
                style: TextStyle(
                  color: Colors.grey[600],
                  fontSize: 16,
                ),
                textAlign: TextAlign.center,
              ),
              if (_searchQuery.isNotEmpty) ...[
                const SizedBox(height: 8),
                TextButton(
                  onPressed: () {
                    _searchController.clear();
                    setState(() {
                      _searchQuery = '';
                    });
                  },
                  child: const Text('Clear search'),
                ),
              ],
            ],
          ),
        ),
      );
    }

    return SizedBox(
      height: 400, // Fixed height for the list
      child: ListView.builder(
        itemCount: filteredTasks.length,
        itemBuilder: (context, index) {
          return _buildTaskCard(filteredTasks[index], authProvider);
        },
      ),
    );
  }

  Widget _buildTaskCard(TaskHistoryModel task, AuthProvider authProvider) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      child: InkWell(
        onTap: () => _showTaskDetails(task, authProvider),
        borderRadius: BorderRadius.circular(4),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header row
              Row(
                children: [
                  Expanded(
                    child: Text(
                      task.taskId,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                        color: Color(0xFF374151),
                      ),
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: _getStatusColor(task.taskStatus).withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: _getStatusColor(task.taskStatus).withOpacity(0.3),
                      ),
                    ),
                    child: Text(
                      task.statusDisplayName,
                      style: TextStyle(
                        color: _getStatusColor(task.taskStatus),
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              
              // Environment and Interface
              Row(
                children: [
                  Icon(Icons.layers, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    task.envName,
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontSize: 12,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Icon(Icons.settings, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    'Interface ${task.interfaceNum}',
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              
              // Instruction
              Text(
                task.shortInstruction,
                style: const TextStyle(
                  fontSize: 13,
                  color: Color(0xFF6B7280),
                ),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 8),
              
              // Footer row
              Row(
                children: [
                  Icon(Icons.person, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(
                    task.userId,
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontSize: 12,
                    ),
                  ),
                  const Spacer(),
                  if (task.hasResults)
                    Tooltip(
                      message: 'Has results',
                      child: Icon(
                        Icons.check_circle,
                        size: 16,
                        color: Colors.green[600],
                      ),
                    ),
                  const SizedBox(width: 8),
                  Text(
                    'Updated: ${task.formattedUpdatedDate}',
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontSize: 12,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showTaskDetails(TaskHistoryModel task, AuthProvider authProvider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Task: ${task.taskId}'),
        content: SizedBox(
          width: double.maxFinite,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _buildDetailRow('Status', task.statusDisplayName),
              _buildDetailRow('Environment', task.envName),
              _buildDetailRow('Interface', task.interfaceNum.toString()),
              _buildDetailRow('User', task.userId),
              _buildDetailRow('Edges', task.numOfEdges.toString()),
              _buildDetailRow('Created', task.formattedCreatedDate),
              _buildDetailRow('Updated', task.formattedUpdatedDate),
              const SizedBox(height: 12),
              const Text(
                'Instruction:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 4),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.grey[50],
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: Colors.grey[300]!),
                ),
                child: Text(
                  task.instruction,
                  style: const TextStyle(fontSize: 13),
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              // Navigate to task workflow screen
              Navigator.pushNamed(context, '/task/${task.taskId}');
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.orange.shade600,
              foregroundColor: Colors.white,
            ),
            child: const Text('Work on Task'),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(
              '$label:',
              style: const TextStyle(fontWeight: FontWeight.w500),
            ),
          ),
          Expanded(
            child: Text(value),
          ),
        ],
      ),
    );
  }

  Color _getStatusColor(String status) {
    switch (status) {
      case 'DRAFT':
        return Colors.grey;
      case 'SUBMITTED':
        return Colors.blue;
      case 'APPROVED':
        return Colors.green;
      case 'REJECTED':
        return Colors.red;
      case 'NEEDS_CHANGES':
        return Colors.orange;
      case 'MERGED':
        return Colors.purple;
      case 'DISCARDED':
        return Colors.grey[400]!;
      default:
        return Colors.grey;
    }
  }
}