import 'package:equatable/equatable.dart';
import 'package:json_annotation/json_annotation.dart';

part 'task_model.g.dart';

@JsonSerializable()
class TaskModel extends Equatable {
  final String instruction;
  final List<String> actions;
  // Full action objects including arguments/output if available
  final List<Map<String, dynamic>>? actionObjects;
  final String userId;
  final List<String> outputs;
  final List<Map<String, String>> edges;
  final String env;
  final int interfaceNum;
  final String repositoryPath;
  final String taskId;

  const TaskModel({
    required this.instruction,
    required this.actions,
    this.actionObjects,
    required this.userId,
    required this.outputs,
    required this.edges,
    this.env = 'finance',
    this.interfaceNum = 4,
    this.repositoryPath = '',
    this.taskId = '',
  });

  factory TaskModel.fromJson(Map<String, dynamic> json) =>
      _$TaskModelFromJson(json);

  Map<String, dynamic> toJson() => _$TaskModelToJson(this);

  TaskModel copyWith({
    String? instruction,
    List<String>? actions,
    List<Map<String, dynamic>>? actionObjects,
    String? userId,
    List<String>? outputs,
    List<Map<String, String>>? edges,
    String? env,
    int? interfaceNum,
    String? repositoryPath,
    String? taskId,
  }) {
    return TaskModel(
      instruction: instruction ?? this.instruction,
      actions: actions ?? this.actions,
      actionObjects: actionObjects ?? this.actionObjects,
      userId: userId ?? this.userId,
      outputs: outputs ?? this.outputs,
      edges: edges ?? this.edges,
      env: env ?? this.env,
      interfaceNum: interfaceNum ?? this.interfaceNum,
      repositoryPath: repositoryPath ?? this.repositoryPath,
      taskId: taskId ?? this.taskId,
    );
  }

  @override
  List<Object?> get props => [instruction, actions, actionObjects, userId, outputs, edges, env, interfaceNum, repositoryPath, taskId];
}

@JsonSerializable()
class ValidationResult extends Equatable {
  final bool isValid;
  final String message;
  final List<String> errors;

  const ValidationResult({
    required this.isValid,
    required this.message,
    this.errors = const [],
  });

  factory ValidationResult.fromJson(Map<String, dynamic> json) =>
      _$ValidationResultFromJson(json);

  Map<String, dynamic> toJson() => _$ValidationResultToJson(this);

  @override
  List<Object?> get props => [isValid, message, errors];
}