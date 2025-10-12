import 'package:flutter/foundation.dart';
import '../models/task_history_model.dart';
import '../services/api_service.dart';

class TaskHistoryProvider with ChangeNotifier {
  List<TaskHistoryModel> _tasks = [];
  Map<String, int> _statusCounts = {};
  bool _isLoading = false;
  String? _errorMessage;
  String? _currentUserId;

  // Getters
  List<TaskHistoryModel> get tasks => _tasks;
  Map<String, int> get statusCounts => _statusCounts;
  bool get isLoading => _isLoading;
  String? get errorMessage => _errorMessage;
  String? get currentUserId => _currentUserId;

  final ApiService _apiService = ApiService();

  /// Load tasks for the specified user
  Future<void> loadTasksForUser(String userId, [String? sessionId]) async {
    _setLoading(true);
    _clearError();
    _currentUserId = userId;

    try {
      final response = await _apiService.getTasksForUser(userId, sessionId);
      
      if (response['success'] == true) {
        final tasksData = response['data'] as List;
        _tasks = tasksData
            .map((task) => TaskHistoryModel.fromJson(task as Map<String, dynamic>))
            .toList();
        
        // Calculate status counts
        _calculateStatusCounts();
        
        if (kDebugMode) {
          print('Loaded ${_tasks.length} tasks for user: $userId');
        }
      } else {
        _setError(response['message'] ?? 'Failed to load tasks');
      }
    } catch (e) {
      _setError('Error loading tasks: $e');
      if (kDebugMode) {
        print('Error loading tasks: $e');
      }
    } finally {
      _setLoading(false);
    }
  }

  /// Load tasks by status
  Future<void> loadTasksByStatus(String userId, String status, [String? sessionId]) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.getTasksByStatus(userId, status, sessionId);
      
      if (response['success'] == true) {
        final tasksData = response['data'] as List;
        _tasks = tasksData
            .map((task) => TaskHistoryModel.fromJson(task as Map<String, dynamic>))
            .toList();
        
        if (kDebugMode) {
          print('Loaded ${_tasks.length} tasks with status $status for user: $userId');
        }
      } else {
        _setError(response['message'] ?? 'Failed to load tasks by status');
      }
    } catch (e) {
      _setError('Error loading tasks by status: $e');
      if (kDebugMode) {
        print('Error loading tasks by status: $e');
      }
    } finally {
      _setLoading(false);
    }
  }

  /// Load recent tasks
  Future<void> loadRecentTasks(String userId, [String? sessionId]) async {
    _setLoading(true);
    _clearError();

    try {
      final response = await _apiService.getRecentTasks(userId, sessionId);
      
      if (response['success'] == true) {
        final tasksData = response['data'] as List;
        _tasks = tasksData
            .map((task) => TaskHistoryModel.fromJson(task as Map<String, dynamic>))
            .toList();
        
        if (kDebugMode) {
          print('Loaded ${_tasks.length} recent tasks for user: $userId');
        }
      } else {
        _setError(response['message'] ?? 'Failed to load recent tasks');
      }
    } catch (e) {
      _setError('Error loading recent tasks: $e');
      if (kDebugMode) {
        print('Error loading recent tasks: $e');
      }
    } finally {
      _setLoading(false);
    }
  }

  /// Load task statistics
  Future<Map<String, dynamic>?> loadTaskStatistics(String userId, [String? sessionId]) async {
    try {
      final response = await _apiService.getTaskStatistics(userId, sessionId);
      
      if (response['success'] == true) {
        final statistics = response['data'] as Map<String, dynamic>;
        
        // Update status counts from statistics
        final statusBreakdown = statistics['statusBreakdown'] as Map<String, dynamic>?;
        if (statusBreakdown != null) {
          _statusCounts = statusBreakdown.map((key, value) => MapEntry(key, (value as num).toInt()));
          notifyListeners();
        }
        
        return statistics;
      }
    } catch (e) {
      if (kDebugMode) {
        print('Error loading task statistics: $e');
      }
    }
    return null;
  }

  /// Get task details
  Future<TaskHistoryModel?> getTaskDetails(String userId, String taskId, [String? sessionId]) async {
    try {
      final response = await _apiService.getTaskDetails(userId, taskId, sessionId);
      
      if (response['success'] == true) {
        return TaskHistoryModel.fromJson(response['data'] as Map<String, dynamic>);
      } else {
        _setError(response['message'] ?? 'Failed to load task details');
      }
    } catch (e) {
      _setError('Error loading task details: $e');
      if (kDebugMode) {
        print('Error loading task details: $e');
      }
    }
    return null;
  }

  /// Filter tasks by status
  List<TaskHistoryModel> getTasksByStatusFilter(String status) {
    return _tasks.where((task) => task.taskStatus == status).toList();
  }

  /// Filter tasks by environment
  List<TaskHistoryModel> getTasksByEnvironment(String envName) {
    return _tasks.where((task) => task.envName == envName).toList();
  }

  /// Filter tasks that need attention
  List<TaskHistoryModel> getTasksNeedingAttention() {
    return _tasks.where((task) => task.needsAttention).toList();
  }

  /// Filter completed tasks
  List<TaskHistoryModel> getCompletedTasks() {
    return _tasks.where((task) => task.isComplete).toList();
  }

  /// Search tasks by instruction content
  List<TaskHistoryModel> searchTasks(String query) {
    if (query.isEmpty) return _tasks;
    
    final lowercaseQuery = query.toLowerCase();
    return _tasks.where((task) {
      return task.instruction.toLowerCase().contains(lowercaseQuery) ||
             task.taskId.toLowerCase().contains(lowercaseQuery) ||
             task.envName.toLowerCase().contains(lowercaseQuery);
    }).toList();
  }

  /// Calculate status counts from current tasks
  void _calculateStatusCounts() {
    _statusCounts = {};
    for (final task in _tasks) {
      _statusCounts[task.taskStatus] = (_statusCounts[task.taskStatus] ?? 0) + 1;
    }
  }

  /// Clear all data
  void clearTasks() {
    _tasks = [];
    _statusCounts = {};
    _currentUserId = null;
    _clearError();
    notifyListeners();
  }

  /// Refresh current data
  Future<void> refresh([String? sessionId]) async {
    if (_currentUserId != null) {
      await loadTasksForUser(_currentUserId!, sessionId);
    }
  }

  /// Get total task count
  int get totalTasks => _tasks.length;

  /// Get unique environment names
  List<String> get environments {
    return _tasks.map((task) => task.envName).toSet().toList();
  }

  /// Get unique users
  List<String> get users {
    return _tasks.map((task) => task.userId).toSet().toList();
  }

  /// Private helper methods
  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void _setError(String error) {
    _errorMessage = error;
    notifyListeners();
  }

  void _clearError() {
    _errorMessage = null;
    notifyListeners();
  }
}