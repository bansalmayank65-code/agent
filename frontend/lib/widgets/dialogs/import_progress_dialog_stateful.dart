import 'package:flutter/material.dart';

/// Controller to manage import progress dialog state
class ImportProgressController extends ChangeNotifier {
  String? _currentStep;
  double _progress = 0.0;
  String? _errorMessage;
  bool _isComplete = false;

  String? get currentStep => _currentStep;
  double get progress => _progress;
  String? get errorMessage => _errorMessage;
  bool get isComplete => _isComplete;
  bool get hasError => _errorMessage != null;

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
    notifyListeners();
  }
}

/// Stateful dialog showing progress while importing task.json
class ImportProgressDialog extends StatelessWidget {
  final ImportProgressController controller;

  const ImportProgressDialog({
    Key? key,
    required this.controller,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controller,
      builder: (context, child) {
        final isError = controller.hasError;
        final isComplete = controller.isComplete;

        return WillPopScope(
          onWillPop: () async => isError || isComplete,
          child: Dialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
            child: Container(
              width: 450,
              padding: const EdgeInsets.all(32),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Icon
                  Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      color: isError
                          ? Colors.red.shade50
                          : isComplete
                              ? Colors.green.shade50
                              : Colors.orange.shade50,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      isError
                          ? Icons.error_outline
                          : isComplete
                              ? Icons.check_circle_outline
                              : Icons.upload_file,
                      size: 36,
                      color: isError
                          ? Colors.red.shade600
                          : isComplete
                              ? Colors.green.shade600
                              : Colors.orange.shade600,
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Title
                  Text(
                    isError
                        ? 'Import Failed'
                        : isComplete
                            ? 'Import Complete!'
                            : 'Importing Task JSON',
                    style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                        ),
                  ),
                  const SizedBox(height: 12),

                  // Current step or error message
                  if (isError)
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.red.shade50,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.warning_amber, color: Colors.red.shade700, size: 20),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              controller.errorMessage!,
                              style: TextStyle(
                                color: Colors.red.shade700,
                                fontSize: 14,
                              ),
                            ),
                          ),
                        ],
                      ),
                    )
                  else if (controller.currentStep != null)
                    Text(
                      controller.currentStep!,
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                            color: Colors.grey.shade700,
                          ),
                      textAlign: TextAlign.center,
                    ),

                  const SizedBox(height: 28),

                  // Progress indicator
                  if (!isError && !isComplete) ...[
                    ClipRRect(
                      borderRadius: BorderRadius.circular(8),
                      child: LinearProgressIndicator(
                        value: controller.progress > 0 ? controller.progress : null,
                        minHeight: 8,
                        backgroundColor: Colors.grey.shade200,
                        valueColor: AlwaysStoppedAnimation<Color>(Colors.orange.shade600),
                      ),
                    ),
                    const SizedBox(height: 12),
                    Text(
                      '${(controller.progress * 100).toInt()}%',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: Colors.grey.shade700,
                            fontWeight: FontWeight.w600,
                          ),
                    ),
                  ] else if (isComplete) ...[
                    Icon(Icons.done_all, size: 56, color: Colors.green.shade600),
                  ],

                  // Close button (only shown on error or complete)
                  if (isError || isComplete) ...[
                    const SizedBox(height: 24),
                    Row(
                      children: [
                        if (isError) ...[
                          Expanded(
                            child: OutlinedButton(
                              onPressed: () => Navigator.of(context).pop(false),
                              style: OutlinedButton.styleFrom(
                                padding: const EdgeInsets.symmetric(vertical: 14),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(8),
                                ),
                              ),
                              child: const Text('Cancel'),
                            ),
                          ),
                          const SizedBox(width: 12),
                        ],
                        Expanded(
                          child: ElevatedButton(
                            onPressed: () => Navigator.of(context).pop(isComplete),
                            style: ElevatedButton.styleFrom(
                              backgroundColor: isError 
                                  ? Colors.red.shade600 
                                  : Colors.green.shade600,
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(8),
                              ),
                            ),
                            child: Text(
                              isError ? 'Retry' : 'Continue',
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  /// Show the import progress dialog with a controller
  static Future<bool?> show(
    BuildContext context,
    ImportProgressController controller,
  ) {
    return showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => ImportProgressDialog(controller: controller),
    );
  }
}
