import 'package:flutter_test/flutter_test.dart';
import 'package:agentic_workstation/services/interface_mapping_service.dart';
import 'dart:convert';

void main() {
  group('Wiki Confluence Interface Translation Tests', () {
    late Map<String, dynamic> wikiMapping;
    late Map<String, dynamic> testTaskJson;

    setUpAll(() async {
      // Load the wiki confluence mapping
      wikiMapping = await InterfaceMappingService.loadMappingForEnvironment('wiki_confluence');
      
      // Sample task JSON based on the provided file
      testTaskJson = {
        "env": "wiki_confluence",
        "model_provider": "fireworks",
        "model": "qwen3-coder-480b-a35b-instruct",
        "num_trials": 3,
        "temperature": 1,
        "interface_num": 3,
        "task": {
          "user_id": "1",
          "instruction": "You are Danielle Johnson, a Global Administrator. You want to establish a new user group called Engineering Leadership with one initial member, and then send a notification to this user.",
          "actions": [
            {
              "name": "retrieve_user",
              "arguments": {
                "identifier": "danielle.johnson@siemens.com",
                "identifier_type": "email"
              }
            },
            {
              "name": "retrieve_group",
              "arguments": {
                "identifier": "Engineering Leadership",
                "identifier_type": "group_name"
              }
            },
            {
              "name": "manipulate_groups",
              "arguments": {
                "action": "create",
                "group_name": "Engineering Leadership"
              }
            },
            {
              "name": "manipulate_group_memberships",
              "arguments": {
                "action": "add",
                "group_id": "9",
                "user_id": "19"
              }
            },
            {
              "name": "transmit_notification",
              "arguments": {
                "event_type": "system_alert",
                "message": "You have been added to a new Engineering Leadership Group",
                "recipient_user_id": "9"
              }
            },
            {
              "name": "register_new_audit_trail",
              "arguments": {
                "action_type": "create_group",
                "actor_user_id": "1",
                "target_entity_id": "9",
                "target_entity_type": "group"
              }
            }
          ]
        }
      };
    });

    test('should load wiki confluence mapping correctly', () {
      expect(wikiMapping, isNotNull);
      expect(wikiMapping['environment'], equals('wiki_confluence'));
      expect(wikiMapping['method_mappings'], isNotNull);
      expect(wikiMapping['method_mappings'], isA<Map<String, dynamic>>());
      
      final methodMappings = wikiMapping['method_mappings'] as Map<String, dynamic>;
      expect(methodMappings.isNotEmpty, isTrue);
    });

    test('should have correct interface mappings for key methods', () {
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      
      // Test retrieve_user mapping
      expect(methodMappings['get_user'], isNotNull);
      final getUserMapping = methodMappings['get_user'] as Map<String, dynamic>;
      expect(getUserMapping['interface_1'], equals('get_user'));
      expect(getUserMapping['interface_3'], equals('retrieve_user'));
      
      // Test manipulate_groups mapping
      expect(methodMappings['manage_groups'], isNotNull);
      final manageGroupsMapping = methodMappings['manage_groups'] as Map<String, dynamic>;
      expect(manageGroupsMapping['interface_1'], equals('manage_groups'));
      expect(manageGroupsMapping['interface_3'], equals('manipulate_groups'));
      
      // Test transmit_notification mapping
      expect(methodMappings['send_notification'], isNotNull);
      final sendNotificationMapping = methodMappings['send_notification'] as Map<String, dynamic>;
      expect(sendNotificationMapping['interface_1'], equals('send_notification'));
      expect(sendNotificationMapping['interface_3'], equals('transmit_notification'));
      
      // Test register_new_audit_trail mapping
      expect(methodMappings['record_audit_log'], isNotNull);
      final recordAuditMapping = methodMappings['record_audit_log'] as Map<String, dynamic>;
      expect(recordAuditMapping['interface_1'], equals('record_audit_log'));
      expect(recordAuditMapping['interface_3'], equals('register_new_audit_trail'));
    });

    test('should translate method names from interface_3 to interface_1', () {
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      final translatedMethods = <String, String>{};
      
      // Helper function to map method names (simplified version of the actual translation logic)
      String mapMethodName(String methodName, String sourceInterface, String targetInterface) {
        for (final entry in methodMappings.entries) {
          final mapEntry = entry.value as Map<String, dynamic>;
          final sourceVal = mapEntry[sourceInterface];
          final targetVal = mapEntry[targetInterface];
          if (sourceVal != null && sourceVal == methodName && targetVal != null) {
            return targetVal.toString();
          }
        }
        return methodName; // Return original if not found
      }
      
      // Test specific method translations
      final testCases = {
        'retrieve_user': 'get_user',
        'retrieve_group': 'get_group',
        'manipulate_groups': 'manage_groups',
        'manipulate_group_memberships': 'manage_group_memberships',
        'transmit_notification': 'send_notification',
        'register_new_audit_trail': 'record_audit_log',
      };
      
      for (final entry in testCases.entries) {
        final sourceMethod = entry.key;
        final expectedTarget = entry.value;
        
        final actualTarget = mapMethodName(sourceMethod, 'interface_3', 'interface_1');
        expect(actualTarget, equals(expectedTarget), 
               reason: 'Method $sourceMethod should translate to $expectedTarget but got $actualTarget');
        
        if (actualTarget != sourceMethod) {
          translatedMethods[sourceMethod] = actualTarget;
        }
      }
      
      expect(translatedMethods.length, greaterThan(0), 
             reason: 'Should have translated at least some methods');
    });

    test('should transform complete task JSON correctly', () {
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      final originalJson = Map<String, dynamic>.from(testTaskJson);
      final transformedJson = Map<String, dynamic>.from(testTaskJson);
      
      // Helper function to map method names
      String mapMethodName(String methodName) {
        for (final entry in methodMappings.entries) {
          final mapEntry = entry.value as Map<String, dynamic>;
          final interface3Val = mapEntry['interface_3'];
          final interface1Val = mapEntry['interface_1'];
          if (interface3Val != null && interface3Val == methodName && interface1Val != null) {
            return interface1Val.toString();
          }
        }
        return methodName;
      }
      
      // Transform the actions array
      final actions = transformedJson['task']['actions'] as List<dynamic>;
      for (final action in actions) {
        final actionMap = action as Map<String, dynamic>;
        final originalName = actionMap['name'] as String;
        final newName = mapMethodName(originalName);
        actionMap['name'] = newName;
      }
      
      // Verify transformations
      final transformedActions = transformedJson['task']['actions'] as List<dynamic>;
      final expectedTransformations = {
        'retrieve_user': 'get_user',
        'retrieve_group': 'get_group', 
        'manipulate_groups': 'manage_groups',
        'manipulate_group_memberships': 'manage_group_memberships',
        'transmit_notification': 'send_notification',
        'register_new_audit_trail': 'record_audit_log',
      };
      
      for (int i = 0; i < transformedActions.length; i++) {
        final action = transformedActions[i] as Map<String, dynamic>;
        final actionName = action['name'] as String;
        final originalActions = originalJson['task']['actions'] as List<dynamic>;
        final originalAction = originalActions[i] as Map<String, dynamic>;
        final originalName = originalAction['name'] as String;
        
        if (expectedTransformations.containsKey(originalName)) {
          expect(actionName, equals(expectedTransformations[originalName]),
                 reason: 'Action $originalName should be transformed to ${expectedTransformations[originalName]}');
        }
      }
      
      // Verify that non-method fields remain unchanged
      expect(transformedJson['env'], equals(originalJson['env']));
      expect(transformedJson['model_provider'], equals(originalJson['model_provider']));
      expect(transformedJson['task']['user_id'], equals(originalJson['task']['user_id']));
      expect(transformedJson['task']['instruction'], equals(originalJson['task']['instruction']));
    });

    test('should maintain JSON structure integrity after translation', () {
      final originalJsonString = jsonEncode(testTaskJson);
      final parsedJson = jsonDecode(originalJsonString) as Map<String, dynamic>;
      
      // Verify structure remains the same
      expect(parsedJson['env'], isNotNull);
      expect(parsedJson['task'], isNotNull);
      expect(parsedJson['task']['actions'], isA<List>());
      expect(parsedJson['task']['user_id'], isA<String>());
      expect(parsedJson['task']['instruction'], isA<String>());
      
      final actions = parsedJson['task']['actions'] as List<dynamic>;
      for (final action in actions) {
        expect(action, isA<Map<String, dynamic>>());
        final actionMap = action as Map<String, dynamic>;
        expect(actionMap['name'], isA<String>());
        expect(actionMap['arguments'], isA<Map<String, dynamic>>());
      }
    });

    test('should handle edge cases gracefully', () {
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      
      // Test with non-existent method
      String mapMethodName(String methodName) {
        for (final entry in methodMappings.entries) {
          final mapEntry = entry.value as Map<String, dynamic>;
          final interface3Val = mapEntry['interface_3'];
          final interface1Val = mapEntry['interface_1'];
          if (interface3Val != null && interface3Val == methodName && interface1Val != null) {
            return interface1Val.toString();
          }
        }
        return methodName;
      }
      
      // Test non-existent method - should return original
      expect(mapMethodName('non_existent_method'), equals('non_existent_method'));
      
      // Test empty string - should return original
      expect(mapMethodName(''), equals(''));
      
      // Test method that doesn't need translation - should return original
      expect(mapMethodName('some_other_method'), equals('some_other_method'));
    });
  });
}