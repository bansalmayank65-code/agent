import 'dart:convert';
import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_json_viewer/flutter_json_viewer.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'dart:html' as html;
// Platform utilities for clipboard
import '../utils/platform_file_picker_stub.dart'
  if (dart.library.html) '../utils/platform_file_picker_web.dart';

/// A comprehensive JSON Editor + Viewer combo widget that supports:
/// - Loading/pasting JSON
/// - Viewing as collapsible tree
/// - Inline editing of values
/// - Formatting and validation
/// - Copy to clipboard functionality
class JsonEditorViewer extends StatefulWidget {
  final String? initialJson;
  final Function(String)? onJsonChanged;
  final String? title;
  final bool showToolbar;
  final bool splitView;
  final TextEditingController? controller;
  final void Function(TextEditingController)? onControllerReady;

  const JsonEditorViewer({
    super.key,
    this.initialJson,
    this.onJsonChanged,
    this.title,
    this.showToolbar = true,
    this.splitView = true,
    this.controller,
    this.onControllerReady,
  });

  @override
  State<JsonEditorViewer> createState() => _JsonEditorViewerState();
}

class _JsonEditorViewerState extends State<JsonEditorViewer> with TickerProviderStateMixin {
  late final TextEditingController _textController;
  late TabController _tabController;
  dynamic _parsedJson;
  bool _isValidJson = true;
  String? _validationError;
  Timer? _debounceTimer;
  bool _isInitializing = true;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    
    _textController = widget.controller ?? TextEditingController();
    widget.onControllerReady?.call(_textController);

    if (widget.initialJson != null && widget.initialJson!.isNotEmpty) {
      _textController.text = widget.initialJson!;
      _parseJson(immediate: true);
    } else if (_textController.text.isNotEmpty) {
      // If external controller already has content, parse it
      _parseJson(immediate: true);
    } else {
      _textController.text = _getDefaultJson();
      _parseJson(immediate: true);
    }
    
    // Set initialization flag to false after first build
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _isInitializing = false;
    });
  }

  @override
  void dispose() {
    if (widget.controller == null) {
      // Only dispose controller if we created it
      _textController.dispose();
    }
    _tabController.dispose();
    _debounceTimer?.cancel();
    super.dispose();
  }

  String _getDefaultJson() {
    return '''
{
  "env": "finance",
  "interface_num": 4,
  "task": {
    "instruction": "Sample task instruction",
    "actions": [
      {
        "name": "sample_action",
        "arguments": {
          "param1": "value1",
          "param2": 123,
          "param3": true
        }
      }
    ],
    "user_id": "user123",
    "outputs": ["output1", "output2"],
    "edges": [
      {
        "from": "instruction",
        "to": "sample_action",
        "connection": {
          "output": "result",
          "input": "data"
        }
      }
    ]
  }
}''';
  }

  void _parseJson({bool immediate = false}) {
    setState(() {
      try {
        _parsedJson = jsonDecode(_textController.text);
        _isValidJson = true;
        _validationError = null;
        
        // Debounce the onJsonChanged callback to avoid excessive updates
        // Don't call during initialization to prevent build phase issues
        if (!_isInitializing) {
          if (immediate) {
            widget.onJsonChanged?.call(_textController.text);
          } else {
            _debounceTimer?.cancel();
            _debounceTimer = Timer(const Duration(milliseconds: 500), () {
              widget.onJsonChanged?.call(_textController.text);
            });
          }
        }
      } catch (e) {
        _isValidJson = false;
        _validationError = e.toString();
        _parsedJson = {'error': 'Invalid JSON: $e'};
      }
    });
  }

  void _formatJson() {
    try {
      final decoded = jsonDecode(_textController.text);
      const encoder = JsonEncoder.withIndent('  ');
      setState(() {
        _textController.text = encoder.convert(decoded);
        _parseJson(immediate: true);
      });
      _showSnackBar('JSON formatted successfully', Colors.green);
    } catch (e) {
      _showSnackBar('Invalid JSON: Cannot format', Colors.red);
    }
  }

  void _minifyJson() {
    try {
      final decoded = jsonDecode(_textController.text);
      setState(() {
        _textController.text = jsonEncode(decoded);
        _parseJson(immediate: true);
      });
      _showSnackBar('JSON minified successfully', Colors.green);
    } catch (e) {
      _showSnackBar('Invalid JSON: Cannot minify', Colors.red);
    }
  }

  void _copyToClipboard() async {
    try {
      if (kIsWeb) {
        final success = await copyTextWeb(_textController.text);
        if (success) {
          _showSnackBar('JSON copied to clipboard', Colors.green);
        } else {
          _showSnackBar('Failed to copy to clipboard', Colors.red);
        }
      } else {
        await Clipboard.setData(ClipboardData(text: _textController.text));
        _showSnackBar('JSON copied to clipboard', Colors.green);
      }
    } catch (e) {
      _showSnackBar('Failed to copy: $e', Colors.red);
    }
  }

  void _clearJson() {
    setState(() {
      _textController.clear();
      _parsedJson = null;
      _isValidJson = true;
      _validationError = null;
    });
  }

  void _downloadJson() async {
    try {
      if (_textController.text.isEmpty) {
        _showSnackBar('No JSON content to download', Colors.orange);
        return;
      }

      // Create filename with timestamp
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final filename = '${widget.title?.replaceAll(' ', '_') ?? 'json_data'}_$timestamp.json';
      
      // Prepare JSON content (formatted if valid)
      String jsonContent = _textController.text;
      if (_isValidJson && _parsedJson != null) {
        // Use formatted JSON for better readability
        jsonContent = const JsonEncoder.withIndent('  ').convert(_parsedJson);
      }
      
      // Download for web
      if (kIsWeb) {
        final bytes = Uint8List.fromList(jsonContent.codeUnits);
        final blob = html.Blob([bytes]);
        final url = html.Url.createObjectUrlFromBlob(blob);
        
        (html.AnchorElement(href: url)
          ..setAttribute('download', filename))
          .click();
        
        html.Url.revokeObjectUrl(url);
        _showSnackBar('JSON downloaded as $filename', Colors.green);
      } else {
        _showSnackBar('Download not supported on this platform', Colors.orange);
      }
    } catch (e) {
      _showSnackBar('Failed to download JSON: $e', Colors.red);
    }
  }

  void _showSnackBar(String message, Color color) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: color,
        duration: const Duration(seconds: 2),
      ),
    );
  }

  Widget _buildToolbar() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.grey[100],
        border: Border(bottom: BorderSide(color: Colors.grey[300]!)),
      ),
      child: Row(
        children: [
          // Validation Status
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: _isValidJson ? Colors.green.withValues(alpha: 0.1) : Colors.red.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(
                color: _isValidJson ? Colors.green : Colors.red,
                width: 1,
              ),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(
                  _isValidJson ? Icons.check_circle : Icons.error,
                  color: _isValidJson ? Colors.green : Colors.red,
                  size: 16,
                ),
                const SizedBox(width: 4),
                Text(
                  _isValidJson ? 'Valid JSON' : 'Invalid JSON',
                  style: TextStyle(
                    color: _isValidJson ? Colors.green[700] : Colors.red[700],
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          // Action Buttons
          IconButton(
            icon: const Icon(Icons.code),
            tooltip: 'Format JSON (Pretty Print)',
            onPressed: _isValidJson ? _formatJson : null,
            iconSize: 20,
          ),
          IconButton(
            icon: const Icon(Icons.compress),
            tooltip: 'Minify JSON',
            onPressed: _isValidJson ? _minifyJson : null,
            iconSize: 20,
          ),
          IconButton(
            icon: const Icon(Icons.copy),
            tooltip: 'Copy to Clipboard',
            onPressed: _copyToClipboard,
            iconSize: 20,
          ),
          IconButton(
            icon: const Icon(Icons.download),
            tooltip: 'Download JSON',
            onPressed: _isValidJson ? _downloadJson : null,
            iconSize: 20,
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Parse/Refresh',
            onPressed: () => _parseJson(immediate: true),
            iconSize: 20,
          ),
          const VerticalDivider(width: 16),
          IconButton(
            icon: const Icon(Icons.clear),
            tooltip: 'Clear JSON',
            onPressed: _clearJson,
            iconSize: 20,
          ),
          const Spacer(),
          // Character count
          if (_textController.text.isNotEmpty)
            Text(
              '${_textController.text.length} chars',
              style: TextStyle(
                fontSize: 11,
                color: Colors.grey[600],
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildEditorView() {
    return Container(
      padding: const EdgeInsets.all(8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'JSON Editor',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: Container(
              decoration: BoxDecoration(
                border: Border.all(color: Colors.grey[300]!),
                borderRadius: BorderRadius.circular(4),
              ),
              child: TextField(
                controller: _textController,
                maxLines: null,
                expands: true,
                decoration: const InputDecoration(
                  border: InputBorder.none,
                  contentPadding: EdgeInsets.all(12),
                  hintText: 'Paste or type JSON here...',
                ),
                style: const TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 12,
                ),
                onChanged: (_) => _parseJson(),
              ),
            ),
          ),
          if (!_isValidJson && _validationError != null) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(
                color: Colors.red[50],
                borderRadius: BorderRadius.circular(4),
                border: Border.all(color: Colors.red[300]!),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.error_outline, color: Colors.red[700], size: 16),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _validationError!,
                      style: TextStyle(
                        color: Colors.red[700],
                        fontSize: 11,
                        fontFamily: 'monospace',
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildViewerView() {
    return Container(
      padding: const EdgeInsets.all(8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            'JSON Tree Viewer',
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 8),
          Expanded(
            child: Container(
              decoration: BoxDecoration(
                color: Colors.white,
                border: Border.all(color: Colors.grey[300]!),
                borderRadius: BorderRadius.circular(4),
              ),
              child: _isValidJson && _parsedJson != null
                  ? SingleChildScrollView(
                      padding: const EdgeInsets.all(12),
                      child: JsonViewer(_parsedJson),
                    )
                  : Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            _isValidJson ? Icons.code : Icons.error_outline,
                            size: 48,
                            color: Colors.grey[400],
                          ),
                          const SizedBox(height: 16),
                          Text(
                            _isValidJson 
                                ? 'Enter valid JSON to view tree structure'
                                : 'Fix JSON syntax errors to view tree',
                            style: TextStyle(
                              color: Colors.grey[600],
                              fontSize: 14,
                            ),
                            textAlign: TextAlign.center,
                          ),
                        ],
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSplitView() {
    return Row(
      children: [
        Expanded(child: _buildEditorView()),
        Container(
          width: 1,
          color: Colors.grey[300],
        ),
        Expanded(child: _buildViewerView()),
      ],
    );
  }

  Widget _buildTabbedView() {
    return Column(
      children: [
        TabBar(
          controller: _tabController,
          tabs: const [
            Tab(
              icon: Icon(Icons.edit),
              text: 'Editor',
            ),
            Tab(
              icon: Icon(Icons.account_tree),
              text: 'Viewer',
            ),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: _tabController,
            children: [
              _buildEditorView(),
              _buildViewerView(),
            ],
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        if (widget.showToolbar) _buildToolbar(),
        Expanded(
          child: widget.splitView ? _buildSplitView() : _buildTabbedView(),
        ),
      ],
    );
  }
}

/// Standalone JSON Editor + Viewer Screen
class JsonEditorViewerScreen extends StatelessWidget {
  const JsonEditorViewerScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('JSON Editor + Viewer'),
        backgroundColor: const Color(0xFF2563EB),
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: const JsonEditorViewer(
        showToolbar: true,
        splitView: true,
      ),
    );
  }
}