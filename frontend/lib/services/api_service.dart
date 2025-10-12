import 'dart:convert';
import 'dart:typed_data';
import 'package:http/http.dart' as http;
import '../models/task_model.dart';

class ApiService {
  static const String baseUrl = 'http://localhost:8080';
  
  // Singleton pattern
  static final ApiService _instance = ApiService._internal();
  factory ApiService() => _instance;
  ApiService._internal();

  // Headers for JSON requests
  Map<String, String> get _jsonHeaders => {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
  };

  // Headers with authorization
  Map<String, String> _authHeaders(String sessionId) => {
    'Content-Type': 'application/json',
    'Accept': 'application/json',
    'Authorization': 'Bearer $sessionId',
  };

  /// Authentication Methods
  
  /// Login user
  Future<Map<String, dynamic>> login(String userId, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/login'),
        headers: _jsonHeaders,
        body: jsonEncode({
          'userId': userId,
          'password': password,
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        // Return the response data as-is, let the caller handle success/failure
        return data;
      } else {
        throw Exception('Login failed: HTTP ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error during login: $e');
    }
  }

  /// Logout user
  Future<Map<String, dynamic>> logout(String sessionId) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/logout'),
        headers: _authHeaders(sessionId),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Logout failed: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error during logout: $e');
    }
  }

  /// Validate session
  Future<bool> validateSession(String sessionId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/auth/validate'),
        headers: _authHeaders(sessionId),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return data['valid'] == true;
      } else {
        return false;
      }
    } catch (e) {
      return false;
    }
  }

  /// Get current user info
  Future<Map<String, dynamic>> getCurrentUser(String sessionId) async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/auth/user'),
        headers: _authHeaders(sessionId),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get user info: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error getting user info: $e');
    }
  }

  /// Register new user (optional)
  Future<Map<String, dynamic>> register(String userId, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/register'),
        headers: _jsonHeaders,
        body: jsonEncode({
          'userId': userId,
          'password': password,
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Registration failed: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error during registration: $e');
    }
  }

  /// Validate instruction text
  Future<String> validateInstruction(String instruction) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/validate-instruction'),
        headers: _jsonHeaders,
        body: jsonEncode({'instruction': instruction}),
      );

      if (response.statusCode == 200) {
        return response.body;
      } else {
        throw Exception('Failed to validate instruction: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error validating instruction: $e');
    }
  }

  /// Generate task.json from TaskModel
  Future<String> generateTaskJson(TaskModel task) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/generate-task'),
        headers: _jsonHeaders,
        body: jsonEncode(task.toJson()),
      );

      if (response.statusCode == 200) {
        return response.body;
      } else {
        throw Exception('Failed to generate task JSON: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error generating task JSON: $e');
    }
  }

  /// Validate task.json
  Future<String> validateTaskJson(String taskJson) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/validate-task'),
        headers: _jsonHeaders,
        body: taskJson,
      );

      if (response.statusCode == 200) {
        return response.body;
      } else {
        throw Exception('Failed to validate task JSON: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error validating task JSON: $e');
    }
  }

  /// Download all files as zip
  Future<Uint8List> downloadAllFiles() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/download-all'),
      );

      if (response.statusCode == 200) {
        return response.bodyBytes;
      } else {
        throw Exception('Failed to download files: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error downloading files: $e');
    }
  }

  /// Health check endpoint
  Future<bool> checkServerHealth() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/actuator/health'),
      ).timeout(const Duration(seconds: 5));

      return response.statusCode == 200;
    } catch (e) {
      return false;
    }
  }

  /// Update cache with partial fields
  Future<Map<String, dynamic>> updateCache(Map<String, dynamic> payload) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/cache/update'),
        headers: _jsonHeaders,
        body: jsonEncode(payload),
      );
      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      }
      throw Exception('Cache update failed: ${response.statusCode}');
    } catch (e) {
      throw Exception('Error updating cache: $e');
    }
  }

  Future<String> fetchCurrentCache() async {
    final response = await http.get(Uri.parse('$baseUrl/cache/current'));
    if (response.statusCode == 200) {
      return response.body;
    }
    throw Exception('Failed to fetch cache: ${response.statusCode}');
  }

  Future<Map<String,dynamic>> saveTaskJson([String? directory]) async {
    final response = await http.post(
      Uri.parse('$baseUrl/cache/save-file'),
      headers: _jsonHeaders,
      body: jsonEncode(directory != null ? {'directory': directory} : {}),
    );
    if (response.statusCode == 200) {
      return jsonDecode(response.body) as Map<String,dynamic>;
    }
    throw Exception('Failed to download task.json: ${response.statusCode}');
  }

  Future<Map<String, dynamic>> runValidationStep(String step, [String? directory]) async {
    final response = await http.post(
      Uri.parse('$baseUrl/cache/validate/$step'),
      headers: _jsonHeaders,
      body: jsonEncode(directory != null ? {'directory': directory} : {}),
    );
    if (response.statusCode == 200 || response.statusCode == 400) { // return body even on logical failure
      return jsonDecode(response.body) as Map<String,dynamic>;
    }
    throw Exception('Failed step $step: ${response.statusCode}');
  }

  Future<Map<String,dynamic>> loadExisting(String directory) async {
    if (directory.isEmpty) {
      throw Exception('Directory path cannot be empty');
    }
    
    print('API: Loading existing task from directory: $directory'); // Debug log
    
    final response = await http.post(
      Uri.parse('$baseUrl/cache/load'),
      headers: _jsonHeaders,
      body: jsonEncode({'directory': directory}),
    );
    
    print('API: Load existing response status: ${response.statusCode}'); // Debug log
    print('API: Load existing response body: ${response.body}'); // Debug log
    
    if (response.statusCode == 200) {
      return jsonDecode(response.body) as Map<String,dynamic>;
    }
    throw Exception('Failed to load existing task.json: ${response.statusCode} - ${response.body}');
  }

  /// List directory contents
  Future<List<Map<String, dynamic>>> listDirectoryContents(String directory) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/files/list'),
        headers: _jsonHeaders,
        body: jsonEncode({'directory': directory}),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return List<Map<String, dynamic>>.from(data['files'] ?? []);
      }
      throw Exception('Failed to list directory: ${response.statusCode}');
    } catch (e) {
      throw Exception('Error listing directory: $e');
    }
  }

  /// Read file content
  Future<String> readFileContent(String filePath) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/files/read'),
        headers: _jsonHeaders,
        body: jsonEncode({'filePath': filePath}),
      );
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as Map<String, dynamic>;
        return data['content'] ?? '';
      }
      throw Exception('Failed to read file: ${response.statusCode}');
    } catch (e) {
      throw Exception('Error reading file: $e');
    }
  }

  /// Write file content
  Future<void> writeFileContent(String filePath, String content) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/files/write'),
        headers: _jsonHeaders,
        body: jsonEncode({'filePath': filePath, 'content': content}),
      );
      if (response.statusCode != 200) {
        throw Exception('Failed to write file: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error writing file: $e');
    }
  }

  /// Delete file
  Future<void> deleteFile(String filePath) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/files/delete'),
        headers: _jsonHeaders,
        body: jsonEncode({'filePath': filePath}),
      );
      if (response.statusCode != 200) {
        throw Exception('Failed to delete file: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Error deleting file: $e');
    }
  }

  /// Get policy content
  Future<Map<String, dynamic>> getPolicy({
    String? directory,
    required String env,
    required int interfaceNum,
  }) async {
    try {
      final uri = Uri.parse('$baseUrl/cache/policy').replace(queryParameters: {
        if (directory != null) 'directory': directory,
        'env': env,
        'interfaceNum': interfaceNum.toString(),
      });

      final response = await http.get(uri, headers: _jsonHeaders);
      
      if (response.statusCode == 200) {
        return json.decode(response.body);
      } else {
        final errorBody = json.decode(response.body);
        throw Exception(errorBody['error'] ?? 'Failed to get policy');
      }
    } catch (e) {
      throw Exception('Error getting policy: $e');
    }
  }

  /// TauBench API methods
  
  /// Execute task with specified endpoint using userId and taskId
  Future<Map<String, dynamic>> executeTaskById(String userId, String taskId, String endpoint) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/execute-by-id');
      final queryParams = <String, String>{
        'userId': userId,
        'taskId': taskId,
        'endpoint': endpoint,
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to execute task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error executing task: $e');
    }
  }

  /// Execute task with specified endpoint (legacy file-based method)
  Future<Map<String, dynamic>> executeTauBenchTask(String endpoint, String taskFilePath) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/tau-bench/execute'),
        headers: _jsonHeaders,
        body: jsonEncode({
          'endpoint': endpoint,
          'taskFilePath': taskFilePath,
        }),
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to execute task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error executing TauBench task: $e');
    }
  }

  /// Execute task with default endpoint
  Future<Map<String, dynamic>> executeTauBenchTaskDefault(String taskFilePath) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/execute/default');
      final uriWithParams = uri.replace(queryParameters: {'taskFilePath': taskFilePath});

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to execute task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error executing TauBench task: $e');
    }
  }

  /// Compute complexity analysis
  Future<Map<String, dynamic>> computeComplexity(String taskFilePath, [int numTrials = 1]) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/compute-complexity');
      final queryParams = <String, String>{
        'numTrials': numTrials.toString(),
        'taskFilePath': taskFilePath,
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to compute complexity: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error computing complexity: $e');
    }
  }

  /// Compute complexity analysis using user ID and task ID
  Future<Map<String, dynamic>> computeComplexityById(String userId, String taskId, [int numTrials = 1]) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/execute-by-id');
      final queryParams = <String, String>{
        'userId': userId,
        'taskId': taskId,
        'endpoint': 'compute_complexity',
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to compute complexity: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error computing complexity: $e');
    }
  }

  /// Task verification
  Future<Map<String, dynamic>> taskVerification(String taskFilePath, [int numTrials = 1]) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/task-verification');
      final queryParams = <String, String>{
        'numTrials': numTrials.toString(),
        'taskFilePath': taskFilePath,
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to verify task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error verifying task: $e');
    }
  }

  /// Task verification using user ID and task ID
  Future<Map<String, dynamic>> taskVerificationById(String userId, String taskId, [int numTrials = 1]) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/execute-by-id');
      final queryParams = <String, String>{
        'userId': userId,
        'taskId': taskId,
        'endpoint': 'task_verification',
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to verify task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error verifying task: $e');
    }
  }

  /// Run task
  Future<Map<String, dynamic>> runTask(String taskFilePath, [int numTrials = 1]) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/run-task');
      final queryParams = <String, String>{
        'numTrials': numTrials.toString(),
        'taskFilePath': taskFilePath,
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to run task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error running task: $e');
    }
  }

  /// Run task using user ID and task ID
  Future<Map<String, dynamic>> runTaskById(String userId, String taskId, [int numTrials = 1]) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/execute-by-id');
      final queryParams = <String, String>{
        'userId': userId,
        'taskId': taskId,
        'endpoint': 'run_task',
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to run task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error running task: $e');
    }
  }

  /// Evaluate task
  Future<Map<String, dynamic>> evaluateTask(String taskFilePath) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/evaluate');
      final uriWithParams = uri.replace(queryParameters: {'taskFilePath': taskFilePath});
      
      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to evaluate task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error evaluating task: $e');
    }
  }

  /// Evaluate task using user ID and task ID
  Future<Map<String, dynamic>> evaluateTaskById(String userId, String taskId) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/execute-by-id');
      final queryParams = <String, String>{
        'userId': userId,
        'taskId': taskId,
        'endpoint': 'evaluate',
      };
      final uriWithParams = uri.replace(queryParameters: queryParams);

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to evaluate task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error evaluating task: $e');
    }
  }

  /// Check results status
  Future<Map<String, dynamic>> checkResultsStatus(String taskFilePath) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/results-status');
      final uriWithParams = uri.replace(queryParameters: {'taskFilePath': taskFilePath});

      final response = await http.get(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to check results: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error checking results: $e');
    }
  }

  /// Get available endpoints
  Future<List<String>> getAvailableEndpoints() async {
    try {
      final response = await http.get(
        Uri.parse('$baseUrl/api/tau-bench/endpoints'),
        headers: _jsonHeaders,
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body) as List;
        return data.cast<String>();
      } else {
        throw Exception('Failed to get endpoints: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error getting endpoints: $e');
    }
  }

  /// Generate edges using EdgeGenerator
  Future<Map<String, dynamic>> generateEdges(String taskFilePath) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/generate-edges');
      final uriWithParams = uri.replace(queryParameters: {'taskFilePath': taskFilePath});

      final response = await http.post(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to generate edges: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error generating edges: $e');
    }
  }

  /// Get result.json content from memory cache using userId and taskId
  Future<Map<String, dynamic>> getResults(String userId, String taskId) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tau-bench/result');
      final uriWithParams = uri.replace(queryParameters: {
        'userId': userId,
        'taskId': taskId
      });

      final response = await http.get(uriWithParams, headers: _jsonHeaders);

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get results: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error getting results: $e');
    }
  }

  /// Import task JSON and create it in the database
  Future<Map<String, dynamic>> importTask(String userId, String taskJsonContent) async {
    try {
      final uri = Uri.parse('$baseUrl/api/tasks/import-task');
      
      // Parse the taskJsonContent to ensure it's valid JSON
      final taskJsonObject = jsonDecode(taskJsonContent);
      
      final requestBody = {
        'userId': userId,
        'taskJsonContent': taskJsonObject,
      };

      final response = await http.post(uri, 
        headers: _jsonHeaders,
        body: jsonEncode(requestBody));

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to import task: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error importing task: $e');
    }
  }

  /// ====================================================================
  /// Task History API Methods
  /// ====================================================================

  /// Get all tasks for a user
  Future<Map<String, dynamic>> getTasksForUser(String userId, [String? sessionId]) async {
    try {
      final headers = sessionId != null ? _authHeaders(sessionId) : _jsonHeaders;
      final response = await http.get(
        Uri.parse('$baseUrl/api/tasks-history/user/$userId'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get tasks for user: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error getting tasks for user: $e');
    }
  }

  /// Get tasks by status for a user
  Future<Map<String, dynamic>> getTasksByStatus(String userId, String status, [String? sessionId]) async {
    try {
      final headers = sessionId != null ? _authHeaders(sessionId) : _jsonHeaders;
      final response = await http.get(
        Uri.parse('$baseUrl/api/tasks-history/user/$userId/status/$status'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get tasks by status: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error getting tasks by status: $e');
    }
  }

  /// Get task statistics for a user
  Future<Map<String, dynamic>> getTaskStatistics(String userId, [String? sessionId]) async {
    try {
      final headers = sessionId != null ? _authHeaders(sessionId) : _jsonHeaders;
      final response = await http.get(
        Uri.parse('$baseUrl/api/tasks-history/user/$userId/statistics'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get task statistics: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error getting task statistics: $e');
    }
  }

  /// Get detailed task information
  Future<Map<String, dynamic>> getTaskDetails(String userId, String taskId, [String? sessionId]) async {
    try {
      final headers = sessionId != null ? _authHeaders(sessionId) : _jsonHeaders;
      final response = await http.get(
        Uri.parse('$baseUrl/api/tasks-history/user/$userId/task/$taskId'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get task details: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error getting task details: $e');
    }
  }

  /// Get recent tasks for a user
  Future<Map<String, dynamic>> getRecentTasks(String userId, [String? sessionId]) async {
    try {
      final headers = sessionId != null ? _authHeaders(sessionId) : _jsonHeaders;
      final response = await http.get(
        Uri.parse('$baseUrl/api/tasks-history/user/$userId/recent'),
        headers: headers,
      );

      if (response.statusCode == 200) {
        return jsonDecode(response.body) as Map<String, dynamic>;
      } else {
        throw Exception('Failed to get recent tasks: ${response.statusCode} - ${response.body}');
      }
    } catch (e) {
      throw Exception('Error getting recent tasks: $e');
    }
  }
}