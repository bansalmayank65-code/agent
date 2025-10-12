import 'package:flutter/material.dart';

class CustomAppBar extends StatelessWidget {
  final String title;
  final VoidCallback? onBackPressed;
  final List<Widget>? actions;
  final Widget? leading;
  final bool showLogo;
  final Color? backgroundColor;
  final Color? foregroundColor;
  final double? elevation;

  const CustomAppBar({
    Key? key,
    required this.title,
    this.onBackPressed,
    this.actions,
    this.leading,
    this.showLogo = true,
    this.backgroundColor,
    this.foregroundColor,
    this.elevation,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final screenWidth = MediaQuery.of(context).size.width;
    final isDesktop = screenWidth > 1200;
    
    return Container(
      decoration: BoxDecoration(
        color: backgroundColor ?? Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.08),
            blurRadius: elevation ?? 8,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: SafeArea(
        child: Padding(
          padding: EdgeInsets.symmetric(
            horizontal: isDesktop ? 32.0 : 16.0, 
            vertical: isDesktop ? 16.0 : 8.0,
          ),
          child: Row(
            children: [
              if (leading != null)
                leading!
              else if (onBackPressed != null)
                IconButton(
                  icon: Icon(
                    Icons.arrow_back,
                    color: foregroundColor ?? const Color(0xFF667eea),
                    size: isDesktop ? 28 : 24,
                  ),
                  onPressed: onBackPressed,
                ),
              
              if (showLogo) ...[
                _buildLogo(isDesktop),
                SizedBox(width: isDesktop ? 24 : 16),
              ],
              
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(
                    fontSize: isDesktop ? 24 : 20,
                    fontWeight: FontWeight.bold,
                    color: foregroundColor ?? const Color(0xFF2d3748),
                  ),
                ),
              ),
              
              if (actions != null) ...actions!,
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildLogo(bool isDesktop) {
    final size = isDesktop ? 48.0 : 40.0;
    final iconSize = isDesktop ? 28.0 : 24.0;
    
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF667eea), Color(0xFF764ba2)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(isDesktop ? 12 : 8),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF667eea).withValues(alpha: 0.3),
            blurRadius: isDesktop ? 12 : 8,
            offset: Offset(0, isDesktop ? 6 : 4),
          ),
        ],
      ),
      child: Icon(
        Icons.auto_awesome,
        color: Colors.white,
        size: iconSize,
      ),
    );
  }
}

class TuringAppBar extends StatelessWidget implements PreferredSizeWidget {
  final String title;
  final List<Widget>? actions;
  final Widget? leading;
  final bool automaticallyImplyLeading;

  const TuringAppBar({
    Key? key,
    required this.title,
    this.actions,
    this.leading,
    this.automaticallyImplyLeading = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      title: Row(
        children: [
          _buildTuringLogo(),
          const SizedBox(width: 12),
          Text(
            title,
            style: const TextStyle(
              fontWeight: FontWeight.bold,
              color: Color(0xFF2d3748),
            ),
          ),
        ],
      ),
      backgroundColor: Colors.white,
      foregroundColor: const Color(0xFF667eea),
      elevation: 2,
      shadowColor: Colors.black.withValues(alpha: 0.1),
      leading: leading,
      automaticallyImplyLeading: automaticallyImplyLeading,
      actions: actions,
    );
  }

  Widget _buildTuringLogo() {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF667eea), Color(0xFF764ba2)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(6),
      ),
      child: const Center(
        child: Text(
          'T',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 18,
          ),
        ),
      ),
    );
  }

  @override
  Size get preferredSize => const Size.fromHeight(kToolbarHeight);
}