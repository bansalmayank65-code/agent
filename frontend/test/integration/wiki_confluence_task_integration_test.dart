import 'package:flutter_test/flutter_test.dart';
import 'package:agentic_workstation/services/interface_mapping_service.dart';
import 'dart:convert';

void main() {
  group('Wiki Confluence Task JSON Integration Tests', () {
    late Map<String, dynamic> wikiMapping;
    late Map<String, dynamic> actualTaskJson;

    setUpAll(() async {
      // Load the wiki confluence mapping
      wikiMapping = await InterfaceMappingService.loadMappingForEnvironment('wiki_confluence');
      
      // Load the actual task JSON file (this is the content from your file)
      actualTaskJson = {
        "env": "wiki_confluence",
        "model_provider": "fireworks",
        "model": "qwen3-coder-480b-a35b-instruct",
        "num_trials": 3,
        "temperature": 1,
        "interface_num": 3,
        "task": {
          "user_id": "1",
          "instruction": "You are Danielle Johnson (danielle.johnson@siemens.com), a Global Administrator. You want to establish a new user group called Engineering Leadership with one initial member (mitchell.clark@siemens.com), and then send a notification to this user that you have been added to a new Engineering Leadership Group",
          "actions": [
            {
              "name": "retrieve_user",
              "arguments": {
                "identifier": "danielle.johnson@siemens.com",
                "identifier_type": "email"
              },
              "output": {
                "success": true,
                "user_data": {
                  "account_id": "ADM_46913810",
                  "created_at": "2023-01-29T22:32:16.592034",
                  "email": "danielle.johnson@siemens.com",
                  "full_name": "Danielle Johnson",
                  "global_role": "global_admin",
                  "user_id": "1"
                }
              }
            },
            {
              "name": "retrieve_group",
              "arguments": {
                "identifier": "Engineering Leadership",
                "identifier_type": "group_name"
              },
              "output": {
                "error": "Group with name 'Engineering Leadership' not found",
                "success": false
              }
            },
            {
              "name": "retrieve_user",
              "arguments": {
                "identifier": "mitchell.clark@siemens.com",
                "identifier_type": "email"
              },
              "output": {
                "success": true,
                "user_data": {
                  "account_id": "EMP_91756179",
                  "created_at": "2024-05-03T17:59:02.296518",
                  "email": "mitchell.clark@siemens.com",
                  "full_name": "Mitchell Clark",
                  "global_role": "space_member",
                  "user_id": "19"
                }
              }
            },
            {
              "name": "manipulate_groups",
              "arguments": {
                "action": "create",
                "group_name": "Engineering Leadership"
              },
              "output": {
                "action": "create",
                "group_data": {
                  "created_at": "2025-10-01T12:00:00",
                  "group_id": "9",
                  "group_name": "Engineering Leadership"
                },
                "group_id": "9",
                "message": "Group created successfully with name 'Engineering Leadership'",
                "success": true
              }
            },
            {
              "name": "manipulate_group_memberships",
              "arguments": {
                "action": "add",
                "group_id": "9",
                "user_id": "19"
              },
              "output": {
                "action": "add",
                "membership_data": {
                  "group_id": "9",
                  "joined_at": "2025-10-01T12:00:00",
                  "user_id": "19"
                },
                "message": "User 19 added to group 9 successfully",
                "success": true
              }
            },
            {
              "name": "transmit_notification",
              "arguments": {
                "event_type": "system_alert",
                "message": "You have been added to a new Engineering Leadership Group",
                "recipient_user_id": "9"
              },
              "output": {
                "message": "Notification sent to user 9",
                "notification_data": {
                  "channel": "system",
                  "created_at": "2025-10-01T12:00:00",
                  "delivery_status": "pending",
                  "event_type": "system_alert",
                  "message": "You have been added to a new Engineering Leadership Group",
                  "metadata": null,
                  "notification_id": "101",
                  "read_at": null,
                  "recipient_user_id": "9",
                  "related_entity_id": null,
                  "related_entity_type": null,
                  "sender_user_id": null,
                  "sent_at": null
                },
                "notification_id": "101",
                "success": true
              }
            },
            {
              "name": "register_new_audit_trail",
              "arguments": {
                "action_type": "create_group",
                "actor_user_id": "1",
                "target_entity_id": "9",
                "target_entity_type": "group"
              },
              "output": {
                "log_data": {
                  "action_type": "create_group",
                  "actor_user_id": "1",
                  "details": null,
                  "log_id": "101",
                  "occurred_at": "2025-10-01T12:00:00",
                  "target_entity_id": "9",
                  "target_entity_type": "group"
                },
                "log_id": "101",
                "message": "Audit log created for action 'create_group' by user 1",
                "success": true
              }
            },
            {
              "name": "register_new_audit_trail",
              "arguments": {
                "action_type": "add_user_to_group",
                "actor_user_id": "1",
                "target_entity_id": "19",
                "target_entity_type": "user"
              },
              "output": {
                "log_data": {
                  "action_type": "add_user_to_group",
                  "actor_user_id": "1",
                  "details": null,
                  "log_id": "102",
                  "occurred_at": "2025-10-01T12:00:00",
                  "target_entity_id": "19",
                  "target_entity_type": "user"
                },
                "log_id": "102",
                "message": "Audit log created for action 'add_user_to_group' by user 1",
                "success": true
              }
            },
            {
              "name": "register_new_audit_trail",
              "arguments": {
                "action_type": "send_notification",
                "actor_user_id": "1",
                "target_entity_id": "19",
                "target_entity_type": "user"
              },
              "output": {
                "log_data": {
                  "action_type": "send_notification",
                  "actor_user_id": "1",
                  "details": null,
                  "log_id": "103",
                  "occurred_at": "2025-10-01T12:00:00",
                  "target_entity_id": "19",
                  "target_entity_type": "user"
                },
                "log_id": "103",
                "message": "Audit log created for action 'send_notification' by user 1",
                "success": true
              }
            }
          ],
          "outputs": [],
          "edges": [
            {
              "from": "instruction",
              "connection": {
                "output": "identifier_type, identifier",
                "input": "identifier_type, identifier"
              },
              "to": "retrieve_user"
            },
            {
              "from": "instruction",
              "connection": {
                "output": "identifier_type, identifier",
                "input": "identifier_type, identifier"
              },
              "to": "retrieve_group"
            },
            {
              "from": "instruction",
              "connection": {
                "output": "action, group_name",
                "input": "action, group_name"
              },
              "to": "manipulate_groups"
            },
            {
              "from": "manipulate_groups",
              "connection": {
                "output": "group_id",
                "input": "group_id"
              },
              "to": "manipulate_group_memberships"
            },
            {
              "from": "retrieve_user",
              "connection": {
                "output": "user_data.user_id",
                "input": "user_id"
              },
              "to": "manipulate_group_memberships"
            },
            {
              "from": "instruction",
              "connection": {
                "output": "action",
                "input": "action"
              },
              "to": "manipulate_group_memberships"
            },
            {
              "from": "instruction",
              "connection": {
                "output": "event_type, message, recipient_user_id",
                "input": "event_type, message, recipient_user_id"
              },
              "to": "transmit_notification"
            },
            {
              "from": "retrieve_user",
              "connection": {
                "output": "user_data.user_id, user_data.user_id",
                "input": "actor_user_id, target_entity_id"
              },
              "to": "register_new_audit_trail"
            },
            {
              "from": "manipulate_groups",
              "connection": {
                "output": "group_id",
                "input": "target_entity_id"
              },
              "to": "register_new_audit_trail"
            },
            {
              "from": "instruction",
              "connection": {
                "output": "action_type, target_entity_type",
                "input": "action_type, target_entity_type"
              },
              "to": "register_new_audit_trail"
            }
          ],
          "num_edges": 10
        }
      };
    });

    test('should validate actual task JSON structure', () {
      expect(actualTaskJson['env'], equals('wiki_confluence'));
      expect(actualTaskJson['interface_num'], equals(3));
      expect(actualTaskJson['task'], isNotNull);
      expect(actualTaskJson['task']['actions'], isA<List>());
      
      final actions = actualTaskJson['task']['actions'] as List<dynamic>;
      expect(actions.length, equals(9));
      
      // Verify all expected method names are present
      final methodNames = actions.map((action) => action['name']).toList();
      final expectedMethods = [
        'retrieve_user',
        'retrieve_group', 
        'retrieve_user', // appears twice
        'manipulate_groups',
        'manipulate_group_memberships',
        'transmit_notification',
        'register_new_audit_trail', // appears 3 times
        'register_new_audit_trail',
        'register_new_audit_trail'
      ];
      
      for (int i = 0; i < expectedMethods.length; i++) {
        expect(methodNames[i], equals(expectedMethods[i]));
      }
    });

    test('should perform complete translation from interface_3 to interface_1', () {
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      final translatedJson = Map<String, dynamic>.from(actualTaskJson);
      final translatedMethods = <String, String>{};
      int totalReplacements = 0;
      
      // Helper function to map method names (mimicking the actual app logic)
      String mapMethodName(String methodName) {
        for (final entry in methodMappings.entries) {
          final mapEntry = entry.value as Map<String, dynamic>;
          final interface3Val = mapEntry['interface_3'];
          final interface1Val = mapEntry['interface_1'];
          if (interface3Val != null && interface3Val == methodName && interface1Val != null) {
            if (interface3Val != interface1Val) {
              translatedMethods[methodName] = interface1Val.toString();
              totalReplacements++;
            }
            return interface1Val.toString();
          }
        }
        return methodName;
      }
      
      // Transform the actions array
      final actions = translatedJson['task']['actions'] as List<dynamic>;
      for (final action in actions) {
        final actionMap = action as Map<String, dynamic>;
        final originalName = actionMap['name'] as String;
        final newName = mapMethodName(originalName);
        actionMap['name'] = newName;
      }
      
      // Verify specific translations occurred
      final expectedTranslations = {
        'retrieve_user': 'get_user',
        'retrieve_group': 'get_group',
        'manipulate_groups': 'manage_groups',
        'manipulate_group_memberships': 'manage_group_memberships',
        'transmit_notification': 'send_notification',
        'register_new_audit_trail': 'record_audit_log',
      };
      
      for (final entry in expectedTranslations.entries) {
        expect(translatedMethods[entry.key], equals(entry.value),
               reason: 'Method ${entry.key} should translate to ${entry.value}');
      }
      
      // Verify all actions were translated
      final translatedActions = translatedJson['task']['actions'] as List<dynamic>;
      final translatedMethodNames = translatedActions.map((action) => action['name']).toList();
      
      expect(translatedMethodNames, contains('get_user'));
      expect(translatedMethodNames, contains('get_group'));
      expect(translatedMethodNames, contains('manage_groups'));
      expect(translatedMethodNames, contains('manage_group_memberships'));
      expect(translatedMethodNames, contains('send_notification'));
      expect(translatedMethodNames, contains('record_audit_log'));
      
      // Verify we had translations
      expect(totalReplacements, greaterThan(0));
      expect(translatedMethods.length, greaterThan(0));
    });

    test('should preserve all other JSON data during translation', () {
      final originalJson = Map<String, dynamic>.from(actualTaskJson);
      final translatedJson = Map<String, dynamic>.from(actualTaskJson);
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      
      // Perform translation
      final actions = translatedJson['task']['actions'] as List<dynamic>;
      for (final action in actions) {
        final actionMap = action as Map<String, dynamic>;
        final originalName = actionMap['name'] as String;
        
        // Find translation
        for (final entry in methodMappings.entries) {
          final mapEntry = entry.value as Map<String, dynamic>;
          final interface3Val = mapEntry['interface_3'];
          final interface1Val = mapEntry['interface_1'];
          if (interface3Val != null && interface3Val == originalName && interface1Val != null) {
            actionMap['name'] = interface1Val.toString();
            break;
          }
        }
      }
      
      // Verify metadata preserved
      expect(translatedJson['env'], equals(originalJson['env']));
      expect(translatedJson['model_provider'], equals(originalJson['model_provider']));
      expect(translatedJson['num_trials'], equals(originalJson['num_trials']));
      expect(translatedJson['temperature'], equals(originalJson['temperature']));
      expect(translatedJson['interface_num'], equals(originalJson['interface_num']));
      
      // Verify task structure preserved
      expect(translatedJson['task']['user_id'], equals(originalJson['task']['user_id']));
      expect(translatedJson['task']['instruction'], equals(originalJson['task']['instruction']));
      expect(translatedJson['task']['outputs'], equals(originalJson['task']['outputs']));
      expect(translatedJson['task']['edges'], equals(originalJson['task']['edges']));
      expect(translatedJson['task']['num_edges'], equals(originalJson['task']['num_edges']));
      
      // Verify action arguments and outputs preserved
      final originalActions = originalJson['task']['actions'] as List<dynamic>;
      final translatedActions = translatedJson['task']['actions'] as List<dynamic>;
      
      for (int i = 0; i < originalActions.length; i++) {
        final originalAction = originalActions[i] as Map<String, dynamic>;
        final translatedAction = translatedActions[i] as Map<String, dynamic>;
        
        expect(translatedAction['arguments'], equals(originalAction['arguments']));
        if (originalAction.containsKey('output')) {
          expect(translatedAction['output'], equals(originalAction['output']));
        }
      }
    });

    test('should generate valid JSON after translation', () {
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      final translatedJson = Map<String, dynamic>.from(actualTaskJson);
      
      // Perform translation
      final actions = translatedJson['task']['actions'] as List<dynamic>;
      for (final action in actions) {
        final actionMap = action as Map<String, dynamic>;
        final originalName = actionMap['name'] as String;
        
        for (final entry in methodMappings.entries) {
          final mapEntry = entry.value as Map<String, dynamic>;
          final interface3Val = mapEntry['interface_3'];
          final interface1Val = mapEntry['interface_1'];
          if (interface3Val != null && interface3Val == originalName && interface1Val != null) {
            actionMap['name'] = interface1Val.toString();
            break;
          }
        }
      }
      
      // Test JSON serialization/deserialization
      final jsonString = jsonEncode(translatedJson);
      expect(() => jsonDecode(jsonString), returnsNormally);
      
      final reloadedJson = jsonDecode(jsonString) as Map<String, dynamic>;
      expect(reloadedJson['env'], equals('wiki_confluence'));
      expect(reloadedJson['task']['actions'], isA<List>());
    });

    test('should count translations correctly', () {
      final methodMappings = InterfaceMappingService.getMethodMappings(wikiMapping);
      final translatedMethods = <String, String>{};
      final methodCounts = <String, int>{};
      
      // Count occurrences of each method in the original JSON
      final actions = actualTaskJson['task']['actions'] as List<dynamic>;
      for (final action in actions) {
        final methodName = action['name'] as String;
        methodCounts[methodName] = (methodCounts[methodName] ?? 0) + 1;
      }
      
      // Verify expected counts
      expect(methodCounts['retrieve_user'] ?? 0, equals(2));
      expect(methodCounts['retrieve_group'] ?? 0, equals(1));
      expect(methodCounts['manipulate_groups'] ?? 0, equals(1));
      expect(methodCounts['manipulate_group_memberships'] ?? 0, equals(1));
      expect(methodCounts['transmit_notification'] ?? 0, equals(1));
      expect(methodCounts['register_new_audit_trail'] ?? 0, equals(3));
      
      // Perform translations and count unique translations
      for (final methodName in methodCounts.keys) {
        for (final entry in methodMappings.entries) {
          final mapEntry = entry.value as Map<String, dynamic>;
          final interface3Val = mapEntry['interface_3'];
          final interface1Val = mapEntry['interface_1'];
          if (interface3Val != null && interface3Val == methodName && interface1Val != null) {
            if (interface3Val != interface1Val) {
              translatedMethods[methodName] = interface1Val.toString();
            }
            break;
          }
        }
      }
      
      // Should have 6 unique method translations
      expect(translatedMethods.length, equals(6));
    });
  });
}