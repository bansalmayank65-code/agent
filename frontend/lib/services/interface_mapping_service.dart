import 'dart:convert';
import 'package:http/http.dart' as http;
import '../data/hr_experts_interface_method_mappings.dart';
import '../data/hr_talent_management_interface_method_mappings.dart';

class InterfaceMappingService {
  static const Map<String, String> _environmentFileMapping = {
    'hr_experts': 'hr_experts_interface_method_mappings.json',
    'hr_talent_management': 'hr_talent_management_interface_method_mappings.json',
    'wiki_confluence': 'wiki_confluence_interface_method_mappings.json',
  };

  static const Map<String, String> _environmentDisplayNames = {
    'hr_experts': 'HR Experts',
    'hr_talent_management': 'HR Talent Management',
    'wiki_confluence': 'Wiki Confluence',
  };

  /// Get list of available environments
  static List<String> getAvailableEnvironments() {
    return _environmentDisplayNames.keys.toList();
  }

  /// Get display name for environment
  static String getEnvironmentDisplayName(String envKey) {
    return _environmentDisplayNames[envKey] ?? envKey;
  }

  /// Load mapping data for a specific environment
  static Future<Map<String, dynamic>> loadMappingForEnvironment(String environment) async {
    print('DEBUG SERVICE: Loading mapping for environment: $environment');
    
    if (environment == 'hr_experts') {
      // Use the existing in-memory mapping for hr_experts
      print('DEBUG SERVICE: Using in-memory mapping for hr_experts');
      return loadHrMapping();
    }

    if (environment == 'hr_talent_management') {
      // Use the in-memory mapping for hr_talent_management for now
      print('DEBUG SERVICE: Using in-memory mapping for hr_talent_management');
      return loadHrTalentManagementMapping();
    }

    // For other environments, load from JSON files in workspace root
    final fileName = _environmentFileMapping[environment];
    if (fileName == null) {
      throw ArgumentError('Unknown environment: $environment');
    }

    print('DEBUG SERVICE: Attempting to load file: $fileName');
    
    try {
      // Try to load from workspace root (relative to the frontend serving location)
      final uri = Uri.parse('../$fileName');
      print('DEBUG SERVICE: Loading from URI: $uri');
      final response = await http.get(uri);
      
      print('DEBUG SERVICE: HTTP response status: ${response.statusCode}');
      
      if (response.statusCode == 200) {
        print('DEBUG SERVICE: Successfully loaded file, parsing JSON...');
        final parsed = jsonDecode(response.body) as Map<String, dynamic>;
        print('DEBUG SERVICE: Parsed mapping with ${parsed['method_mappings']?.length ?? 0} method mappings');
        return parsed;
      } else {
        print('DEBUG SERVICE: HTTP error ${response.statusCode}, falling back');
        throw Exception('Failed to load mapping file: $fileName (${response.statusCode})');
      }
    } catch (e) {
      print('DEBUG SERVICE: Error loading $fileName: $e');
      print('DEBUG SERVICE: Using fallback mapping');
      // Fallback: Return a basic structure with some common methods
      return _createFallbackMapping(environment);
    }
  }

  /// Create a fallback mapping structure when file loading fails
  static Map<String, dynamic> _createFallbackMapping(String environment) {
    return {
      'environment': environment,
      'description': 'Fallback mapping for $environment environment - JSON file not accessible',
      'version': '1.0',
      'last_updated': DateTime.now().toIso8601String(),
      'interface_count': 5,
      'interfaces': {
        'interface_1': {},
        'interface_2': {},
        'interface_3': {},
        'interface_4': {},
        'interface_5': {},
      },
      'method_mappings': {
        'example_method': {
          'category': 'management_operations',
          'description': 'Example method - JSON file not loaded',
          'interface_1': 'example_method',
          'interface_2': 'example_method',
          'interface_3': 'example_method',
          'interface_4': 'example_method',
          'interface_5': 'example_method'
        }
      }
    };
  }

  /// Get all method mappings from a loaded mapping
  static Map<String, dynamic> getMethodMappings(Map<String, dynamic> mapping) {
    return mapping['method_mappings'] as Map<String, dynamic>? ?? {};
  }

  /// Get interface list from a loaded mapping
  static List<String> getInterfaces(Map<String, dynamic> mapping) {
    final interfaces = mapping['interfaces'] as Map<String, dynamic>? ?? {};
    return interfaces.keys.toList();
  }

  /// Get environment name from a loaded mapping
  static String getEnvironmentName(Map<String, dynamic> mapping) {
    return mapping['environment'] as String? ?? 'unknown';
  }
}