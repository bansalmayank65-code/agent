import 'dart:io';
import 'dart:math' as math;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_markdown/flutter_markdown.dart';
import 'package:provider/provider.dart';
import '../../providers/task_provider.dart';
import '../../services/api_service.dart';
import '../../widgets/home_screen/troubleshooting_steps_widget.dart';

class PolicySectionWidget extends StatelessWidget {
  const PolicySectionWidget({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = context.watch<TaskProvider>();
    
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Icon(Icons.policy, color: Color(0xFF059669)),
            const SizedBox(width: 8),
            const Text(
              'Policy Document (Read Only)',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        const SizedBox(height: 12),
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
                  'Policy document loaded from the selected repository. This defines rules and constraints for task creation. This section is read-only.',
                  style: TextStyle(fontSize: 12),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        
        // Policy file path info
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: Colors.blue.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.blue.withValues(alpha: 0.3)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(Icons.folder_open, color: Colors.blue[700], size: 16),
                  const SizedBox(width: 8),
                  Text(
                    'Policy File Location',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 12,
                      color: Colors.blue[700],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                _buildPolicyPath(provider),
                style: const TextStyle(fontSize: 11, fontFamily: 'monospace'),
              ),
              const SizedBox(height: 8),
              _buildLocationStatus(provider),
            ],
          ),
        ),
        const SizedBox(height: 16),
        
        // Policy content
        Container(
          height: 400,
          decoration: BoxDecoration(
            border: Border.all(color: Colors.grey[300]!),
            borderRadius: BorderRadius.circular(8),
            color: Colors.grey[50],
          ),
          child: FutureBuilder<String>(
            future: _loadPolicyContent(provider),
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      CircularProgressIndicator(),
                      SizedBox(height: 16),
                      Text('Loading policy document...'),
                    ],
                  ),
                );
              }
              
              if (snapshot.hasError) {
                return Center(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.error_outline, color: Colors.red, size: 48),
                        const SizedBox(height: 16),
                        Text(
                          'Policy Document Not Found',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.red[700],
                            fontSize: 18,
                          ),
                        ),
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.red.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: Colors.red.withValues(alpha: 0.3)),
                          ),
                          child: Text(
                            snapshot.error.toString(),
                            style: const TextStyle(fontSize: 12),
                            textAlign: TextAlign.center,
                          ),
                        ),
                        const SizedBox(height: 16),
                        const TroubleshootingStepsWidget(),
                      ],
                    ),
                  ),
                );
              }
              
              if (!snapshot.hasData || snapshot.data!.isEmpty) {
                return Center(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(Icons.description_outlined, color: Colors.grey, size: 48),
                        const SizedBox(height: 16),
                        Text(
                          'No Policy Document Available',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            color: Colors.grey[700],
                            fontSize: 18,
                          ),
                        ),
                        const SizedBox(height: 12),
                        const TroubleshootingStepsWidget(),
                      ],
                    ),
                  ),
                );
              }
              
              return Container(
                width: double.infinity,
                height: double.infinity,
                padding: const EdgeInsets.all(16),
                child: SingleChildScrollView(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // Header with read-only indicator and copy button
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          // Read-only indicator
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            decoration: BoxDecoration(
                              color: Colors.orange.withValues(alpha: 0.1),
                              borderRadius: BorderRadius.circular(4),
                              border: Border.all(color: Colors.orange.withValues(alpha: 0.3)),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                Icon(Icons.lock, color: Colors.orange[700], size: 14),
                                const SizedBox(width: 4),
                                Text(
                                  'READ ONLY',
                                  style: TextStyle(
                                    fontSize: 10,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.orange[700],
                                  ),
                                ),
                              ],
                            ),
                          ),
                          // Copy button
                          OutlinedButton.icon(
                            onPressed: () => _copyPolicyToClipboard(context, snapshot.data!),
                            icon: const Icon(Icons.copy, size: 16),
                            label: const Text('Copy Policy', style: TextStyle(fontSize: 12)),
                            style: OutlinedButton.styleFrom(
                              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                              minimumSize: Size.zero,
                              foregroundColor: Colors.blue[700],
                              side: BorderSide(color: Colors.blue[300]!),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      // Markdown content
                      MarkdownBody(
                        data: snapshot.data!,
                        selectable: true,
                        styleSheet: MarkdownStyleSheet(
                          h1: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                          h2: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                          h3: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                          p: const TextStyle(fontSize: 14, height: 1.5),
                          code: TextStyle(
                            backgroundColor: Colors.grey[100],
                            fontFamily: 'monospace',
                            fontSize: 12,
                          ),
                          codeblockDecoration: BoxDecoration(
                            color: Colors.grey[100],
                            borderRadius: BorderRadius.circular(4),
                            border: Border.all(color: Colors.grey[300]!),
                          ),
                          blockquoteDecoration: BoxDecoration(
                            color: Colors.blue.withValues(alpha: 0.1),
                            border: Border(
                              left: BorderSide(color: Colors.blue, width: 4),
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  String _buildPolicyPath(TaskProvider provider) {
    if (provider.task.repositoryPath.isEmpty || provider.task.env.isEmpty) {
      return 'Repository and environment must be selected first';
    }
    
    // Use forward slashes for web compatibility and let the backend handle path conversion
    String pathSeparator = '/';
    // Only check platform if not on web
    if (!kIsWeb) {
      try {
        if (Platform.isWindows) {
          pathSeparator = '\\';
        }
      } catch (e) {
        // Fallback to forward slashes if platform check fails
        pathSeparator = '/';
      }
    }
    
    String envsPath;
    
    if (kIsWeb) {
      // For web, construct path using string manipulation
      final repoDirPath = provider.task.repositoryPath;
      final parts = repoDirPath.split(RegExp(r'[/\\]'));
      // Remove last 3 parts (week_X/user/task) to get to amazon-tau-bench-tasks-main
      // But ensure we don't go below 0
      final basePartsLength = math.max(0, parts.length - 3);
      final baseParts = parts.sublist(0, basePartsLength);
      envsPath = baseParts.join(pathSeparator);
    } else {
      // For native platforms, use Directory.parent safely
      try {
        final repoDir = Directory(provider.task.repositoryPath);
        envsPath = repoDir.parent.parent.parent.path;
      } catch (e) {
        // Fallback to string manipulation if Directory operations fail
        final repoDirPath = provider.task.repositoryPath;
        final parts = repoDirPath.split(RegExp(r'[/\\]'));
        final basePartsLength = math.max(0, parts.length - 3);
        final baseParts = parts.sublist(0, basePartsLength);
        envsPath = baseParts.join(pathSeparator);
      }
    }
    
    return '$envsPath${pathSeparator}envs${pathSeparator}${provider.task.env}${pathSeparator}tools${pathSeparator}interface_${provider.task.interfaceNum}${pathSeparator}policy.md';
  }

  Future<String> _loadPolicyContent(TaskProvider provider) async {
    if (provider.task.repositoryPath.isEmpty || provider.task.env.isEmpty) {
      throw Exception('Repository and environment must be selected first');
    }
    
    try {
      // First try to load via API (works for both web and native with proper paths)
      final apiService = ApiService();
      final result = await apiService.getPolicy(
        directory: provider.task.repositoryPath,
        env: provider.task.env,
        interfaceNum: provider.task.interfaceNum,
      );
      
      return result['content'] as String;
    } catch (apiError) {
      // If API fails and we're not on web, try direct file access as fallback
      if (!kIsWeb) {
        try {
          final policyPath = _buildPolicyPath(provider);
          final policyFile = File(policyPath);
          
          if (!await policyFile.exists()) {
            throw Exception('Policy file not found at: $policyPath');
          }
          
          final content = await policyFile.readAsString();
          if (content.trim().isEmpty) {
            throw Exception('Policy file is empty');
          }
          
          return content;
        } catch (fileError) {
          throw Exception('Failed to load policy: API error: $apiError, File error: $fileError');
        }
      } else {
        // On web, if API fails, show a helpful error message
        throw Exception('Failed to load policy via API: $apiError. '
            'Make sure your repository path is correct and the backend is running.');
      }
    }
  }

  Widget _buildLocationStatus(TaskProvider provider) {
    if (provider.task.repositoryPath.isEmpty || provider.task.env.isEmpty) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.orange.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(4),
          border: Border.all(color: Colors.orange.withValues(alpha: 0.3)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.warning, color: Colors.orange[700], size: 14),
            const SizedBox(width: 4),
            Text(
              'MISSING PREREQUISITES',
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.bold,
                color: Colors.orange[700],
              ),
            ),
          ],
        ),
      );
    }

    // Check if running on web - file operations are limited
    if (kIsWeb) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: Colors.blue.withValues(alpha: 0.1),
          borderRadius: BorderRadius.circular(4),
          border: Border.all(color: Colors.blue.withValues(alpha: 0.3)),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.web, color: Colors.blue[700], size: 14),
            const SizedBox(width: 4),
            Text(
              'WEB MODE',
              style: TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.bold,
                color: Colors.blue[700],
              ),
            ),
          ],
        ),
      );
    }

    final policyPath = _buildPolicyPath(provider);
    
    return FutureBuilder<bool>(
      future: _checkFileExists(policyPath),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: Colors.blue.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: Colors.blue.withValues(alpha: 0.3)),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                SizedBox(
                  width: 14,
                  height: 14,
                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.blue[700]),
                ),
                const SizedBox(width: 4),
                Text(
                  'CHECKING...',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: Colors.blue[700],
                  ),
                ),
              ],
            ),
          );
        }

        final exists = snapshot.data ?? false;
        return Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: exists ? Colors.green.withValues(alpha: 0.1) : Colors.red.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: exists ? Colors.green.withValues(alpha: 0.3) : Colors.red.withValues(alpha: 0.3)),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                exists ? Icons.check_circle : Icons.error,
                color: exists ? Colors.green[700] : Colors.red[700],
                size: 14,
              ),
              const SizedBox(width: 4),
              Text(
                exists ? 'FILE EXISTS' : 'FILE NOT FOUND',
                style: TextStyle(
                  fontSize: 10,
                  fontWeight: FontWeight.bold,
                  color: exists ? Colors.green[700] : Colors.red[700],
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Future<bool> _checkFileExists(String path) async {
    try {
      final file = File(path);
      return await file.exists();
    } catch (e) {
      debugPrint('Error checking file existence: $e');
      return false;
    }
  }

  Future<void> _copyPolicyToClipboard(BuildContext context, String policyContent) async {
    try {
      await Clipboard.setData(ClipboardData(text: policyContent));
      
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle, color: Colors.white, size: 20),
                const SizedBox(width: 8),
                Text(
                  'Policy content copied to clipboard! (${policyContent.length} characters)',
                  style: const TextStyle(fontSize: 14),
                ),
              ],
            ),
            backgroundColor: Colors.green[600],
            duration: const Duration(seconds: 3),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          ),
        );
      }
    } catch (e) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.error, color: Colors.white, size: 20),
                const SizedBox(width: 8),
                Text('Failed to copy policy: $e'),
              ],
            ),
            backgroundColor: Colors.red[600],
            duration: const Duration(seconds: 3),
            behavior: SnackBarBehavior.floating,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          ),
        );
      }
    }
  }
}