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
          title: const Text('Interface Changer'),
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
  String _selectedEnvironment = 'hr_experts';
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
      final mapping = await InterfaceMappingService.loadMappingForEnvironment(_selectedEnvironment);
      final interfaces = InterfaceMappingService.getInterfaces(mapping);
      
      setState(() {
        _currentMapping = mapping;
        _interfaces = interfaces;
        _isLoading = false;
        
        // Reset interface selections
        _sourceNotifier.value = interfaces.isNotEmpty ? interfaces.first : null;
        _targetNotifier.value = interfaces.length > 1 ? interfaces[1] : (interfaces.isNotEmpty ? interfaces.first : null);
      });
    } catch (e) {
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
  
  // Track changes for summary
  final translatedMethods = <String, String>{};
  int totalReplacements = 0;

  // Helper to map a single method name from source interface to target interface
  String mapMethodName(String methodName, String sourceInterface, String targetInterface) {
    // methodName is expected like "manage_user" etc. We'll find it in the values of method_mappings for sourceInterface and replace with corresponding targetInterface entry.
    for (final entry in methodMappings.entries) {
      final mapEntry = entry.value as Map<String, dynamic>;
      final sourceVal = mapEntry[sourceInterface];
      final targetVal = mapEntry[targetInterface];
      if (sourceVal != null && sourceVal == methodName && targetVal != null) {
        // Only count as a translation if the names are actually different
        if (sourceVal != targetVal) {
          translatedMethods[methodName] = targetVal.toString();
          totalReplacements++;
        }
        return targetVal.toString();
      }
    }
    // If not found, return original
    return methodName;
  }

  // Use provided source/target or fall back to defaults
  final src = sourceInterface ?? 'interface_1';
  final tgt = targetInterface ?? 'interface_2';

  // Walk decoded object and replace any string values that match known method names
  void traverseAndReplace(dynamic node) {
    if (node is Map<String, dynamic>) {
      final keys = List<String>.from(node.keys);
      for (final k in keys) {
        final v = node[k];
          if (v is String) {
          node[k] = mapMethodName(v, src, tgt);
        } else {
          traverseAndReplace(v);
        }
      }
    } else if (node is List) {
      for (var i = 0; i < node.length; i++) {
        final v = node[i];
        if (v is String) {
          node[i] = mapMethodName(v, src, tgt);
        } else {
          traverseAndReplace(v);
        }
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
