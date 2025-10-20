import 'package:flutter/material.dart';

/// Dialog to show a summary of changes after an operation completes
class ChangesSummaryDialog extends StatelessWidget {
  final String title;
  final List<ChangeItem> changes;
  final VoidCallback? onClose;

  const ChangesSummaryDialog({
    Key? key,
    required this.title,
    required this.changes,
    this.onClose,
  }) : super(key: key);

  static Future<void> show(
    BuildContext context, {
    required String title,
    required List<ChangeItem> changes,
  }) {
    return showDialog(
      context: context,
      builder: (context) => ChangesSummaryDialog(
        title: title,
        changes: changes,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
      ),
      child: Container(
        constraints: const BoxConstraints(maxWidth: 600, maxHeight: 500),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              children: [
                Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: Colors.green.shade50,
                    shape: BoxShape.circle,
                  ),
                  child: Icon(
                    Icons.check_circle_outline,
                    color: Colors.green.shade600,
                    size: 24,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF111827),
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () {
                    Navigator.of(context).pop();
                    if (onClose != null) onClose!();
                  },
                  tooltip: 'Close',
                ),
              ],
            ),
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 12),
            
            // Changes list
            Expanded(
              child: changes.isEmpty
                  ? const Center(
                      child: Text(
                        'No changes were made',
                        style: TextStyle(
                          fontSize: 14,
                          color: Color(0xFF6B7280),
                        ),
                      ),
                    )
                  : ListView.builder(
                      itemCount: changes.length,
                      itemBuilder: (context, index) {
                        final change = changes[index];
                        return _buildChangeItem(change);
                      },
                    ),
            ),
            
            const SizedBox(height: 16),
            const Divider(),
            const SizedBox(height: 12),
            
            // Footer with close button
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                ElevatedButton.icon(
                  onPressed: () {
                    Navigator.of(context).pop();
                    if (onClose != null) onClose!();
                  },
                  icon: const Icon(Icons.done, size: 18),
                  label: const Text('Got it'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green.shade600,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildChangeItem(ChangeItem change) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: _getColorForType(change.type).withOpacity(0.3)),
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(
                  _getIconForType(change.type),
                  color: _getColorForType(change.type),
                  size: 20,
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        change.title,
                        style: TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: _getColorForType(change.type),
                        ),
                      ),
                      if (change.description != null) ...[
                        const SizedBox(height: 4),
                        Text(
                          change.description!,
                          style: const TextStyle(
                            fontSize: 13,
                            color: Color(0xFF4B5563),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ],
            ),
            if (change.details != null && change.details!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: const Color(0xFFF9FAFB),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: change.details!.map((detail) {
                    return Padding(
                      padding: const EdgeInsets.symmetric(vertical: 2),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            '• ',
                            style: TextStyle(
                              fontSize: 12,
                              color: Color(0xFF6B7280),
                            ),
                          ),
                          Expanded(
                            child: Text(
                              detail,
                              style: const TextStyle(
                                fontSize: 12,
                                color: Color(0xFF6B7280),
                              ),
                            ),
                          ),
                        ],
                      ),
                    );
                  }).toList(),
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }

  IconData _getIconForType(ChangeType type) {
    switch (type) {
      case ChangeType.added:
        return Icons.add_circle_outline;
      case ChangeType.removed:
        return Icons.remove_circle_outline;
      case ChangeType.modified:
        return Icons.edit_outlined;
      case ChangeType.merged:
        return Icons.merge;
      case ChangeType.moved:
        return Icons.drive_file_move_outline;
      case ChangeType.translated:
        return Icons.translate;
      case ChangeType.info:
        return Icons.info_outline;
    }
  }

  Color _getColorForType(ChangeType type) {
    switch (type) {
      case ChangeType.added:
        return Colors.green.shade600;
      case ChangeType.removed:
        return Colors.red.shade600;
      case ChangeType.modified:
        return Colors.blue.shade600;
      case ChangeType.merged:
        return Colors.purple.shade600;
      case ChangeType.moved:
        return Colors.orange.shade600;
      case ChangeType.translated:
        return Colors.teal.shade600;
      case ChangeType.info:
        return Colors.grey.shade600;
    }
  }
}

/// Represents a single change item in the summary
class ChangeItem {
  final ChangeType type;
  final String title;
  final String? description;
  final List<String>? details;

  ChangeItem({
    required this.type,
    required this.title,
    this.description,
    this.details,
  });
}

/// Type of change
enum ChangeType {
  added,
  removed,
  modified,
  merged,
  moved,
  translated,
  info,
}
