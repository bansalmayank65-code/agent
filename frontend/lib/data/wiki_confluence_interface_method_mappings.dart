import 'dart:convert';

const String _wikiConfluenceMappingJson = r'''
{
  "environment": "wiki_confluence",
  "description": "Method mappings between different interfaces for Wiki Confluence environment.",
  "version": "1.0",
  "last_updated": "2025-10-23",
  "interface_count": 5,
  "total_methods_per_interface": 25,
  "is_audit_log": true,
  "method_categories": {
    "management_operations": {
      "description": "CRUD operations for wiki and confluence entities",
      "is_crud": true
    },
    "discovery_operations": {
      "description": "Search and query operations for finding wiki content",
      "is_crud": false
    },
    "approval_operations": {
      "description": "Approval workflow validation and processing for wiki operations",
      "is_crud": false
    },
    "human_transfer": {
      "description": "Operations to transfer control to human agents",
      "is_crud": false
    }
  },
  "interfaces": {
    "interface_1": {
      "prefix_patterns": {
        "management": "Manage",
        "discovery": "Discover",
        "approval": "Check",
        "human_transfer": "TransferTo"
      }
    },
    "interface_2": {
      "prefix_patterns": {
        "management": "Handle",
        "discovery": "Search",
        "approval": "Validate",
        "human_transfer": "SwitchTo"
      }
    },
    "interface_3": {
      "prefix_patterns": {
        "management": "Process",
        "discovery": "Find",
        "approval": "Verify",
        "human_transfer": "EscalateTo"
      }
    },
    "interface_4": {
      "prefix_patterns": {
        "management": "Administer",
        "discovery": "Lookup",
        "approval": "Confirm",
        "human_transfer": "HandOverTo"
      }
    },
    "interface_5": {
      "prefix_patterns": {
        "management": "Execute",
        "discovery": "Retrieve",
        "approval": "Authenticate",
        "human_transfer": "RouteTo"
      }
    }
  },
  "method_mappings": {
    "manage_wiki_page": {
      "category": "management_operations",
      "description": "Manage wiki pages and content",
      "interface_1": "manage_wiki_page",
      "interface_2": "handle_wiki_page",
      "interface_3": "process_wiki_page",
      "interface_4": "administer_wiki_page",
      "interface_5": "execute_wiki_page"
    },
    "manage_confluence_space": {
      "category": "management_operations",
      "description": "Manage confluence spaces and permissions",
      "interface_1": "manage_confluence_space",
      "interface_2": "handle_confluence_space",
      "interface_3": "process_confluence_space",
      "interface_4": "administer_confluence_space",
      "interface_5": "execute_confluence_space"
    },
    "discover_wiki_content": {
      "category": "discovery_operations",
      "description": "Search and discover wiki content",
      "interface_1": "discover_wiki_content",
      "interface_2": "search_wiki_content",
      "interface_3": "find_wiki_content",
      "interface_4": "lookup_wiki_content",
      "interface_5": "retrieve_wiki_content"
    }
  }
}
''';

Map<String, dynamic> loadWikiConfluenceMapping() {
  return jsonDecode(_wikiConfluenceMappingJson) as Map<String, dynamic>;
}