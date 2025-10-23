import '../data/hr_experts_interface_method_mappings.dart';
import '../data/hr_talent_management_interface_method_mappings.dart';
import '../data/wiki_confluence_interface_method_mappings.dart';

class InterfaceMappingService {
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
    switch (environment) {
      case 'hr_experts':
        return loadHrMapping();
      case 'hr_talent_management':
        return loadHrTalentManagementMapping();
      case 'wiki_confluence':
        return loadWikiConfluenceMapping();
      default:
        throw ArgumentError('Unknown environment: $environment');
    }
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