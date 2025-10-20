import 'package:flutter/material.dart';

/// A searchable dropdown widget that allows users to type and filter options
class SearchableDropdown<T> extends StatefulWidget {
  final T? value;
  final List<T> items;
  final String Function(T) itemAsString;
  final void Function(T?)? onChanged;
  final InputDecoration? decoration;
  final String? hintText;
  final bool enabled;

  const SearchableDropdown({
    Key? key,
    this.value,
    required this.items,
    required this.itemAsString,
    this.onChanged,
    this.decoration,
    this.hintText,
    this.enabled = true,
  }) : super(key: key);

  @override
  State<SearchableDropdown<T>> createState() => _SearchableDropdownState<T>();
}

class _SearchableDropdownState<T> extends State<SearchableDropdown<T>> {
  final TextEditingController _searchController = TextEditingController();
  final FocusNode _focusNode = FocusNode();
  List<T> _filteredItems = [];
  OverlayEntry? _overlayEntry;
  final LayerLink _layerLink = LayerLink();

  @override
  void initState() {
    super.initState();
    _filteredItems = widget.items;
    if (widget.value != null) {
      _searchController.text = widget.itemAsString(widget.value as T);
    }
    _focusNode.addListener(_onFocusChange);
  }

  @override
  void didUpdateWidget(SearchableDropdown<T> oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.value != oldWidget.value) {
      if (widget.value != null) {
        _searchController.text = widget.itemAsString(widget.value as T);
      } else {
        _searchController.clear();
      }
    }
    if (widget.items != oldWidget.items) {
      _filteredItems = widget.items;
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    _focusNode.removeListener(_onFocusChange);
    _focusNode.dispose();
    _removeOverlay();
    super.dispose();
  }

  void _onFocusChange() {
    if (_focusNode.hasFocus) {
      _showOverlay();
    } else {
      Future.delayed(const Duration(milliseconds: 200), () {
        _removeOverlay();
      });
    }
  }

  void _filterItems(String query) {
    setState(() {
      if (query.isEmpty) {
        _filteredItems = widget.items;
      } else {
        _filteredItems = widget.items.where((item) {
          return widget.itemAsString(item).toLowerCase().contains(query.toLowerCase());
        }).toList();
      }
    });
    _updateOverlay();
  }

  void _showOverlay() {
    _removeOverlay();
    _overlayEntry = _createOverlayEntry();
    Overlay.of(context).insert(_overlayEntry!);
  }

  void _removeOverlay() {
    _overlayEntry?.remove();
    _overlayEntry = null;
  }

  void _updateOverlay() {
    _overlayEntry?.markNeedsBuild();
  }

  OverlayEntry _createOverlayEntry() {
    RenderBox renderBox = context.findRenderObject() as RenderBox;
    var size = renderBox.size;

    return OverlayEntry(
      builder: (context) => Positioned(
        width: size.width,
        child: CompositedTransformFollower(
          link: _layerLink,
          showWhenUnlinked: false,
          offset: Offset(0.0, size.height + 5.0),
          child: Material(
            elevation: 4.0,
            borderRadius: BorderRadius.circular(8),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxHeight: 300),
              child: _filteredItems.isEmpty
                  ? Container(
                      padding: const EdgeInsets.all(16),
                      child: Text(
                        'No matching items',
                        style: TextStyle(color: Colors.grey[600]),
                      ),
                    )
                  : ListView.builder(
                      padding: EdgeInsets.zero,
                      shrinkWrap: true,
                      itemCount: _filteredItems.length,
                      itemBuilder: (context, index) {
                        final item = _filteredItems[index];
                        final isSelected = widget.value == item;
                        return InkWell(
                          onTap: () {
                            _selectItem(item);
                          },
                          child: Container(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                            decoration: BoxDecoration(
                              color: isSelected
                                  ? const Color(0xFF667eea).withOpacity(0.1)
                                  : null,
                              border: Border(
                                bottom: BorderSide(color: Colors.grey[200]!),
                              ),
                            ),
                            child: Row(
                              children: [
                                Expanded(
                                  child: Text(
                                    widget.itemAsString(item),
                                    style: TextStyle(
                                      fontWeight: isSelected ? FontWeight.w600 : FontWeight.normal,
                                      color: isSelected ? const Color(0xFF667eea) : Colors.black87,
                                    ),
                                  ),
                                ),
                                if (isSelected)
                                  const Icon(
                                    Icons.check,
                                    color: Color(0xFF667eea),
                                    size: 18,
                                  ),
                              ],
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ),
        ),
      ),
    );
  }

  void _selectItem(T item) {
    _searchController.text = widget.itemAsString(item);
    _focusNode.unfocus();
    _removeOverlay();
    widget.onChanged?.call(item);
  }

  @override
  Widget build(BuildContext context) {
    return CompositedTransformTarget(
      link: _layerLink,
      child: TextField(
        controller: _searchController,
        focusNode: _focusNode,
        enabled: widget.enabled,
        decoration: (widget.decoration ?? const InputDecoration()).copyWith(
          suffixIcon: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              if (_searchController.text.isNotEmpty && widget.enabled)
                IconButton(
                  icon: const Icon(Icons.clear, size: 18),
                  onPressed: () {
                    _searchController.clear();
                    _filterItems('');
                    widget.onChanged?.call(null);
                  },
                ),
              Icon(
                Icons.arrow_drop_down,
                color: widget.enabled ? null : Colors.grey,
              ),
              const SizedBox(width: 8),
            ],
          ),
        ),
        onChanged: (value) {
          _filterItems(value);
          // Don't auto-select while typing
        },
        onTap: () {
          _searchController.selection = TextSelection(
            baseOffset: 0,
            extentOffset: _searchController.text.length,
          );
        },
      ),
    );
  }
}
