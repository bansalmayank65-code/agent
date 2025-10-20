import 'package:flutter/material.dart';

/// Dialog showing progress while importing task.json
class ImportProgressDialog extends StatelessWidget {
  final String? currentStep;
  final double progress;
  final String? errorMessage;

  const ImportProgressDialog({
    Key? key,
    this.currentStep,
    this.progress = 0.0,
    this.errorMessage,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final isError = errorMessage != null;
    final isComplete = progress >= 1.0 && !isError;

    return Dialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Container(
        width: 400,
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Icon
            Container(
              width: 64,
              height: 64,
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
                size: 32,
                color: isError
                    ? Colors.red.shade600
                    : isComplete
                        ? Colors.green.shade600
                        : Colors.orange.shade600,
              ),
            ),
            const SizedBox(height: 16),

            // Title
            Text(
              isError
                  ? 'Import Failed'
                  : isComplete
                      ? 'Import Complete!'
                      : 'Importing Task...',
              style: Theme.of(context).textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                  ),
            ),
            const SizedBox(height: 8),

            // Current step or error message
            if (isError)
              Text(
                errorMessage!,
                style: TextStyle(color: Colors.red.shade700),
                textAlign: TextAlign.center,
              )
            else if (currentStep != null)
              Text(
                currentStep!,
                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                      color: Colors.grey.shade700,
                    ),
                textAlign: TextAlign.center,
              ),

            const SizedBox(height: 24),

            // Progress indicator
            if (!isError && !isComplete) ...[
              LinearProgressIndicator(
                value: progress > 0 ? progress : null,
                backgroundColor: Colors.grey.shade200,
                valueColor: AlwaysStoppedAnimation<Color>(Colors.orange.shade600),
              ),
              const SizedBox(height: 8),
              Text(
                '${(progress * 100).toInt()}%',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Colors.grey.shade600,
                    ),
              ),
            ] else if (isComplete) ...[
              Icon(Icons.done, size: 48, color: Colors.green.shade600),
            ],

            // Close button (only shown on error or complete)
            if (isError || isComplete) ...[
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () => Navigator.of(context).pop(isComplete),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: isError ? Colors.red.shade600 : Colors.orange.shade600,
                    padding: const EdgeInsets.symmetric(vertical: 12),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                  ),
                  child: Text(
                    isError ? 'Close' : 'Continue',
                    style: const TextStyle(color: Colors.white),
                  ),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  /// Show the import progress dialog
  static void show(BuildContext context) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (context) => const ImportProgressDialog(),
    );
  }

  /// Update the dialog with new progress
  static void update(
    BuildContext context, {
    String? currentStep,
    double? progress,
    String? errorMessage,
  }) {
    // This will be used with a StatefulWidget wrapper if needed
    // For now, we'll manage state externally
  }
}
