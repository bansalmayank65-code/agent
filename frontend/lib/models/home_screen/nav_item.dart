import 'package:flutter/material.dart';

/// Navigation item model for the home screen
class NavItem {
  final String label;
  final IconData icon;
  final String? sectionKey;

  const NavItem(this.label, this.icon, {this.sectionKey});
}