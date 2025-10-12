import 'package:flutter/material.dart';
import '../../models/home_screen/nav_item.dart';

/// Navigation service for managing navigation state and items
class NavigationService {
  static const List<NavItem> _navItems = [
    NavItem('Tasks History', Icons.history, sectionKey: 'tasks_history'),
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
  ];

  static List<NavItem> get navItems => _navItems;

  static int get itemCount => _navItems.length;

  static NavItem getItem(int index) => _navItems[index];
}