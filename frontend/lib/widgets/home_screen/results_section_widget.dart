import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../json_editor_viewer.dart';

/// Widget for Results section - displays result.json from TauBench execution
class ResultsSectionWidget extends StatefulWidget {
  final Function(int)? onNavigate;
  
  const ResultsSectionWidget({super.key, this.onNavigate});

  @override
  State<ResultsSectionWidget> createState() => _ResultsSectionWidgetState();
}

class _ResultsSectionWidgetState extends State<ResultsSectionWidget> {
  @override
  void initState() {
    super.initState();
    // Use post-frame callback to avoid setState during build
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadResults();
    });
  }

  /// Load result.json from backend
  Future<void> _loadResults() async {
    if (!mounted) return;
    
    final provider = Provider.of<TaskProvider>(context, listen: false);
    await provider.getResults(); // This will store the data in TaskProvider
  }



  @override
  Widget build(BuildContext context) {
    return Consumer<TaskProvider>(
      builder: (context, provider, child) {
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.assessment, color: Color(0xFF059669)),
                const SizedBox(width: 8),
                const Text(
                  'Result.json',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const Spacer(),
                IconButton(
                  onPressed: _loadResults,
                  icon: const Icon(Icons.refresh),
                  tooltip: 'Refresh Results',
                ),
              ],
            ),
            const SizedBox(height: 16),
            
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
                      'View and download results from TauBench execution. Results are automatically saved after running the "Run Task" validation step.',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            if (provider.resultFilePath != null)
              Container(
                padding: const EdgeInsets.all(8),
                margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(
                  color: Colors.blue[50],
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: Colors.blue[200]!),
                ),
                child: Row(
                  children: [
                    Icon(Icons.folder_open, color: Colors.blue[700], size: 16),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        'Result file: ${provider.resultFilePath}',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.blue[700],
                          fontFamily: 'monospace',
                        ),
                      ),
                    ),
                  ],
                ),
              ),

            // Content area
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.6,
              child: _buildContentArea(provider),
            ),
          ],
        );
      },
    );
  }

  /// Build the main content area
  Widget _buildContentArea(TaskProvider provider) {
    if (provider.isLoading) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Loading results...'),
          ],
        ),
      );
    }

    if (provider.error != null) {
      return _buildErrorState(provider);
    }

    if (provider.resultData == null) {
      return _buildEmptyState();
    }

    return _buildResultsViewer(provider);
  }

  /// Build error state
  Widget _buildErrorState(TaskProvider provider) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.error_outline,
            size: 64,
            color: Colors.red[400],
          ),
          const SizedBox(height: 16),
          Text(
            'Error Loading Results',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.red[700],
            ),
          ),
          const SizedBox(height: 8),
          Container(
            constraints: const BoxConstraints(maxWidth: 400),
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.red[50],
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.red[200]!),
            ),
            child: Text(
              provider.error!,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.red[700]),
            ),
          ),
          const SizedBox(height: 16),
          ElevatedButton.icon(
            onPressed: _loadResults,
            icon: const Icon(Icons.refresh),
            label: const Text('Retry'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red[600],
              foregroundColor: Colors.white,
            ),
          ),
        ],
      ),
    );
  }

  /// Build empty state when no results are available
  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(
            Icons.assessment_outlined,
            size: 64,
            color: Colors.grey[400],
          ),
          const SizedBox(height: 16),
          Text(
            'No Results Available',
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.bold,
              color: Colors.grey[700],
            ),
          ),
          const SizedBox(height: 8),
          Container(
            constraints: const BoxConstraints(maxWidth: 400),
            child: Text(
              'Run the "Run Task" validation step to generate results that will be displayed here.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
          ),
          const SizedBox(height: 24),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              ElevatedButton.icon(
                onPressed: _loadResults,
                icon: const Icon(Icons.refresh),
                label: const Text('Check for Results'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue[600],
                  foregroundColor: Colors.white,
                ),
              ),
              const SizedBox(width: 16),
              TextButton.icon(
                onPressed: widget.onNavigate != null ? () => widget.onNavigate!(9) : null,
                icon: const Icon(Icons.play_arrow),
                label: const Text('Go to Validation'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  /// Build the JSON results viewer
  Widget _buildResultsViewer(TaskProvider provider) {
    // Format the JSON properly whether it's a Map or List
    String resultsJson;
    try {
      resultsJson = JsonEncoder.withIndent('  ').convert(provider.resultData);
    } catch (e) {
      resultsJson = provider.resultData.toString();
    }
    
    return Container(
      decoration: BoxDecoration(
        border: Border.all(color: Colors.grey[300]!),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey[100],
              borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
              border: Border(bottom: BorderSide(color: Colors.grey[300]!)),
            ),
            child: Row(
              children: [
                const Icon(Icons.data_object, size: 20),
                const SizedBox(width: 8),
                const Text(
                  'Result JSON Data',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.green[100],
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.green[300]!),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(Icons.check_circle, size: 14, color: Colors.green[700]),
                      const SizedBox(width: 4),
                      Text(
                        'Results Available',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.green[700],
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          // JSON Editor
          Flexible(
            child: JsonEditorViewer(
              title: 'Result JSON',
              key: ValueKey(resultsJson.hashCode),
              initialJson: resultsJson,
            ),
          ),
        ],
      ),
    );
  }
}