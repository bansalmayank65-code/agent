import 'package:flutter/material.dart';
import '../widgets/json_editor_viewer.dart';
import '../widgets/dialogs/changes_summary_dialog.dart';
import '../services/interface_mapping_service.dart';
import 'dart:convert';

/// Screen wrapper that provides UI controls and uses the existing JsonEditorViewer
/// to present a before/after side-by-side editor for translating a full task.json
/// between interface naming conventions. The translation is performed in-memory
/// (fail-fast, no DB, no cache) and cleared on refresh.

class TaskInterfaceConverterScreen extends StatelessWidget {
  /// If true, wraps content in Scaffold with AppBar (for standalone navigation).
  /// If false, returns just the body content (for embedding in other layouts).
  final bool standalone;
  
  const TaskInterfaceConverterScreen({Key? key, this.standalone = true}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final bodyContent = _TaskInterfaceConverterBody();
    
    if (standalone) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('Interface Translator'),
          backgroundColor: Colors.white,
          foregroundColor: const Color(0xFF2d3748),
        ),
        body: bodyContent,
      );
    }
    
    return bodyContent;
  }
}

/// The actual converter UI body (without Scaffold/AppBar) - now stateful to handle environment selection
class _TaskInterfaceConverterBody extends StatefulWidget {
  @override
  State<_TaskInterfaceConverterBody> createState() => _TaskInterfaceConverterBodyState();
}

class _TaskInterfaceConverterBodyState extends State<_TaskInterfaceConverterBody> {
  String _selectedEnvironment = 'wiki_confluence'; // Default to wiki_confluence for testing
  Map<String, dynamic>? _currentMapping;
  List<String> _interfaces = [];
  bool _isLoading = true;

  final TextEditingController _beforeController = TextEditingController();
  final TextEditingController _afterController = TextEditingController();
  final ValueNotifier<String?> _sourceNotifier = ValueNotifier<String?>(null);
  final ValueNotifier<String?> _targetNotifier = ValueNotifier<String?>(null);

  @override
  void initState() {
    super.initState();
    _loadMapping();
  }

  @override
  void dispose() {
    _beforeController.dispose();
    _afterController.dispose();
    _sourceNotifier.dispose();
    _targetNotifier.dispose();
    super.dispose();
  }

  Future<void> _loadMapping() async {
    setState(() {
      _isLoading = true;
    });

    try {
      print('DEBUG: Loading mapping for environment: $_selectedEnvironment');
      final mapping = await InterfaceMappingService.loadMappingForEnvironment(_selectedEnvironment);
      final interfaces = InterfaceMappingService.getInterfaces(mapping);
      
      print('DEBUG: Loaded mapping with ${interfaces.length} interfaces: $interfaces');
      print('DEBUG: Environment from mapping: ${InterfaceMappingService.getEnvironmentName(mapping)}');
      
      setState(() {
        _currentMapping = mapping;
        _interfaces = interfaces;
        _isLoading = false;
        
        // Reset interface selections
        _sourceNotifier.value = interfaces.isNotEmpty ? interfaces.first : null;
        _targetNotifier.value = interfaces.length > 1 ? interfaces[1] : (interfaces.isNotEmpty ? interfaces.first : null);
      });
      
      print('DEBUG: Interface selections - Source: ${_sourceNotifier.value}, Target: ${_targetNotifier.value}');
    } catch (e) {
      print('DEBUG: Error loading mapping: $e');
      setState(() {
        _isLoading = false;
      });
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error loading mapping: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Loading interface mappings...'),
          ],
        ),
      );
    }

    if (_currentMapping == null) {
      return const Center(
        child: Text('Failed to load interface mappings'),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // Environment selection and controls row
        Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            children: [
              // Environment selector row
              Row(
                children: [
                  const Text('Environment: '),
                  const SizedBox(width: 8),
                  Expanded(
                    child: DropdownButton<String>(
                      isExpanded: true,
                      value: _selectedEnvironment,
                      items: InterfaceMappingService.getAvailableEnvironments()
                          .map((env) => DropdownMenuItem(
                                value: env,
                                child: Text(InterfaceMappingService.getEnvironmentDisplayName(env)),
                              ))
                          .toList(),
                      onChanged: (newEnvironment) {
                        if (newEnvironment != null && newEnvironment != _selectedEnvironment) {
                          setState(() {
                            _selectedEnvironment = newEnvironment;
                          });
                          _loadMapping();
                        }
                      },
                    ),
                  ),
                  const SizedBox(width: 12),
                  IconButton(
                    icon: const Icon(Icons.refresh),
                    onPressed: _loadMapping,
                    tooltip: 'Reload mapping',
                  ),
                ],
              ),
              const SizedBox(height: 12),
              // Interface selection row
              Row(
                children: [
                  const Text('Source interface: '),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _InterfaceSelector(interfaces: _interfaces, isSource: true, notifier: _sourceNotifier),
                  ),
                  const SizedBox(width: 12),
                  const Text('Target interface: '),
                  const SizedBox(width: 8),
                  Expanded(
                    child: _InterfaceSelector(interfaces: _interfaces, isSource: false, notifier: _targetNotifier),
                  ),
                  const SizedBox(width: 12),
                  ElevatedButton.icon(
                    onPressed: () => _convertAndShow(context, _currentMapping!, _beforeController, _afterController, _sourceNotifier.value, _targetNotifier.value),
                    icon: const Icon(Icons.swap_horiz),
                    label: const Text('Translate'),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        // Fixed height container for editors to avoid layout conflicts
        SizedBox(
          height: 600,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: Row(
              children: [
                Expanded(
                  child: JsonEditorViewer(
                    title: 'Before',
                    showToolbar: true,
                    splitView: true,
                    controller: _beforeController,
                  ),
                ),
                const VerticalDivider(width: 12),
                Expanded(
                  child: JsonEditorViewer(
                    title: 'After',
                    showToolbar: true,
                    splitView: true,
                    controller: _afterController,
                  ),
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 8),
      ],
    );
  }
}

// Small stateful selector widget that stores the selected interface in memory
class _InterfaceSelector extends StatefulWidget {
  final List<String> interfaces;
  final bool isSource;
  final ValueNotifier<String?> notifier;

  const _InterfaceSelector({required this.interfaces, required this.isSource, required this.notifier, Key? key}) : super(key: key);

  @override
  State<_InterfaceSelector> createState() => _InterfaceSelectorState();
}

class _InterfaceSelectorState extends State<_InterfaceSelector> {
  String? _selected;

  @override
  void initState() {
    super.initState();
    _selected = widget.interfaces.isNotEmpty ? widget.interfaces.first : null;
    widget.notifier.value = _selected;
  }

  @override
  Widget build(BuildContext context) {
    return DropdownButton<String>(
      isExpanded: true,
      value: _selected,
      items: widget.interfaces.map((i) => DropdownMenuItem(value: i, child: Text(i))).toList(),
      onChanged: (v) {
        setState(() => _selected = v);
        widget.notifier.value = v;
      },
    );
  }
}

void _convertAndShow(BuildContext context, Map<String, dynamic> mapping, TextEditingController beforeController, TextEditingController afterController, String? sourceInterface, String? targetInterface) {
  // Fail-fast: validate input JSON first
  final beforeText = beforeController.text;
  if (beforeText.trim().isEmpty) {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Paste a task.json into the left editor first')));
    return;
  }

  dynamic decoded;
  try {
    decoded = jsonDecode(beforeText);
  } catch (e) {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Invalid JSON in left editor')));
    return;
  }

  // Perform a traversal and replace method names according to mapping.
  final methodMappings = InterfaceMappingService.getMethodMappings(mapping);
  
  // Debug: Print mapping info
  print('DEBUG: Method mappings count: ${methodMappings.length}');
  print('DEBUG: Source interface: $sourceInterface, Target interface: $targetInterface');
  print('DEBUG: Available interfaces: ${InterfaceMappingService.getInterfaces(mapping)}');
  
  // Track changes for summary
  final translatedMethods = <String, String>{};
  int totalReplacements = 0;

  // Helper to check if a method matches a pattern (e.g., fetch_reference_entities matches discover_reference_entities pattern)
  bool matchesMethodPattern(String methodName, String patternMethod, String baseKey) {
    // Try to extract the operation type from both method and pattern
    final methodParts = methodName.split('_');
    final patternParts = patternMethod.split('_');
    
    if (methodParts.length >= 2 && patternParts.length >= 2) {
      // Check if they have the same suffix pattern (e.g., both end with "reference_entities")
      final methodSuffix = methodParts.skip(1).join('_');
      final patternSuffix = patternParts.skip(1).join('_');
      
      return methodSuffix == patternSuffix;
    }
    
    return false;
  }

  // Helper to convert a method name from source pattern to target pattern
  String convertMethodPattern(String methodName, String sourcePattern, String targetPattern) {
    final methodParts = methodName.split('_');
    final sourceParts = sourcePattern.split('_');
    final targetParts = targetPattern.split('_');
    
    if (methodParts.length >= 2 && sourceParts.length >= 2 && targetParts.length >= 2) {
      // Replace the prefix with the target prefix, keep the rest the same
      final methodSuffix = methodParts.skip(1).join('_');
      final targetPrefix = targetParts.first;
      
      return '${targetPrefix}_$methodSuffix';
    }
    
    return methodName;
  }

  // Helper to map a single method name from source interface to target interface
  String mapMethodName(String methodName, String sourceInterface, String targetInterface) {
    print('DEBUG: Trying to map "$methodName" from $sourceInterface to $targetInterface');
    
    // First try exact match
    for (final entry in methodMappings.entries) {
      final mapEntry = entry.value as Map<String, dynamic>;
      final sourceVal = mapEntry[sourceInterface];
      final targetVal = mapEntry[targetInterface];
      if (sourceVal != null && sourceVal == methodName && targetVal != null) {
        print('DEBUG: Exact match found - $methodName -> ${targetVal.toString()}');
        // Only count as a translation if the names are actually different
        if (sourceVal != targetVal) {
          translatedMethods[methodName] = targetVal.toString();
          totalReplacements++;
        }
        return targetVal.toString();
      }
    }
    
    // If exact match not found, try to match by operation type patterns
    print('DEBUG: No exact match, trying pattern matching for "$methodName"');
    for (final entry in methodMappings.entries) {
      final mapEntry = entry.value as Map<String, dynamic>;
      final sourceVal = mapEntry[sourceInterface]?.toString();
      final targetVal = mapEntry[targetInterface]?.toString();
      
      if (sourceVal != null && targetVal != null) {
        // Check if methodName matches the pattern for the source interface
        if (matchesMethodPattern(methodName, sourceVal, entry.key)) {
          final newMethodName = convertMethodPattern(methodName, sourceVal, targetVal);
          print('DEBUG: Pattern match found - $methodName -> $newMethodName (via pattern $sourceVal -> $targetVal)');
          if (newMethodName != methodName) {
            translatedMethods[methodName] = newMethodName;
            totalReplacements++;
          }
          return newMethodName;
        }
      }
    }
    
    print('DEBUG: No match found for "$methodName"');
    // If not found, return original
    return methodName;
  }

  // Use provided source/target or fall back to defaults
  final src = sourceInterface ?? 'interface_1';
  final tgt = targetInterface ?? 'interface_2';

  // Walk decoded object and replace only actual API method names in "name" fields
  void traverseAndReplace(dynamic node, [String? parentKey]) {
    if (node is Map<String, dynamic>) {
      final keys = List<String>.from(node.keys);
      for (final k in keys) {
        final v = node[k];
        if (v is String) {
          // Only translate method names in "name" fields (actual API method names)
          if (k == 'name' && v.contains('_')) {
            print('DEBUG: Found API method name: "$v" in action');
            final newValue = mapMethodName(v, src, tgt);
            if (newValue != v) {
              print('DEBUG: Replaced method "$v" with "$newValue"');
            }
            node[k] = newValue;
          } else {
            // Keep original for all other string values (like action_type values)
            node[k] = v;
          }
        } else {
          traverseAndReplace(v, k);
        }
      }
    } else if (node is List) {
      for (var i = 0; i < node.length; i++) {
        final v = node[i];
        traverseAndReplace(v, parentKey);
      }
    }
  }

  traverseAndReplace(decoded);

  // Write formatted JSON to afterController
  try {
    const encoder = JsonEncoder.withIndent('  ');
    afterController.text = encoder.convert(decoded);
    
    // Show success snackbar
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Translation complete ($src -> $tgt)'),
        backgroundColor: Colors.green,
        duration: const Duration(seconds: 2),
      ),
    );
    
    // Build changes summary
    final changes = <ChangeItem>[];
    
    if (totalReplacements > 0) {
      final uniqueMethods = translatedMethods.length;
      changes.add(ChangeItem(
        type: ChangeType.translated,
        title: 'Interface Translation Complete',
        description: '$totalReplacements method name(s) translated from $src to $tgt',
        details: [
          '$uniqueMethods unique method(s) converted',
        ],
      ));
      
      // Show top 10 translations as examples
      final translations = translatedMethods.entries.take(10).toList();
      if (translations.isNotEmpty) {
        changes.add(ChangeItem(
          type: ChangeType.info,
          title: 'Sample Translations',
          description: 'Examples of method names changed:',
          details: translations.map((e) => '${e.key} → ${e.value}').toList(),
        ));
      }
      
      if (translatedMethods.length > 10) {
        changes.add(ChangeItem(
          type: ChangeType.info,
          title: 'Additional Changes',
          description: 'And ${translatedMethods.length - 10} more method(s) translated',
        ));
      }
    } else {
      changes.add(ChangeItem(
        type: ChangeType.info,
        title: 'No Changes Made',
        description: 'No method names matching $src interface were found in the task',
      ));
    }
    
    // Show changes summary dialog
    ChangesSummaryDialog.show(
      context,
      title: 'Interface Translation - Summary',
      changes: changes,
    );
  } catch (e) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('Error during translation: $e')));
  }
}
