import 'package:flutter/material.dart';

/// Controller to manage import progress state
class ImportProgressController extends ChangeNotifier {
  String? _currentStep;
  double _progress = 0.0;
  String? _errorMessage;
  bool _isComplete = false;
  bool _isVisible = false;

  String? get currentStep => _currentStep;
  double get progress => _progress;
  String? get errorMessage => _errorMessage;
  bool get isComplete => _isComplete;
  bool get hasError => _errorMessage != null;
  bool get isVisible => _isVisible;
  bool get isLoading => _isVisible && !_isComplete && !hasError;

  void show() {
    _isVisible = true;
    _progress = 0.0;
    _currentStep = null;
    _errorMessage = null;
    _isComplete = false;
    notifyListeners();
  }

  void hide() {
    _isVisible = false;
    notifyListeners();
  }

  void updateProgress({
    String? currentStep,
    double? progress,
  }) {
    if (currentStep != null) _currentStep = currentStep;
    if (progress != null) _progress = progress.clamp(0.0, 1.0);
    _isComplete = _progress >= 1.0 && _errorMessage == null;
    notifyListeners();
  }

  void setError(String message) {
    _errorMessage = message;
    _isComplete = false;
    notifyListeners();
  }

  void complete() {
    _progress = 1.0;
    _isComplete = true;
    _currentStep = 'Import completed successfully';
    notifyListeners();
  }

  void reset() {
    _currentStep = null;
    _progress = 0.0;
    _errorMessage = null;
    _isComplete = false;
    _isVisible = false;
    notifyListeners();
  }
}

/// Inline widget showing progress while importing task.json
class InlineImportProgress extends StatelessWidget {
  final ImportProgressController controller;
  final VoidCallback? onRetry;
  final VoidCallback? onDismiss;

  const InlineImportProgress({
    Key? key,
    required this.controller,
    this.onRetry,
    this.onDismiss,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, child) {
        if (!controller.isVisible) {
          return const SizedBox.shrink();
        }

        final isError = controller.hasError;
        final isComplete = controller.isComplete;
        final isLoading = controller.isLoading;

        return Container(
          margin: const EdgeInsets.only(bottom: 8),
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
          decoration: BoxDecoration(
            color: isError
                ? Colors.red.shade50
                : isComplete
                    ? Colors.green.shade50
                    : Colors.blue.shade50,
            borderRadius: BorderRadius.circular(6),
            border: Border.all(
              color: isError
                  ? Colors.red.shade200
                  : isComplete
                      ? Colors.green.shade200
                      : Colors.blue.shade200,
              width: 1,
            ),
          ),
          child: Row(
            children: [
              // Icon
              Icon(
                isError
                    ? Icons.error_outline
                    : isComplete
                        ? Icons.check_circle
                        : Icons.sync,
                size: 16,
                color: isError
                    ? Colors.red.shade700
                    : isComplete
                        ? Colors.green.shade700
                        : Colors.blue.shade700,
              ),
              const SizedBox(width: 10),
              
              // Text content
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      isError
                          ? 'Import Failed'
                          : isComplete
                              ? 'Import completed successfully'
                              : controller.currentStep ?? 'Importing...',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w500,
                        color: isError
                            ? Colors.red.shade800
                            : isComplete
                                ? Colors.green.shade800
                                : Colors.blue.shade800,
                      ),
                    ),
                    
                    // Error message
                    if (isError && controller.errorMessage != null) ...[
                      const SizedBox(height: 4),
                      Text(
                        controller.errorMessage!,
                        style: TextStyle(
                          fontSize: 11,
                          color: Colors.red.shade700,
                        ),
                      ),
                    ],
                    
                    // Progress bar (only show when loading)
                    if (isLoading) ...[
                      const SizedBox(height: 6),
                      Row(
                        children: [
                          Expanded(
                            child: ClipRRect(
                              borderRadius: BorderRadius.circular(2),
                              child: LinearProgressIndicator(
                                value: controller.progress > 0 ? controller.progress : null,
                                minHeight: 3,
                                backgroundColor: Colors.grey.shade200,
                                valueColor: AlwaysStoppedAnimation<Color>(Colors.blue.shade600),
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            '${(controller.progress * 100).toInt()}%',
                            style: TextStyle(
                              fontSize: 10,
                              color: Colors.grey.shade600,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
              
              // Action buttons
              if (isError && onRetry != null) ...[
                const SizedBox(width: 8),
                TextButton(
                  onPressed: onRetry,
                  style: TextButton.styleFrom(
                    foregroundColor: Colors.red.shade700,
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    minimumSize: const Size(0, 24),
                    textStyle: const TextStyle(fontSize: 11),
                  ),
                  child: const Text('Retry'),
                ),
              ],
              
              // Close button
              if (isComplete || isError) ...[
                const SizedBox(width: 4),
                IconButton(
                  icon: const Icon(Icons.close, size: 14),
                  padding: const EdgeInsets.all(4),
                  constraints: const BoxConstraints(minWidth: 24, minHeight: 24),
                  onPressed: onDismiss ?? controller.hide,
                  tooltip: 'Dismiss',
                  color: Colors.grey.shade600,
                ),
              ],
            ],
          ),
        );
      },
    );
  }
}
