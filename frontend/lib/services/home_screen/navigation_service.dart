import 'package:flutter/material.dart';
import '../../models/home_screen/nav_item.dart';

/// Navigation service for managing navigation state and items
class NavigationService {
  static const List<NavItem> _navItems = [
    // Main workflow items
    // TODO: TEMPORARILY HIDDEN - NavItem('Tasks History', Icons.history, sectionKey: 'tasks_history'),
    NavItem('Import JSON', Icons.upload_file, sectionKey: 'repo'),
    NavItem('Project Parameters', Icons.settings_applications, sectionKey: 'params'),
    NavItem('Instruction', Icons.description, sectionKey: 'instruction'),
    NavItem('Actions', Icons.list_alt, sectionKey: 'actions'),
    NavItem('User ID', Icons.person, sectionKey: 'user'),
    NavItem('Outputs', Icons.output, sectionKey: 'outputs'),
    NavItem('Edges', Icons.account_tree, sectionKey: 'edges'),
    NavItem('Number of edges', Icons.analytics, sectionKey: 'num_edges'),
    NavItem('Task.json', Icons.code),
    NavItem('Validate Task.json', Icons.rule_folder),
    NavItem('Result.json', Icons.assessment, sectionKey: 'results'),
    
    // Utility tools section (separator)
    NavItem('', Icons.more_horiz, sectionKey: 'separator'), // Visual separator
    NavItem('Policy Actions Builder', Icons.build, sectionKey: 'policy_actions_builder'),
    NavItem('HR Expert Interface Changer', Icons.swap_horiz, sectionKey: 'hr_interface_changer'),
    NavItem('Merge Edges', Icons.merge, sectionKey: 'merge_edges'),
    NavItem('Refine task.json', Icons.auto_fix_high, sectionKey: 'task_refiner'),
  ];

  static List<NavItem> get navItems => _navItems;

  static int get itemCount => _navItems.length;

  static NavItem getItem(int index) => _navItems[index];
  
  /// Get the index where utility tools section starts
  static int get utilityToolsStartIndex => 11; // After Result.json (adjusted because Tasks History is hidden)
}