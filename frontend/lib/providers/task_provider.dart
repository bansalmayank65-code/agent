import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/foundation.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:path_provider/path_provider.dart';
import '../models/task_model.dart';
import '../services/api_service.dart';

// Web-specific imports
import 'dart:html' as html show Blob, Url, AnchorElement;

// Validation step enums and classes
enum ValidationStepStatus { idle, running, success, error }

class ValidationStepResult {
  final String step;
  final ValidationStepStatus status;
  final Map<String, dynamic>? data;
  final String? error;
  final DateTime timestamp;

  ValidationStepResult({
    required this.step,
    required this.status,
    this.data,
    this.error,
    required this.timestamp,
  });
}

class TaskProvider extends ChangeNotifier {
  final ApiService _apiService = ApiService();

  // Task data
  TaskModel _task = const TaskModel(
    instruction: '',
    actions: [],
    actionObjects: null,
    userId: '',
    outputs: [],
    edges: [],
    env: 'finance',
    interfaceNum: 4,
    repositoryPath: '',
    taskId: '',
  );

  // Current task context
  String? _currentTaskId;

  // UI state
  List<bool> _expandedSteps = List.generate(9, (_) => false);
  bool _isLoading = false;
  bool _isInitializing = true;
  String? _error;
  String _validationResult = '';
  String _generatedTaskJson = '';
  bool _isServerConnected = false;
  
  // Import tracking
  bool _hasImportedTaskJson = false;
  String? _importedJsonPath;
  
  // Validation step tracking
  Map<String, ValidationStepResult> _validationStepResults = {};
  Set<String> _runningValidationSteps = {};
  
  // Result data storage (result.json from TauBench execution)
  dynamic _resultData;
  String? _resultFilePath;
  
  // Dirty tracking
  bool _dirtyInstruction = false;
  bool _dirtyActions = false;
  bool _dirtyUserId = false;
  bool _dirtyOutputs = false;
  bool _dirtyEdges = false;
  bool _dirtyParams = false;
  bool _dirtyRepo = false;
  
  // Edge schema validation result
  ValidationResult? _edgeSchemaResult;

  bool get dirtyInstruction => _dirtyInstruction;
  bool get dirtyActions => _dirtyActions;
  bool get dirtyUserId => _dirtyUserId;
  bool get dirtyOutputs => _dirtyOutputs;
  bool get dirtyEdges => _dirtyEdges;
  bool get dirtyParams => _dirtyParams;
  bool get dirtyRepo => _dirtyRepo;

  // Getters
  TaskModel get task => _task;
  String? get currentTaskId => _currentTaskId;
  bool get hasActiveTask => _currentTaskId != null && _currentTaskId!.isNotEmpty;
  List<bool> get expandedSteps => _expandedSteps;
  bool get isLoading => _isLoading;
  bool get isInitializing => _isInitializing;
  String? get error => _error;
  String get validationResult => _validationResult;
  String get generatedTaskJson => _generatedTaskJson;
  bool get isServerConnected => _isServerConnected;
  ValidationResult? get edgeSchemaResult => _edgeSchemaResult;
  bool get hasImportedTaskJson => _hasImportedTaskJson;
  String? get importedJsonPath => _importedJsonPath;
  Map<String, ValidationStepResult> get validationStepResults => _validationStepResults;
  Set<String> get runningValidationSteps => _runningValidationSteps;
  
  // Navigation State Management
  bool get canNavigateToWorkflow => hasActiveTask;
  bool get canNavigateToInstruction => hasActiveTask;
  bool get canNavigateToActions => hasActiveTask;
  bool get canNavigateToUserId => hasActiveTask;
  bool get canNavigateToOutputs => hasActiveTask;
  bool get canNavigateToEdges => hasActiveTask;
  bool get canNavigateToNumberOfEdges => hasActiveTask;
  bool get canNavigateToTaskJson => hasActiveTask;
  bool get canNavigateToValidation => hasActiveTask;
  bool get canNavigateToResults => hasActiveTask;
  
  // Result data getters
  dynamic get resultData => _resultData;
  String? get resultFilePath => _resultFilePath;
  bool get hasResultData => _resultData != null;

  // Constructor
  TaskProvider() {
    _initializeProvider();
  }

  /// Initialize provider with proper loading state management
  Future<void> _initializeProvider() async {
    try {
      await _checkServerConnection();
      await _restorePersistedState();
    } catch (e) {
      if (kDebugMode) {
        print('TaskProvider initialization error: $e');
      }
      // Set error state but don't throw
      _error = 'Initialization failed: ${e.toString()}';
    } finally {
      _isInitializing = false;
      notifyListeners();
    }
  }

  /// Update task instruction
  void updateInstruction(String instruction) {
    _task = _task.copyWith(instruction: instruction);
    _dirtyInstruction = true;
    _clearError();
    notifyListeners();
  }

  void updateEnv(String env) {
    _task = _task.copyWith(env: env);
    _dirtyParams = true;
    _clearError();
    notifyListeners();
  }

  void updateInterfaceNum(int num) {
    _task = _task.copyWith(interfaceNum: num);
    _dirtyParams = true;
    _clearError();
    notifyListeners();
  }

  void updateRepositoryPath(String path) {
    _task = _task.copyWith(repositoryPath: path);
    _clearError();
    notifyListeners();
    _persistRepositoryPath(path);
    // Attempt to load existing task.json automatically
    _loadExistingIfPresent();
    _dirtyRepo = true; // mark until persisted
  }

  /// Update task actions
  void updateActions(List<String> actions) {
    _task = _task.copyWith(actions: actions);
    _dirtyActions = true;
    _clearError();
    notifyListeners();
  }

  /// Replace full action objects (advanced editor / future UI)
  void updateActionObjects(List<Map<String,dynamic>> objects) {
    _task = _task.copyWith(actionObjects: objects);
    // keep simple names list in sync for now
    _task = _task.copyWith(actions: objects.map((e)=> (e['name']??'').toString()).where((e)=> e.isNotEmpty).toList());
    _dirtyActions = true;
    notifyListeners();
  }

  /// Update user ID
  void updateUserId(String userId) {
    _task = _task.copyWith(userId: userId);
    _dirtyUserId = true;
    _clearError();
    notifyListeners();
  }

  /// Update output ids
  void updateOutputs(List<String> outputs) {
    _task = _task.copyWith(outputs: outputs);
    _dirtyOutputs = true;
    _clearError();
    notifyListeners();
  }

  /// Update edges
  void updateEdges(List<Map<String, String>> edges) {
    _task = _task.copyWith(edges: edges);
    _dirtyEdges = true;
    _clearError();
    _validateEdgesSchema();
    notifyListeners();
  }

  /// Toggle step expansion
  void toggleStepExpansion(int stepIndex) {
    _expandedSteps[stepIndex] = !_expandedSteps[stepIndex];
    notifyListeners();
  }

  /// Expand all steps
  void expandAllSteps() {
    _expandedSteps = List.generate(9, (_) => true);
    notifyListeners();
  }

  /// Collapse all steps
  void collapseAllSteps() {
    _expandedSteps = List.generate(9, (_) => false);
    notifyListeners();
  }

  /// Check server connection
  Future<void> _checkServerConnection() async {
    try {
      _isServerConnected = await _apiService.checkServerHealth();
    } catch (e) {
      _isServerConnected = false;
    }
    notifyListeners();
  }

  /// Validate instruction
  Future<void> validateInstruction() async {
    if (_task.instruction.isEmpty) {
      _setError('Instruction cannot be empty');
      return;
    }

    _setLoading(true);
    try {
      _validationResult = await _apiService.validateInstruction(_task.instruction);
      _clearError();
    } catch (e) {
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  /// Generate task JSON
  Future<void> generateTaskJson() async {
    if (_task.instruction.isEmpty) {
      _setError('Instruction is required to generate task JSON');
      return;
    }

    _setLoading(true);
    try {
      _generatedTaskJson = await _apiService.generateTaskJson(_task);
      _clearError();
    } catch (e) {
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  /// Validate task JSON
  Future<void> validateTaskJson() async {
    if (_generatedTaskJson.isEmpty) {
      _setError('Generate task JSON first');
      return;
    }

    _setLoading(true);
    try {
      _validationResult = await _apiService.validateTaskJson(_generatedTaskJson);
      _clearError();
    } catch (e) {
      _setError(e.toString());
    } finally {
      _setLoading(false);
    }
  }

  /// Download all files
  Future<Uint8List?> downloadAllFiles() async {
    _setLoading(true);
    try {
      final data = await _apiService.downloadAllFiles();
      _clearError();
      return data;
    } catch (e) {
      _setError(e.toString());
      return null;
    } finally {
      _setLoading(false);
    }
  }

  /// Reset task data
  void resetTask() {
    _task = const TaskModel(
      instruction: '',
      actions: [],
      userId: '',
      outputs: [],
      edges: [],
      env: 'finance',
      interfaceNum: 4,
      repositoryPath: '',
    );
    _validationResult = '';
    _generatedTaskJson = '';
    _hasImportedTaskJson = false;
    _importedJsonPath = null;
    _validationStepResults.clear();
    _runningValidationSteps.clear();
    
    // Clear result data
    _resultData = null;
    _resultFilePath = null;
    
    _clearError();
    notifyListeners();
    _persistRepositoryPath('');
  }

  /// Create a blank task with minimal structure
  void createBlankTask() {
    _task = TaskModel(
      instruction: 'Enter your task instruction here',
      actions: [],
      actionObjects: null,
      userId: 'user123',
      outputs: [],
      edges: [],
      env: _task.env, // Keep current environment
      interfaceNum: _task.interfaceNum, // Keep current interface
      repositoryPath: _task.repositoryPath, // Keep repository path
    );
    _validationResult = '';
    _generatedTaskJson = '';
    _clearError();
    
    // Mark relevant fields as dirty since we're creating new content
    _dirtyInstruction = true;
    _dirtyUserId = true;
    
    notifyListeners();
  }

  /// Persist current partial state to backend cache
  Future<void> syncCache() async {
    try {
      await _apiService.updateCache(buildCacheUpdatePayload());
      // Reset dirties on successful sync
      _dirtyInstruction = false;
      _dirtyActions = false;
      _dirtyUserId = false;
      _dirtyOutputs = false;
      _dirtyEdges = false;
      _dirtyParams = false;
      _dirtyRepo = false;
      notifyListeners();
    } catch (e) {
      _setError('Cache sync failed: $e');
    }
  }

  Future<String?> saveTaskJson() async {
    try {
      final resp = await _apiService.saveTaskJson(_task.repositoryPath.isEmpty ? null : _task.repositoryPath);
      return resp['taskJson'] as String?;
    } catch (e) {
      _setError('Save failed: $e');
      return null;
    }
  }

  Future<Map<String,dynamic>?> runStep(String step) async {
    try {
      final resp = await _apiService.runValidationStep(step, _task.repositoryPath.isEmpty ? null : _task.repositoryPath);
      return resp;
    } catch (e) {
      _setError('Step $step failed: $e');
      return null;
    }
  }

  Future<String?> fetchCurrentCache() async {
    try { 
      return await _apiService.fetchCurrentCache(); 
    } catch (e) { 
      throw Exception('Failed to fetch current cache: $e');
    }
  }

  // ---------------- Persistence Helpers ----------------
  static const _repoKey = 'last_repository_path';

  Future<void> _persistRepositoryPath(String path) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      if (path.isEmpty) {
        await prefs.remove(_repoKey);
      } else {
        await prefs.setString(_repoKey, path);
      }
    } catch (e) {
      // Log persistence errors but don't throw as this is a background operation
      print('Warning: Failed to persist repository path: $e');
    }
  }

  Future<void> _restorePersistedState() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedPath = prefs.getString(_repoKey);
      if (savedPath != null && savedPath.isNotEmpty) {
        _task = _task.copyWith(repositoryPath: savedPath);
        notifyListeners();
        _loadExistingIfPresent();
      }
    } catch (e) {
      // Log persistence errors but don't throw during initialization
      print('Warning: Failed to restore persisted state: $e');
    }
  }

  Future<void> clearSavedRepositoryPath() async {
    await _persistRepositoryPath('');
    updateRepositoryPath('');
  }

  /// Helper methods
  void _setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  void _setError(String error) {
    _error = error;
    notifyListeners();
  }

  void _clearError() {
    _error = null;
    notifyListeners();
  }

  /// Clear error message (public method)
  void clearError() {
    _clearError();
  }

  /// Check server connection (public method)
  Future<void> checkServerConnection() async {
    await _checkServerConnection();
  }

  /// Parse actions from comma-separated string
  List<String> parseActions(String actionsText) {
    if (actionsText.trim().isEmpty) return [];
    return actionsText
        .split(',')
        .map((action) => action.trim())
        .where((action) => action.isNotEmpty)
        .toList();
  }

  /// Parse output ids from comma-separated string
  List<String> parseOutputs(String outputsText) {
    if (outputsText.trim().isEmpty) return [];
    return outputsText
        .split(',')
        .map((output) => output.trim())
        .where((output) => output.isNotEmpty)
        .toList();
  }

  /// Parse edges from JSON string
  List<Map<String, String>> parseEdges(String edgesText) {
    if (edgesText.trim().isEmpty) return [];
    try {
      final List<dynamic> edgesList = jsonDecode(edgesText);
      return edgesList.map((edge) {
        if (edge is Map) {
          final result = <String,String>{};
          edge.forEach((k,v){
            final key = k.toString();
            if (key == 'connection' && v is Map) {
              // Special handling for connection objects - store as minimal JSON without extra escaping
              result[key] = jsonEncode(v);
            } else if (v is Map || v is List) {
              // For other nested structures, encode normally
              result[key] = jsonEncode(v);
            } else if (v is String && key == 'connection' && (v.startsWith('{') || v.startsWith('['))) {
              // If connection is already a JSON string, validate and keep it
              try {
                jsonDecode(v); // Validate it's valid JSON
                result[key] = v; // Keep the original string
              } catch (_) {
                result[key] = v; // If invalid, keep as-is for user to fix
              }
            } else {
              result[key] = v?.toString() ?? '';
            }
          });
          return result;
        }
        throw Exception('Edge item must be an object');
      }).toList();
    } catch (e) {
      throw Exception('Invalid JSON format for edges');
    }
  }

  /// Clean up malformed connection strings in edges
  void cleanupEdgeConnections() {
    final cleanedEdges = _task.edges.map((edge) {
      final cleanedEdge = <String, String>{};
      edge.forEach((key, value) {
        if (key == 'connection' && value.isNotEmpty) {
          // First check if it's already a properly formatted JSON string
          if (value.startsWith('{') && value.contains('"')) {
            try {
              // Validate that it's proper JSON
              jsonDecode(value);
              // If it parses successfully, keep it as-is
              cleanedEdge[key] = value;
              return;
            } catch (_) {
              // If it doesn't parse, continue with cleanup below
            }
          }
          
          // Check if it's a malformed string representation of an object
          if (value.startsWith('{') && !value.contains('"')) {
            // This looks like {output: action, requester_email, input: action, requester_email}
            // Try to convert it to proper JSON
            try {
              // Simple heuristic to fix common malformed cases
              String fixed = value
                  .replaceAll(RegExp(r'\{([^:]+):([^,}]+)'), r'{"$1":"$2"')
                  .replaceAll(RegExp(r',\s*([^:]+):([^,}]+)'), r',"$1":"$2"')
                  .replaceAll(RegExp(r':\s*([^",}]+)(?=[,}])'), r':"$1"');
              
              // Try to parse the fixed JSON
              final parsed = jsonDecode(fixed);
              if (parsed is Map) {
                cleanedEdge[key] = jsonEncode(parsed);
              } else {
                cleanedEdge[key] = value; // Keep original if conversion fails
              }
            } catch (_) {
              cleanedEdge[key] = value; // Keep original if conversion fails
            }
          } else {
            cleanedEdge[key] = value;
          }
        } else {
          cleanedEdge[key] = value;
        }
      });
      return cleanedEdge;
    }).toList();

    // Update the task with cleaned edges
    _task = _task.copyWith(edges: cleanedEdges);
    _dirtyEdges = true;
    notifyListeners();
  }

  Future<void> _loadExistingIfPresent() async {
    if (_task.repositoryPath.isEmpty) {
      print('TaskProvider: Skipping load existing - repository path is empty'); // Debug log
      return;
    }
    
    print('TaskProvider: Attempting to load existing task from: ${_task.repositoryPath}'); // Debug log
    
    try {
      final resp = await _apiService.loadExisting(_task.repositoryPath);
      final aggregated = resp['aggregated'];
      if (aggregated is String) {
        final decoded = jsonDecode(aggregated) as Map<String,dynamic>;
        final env = decoded['env'] as String? ?? _task.env;
        final iface = decoded['interface_num'] as int? ?? _task.interfaceNum;
        final taskSection = decoded['task'] as Map<String,dynamic>?;
        String instruction = _task.instruction;
        String userId = _task.userId;
  List<String> actions = _task.actions;
  List<Map<String,dynamic>>? actionObjects = _task.actionObjects;
        List<String> outputs = _task.outputs;
        List<Map<String,String>> edges = _task.edges;
        if (taskSection != null) {
          instruction = taskSection['instruction'] as String? ?? instruction;
          userId = taskSection['user_id'] as String? ?? userId;
          final outs = taskSection['outputs'];
            if (outs is List) { outputs = outs.map((e)=> e.toString()).toList(); }
          final acts = taskSection['actions'];
            if (acts is List) {
              // preserve full objects
              final objs = <Map<String,dynamic>>[];
              actions = [];
              for (final a in acts) {
                if (a is Map) {
                  objs.add(a.map((k,v)=> MapEntry(k.toString(), v)));
                  if (a['name'] != null) actions.add(a['name'].toString());
                } else {
                  actions.add(a.toString());
                }
              }
              actionObjects = objs.isEmpty ? null : objs;
            }
          final eds = taskSection['edges'];
            if (eds is List) {
              edges = eds.map((e){
                if (e is Map) {
                  final out = <String,String>{};
                  e.forEach((k,v){
                    if (v is Map || v is List) {
                      out[k.toString()] = jsonEncode(v);
                    } else {
                      out[k.toString()] = v.toString();
                    }
                  });
                  return out;
                }
                return <String,String>{};
              }).toList();
            }
        }
        _task = _task.copyWith(
          env: env,
          interfaceNum: iface,
          instruction: instruction,
          userId: userId,
          actions: actions,
          actionObjects: actionObjects,
          outputs: outputs,
          edges: edges,
        );
        _dirtyInstruction = false;
        _dirtyActions = false;
        _dirtyUserId = false;
        _dirtyOutputs = false;
        _dirtyEdges = false;
        _dirtyParams = false;
        _dirtyRepo = false;
        notifyListeners();
      }
    } catch (e) {
      print('TaskProvider: Error loading existing task from ${_task.repositoryPath}: $e'); // Debug log
      // ignore load errors silently
    }
  }

  Future<bool> reloadFromDisk() async {
    try {
      await _loadExistingIfPresent();
      // Re-run edge schema validation after reload
      _validateEdgesSchema();
      return true;
    } catch (e) {
      // Don't hide the error - let caller know what went wrong
      throw Exception('Failed to reload from disk: $e');
    }
  }

  /// Discard unsaved changes by reloading from last saved state
  Future<void> discardChanges() async {
    try {
      await _loadExistingIfPresent();
      // Re-run edge schema validation after reload
      _validateEdgesSchema();
      // Reset all dirty flags
      _dirtyInstruction = false;
      _dirtyActions = false;
      _dirtyUserId = false;
      _dirtyOutputs = false;
      _dirtyEdges = false;
      _dirtyParams = false;
      _dirtyRepo = false;
      notifyListeners();
    } catch (e) {
      throw Exception('Failed to discard changes and reload: $e');
    }
  }

  /// Import a task.json file content (web upload) and create it in backend database.
  /// Returns a map with 'success' bool and 'message' string for detailed error reporting.
  /// loggedInUserId: Optional - The ID of the currently logged-in user (will override any user_id from the JSON if provided)
  /// dbUserId: Optional - The database user ID for foreign key constraint (should be logged-in user)
  Future<Map<String, dynamic>> importTaskJson(Map<String,dynamic> root, {String? loggedInUserId, String? dbUserId}) async {
    try {
      final env = root['env'] as String? ?? _task.env;
      final iface = (root['interface_num'] is int) ? root['interface_num'] as int : _task.interfaceNum;
      final taskSection = root['task'] as Map<String,dynamic>?;
      String instruction = _task.instruction;
      // Use logged-in user ID if provided, otherwise fall back to task userId or JSON user_id
      String userId = loggedInUserId ?? _task.userId;
      List<String> actions = _task.actions;
      List<Map<String,dynamic>>? actionObjects = _task.actionObjects;
      List<String> outputs = _task.outputs;
      List<Map<String,String>> edges = _task.edges;

      if (taskSection != null){
        instruction = taskSection['instruction'] as String? ?? instruction;
        // Only use JSON user_id if no logged-in user ID was provided
        if (loggedInUserId == null) {
          userId = taskSection['user_id'] as String? ?? userId;
        }
        final outs = taskSection['outputs'];
        if (outs is List) { outputs = outs.map((e)=> e.toString()).toList(); }
        final acts = taskSection['actions'];
        if (acts is List) {
          final objs = <Map<String,dynamic>>[];
          actions = [];
          for (final a in acts) {
            if (a is Map) { objs.add(a.map((k,v)=> MapEntry(k.toString(), v))); if (a['name']!=null) actions.add(a['name'].toString()); }
            else { actions.add(a.toString()); }
          }
          actionObjects = objs.isEmpty ? null : objs;
        }
        final eds = taskSection['edges'];
        if (eds is List) {
          edges = eds.map((e){ 
            if (e is Map){ 
              final out = <String,String>{};
              e.forEach((k,v){
                final key = k.toString();
                if (key == 'connection' && v is Map) {
                  // Special handling for connection objects - store as minimal JSON without extra escaping
                  out[key] = jsonEncode(v);
                } else if (v is Map || v is List) {
                  // For other nested structures, encode normally
                  out[key] = jsonEncode(v);
                } else if (v is String && key == 'connection' && (v.startsWith('{') || v.startsWith('['))) {
                  // If connection is already a JSON string, validate and clean up potential double-escaping
                  try {
                    // First try to parse as-is
                    final parsed = jsonDecode(v);
                    // Re-encode to ensure clean format
                    out[key] = jsonEncode(parsed);
                  } catch (_) {
                    // If parsing fails, check if it's double-escaped
                    try {
                      // Try to fix common double-escaping patterns
                      String cleaned = v
                          .replaceAll(r'\"', '"')  // Fix double-escaped quotes
                          .replaceAll(r'\\', '\\'); // Fix double-escaped backslashes
                      final parsed = jsonDecode(cleaned);
                      out[key] = jsonEncode(parsed);
                    } catch (_) {
                      out[key] = v; // If all fails, keep as-is for user to fix
                    }
                  }
                } else {
                  out[key] = v?.toString() ?? '';
                }
              });
              return out; 
            } 
            return <String,String>{}; 
          }).toList();
        }
      }

      // Update local task state
      _task = _task.copyWith(env: env, interfaceNum: iface, instruction: instruction, userId: userId, actions: actions, actionObjects: actionObjects, outputs: outputs, edges: edges);
      _dirtyInstruction = false; _dirtyActions = false; _dirtyUserId = false; _dirtyOutputs = false; _dirtyEdges = false; _dirtyParams = false; _dirtyRepo = false;
      
      // Import to backend database (task ID will be generated by backend)
      try {
        // Validate that we have a valid userId before making API call
        if (userId.isEmpty) {
          throw Exception('User ID is required to import task. Please ensure you are logged in.');
        }
        
        final taskJsonString = jsonEncode(root);
        final response = await _apiService.importTask(userId, taskJsonString, dbUserId: dbUserId);
        
        if (response['success'] != true) {
          final errorMsg = response['message'] ?? 'Unknown backend error';
          throw Exception('Backend import failed: $errorMsg');
        }
        
        // Extract the generated task ID from backend response
        final generatedTaskId = response['taskId'] as String?;
        if (generatedTaskId == null) {
          throw Exception('Backend did not return a task ID');
        }
        
        print('Successfully imported task $generatedTaskId to backend database');
        
        // Update local task with the backend-generated task ID and set as current
        _task = _task.copyWith(taskId: generatedTaskId);
        _currentTaskId = generatedTaskId;
      } catch (e) {
        // If backend import fails, we should fail the entire operation
        throw Exception('Failed to save task to backend database: $e');
      }
      
      // Set import tracking
      _hasImportedTaskJson = true;
      
      // Clear previous result data since we have a new task
      _resultData = null;
      _resultFilePath = null;
      
      _validateEdgesSchema();
      notifyListeners();
      return {
        'success': true, 
        'message': 'Task imported successfully',
        'taskId': _currentTaskId
      };
    } catch (e) { 
      print('Error importing task JSON: $e');
      return {'success': false, 'message': 'Error importing task: $e'}; 
    }
  }

  /// Set imported JSON path for UI display
  void setImportedJsonPath(String? path) {
    _importedJsonPath = path;
    notifyListeners();
  }

  /// Clear import tracking (e.g., when creating new task)
  void clearImportTracking() {
    _hasImportedTaskJson = false;
    _importedJsonPath = null;
    notifyListeners();
  }

  /// Set current task context from task history selection
  void selectTaskFromHistory(String taskId, TaskModel taskData) {
    _currentTaskId = taskId;
    _task = taskData.copyWith(taskId: taskId);
    _hasImportedTaskJson = true; // Mark as having task data
    
    // Reset all dirty flags since this is loaded from history
    _dirtyInstruction = false;
    _dirtyActions = false;
    _dirtyUserId = false;
    _dirtyOutputs = false;
    _dirtyEdges = false;
    _dirtyParams = false;
    _dirtyRepo = false;
    
    notifyListeners();
  }

  /// Load task by ID from task history for navigation - imports full task data
  Future<void> loadTaskById(String taskId, String userId) async {
    _setLoading(true);
    _clearError();
    
    try {
      // First check if this task is already in the current context
      if (_currentTaskId == taskId) {
        _setLoading(false);
        return; // Already loaded
      }
      
      // Get the full task details from the database
      final response = await _apiService.getTaskDetails(userId, taskId);
      if (response['success'] == true) {
        final taskData = response['data'];
        
        if (taskData != null) {
          // Extract the stored JSON content from the database
          final String? storedJsonContent = taskData['jsonContent'];
          
          if (storedJsonContent != null && storedJsonContent.isNotEmpty) {
            // Parse the stored JSON content - this is the full task import
            final Map<String, dynamic> taskJson = jsonDecode(storedJsonContent);
            
            // Load the task content without importing to database (since it's already there)
            _loadTaskFromJson(taskJson, userId, taskId);
            
            // Set the task ID to match the database record
            _currentTaskId = taskId;
            _task = _task.copyWith(
              taskId: taskId,
              userId: userId,
            );
            
            print('Successfully loaded and imported task $taskId from database');
            print('Task instruction: ${_task.instruction}');
            print('Task actions: ${_task.actions}');
            print('Task outputs: ${_task.outputs}');
          } else {
            // Task exists but has no JSON content - create minimal task
            _currentTaskId = taskId;
            _task = _task.copyWith(
              userId: userId,
              taskId: taskId,
            );
            _hasImportedTaskJson = true;
            
            print('Loaded task $taskId with minimal data (no JSON content)');
          }
          
          // Reset all dirty flags since this is a fresh load from DB
          _dirtyInstruction = false;
          _dirtyActions = false;
          _dirtyUserId = false;
          _dirtyOutputs = false;
          _dirtyEdges = false;
          _dirtyParams = false;
          _dirtyRepo = false;
          
          notifyListeners();
        } else {
          throw Exception('Task data is null');
        }
      } else {
        throw Exception('Failed to load task details: ${response['message'] ?? 'Unknown error'}');
      }
    } catch (e) {
      _error = 'Failed to load and import task: $e';
      throw Exception(_error);
    } finally {
      _setLoading(false);
    }
  }

  /// Helper method to load task content from JSON without database import
  void _loadTaskFromJson(Map<String, dynamic> root, String userId, String taskId) {
    final env = root['env'] as String? ?? _task.env;
    final iface = (root['interface_num'] is int) ? root['interface_num'] as int : _task.interfaceNum;
    final taskSection = root['task'] as Map<String,dynamic>?;
    String instruction = _task.instruction;
    List<String> actions = _task.actions;
    List<Map<String,dynamic>>? actionObjects = _task.actionObjects;
    List<String> outputs = _task.outputs;
    List<Map<String,String>> edges = _task.edges;

    if (taskSection != null){
      instruction = taskSection['instruction'] as String? ?? instruction;
      final outs = taskSection['outputs'];
      if (outs is List) { outputs = outs.map((e)=> e.toString()).toList(); }
      final acts = taskSection['actions'];
      if (acts is List) {
        final objs = <Map<String,dynamic>>[];
        actions = [];
        for (final a in acts) {
          if (a is Map) { objs.add(a.map((k,v)=> MapEntry(k.toString(), v))); if (a['name']!=null) actions.add(a['name'].toString()); }
          else { actions.add(a.toString()); }
        }
        actionObjects = objs.isEmpty ? null : objs;
      }
      final eds = taskSection['edges'];
      if (eds is List) {
        edges = eds.map((e){ 
          if (e is Map){ 
            final out = <String,String>{};
            e.forEach((k,v){
              final key = k.toString();
              if (key == 'connection' && v is Map) {
                out[key] = jsonEncode(v);
              } else if (v is Map || v is List) {
                out[key] = jsonEncode(v);
              } else if (v is String && key == 'connection' && (v.startsWith('{') || v.startsWith('['))) {
                try {
                  final parsed = jsonDecode(v);
                  out[key] = jsonEncode(parsed);
                } catch (_) {
                  try {
                    String cleaned = v
                        .replaceAll(r'\"', '"')
                        .replaceAll(r'\\', '\\');
                    final parsed = jsonDecode(cleaned);
                    out[key] = jsonEncode(parsed);
                  } catch (_) {
                    out[key] = v;
                  }
                }
              } else {
                out[key] = v?.toString() ?? '';
              }
            });
            return out; 
          } 
          return <String,String>{}; 
        }).toList();
      }
    }

    // Update local task state with provided taskId and userId
    _task = _task.copyWith(
      env: env, 
      interfaceNum: iface, 
      instruction: instruction, 
      userId: userId, 
      taskId: taskId,
      actions: actions, 
      actionObjects: actionObjects, 
      outputs: outputs, 
      edges: edges
    );
    
    // Mark as having imported task data
    _hasImportedTaskJson = true;
    
    // Clear previous result data since we have a loaded task
    _resultData = null;
    _resultFilePath = null;
    
    _validateEdgesSchema();
  }

  /// Clear current task context (return to initial state)
  void clearTaskContext() {
    _currentTaskId = null;
    _task = const TaskModel(
      instruction: '',
      actions: [],
      actionObjects: null,
      userId: '',
      outputs: [],
      edges: [],
      env: 'finance',
      interfaceNum: 4,
      repositoryPath: '',
      taskId: '',
    );
    _hasImportedTaskJson = false;
    _importedJsonPath = null;
    
    // Reset all dirty flags
    _dirtyInstruction = false;
    _dirtyActions = false;
    _dirtyUserId = false;
    _dirtyOutputs = false;
    _dirtyEdges = false;
    _dirtyParams = false;
    _dirtyRepo = false;
    
    // Clear result data
    _resultData = null;
    _resultFilePath = null;
    
    notifyListeners();
  }

  /// Set validation step result
  void setValidationStepResult(String step, ValidationStepResult result) {
    _validationStepResults[step] = result;
    _runningValidationSteps.remove(step);
    notifyListeners();
  }

  /// Mark validation step as running
  void setValidationStepRunning(String step) {
    _runningValidationSteps.add(step);
    _validationStepResults[step] = ValidationStepResult(
      step: step,
      status: ValidationStepStatus.running,
      timestamp: DateTime.now(),
    );
    notifyListeners();
  }

  /// Clear all validation step results
  void clearValidationResults() {
    _validationStepResults.clear();
    _runningValidationSteps.clear();
    notifyListeners();
  }

  /// Return pretty formatted actions JSON (full objects if present)
  String get formattedActionsJson {
    final objs = _task.actionObjects;
    if (objs != null && objs.isNotEmpty) {
      try { 
        return const JsonEncoder.withIndent('  ').convert(objs); 
      } catch (e) {
        throw Exception('Failed to format actions JSON: $e');
      }
    }
    // Use simple list if no action objects available
    try {
      return const JsonEncoder.withIndent('  ').convert(_task.actions);
    } catch (e) {
      throw Exception('Failed to format actions JSON: $e');
    }
  }

  /// Build payload for cache update including actionObjects
  Map<String,Object?> buildCacheUpdatePayload() {
    // Convert edges with connection strings back to proper objects for backend
    List<Map<String, Object>>? processedEdges;
    if (_task.edges.isNotEmpty) {
      processedEdges = _task.edges.map((edge) {
        final result = <String, Object>{};
        edge.forEach((key, value) {
          if (key == 'connection' && value.trim().startsWith('{')) {
            try {
              // Parse connection JSON string back to Map for backend
              final parsed = jsonDecode(value);
              result[key] = parsed;
              print('DEBUG: Converted connection string to object: $parsed');
            } catch (_) {
              // If parsing fails, keep as string
              result[key] = value;
              print('DEBUG: Failed to parse connection, keeping as string: $value');
            }
          } else {
            result[key] = value;
          }
        });
        return result;
      }).toList();
      
      print('DEBUG: Processed edges for backend: $processedEdges');
    }

    return {
      'repositoryPath': _task.repositoryPath.isEmpty ? null : _task.repositoryPath,
      'env': _task.env,
      'interfaceNum': _task.interfaceNum,
      'instruction': _task.instruction.isEmpty ? null : _task.instruction,
      'userId': _task.userId.isEmpty ? null : _task.userId,
      'actions': _task.actions.isEmpty ? null : _task.actions,
      'actionObjects': (_task.actionObjects==null || _task.actionObjects!.isEmpty) ? null : _task.actionObjects,
      'outputs': _task.outputs.isEmpty ? null : _task.outputs,
      'edges': processedEdges,
    };
  }

  /// Perform schema validation on edges ensuring required keys exist:
  /// - from (non-empty)
  /// - to (non-empty)
  /// - connection object with output & input (non-empty)
  /// Because edges are currently stored as Map<String,String>, a nested
  /// connection object must be represented as JSON in the 'connection' string.
  /// Fallback: if parsing fails but top-level contains 'output' and 'input', we accept it.
  ValidationResult _validateEdgesSchema() {
    final errors = <String>[];
    final edges = _task.edges;
    for (int i = 0; i < edges.length; i++) {
      final edge = edges[i];
      final label = 'Edge ${i+1}';
      final from = edge['from']?.trim() ?? '';
      final to = edge['to']?.trim() ?? '';
      if (from.isEmpty) errors.add('$label missing "from"');
      if (to.isEmpty) errors.add('$label missing "to"');
      // Connection validation
      Map<String,dynamic>? conn;
      if (edge.containsKey('connection')) {
        final raw = edge['connection']?.trim() ?? '';
        if (raw.isEmpty) {
          errors.add('$label has empty connection');
        } else {
          if (raw.startsWith('{') && raw.endsWith('}')) {
            try {
              final decoded = jsonDecode(raw);
              if (decoded is Map<String,dynamic>) {
                conn = decoded;
              } else {
                errors.add('$label connection is not an object');
              }
            } catch (_) {
              errors.add('$label connection JSON parse failed');
            }
          } else {
            // Not JSON; attempt simple key=value;key2=value2 parsing
            final map = <String,String>{};
            try {
              for (final part in raw.split(';')) {
                final eq = part.indexOf('=');
                if (eq>0) {
                  final k = part.substring(0,eq).trim();
                  final v = part.substring(eq+1).trim();
                  if (k.isNotEmpty) map[k]=v;
                }
              }
              if (map.isNotEmpty) conn = map;
            } catch (e) {
              throw Exception('Failed to parse edge connection string "$raw": $e');
            }
          }
        }
      }
      // Fallback: treat flattened keys output/input at top-level if no connection field
      if (conn == null && !edge.containsKey('connection')) {
        final outputFlat = edge['output'];
        final inputFlat = edge['input'];
        if (outputFlat != null && outputFlat.trim().isNotEmpty && inputFlat != null && inputFlat.trim().isNotEmpty) {
          conn = {'output': outputFlat.trim(), 'input': inputFlat.trim()};
        } else {
          errors.add('$label missing "connection"');
        }
      }
      if (conn != null) {
        final out = (conn['output']??'').toString().trim();
        final inp = (conn['input']??'').toString().trim();
        if (out.isEmpty) errors.add('$label connection missing "output"');
        if (inp.isEmpty) errors.add('$label connection missing "input"');
      }
    }
    _edgeSchemaResult = ValidationResult(
      isValid: errors.isEmpty,
      message: errors.isEmpty ? 'Edges schema valid' : 'Edges schema has ${errors.length} issue(s)',
      errors: errors,
    );
    notifyListeners();
    return _edgeSchemaResult!;
  }

  /// Public trigger for edge schema validation (e.g., UI button)
  ValidationResult validateEdgesSchema() => _validateEdgesSchema();

  /// TauBench integration methods

  /// Run TauBench validation step 
  Future<Map<String, dynamic>?> runTauBenchStep(String step) async {
    _setLoading(true);
    _clearError();
    
    try {
      Map<String, dynamic> result;
      
      // Validate that we have the required user ID and task ID
      if (_task.userId.isEmpty) {
        throw Exception('No user ID available. Please log in.');
      }
      
      if (_currentTaskId == null || _currentTaskId!.isEmpty) {
        throw Exception('No task selected. Please import or select a task first.');
      }
      
      // Use the actual task ID from import/selection instead of generating one
      final userId = _task.userId;
      final taskId = _currentTaskId!;
      
      // Use ID-based approach instead of file paths
      switch (step) {
        case 'compute_complexity':
          result = await _apiService.computeComplexityById(userId, taskId);
          break;
        case 'task_verification':
          result = await _apiService.taskVerificationById(userId, taskId);
          break;
        case 'run_task':
          result = await _apiService.runTaskById(userId, taskId);
          break;
        case 'evaluate':
          result = await _apiService.evaluateTaskById(userId, taskId);
          break;
        default:
          throw Exception('Unknown validation step: $step');
      }
      
      // Check if the API response indicates failure
      if (result['success'] == false) {
        String errorMsg = result['message'] ?? result['error'] ?? 'API request failed';
        throw Exception(errorMsg);
      }
      
      return result;
    } catch (e) {
      // Extract meaningful error messages from the tau-bench service
      String errorMessage = e.toString();
      
      // Try to extract the detailed error from nested tau-bench response
      if (errorMessage.contains('Verification errors:')) {
        // Extract the verification error details
        final regex = RegExp(r'Verification errors: (.+?)(?:\"}|$)');
        final match = regex.firstMatch(errorMessage);
        if (match != null) {
          errorMessage = 'Task verification failed: ${match.group(1)}';
        }
      } else if (errorMessage.contains('Action order violation:')) {
        // Extract action order violation details
        final regex = RegExp(r'Action order violation: (.+?)(?:\"}|$)');
        final match = regex.firstMatch(errorMessage);
        if (match != null) {
          errorMessage = 'Action order violation: ${match.group(1)}';
        }
      } else if (errorMessage.contains('Invalid model provider and model combination')) {
        errorMessage = 'Validation step failed: The configured AI model (openai:gpt-4o) is not supported by the tau-bench service. Please contact your administrator to update the model configuration.';
      } else if (errorMessage.contains('400 Bad Request')) {
        errorMessage = 'Validation step failed: Invalid request sent to tau-bench service. Please check your task configuration and try again.';
      } else if (errorMessage.contains('Connection refused') || errorMessage.contains('timeout')) {
        errorMessage = 'Validation step failed: Cannot connect to tau-bench service. Please check your network connection and try again later.';
      } else if (errorMessage.contains('401') || errorMessage.contains('403')) {
        errorMessage = 'Validation step failed: Authentication error with tau-bench service. Please contact your administrator.';
      }
      
      _error = errorMessage;
      return {'success': false, 'error': errorMessage};
    } finally {
      _setLoading(false);
    }
  }

  /// Execute TauBench task with specific endpoint using current user and task context
  Future<Map<String, dynamic>?> executeTauBenchTask(String endpoint) async {
    _setLoading(true);
    _clearError();
    
    try {
      // Check if we have a current task context
      if (_task.userId.isEmpty) {
        throw Exception('No user ID available. Please log in.');
      }
      
      if (_currentTaskId == null || _currentTaskId!.isEmpty) {
        throw Exception('No task selected. Please import or select a task first.');
      }
      
      // Use current user ID and task ID
      final result = await _apiService.executeTaskById(_task.userId, _currentTaskId!, endpoint);
      return result;
    } catch (e) {
      // Provide more user-friendly error messages for common issues
      String errorMessage = e.toString();
      
      if (errorMessage.contains('Invalid model provider and model combination')) {
        errorMessage = 'Task execution failed: The configured AI model (openai:gpt-4o) is not supported by the tau-bench service. Please contact your administrator to update the model configuration.';
      } else if (errorMessage.contains('400 Bad Request')) {
        errorMessage = 'Task execution failed: Invalid request sent to tau-bench service. Please check your task configuration and try again.';
      } else if (errorMessage.contains('Connection refused') || errorMessage.contains('timeout')) {
        errorMessage = 'Task execution failed: Cannot connect to tau-bench service. Please check your network connection and try again later.';
      } else if (errorMessage.contains('401') || errorMessage.contains('403')) {
        errorMessage = 'Task execution failed: Authentication error with tau-bench service. Please contact your administrator.';
      }
      
      _error = errorMessage;
      return {'success': false, 'error': errorMessage};
    } finally {
      _setLoading(false);
    }
  }

  /// Get result.json from TauBench execution
  Future<Map<String, dynamic>?> getResults() async {
    _setLoading(true);
    _clearError();
    
    try {
      // Check if we have a current task context
      if (_task.userId.isEmpty) {
        throw Exception('No user ID available. Please log in.');
      }
      
      if (_currentTaskId == null || _currentTaskId!.isEmpty) {
        throw Exception('No task selected. Please import or select a task first.');
      }
      
      // Use current user ID and task ID
      final response = await _apiService.getResults(_task.userId, _currentTaskId!);
      
      // Store result data in memory for cache management
      if (response['success'] == true) {
        _resultData = response['data'];
        _resultFilePath = response['filePath']?.toString();
        notifyListeners();
      }
      
      _setLoading(false);
      return response;
    } catch (e) {
      _setError(e.toString());
      _setLoading(false);
      return null;
    }
  }

  /// Clear result data from memory
  void clearResultData() {
    _resultData = null;
    _resultFilePath = null;
    notifyListeners();
  }

  /// Generate edges using EdgeGenerator
  Future<Map<String, dynamic>?> generateEdges() async {
    _setLoading(true);
    _clearError();
    
    try {
      // Save current task to server first
      await syncCache();
      
      // Use the current task file path
      final taskFilePath = 'task_${DateTime.now().millisecondsSinceEpoch}.json';
      final result = await _apiService.generateEdges(taskFilePath);
      
      if (result['success'] == true && result['edges'] != null) {
        // Update the edges in the task
        final newEdges = (result['edges'] as List).map((edge) {
          if (edge is Map<String, dynamic>) {
            return edge.map((key, value) {
              if (key == 'connection' && value is Map) {
                // For connection objects, encode as proper JSON
                return MapEntry(key, jsonEncode(value));
              } else if (value is Map || value is List) {
                // For other complex objects, encode as JSON
                return MapEntry(key, jsonEncode(value));
              } else {
                // For simple values, convert to string
                return MapEntry(key, value.toString());
              }
            });
          }
          return <String, String>{};
        }).toList();
        
        updateEdges(newEdges);
        
        // Auto-save the updated edges
        await syncCache();
      }
      
      return result;
    } catch (e) {
      _error = e.toString();
      return {'success': false, 'error': e.toString()};
    } finally {
      _setLoading(false);
    }
  }

  /// Check TauBench results status
  Future<Map<String, dynamic>?> checkResultsStatus() async {
    try {
      // Use the current task file path
      final taskFilePath = 'task_${DateTime.now().millisecondsSinceEpoch}.json';
      final result = await _apiService.checkResultsStatus(taskFilePath);
      return result;
    } catch (e) {
      _error = e.toString();
      return {'success': false, 'error': e.toString()};
    }
  }

  /// Get available TauBench endpoints
  Future<List<String>?> getAvailableEndpoints() async {
    try {
      final result = await _apiService.getAvailableEndpoints();
      return result;
    } catch (e) {
      _error = e.toString();
      return null;
    }
  }

  /// Download graph data as JSON file
  Future<void> downloadGraphData(Map<String, dynamic> graphData, String filename) async {
    try {
      final jsonString = jsonEncode(graphData);
      
      // In web environment, create a download link
      if (kIsWeb) {
        final bytes = utf8.encode(jsonString);
        final blob = html.Blob([bytes]);
        final url = html.Url.createObjectUrlFromBlob(blob);
        
        (html.AnchorElement(href: url)
          ..setAttribute('download', filename))
          .click();
        
        html.Url.revokeObjectUrl(url);
      } else {
        // For other platforms, write to downloads directory
        final directory = await getDownloadsDirectory();
        if (directory != null) {
          final file = File('${directory.path}/$filename');
          await file.writeAsString(jsonString);
        }
      }
    } catch (e) {
      _error = 'Failed to download graph data: $e';
      notifyListeners();
    }
  }

  /// Download image from base64 data
  Future<void> downloadImage(String base64Data, String filename) async {
    try {
      final imageBytes = base64Decode(base64Data);
      
      // In web environment, create a download link
      if (kIsWeb) {
        final blob = html.Blob([imageBytes]);
        final url = html.Url.createObjectUrlFromBlob(blob);
        
        (html.AnchorElement(href: url)
          ..setAttribute('download', filename))
          .click();
        
        html.Url.revokeObjectUrl(url);
      } else {
        // For other platforms, write to downloads directory
        final directory = await getDownloadsDirectory();
        if (directory != null) {
          final file = File('${directory.path}/$filename');
          await file.writeAsBytes(imageBytes);
        }
      }
    } catch (e) {
      _error = 'Failed to download image: $e';
      notifyListeners();
    }
  }

  @override
  String toString() => 'TaskProvider(task: '+_task.toJson().toString()+')';
}