class TaskHistoryModel {
  final String taskId;
  final String envName;
  final int interfaceNum;
  final String instruction;
  final int numOfEdges;
  final String? taskJson;
  final String? resultJson;
  final String userId;
  final String taskStatus;
  final bool isActive;
  final DateTime createdDateTime;
  final DateTime updatedDateTime;

  TaskHistoryModel({
    required this.taskId,
    required this.envName,
    required this.interfaceNum,
    required this.instruction,
    required this.numOfEdges,
    this.taskJson,
    this.resultJson,
    required this.userId,
    required this.taskStatus,
    required this.isActive,
    required this.createdDateTime,
    required this.updatedDateTime,
  });

  factory TaskHistoryModel.fromJson(Map<String, dynamic> json) {
    return TaskHistoryModel(
      taskId: json['taskId'] as String,
      envName: json['envName'] as String,
      interfaceNum: json['interfaceNum'] as int,
      instruction: json['instruction'] as String,
      numOfEdges: json['numOfEdges'] as int,
      taskJson: json['taskJson'] as String?,
      resultJson: json['resultJson'] as String?,
      userId: json['userId'] as String,
      taskStatus: json['taskStatus'] as String,
      isActive: json['isActive'] as bool,
      createdDateTime: DateTime.parse(json['createdDateTime'] as String),
      updatedDateTime: DateTime.parse(json['updatedDateTime'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'taskId': taskId,
      'envName': envName,
      'interfaceNum': interfaceNum,
      'instruction': instruction,
      'numOfEdges': numOfEdges,
      'taskJson': taskJson,
      'resultJson': resultJson,
      'userId': userId,
      'taskStatus': taskStatus,
      'isActive': isActive,
      'createdDateTime': createdDateTime.toIso8601String(),
      'updatedDateTime': updatedDateTime.toIso8601String(),
    };
  }

  // Helper methods for UI
  String get formattedCreatedDate {
    return '${createdDateTime.day}/${createdDateTime.month}/${createdDateTime.year}';
  }

  String get formattedUpdatedDate {
    return '${updatedDateTime.day}/${updatedDateTime.month}/${updatedDateTime.year}';
  }

  String get statusDisplayName {
    switch (taskStatus) {
      case 'DRAFT':
        return 'Draft';
      case 'SUBMITTED':
        return 'Submitted';
      case 'APPROVED':
        return 'Approved';
      case 'REJECTED':
        return 'Rejected';
      case 'NEEDS_CHANGES':
        return 'Needs Changes';
      case 'MERGED':
        return 'Merged';
      case 'DISCARDED':
        return 'Discarded';
      default:
        return taskStatus;
    }
  }

  // Status color for UI
  String get statusColor {
    switch (taskStatus) {
      case 'DRAFT':
        return '#6B7280'; // Gray
      case 'SUBMITTED':
        return '#3B82F6'; // Blue
      case 'APPROVED':
        return '#10B981'; // Green
      case 'REJECTED':
        return '#EF4444'; // Red
      case 'NEEDS_CHANGES':
        return '#F59E0B'; // Yellow
      case 'MERGED':
        return '#8B5CF6'; // Purple
      case 'DISCARDED':
        return '#9CA3AF'; // Light Gray
      default:
        return '#6B7280';
    }
  }

  // Check if task has results
  bool get hasResults => resultJson != null && resultJson!.isNotEmpty;

  // Check if task is complete
  bool get isComplete => ['APPROVED', 'MERGED'].contains(taskStatus);

  // Check if task needs attention
  bool get needsAttention => ['REJECTED', 'NEEDS_CHANGES'].contains(taskStatus);

  // Short instruction for display
  String get shortInstruction {
    if (instruction.length <= 100) return instruction;
    return '${instruction.substring(0, 97)}...';
  }

  @override
  String toString() {
    return 'TaskHistoryModel{taskId: $taskId, envName: $envName, userId: $userId, status: $taskStatus}';
  }

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    return other is TaskHistoryModel && other.taskId == taskId;
  }

  @override
  int get hashCode => taskId.hashCode;
}