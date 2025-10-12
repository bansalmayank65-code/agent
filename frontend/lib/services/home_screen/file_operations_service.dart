import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:file_picker/file_picker.dart';
import 'dart:convert';
import '../../providers/task_provider.dart';
import '../../utils/platform_file_picker_stub.dart'
  if (dart.library.html) '../../utils/platform_file_picker_web.dart';

/// Service for handling file operations
class FileOperationsService {
  /// Pick a repository directory
  static Future<String?> pickRepositoryDirectory() async {
    if (kIsWeb) {
      return await pickDirectory();
    }
    
    try {
      return await FilePicker.platform.getDirectoryPath();
    } catch (e) {
      throw Exception('Directory pick failed: $e');
    }
  }

  /// Import task JSON file (web-specific)
  static Future<Map<String, dynamic>?> importTaskJsonWeb() async {
    if (!kIsWeb) {
      throw UnsupportedError('This method is only supported on web');
    }
    
    try {
      final result = await pickAndReadJsonFile();
      if (result?['data'] != null) {
        return {
          'data': result!['data'],
          'fileName': result['fileName'] ?? 'Unknown file',
        };
      }
      return null;
    } catch (e) {
      throw Exception('Failed to import JSON: $e');
    }
  }

  /// Pick a task.json file from the selected repository (native platforms)
  static Future<String?> pickTaskJsonFile(String repositoryPath) async {
    if (kIsWeb) {
      throw UnsupportedError('Use importTaskJsonWeb for web platforms');
    }
    
    try {
      // For native platforms, open file picker in the repository directory
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: ['json'],
        dialogTitle: 'Select task.json file',
        initialDirectory: repositoryPath,
      );
      
      if (result != null && result.files.isNotEmpty) {
        return result.files.first.path;
      }
      return null;
    } catch (e) {
      throw Exception('Failed to pick task.json file: $e');
    }
  }

  /// Load policy content from API
  static Future<String> loadPolicyContent(TaskProvider provider) async {
    if (provider.task.repositoryPath.isEmpty) {
      return 'No repository selected. Please select a repository first.';
    }

    try {
      // This would typically make an API call
      // For now, return a placeholder
      return 'Policy content for ${provider.task.repositoryPath}';
    } catch (e) {
      return 'Failed to load policy: $e';
    }
  }

  /// Copy aggregated content to clipboard
  static Future<void> copyAggregated(TaskProvider provider) async {
    final content = await _generateAggregatedContent(provider);
    if (content != null) {
      // Copy to clipboard implementation would go here
      // For now, just return the content
    }
  }

  /// Generate aggregated content
  static Future<String?> _generateAggregatedContent(TaskProvider provider) async {
    try {
      final jsonContent = jsonEncode(provider.task.toJson());
      return jsonContent;
    } catch (e) {
      return null;
    }
  }
}