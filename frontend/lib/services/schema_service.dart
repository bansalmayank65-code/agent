import 'dart:convert';
import 'package:flutter/services.dart';

class SchemaService {
  static Map<String, dynamic>? _schemas;

  static Future<Map<String, dynamic>> loadSchemas() async {
    if (_schemas != null) return _schemas!;
    
    try {
      final String response = await rootBundle.loadString('assets/samples/schemas.json');
      _schemas = json.decode(response);
      return _schemas!;
    } catch (e) {
      // Fallback to empty schemas if file load fails
      _schemas = {
        'actions_schema': [],
        'edges_schema': [],
        'task_schema': {}
      };
      return _schemas!;
    }
  }

  static Future<String> getActionsSchema() async {
    final schemas = await loadSchemas();
    return const JsonEncoder.withIndent('  ').convert(schemas['actions_schema']);
  }

  static Future<String> getEdgesSchema() async {
    final schemas = await loadSchemas();
    return const JsonEncoder.withIndent('  ').convert(schemas['edges_schema']);
  }

  static Future<String> getTaskSchema() async {
    final schemas = await loadSchemas();
    return const JsonEncoder.withIndent('  ').convert(schemas['task_schema']);
  }
}