import 'package:flutter/material.dart';
import '../widgets/json_editor_viewer.dart';
import '../services/api_service.dart';
import '../widgets/common/searchable_dropdown.dart';
import 'dart:convert';

/// Policy Actions Builder Screen
/// Always enabled utility tool for building policy actions from different scenarios
/// Allows users to add, view, edit policy actions stored in database
class PolicyActionsBuilderScreen extends StatefulWidget {
  final bool standalone;

  const PolicyActionsBuilderScreen({Key? key, this.standalone = true}) : super(key: key);

  @override
  State<PolicyActionsBuilderScreen> createState() => _PolicyActionsBuilderScreenState();
}

class _PolicyActionsBuilderScreenState extends State<PolicyActionsBuilderScreen> {
  final ApiService _apiService = ApiService();
  
  // Mode selection: 'add', 'view', 'build'
  String _mode = 'add';
  
  // Controllers for Add/Edit mode
  final TextEditingController _envNameController = TextEditingController();
  final TextEditingController _interfaceNumController = TextEditingController();
  final TextEditingController _policyCat1Controller = TextEditingController();
  final TextEditingController _policyCat2Controller = TextEditingController();
  final TextEditingController _policyDescriptionController = TextEditingController();
  final TextEditingController _actionsJsonController = TextEditingController();
  final TextEditingController _mergedJsonController = TextEditingController();
  
  // View mode filters
  String? _selectedEnvName;
  int? _selectedInterfaceNum;
  String? _selectedPolicyCat1;
  String? _selectedPolicyCat2;
  
  // Dropdown options for View mode
  List<String> _envNameOptions = [];
  List<int> _interfaceNumOptions = [];
  List<String> _policyCat1Options = [];
  List<String> _policyCat2Options = [];
  
  // Dropdown options for Add/Edit mode (to show existing values)
  List<String> _addModeEnvNameOptions = [];
  List<String> _addModePolicyCat1Options = [];
  List<String> _addModePolicyCat2Options = [];
  
  // Selected value for Interface Number dropdown in Add/Edit mode
  int? _addModeSelectedInterfaceNum;
  
  // Policy actions list
  List<Map<String, dynamic>> _policyActions = [];
  
  // Selected policy action for editing
  Map<String, dynamic>? _selectedPolicyAction;
  
  // Build mode state
  String? _buildEnvName;
  int? _buildInterfaceNum;
  String? _buildPolicyCat1;
  String? _buildPolicyCat2;
  List<String> _buildEnvNameOptions = [];
  List<int> _buildInterfaceNumOptions = [];
  List<String> _buildPolicyCat1Options = [];
  List<String> _buildPolicyCat2Options = [];
  List<Map<String, dynamic>> _availablePolicyActions = [];
  List<Map<String, dynamic>> _selectedPolicyActionsForBuild = [];
  String _mergedActionsJson = '[]';
  
  bool _isLoading = false;
  bool _isLoadingAddCat1 = false;
  bool _isLoadingAddCat2 = false;

  @override
  void initState() {
    super.initState();
    _loadDistinctEnvNames();
    _loadBuildEnvNames();
    _loadAddModeOptions();
  }

  // Load existing values for Add mode dropdowns
  Future<void> _loadAddModeOptions() async {
    try {
      // Load all existing environment names
      final envNames = await _apiService.getDistinctEnvNames();
      
      setState(() {
        _addModeEnvNameOptions = envNames;
      });
    } catch (e) {
      // Silent fail - user can still type new values
      print('Failed to load add mode options: $e');
    }
  }

  // Load Policy Category 2 options based on selected Category 1 for Add mode
  Future<void> _loadAddModePolicyCat2Options(String policyCat1) async {
    if (policyCat1.isEmpty) {
      setState(() {
        _addModePolicyCat2Options = [];
      });
      return;
    }

    setState(() => _isLoadingAddCat2 = true);
    try {
      // Load all policy actions with the selected Category 1
      final response = await _apiService.getAllPolicyActions();
      List<Map<String, dynamic>> allActions = [];
      if (response['success'] == true) {
        allActions = List<Map<String, dynamic>>.from(response['data'] ?? []);
      }
      
      // Extract distinct Category 2 values for the selected Category 1
      Set<String> cat2Set = {};
      for (var action in allActions) {
        if (action['policyCat1']?.toString() == policyCat1 &&
            action['policyCat2'] != null && 
            action['policyCat2'].toString().isNotEmpty) {
          cat2Set.add(action['policyCat2'].toString());
        }
      }
      
      setState(() {
        _addModePolicyCat2Options = cat2Set.toList()..sort();
        _isLoadingAddCat2 = false;
      });
    } catch (e) {
      print('Failed to load policy cat2 options: $e');
      setState(() => _isLoadingAddCat2 = false);
    }
  }

  // Load Policy Category 1 options based on selected env and interface for Add mode
  Future<void> _loadAddModePolicyCat1Options() async {
    final envName = _envNameController.text.trim();
    final interfaceNum = _addModeSelectedInterfaceNum;
    
    if (envName.isEmpty || interfaceNum == null) {
      setState(() {
        _addModePolicyCat1Options = [];
      });
      return;
    }

    setState(() => _isLoadingAddCat1 = true);
    try {
      final categories = await _apiService.getDistinctPolicyCat1(envName, interfaceNum);
      setState(() {
        _addModePolicyCat1Options = categories;
        _isLoadingAddCat1 = false;
      });
    } catch (e) {
      print('Failed to load policy cat1 options: $e');
      setState(() => _isLoadingAddCat1 = false);
    }
  }

  @override
  void dispose() {
    _envNameController.dispose();
    _interfaceNumController.dispose();
    _policyCat1Controller.dispose();
    _policyCat2Controller.dispose();
    _policyDescriptionController.dispose();
    _actionsJsonController.dispose();
    _mergedJsonController.dispose();
    super.dispose();
  }

  Future<void> _loadDistinctEnvNames() async {
    try {
      final envNames = await _apiService.getDistinctEnvNames();
      setState(() {
        _envNameOptions = envNames;
      });
    } catch (e) {
      _showError('Failed to load environment names: $e');
    }
  }

  Future<void> _loadDistinctInterfaceNums() async {
    if (_selectedEnvName == null) return;
    
    try {
      final interfaceNums = await _apiService.getDistinctInterfaceNums(_selectedEnvName!);
      setState(() {
        _interfaceNumOptions = interfaceNums;
      });
    } catch (e) {
      _showError('Failed to load interface numbers: $e');
    }
  }

  Future<void> _loadDistinctPolicyCat1() async {
    if (_selectedEnvName == null || _selectedInterfaceNum == null) return;
    
    try {
      final policyCat1 = await _apiService.getDistinctPolicyCat1(_selectedEnvName!, _selectedInterfaceNum!);
      setState(() {
        _policyCat1Options = policyCat1;
        _selectedPolicyCat1 = null;
        _selectedPolicyCat2 = null;
        _policyCat2Options = [];
      });
    } catch (e) {
      _showError('Failed to load policy category 1: $e');
    }
  }

  Future<void> _loadDistinctPolicyCat2() async {
    if (_selectedEnvName == null || _selectedInterfaceNum == null || _selectedPolicyCat1 == null) return;
    
    try {
      final policyCat2 = await _apiService.getDistinctPolicyCat2(
        _selectedEnvName!, _selectedInterfaceNum!, _selectedPolicyCat1!);
      setState(() {
        _policyCat2Options = policyCat2;
        _selectedPolicyCat2 = null;
      });
    } catch (e) {
      _showError('Failed to load policy category 2: $e');
    }
  }

  Future<void> _loadPolicyActions() async {
    if (_selectedEnvName == null || _selectedInterfaceNum == null) {
      _showError('Please select environment name and interface number');
      return;
    }
    
    setState(() => _isLoading = true);
    
    try {
      final response = await _apiService.getPolicyActionsWithFilters(
        envName: _selectedEnvName!,
        interfaceNum: _selectedInterfaceNum!,
        policyCat1: _selectedPolicyCat1,
        policyCat2: _selectedPolicyCat2,
      );
      
      if (response['success'] == true) {
        setState(() {
          _policyActions = List<Map<String, dynamic>>.from(response['data'] ?? []);
        });
      } else {
        _showError(response['message'] ?? 'Failed to load policy actions');
      }
    } catch (e) {
      _showError('Error loading policy actions: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _savePolicyAction() async {
    // Fail-fast validation
    if (_envNameController.text.trim().isEmpty) {
      _showError('Environment name is required');
      return;
    }
    
    if (_interfaceNumController.text.trim().isEmpty) {
      _showError('Interface number is required');
      return;
    }
    
    int? interfaceNum = int.tryParse(_interfaceNumController.text.trim());
    if (interfaceNum == null) {
      _showError('Interface number must be a valid integer');
      return;
    }
    
    if (_policyCat1Controller.text.trim().isEmpty) {
      _showError('Policy category 1 is required');
      return;
    }
    
    if (_policyCat2Controller.text.trim().isEmpty) {
      _showError('Policy category 2 is required');
      return;
    }
    
    if (_actionsJsonController.text.trim().isEmpty) {
      _showError('Actions JSON is required');
      return;
    }
    
    // Validate JSON format
    try {
      jsonDecode(_actionsJsonController.text);
    } catch (e) {
      _showError('Invalid JSON format in actions');
      return;
    }
    
    setState(() => _isLoading = true);
    
    try {
      Map<String, dynamic> response;
      
      if (_mode == 'edit' && _selectedPolicyAction != null) {
        // Update existing policy action
        response = await _apiService.updatePolicyAction(
          policyActionId: _selectedPolicyAction!['policyActionId'],
          envName: _envNameController.text.trim(),
          interfaceNum: interfaceNum,
          policyCat1: _policyCat1Controller.text.trim(),
          policyCat2: _policyCat2Controller.text.trim(),
          policyDescription: _policyDescriptionController.text.trim(),
          actionsJson: _actionsJsonController.text.trim(),
        );
      } else {
        // Create new policy action
        response = await _apiService.createPolicyAction(
          envName: _envNameController.text.trim(),
          interfaceNum: interfaceNum,
          policyCat1: _policyCat1Controller.text.trim(),
          policyCat2: _policyCat2Controller.text.trim(),
          policyDescription: _policyDescriptionController.text.trim(),
          actionsJson: _actionsJsonController.text.trim(),
        );
      }
      
      if (response['success'] == true) {
        _showSuccess(_mode == 'edit' ? 'Policy action updated successfully' : 'Policy action created successfully');
        _clearForm();
        // Reload all dropdown options in case new values were added
        _loadDistinctEnvNames();
        _loadAddModeOptions();
        _refreshBuildTabData();
      } else {
        _showError(response['message'] ?? 'Failed to save policy action');
      }
    } catch (e) {
      // Extract user-friendly error message
      String errorMsg = e.toString();
      
      // Check for duplicate policy combination error
      if (errorMsg.contains('already exists for the combination')) {
        errorMsg = 'This policy has already been added!\n\nA policy action with the same Environment, Interface Number, Category 1, and Category 2 already exists in the database. Please use different values or edit the existing policy.';
      }
      // If it's an HTTP exception with a response message
      else if (errorMsg.contains('Failed to create policy action:') || 
          errorMsg.contains('Failed to update policy action:')) {
        // Extract the part after the colon
        final parts = errorMsg.split(':');
        if (parts.length > 1) {
          errorMsg = parts.skip(1).join(':').trim();
        }
      }
      // Further clean up - remove technical details after "Error:"
      else if (errorMsg.contains('Error: Cannot deserialize')) {
        errorMsg = 'Invalid Actions JSON format. Each action must have "name" (string) and "arguments" (object) fields.';
      }
      
      _showError(errorMsg);
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _deletePolicyAction(int policyActionId) async {
    // Confirm deletion
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Confirm Delete'),
        content: const Text('Are you sure you want to delete this policy action?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    
    if (confirmed != true) return;
    
    setState(() => _isLoading = true);
    
    try {
      final response = await _apiService.deletePolicyAction(policyActionId);
      
      if (response['success'] == true) {
        _showSuccess('Policy action deleted successfully');
        _loadPolicyActions();
        // Refresh Build tab dropdowns after deletion
        _refreshBuildTabData();
      } else {
        _showError(response['message'] ?? 'Failed to delete policy action');
      }
    } catch (e) {
      _showError('Error deleting policy action: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  void _editPolicyAction(Map<String, dynamic> policyAction) {
    setState(() {
      _mode = 'edit';
      _selectedPolicyAction = policyAction;
      _envNameController.text = policyAction['envName'] ?? '';
      _interfaceNumController.text = policyAction['interfaceNum']?.toString() ?? '';
      _policyCat1Controller.text = policyAction['policyCat1'] ?? '';
      _policyCat2Controller.text = policyAction['policyCat2'] ?? '';
      _policyDescriptionController.text = policyAction['policyDescription'] ?? '';
      _actionsJsonController.text = policyAction['actionsJson'] ?? '';
    });
  }

  void _clearForm() {
    _envNameController.clear();
    _interfaceNumController.clear();
    _policyCat1Controller.clear();
    _policyCat2Controller.clear();
    _policyDescriptionController.clear();
    _actionsJsonController.clear();
    setState(() {
      _addModePolicyCat2Options = []; // Clear Category 2 options when form is cleared
      _selectedPolicyAction = null;
      _addModeSelectedInterfaceNum = null;
    });
  }

  void _showError(String message) {
    // For long error messages, show in a dialog instead of snackbar
    if (message.length > 100) {
      showDialog(
        context: context,
        builder: (context) => AlertDialog(
          title: Row(
            children: [
              Icon(Icons.error_outline, color: Colors.red[700], size: 28),
              const SizedBox(width: 12),
              const Text('Error', style: TextStyle(color: Colors.red)),
            ],
          ),
          content: SingleChildScrollView(
            child: Text(
              message,
              style: const TextStyle(fontSize: 14),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('OK'),
            ),
          ],
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(message),
          backgroundColor: Colors.red,
          duration: const Duration(seconds: 4),
        ),
      );
    }
  }

  void _showSuccess(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 3),
      ),
    );
  }

  // Build mode methods
  Future<void> _loadBuildEnvNames() async {
    try {
      final envNames = await _apiService.getDistinctEnvNames();
      setState(() {
        _buildEnvNameOptions = envNames;
      });
    } catch (e) {
      _showError('Failed to load environment names: $e');
    }
  }

  Future<void> _loadBuildInterfaceNums() async {
    if (_buildEnvName == null) return;
    
    try {
      final interfaceNums = await _apiService.getDistinctInterfaceNums(_buildEnvName!);
      setState(() {
        _buildInterfaceNumOptions = interfaceNums;
      });
    } catch (e) {
      _showError('Failed to load interface numbers: $e');
    }
  }

  Future<void> _loadBuildPolicyCat1() async {
    if (_buildEnvName == null || _buildInterfaceNum == null) return;
    
    try {
      final categories = await _apiService.getDistinctPolicyCat1(_buildEnvName!, _buildInterfaceNum!);
      setState(() {
        _buildPolicyCat1Options = categories;
        // Don't auto-select, let user choose or leave as 'All'
      });
    } catch (e) {
      _showError('Failed to load policy categories: $e');
    }
  }

  Future<void> _loadBuildPolicyCat2() async {
    if (_buildEnvName == null || _buildInterfaceNum == null || _buildPolicyCat1 == null) return;
    
    try {
      final categories = await _apiService.getDistinctPolicyCat2(
        _buildEnvName!,
        _buildInterfaceNum!,
        _buildPolicyCat1!,
      );
      setState(() {
        _buildPolicyCat2Options = categories;
        // Don't auto-select, let user choose or leave as 'All'
      });
    } catch (e) {
      _showError('Failed to load policy sub-categories: $e');
    }
  }

  Future<void> _loadAvailablePolicyActions() async {
    if (_buildEnvName == null || _buildInterfaceNum == null) return;
    
    setState(() => _isLoading = true);
    
    try {
      final response = await _apiService.getPolicyActionsWithFilters(
        envName: _buildEnvName!,
        interfaceNum: _buildInterfaceNum!,
        policyCat1: _buildPolicyCat1,
        policyCat2: _buildPolicyCat2,
      );
      
      if (response['success'] == true) {
        setState(() {
          _availablePolicyActions = List<Map<String, dynamic>>.from(response['data'] ?? []);
        });
      } else {
        _showError(response['message'] ?? 'Failed to load policy actions');
      }
    } catch (e) {
      _showError('Error loading policy actions: $e');
    } finally {
      setState(() => _isLoading = false);
    }
  }

  /// Refresh Build Actions tab data after create/update/delete operations
  /// This ensures dropdown values are immediately updated without page refresh
  Future<void> _refreshBuildTabData() async {
    // Reload the base environment names first
    await _loadBuildEnvNames();
    
    // If user has already selected values in Build tab, refresh those cascading dropdowns
    if (_buildEnvName != null) {
      await _loadBuildInterfaceNums();
      
      if (_buildInterfaceNum != null) {
        await _loadBuildPolicyCat1();
        
        if (_buildPolicyCat1 != null) {
          await _loadBuildPolicyCat2();
        }
        
        // Refresh the available policy actions list
        await _loadAvailablePolicyActions();
      }
    }
  }

  void _addPolicyActionToBuild(Map<String, dynamic> policyAction) {
    setState(() {
      _selectedPolicyActionsForBuild.add(policyAction);
      _updateMergedActionsJson();
    });
  }

  void _removePolicyActionFromBuild(int index) {
    setState(() {
      _selectedPolicyActionsForBuild.removeAt(index);
      _updateMergedActionsJson();
    });
  }

  void _movePolicyActionUp(int index) {
    if (index > 0) {
      setState(() {
        final item = _selectedPolicyActionsForBuild.removeAt(index);
        _selectedPolicyActionsForBuild.insert(index - 1, item);
        _updateMergedActionsJson();
      });
    }
  }

  void _movePolicyActionDown(int index) {
    if (index < _selectedPolicyActionsForBuild.length - 1) {
      setState(() {
        final item = _selectedPolicyActionsForBuild.removeAt(index);
        _selectedPolicyActionsForBuild.insert(index + 1, item);
        _updateMergedActionsJson();
      });
    }
  }

  void _updateMergedActionsJson() {
    try {
      List<dynamic> mergedActions = [];
      
      for (var policyAction in _selectedPolicyActionsForBuild) {
        String actionsJson = policyAction['actionsJson'] ?? '[]';
        try {
          var actions = jsonDecode(actionsJson);
          if (actions is List) {
            mergedActions.addAll(actions);
          }
        } catch (e) {
          _showError('Error parsing actions JSON: $e');
        }
      }
      
      setState(() {
        _mergedActionsJson = const JsonEncoder.withIndent('  ').convert(mergedActions);
        _mergedJsonController.text = _mergedActionsJson;
      });
    } catch (e) {
      _showError('Error merging actions: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    // For standalone mode (opened from menu), use full Column with Expanded
    // For embedded mode (in home screen), use shrinkWrap column without Expanded
    final bodyContent = widget.standalone
        ? Column(
            children: [
              _buildModeSelector(),
              Expanded(
                child: _isLoading
                    ? const Center(child: CircularProgressIndicator())
                    : _mode == 'add' || _mode == 'edit'
                        ? _buildAddEditMode()
                        : _mode == 'build'
                            ? _buildBuildMode()
                            : _buildViewMode(),
              ),
            ],
          )
        : Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              _buildModeSelector(),
              _isLoading
                  ? const Center(
                      child: Padding(
                        padding: EdgeInsets.all(32.0),
                        child: CircularProgressIndicator(),
                      ),
                    )
                  : _mode == 'add' || _mode == 'edit'
                      ? _buildAddEditMode()
                      : _mode == 'build'
                          ? _buildBuildMode()
                          : _buildViewMode(),
            ],
          );

    if (widget.standalone) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('Policy Actions Builder'),
          backgroundColor: Colors.white,
          foregroundColor: const Color(0xFF2d3748),
        ),
        body: bodyContent,
      );
    }

    return bodyContent;
  }

  // Helper method to build consistent input decoration
  InputDecoration _buildInputDecoration({
    required String label,
    required String hint,
    required IconData icon,
  }) {
    return InputDecoration(
      labelText: label,
      hintText: hint,
      prefixIcon: Icon(icon, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: Colors.grey[300]!),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: BorderSide(color: Colors.grey[300]!),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(8),
        borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
      ),
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      isDense: true,
      filled: true,
      fillColor: Colors.grey[50],
    );
  }

  Widget _buildModeSelector() {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: _buildModeTab('Add Actions', 'add', Icons.add_circle_outline),
          ),
          Expanded(
            child: _buildModeTab('View Actions', 'view', Icons.view_list_rounded),
          ),
          Expanded(
            child: _buildModeTab('Build Actions', 'build', Icons.construction_rounded),
          ),
        ],
      ),
    );
  }

  Widget _buildModeTab(String label, String mode, IconData icon) {
    final isSelected = _mode == mode;
    return InkWell(
      onTap: () => setState(() {
        _mode = mode;
        if (mode == 'view') {
          _clearForm();
        }
      }),
      child: Container(
        padding: const EdgeInsets.symmetric(vertical: 14),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFF667eea).withOpacity(0.08) : Colors.transparent,
          border: Border(
            bottom: BorderSide(
              color: isSelected ? const Color(0xFF667eea) : Colors.grey[300]!,
              width: isSelected ? 3 : 1,
            ),
          ),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              icon,
              size: 20,
              color: isSelected ? const Color(0xFF667eea) : Colors.grey[600],
            ),
            const SizedBox(width: 8),
            Text(
              label,
              style: TextStyle(
                fontSize: 15,
                fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                color: isSelected ? const Color(0xFF667eea) : Colors.grey[700],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAddEditMode() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          if (_mode == 'edit')
            Container(
              padding: const EdgeInsets.all(14),
              margin: const EdgeInsets.only(bottom: 20),
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  colors: [Colors.blue[50]!, Colors.blue[100]!.withOpacity(0.3)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: Colors.blue[300]!, width: 1.5),
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: Colors.blue[600],
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(Icons.edit, color: Colors.white, size: 20),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Text(
                      'Editing Policy Action',
                      style: TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                        color: Color(0xFF1e40af),
                      ),
                    ),
                  ),
                  TextButton.icon(
                    onPressed: () {
                      _clearForm();
                      setState(() => _mode = 'add');
                    },
                    icon: const Icon(Icons.close, size: 18),
                    label: const Text('Cancel'),
                    style: TextButton.styleFrom(
                      foregroundColor: Colors.blue[700],
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    ),
                  ),
                ],
              ),
            ),
          
          // Compact form layout
          Row(
            children: [
              Expanded(
                flex: 2,
                child: Autocomplete<String>(
                  optionsBuilder: (TextEditingValue textEditingValue) {
                    if (textEditingValue.text.isEmpty) {
                      return _addModeEnvNameOptions;
                    }
                    return _addModeEnvNameOptions.where((String option) {
                      return option.toLowerCase().contains(textEditingValue.text.toLowerCase());
                    });
                  },
                  onSelected: (String selection) {
                    _envNameController.text = selection;
                    // Reset all dependent fields
                    setState(() {
                      _addModeSelectedInterfaceNum = null;
                      _interfaceNumController.clear();
                      _policyCat1Controller.clear();
                      _policyCat2Controller.clear();
                      _addModePolicyCat1Options = [];
                      _addModePolicyCat2Options = [];
                    });
                    // If interface is already selected, reload Category 1
                    if (_addModeSelectedInterfaceNum != null) {
                      _loadAddModePolicyCat1Options();
                    }
                  },
                  fieldViewBuilder: (context, controller, focusNode, onFieldSubmitted) {
                    // Sync with our main controller
                    if (_envNameController.text.isNotEmpty && controller.text.isEmpty) {
                      controller.text = _envNameController.text;
                    }
                    controller.addListener(() {
                      final newValue = controller.text;
                      if (_envNameController.text != newValue) {
                        _envNameController.text = newValue;
                        // Reset dependent fields when user types
                        if (newValue.isEmpty) {
                          setState(() {
                            _addModeSelectedInterfaceNum = null;
                            _interfaceNumController.clear();
                            _policyCat1Controller.clear();
                            _policyCat2Controller.clear();
                            _addModePolicyCat1Options = [];
                            _addModePolicyCat2Options = [];
                          });
                        }
                      }
                    });
                    
                    return TextField(
                      controller: controller,
                      focusNode: focusNode,
                      decoration: _buildInputDecoration(
                        label: 'Environment Name *',
                        hint: 'Select or type new',
                        icon: Icons.folder_outlined,
                      ),
                      onSubmitted: (value) => onFieldSubmitted(),
                    );
                  },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                flex: 1,
                child: DropdownButtonFormField<int>(
                  value: _addModeSelectedInterfaceNum,
                  decoration: _buildInputDecoration(
                    label: 'Interface Number *',
                    hint: 'Select',
                    icon: Icons.tag,
                  ),
                  items: [1, 2, 3, 4, 5].map((num) {
                    return DropdownMenuItem(value: num, child: Text(num.toString()));
                  }).toList(),
                  onChanged: (value) {
                    setState(() {
                      _addModeSelectedInterfaceNum = value;
                      _interfaceNumController.text = value?.toString() ?? '';
                      // Reset dependent fields
                      _policyCat1Controller.clear();
                      _policyCat2Controller.clear();
                      _addModePolicyCat1Options = [];
                      _addModePolicyCat2Options = [];
                    });
                    // Load Category 1 options based on env and interface
                    if (value != null && _envNameController.text.trim().isNotEmpty) {
                      _loadAddModePolicyCat1Options();
                    }
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          
          Row(
            children: [
              Expanded(
                child: Autocomplete<String>(
                  optionsBuilder: (TextEditingValue textEditingValue) {
                    if (textEditingValue.text.isEmpty) {
                      return _addModePolicyCat1Options;
                    }
                    return _addModePolicyCat1Options.where((String option) {
                      return option.toLowerCase().contains(textEditingValue.text.toLowerCase());
                    });
                  },
                  onSelected: (String selection) {
                    _policyCat1Controller.text = selection;
                    // Clear Category 2 and reload options based on Category 1
                    _policyCat2Controller.clear();
                    _loadAddModePolicyCat2Options(selection);
                  },
                  fieldViewBuilder: (context, controller, focusNode, onFieldSubmitted) {
                    // Sync with our main controller
                    if (_policyCat1Controller.text.isNotEmpty && controller.text.isEmpty) {
                      controller.text = _policyCat1Controller.text;
                    }
                    controller.addListener(() {
                      final newValue = controller.text;
                      if (_policyCat1Controller.text != newValue) {
                        _policyCat1Controller.text = newValue;
                        // When user types (not selecting from dropdown), also update Category 2
                        if (newValue.isNotEmpty) {
                          _policyCat2Controller.clear();
                          _loadAddModePolicyCat2Options(newValue);
                        } else {
                          setState(() {
                            _addModePolicyCat2Options = [];
                          });
                        }
                      }
                    });
                    
                    return TextField(
                      controller: controller,
                      focusNode: focusNode,
                      decoration: _buildInputDecoration(
                        label: 'Policy Category 1 *',
                        hint: _isLoadingAddCat1 ? 'Loading...' : 'Select or type new',
                        icon: Icons.category_outlined,
                      ).copyWith(
                        suffixIcon: _isLoadingAddCat1
                            ? const Padding(
                                padding: EdgeInsets.all(12.0),
                                child: SizedBox(
                                  width: 16,
                                  height: 16,
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                ),
                              )
                            : null,
                      ),
                      enabled: !_isLoadingAddCat1,
                      onSubmitted: (value) => onFieldSubmitted(),
                    );
                  },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Autocomplete<String>(
                  optionsBuilder: (TextEditingValue textEditingValue) {
                    if (textEditingValue.text.isEmpty) {
                      return _addModePolicyCat2Options;
                    }
                    return _addModePolicyCat2Options.where((String option) {
                      return option.toLowerCase().contains(textEditingValue.text.toLowerCase());
                    });
                  },
                  onSelected: (String selection) {
                    _policyCat2Controller.text = selection;
                  },
                  fieldViewBuilder: (context, controller, focusNode, onFieldSubmitted) {
                    // Sync with our main controller
                    if (_policyCat2Controller.text.isNotEmpty && controller.text.isEmpty) {
                      controller.text = _policyCat2Controller.text;
                    }
                    controller.addListener(() {
                      _policyCat2Controller.text = controller.text;
                    });
                    
                    return TextField(
                      controller: controller,
                      focusNode: focusNode,
                      decoration: _buildInputDecoration(
                        label: 'Policy Category 2 *',
                        hint: _isLoadingAddCat2 ? 'Loading...' : 'Select or type new',
                        icon: Icons.label_outline,
                      ).copyWith(
                        suffixIcon: _isLoadingAddCat2
                            ? const Padding(
                                padding: EdgeInsets.all(12.0),
                                child: SizedBox(
                                  width: 16,
                                  height: 16,
                                  child: CircularProgressIndicator(strokeWidth: 2),
                                ),
                              )
                            : null,
                      ),
                      enabled: !_isLoadingAddCat2,
                      onSubmitted: (value) => onFieldSubmitted(),
                    );
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          
          // Policy Description
          TextField(
            controller: _policyDescriptionController,
            maxLines: 2,
            decoration: _buildInputDecoration(
              label: 'Policy Description (Optional)',
              hint: 'Brief description of the policy actions',
              icon: Icons.description_outlined,
            ),
            autocorrect: true,
            enableSuggestions: true,
          ),
          const SizedBox(height: 20),
          
          // Actions JSON Editor Section
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: Colors.grey[300]!),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(Icons.code, size: 20, color: const Color(0xFF667eea)),
                    const SizedBox(width: 8),
                    const Text(
                      'Actions JSON *',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w600,
                        color: Color(0xFF1f2937),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                SizedBox(
                  height: 400,
                  child: JsonEditorViewer(
                    title: 'Actions JSON',
                    showToolbar: true,
                    controller: _actionsJsonController,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          
          // Save Button with modern design
          Container(
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF667eea), Color(0xFF764ba2)],
                begin: Alignment.centerLeft,
                end: Alignment.centerRight,
              ),
              borderRadius: BorderRadius.circular(10),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF667eea).withOpacity(0.3),
                  blurRadius: 8,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: ElevatedButton.icon(
              onPressed: _isLoading ? null : _savePolicyAction,
              icon: Icon(
                _mode == 'edit' ? Icons.save_rounded : Icons.add_circle_rounded,
                size: 22,
              ),
              label: Text(
                _mode == 'edit' ? 'Update Policy Action' : 'Add Policy Action',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                backgroundColor: Colors.transparent,
                foregroundColor: Colors.white,
                shadowColor: Colors.transparent,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
                minimumSize: const Size(double.infinity, 50),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildViewMode() {
    final filterSection = Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.grey[50]!, Colors.white],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
        border: Border(bottom: BorderSide(color: Colors.grey[200]!)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF667eea).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.filter_list_rounded, color: Color(0xFF667eea), size: 22),
              ),
              const SizedBox(width: 12),
              const Text(
                'Filter Policy Actions',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1f2937),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: SearchableDropdown<String>(
                  value: _selectedEnvName,
                  items: _envNameOptions,
                  itemAsString: (item) => item,
                  decoration: InputDecoration(
                    labelText: 'Environment Name',
                    hintText: 'Select environment',
                    prefixIcon: Icon(Icons.folder_outlined, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() {
                      _selectedEnvName = value;
                      _selectedInterfaceNum = null;
                      _selectedPolicyCat1 = null;
                      _selectedPolicyCat2 = null;
                      _interfaceNumOptions = [];
                      _policyCat1Options = [];
                      _policyCat2Options = [];
                      _policyActions = [];
                    });
                    if (value != null) {
                      _loadDistinctInterfaceNums();
                    }
                  },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: SearchableDropdown<int>(
                  value: _selectedInterfaceNum,
                  items: _interfaceNumOptions,
                  itemAsString: (item) => item.toString(),
                  decoration: InputDecoration(
                    labelText: 'Interface Number',
                    hintText: 'Select interface number',
                    prefixIcon: Icon(Icons.tag, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() {
                      _selectedInterfaceNum = value;
                      _selectedPolicyCat1 = null;
                      _selectedPolicyCat2 = null;
                      _policyCat1Options = [];
                      _policyCat2Options = [];
                      _policyActions = [];
                    });
                    if (value != null) {
                      _loadDistinctPolicyCat1();
                      _loadPolicyActions();
                    }
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: SearchableDropdown<String>(
                  value: _selectedPolicyCat1,
                  items: _policyCat1Options,
                  itemAsString: (item) => item,
                  decoration: InputDecoration(
                    labelText: 'Policy Category 1 (Optional)',
                    hintText: 'All',
                    prefixIcon: Icon(Icons.category_outlined, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() {
                      _selectedPolicyCat1 = value;
                      _selectedPolicyCat2 = null;
                      _policyCat2Options = [];
                    });
                    if (value != null) {
                      _loadDistinctPolicyCat2();
                    }
                    _loadPolicyActions();
                  },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: SearchableDropdown<String>(
                  value: _selectedPolicyCat2,
                  items: _policyCat2Options,
                  itemAsString: (item) => item,
                  decoration: InputDecoration(
                    labelText: 'Policy Category 2 (Optional)',
                    hintText: 'All',
                    prefixIcon: Icon(Icons.label_outline, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() => _selectedPolicyCat2 = value);
                    _loadPolicyActions();
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    );

    final listSection = _policyActions.isEmpty
        ? Container(
            padding: const EdgeInsets.all(48),
            alignment: Alignment.center,
            child: const Text(
              'No policy actions found.\nSelect filters or add new actions.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey),
            ),
          )
        : ListView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            itemCount: _policyActions.length,
            itemBuilder: (context, index) {
              final policyAction = _policyActions[index];
              return _buildPolicyActionCard(policyAction);
            },
          );

    // For standalone mode, use Column with Expanded
    if (widget.standalone) {
      return Column(
        children: [
          filterSection,
          Expanded(child: SingleChildScrollView(child: listSection)),
        ],
      );
    }

    // For embedded mode, use simple Column
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        filterSection,
        listSection,
      ],
    );
  }

  Widget _buildBuildMode() {
    // For standalone mode, use Column with Expanded
    if (widget.standalone) {
      return Column(
        children: [
          _buildBuildFilters(),
          Expanded(
            child: Row(
              children: [
                // Left side: Available actions and selected actions list
                Expanded(
                  flex: 2,
                  child: Column(
                    children: [
                      Expanded(child: _buildAvailableActionsList()),
                      const Divider(height: 1),
                      Expanded(child: _buildSelectedActionsList()),
                    ],
                  ),
                ),
                const VerticalDivider(width: 1),
                // Right side: Merged JSON preview
                Expanded(
                  flex: 3,
                  child: _buildMergedJsonPreview(),
                ),
              ],
            ),
          ),
        ],
      );
    }

    // For embedded mode - use constrained heights
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        _buildBuildFilters(),
        SizedBox(
          height: 300,
          child: _buildAvailableActionsList(),
        ),
        const Divider(),
        SizedBox(
          height: 300,
          child: _buildSelectedActionsList(),
        ),
        const Divider(),
        SizedBox(
          height: 400,
          child: _buildMergedJsonPreview(),
        ),
      ],
    );
  }

  Widget _buildBuildFilters() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [Colors.grey[50]!, Colors.white],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
        border: Border(bottom: BorderSide(color: Colors.grey[200]!)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF667eea).withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.construction_rounded, color: Color(0xFF667eea), size: 22),
              ),
              const SizedBox(width: 12),
              const Text(
                'Build Combined Actions',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1f2937),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: SearchableDropdown<String>(
                  value: _buildEnvName,
                  items: _buildEnvNameOptions,
                  itemAsString: (item) => item,
                  decoration: InputDecoration(
                    labelText: 'Environment Name',
                    hintText: 'Select environment',
                    prefixIcon: Icon(Icons.folder_outlined, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() {
                      _buildEnvName = value;
                      _buildInterfaceNum = null;
                      _buildPolicyCat1 = null;
                      _buildPolicyCat2 = null;
                      _buildInterfaceNumOptions = [];
                      _buildPolicyCat1Options = [];
                      _buildPolicyCat2Options = [];
                      _availablePolicyActions = [];
                      _selectedPolicyActionsForBuild = [];
                      _mergedActionsJson = '[]';
                    });
                    if (value != null) {
                      _loadBuildInterfaceNums();
                    }
                  },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: SearchableDropdown<int>(
                  value: _buildInterfaceNum,
                  items: _buildInterfaceNumOptions,
                  itemAsString: (item) => item.toString(),
                  decoration: InputDecoration(
                    labelText: 'Interface Number',
                    hintText: 'Select interface number',
                    prefixIcon: Icon(Icons.tag, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() {
                      _buildInterfaceNum = value;
                      _buildPolicyCat1 = null;
                      _buildPolicyCat2 = null;
                      _buildPolicyCat1Options = [];
                      _buildPolicyCat2Options = [];
                      _availablePolicyActions = [];
                      _selectedPolicyActionsForBuild = [];
                      _mergedActionsJson = '[]';
                    });
                    if (value != null) {
                      _loadBuildPolicyCat1();
                      _loadAvailablePolicyActions();
                    }
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: SearchableDropdown<String>(
                  value: _buildPolicyCat1,
                  items: _buildPolicyCat1Options,
                  itemAsString: (item) => item,
                  decoration: InputDecoration(
                    labelText: 'Policy Category 1 (Optional)',
                    hintText: 'All',
                    prefixIcon: Icon(Icons.category_outlined, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() {
                      _buildPolicyCat1 = value;
                      _buildPolicyCat2 = null;
                      _buildPolicyCat2Options = [];
                    });
                    if (value != null) {
                      _loadBuildPolicyCat2();
                    }
                    _loadAvailablePolicyActions();
                  },
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: SearchableDropdown<String>(
                  value: _buildPolicyCat2,
                  items: _buildPolicyCat2Options,
                  itemAsString: (item) => item,
                  decoration: InputDecoration(
                    labelText: 'Policy Category 2 (Optional)',
                    hintText: 'All',
                    prefixIcon: Icon(Icons.label_outline, size: 20, color: const Color(0xFF667eea).withOpacity(0.7)),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide(color: Colors.grey[300]!),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: const BorderSide(color: Color(0xFF667eea), width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    isDense: true,
                  ),
                  onChanged: (value) {
                    setState(() => _buildPolicyCat2 = value);
                    _loadAvailablePolicyActions();
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildAvailableActionsList() {
    return Container(
      color: Colors.grey[50],
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white,
              border: Border(bottom: BorderSide(color: Colors.grey[300]!)),
            ),
            child: Row(
              children: [
                const Icon(Icons.list_alt, size: 18, color: Color(0xFF667eea)),
                const SizedBox(width: 8),
                const Text(
                  'Available Policy Actions',
                  style: TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                ),
                const Spacer(),
                Text(
                  '${_availablePolicyActions.length} items',
                  style: TextStyle(color: Colors.grey[600], fontSize: 13),
                ),
              ],
            ),
          ),
          Expanded(
            child: _availablePolicyActions.isEmpty
                ? Center(
                    child: Text(
                      'No policy actions available.\nSelect environment and interface.',
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Colors.grey[500]),
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.all(12),
                    itemCount: _availablePolicyActions.length,
                    itemBuilder: (context, index) {
                      final action = _availablePolicyActions[index];
                      return _buildAvailableActionCard(action);
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildAvailableActionCard(Map<String, dynamic> action) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        dense: true,
        leading: Container(
          padding: const EdgeInsets.all(6),
          decoration: BoxDecoration(
            color: const Color(0xFF667eea).withOpacity(0.1),
            borderRadius: BorderRadius.circular(6),
          ),
          child: const Icon(Icons.policy, color: Color(0xFF667eea), size: 18),
        ),
        title: Text(
          '${action['policyCat1']} / ${action['policyCat2']}',
          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
        ),
        subtitle: action['policyDescription'] != null && action['policyDescription'].toString().isNotEmpty
            ? Text(
                action['policyDescription'],
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(fontSize: 12, color: Colors.grey[600]),
              )
            : null,
        trailing: IconButton(
          icon: const Icon(Icons.add_circle_outline, color: Color(0xFF667eea)),
          onPressed: () => _addPolicyActionToBuild(action),
          tooltip: 'Add to build',
        ),
      ),
    );
  }

  Widget _buildSelectedActionsList() {
    return Container(
      color: Colors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: [const Color(0xFF667eea).withOpacity(0.1), Colors.white],
              ),
              border: Border(bottom: BorderSide(color: Colors.grey[300]!)),
            ),
            child: Row(
              children: [
                const Icon(Icons.playlist_add_check, size: 18, color: Color(0xFF667eea)),
                const SizedBox(width: 8),
                const Text(
                  'Selected Actions (in order)',
                  style: TextStyle(fontWeight: FontWeight.w600, fontSize: 15),
                ),
                const Spacer(),
                Text(
                  '${_selectedPolicyActionsForBuild.length} items',
                  style: TextStyle(color: Colors.grey[600], fontSize: 13),
                ),
              ],
            ),
          ),
          Expanded(
            child: _selectedPolicyActionsForBuild.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.add_box_outlined, size: 48, color: Colors.grey[400]),
                        const SizedBox(height: 12),
                        Text(
                          'No actions selected yet.\nAdd actions from above.',
                          textAlign: TextAlign.center,
                          style: TextStyle(color: Colors.grey[500]),
                        ),
                      ],
                    ),
                  )
                : ReorderableListView.builder(
                    buildDefaultDragHandles: false,
                    padding: const EdgeInsets.all(12),
                    itemCount: _selectedPolicyActionsForBuild.length,
                    onReorder: (oldIndex, newIndex) {
                      setState(() {
                        if (newIndex > oldIndex) newIndex--;
                        final item = _selectedPolicyActionsForBuild.removeAt(oldIndex);
                        _selectedPolicyActionsForBuild.insert(newIndex, item);
                        _updateMergedActionsJson();
                      });
                    },
                    itemBuilder: (context, index) {
                      final action = _selectedPolicyActionsForBuild[index];
                      return _buildSelectedActionCard(action, index, key: ValueKey('selected_$index'));
                    },
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildSelectedActionCard(Map<String, dynamic> action, int index, {required Key key}) {
    return ReorderableDragStartListener(
      key: key,
      index: index,
      child: Card(
        margin: const EdgeInsets.only(bottom: 8),
        elevation: 2,
        child: ListTile(
          dense: true,
          leading: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  gradient: const LinearGradient(
                    colors: [Color(0xFF667eea), Color(0xFF764ba2)],
                  ),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Text(
                  '${index + 1}',
                  style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 12),
                ),
              ),
              const SizedBox(width: 8),
              const Icon(Icons.drag_indicator, color: Colors.grey),
            ],
          ),
          title: Text(
            '${action['policyCat1']} / ${action['policyCat2']}',
            style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
          ),
          trailing: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              IconButton(
                icon: Icon(Icons.arrow_upward, size: 18, color: Colors.grey[700]),
                onPressed: index > 0 ? () => _movePolicyActionUp(index) : null,
                tooltip: 'Move up',
              ),
              IconButton(
                icon: Icon(Icons.arrow_downward, size: 18, color: Colors.grey[700]),
                onPressed: index < _selectedPolicyActionsForBuild.length - 1
                    ? () => _movePolicyActionDown(index)
                    : null,
                tooltip: 'Move down',
              ),
              IconButton(
                icon: const Icon(Icons.delete_outline, color: Colors.red, size: 18),
                onPressed: () => _removePolicyActionFromBuild(index),
                tooltip: 'Remove',
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildMergedJsonPreview() {
    return Container(
      color: Colors.white,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF10b981), Color(0xFF059669)],
              ),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF10b981).withOpacity(0.2),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            child: Row(
              children: [
                const Icon(Icons.merge_type_rounded, size: 20, color: Colors.white),
                const SizedBox(width: 8),
                const Text(
                  'Merged Actions JSON',
                  style: TextStyle(
                    fontWeight: FontWeight.w600,
                    fontSize: 15,
                    color: Colors.white,
                  ),
                ),
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    '${_selectedPolicyActionsForBuild.length} merged',
                    style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: JsonEditorViewer(
                title: 'Merged Actions',
                showToolbar: true,
                controller: _mergedJsonController,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPolicyActionCard(Map<String, dynamic> policyAction) {
    return Card(
      margin: const EdgeInsets.only(bottom: 14),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.grey[200]!, width: 1),
      ),
      child: ExpansionTile(
        tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        childrenPadding: const EdgeInsets.all(16),
        leading: Container(
          padding: const EdgeInsets.all(10),
          decoration: BoxDecoration(
            gradient: const LinearGradient(
              colors: [Color(0xFF667eea), Color(0xFF764ba2)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
            borderRadius: BorderRadius.circular(10),
          ),
          child: const Icon(Icons.policy_rounded, color: Colors.white, size: 22),
        ),
        title: Text(
          '${policyAction['policyCat1']} / ${policyAction['policyCat2']}',
          style: const TextStyle(
            fontWeight: FontWeight.w600,
            fontSize: 15,
            color: Color(0xFF1f2937),
          ),
        ),
        subtitle: Padding(
          padding: const EdgeInsets.only(top: 4),
          child: Row(
            children: [
              Icon(Icons.folder_outlined, size: 14, color: Colors.grey[600]),
              const SizedBox(width: 4),
              Text(
                '${policyAction['envName']}',
                style: TextStyle(color: Colors.grey[600], fontSize: 13),
              ),
              const SizedBox(width: 12),
              Icon(Icons.tag, size: 14, color: Colors.grey[600]),
              const SizedBox(width: 4),
              Text(
                'Interface ${policyAction['interfaceNum']}',
                style: TextStyle(color: Colors.grey[600], fontSize: 13),
              ),
            ],
          ),
        ),
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (policyAction['policyDescription'] != null &&
                  policyAction['policyDescription'].toString().isNotEmpty)
                Container(
                  padding: const EdgeInsets.all(14),
                  margin: const EdgeInsets.only(bottom: 14),
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [Colors.blue[50]!, Colors.blue[100]!.withOpacity(0.3)],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: Colors.blue[200]!, width: 1),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Icon(Icons.info_outline, color: Colors.blue[700], size: 20),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Description',
                              style: TextStyle(
                                fontWeight: FontWeight.w600,
                                color: Colors.blue[900],
                                fontSize: 13,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              policyAction['policyDescription'],
                              style: TextStyle(color: Colors.blue[800], fontSize: 13),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              
              // Actions JSON Viewer
              Container(
                decoration: BoxDecoration(
                  color: Colors.grey[50],
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: Colors.grey[300]!),
                ),
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(Icons.code_rounded, size: 18, color: const Color(0xFF667eea)),
                        const SizedBox(width: 8),
                        const Text(
                          'Actions JSON',
                          style: TextStyle(
                            fontWeight: FontWeight.w600,
                            fontSize: 14,
                            color: Color(0xFF1f2937),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    SizedBox(
                      height: 300,
                      child: JsonEditorViewer(
                        title: 'Actions JSON',
                        showToolbar: true,
                        controller: TextEditingController(
                          text: policyAction['actionsJson'] ?? '{}',
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              
              // Action buttons with modern styling
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  OutlinedButton.icon(
                    onPressed: () => _editPolicyAction(policyAction),
                    icon: const Icon(Icons.edit_rounded, size: 18),
                    label: const Text('Edit'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF667eea),
                      side: const BorderSide(color: Color(0xFF667eea)),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    ),
                  ),
                  const SizedBox(width: 10),
                  OutlinedButton.icon(
                    onPressed: () => _deletePolicyAction(policyAction['policyActionId']),
                    icon: const Icon(Icons.delete_rounded, size: 18),
                    label: const Text('Delete'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: Colors.red[600],
                      side: BorderSide(color: Colors.red[300]!),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }
}
