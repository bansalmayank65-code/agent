// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'task_model.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

TaskModel _$TaskModelFromJson(Map<String, dynamic> json) => TaskModel(
      instruction: json['instruction'] as String,
      actions:
          (json['actions'] as List<dynamic>).map((e) => e as String).toList(),
      actionObjects: (json['actionObjects'] as List<dynamic>?)
          ?.map((e) => e as Map<String, dynamic>)
          .toList(),
      userId: json['userId'] as String,
      outputs:
          (json['outputs'] as List<dynamic>).map((e) => e as String).toList(),
      edges: (json['edges'] as List<dynamic>)
          .map((e) => Map<String, String>.from(e as Map))
          .toList(),
      env: json['env'] as String? ?? 'finance',
      interfaceNum: (json['interfaceNum'] as num?)?.toInt() ?? 4,
      repositoryPath: json['repositoryPath'] as String? ?? '',
      taskId: json['taskId'] as String? ?? '',
    );

Map<String, dynamic> _$TaskModelToJson(TaskModel instance) => <String, dynamic>{
      'instruction': instance.instruction,
      'actions': instance.actions,
      'actionObjects': instance.actionObjects,
      'userId': instance.userId,
      'outputs': instance.outputs,
      'edges': instance.edges,
      'env': instance.env,
      'interfaceNum': instance.interfaceNum,
      'repositoryPath': instance.repositoryPath,
      'taskId': instance.taskId,
    };

ValidationResult _$ValidationResultFromJson(Map<String, dynamic> json) =>
    ValidationResult(
      isValid: json['isValid'] as bool,
      message: json['message'] as String,
      errors: (json['errors'] as List<dynamic>?)
              ?.map((e) => e as String)
              .toList() ??
          const [],
    );

Map<String, dynamic> _$ValidationResultToJson(ValidationResult instance) =>
    <String, dynamic>{
      'isValid': instance.isValid,
      'message': instance.message,
      'errors': instance.errors,
    };
