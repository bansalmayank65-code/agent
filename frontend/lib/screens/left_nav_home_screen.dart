import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'package:file_picker/file_picker.dart';
import '../providers/task_provider.dart';
import '../providers/auth_provider.dart';

import '../widgets/json_editor_viewer.dart';
import '../widgets/schema_display_widget.dart';
import '../widgets/common/inline_import_progress.dart';
import 'package:url_launcher/url_launcher.dart';
import 'dart:convert';

import 'package:flutter/foundation.dart' show kIsWeb; // for platform check
// Platform utilities (web vs others) for picking directories / JSON and clipboard copy
import '../utils/platform_file_picker_stub.dart'
  if (dart.library.html) '../utils/platform_file_picker_web.dart';



/// Left navigation based home screen implementing new layout.
class LeftNavHomeScreen extends StatefulWidget {
  const LeftNavHomeScreen({super.key});

  @override
  State<LeftNavHomeScreen> createState() => _LeftNavHomeScreenState();
}

class _LeftNavHomeScreenState extends State<LeftNavHomeScreen> {
  int _selectedIndex = 0; // also used for Ctrl+S mapping

  final TextEditingController _repoController = TextEditingController();
  final TextEditingController _envController = TextEditingController(text: 'finance');
  final TextEditingController _interfaceController = TextEditingController(text: '4');
  final TextEditingController _instructionController = TextEditingController();
  final TextEditingController _userIdController = TextEditingController();
  final TextEditingController _outputsController = TextEditingController();
  final ScrollController _rightScroll = ScrollController();
  final ImportProgressController _importProgressController = ImportProgressController();
  
  String? _importedJsonPath; // Track imported JSON file path

  @override
  void dispose() {
    _repoController.dispose();
    _envController.dispose();
    _interfaceController.dispose();
    _instructionController.dispose();
    _userIdController.dispose();
    _outputsController.dispose();
    _rightScroll.dispose();
    _importProgressController.dispose();
    super.dispose();
  }

  List<_NavItem> get _items => [
    _NavItem('Import Task JSON', Icons.upload_file, sectionKey: 'repo'),
    _NavItem('Project Parameters', Icons.settings_applications, sectionKey: 'params'),
    _NavItem('Instruction', Icons.description, sectionKey: 'instruction'),
    _NavItem('Actions', Icons.list_alt, sectionKey: 'actions'),
    _NavItem('User ID', Icons.person, sectionKey: 'user'),
    _NavItem('Output Ids', Icons.tag, sectionKey: 'outputs'),
    _NavItem('Edges', Icons.account_tree, sectionKey: 'edges'),
    _NavItem('Number of edges', Icons.analytics, sectionKey: 'num_edges'),
    _NavItem('Task.json', Icons.code),
    _NavItem('Graph', Icons.auto_graph, sectionKey: 'graph'),
    _NavItem('Validate Task.json', Icons.rule_folder),
      ];

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TaskProvider>();
    return Scaffold(
      body: Row(
        children: [
          _buildLeftNav(),
          Expanded(
            child: AnimatedSwitcher(
              duration: const Duration(milliseconds: 250),
              child: _buildRightContent(provider, key: ValueKey(_selectedIndex)),
            ),
          )
        ],
      ),
    );
  }

  Widget _buildLeftNav() {
    final provider = context.watch<TaskProvider>();
    return Container(
      width: 260,
      decoration: const BoxDecoration(
        color: Color(0xFF1f2937),
        boxShadow: [BoxShadow(color: Colors.black26, blurRadius: 6)],
      ),
      child: Column(
        children: [
          const SizedBox(height: 32),
          const Text('Agentic Workstation', style: TextStyle(color: Colors.white,fontSize:16,fontWeight: FontWeight.bold)),
          const SizedBox(height: 24),
          Expanded(
            child: ListView.builder(
              itemCount: _items.length,
              itemBuilder: (context, index) {
                final item = _items[index];
                final selected = index == _selectedIndex;
                final dirty = _isSectionDirty(provider, item.sectionKey);
                return ListTile(
                  leading: Icon(item.icon, color: selected ? Colors.white : Colors.grey[400]),
                  title: Row(children:[
                    Expanded(child: Text(item.label, style: TextStyle(color: selected ? Colors.white : Colors.grey[300], fontSize: 14))),
                    if (dirty) const Text('•', style: TextStyle(color: Colors.orangeAccent, fontSize: 16, fontWeight: FontWeight.bold))
                  ]),
                  selected: selected,
                  selectedTileColor: const Color(0xFF374151),
                  onTap: () async {
                    if (index != _selectedIndex && _hasAnyDirty(provider)) {
                      final proceed = await _confirmDiscardChanges();
                      if (!proceed) return;
                      // Discard changes by reloading from last saved state
                      try {
                        await provider.discardChanges();
                        setState(() {
                          // Refresh text controllers with reverted data
                          _instructionController.text = provider.task.instruction;
                          _userIdController.text = provider.task.userId;
                          _outputsController.text = provider.task.outputs.join(', ');
                        });
                        _toast('Changes discarded successfully');
                      } catch (e) {
                        _toast('Failed to discard changes: $e');
                      }
                    }
                    setState(() => _selectedIndex = index);
                  },
                );
              },
            ),
          ),
          const Padding(
            padding: EdgeInsets.all(12.0),
            child: Text('v1.0.0', style: TextStyle(color: Colors.grey, fontSize: 12)),
          )
        ],
      ),
    );
  }

  Widget _buildRightContent(TaskProvider provider, {Key? key}) {
    return FocusableActionDetector(
      shortcuts: {
        LogicalKeySet(LogicalKeyboardKey.control, LogicalKeyboardKey.keyS): const ActivateIntent(),
      },
      actions: {
        ActivateIntent: CallbackAction<ActivateIntent>(onInvoke: (_) { _saveCurrentSection(provider, advance: false); return null; })
      },
      child: _sectionContent(provider, key: key),
    );
  }

  Widget _sectionContent(TaskProvider provider, {Key? key}) {
    switch (_selectedIndex) {
      case 0:
        return _sectionWrapper(key, 'Select local repository folder', _buildRepositorySection(provider));
      case 1:
        return _sectionWrapper(key, 'Edit project parameters', _buildProjectParams(provider));
      case 2:
        return _sectionWrapper(key, 'Instruction', _buildInstruction(provider));
      case 3:
        return _sectionWrapper(key, 'Actions', _buildActions(provider));
      case 4:
        return _sectionWrapper(key, 'User ID', _buildUserId(provider));
      case 5:
        return _sectionWrapper(key, 'Output Ids', _buildOutputIds(provider));
      case 6:
        return _sectionWrapper(key, 'Edges', _buildEdges(provider));
      case 7:
        return _sectionWrapper(key, 'Number of edges', _buildNumberOfEdges(provider));
      case 8:
        return _sectionWrapper(key, 'Task.json', _buildTaskJson(provider));
      case 9:
        return _sectionWrapper(key, 'Graph', _buildGraph(provider));
      case 10:
        return _sectionWrapper(key, 'Validate Task.json', _buildValidate(provider));
      default:
        return const SizedBox();
    }
  }

  Widget _sectionWrapper(Key? key, String title, Widget child) {
    return Container(
      key: key,
      color: const Color(0xFFF9FAFB),
      child: Scrollbar(
        controller: _rightScroll,
        child: SingleChildScrollView(
          controller: _rightScroll,
          padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            // Inline import progress - shown at top when active
            InlineImportProgress(
              controller: _importProgressController,
              onRetry: _retryImportWeb,
              onDismiss: () => _importProgressController.hide(),
            ),
            Text(title, style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF111827))),
            const SizedBox(height: 16),
            child
          ]),
        ),
      ),
    );
  }

  Widget _buildRepositorySection(TaskProvider provider) {
    if (_repoController.text.isEmpty && provider.task.repositoryPath.isNotEmpty) {
      _repoController.text = provider.task.repositoryPath;
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Directory Selection Card
        Card(
          elevation: 2,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.folder_open, color: Color(0xFF2563EB)),
                    const SizedBox(width: 8),
                    const Text('Repository Directory', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: provider.task.repositoryPath.isEmpty ? Colors.grey[100] : const Color(0xFF2563EB).withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(
                            color: provider.task.repositoryPath.isEmpty ? Colors.grey[300]! : const Color(0xFF2563EB),
                            width: 1.5,
                          ),
                        ),
                        child: Row(
                          children: [
                            Icon(
                              provider.task.repositoryPath.isEmpty ? Icons.folder_outlined : Icons.folder,
                              color: provider.task.repositoryPath.isEmpty ? Colors.grey[600] : const Color(0xFF2563EB),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                provider.task.repositoryPath.isEmpty ? 'No directory selected' : provider.task.repositoryPath,
                                style: TextStyle(
                                  color: provider.task.repositoryPath.isEmpty ? Colors.grey[600] : const Color(0xFF1E3A8A),
                                  fontWeight: provider.task.repositoryPath.isEmpty ? FontWeight.normal : FontWeight.w500,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    OutlinedButton.icon(
                      onPressed: () async { await _pickRepositoryDirectory(provider); },
                      icon: const Icon(Icons.folder_open),
                      label: const Text('Browse'),
                      style: OutlinedButton.styleFrom(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                      ),
                    ),
                  ],
                ),
                if (kIsWeb) ...[
                  const SizedBox(height: 8),
                  _directoryPickerNote(),
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    onPressed: _importTaskJsonWeb,
                    icon: const Icon(Icons.upload_file),
                    label: const Text('Import task.json'),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF059669),
                      side: const BorderSide(color: Color(0xFF059669)),
                    ),
                  ),
                  if (_importedJsonPath != null) ...[
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: const Color(0xFF059669).withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.3)),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.check_circle, color: const Color(0xFF059669), size: 16),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              'Imported: $_importedJsonPath',
                              style: const TextStyle(fontSize: 12),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
                const SizedBox(height: 12),
                Row(
                  children: [
                    ElevatedButton.icon(
                      onPressed: (provider.task.repositoryPath.isEmpty || !provider.dirtyRepo) ? null : () async { 
                        await provider.syncCache(); 
                        _toast('Repository path saved'); 
                      },
                      icon: const Icon(Icons.save),
                      label: const Text('Save Path'),
                    ),
                    const SizedBox(width: 12),
                    TextButton.icon(
                      onPressed: provider.task.repositoryPath.isEmpty ? null : () async { 
                        await provider.clearSavedRepositoryPath(); 
                        _repoController.clear(); 
                        setState(() {
                          _importedJsonPath = null; // Clear imported JSON path too
                        }); 
                        _toast('Cleared saved path'); 
                      },
                      icon: const Icon(Icons.clear),
                      label: const Text('Clear'),
                      style: TextButton.styleFrom(foregroundColor: Colors.red[600]),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _directoryPickerNote() {
    if (kIsWeb) {
      return Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: Colors.orange[50],
          borderRadius: BorderRadius.circular(6),
          border: Border.all(color: Colors.orange[200]!),
        ),
        child: Row(
          children: [
            Icon(Icons.info_outline, color: Colors.orange[700], size: 16),
            const SizedBox(width: 8),
            const Expanded(
              child: Text(
                'Web mode: Directory picking uses browser sandbox. File operations are simulated.',
                style: TextStyle(fontSize: 12, color: Colors.black87),
              ),
            ),
          ],
        ),
      );
    }
    return const SizedBox.shrink();
  }

  Future<void> _pickRepositoryDirectory(TaskProvider provider) async {
    if (kIsWeb) {
      final path = await pickDirectory();
      if (path == null) { _toast('No folder selected'); return; }
      _repoController.text = path;
      provider.updateRepositoryPath(path);
      setState(() {}); // Refresh UI to show the path and enable save button
      _toast('Selected (web): $path');
      return;
    }
    try {
      final path = await FilePicker.platform.getDirectoryPath();
      if (path != null) {
        _repoController.text = path;
        provider.updateRepositoryPath(path);
        setState(() {}); // Refresh UI to show the path and enable save button
        _toast('Selected: $path');
      }
    } catch (e) {
      _toast('Directory pick failed: $e');
    }
  }

  Widget _buildProjectParams(TaskProvider provider) {
    if (_envController.text != provider.task.env) _envController.text = provider.task.env;
    if (_interfaceController.text != provider.task.interfaceNum.toString()) _interfaceController.text = provider.task.interfaceNum.toString();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(children:[
          Expanded(child: TextField(controller: _envController, decoration: const InputDecoration(labelText: 'Environment Name'), onChanged: (v){ provider.updateEnv(v); })),
          const SizedBox(width:16),
          Expanded(child: TextField(controller: _interfaceController, decoration: const InputDecoration(labelText: 'Interface Number'), keyboardType: TextInputType.number, onChanged: (v){ final n = int.tryParse(v); if(n!=null) provider.updateInterfaceNum(n); })),
        ]),
        const SizedBox(height: 12),
        ElevatedButton.icon(
          onPressed: provider.dirtyParams ? () async { await provider.syncCache(); _toast('Parameters saved'); } : null,
          icon: const Icon(Icons.save),
          label: const Text('Save Parameters')
        )
      ],
    );
  }

  Widget _buildInstruction(TaskProvider provider) {
    if (_instructionController.text.isEmpty && provider.task.instruction.isNotEmpty) _instructionController.text = provider.task.instruction;
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
      TextField(controller: _instructionController, maxLines: 5, decoration: const InputDecoration(labelText: 'Instruction', alignLabelWithHint: true, prefixIcon: Icon(Icons.edit)), onChanged: provider.updateInstruction),
      const SizedBox(height: 12),
      Wrap(spacing:12, children:[
        ElevatedButton.icon(
          onPressed: provider.dirtyInstruction ? () async { await provider.syncCache(); _toast('Instruction cached'); } : null,
          icon: const Icon(Icons.save),
          label: const Text('Save'),
        ),
        OutlinedButton.icon(
          onPressed: provider.task.instruction.isEmpty ? null : () async {
            final uri = Uri.parse('https://turing-amazon-toolings.vercel.app/instruction_validation');
            if (!await launchUrl(uri, mode: LaunchMode.externalApplication)) {
              _toast('Could not open validation tool');
            }
          },
          icon: const Icon(Icons.open_in_new),
          label: const Text('Validate (new tab)'),
        ),
      ])
    ]);
  }







  Widget _buildActions(TaskProvider provider) {
    // Get current actions as JSON
    final actionsJson = provider.task.actionObjects != null && provider.task.actionObjects!.isNotEmpty
        ? const JsonEncoder.withIndent('  ').convert(provider.task.actionObjects)
        : const JsonEncoder.withIndent('  ').convert(provider.task.actions.map((name) => {'name': name}).toList());

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.list_alt, color: Color(0xFF2563EB)),
            const SizedBox(width: 8),
            const Text(
              'Actions JSON Editor',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: const Color(0xFF2563EB).withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: const Color(0xFF2563EB).withValues(alpha: 0.3)),
          ),
          child: Row(
            children: [
              Icon(Icons.info_outline, color: const Color(0xFF2563EB), size: 16),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Edit actions as JSON with full argument definitions. Changes auto-sync to task data.',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        
        // Schema reference
        const SchemaDisplayWidget(
          schemaType: 'actions',
          title: 'Actions Schema Reference',
          color: Color(0xFF2563EB),
        ),
        const SizedBox(height: 16),
        
        Container(
          height: 500,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(8),
          ),
          child: JsonEditorViewer(
            initialJson: actionsJson,
            onJsonChanged: (jsonString) {
              try {
                final parsed = jsonDecode(jsonString);
                if (parsed is List) {
                  final actionObjects = parsed.map((item) {
                    if (item is Map<String, dynamic>) {
                      return item;
                    } else if (item is String) {
                      return {'name': item};
                    }
                    return {'name': item.toString()};
                  }).toList();
                  
                  provider.updateActionObjects(actionObjects);
                  _toast('Actions updated');
                }
              } catch (e) {
                // Ignore parse errors during editing
              }
            },
            title: 'Actions Editor',
            showToolbar: true,
            splitView: MediaQuery.of(context).size.width > 800,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            ElevatedButton.icon(
              onPressed: provider.dirtyActions ? () async { 
                await provider.syncCache(); 
                _toast('Actions saved'); 
              } : null,
              icon: const Icon(Icons.save),
              label: const Text('Save Actions'),
            ),
          ],
        ),
      ],
    );
  }



  // Editor for full action objects (arguments & output) when detailed editing is needed










  Widget _buildUserId(TaskProvider provider) {
    if (_userIdController.text.isEmpty && provider.task.userId.isNotEmpty) _userIdController.text = provider.task.userId;
    return Column(children:[
      TextField(controller: _userIdController, decoration: const InputDecoration(labelText: 'User ID'), onChanged: provider.updateUserId),
      const SizedBox(height: 12),
      ElevatedButton.icon(onPressed: provider.dirtyUserId ? () async { await provider.syncCache(); _toast('User ID saved'); } : null, icon: const Icon(Icons.save), label: const Text('Save')),
    ]);
  }

  Widget _buildOutputIds(TaskProvider provider) {
    if (_outputsController.text.isEmpty && provider.task.outputs.isNotEmpty) _outputsController.text = provider.task.outputs.join(', ');
    
    // Count current output ids
    final currentIds = provider.task.outputs.where((id) => id.isNotEmpty).toList();
    final idCount = currentIds.length;
    final isValidCount = idCount >= 3 && idCount <= 5;
    
    return Column(children:[
      TextField(
        controller: _outputsController,
        decoration: InputDecoration(
          labelText: 'Output Ids (comma separated)',
          helperText: 'Important IDs from action outputs. Min 3, Max 5. Example: user_id, transaction_id, confirmation_code',
          errorText: idCount > 0 && !isValidCount ? 'Must have 3-5 output IDs' : null,
        ),
        maxLines: 3,
        onChanged: (v){ provider.updateOutputs(provider.parseOutputs(v)); }
      ),
      const SizedBox(height:8),
      // Count indicator
      Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: isValidCount ? Colors.green[100] : (idCount > 0 ? Colors.orange[100] : Colors.grey[100]),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isValidCount ? Colors.green[300]! : (idCount > 0 ? Colors.orange[300]! : Colors.grey[300]!),
          ),
        ),
        child: Text(
          '$idCount / 3-5 IDs',
          style: TextStyle(
            fontSize: 12,
            color: isValidCount ? Colors.green[800] : (idCount > 0 ? Colors.orange[800] : Colors.grey[600]),
          ),
        ),
      ),
      const SizedBox(height:12),
      ElevatedButton.icon(
        onPressed: (provider.dirtyOutputs && isValidCount) ? () async { 
          await provider.syncCache(); 
          _toast('Output IDs saved'); 
        } : null, 
        icon: const Icon(Icons.save), 
        label: const Text('Save')
      ),
      if (provider.task.outputs.isNotEmpty) Padding(
        padding: const EdgeInsets.only(top:12), 
        child: _pillList(provider.task.outputs)
      )
    ]);
  }

  Widget _buildEdges(TaskProvider provider) {
    // Get current edges as JSON
    final edgesJson = provider.task.edges.isNotEmpty
        ? const JsonEncoder.withIndent('  ').convert(provider.task.edges)
        : const JsonEncoder.withIndent('  ').convert([]);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.account_tree, color: Color(0xFF059669)),
            const SizedBox(width: 8),
            const Text(
              'Edges JSON Editor',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 12),
        
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: const Color(0xFF059669).withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.3)),
          ),
          child: Row(
            children: [
              Icon(Icons.info_outline, color: const Color(0xFF059669), size: 16),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Define workflow edges with schema validation. Each edge needs from, to, and connection properties.',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        
        // Schema reference
        const SchemaDisplayWidget(
          schemaType: 'edges',
          title: 'Edges Schema Reference',
          color: Color(0xFF059669),
        ),
        const SizedBox(height: 16),
        
        Container(
          height: 500,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(8),
          ),
          child: JsonEditorViewer(
            initialJson: edgesJson,
            onJsonChanged: (jsonString) {
              try {
                final parsed = jsonDecode(jsonString);
                if (parsed is List) {
                  final edges = parsed.map((item) {
                    if (item is Map<String, dynamic>) {
                      // Convert Map<String, dynamic> to Map<String, String>
                      return item.map((key, value) => MapEntry(key, value.toString()));
                    }
                    return <String, String>{};
                  }).toList();
                  
                  provider.updateEdges(edges);
                  _toast('Edges updated');
                }
              } catch (e) {
                // Ignore parse errors during editing
              }
            },
            title: 'Edges Editor',
            showToolbar: true,
            splitView: MediaQuery.of(context).size.width > 800,
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            ElevatedButton.icon(
              onPressed: provider.dirtyEdges ? () async { 
                await provider.syncCache(); 
                _toast('Edges saved'); 
              } : null,
              icon: const Icon(Icons.save),
              label: const Text('Save Edges'),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildNumberOfEdges(TaskProvider provider) {
    // Calculate edge count from the current edges
    final edgeCount = provider.task.edges.length;
    
    // Determine complexity based on the rules
    String complexityLevel;
    Color complexityColor;
    String complexityDescription;
    
    if (edgeCount < 7) {
      complexityLevel = 'INVALID';
      complexityColor = Colors.red;
      complexityDescription = 'Task is not valid - minimum 7 edges required';
    } else if (edgeCount >= 7 && edgeCount <= 12) {
      complexityLevel = 'MEDIUM';
      complexityColor = Colors.orange;
      complexityDescription = 'Medium complexity (7-12 edges): 20% of tasks';
    } else if (edgeCount >= 13 && edgeCount <= 15) {
      complexityLevel = 'HARD';
      complexityColor = Colors.blue;
      complexityDescription = 'Hard complexity (13-15 edges): 50% of tasks';
    } else {
      complexityLevel = 'EXPERT';
      complexityColor = Colors.purple;
      complexityDescription = 'Expert complexity (16+ edges): 30% of tasks';
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.analytics, color: Color(0xFF059669)),
            const SizedBox(width: 8),
            const Text(
              'Number of Edges Analysis',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 16),
        
        // Edge count display
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFF059669).withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.3)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.account_tree, color: const Color(0xFF059669), size: 20),
                  const SizedBox(width: 8),
                  const Text(
                    'Current Edge Count',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                '$edgeCount edges',
                style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 4),
              Text(
                'Calculated from current edges JSON',
                style: TextStyle(fontSize: 12, color: Colors.grey[600]),
              ),
            ],
          ),
        ),
        
        const SizedBox(height: 16),
        
        // Complexity analysis
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: complexityColor.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: complexityColor.withValues(alpha: 0.3)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.assessment, color: complexityColor, size: 20),
                  const SizedBox(width: 8),
                  Text(
                    'Task Complexity: $complexityLevel',
                    style: TextStyle(
                      fontWeight: FontWeight.bold, 
                      fontSize: 14,
                      color: complexityColor,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Text(
                complexityDescription,
                style: const TextStyle(fontSize: 12),
              ),
            ],
          ),
        ),
        
        const SizedBox(height: 16),
        
        // Complexity distribution reference
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.grey[50],
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.grey[300]!),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.grey[600], size: 20),
                  const SizedBox(width: 8),
                  Text(
                    'Complexity Distribution Guidelines',
                    style: TextStyle(
                      fontWeight: FontWeight.bold, 
                      fontSize: 14,
                      color: Colors.grey[700],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              _complexityRow('Medium (7-12 edges)', '20%', Colors.orange),
              const SizedBox(height: 6),
              _complexityRow('Hard (13-15 edges)', '50%', Colors.blue),
              const SizedBox(height: 6),
              _complexityRow('Expert (16+ edges)', '30%', Colors.purple),
              const SizedBox(height: 8),
              Text(
                'Tasks with fewer than 7 edges are considered invalid.',
                style: TextStyle(
                  fontSize: 11, 
                  color: Colors.red[600],
                  fontStyle: FontStyle.italic,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _complexityRow(String label, String percentage, Color color) {
    return Row(
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.2),
            border: Border.all(color: color),
            borderRadius: BorderRadius.circular(2),
          ),
        ),
        const SizedBox(width: 8),
        Text(
          label,
          style: const TextStyle(fontSize: 12),
        ),
        const Spacer(),
        Text(
          percentage,
          style: TextStyle(
            fontSize: 12, 
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
      ],
    );
  }













  Widget _buildTaskJson(TaskProvider provider) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.code, color: Color(0xFF059669)),
            const SizedBox(width: 8),
            const Text(
              'Task.json Editor',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: const Color(0xFF059669).withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: const Color(0xFF059669).withValues(alpha: 0.3)),
          ),
          child: Row(
            children: [
              Icon(Icons.info_outline, color: const Color(0xFF059669), size: 16),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Complete task configuration with environment, model settings, and task definition. Changes auto-sync.',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        
        // Schema reference
        const SchemaDisplayWidget(
          schemaType: 'task',
          title: 'Task.json Schema Reference',
          color: Color(0xFF059669),
        ),
        const SizedBox(height: 16),
        
        // JSON Editor
        Container(
          height: 500,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(8),
          ),
          child: FutureBuilder<String?>(
            future: _futureAggregated(provider),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (!snapshot.hasData) {
                return const Center(child: Text('No data available'));
              }
              
              return JsonEditorViewer(
                initialJson: snapshot.data!,
                onJsonChanged: (jsonString) {
                  try {
                    final parsed = jsonDecode(jsonString);
                    if (parsed is Map<String, dynamic>) {
                      // Update the provider with the parsed task JSON
                      // Note: This is a read-only view for now since task.json
                      // is automatically generated from other sections
                      _toast('Task.json structure validated');
                    }
                  } catch (e) {
                    // Ignore parse errors during editing
                  }
                },
                title: 'Task.json Editor',
                showToolbar: true,
                splitView: MediaQuery.of(context).size.width > 800,
              );
            },
          ),
        ),
        const SizedBox(height: 16),
        Row(
          children: [
            ElevatedButton.icon(
              onPressed: () async { 
                await provider.syncCache(); 
                final json = await provider.saveTaskJson(); 
                if(json != null) _toast('task.json written'); 
              },
              icon: const Icon(Icons.save),
              label: const Text('Download Task.json'),
            ),
          ],
        ),
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.amber.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.amber.withValues(alpha: 0.3)),
          ),
          child: Row(
            children: [
              Icon(Icons.info_outline, color: Colors.amber[700], size: 16),
              const SizedBox(width: 8),
              const Expanded(
                child: Text(
                  'Task.json is automatically generated from all sections. Use individual sections to make changes.',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Future<String?> _futureAggregated(TaskProvider provider) {
    // Build the complete task.json structure from current provider data
    final taskJson = {
      'env': provider.task.env,
      'model_provider': 'fireworks', // Default value
      'model': 'qwen3-coder-480b-a35b-instruct', // Default value
      'num_trials': 3, // Default value
      'temperature': 1, // Default value
      'interface_num': provider.task.interfaceNum,
      'task': {
        'user_id': provider.task.userId,
        'instruction': provider.task.instruction,
        'actions': provider.task.actionObjects ?? provider.task.actions.map((name) => {'name': name}).toList(),
        'outputs': provider.task.outputs,
        'edges': provider.task.edges,
        'num_edges': provider.task.edges.length,
      }
    };
    
    return Future.value(const JsonEncoder.withIndent('  ').convert(taskJson));
  }

  Widget _buildGraph(TaskProvider provider) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children:[
      const Text('Repository Graph Visualization', style: TextStyle(fontWeight: FontWeight.bold)),
      const SizedBox(height: 12),
      const Text('Shows the latest PNG file from the selected repository for graph visualization.', 
                 style: TextStyle(color: Colors.grey)),
      const SizedBox(height: 16),
      
      if (provider.task.repositoryPath.isEmpty) ...[
        Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: Colors.orange[50],
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.orange[200]!),
          ),
          child: Row(
            children: [
              Icon(Icons.warning, color: Colors.orange[600]),
              const SizedBox(width: 8),
              const Expanded(
                child: Text('Please select a repository first to view graph visualizations.'),
              ),
            ],
          ),
        ),
      ] else ...[
        ElevatedButton.icon(
          onPressed: () => _loadLatestGraph(provider),
          icon: const Icon(Icons.refresh),
          label: const Text('Load Latest Graph'),
        ),
        const SizedBox(height: 16),
        _buildGraphDisplay(provider),
      ],
    ]);
  }

  Widget _buildGraphDisplay(TaskProvider provider) {
    return Container(
      width: double.infinity,
      height: 400,
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey[300]!),
        borderRadius: BorderRadius.circular(8),
      ),
      child: const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.auto_graph, size: 48, color: Colors.grey),
            SizedBox(height: 8),
            Text('Graph visualization will appear here', style: TextStyle(color: Colors.grey)),
            SizedBox(height: 4),
            Text('Click "Load Latest Graph" to display the latest PNG file', 
                 style: TextStyle(fontSize: 12, color: Colors.grey)),
          ],
        ),
      ),
    );
  }

  void _loadLatestGraph(TaskProvider provider) {
    // TODO: Implement loading latest PNG from repository
    _toast('Graph loading functionality to be implemented');
  }

  Widget _buildValidate(TaskProvider provider) {
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children:[
      const Text('Validation Steps:', style: TextStyle(fontWeight: FontWeight.bold)),
      const SizedBox(height: 12),
      _validationButton(provider, 'Compute Complexity', 'compute_complexity'),
      _validationButton(provider, 'Task Verification', 'task_verification'),
      _validationButton(provider, 'Run Task', 'run_task'),
      _validationButton(provider, 'Evaluate', 'evaluate'),
      const SizedBox(height: 24),
      const Text('Results:', style: TextStyle(fontWeight: FontWeight.bold)),
      const SizedBox(height: 8),
      _resultArea(provider),
    ]);
  }

  Map<String,String> _latestResults = {};

  Widget _validationButton(TaskProvider provider, String label, String step){
    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: ElevatedButton.icon(
        onPressed: () async {
          final resp = await provider.runStep(step);
            if(resp!=null){
              setState(()=> _latestResults[step] = (resp['result']?? resp['error']?? '').toString());
              _toast('Step $step finished');
            }
        },
        icon: const Icon(Icons.play_arrow),
        label: Text(label),
      ),
    );
  }

  Widget _resultArea(TaskProvider provider){
    if(_latestResults.isEmpty) return const Text('No results yet');
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: _latestResults.entries.map((e)=>Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8), border: Border.all(color: Colors.grey[300]!)),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children:[
          Text(e.key, style: const TextStyle(fontWeight: FontWeight.bold)),
          const SizedBox(height:4),
          Text(e.value, style: const TextStyle(fontFamily: 'monospace', fontSize: 12)),
        ]),
      )).toList(),
    );
  }

  Widget _pillList(List<String> items){
    return Wrap(spacing: 8, runSpacing: 8, children: items.map((e)=> Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(color: const Color(0xFF2563EB).withValues(alpha: .1), borderRadius: BorderRadius.circular(32)),
      child: Text(e, style: const TextStyle(color: Color(0xFF1E3A8A), fontSize: 12)),
    )).toList());
  }

  void _toast(String msg){
    if(!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  bool _hasAnyDirty(TaskProvider p) => p.dirtyInstruction || p.dirtyActions || p.dirtyUserId || p.dirtyOutputs || p.dirtyEdges || p.dirtyParams || p.dirtyRepo;
  bool _isSectionDirty(TaskProvider p, String? section){
    if(section == null) return false;
    return switch(section){
      'repo' => p.dirtyRepo,
      'params' => p.dirtyParams,
      'instruction' => p.dirtyInstruction,
      'actions' => p.dirtyActions,
      'user' => p.dirtyUserId,
      'outputs' => p.dirtyOutputs,
      'edges' => p.dirtyEdges,
      _ => false
    };
  }

  Future<bool> _confirmDiscardChanges() async {
    return await showDialog<bool>(context: context, builder: (ctx){
      return AlertDialog(
        title: const Text('Unsaved Changes'),
        content: const Text('You have unsaved changes. Navigate away and discard them?'),
        actions: [
          TextButton(onPressed: ()=> Navigator.of(ctx).pop(false), child: const Text('Cancel')),
          ElevatedButton(onPressed: ()=> Navigator.of(ctx).pop(true), child: const Text('Discard')),
        ],
      );
    }) ?? false;
  }

  void _importTaskJsonWeb() async {
    try {
      final result = await pickAndReadJsonFile();
      if (result == null) { 
        _toast('Import cancelled/failed'); 
        return; 
      }
      
      final decoded = result['data'];
      final fileName = result['fileName'] as String?;
      
      if (decoded == null) { 
        _toast('Import failed - invalid JSON'); 
        return; 
      }
      
      final provider = context.read<TaskProvider>();
      final authProvider = context.read<AuthProvider>();
      
      // Show progress
      _importProgressController.show();
      _importProgressController.updateProgress(
        currentStep: 'Reading task.json file...',
        progress: 0.1,
      );
      
      if (!mounted) return;
      
      // Update progress
      await Future.delayed(const Duration(milliseconds: 200));
      _importProgressController.updateProgress(
        currentStep: 'Validating task structure...',
        progress: 0.3,
      );
      
      await Future.delayed(const Duration(milliseconds: 200));
      
      // Update progress
      _importProgressController.updateProgress(
        currentStep: 'Uploading to server...',
        progress: 0.5,
      );
      
      // Import without overriding the user ID from JSON - let users edit it in step 4
      // But pass the logged-in user ID separately for database foreign key constraint
      final importResult = await provider.importTaskJson(decoded, dbUserId: authProvider.userId);
      
      if (importResult['success'] == true) {
        // Update progress
        _importProgressController.updateProgress(
          currentStep: 'Saving to database...',
          progress: 0.8,
        );
        
        await Future.delayed(const Duration(milliseconds: 300));
        
        if (fileName != null) {
          setState(() {
            _importedJsonPath = fileName;
            _instructionController.text = provider.task.instruction;
            _userIdController.text = provider.task.userId;
            _outputsController.text = provider.task.outputs.join(', ');
          });
        }
        
        // Complete
        _importProgressController.updateProgress(
          currentStep: 'Import completed! taskId: ${importResult['taskId']}',
          progress: 1.0,
        );
        _importProgressController.complete();
        
        _toast('Imported task.json from: $fileName');
      } else {
        final errorMsg = importResult['message'] ?? 'Import failed';
        _importProgressController.setError(errorMsg);
        _toast('Import failed: $errorMsg');
      }
    } catch (e) {
      _importProgressController.setError('Import failed: $e');
      _toast('Import failed: $e');
    }
  }

  void _retryImportWeb() {
    _importProgressController.hide();
    _importTaskJsonWeb();
  }

  void _saveCurrentSection(TaskProvider provider, {required bool advance}) async {
    switch(_selectedIndex){
      case 0: if(provider.dirtyRepo) await provider.syncCache(); break;
      case 1: if(provider.dirtyParams) await provider.syncCache(); break;
      case 2: break; // Policy is read-only, no save needed
      case 3: if(provider.dirtyInstruction) await provider.syncCache(); break;
      case 4: if(provider.dirtyActions) await provider.syncCache(); break;
      case 5: if(provider.dirtyUserId) await provider.syncCache(); break;
      case 6: if(provider.dirtyOutputs) await provider.syncCache(); break;
      case 7: if(provider.dirtyEdges) await provider.syncCache(); break;
      case 8: break; // Number of edges is read-only, no save needed
      case 9: await provider.syncCache(); final json = await provider.saveTaskJson(); if(json!=null) _toast('task.json written'); break;
      case 10: break; // Validate section doesn't need saving
    }
    if(advance){
      setState(() { _selectedIndex = (_selectedIndex + 1).clamp(0, _items.length -1); });
    }
  }
}

class _NavItem { final String label; final IconData icon; final String? sectionKey; _NavItem(this.label, this.icon, {this.sectionKey}); }
