import 'dart:convert';
import 'dart:io';

// Simplified test to check if our translation logic works
void main() async {
  // Load the mapping file
  final mappingFile = File('../hr_talent_management_interface_method_mappings.json');
  final mappingContent = await mappingFile.readAsString();
  final mapping = jsonDecode(mappingContent) as Map<String, dynamic>;
  
  print('Loaded mapping for: ${mapping['environment']}');
  
  // Get method mappings
  final methodMappings = mapping['method_mappings'] as Map<String, dynamic>;
  print('Method mappings count: ${methodMappings.length}');
  
  // Test methods from the task JSON
  final testMethods = [
    'fetch_reference_entities',
    'fetch_job_entities', 
    'administer_job_operations',
    'add_audit_entry'
  ];
  
  print('\nTesting method translations from interface_2 to interface_1:');
  
  for (final method in testMethods) {
    print('\nTesting method: $method');
    
    // Look for exact matches first
    String? foundTranslation;
    for (final entry in methodMappings.entries) {
      final mapEntry = entry.value as Map<String, dynamic>;
      final interface2Val = mapEntry['interface_2'];
      final interface1Val = mapEntry['interface_1'];
      
      if (interface2Val == method) {
        foundTranslation = interface1Val.toString();
        print('  Exact match found: $method -> $foundTranslation');
        break;
      }
    }
    
    if (foundTranslation == null) {
      // Try pattern matching
      print('  No exact match, trying pattern matching...');
      for (final entry in methodMappings.entries) {
        final mapEntry = entry.value as Map<String, dynamic>;
        final interface2Val = mapEntry['interface_2']?.toString();
        final interface1Val = mapEntry['interface_1']?.toString();
        
        if (interface2Val != null && interface1Val != null) {
          // Check pattern match
          if (matchesPattern(method, interface2Val)) {
            foundTranslation = convertPattern(method, interface2Val, interface1Val);
            print('  Pattern match found: $method -> $foundTranslation');
            print('    Via pattern: $interface2Val -> $interface1Val');
            break;
          }
        }
      }
    }
    
    if (foundTranslation == null) {
      print('  No translation found');
    }
  }
}

bool matchesPattern(String methodName, String patternMethod) {
  final methodParts = methodName.split('_');
  final patternParts = patternMethod.split('_');
  
  if (methodParts.length >= 2 && patternParts.length >= 2) {
    final methodSuffix = methodParts.skip(1).join('_');
    final patternSuffix = patternParts.skip(1).join('_');
    return methodSuffix == patternSuffix;
  }
  
  return false;
}

String convertPattern(String methodName, String sourcePattern, String targetPattern) {
  final methodParts = methodName.split('_');
  final targetParts = targetPattern.split('_');
  
  if (methodParts.length >= 2 && targetParts.length >= 2) {
    final methodSuffix = methodParts.skip(1).join('_');
    final targetPrefix = targetParts.first;
    return '${targetPrefix}_$methodSuffix';
  }
  
  return methodName;
}