import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../services/home_screen/ui_helper_service.dart';
import '../progress_indicator_widget.dart';

/// Widget for Validation section
class ValidationSectionWidget extends StatefulWidget {
  const ValidationSectionWidget({super.key});

  @override
  State<ValidationSectionWidget> createState() => _ValidationSectionWidgetState();
}

class _ValidationSectionWidgetState extends State<ValidationSectionWidget> {

  /// Safely get validation state from provider
  ({Set<String> runningSteps, Map<String, ValidationStepResult> stepResults}) _getValidationState(TaskProvider provider) {
    try {
      return (
        runningSteps: provider.runningValidationSteps,
        stepResults: provider.validationStepResults,
      );
    } catch (e) {
      // Return empty state if provider is not ready
      return (
        runningSteps: <String>{},
        stepResults: <String, ValidationStepResult>{},
      );
    }
  }

  /// Safely check if a step is running
  bool _isStepRunning(TaskProvider provider, String step) {
    try {
      final state = _getValidationState(provider);
      return state.runningSteps.contains(step);
    } catch (e) {
      return false;
    }
  }

  /// Safely check if any steps are running
  bool _hasRunningSteps(TaskProvider provider) {
    try {
      final state = _getValidationState(provider);
      return state.runningSteps.isNotEmpty;
    } catch (e) {
      return false;
    }
  }

  /// Safely check if there are any step results
  bool _hasStepResults(TaskProvider provider) {
    try {
      final state = _getValidationState(provider);
      return state.stepResults.isNotEmpty;
    } catch (e) {
      return false;
    }
  }

  /// Build validation button with progress indicator
  Widget _buildValidationButton(TaskProvider provider, String label, String step) {
    // Safely get validation state
    final state = _getValidationState(provider);
    
    final isRunning = _isStepRunning(provider, step);
    final result = state.stepResults[step];
    
    Color? buttonColor;
    IconData icon = Icons.play_arrow;
    
    if (isRunning) {
      buttonColor = Colors.orange;
      icon = Icons.hourglass_empty;
    } else if (result != null) {
      switch (result.status) {
        case ValidationStepStatus.success:
          buttonColor = Colors.green;
          icon = Icons.check_circle;
          break;
        case ValidationStepStatus.error:
          buttonColor = Colors.red;
          icon = Icons.error;
          break;
        default:
          break;
      }
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 8.0),
      child: SizedBox(
        width: double.infinity,
        child: ElevatedButton.icon(
          onPressed: isRunning ? null : () => _runValidationStep(provider, step),
          style: ElevatedButton.styleFrom(
            backgroundColor: buttonColor,
            foregroundColor: buttonColor != null ? Colors.white : null,
            padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
          ),
          icon: isRunning 
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    valueColor: AlwaysStoppedAnimation<Color>(Colors.white),
                  ),
                )
              : Icon(icon),
          label: Text(
            isRunning ? 'Running $label...' : label,
            style: const TextStyle(fontWeight: FontWeight.w500),
          ),
        ),
      ),
    );
  }

  /// Run validation step with progress tracking
  Future<void> _runValidationStep(TaskProvider provider, String step) async {
    provider.setValidationStepRunning(step);

    try {
      final resp = await provider.runTauBenchStep(step);
      
      if (resp != null) {
        final success = resp['success'] == true;
        provider.setValidationStepResult(step, ValidationStepResult(
          step: step,
          status: success ? ValidationStepStatus.success : ValidationStepStatus.error,
          data: resp,
          error: success ? null : (resp['error']?.toString() ?? 'Unknown error'),
          timestamp: DateTime.now(),
        ));
        
        if (mounted) {
          UIHelperService.showToast(
            context, 
            success ? 'Step $step completed successfully' : 'Step $step failed'
          );
        }
      } else {
        provider.setValidationStepResult(step, ValidationStepResult(
          step: step,
          status: ValidationStepStatus.error,
          error: 'No response received',
          timestamp: DateTime.now(),
        ));
      }
    } catch (e) {
      provider.setValidationStepResult(step, ValidationStepResult(
        step: step,
        status: ValidationStepStatus.error,
        error: e.toString(),
        timestamp: DateTime.now(),
      ));
    }
    // Note: setValidationStepResult already removes from running steps
  }

  /// Build results area with expandable log details
  Widget _buildResultArea(TaskProvider provider) {
    final state = _getValidationState(provider);
    if (state.stepResults.isEmpty) {
      return Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.grey[50],
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: Colors.grey[300]!),
        ),
        child: const Row(
          children: [
            Icon(Icons.info_outline, color: Colors.grey),
            SizedBox(width: 8),
            Text(
              'Run validation steps to see results',
              style: TextStyle(color: Colors.grey),
            ),
          ],
        ),
      );
    }
    
    return Column(
      children: state.stepResults.values.map((result) => _buildResultCard(result)).toList(),
    );
  }

  /// Build individual result card
  Widget _buildResultCard(ValidationStepResult result) {
    Color headerColor;
    IconData statusIcon;
    String statusText;
    
    switch (result.status) {
      case ValidationStepStatus.running:
        headerColor = Colors.orange;
        statusIcon = Icons.hourglass_empty;
        statusText = 'Running...';
        break;
      case ValidationStepStatus.success:
        headerColor = Colors.green;
        statusIcon = Icons.check_circle;
        statusText = 'Success';
        break;
      case ValidationStepStatus.error:
        headerColor = Colors.red;
        statusIcon = Icons.error;
        statusText = 'Failed';
        break;
      default:
        headerColor = Colors.grey;
        statusIcon = Icons.help;
        statusText = 'Unknown';
    }

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: headerColor.withOpacity(0.3)),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 4,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: headerColor.withOpacity(0.1),
              borderRadius: const BorderRadius.vertical(top: Radius.circular(8)),
            ),
            child: Row(
              children: [
                Icon(statusIcon, color: headerColor, size: 20),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    result.step.replaceAll('_', ' ').toUpperCase(),
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: headerColor,
                    ),
                  ),
                ),
                Text(
                  statusText,
                  style: TextStyle(
                    color: headerColor,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  _formatTimestamp(result.timestamp),
                  style: TextStyle(
                    color: Colors.grey[600],
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
          
          // Content
          if (result.status == ValidationStepStatus.running)
            const Padding(
              padding: EdgeInsets.all(16),
              child: Row(
                children: [
                  SizedBox(
                    width: 20,
                    height: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                  SizedBox(width: 12),
                  Text('Validation in progress...'),
                ],
              ),
            )
          else if (result.error != null)
            _buildErrorContent(result.error!)
          else if (result.data != null)
            _buildSuccessContent(result.data!),
        ],
      ),
    );
  }

  /// Build error content section
  Widget _buildErrorContent(String error) {
    return ExpansionTile(
      title: const Text(
        'Error Details',
        style: TextStyle(color: Colors.red, fontWeight: FontWeight.w500),
      ),
      leading: const Icon(Icons.error_outline, color: Colors.red),
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          margin: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.red[50],
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: Colors.red[200]!),
          ),
          child: Text(
            error,
            style: const TextStyle(
              fontFamily: 'monospace',
              fontSize: 12,
              color: Colors.red,
            ),
          ),
        ),
      ],
    );
  }

  /// Build success content section
  Widget _buildSuccessContent(Map<String, dynamic> data) {
    // Always show detailed data for TauBench responses
    return ExpansionTile(
      title: const Text(
        'Validation Results',
        style: TextStyle(color: Colors.green, fontWeight: FontWeight.w500),
      ),
      leading: const Icon(Icons.check_circle, color: Colors.green),
      initiallyExpanded: true, // Expand by default to show details
      children: [
        Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Success status header
              if (data['success'] == true)
                Container(
                  padding: const EdgeInsets.all(8),
                  margin: const EdgeInsets.only(bottom: 16),
                  decoration: BoxDecoration(
                    color: Colors.green[50],
                    borderRadius: BorderRadius.circular(4),
                    border: Border.all(color: Colors.green[200]!),
                  ),
                  child: const Row(
                    children: [
                      Icon(Icons.check, color: Colors.green, size: 16),
                      SizedBox(width: 8),
                      Text(
                        'Validation completed successfully',
                        style: TextStyle(color: Colors.green, fontWeight: FontWeight.w500),
                      ),
                    ],
                  ),
                ),
              
              // Response message
              if (data['message'] != null)
                _buildInfoItem('Response Message', data['message'].toString()),
              
              // Show visualization if available (for compute_complexity)
              if (data['hasPlot'] == true || data['plotBase64'] != null)
                _buildVisualizationSection(data),
              
              // Show all response data in expandable format
              _buildFullResponseData(data),
              
              // Download buttons if data is available
              _buildActionButtons(data),
            ],
          ),
        ),
      ],
    );
  }

  /// Build visualization section for generated graphs/charts
  Widget _buildVisualizationSection(Map<String, dynamic> data) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Generated Visualization:',
            style: TextStyle(fontWeight: FontWeight.w500, fontSize: 14),
          ),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey[50],
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey[300]!),
            ),
            child: Column(
              children: [
                Row(
                  children: [
                    const Icon(Icons.image, color: Colors.blue, size: 20),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text(
                        'Complexity analysis graph',
                        style: TextStyle(fontWeight: FontWeight.w500),
                      ),
                    ),
                    TextButton.icon(
                      onPressed: () => _viewImageFullscreen(data),
                      icon: const Icon(Icons.open_in_new, size: 16),
                      label: const Text('View Full'),
                      style: TextButton.styleFrom(
                        foregroundColor: Colors.blue,
                      ),
                    ),
                    const SizedBox(width: 8),
                    TextButton.icon(
                      onPressed: () => _downloadData(data),
                      icon: const Icon(Icons.download, size: 16),
                      label: const Text('Download'),
                      style: TextButton.styleFrom(
                        foregroundColor: Colors.green,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                // Show actual image or loading state
                _buildImageWidget(data),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Build the actual image widget
  Widget _buildImageWidget(Map<String, dynamic> data) {
    // Check if we have base64 image data from the response
    final String? plotBase64 = data['plotBase64'] as String?;
    
    // Debug information
    print('Regular image data keys: ${data.keys}');
    print('Regular plotBase64 length: ${plotBase64?.length ?? 0}');
    
    if (plotBase64 != null && plotBase64.isNotEmpty) {
      try {
        // Decode base64 image
        final imageBytes = base64Decode(plotBase64);
        return Container(
          height: 200,
          width: double.infinity,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: Colors.grey[300]!),
          ),
          child: ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: Image.memory(
              imageBytes,
              fit: BoxFit.contain,
              errorBuilder: (context, error, stackTrace) {
                return _buildImageError('Failed to decode image data');
              },
            ),
          ),
        );
      } catch (e) {
        return _buildImageError('Invalid image data format');
      }
    }
    
    // Fallback: try to load from URL (though this might fail)
    const String baseUrl = 'http://localhost:8080';
    final String imageUrl = '$baseUrl/cache/image/output.png';
    
    return Container(
      height: 200,
      width: double.infinity,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: Colors.grey[300]!),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(4),
        child: Image.network(
          imageUrl,
          fit: BoxFit.contain,
          headers: {
            'Cache-Control': 'no-cache',
          },
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return Container(
              color: Colors.grey[200],
              child: const Center(
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    CircularProgressIndicator(),
                    SizedBox(height: 8),
                    Text('Loading visualization...'),
                  ],
                ),
              ),
            );
          },
          errorBuilder: (context, error, stackTrace) {
            return _buildImageError('Failed to load image from server');
          },
        ),
      ),
    );
  }

  /// Build error state for image loading
  Widget _buildImageError(String message) {
    return Container(
      color: Colors.grey[200],
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const Icon(Icons.error_outline, size: 32, color: Colors.red),
          const SizedBox(height: 8),
          Text(
            message,
            style: const TextStyle(color: Colors.red),
          ),
          const Text(
            'Check if visualization was generated',
            style: TextStyle(color: Colors.grey, fontSize: 12),
          ),
        ],
      ),
    );
  }

  /// Download data - either image or JSON based on content
  Future<void> _downloadData(Map<String, dynamic> data) async {
    final provider = Provider.of<TaskProvider>(context, listen: false);
    
    try {
      // Check if this has image data
      final String? plotBase64 = data['plotBase64'] as String?;
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      
      if (plotBase64 != null && plotBase64.isNotEmpty) {
        // Download as image
        final filename = 'complexity_graph_$timestamp.png';
        await provider.downloadImage(plotBase64, filename);
        
        if (mounted) {
          UIHelperService.showToast(
            context,
            'Image downloaded successfully as $filename'
          );
        }
      } else {
        // Download as JSON
        final filename = 'taubench_graph_data_$timestamp.json';
        await provider.downloadGraphData(data, filename);
        
        if (mounted) {
          UIHelperService.showToast(
            context,
            'Graph data downloaded successfully as $filename'
          );
        }
      }
    } catch (e) {
      if (mounted) {
        UIHelperService.showToast(
          context,
          'Failed to download data: $e'
        );
      }
    }
  }

  /// Open image in fullscreen dialog
  void _viewImageFullscreen(Map<String, dynamic> data) {
    showDialog(
      context: context,
      barrierDismissible: true,
      builder: (context) => Dialog.fullscreen(
        child: Container(
          color: Colors.white,
          child: Column(
            children: [
              // Header
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 4,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                child: Row(
                  children: [
                    const Icon(Icons.bar_chart, color: Colors.blue),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text(
                        'Complexity Analysis Visualization',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
                      ),
                    ),
                    IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(Icons.close),
                      tooltip: 'Close',
                    ),
                  ],
                ),
              ),
              // Image content
              Expanded(
                child: Container(
                  padding: const EdgeInsets.all(24),
                  child: Center(
                    child: _buildFullscreenImage(data),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// Build fullscreen image widget
  Widget _buildFullscreenImage(Map<String, dynamic> data) {
    final String? plotBase64 = data['plotBase64'] as String?;
    
    // Debug information
    print('Fullscreen image data keys: ${data.keys}');
    print('plotBase64 length: ${plotBase64?.length ?? 0}');
    
    if (plotBase64 != null && plotBase64.isNotEmpty) {
      try {
        final imageBytes = base64Decode(plotBase64);
        return Container(
          constraints: const BoxConstraints(
            maxWidth: double.infinity,
            maxHeight: double.infinity,
          ),
          child: InteractiveViewer(
            panEnabled: true,
            scaleEnabled: true,
            maxScale: 5.0,
            minScale: 0.5,
            child: Image.memory(
              imageBytes,
              fit: BoxFit.contain,
              errorBuilder: (context, error, stackTrace) {
                print('Error loading fullscreen image: $error');
                return const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.error_outline, size: 64, color: Colors.red),
                      SizedBox(height: 16),
                      Text(
                        'Failed to decode image data',
                        style: TextStyle(fontSize: 18, color: Colors.red),
                      ),
                    ],
                  ),
                );
              },
            ),
          ),
        );
      } catch (e) {
        print('Error decoding base64 image: $e');
        return Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 64, color: Colors.red),
              const SizedBox(height: 16),
              const Text(
                'Invalid image format',
                style: TextStyle(fontSize: 18, color: Colors.red),
              ),
              Text(
                'Error: $e',
                style: const TextStyle(color: Colors.grey, fontSize: 12),
              ),
            ],
          ),
        );
      }
    }
    
    // Fallback to network image
    const String baseUrl = 'http://localhost:8080';
    final String imageUrl = '$baseUrl/cache/image/output.png';
    
    return Container(
      constraints: const BoxConstraints(
        maxWidth: double.infinity,
        maxHeight: double.infinity,
      ),
      child: InteractiveViewer(
        panEnabled: true,
        scaleEnabled: true,
        maxScale: 5.0,
        minScale: 0.5,
        child: Image.network(
          imageUrl,
          fit: BoxFit.contain,
          headers: {'Cache-Control': 'no-cache'},
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  CircularProgressIndicator(),
                  SizedBox(height: 16),
                  Text('Loading full-size visualization...'),
                ],
              ),
            );
          },
          errorBuilder: (context, error, stackTrace) {
            print('Error loading network image: $error');
            return const Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.error_outline, size: 64, color: Colors.red),
                  SizedBox(height: 16),
                  Text(
                    'Failed to load visualization',
                    style: TextStyle(fontSize: 18, color: Colors.red),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'No image data available',
                    style: TextStyle(color: Colors.grey),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
            );
          },
        ),
      ),
    );
  }

  /// Build info item row
  Widget _buildInfoItem(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(
              '$label:',
              style: const TextStyle(fontWeight: FontWeight.w500),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }

  /// Build comprehensive response data display
  Widget _buildFullResponseData(Map<String, dynamic> data) {
    return Container(
      margin: const EdgeInsets.only(top: 16),
      child: ExpansionTile(
        title: const Text(
          'Full Response Data',
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
        ),
        leading: const Icon(Icons.data_object, size: 18),
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.all(12),
            margin: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: Colors.grey[50],
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: Colors.grey[300]!),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Show key response fields
                ...data.entries.map((entry) => _buildDataRow(entry.key, entry.value)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Build individual data row
  Widget _buildDataRow(String key, dynamic value) {
    String displayValue;
    
    if (value == null) {
      displayValue = 'null';
    } else if (value is Map || value is List) {
      displayValue = JsonEncoder.withIndent('  ').convert(value);
    } else {
      displayValue = value.toString();
    }

    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(
              '$key:',
              style: const TextStyle(
                fontWeight: FontWeight.w500,
                fontSize: 12,
              ),
            ),
          ),
          Expanded(
            child: Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(2),
                border: Border.all(color: Colors.grey[200]!),
              ),
              child: Text(
                displayValue,
                style: const TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 11,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Build action buttons for downloading/copying data
  Widget _buildActionButtons(Map<String, dynamic> data) {
    return Container(
      margin: const EdgeInsets.only(top: 16),
      child: Row(
        children: [
          ElevatedButton.icon(
            onPressed: () => _downloadResponseData(data),
            icon: const Icon(Icons.download, size: 16),
            label: const Text('Download Response'),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.blue[600],
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            ),
          ),
          const SizedBox(width: 8),
          OutlinedButton.icon(
            onPressed: () => _copyResponseToClipboard(data),
            icon: const Icon(Icons.copy, size: 16),
            label: const Text('Copy Response'),
            style: OutlinedButton.styleFrom(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
            ),
          ),
        ],
      ),
    );
  }

  /// Download response data as JSON
  Future<void> _downloadResponseData(Map<String, dynamic> data) async {
    try {
      final provider = Provider.of<TaskProvider>(context, listen: false);
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final filename = 'validation_response_$timestamp.json';
      
      await provider.downloadGraphData(data, filename);
      
      if (mounted) {
        UIHelperService.showToast(
          context,
          'Response data downloaded as $filename'
        );
      }
    } catch (e) {
      if (mounted) {
        UIHelperService.showToast(
          context,
          'Failed to download response: $e'
        );
      }
    }
  }

  /// Copy response data to clipboard
  Future<void> _copyResponseToClipboard(Map<String, dynamic> data) async {
    try {
      final jsonString = JsonEncoder.withIndent('  ').convert(data);
      await Clipboard.setData(ClipboardData(text: jsonString));
      
      if (mounted) {
        UIHelperService.showToast(
          context,
          'Response data copied to clipboard'
        );
      }
    } catch (e) {
      if (mounted) {
        UIHelperService.showToast(
          context,
          'Failed to copy response: $e'
        );
      }
    }
  }

  /// Format timestamp for display
  String _formatTimestamp(DateTime timestamp) {
    final now = DateTime.now();
    final diff = now.difference(timestamp);
    
    if (diff.inMinutes < 1) {
      return 'Just now';
    } else if (diff.inMinutes < 60) {
      return '${diff.inMinutes}m ago';
    } else {
      return '${timestamp.hour.toString().padLeft(2, '0')}:${timestamp.minute.toString().padLeft(2, '0')}';
    }
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
                const Icon(Icons.verified, color: Color(0xFF059669)),
                const SizedBox(width: 8),
                const Text(
                  'Task Validation',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
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
                      'Run validation steps to verify your task configuration before deployment.',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            
            const Text(
              'Validation Steps:',
              style: TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            
            // _buildValidationButton(provider, 'Compute Complexity', 'compute_complexity'), // Temporarily disabled
            _buildValidationButton(provider, 'Task Verification', 'task_verification'),
            _buildValidationButton(provider, 'Run Task', 'run_task'),
            _buildValidationButton(provider, 'Evaluate', 'evaluate'),
            
            // Show progress indicators for running steps
            ...(() {
              final state = _getValidationState(provider);
              return state.runningSteps.map((step) => ValidationProgressIndicator(
                stepName: step.replaceAll('_', ' ').toLowerCase(),
                isRunning: true,
                currentAction: 'Executing validation...',
              ));
            })(),
            
            const SizedBox(height: 24),
            
            // Action buttons row
            Row(
              children: [
                const Text(
                  'Results:',
                  style: TextStyle(fontWeight: FontWeight.bold),
                ),
                const Spacer(),
                if (_hasStepResults(provider)) ...[
                  TextButton.icon(
                    onPressed: () {
                      provider.clearValidationResults();
                    },
                    icon: const Icon(Icons.clear_all, size: 16),
                    label: const Text('Clear Results'),
                    style: TextButton.styleFrom(
                      foregroundColor: Colors.grey[600],
                    ),
                  ),
                  const SizedBox(width: 8),
                ],
                TextButton.icon(
                  onPressed: () async {
                    // Run all validation steps in sequence (excluding compute_complexity temporarily)
                    final steps = ['task_verification', 'run_task', 'evaluate'];
                    for (final step in steps) {
                      await _runValidationStep(provider, step);
                      // Small delay between steps
                      await Future.delayed(const Duration(milliseconds: 500));
                    }
                  },
                  icon: _hasRunningSteps(provider) 
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.play_circle, size: 16),
                  label: Text(_hasRunningSteps(provider) ? 'Running...' : 'Run All'),
                  style: TextButton.styleFrom(
                    foregroundColor: Colors.green[700],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            
            _buildResultArea(provider),
          ],
        );
      },
    );
  }
}